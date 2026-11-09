//! Sampler: sysfs reads every ~1s, no shell forks.

use super::{frametime, now_ms, now_epoch_sec, Sample, Session, read_int};
use std::io::Write;
use std::path::{Path, PathBuf};

pub fn sample_once(sess: &mut Session, t: i64) {
    let mut row = Sample::new(t);
    row.fps = read_fps().unwrap_or(-1);
    row.refresh_hz = read_refresh_hz().unwrap_or(-1);
    // Frametime delta for this tick (needs this tick's refresh_hz for
    // jank). -1/-1 on missing/stale/unparsable/none/gen-reset ticks.
    let (ft_avg, ft_max) = frametime::update_session(sess, row.refresh_hz, now_ms());
    row.ft_avg_tenths = ft_avg;
    row.ft_max_tenths = ft_max;

    row.cpu_temp = sess.cpu_temp_path.as_ref().and_then(read_int).filter(|v| *v != 0).map(mc_to_deg_tenths).unwrap_or(-1);
    row.gpu_temp = max_temp(&sess.gpu_temps).map(mc_to_deg_tenths).unwrap_or(-1);
    row.bat_temp = max_temp(&sess.bat_temps).or_else(|| read_bat_temp_supply()).map(mc_to_deg_tenths).unwrap_or(-1);

    row.gpu_freq = sess.gpu_freq_src.as_ref().and_then(|s| s.read()).unwrap_or(-1);
    row.gpu_load = sess.gpu_load_src.as_ref().and_then(|s| s.read()).unwrap_or(-1);

    row.cpu_freq_l = sess.cpu_freq_l.as_ref().and_then(read_int).unwrap_or(-1);
    row.cpu_freq_b = sess.cpu_freq_b.as_ref().and_then(read_int).unwrap_or(-1);
    row.cpu_load = cpu_load_delta(&mut sess.prev_stat).unwrap_or(-1);

    row.ma = read_int(Path::new("/sys/class/power_supply/battery/current_now")).map(|v| (v / 1000).abs()).unwrap_or(-1);
    row.mv = read_int(Path::new("/sys/class/power_supply/battery/voltage_now")).map(|v| v / 1000).unwrap_or(-1);
    row.bat_lvl = read_int(Path::new("/sys/class/power_supply/battery/capacity")).unwrap_or(-1);

    let line = row.to_csv();
    let _ = std::io::Write::write_all(&mut sess.writer, format!("{}\n", line).as_bytes());
    sess.samples.push(row);
    sess.rows_since_flush += 1;
    if sess.rows_since_flush >= super::FLUSH_EVERY {
        let _ = sess.writer.flush();
        let _ = sess.writer.get_ref().sync_all();
        sess.rows_since_flush = 0;
    }
}

fn read_fps() -> Option<i64> {
    let content = std::fs::read_to_string(super::CURRENT_FPS_PATH).ok()?;
    let mut parts = content.split_whitespace();
    let fps: i64 = parts.next()?.parse().ok()?;
    let stamp: u64 = parts.next().and_then(|s| s.parse().ok()).unwrap_or(0);
    if stamp != 0 && now_epoch_sec() * 1000 > stamp + 3000 {
        return None;
    }
    Some(fps)
}

/// Active display refresh rate from app_status `refresh_rate <hz>`
/// (the same file the daemon's system-state cache is updated from).
/// Plain file read, no forks.
fn read_refresh_hz() -> Option<i64> {
    let content = std::fs::read_to_string("/data/adb/.config/AZenith/app_status").ok()?;
    for line in content.lines() {
        if let Some(rest) = line.strip_prefix("refresh_rate ") {
            return rest.trim().parse::<i64>().ok();
        }
    }
    None
}

fn cpu_load_delta(prev: &mut Option<(u64, u64)>) -> Option<i64> {
    let content = std::fs::read_to_string("/proc/stat").ok()?;
    let line = content.lines().next()?;
    if !line.starts_with("cpu ") {
        return None;
    }
    let mut it = line.split_whitespace();
    it.next(); // "cpu"
    let fields: Vec<u64> = it.filter_map(|s| s.parse().ok()).collect();
    if fields.len() < 4 {
        return None;
    }
    // fields: user nice system idle iowait irq softirq steal guest guest_nice
    let idle = fields[3] + fields[4];
    let busy: u64 = fields.iter().enumerate().filter(|(i, _)| *i != 3 && *i != 4).map(|(_, v)| *v).sum();
    let total = busy + idle;
    let result = match prev {
        Some((p_busy, p_total)) => {
            let dt = total.saturating_sub(*p_total);
            if dt == 0 {
                0
            } else {
                ((busy.saturating_sub(*p_busy)) as f64 / dt as f64 * 100.0).round() as i64
            }
        }
        None => -1,
    };
    *prev = Some((busy, total));
    Some(result)
}

fn max_temp(paths: &[PathBuf]) -> Option<i64> {
    paths.iter().filter_map(|p| read_int(p)).filter(|v| *v != 0 && *v > -100_000).max()
}

fn read_bat_temp_supply() -> Option<i64> {
    // /sys/class/power_supply/battery/temp is in tenths of a degree Celsius.
    read_int(Path::new("/sys/class/power_supply/battery/temp")).map(|v| v * 100)
}

/// Normalise a raw GPU frequency to kHz. Devfreq/kgsl nodes report Hz
/// (e.g. 886000000), GED reports kHz already (e.g. 886000) — scale Hz down
/// only when the raw value is above 1_000_000.
fn normalize_freq_khz(raw: i64) -> i64 {
    if raw >= 1_000_000 {
        raw / 1000
    } else {
        raw
    }
}

/// Convert a sysfs milli-°C reading to tenths of °C, the recorder's
/// canonical temperature unit (one rounding step).
pub fn mc_to_deg_tenths(mc: i64) -> i64 {
    mc / 100
}

fn read_gpu_load(dir: &Path) -> Option<i64> {
    if let Some(v) = read_int(dir.join("gpu_load")) {
        return Some(v);
    }
    // Some kernels expose `load` as a per-mille value.
    read_int(dir.join("load")).map(|v| v / 10)
}

// MediaTek GED GPU nodes (live under load on this device; devfreq cur_freq
// here is a stuck stub at the minimum OPP, so GED is authoritative).
const GED_FREQ_ENTIRE: &str = "/sys/kernel/ged/hal/current_freqency"; // "<opp idx> <kHz>" (typo is kernel ABI)
const GED_UTIL_PATH: &str = "/sys/kernel/ged/hal/gpu_utilization"; // "<busy%> <busy> <idle%>" (~first field = % busy)

/// Where GPU frequency comes from for this session.
#[derive(Debug, Clone)]
pub enum GpuFreqSrc {
    /// MTK GED: /sys/kernel/ged/hal/current_freqency (kHz direct).
    GedFreq { path: PathBuf },
    /// Mali devfreq cur_freq is in Hz (÷1000 → kHz).
    DevfreqFreq { dir: PathBuf },
    /// Adreno kgsl devfreq cur_freq is in Hz (÷1000 → kHz).
    KgslFreq { dir: PathBuf },
}

impl GpuFreqSrc {
    pub fn read(&self) -> Option<i64> {
        match self {
            GpuFreqSrc::GedFreq { path } => {
                let content = std::fs::read_to_string(path).ok()?;
                let s = content.split_whitespace().nth(1).unwrap_or("");
                s.parse::<i64>().ok()
            }
            GpuFreqSrc::DevfreqFreq { dir } => read_int(dir.join("cur_freq")).map(|v| normalize_freq_khz(v)),
            GpuFreqSrc::KgslFreq { dir } => read_int(dir.join("cur_freq")).map(|v| normalize_freq_khz(v)),
        }
    }
}

/// Where GPU load comes from for this session.
#[derive(Debug, Clone)]
pub enum GpuLoadSrc {
    /// MTK GED: /sys/kernel/ged/hal/gpu_utilization (busy% first field).
    GedUtil { path: PathBuf },
    Devfreq { dir: PathBuf },
    Kgsl { dir: PathBuf },
}

impl GpuLoadSrc {
    pub fn read(&self) -> Option<i64> {
        match self {
            GpuLoadSrc::GedUtil { path } => {
                let content = std::fs::read_to_string(path).ok()?;
                let v: i64 = content.split_whitespace().next()?.parse().ok()?;
                Some(v.clamp(0, 100))
            }
            GpuLoadSrc::Devfreq { dir } => read_gpu_load(dir),
            GpuLoadSrc::Kgsl { dir } => read_gpu_load(dir),
        }
    }
}

impl std::fmt::Display for GpuFreqSrc {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            GpuFreqSrc::GedFreq { path } => write!(f, "ged:{}", path.display()),
            GpuFreqSrc::DevfreqFreq { dir } => write!(f, "devfreq:{}", dir.display()),
            GpuFreqSrc::KgslFreq { dir } => write!(f, "kgsl:{}", dir.display()),
        }
    }
}

impl std::fmt::Display for GpuLoadSrc {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            GpuLoadSrc::GedUtil { path } => write!(f, "ged:{}", path.display()),
            GpuLoadSrc::Devfreq { dir } => write!(f, "devfreq:{}", dir.display()),
            GpuLoadSrc::Kgsl { dir } => write!(f, "kgsl:{}", dir.display()),
        }
    }
}

// ---------------------------------------------------------------------------
// Node detection (performed once per session start; values read each row).
// ---------------------------------------------------------------------------

pub struct Nodes {
    pub cpu_zone: Option<CpuZone>,
    pub gpu_temps: Vec<PathBuf>,
    pub bat_temps: Vec<PathBuf>,
    pub gpu_freq_src: Option<GpuFreqSrc>,
    pub gpu_load_src: Option<GpuLoadSrc>,
    pub cpu_freq_l: Option<PathBuf>,
    pub cpu_freq_b: Option<PathBuf>,
}

pub fn detect_nodes() -> Nodes {
    let (cpu_zones, gpu_temps, bat_temps) = thermal_zones();
    let cpu_zone = choose_cpu_zone(&cpu_zones);
    let (gpu_freq_src, gpu_load_src) = detect_gpu_nodes();
    let policies = detect_cpu_policies();
    let cpu_freq_l = policies.first().cloned();
    let cpu_freq_b = policies.last().filter(|_| policies.len() > 1).cloned();
    Nodes {
        cpu_zone,
        gpu_temps,
        bat_temps,
        gpu_freq_src,
        gpu_load_src,
        cpu_freq_l,
        cpu_freq_b,
    }
}

/// Pick GPU freq/load sources in priority order: MTK GED first (its devfreq
/// cur_freq is a stale stub on some kernels), then Mali devfreq, then KGSL.
fn detect_gpu_nodes() -> (Option<GpuFreqSrc>, Option<GpuLoadSrc>) {
    if Path::new(GED_FREQ_ENTIRE).exists() {
        let freq = Some(GpuFreqSrc::GedFreq { path: PathBuf::from(GED_FREQ_ENTIRE) });
        let load = if Path::new(GED_UTIL_PATH).exists() {
            Some(GpuLoadSrc::GedUtil { path: PathBuf::from(GED_UTIL_PATH) })
        } else {
            None
        };
        return (freq, load);
    }
    // Mali devfreq (MediaTek) — cur_freq in Hz.
    let base = Path::new("/sys/class/devfreq");
    if let Ok(entries) = std::fs::read_dir(base) {
        for entry in entries.flatten() {
            let path = entry.path();
            let name = path.file_name().map(|n| n.to_string_lossy().to_lowercase()).unwrap_or_default();
            if name.contains("mali") {
                let freq = Some(GpuFreqSrc::DevfreqFreq { dir: path.clone() });
                let load = Some(GpuLoadSrc::Devfreq { dir: path });
                return (freq, load);
            }
        }
    }
    // Adreno KGSL (Qualcomm).
    let kgsl = Path::new("/sys/class/kgsl/kgsl-3d0/devfreq");
    if kgsl.exists() {
        let freq = Some(GpuFreqSrc::KgslFreq { dir: kgsl.to_path_buf() });
        let load = Some(GpuLoadSrc::Kgsl { dir: kgsl.to_path_buf() });
        return (freq, load);
    }
    (None, None)
}

/// Enumerate thermal zones as (temp path, type string) groups so the caller
/// can pick specific sensors by name instead of a blind max over guesses.
fn thermal_zones() -> (Vec<(PathBuf, String)>, Vec<PathBuf>, Vec<PathBuf>) {
    let base = Path::new("/sys/class/thermal");
    let mut cpu = Vec::new();
    let mut gpu = Vec::new();
    let mut bat = Vec::new();
    let Ok(entries) = std::fs::read_dir(base) else {
        return (cpu, gpu, bat);
    };
    for entry in entries.flatten() {
        let path = entry.path();
        if !path.file_name().map(|n| n.to_string_lossy().starts_with("thermal_zone")).unwrap_or(false) {
            continue;
        }
        let Ok(t) = std::fs::read_to_string(path.join("type")) else { continue };
        let t = t.trim().to_string();
        let temp = path.join("temp");
        let lower = t.to_lowercase();
        if lower.contains("cpu") {
            cpu.push((temp, t));
        } else if lower.contains("gpu") || lower.contains("mali") || lower.contains("adreno") {
            gpu.push(temp);
        } else if lower.contains("battery") {
            bat.push(temp);
        }
    }
    (cpu, gpu, bat)
}

/// Choose ONE CPU thermal zone by type name (not by index, and never a max
/// over arbitrary matched zones — readings like 100+°C came from such a pool).
/// Prefers known CPU sensor names, then falls back to any cpu/soc zone.
pub struct CpuZone {
    pub path: PathBuf,
    pub type_name: String,
}

pub fn choose_cpu_zone(zones: &[(PathBuf, String)]) -> Option<CpuZone> {
    // Exact-ish preferred sensor type names, in priority order.
    const PREFERRED: &[&str] = &["cpu0", "cpu", "mtktscpu", "tscpu", "tzcpu", "tjmax", "soc"];
    let lower: Vec<(PathBuf, String)> = zones
        .iter()
        .map(|(p, t)| (p.clone(), t.to_lowercase()))
        .collect();
    for name in PREFERRED {
        if let Some((p, t)) = lower.iter().find(|(_, tz)| tz == name) {
            return Some(CpuZone { path: p.clone(), type_name: t.clone() });
        }
    }
    // Fallback: first zone whose type contains "cpu" (sorted by path for determinism).
    let mut sorted = lower.clone();
    sorted.sort_by(|a, b| a.0.cmp(&b.0));
    sorted.iter().find(|(_, tz)| tz.contains("cpu")).map(|(p, t)| CpuZone { path: p.clone(), type_name: t.clone() })
}

fn detect_gpu_devfreq() -> Option<PathBuf> {
    // Mali (MediaTek): /sys/class/devfreq/*mali*; Adreno (Qualcomm):
    // /sys/class/kgsl/kgsl-3d0/devfreq.
    let base = Path::new("/sys/class/devfreq");
    if let Ok(entries) = std::fs::read_dir(base) {
        for entry in entries.flatten() {
            let path = entry.path();
            let name = path.file_name().map(|n| n.to_string_lossy().to_lowercase()).unwrap_or_default();
            if name.contains("mali") {
                return Some(path);
            }
        }
    }
    let kgsl = Path::new("/sys/class/kgsl/kgsl-3d0/devfreq");
    if kgsl.exists() {
        return Some(kgsl.to_path_buf());
    }
    None
}

fn detect_cpu_policies() -> Vec<PathBuf> {
    let base = Path::new("/sys/devices/system/cpu/cpufreq");
    let mut out = Vec::new();
    if let Ok(entries) = std::fs::read_dir(base) {
        for entry in entries.flatten() {
            let path = entry.path();
            if path.file_name().map(|n| n.to_string_lossy().starts_with("policy")).unwrap_or(false) {
                let cur = path.join("scaling_cur_freq");
                if cur.exists() {
                    out.push(cur);
                }
            }
        }
    }
    out.sort_by(|a, b| {
        policy_id(a).cmp(&policy_id(b))
    });
    out
}

fn policy_id(p: &Path) -> u64 {
    p.parent()
        .and_then(|parent| parent.file_name())
        .map(|name| {
            let s = name.to_string_lossy();
            s.strip_prefix("policy").and_then(|n| n.parse().ok()).unwrap_or(0)
        })
        .unwrap_or(0)
}
