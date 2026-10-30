// ---------------- fps client: presented frame rate for the focused task ----------------
//
// The presented frame rate has to come from SurfaceFlinger. Two sources are available:
//
//   windowmanager   root app_process hosting WindowManager.registerTaskFpsCallback. The call is
//                   gated on ACCESS_FPS_COUNTER (signature|privileged), which AMS hands to uid 0
//                   for free, so the client has to run as root - hence an app_process this daemon
//                   spawns rather than code in the manager APK. Cheap: the framework pushes the
//                   rate at roughly 2 Hz.
//   surfaceflinger  `dumpsys SurfaceFlinger --timestats -dump` sampled once per second and
//                   differentiated from the global `totalFrames` counter. Costs a dumpsys per
//                   tick, but works on builds where the TaskFpsCallback path is unavailable.
//
// Both publish "<fps> <epoch_ms>" to <cfg>/API/current_fps, so the manager does not care which
// one is active.
//
// <cfg>/debug/explicit_fps_mthd pins the source:
//
//   missing / empty     automatic: try windowmanager, switch to surfaceflinger when it never
//                       publishes, and back again when that one dies
//   use_sf              surfaceflinger only, even when it keeps failing
//   use_windmanager     windowmanager only, even when it keeps failing
//
// The flag is re-read while a source runs, so changing it takes effect without restarting the
// daemon.

use std::fs;
use std::io::{BufRead, BufReader};
use std::os::unix::fs::PermissionsExt;
use std::process::{Command, Stdio};
use std::thread;
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

use crate::codes::RESOLVER_APK;
use crate::log::{error, info, warn};

/// Staged copy of the dex the client runs from. The resolver's own copy under /data/local/tmp is
/// deleted after every transaction-code refresh, so this one has to be separate.
const FPS_CLIENT_APK: &str = "/data/adb/.config/AZenith/fpsclient.apk";
/// "<fps> <epoch_ms>", read by the manager's live monitor.
pub(crate) const FPS_FILE: &str = "/data/adb/.config/AZenith/API/current_fps";
/// Directory `FPS_FILE` lives in; the surfaceflinger source may be the first writer.
const FPS_DIR: &str = "/data/adb/.config/AZenith/API";
/// stdout/stderr of the client, so its diagnostics do not vanish with the daemon's stdio.
const FPS_LOG: &str = "/data/adb/.config/AZenith/fpsclient.log";
/// Source pin, see the module docs. Missing or empty means automatic.
const METHOD_FLAG: &str = "/data/adb/.config/AZenith/debug/explicit_fps_mthd";

/// The windowmanager client has to publish a reading within this window, otherwise the source
/// counts as broken and the fallback takes over.
const CLIENT_STARTUP_TIMEOUT: Duration = Duration::from_secs(15);
/// Pause before a source is started again after an ordinary exit.
const RETRY_DELAY: Duration = Duration::from_secs(5);
/// Poll cadence for the child process.
const POLL_INTERVAL: Duration = Duration::from_millis(500);
/// Re-read the pin at most this often while a source is running.
const FLAG_CHECK_INTERVAL: Duration = Duration::from_secs(5);
/// SurfaceFlinger sampling interval. The manager treats a stamp older than 3 s as 0.
const SF_INTERVAL: Duration = Duration::from_secs(1);
/// Consecutive unusable `dumpsys` results before the source is declared dead.
const SF_MAX_MISSES: u32 = 5;
/// Line prefix of SurfaceFlinger's cumulative frame counter.
const SF_TOTAL_FRAMES: &str = "totalFrames = ";

/// Frame rate source.
#[derive(Clone, Copy, PartialEq, Eq)]
enum Method {
    WindowManager,
    SurfaceFlinger,
}

impl Method {
    fn name(self) -> &'static str {
        match self {
            Method::WindowManager => "windowmanager",
            Method::SurfaceFlinger => "surfaceflinger",
        }
    }

    fn other(self) -> Self {
        match self {
            Method::WindowManager => Method::SurfaceFlinger,
            Method::SurfaceFlinger => Method::WindowManager,
        }
    }
}

/// Keep one fps source alive. Runs on its own thread: a dead source must never take the daemon
/// down with it, and the daemon must not block waiting for it.
pub(crate) fn start() {
    thread::spawn(supervise);
}

/// Run a source until it dies, then either restart it or hand over to the other one.
///
/// `run_*` returns `true` when the source was publishing at some point (an ordinary exit, or a
/// flag change) and `false` when it never produced a reading.
fn supervise() {
    // Automatic mode starts with the cheap source and remembers the last working one, so a
    // fallback survives an ordinary restart instead of flapping back every cycle.
    let mut auto = Method::WindowManager;

    loop {
        let forced = explicit_method();
        let method = forced.unwrap_or(auto);

        let healthy = match method {
            Method::WindowManager => run_windmanager(forced),
            Method::SurfaceFlinger => run_surfaceflinger(forced),
        };

        match forced {
            Some(_) => {
                if healthy {
                    info!("fps source stopped method={}, restarting", method.name());
                } else {
                    warn!(
                        "fps source method={} failed, retrying the same one (pinned by {METHOD_FLAG})",
                        method.name()
                    );
                }
            }
            None => {
                if healthy {
                    auto = method;
                    info!("fps source stopped method={}, restarting", method.name());
                } else {
                    auto = method.other();
                    info!(
                        "fps source method={} failed, falling back to method={}",
                        method.name(),
                        auto.name()
                    );
                }
            }
        }

        thread::sleep(RETRY_DELAY);
    }
}

/// Source pinned by `<cfg>/debug/explicit_fps_mthd`, or `None` for automatic mode.
fn explicit_method() -> Option<Method> {
    let raw = fs::read_to_string(METHOD_FLAG).ok()?;
    match raw.trim().to_ascii_lowercase().as_str() {
        "" => None,
        "use_sf" => Some(Method::SurfaceFlinger),
        "use_windmanager" => Some(Method::WindowManager),
        other => {
            crate::log::debug!("ignoring unknown fps method flag value={other}");
            None
        }
    }
}

/// Run the root app_process client. Returns `false` when it never publishes anything.
fn run_windmanager(forced: Option<Method>) -> bool {
    if !stage_apk() {
        return false;
    }

    let started_at = SystemTime::now();
    let started = Instant::now();
    info!("fps client starting method=windowmanager out={FPS_FILE}");

    let mut child = match client().spawn() {
        Ok(child) => child,
        Err(e) => {
            error!("cannot spawn the fps client: {e}");
            return false;
        }
    };

    let mut healthy = false;
    let mut last_flag_check = Instant::now();

    loop {
        match child.try_wait() {
            Ok(Some(status)) => {
                if healthy {
                    warn!("fps client exited ({status}), restarting");
                } else {
                    warn!("fps client exited before publishing ({status})");
                }
                return healthy;
            }
            Ok(None) => {}
            Err(e) => {
                error!("fps client wait failed: {e}");
                return healthy;
            }
        }

        if last_flag_check.elapsed() >= FLAG_CHECK_INTERVAL {
            last_flag_check = Instant::now();
            if explicit_method() != forced {
                info!("fps method flag changed, restarting the client");
                let _ = child.kill();
                let _ = child.wait();
                return true;
            }
        }

        if !healthy {
            if fps_file_updated_since(started_at) {
                healthy = true;
                info!("fps client is publishing to {FPS_FILE}");
            } else if started.elapsed() >= CLIENT_STARTUP_TIMEOUT {
                warn!(
                    "fps client published nothing in {}s, giving up",
                    CLIENT_STARTUP_TIMEOUT.as_secs()
                );
                let _ = child.kill();
                let _ = child.wait();
                return false;
            }
        }

        thread::sleep(POLL_INTERVAL);
    }
}

/// Sample SurfaceFlinger's cumulative frame counter. Returns `false` when `dumpsys` never yields
/// a usable counter for `SF_MAX_MISSES` consecutive ticks.
fn run_surfaceflinger(forced: Option<Method>) -> bool {
    let _ = fs::create_dir_all(FPS_DIR);

    info!("fps source starting method=surfaceflinger out={FPS_FILE}");
    sf_timestats("-enable");

    let mut prev: Option<(u64, Instant)> = None;
    let mut healthy = false;
    let mut misses = 0u32;
    let mut last_flag_check = Instant::now();

    loop {
        if last_flag_check.elapsed() >= FLAG_CHECK_INTERVAL {
            last_flag_check = Instant::now();
            if explicit_method() != forced {
                info!("fps method flag changed, restarting the fps source");
                return true;
            }
        }

        match read_total_frames() {
            Some(total) => {
                misses = 0;
                let now = Instant::now();
                if let Some((prev_total, prev_time)) = prev {
                    let dt = now.duration_since(prev_time).as_secs_f32();
                    // total < prev_total means the counter was reset; skip this tick.
                    if total >= prev_total && dt > 0.0 {
                        let fps = (total - prev_total) as f32 / dt;
                        if publish(fps.round() as i64) && !healthy {
                            healthy = true;
                            info!("surfaceflinger fps source is publishing to {FPS_FILE}");
                        }
                    }
                }
                prev = Some((total, now));
            }
            None => {
                misses += 1;
                warn!("surfaceflinger timestats unavailable ({misses}/{SF_MAX_MISSES})");
                if misses >= SF_MAX_MISSES {
                    return healthy;
                }
            }
        }

        thread::sleep(SF_INTERVAL);
    }
}

fn sf_timestats(flag: &str) {
    let _ = Command::new("dumpsys")
        .args(["SurfaceFlinger", "--timestats", flag])
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .status();
}

/// Cumulative frame counter from SurfaceFlinger's global TimeStats.
fn read_total_frames() -> Option<u64> {
    let mut child = Command::new("dumpsys")
        .args(["SurfaceFlinger", "--timestats", "-dump"])
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .spawn()
        .ok()?;

    let mut total = None;
    if let Some(out) = child.stdout.take() {
        for line in BufReader::new(out).lines().map_while(Result::ok) {
            if let Some(v) = line.trim_start().strip_prefix(SF_TOTAL_FRAMES) {
                total = v.trim().parse::<u64>().ok();
                break;
            }
        }
    }
    let _ = child.wait();
    total
}

/// Publish "<fps> <epoch_ms>", the format the manager parses.
fn publish(fps: i64) -> bool {
    crate::state::write_atomic(FPS_FILE, &format!("{fps} {}\n", now_ms()))
}

fn now_ms() -> u128 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_millis()).unwrap_or(0)
}

fn fps_file_updated_since(since: SystemTime) -> bool {
    fs::metadata(FPS_FILE)
        .and_then(|meta| meta.modified())
        .is_ok_and(|mtime| mtime >= since)
}

fn client() -> Command {
    let mut c = Command::new("/system/bin/app_process");
    c.env_remove("LD_LIBRARY_PATH").env_remove("LD_PRELOAD");
    c.env("ANDROID_ROOT", "/system").env("ANDROID_DATA", "/data");
    c.env("ANDROID_RUNTIME_ROOT", "/apex/com.android.runtime").env("ANDROID_ART_ROOT", "/apex/com.android.art");
    c.env("ANDROID_I18N_ROOT", "/apex/com.android.i18n").env("ANDROID_TZDATA_ROOT", "/apex/com.android.tzdata");
    c.arg(format!("-Djava.class.path={FPS_CLIENT_APK}"))
        .arg("/")
        .arg("--nice-name=sys.azenith-fpsclient")
        .arg("zx.binder.FpsClient")
        .arg("--fps")
        .arg(format!("--out={FPS_FILE}"));
    if let Ok(f) = fs::File::create(FPS_LOG) {
        if let Ok(g) = f.try_clone() {
            c.stdout(g);
        }
        c.stderr(f);
    }
    c
}

fn stage_apk() -> bool {
    if fs::read(FPS_CLIENT_APK).is_ok_and(|v| v == RESOLVER_APK) {
        return true;
    }
    match fs::write(FPS_CLIENT_APK, RESOLVER_APK) {
        Ok(()) => {
            let _ = fs::set_permissions(FPS_CLIENT_APK, fs::Permissions::from_mode(0o644));
            true
        }
        Err(e) => {
            error!("failed to stage {FPS_CLIENT_APK}: {e}");
            false
        }
    }
}
