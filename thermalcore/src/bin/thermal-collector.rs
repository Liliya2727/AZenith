// ============================================================================
// thermal-collector — STAGE 0 dataset collection daemon (PLAN.md)
// ----------------------------------------------------------------------------
// Observation-only. Runs at 1 Hz alongside normal device use and appends a
// complete, time-aligned record of the thermal / power / performance state to
// append-only JSONL, one record per line, rotated at a size cap with bounded
// total retention.
//
// This binary is structurally incapable of actuation: it links the lib's
// observation modules (cpu, thermal_zones, cooling, context, dvfsrc, utils)
// and NEVER calls apply_intensity / DvfsrcActuator::apply. Cooling-device
// `cur_state` and `dvfsrc_req_vcore_opp` are captured as READ-only sysfs
// observations, so whatever mitigation daemon is running (stock or ours) is
// reflected in the data without any IPC.
//
// Schema v0 — one record per line, `kind` discriminates:
//
//   kind=sample (every tick):
//     schema_version, seq, t_mono, t_wall,
//     battery: {temp, current_now, voltage_now, charging}
//     zones:   [{name, temp} ...]          (millidegrees, sentinels rejected)
//     cpu:     {load, core_max, variance, per_core:[[id, load] ...],
//               freqs:{policy: cur_freq_khz}, time_in_state:{policy:[...]}}
//     cooling: [{type, cur_state, max_state} ...]
//     dvfsrc:  {opp} | null
//     context: {screen, charger, current_a}
//
//   kind=event (on change only):
//     schema_version, seq, t_mono, t_wall,
//     event: {type: "screen"|"charger"|"runaway", value, detail}
//
// No in-file derived labels: horizon ΔT labels are computed at training time
// from the raw series. Raw logs stay re-labelable.
// ============================================================================

use std::fs::{ self, File, OpenOptions };
use std::io::{ BufWriter, Write };
use std::path::{ Path, PathBuf };
use std::sync::Arc;
use std::sync::atomic::{ AtomicBool, Ordering };
use std::time::Duration;

use rynx_thermalcore::android_ffi::Logger;
use rynx_thermalcore::constants::DVFSRC_SYSFS_PATH;
use rynx_thermalcore::context::{ ChargerState, ExternalContext, ScreenState };
use rynx_thermalcore::cooling::CoolingDevice;
use rynx_thermalcore::cpu::CpuMonitor;
use rynx_thermalcore::thermal_zones::ThermalZone;
use rynx_thermalcore::utils::{ get_data_path, get_monotonic_time, get_wall_time };

use serde_json::json;

// ----------------------------------------------------------------------------
// Tunables (properties override paths only; cadence/caps are code constants
// so a bad prop can never turn the collector into a hot loop).
// ----------------------------------------------------------------------------

/// Schema version stamped in every record from day one (PLAN.md: schema drift).
const SCHEMA_VERSION: u32 = 0;
/// Fixed 1 Hz cadence.
const SAMPLE_PERIOD: Duration = Duration::from_secs(1);
/// Rotate the active file once it reaches this size.
const FILE_SIZE_CAP: u64 = 64 * 1024 * 1024;
/// Bound total retention; oldest rotated files are pruned beyond this.
const TOTAL_RETENTION: u64 = 1024 * 1024 * 1024;
/// Filename prefix: `thermal-YYYYMMDD-HHMMSS-seq.jsonl`.
const FILE_PREFIX: &str = "thermal-";
/// Accept only zones reading in this millidegree window (matches the
/// sentinel filter in `thermal_zones.rs`; MTK zombie zones read -127000).
const ZONE_TEMP_MAX: i32 = 200000;

fn collector_dir() -> PathBuf {
    let mut dir = get_data_path();
    dir.push("collector");
    if let Err(e) = fs::create_dir_all(&dir) {
        eprintln!("thermal-collector: cannot create {:?}: {}", dir, e);
    }
    dir
}

fn wall_time_secs() -> u64 {
    get_wall_time()
}

/// File stem timestamp is wall-clock for humans (`adb pull` browsing);
/// rotation decisions use byte size, ordering uses the trailing `seq`.
fn stamp_name(wall: u64) -> String {
    // Cheap calendar-free stamp: seconds since epoch is enough to order and
    // browse pulls; pandas parses t_wall inside the records anyway.
    format!("{}{}.jsonl", FILE_PREFIX, wall)
}

// ----------------------------------------------------------------------------
// Read-only sysfs helpers (observation only — no writes anywhere).
// ----------------------------------------------------------------------------

fn read_trim(path: &Path) -> Option<String> {
    fs::read_to_string(path).ok().map(|s| s.trim().to_string())
}

fn read_i64(path: &Path) -> Option<i64> {
    read_trim(path)?.parse::<i64>().ok()
}

fn read_battery(dir: &Path) -> serde_json::Value {
    let temp = read_i64(&dir.join("temp"));
    let current_now = read_i64(&dir.join("current_now"));
    let voltage_now = read_i64(&dir.join("voltage_now"));
    json!({
        "temp": temp,
        "current_now": current_now,
        "voltage_now": voltage_now,
        "charging": current_now.map(|c| c > 0),
    })
}

fn read_freqs() -> (serde_json::Value, serde_json::Value) {
    let mut freqs = serde_json::Map::new();
    let mut tis = serde_json::Map::new();
    let Ok(entries) = fs::read_dir("/sys/devices/system/cpu/cpufreq") else {
        return (json!(freqs), json!(tis));
    };
    for entry in entries.flatten() {
        let name = entry.file_name().to_string_lossy().into_owned();
        if !name.starts_with("policy") {
            continue;
        }
        let base = entry.path();
        if let Some(cur) = read_trim(&base.join("scaling_cur_freq"))
            .and_then(|s| s.parse::<u64>().ok())
        {
            freqs.insert(name.clone(), json!(cur));
        }
        // time_in_state is a two-column table; store raw rows so later
        // analysis can diff counters (deltas are NOT computed on device).
        if let Ok(raw) = fs::read_to_string(base.join("stats/time_in_state")) {
            let rows: Vec<serde_json::Value> = raw
                .lines()
                .filter_map(|line| {
                    let mut p = line.split_whitespace();
                    let freq = p.next()?.parse::<u64>().ok()?;
                    let ticks = p.next()?.parse::<u64>().ok()?;
                    Some(json!([freq, ticks]))
                })
                .collect();
            if !rows.is_empty() {
                tis.insert(name, json!(rows));
            }
        }
    }
    (json!(freqs), json!(tis))
}

fn read_cooling(devices: &[CoolingDevice]) -> serde_json::Value {
    let arr: Vec<serde_json::Value> = devices
        .iter()
        .map(|d| {
            let cur = read_trim(&d.path.join("cur_state"))
                .and_then(|s| s.parse::<i32>().ok());
            json!({ "type": d.device_type, "cur_state": cur, "max_state": d.max_state })
        })
        .collect();
    json!(arr)
}

fn read_dvfsrc_opp() -> Option<u32> {
    read_trim(
        &Path::new(DVFSRC_SYSFS_PATH).join("dvfsrc_req_vcore_opp")
    )?.parse::<u32>().ok()
}

// ----------------------------------------------------------------------------
// Append-only writer with size-cap rotation and bounded retention.
// ----------------------------------------------------------------------------

struct JsonlWriter {
    dir: PathBuf,
    active: PathBuf,
    out: BufWriter<File>,
    active_bytes: u64,
    file_seq: u64,
}

impl JsonlWriter {
    fn new(dir: PathBuf, logger: &Logger) -> std::io::Result<Self> {
        fs::create_dir_all(&dir)?;
        // Fresh file per boot so a crash mid-write only ever costs the last
        // partial line of the previous file (dropped by line-wise parsers).
        let name = stamp_name(wall_time_secs());
        let active = dir.join(&name);
        logger.info(&format!("thermal-collector: writing {}", active.display()));
        let file = OpenOptions::new().create(true).append(true).open(&active)?;
        let active_bytes = file.metadata().map(|m| m.len()).unwrap_or(0);
        Ok(JsonlWriter {
            dir,
            active,
            out: BufWriter::new(file),
            active_bytes,
            file_seq: 0,
        })
    }

    fn write_line(&mut self, line: &str, logger: &Logger) -> std::io::Result<()> {
        self.out.write_all(line.as_bytes())?;
        self.out.write_all(b"\n")?;
        self.active_bytes += line.len() as u64 + 1;
        if self.active_bytes >= FILE_SIZE_CAP {
            self.rotate(logger)?;
        }
        Ok(())
    }

    fn flush(&mut self) -> std::io::Result<()> {
        self.out.flush()
    }

    fn rotate(&mut self, logger: &Logger) -> std::io::Result<()> {
        self.out.flush()?;
        self.file_seq += 1;
        let name = format!("{}{}-{}.jsonl", FILE_PREFIX, wall_time_secs(), self.file_seq);
        let next = self.dir.join(&name);
        logger.info(
            &format!(
                "thermal-collector: rotating {} -> {}",
                self.active.display(),
                next.display()
            )
        );
        let file = OpenOptions::new().create(true).append(true).open(&next)?;
        self.active = next;
        self.active_bytes = 0;
        self.out = BufWriter::new(file);
        self.prune(logger);
        Ok(())
    }

    /// Keep newest files whose cumulative size fits TOTAL_RETENTION.
    /// Never deletes the active file.
    fn prune(&self, logger: &Logger) {
        let mut files: Vec<(u64, u64, PathBuf)> = Vec::new();
        let Ok(entries) = fs::read_dir(&self.dir) else { return };
        for entry in entries.flatten() {
            let path = entry.path();
            if path == self.active {
                continue;
            }
            if !path
                .file_name()
                .map(|n| n.to_string_lossy().starts_with(FILE_PREFIX))
                .unwrap_or(false)
            {
                continue;
            }
            let size = entry.metadata().map(|m| m.len()).unwrap_or(0);
            // mtime ordering: newest kept, oldest pruned first.
            let mtime = entry
                .metadata()
                .ok()
                .and_then(|m| m.modified().ok())
                .map(|_| wall_time_secs())
                .unwrap_or(0);
            files.push((mtime, size, path));
        }
        files.sort_by(|a, b| b.0.cmp(&a.0));

        let mut kept: u64 = self.active_bytes;
        for (_, size, path) in files {
            kept += size;
            if kept > TOTAL_RETENTION {
                logger.info(&format!("thermal-collector: pruning {}", path.display()));
                let _ = fs::remove_file(&path);
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Event log state (typed entries on change; runaway uses grace like daemon).
// ----------------------------------------------------------------------------

#[derive(Default)]
struct EventState {
    last_screen: Option<ScreenState>,
    last_charger: Option<ChargerState>,
    runaway_since: Option<u64>,
}

fn screen_name(s: ScreenState) -> &'static str {
    match s {
        ScreenState::On => "on",
        ScreenState::Off => "off",
    }
}

fn charger_name(c: ChargerState) -> &'static str {
    match c {
        ChargerState::None => "none",
        ChargerState::USB => "usb",
        ChargerState::AC => "ac",
        ChargerState::Wireless => "wireless",
    }
}

fn main() {
    let logger = Logger::new();
    logger.info("thermal-collector: Stage 0 dataset collector starting (observe-only)");

    let term = Arc::new(AtomicBool::new(false));
    if signal_hook::flag::register(signal_hook::consts::SIGTERM, Arc::clone(&term)).is_err() {
        eprintln!("thermal-collector: failed to register SIGTERM");
    }
    if signal_hook::flag::register(signal_hook::consts::SIGINT, Arc::clone(&term)).is_err() {
        eprintln!("thermal-collector: failed to register SIGINT");
    }

    let dir = collector_dir();
    let mut writer = match JsonlWriter::new(dir, &logger) {
        Ok(w) => w,
        Err(e) => {
            logger.error(&format!("thermal-collector: cannot open log: {}", e));
            std::process::exit(1);
        }
    };

    let mut cpu = CpuMonitor::new();
    let mut zones = ThermalZone::enumerate(&logger);
    let cooling = CoolingDevice::enumerate(&logger);
    let mut ctx = ExternalContext::new();
    let mut events = EventState::default();
    let battery_dir = Path::new("/sys/class/power_supply/battery").to_path_buf();

    let mut seq: u64 = 0;
    // Prime delta math (/proc/stat needs two reads) and baseline context
    // before the first logged sample so record #0 has real cpu deltas.
    ctx.update(&logger);
    let _ = cpu.get_snapshot(&logger);
    events.last_screen = Some(ctx.screen_state);
    events.last_charger = Some(ctx.charger_state);

    while !term.load(Ordering::Relaxed) {
        let tick_start = get_monotonic_time();
        let t_wall = wall_time_secs();
        seq += 1;

        ctx.update(&logger);
        let snap = cpu.get_snapshot(&logger);

        // Zones: type + temp for every accepted zone, raw. Sentinel filter
        // mirrors thermal_zones.rs (`temp <= 0 || temp > 200000` rejected)
        // plus an audit counter so the Stage 0 gate can prove no leakage.
        let mut zone_arr = Vec::new();
        let mut zone_rejected = 0u32;
        for z in zones.iter_mut() {
            match z.read_temp() {
                Some(t) if t > 0 && t <= ZONE_TEMP_MAX => {
                    zone_arr.push(json!({ "name": z.zone_type, "temp": t }));
                }
                _ => {
                    zone_rejected += 1;
                }
            }
        }

        let (freqs, tis) = read_freqs();
        let per_core: Vec<serde_json::Value> = snap
            .per_core
            .iter()
            .map(|(id, load)| json!([id, load]))
            .collect();

        let record = json!({
            "kind": "sample",
            "schema_version": SCHEMA_VERSION,
            "seq": seq,
            "t_mono": tick_start,
            "t_wall": t_wall,
            "battery": read_battery(&battery_dir),
            "zones": zone_arr,
            "zones_rejected": zone_rejected,
            "cpu": {
                "load": snap.cpu_load,
                "core_max": snap.core_load_max,
                "variance": snap.load_variance,
                "per_core": per_core,
                "freqs": freqs,
                "time_in_state": tis,
            },
            "cooling": read_cooling(&cooling),
            "dvfsrc": { "opp": read_dvfsrc_opp() },
            "context": {
                "screen": screen_name(ctx.screen_state),
                "charger": charger_name(ctx.charger_state),
                "current_a": ctx.power_features.current_now_abs,
            },
        });

        if let Err(e) = writer.write_line(&record.to_string(), &logger) {
            logger.error(&format!("thermal-collector: write failed: {}", e));
            break;
        }

        // Typed events, interleaved in the same stream, on change only.
        let emit = |ev_type: &str, value: &str, detail: serde_json::Value| {
            json!({
                "kind": "event",
                "schema_version": SCHEMA_VERSION,
                "seq": seq,
                "t_mono": tick_start,
                "t_wall": t_wall,
                "event": { "type": ev_type, "value": value, "detail": detail },
            })
        };

        if events.last_screen != Some(ctx.screen_state) {
            events.last_screen = Some(ctx.screen_state);
            let line = emit(
                "screen",
                screen_name(ctx.screen_state),
                serde_json::Value::Null
            );
            if writer.write_line(&line.to_string(), &logger).is_err() {
                break;
            }
        }
        if events.last_charger != Some(ctx.charger_state) {
            events.last_charger = Some(ctx.charger_state);
            let line = emit(
                "charger",
                charger_name(ctx.charger_state),
                serde_json::Value::Null
            );
            if writer.write_line(&line.to_string(), &logger).is_err() {
                break;
            }
        }

        // Runaway condition: sustained SoC≫battery gradient (>30 s at a high
        // average) mirrors the daemon's alert logic as a logged event, not
        // an action. Reuses zone reads would need fusion state, so this uses
        // the same raw max-vs-battery comparison the gate audits.
        let batt_deci = read_i64(&battery_dir.join("temp"));
        let soc_max = zone_arr
            .iter()
            .filter_map(|z| {
                let name = z.get("name")?.as_str()?.to_lowercase();
                let temp = z.get("temp")?.as_i64()?;
                if name.contains("cpu") || name.contains("soc") || name.contains("gpu") {
                    Some(temp)
                } else {
                    None
                }
            })
            .max();
        if let (Some(soc), Some(batt)) = (soc_max, batt_deci) {
            let gradient = (soc / 100) - batt;
            if gradient > 250 {
                if let Some(since) = events.runaway_since {
                    if tick_start.saturating_sub(since) > 30 {
                        events.runaway_since = Some(tick_start);
                        let line = emit(
                            "runaway",
                            "gradient",
                            json!({ "soc_deci": soc / 100, "batt_deci": batt })
                        );
                        if writer.write_line(&line.to_string(), &logger).is_err() {
                            break;
                        }
                    }
                } else {
                    events.runaway_since = Some(tick_start);
                }
            } else {
                events.runaway_since = None;
            }
        }

        if writer.flush().is_err() {
            break;
        }

        // Fixed 1 Hz from tick start; sleep only the remainder so sampling
        // doesn't drift under slow sysfs reads.
        let elapsed = get_monotonic_time().saturating_sub(tick_start);
        if elapsed < 1 {
            std::thread::sleep(Duration::from_secs(1 - elapsed));
        } else {
            logger.debug(
                &format!("thermal-collector: tick overrun by {}s", elapsed - 1)
            );
        }
    }

    let _ = writer.flush();
    logger.info("thermal-collector: shutting down, log flushed");
}
