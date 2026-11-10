//! FPS Recorder — owns recording state, sampling and file writing.
//!
//! Triggered from the floating monitor: the app writes `start` | `stop` to
//! API/record_cmd (inotify file watch), the daemon writes API/record_state
//! (`idle` | `recording <csv filename>`) for the overlay to read.
//!
//! Storage (root config base, e.g. /data/adb/.config/AZenith):
//!   frameratedata/<pkg>_<yyyyMMdd_HHmmss>.csv   — one row per second
//!   frameratedata/<pkg>_<yyyyMMdd_HHmmss>.json  — session summary
//!
//! CSV column units (header is fixed; unavailable values are always -1):
//!   t        seconds since session start
//!   fps      frames/second (from API/current_fps, stale >3s reads -1)
//!   ft_avg   ms, 1 decimal (from API/current_frametime delta; -1 if invalid)
//!   ft_max   ms, 1 decimal — upper edge of worst bucket; ">=500.0" for
//!            the >= 500 ms overflow bucket; -1 if invalid
//!   cpu_temp °C (thermal zone matched by type name; milli-°C / 100, 1 decimal)
//!   gpu_temp °C (thermal zone *gpu*/*mali*/*adreno; -1 if absent)
//!   bat_temp °C (thermal zone *battery*, else supply temp)
//!   gpu_freq kHz (devfreq cur_freq is in Hz → /1000; GED is already kHz)
//!   gpu_load percent 0-100 (devfreq gpu_load, else load/10; -1 if absent)
//!   cpu_freq_l kHz (first cpufreq policy; scaling_cur_freq is already kHz)
//!   cpu_freq_b kHz (last cpufreq policy)
//!   cpu_load percent 0-100 (from /proc/stat deltas)
//!   ma       milliamps (current_now uA / 1000, signed)
//!   mv       millivolts (voltage_now uV / 1000)
//!   bat_lvl  percent
//! Every numeric column is rendered as-is, so no reader has to guess units;
//! the JSON summary repeats them under its `units` object.
//!
//! Sampling performs no shell forks — every input is a direct file read.
//! Frame-time metrics (low1/low01/jank) are computed from the per-frame
//! histogram in API/current_frametime (see the `frametime` submodule):
//! delta histograms are merged into one session histogram and the worst
//! 1% / 0.1% means are taken at stop. `fps.avg`/`fps.min` stay per-second
//! FPS based. jank counts frames over twice the refresh period; low1
//! needs >= 100 valid frames and low01 >= 1000, otherwise null — never
//! a fabricated value. The recovery path (summary rebuilt from a leftover
//! CSV) has no histograms, so its low1/low01/jank stay null.
//!
//! Submodules: `sampler` (sysfs reads), `frametime` (current_frametime
//! diff + session histogram + lows), `writer` (CSV/JSON output +
//! summary math), `retention` (session cleanup).

pub mod frametime;
pub mod retention;
pub mod sampler;
pub mod writer;

use crate::core::DaemonContext;

use std::io::{BufWriter, Write};
use std::path::{Path, PathBuf};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

pub const RECORD_CMD_PATH: &str = "/data/adb/.config/AZenith/API/record_cmd";
pub const RECORD_STATE_PATH: &str = "/data/adb/.config/AZenith/API/record_state";
pub const FRAMERATEDATA_DIR: &str = "/data/adb/.config/AZenith/frameratedata";
pub const GAME_INFO_PATH: &str = "/data/adb/.config/AZenith/API/gameinfo";
pub const CURRENT_FPS_PATH: &str = "/data/adb/.config/AZenith/API/current_fps";
pub const CURRENT_FRAMETIME_PATH: &str = "/data/adb/.config/AZenith/API/current_frametime";

pub const SESSION_CSV_HEADER: &str =
    "t,fps,refresh_hz,ft_avg,ft_max,cpu_temp,gpu_temp,bat_temp,gpu_freq,gpu_load,cpu_freq_l,cpu_freq_b,cpu_load,ma,mv,bat_lvl";

/// Minimum session length — shorter sessions are discarded (no JSON).
pub const MIN_SESSION_SECS: u64 = 30;
/// Retention budget kept by finalize.
pub const RETENTION_MAX_SESSIONS: usize = 30;
pub const RETENTION_MAX_BYTES: u64 = 20 * 1024 * 1024;
/// Flush the CSV every N samples (rows), not every row.
pub const FLUSH_EVERY: usize = 5;
/// Debounce repeated start/stop commands.
pub const CMD_DEBOUNCE_MS: u64 = 500;

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum StopReason {
    Manual,
    GameClosed,
    PkgChanged,
    DaemonStop,
}

impl StopReason {
    fn as_str(self) -> &'static str {
        match self {
            StopReason::Manual => "manual",
            StopReason::GameClosed => "game_closed",
            StopReason::PkgChanged => "pkg_changed",
            StopReason::DaemonStop => "daemon_stop",
        }
    }
    fn display(self) -> &'static str {
        match self {
            StopReason::Manual => "manual",
            StopReason::GameClosed => "game closed",
            StopReason::PkgChanged => "pkg changed",
            StopReason::DaemonStop => "daemon stopped",
        }
    }
}

#[derive(Debug, Clone)]
pub struct Sample {
    pub t: i64,
    pub fps: i64,
    pub refresh_hz: i64,
    /// Per-second mean frame time in tenths of a ms; -1 = no valid delta.
    pub ft_avg_tenths: i64,
    /// Per-second worst frame time in tenths of a ms (upper edge of the
    /// highest non-empty bucket); -1 = no valid delta, -2 = >= 500 ms
    /// overflow bucket (rendered ">=500.0").
    pub ft_max_tenths: i64,
    pub cpu_temp: i64,
    pub gpu_temp: i64,
    pub bat_temp: i64,
    pub gpu_freq: i64,
    pub gpu_load: i64,
    pub cpu_freq_l: i64,
    pub cpu_freq_b: i64,
    pub cpu_load: i64,
    pub ma: i64,
    pub mv: i64,
    pub bat_lvl: i64,
}

impl Sample {
    fn new(t: i64) -> Self {
        Self {
            t,
            fps: -1,
            refresh_hz: -1,
            ft_avg_tenths: frametime::FT_MISSING,
            ft_max_tenths: frametime::FT_MISSING,
            cpu_temp: -1,
            gpu_temp: -1,
            bat_temp: -1,
            gpu_freq: -1,
            gpu_load: -1,
            cpu_freq_l: -1,
            cpu_freq_b: -1,
            cpu_load: -1,
            ma: -1,
            mv: -1,
            bat_lvl: -1,
        }
    }

    /// Temp columns are stored in tenths of °C (milli-°C/100) and rendered
    /// as a decimal ("103.2"); -1 stays the missing-value sentinel.
    pub fn to_csv(&self) -> String {
        format!(
            "{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}",
            self.t, self.fps, self.refresh_hz,
            frametime::ft_avg_csv(self.ft_avg_tenths),
            frametime::ft_max_csv(self.ft_max_tenths),
            temp_csv(self.cpu_temp), temp_csv(self.gpu_temp), temp_csv(self.bat_temp),
            self.gpu_freq, self.gpu_load, self.cpu_freq_l, self.cpu_freq_b,
            self.cpu_load, self.ma, self.mv, self.bat_lvl
        )
    }
}

/// Format a tenths-of-°C value as a one-decimal degree string.
pub fn temp_csv(tenths: i64) -> String {
    if tenths < 0 {
        "-1".to_string()
    } else {
        format!("{}.{}", tenths / 10, tenths % 10)
    }
}

pub struct Session {
    pub pkg: String,
    pub profile: String,
    pub start_epoch: u64,
    pub csv_path: PathBuf,
    pub json_path: PathBuf,
    pub writer: BufWriter<std::fs::File>,
    pub samples: Vec<Sample>,
    pub rows_since_flush: usize,
    /// One sample per second, deadline-based: next_deadline += 1s after each
    /// written row so a slow read delays the row instead of dropping `t`.
    pub next_deadline: Instant,
    /// `t` of the most recently written row (for finalize's last sample).
    pub last_written_t: i64,
    pub began_at: Instant,
    cpu_temp_path: Option<PathBuf>,
    gpu_temps: Vec<PathBuf>,
    #[allow(dead_code)]
    bat_temps: Vec<PathBuf>,
    gpu_freq_src: Option<sampler::GpuFreqSrc>,
    gpu_load_src: Option<sampler::GpuLoadSrc>,
    cpu_freq_l: Option<PathBuf>,
    cpu_freq_b: Option<PathBuf>,
    prev_stat: Option<(u64, u64)>,
    /// Last `current_frametime` snapshot; the next tick diffs against it.
    pub prev_frametime: Option<frametime::FrametimeSnapshot>,
    /// Merged delta histograms (bounded: exactly `frametime::HIST_LEN` u64s).
    pub session_hist: Vec<u64>,
    /// Total valid frames in `session_hist` (low1/low01 gates on this).
    pub session_frames: u64,
    /// Accumulated jank frames (see `frametime::count_jank`).
    pub session_jank: u64,
    /// Whether any tick ever had a usable refresh rate for jank counting.
    pub session_jank_known: bool,
    /// Whether any valid frametime delta was seen at all.
    pub has_frametime: bool,
}

pub struct Recorder {
    pub active: Option<Session>,
    last_cmd_millis: i64,
}

pub(crate) fn now_ms() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| (d.as_secs() * 1000 + d.subsec_millis() as u64) as i64)
        .unwrap_or(0)
}

pub fn now_epoch_sec() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0)
}

pub fn read_int(path: impl AsRef<Path>) -> Option<i64> {
    std::fs::read_to_string(path.as_ref()).ok()?.trim().parse::<i64>().ok()
}

impl Recorder {
    pub fn new() -> Self {
        Self { active: None, last_cmd_millis: 0 }
    }

    pub fn is_active(&self) -> bool {
        self.active.is_some()
    }

    /// Reset record_state to "idle" if the previous daemon left it saying
    /// "recording" (and build the summary from the leftover CSV).
    pub fn recover(&self, _ctx: &DaemonContext) {
        let _ = std::fs::create_dir_all(FRAMERATEDATA_DIR);
        let state = read_trimmed(RECORD_STATE_PATH);
        if let Some(s) = state {
            if let Some(rest) = s.strip_prefix("recording ") {
                let csv_name = rest.trim();
                if !csv_name.is_empty() {
                    // Sanitize: we only look inside frameratedata/.
                    let csv_path = Path::new(FRAMERATEDATA_DIR).join(csv_name);
                    if csv_path.exists() {
                        if let Err(e) = Self::build_summary_from_csv(&csv_path, csv_name) {
                            crate::log_warn!("recorder", "recovery summary failed file={} err={}", csv_name, e);
                        } else {
                            crate::log_info!("recorder", "recovery summary built file={}", csv_name);
                        }
                    }
                }
            }
        }
        write_record_state("idle");
    }

    /// Handle a record_cmd write ("start" | "stop").
    pub fn handle_cmd(&mut self, ctx: &mut DaemonContext, cmd_raw: &str) {
        let now = now_ms();
        if now - self.last_cmd_millis < CMD_DEBOUNCE_MS as i64 {
            crate::log_debug!("recorder", "cmd debounce ignored cmd={}", cmd_raw.trim());
            return;
        }
        self.last_cmd_millis = now;

        let cmd = cmd_raw.trim();
        if cmd == "start" {
            self.start(ctx);
        } else if cmd == "stop" {
            self.stop(ctx, StopReason::Manual);
        } else {
            crate::log_warn!("recorder", "unknown cmd={}", cmd);
        }
    }

    fn start(&mut self, ctx: &mut DaemonContext) {
        if self.active.is_some() {
            crate::log_debug!("recorder", "start ignored, already recording");
            return;
        }

        let pkg = match read_gameinfo_pkg() {
            Some(p) => p,
            None => {
                crate::log_warn!("recorder", "start refused reason=no active game");
                crate::util::notify("FPS Recorder", "No active game", false, 0, None);
                return;
            }
        };

        match self.begin_session(ctx, &pkg) {
            Ok(name) => {
                write_record_state(&format!("recording {}", name));
                crate::log_info!("recorder", "start pkg={} file={}", pkg, name);
                crate::util::notify("FPS Recorder", "Record started", false, 0, None);
            }
            Err(e) => {
                crate::log_error!("recorder", "start failed pkg={} err={}", pkg, e);
            }
        }
    }

    fn stop(&mut self, ctx: &mut DaemonContext, reason: StopReason) {
        if self.active.is_none() {
            crate::log_debug!("recorder", "stop ignored, not recording");
            return;
        }
        self.finalize(ctx, reason);
    }

    fn begin_session(&mut self, ctx: &mut DaemonContext, pkg: &str) -> std::io::Result<String> {
        let dir = Path::new(FRAMERATEDATA_DIR);
        std::fs::create_dir_all(dir)?;

        let base = session_base(pkg, now_epoch_sec());
        let (csv_name, csv_path) = unique_path(dir, &base, "csv")?;
        let json_path = dir.join(format!("{}.json", base));

        let file = std::fs::OpenOptions::new()
            .create(true)
            .write(true)
            .truncate(true)
            .open(&csv_path)?;
        let mut writer = BufWriter::new(file);
        writer.write_all(SESSION_CSV_HEADER.as_bytes())?;
        writer.write_all(b"\n")?;

        let nodes = sampler::detect_nodes();
        crate::log_info!("recorder", "gpu nodes freq={:?} load={:?}",
            nodes.gpu_freq_src.as_ref().map(|s| s.to_string()).as_deref().unwrap_or("none"),
            nodes.gpu_load_src.as_ref().map(|s| s.to_string()).as_deref().unwrap_or("none"));
        if let Some(z) = &nodes.cpu_zone {
            crate::log_info!("recorder", "cpu zone path={} type={}", z.path.display(), z.type_name);
        } else {
            crate::log_info!("recorder", "cpu zone: none found");
        }
        let began_at = Instant::now();
        self.active = Some(Session {
            pkg: pkg.to_string(),
            profile: crate::profile::profile_mode_name(ctx.profile.cur_mode).to_string(),
            start_epoch: now_epoch_sec(),
            csv_path,
            json_path,
            writer,
            samples: Vec::new(),
            rows_since_flush: 0,
            next_deadline: began_at + Duration::from_secs(1),
            last_written_t: 0,
            began_at,
            cpu_temp_path: nodes.cpu_zone.map(|z| z.path),
            gpu_temps: nodes.gpu_temps,
            bat_temps: nodes.bat_temps,
            gpu_freq_src: nodes.gpu_freq_src,
            gpu_load_src: nodes.gpu_load_src,
            cpu_freq_l: nodes.cpu_freq_l,
            cpu_freq_b: nodes.cpu_freq_b,
            prev_stat: None,
            prev_frametime: None,
            session_hist: vec![0u64; frametime::HIST_LEN],
            session_frames: 0,
            session_jank: 0,
            session_jank_known: false,
            has_frametime: false,
        });
        // First sample at t=0 immediately, then one per second via deadlines.
        if let Some(sess) = self.active.as_mut() {
            sampler::sample_once(sess, 0);
            sess.last_written_t = 0;
        }
        Ok(csv_name)
    }

    /// Per-loop tick: deadline-based sampling, never silently skip a t.
    pub fn tick(&mut self, ctx: &mut DaemonContext) {
        if self.active.is_none() {
            return;
        }

        // Game session checks every tick (ticks run at ~1s while recording).
        match read_gameinfo_pkg() {
            None => {
                self.finalize(ctx, StopReason::GameClosed);
                return;
            }
            Some(pkg) => {
                let current = self.active.as_ref().map(|s| s.pkg.clone()).unwrap_or_default();
                if pkg != current {
                    self.finalize(ctx, StopReason::PkgChanged);
                    return;
                }
            }
        }

        // Deadline catch-up: write every overdue row (written late if the
        // loop fell behind, never skipped).
        let now = Instant::now();
        if let Some(sess) = self.active.as_mut() {
            while now >= sess.next_deadline {
                let t = sess.next_deadline.duration_since(sess.began_at).as_secs() as i64;
                sampler::sample_once(sess, t);
                sess.last_written_t = t;
                sess.next_deadline += Duration::from_secs(1);
            }
        }
    }

    /// Always-run teardown: flush CSV, (maybe) write JSON, retention, notify.
    pub fn finalize(&mut self, _ctx: &mut DaemonContext, reason: StopReason) {
        let Some(mut sess) = self.active.take() else {
            return;
        };

        // Catch up all overdue ticks so no `t` is silently skipped, then
        // write the final sample so duration matches the rows.
        let now = Instant::now();
        while now >= sess.next_deadline {
            let t = sess.next_deadline.duration_since(sess.began_at).as_secs() as i64;
            sampler::sample_once(&mut sess, t);
            sess.last_written_t = t;
            sess.next_deadline += Duration::from_secs(1);
        }
        let elapsed_secs = sess.began_at.elapsed().as_secs() as i64;
        if elapsed_secs > sess.last_written_t {
            sampler::sample_once(&mut sess, elapsed_secs);
            sess.last_written_t = elapsed_secs;
        }

        let duration = sess.began_at.elapsed().as_secs();
        let duration_str = format!("{:02}:{:02}", duration / 60, duration % 60);

        // Flush the CSV writer before reading the file.
        let _ = sess.writer.flush();
        let _ = sess.writer.get_ref().sync_all();

        if duration < MIN_SESSION_SECS {
            let _ = std::fs::remove_file(&sess.csv_path);
            write_record_state("idle");
            crate::log_info!("recorder", "discarded short session pkg={} duration={}s", sess.pkg, duration);
            crate::util::notify("FPS Recorder", &format!("Record discarded (too short, {}s)", duration), false, 0, None);
            retention::apply_retention();
            return;
        }

        let summary = writer::build_summary_json(&sess, reason, false);
        if writer::write_json_atomic(&sess.json_path, &summary).is_ok() {
            crate::log_info!("recorder", "finalized pkg={} reason={} duration={}s file={:?}", sess.pkg, reason.as_str(), duration, sess.json_path);
            crate::util::notify("FPS Recorder", &format!("Record saved ({}, {})", reason.display(), duration_str), false, 0, None);
        } else {
            crate::log_error!("recorder", "json write failed path={:?}", sess.json_path);
        }
        drop(sess);

        write_record_state("idle");
        retention::apply_retention();
    }

    fn build_summary_from_csv(csv_path: &Path, csv_name: &str) -> std::io::Result<()> {
        let content = std::fs::read_to_string(csv_path)?;
        let total_secs: u64 = content.lines().count().saturating_sub(1).max(0) as u64;
        let (pkg, start_epoch) = writer::parse_session_name(csv_name);
        let json = writer::build_summary_json_rows(&content, &pkg, "", start_epoch, total_secs, StopReason::DaemonStop, true);
        let json_path = csv_path.with_extension("json");
        writer::write_json_atomic(&json_path, &json)
    }
}

fn read_trimmed(path: &str) -> Option<String> {
    std::fs::read_to_string(path).ok().map(|s| s.trim().to_string())
}

// ---------------------------------------------------------------------------
// helpers
// ---------------------------------------------------------------------------

fn read_gameinfo_pkg() -> Option<String> {
    let content = std::fs::read_to_string(GAME_INFO_PATH).ok()?;
    let first = content.lines().next()?.trim();
    if first.is_empty() {
        return None;
    }
    let pkg = first.split_whitespace().next()?;
    if pkg == "NULL" {
        None
    } else {
        Some(pkg.to_string())
    }
}

fn sanitize_pkg(pkg: &str) -> String {
    pkg.chars().map(|c| if c.is_ascii_alphanumeric() || c == '.' || c == '_' || c == '-' { c } else { '_' }).collect()
}

fn timestamp_name(epoch: u64) -> String {
    // yyyyMMdd_HHmmss via localtime_r (no chrono dependency).
    let t = epoch as libc::time_t;
    let mut tm = unsafe { std::mem::zeroed::<libc::tm>() };
    unsafe {
        if libc::localtime_r(&t, &mut tm).is_null() {
            return "19700101_000000".to_string();
        }
        let mut buf = [0u8; 32];
        let fmt = std::ffi::CString::new("%Y%m%d_%H%M%S").unwrap();
        libc::strftime(buf.as_mut_ptr() as *mut _, buf.len(), fmt.as_ptr(), &tm as *const libc::tm);
        String::from_utf8_lossy(&buf).trim_matches('\0').to_string()
    }
}

fn session_base(pkg: &str, epoch: u64) -> String {
    format!("{}_{}", sanitize_pkg(pkg), timestamp_name(epoch))
}

/// Find a free "<base>.<ext>" filename (appends -2, -3, ... on collision).
fn unique_path(dir: &Path, base: &str, ext: &str) -> std::io::Result<(String, PathBuf)> {
    let mut n = 1u32;
    loop {
        let name = if n == 1 {
            format!("{}.{}", base, ext)
        } else {
            format!("{}-{}.{}", base, n, ext)
        };
        let path = dir.join(&name);
        if !path.exists() {
            return Ok((name, path));
        }
        n += 1;
    }
}

fn write_record_state(content: &str) {
    let tmp = format!("{}.tmp", RECORD_STATE_PATH);
    let _ = std::fs::write(&tmp, format!("{}\n", content));
    let _ = std::fs::rename(&tmp, RECORD_STATE_PATH);
}
