// Copyright (C) 2026 Rianixia & Zexshia
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, version 3.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
//
// ---------------- fps client: presented frame rate for the focused task ----------------
//
// Three sources are available:
//
//   windowmanager   root app_process hosting WindowManager.registerTaskFpsCallback. The call is
//                   gated on ACCESS_FPS_COUNTER (signature|privileged), which AMS hands to uid 0
//                   for free, so the client has to run as root - hence an app_process this daemon
//                   spawns rather than code in the manager APK. Cheap: the framework pushes the
//                   rate at roughly 2 Hz.
//   uprobe          eBPF uprobe on libgui's Surface::queueBuffer family, attached to the focused
//                   app's processes and counted in userspace. Per-process and independent of
//                   SurfaceFlinger bookkeeping, but needs a kernel with BPF uprobes, a writable
//                   tracefs and root. 64-bit builds only: aya has no 32-bit Android support.
//   surfaceflinger  `dumpsys SurfaceFlinger --timestats -dump` sampled once per second and
//                   differentiated from the global `totalFrames` counter. Costs a dumpsys per
//                   tick, but works on builds where the TaskFpsCallback path is unavailable.
//
// All publish "<fps> <epoch_ms>" to <cfg>/API/current_fps, so the manager does not care which
// one is active.
//
// <cfg>/debug/explicit_fps_mthd pins the source:
//
//   missing / empty          automatic: try windowmanager, then uprobe, then surfaceflinger, and
//                            stay on the last one that published
//   use_windmanager          windowmanager only, even when it keeps failing
//   use_uprobe | use_ebpf    uprobe only, even when it keeps failing
//   use_sf                   surfaceflinger only, even when it keeps failing
//
// The flag is re-read while a source runs, so changing it takes effect without restarting the
// daemon.
//
// Game gate: the source only runs while a game is running (or while always-on is set).
//
//   <cfg>/API/gameinfo            "<package> <pid> <uid>" on its first line while a game runs,
//                                 "NULL 0 0" (or no file at all) when there is none.
//   <cfg>/debug/fps_counter_alwayson
//                                 "true" keeps the fps counter running without a game, exactly
//                                 like before the gate existed. Missing, empty or anything else
//                                 means the gate is in charge.
//
// Both files are watched with inotify (see inotify.rs) and mirrored into `GATE`, so every check
// below is a mutex read instead of file I/O, and sleeping code is woken the moment the answer
// changes; flag edits take effect immediately. While the gate is closed the source is stopped
// and current_fps is not deleted but holds the single line NO_ACTIVE_GAME, so the manager can
// tell "no game" from "daemon dead / file missing". The first real reading replaces it.

use std::fs;
use std::io::{self, BufRead, BufReader};
use std::os::unix::fs::PermissionsExt;
use std::process::{Command, Stdio};
use std::sync::{Condvar, Mutex, MutexGuard};
use std::thread;
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

use crate::codes::RESOLVER_APK;
use crate::inotify;
use crate::log::{error, info, warn};

/// Staged copy of the dex the client runs from. The resolver's own copy under /data/local/tmp is
/// deleted after every transaction-code refresh, so this one has to be separate.
const FPS_CLIENT_APK: &str = "/data/adb/.config/AZenith/fpsclient.apk";
/// "<fps> <epoch_ms>", read by the manager's live monitor. Holds `NO_GAME_MARKER` while the
/// gate is closed.
pub(crate) const FPS_FILE: &str = "/data/adb/.config/AZenith/API/current_fps";
/// Directory `FPS_FILE` lives in; the surfaceflinger source may be the first writer.
const FPS_DIR: &str = "/data/adb/.config/AZenith/API";
/// "<package> <pid> <uid>" while a game runs, "NULL 0 0" otherwise. Watched with inotify.
const GAMEINFO_FILE: &str = "/data/adb/.config/AZenith/API/gameinfo";
/// "true" keeps the fps counter on without a game. Watched with inotify.
const ALWAYS_ON_FLAG: &str = "/data/adb/.config/AZenith/debug/fps_counter_alwayson";
/// Content of `FPS_FILE` while no game runs and always-on is off.
pub(crate) const NO_GAME_MARKER: &str = "NO_ACTIVE_GAME";
/// stdout/stderr of the client, so its diagnostics do not vanish with the daemon's stdio.
const FPS_LOG: &str = "/data/adb/.config/AZenith/fpsclient.log";
/// Source pin, see the module docs. Missing or empty means automatic.
const METHOD_FLAG: &str = "/data/adb/.config/AZenith/debug/explicit_fps_mthd";

/// A source has to publish a reading within this window, otherwise it counts as broken and the
/// fallback takes over.
const CLIENT_STARTUP_TIMEOUT: Duration = Duration::from_secs(15);
/// Pause before a source is started again after an ordinary exit. Cut short when the gate closes.
const RETRY_DELAY: Duration = Duration::from_secs(5);
/// Poll cadence for the child process. Cut short when the gate closes.
const POLL_INTERVAL: Duration = Duration::from_millis(500);
/// Re-read the pin at most this often while a source is running.
const FLAG_CHECK_INTERVAL: Duration = Duration::from_secs(5);
/// Publish interval of the polling sources. The manager treats a stamp older than 3 s as 0.
const SF_INTERVAL: Duration = Duration::from_secs(1);
/// Consecutive unusable `dumpsys` results before the source is declared dead.
const SF_MAX_MISSES: u32 = 5;
/// Line prefix of SurfaceFlinger's cumulative frame counter.
const SF_TOTAL_FRAMES: &str = "totalFrames = ";

/// Whether the uprobe source can be compiled in at all (aya is 64-bit only).
const UPROBE_SUPPORTED: bool = cfg!(target_pointer_width = "64");

/// Whether a source should run, and the condvar that wakes whoever sleeps on it. Written only by
/// `sync_game` and `sync_always_on`, which the inotify threads drive.
static GATE: (Mutex<Gate>, Condvar) =
    (Mutex::new(Gate { game: false, always_on: false }), Condvar::new());

/// Frame rate source.
#[derive(Clone, Copy, PartialEq, Eq)]
enum Method {
    WindowManager,
    Uprobe,
    SurfaceFlinger,
}

impl Method {
    fn name(self) -> &'static str {
        match self {
            Method::WindowManager => "windowmanager",
            Method::Uprobe => "uprobe",
            Method::SurfaceFlinger => "surfaceflinger",
        }
    }

    /// First source tried in automatic mode, in order of preference.
    fn first_auto() -> Self {
        Method::WindowManager
    }

    /// Next source to try in automatic mode, in order of preference.
    fn next_auto(self) -> Self {
        match self {
            Method::WindowManager if UPROBE_SUPPORTED => Method::Uprobe,
            Method::WindowManager => Method::SurfaceFlinger,
            Method::Uprobe => Method::SurfaceFlinger,
            Method::SurfaceFlinger if UPROBE_SUPPORTED => Method::Uprobe,
            Method::SurfaceFlinger => Method::WindowManager,
        }
    }
}

// ---------------- activity gate ----------------

/// Everything that decides whether an fps source runs.
#[derive(Clone, Copy)]
struct Gate {
    /// `GAMEINFO_FILE` names a game.
    game: bool,
    /// `ALWAYS_ON_FLAG` says "true": run with or without a game.
    always_on: bool,
}

impl Gate {
    fn active(self) -> bool {
        self.game || self.always_on
    }
}

fn lock_gate() -> MutexGuard<'static, Gate> {
    GATE.0.lock().unwrap_or_else(|e| e.into_inner())
}

/// Whether an fps source should be running right now.
fn active() -> bool {
    lock_gate().active()
}

fn set_game(running: bool) {
    let changed = {
        let mut gate = lock_gate();
        let changed = gate.game != running;
        gate.game = running;
        changed
    };
    if changed {
        GATE.1.notify_all();
        if running {
            info!("gameinfo: game running");
        } else {
            info!("gameinfo: no game running");
        }
    }
}

fn set_always_on(on: bool) {
    let changed = {
        let mut gate = lock_gate();
        let changed = gate.always_on != on;
        gate.always_on = on;
        changed
    };
    if changed {
        GATE.1.notify_all();
        info!("fps_counter_alwayson: {}", if on { "on" } else { "off" });
    }
}

/// Block until the gate opens.
fn wait_for_active() {
    let mut gate = lock_gate();
    while !gate.active() {
        gate = GATE.1.wait(gate).unwrap_or_else(|e| e.into_inner());
    }
}

/// Sleep for up to `dur`, but wake as soon as the gate closes. Returns whether it is still open
/// afterwards; with the gate closed it returns immediately.
fn nap(dur: Duration) -> bool {
    let guard = lock_gate();
    let (guard, _) = GATE
        .1
        .wait_timeout_while(guard, dur, |gate| gate.active())
        .unwrap_or_else(|e| e.into_inner());
    guard.active()
}

/// Read gameinfo. `Some(true)` game running, `Some(false)` none (including a missing file),
/// `None` when the file is empty or unreadable at this instant, e.g. caught mid-rewrite: the
/// previous state is kept, a half-written file must not look like "game over".
fn read_game() -> Option<bool> {
    match fs::read_to_string(GAMEINFO_FILE) {
        Ok(raw) => {
            let package = raw.lines().next()?.split_whitespace().next()?;
            Some(!package.eq_ignore_ascii_case("null"))
        }
        Err(e) if e.kind() == io::ErrorKind::NotFound => Some(false),
        Err(_) => None,
    }
}

/// Re-read gameinfo and publish the result to `GATE`. Cheap and idempotent, it is what the
/// inotify thread calls on every event.
fn sync_game() {
    if let Some(running) = read_game() {
        set_game(running);
    }
}

/// Always-on is only on for the literal value "true"; a missing or empty file means off.
fn sync_always_on() {
    let on = fs::read_to_string(ALWAYS_ON_FLAG)
        .is_ok_and(|raw| raw.trim().eq_ignore_ascii_case("true"));
    set_always_on(on);
}

/// Replace current_fps with the no-game marker. Only called while no source is running, so
/// nothing can overwrite it behind our back.
fn mark_idle() {
    let _ = fs::create_dir_all(FPS_DIR);
    let _ = crate::state::write_atomic(FPS_FILE, &format!("{NO_GAME_MARKER}\n"));
}

// ---------------- supervisor ----------------

/// Keep one fps source alive while the gate is open (game running or always-on). Runs on its own thread: a dead source must never
/// take the daemon down with it, and the daemon must not block waiting for it.
pub(crate) fn start() {
    // Initial state first, so the supervisor does not start from a blind `false`.
    sync_game();
    sync_always_on();
    inotify::watch(GAMEINFO_FILE, sync_game);
    inotify::watch(ALWAYS_ON_FLAG, sync_always_on);
    thread::spawn(supervise);
}

/// Run a source until it dies, then either restart it or hand over to the other one.
///
/// `run_*` returns `true` when the source was publishing at some point (an ordinary exit, a flag
/// change or the gate closing) and `false` when it never produced a reading.
fn supervise() {
    // Automatic mode starts with the preferred source and remembers the last working one, so a
    // fallback survives an ordinary restart instead of flapping back every cycle.
    let mut auto = Method::first_auto();

    loop {
        // Gate: without a game (and without always-on) there is no source, and current_fps
        // holds NO_ACTIVE_GAME.
        if !active() {
            mark_idle();
            info!("no game running, fps source idle");
            wait_for_active();
            info!("fps source wanted, starting");
        }

        let forced = explicit_method();
        let method = forced.unwrap_or(auto);

        let healthy = match method {
            Method::WindowManager => run_windmanager(forced),
            Method::Uprobe => run_uprobe(forced),
            Method::SurfaceFlinger => run_surfaceflinger(forced),
        };

        // The gate closed: not a source failure, so no fallback and no retry delay. The source
        // is fully stopped by now, nothing can write current_fps behind our back.
        if !active() {
            mark_idle();
            continue;
        }

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
                    auto = method.next_auto();
                    info!(
                        "fps source method={} failed, falling back to method={}",
                        method.name(),
                        auto.name()
                    );
                }
            }
        }

        nap(RETRY_DELAY);
    }
}

/// Source pinned by `<cfg>/debug/explicit_fps_mthd`, or `None` for automatic mode.
fn explicit_method() -> Option<Method> {
    let raw = fs::read_to_string(METHOD_FLAG).ok()?;
    match raw.trim().to_ascii_lowercase().as_str() {
        "" => None,
        "use_sf" => Some(Method::SurfaceFlinger),
        "use_windmanager" => Some(Method::WindowManager),
        "use_uprobe" | "use_ebpf" => {
            if UPROBE_SUPPORTED {
                Some(Method::Uprobe)
            } else {
                warn!("fps method=uprobe is unavailable on 32-bit builds");
                None
            }
        }
        other => {
            crate::log::debug!("ignoring unknown fps method flag value={other}");
            None
        }
    }
}

// ---------------- sources ----------------

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

        // Killed and reaped before returning, so it cannot recreate current_fps afterwards.
        if !active() {
            info!("gate closed, stopping the fps client");
            let _ = child.kill();
            let _ = child.wait();
            return true;
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

        nap(POLL_INTERVAL);
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
        if !active() {
            info!("gate closed, stopping the fps source");
            return true;
        }

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
                        // The dumpsys above can take a while: do not publish after the gate
                        // closed in the meantime, or it would overwrite NO_ACTIVE_GAME.
                        if active() && publish(fps.round() as i64) && !healthy {
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

        nap(SF_INTERVAL);
    }
}

/// Count frames from the uprobe ring. Returns `false` when the kernel/libgui cannot be probed
/// (or when the first attach never produces a frame), so automatic mode can fall back.
#[cfg(target_pointer_width = "64")]
fn run_uprobe(forced: Option<Method>) -> bool {
    let mut probe = match uprobe::FrameProbe::new() {
        Ok(probe) => probe,
        Err(e) => {
            warn!("uprobe fps source unavailable: {e}");
            return false;
        }
    };

    info!("fps source starting method=uprobe out={FPS_FILE}");
    let _ = fs::create_dir_all(FPS_DIR);

    let mut focused: Option<(String, i32, i32)> = None;
    let mut targets: Vec<i32> = Vec::new();
    let mut attached_at: Option<Instant> = None;
    let mut attach_failures = 0u32;
    let mut last_ktime = 0u64;
    let mut frames = 0u32;
    let mut window = Instant::now();
    let mut healthy = false;
    let mut last_focus = Instant::now();
    let mut last_scan = Instant::now();
    let mut last_flag_check = Instant::now();

    loop {
        if !active() {
            info!("gate closed, stopping the fps source");
            return true;
        }

        if last_flag_check.elapsed() >= FLAG_CHECK_INTERVAL {
            last_flag_check = Instant::now();
            if explicit_method() != forced {
                info!("fps method flag changed, restarting the fps source");
                return true;
            }
        }

        if last_focus.elapsed() >= uprobe::FOCUS_INTERVAL {
            last_focus = Instant::now();
            let now_focused = uprobe::focused_app();
            let changed = now_focused != focused;

            // Games often render in a ":<suffix>" process, so the pid of the activity is not
            // enough: rescan the package's processes and follow every one of them.
            if changed || last_scan.elapsed() >= uprobe::TARGET_REFRESH {
                last_scan = Instant::now();
                focused = now_focused;
                let pids = match &focused {
                    Some((package, uid, pid)) => uprobe::target_pids(package, *uid, *pid),
                    None => Vec::new(),
                };

                if changed || pids != targets {
                    probe.detach();
                    // Records still queued belong to the process that just went away.
                    probe.drain(|_| {});

                    targets = pids;
                    attached_at = None;
                    last_ktime = 0;
                    frames = 0;
                    window = Instant::now();

                    if targets.is_empty() {
                        attach_failures = 0;
                    } else {
                        match probe.attach(&targets) {
                            Ok(symbols) => {
                                attach_failures = 0;
                                attached_at = Some(Instant::now());
                                info!("uprobe attached pids={targets:?} symbols={symbols}");
                            }
                            Err(e) => {
                                attach_failures += 1;
                                warn!(
                                    "uprobe attach pids={targets:?} failed ({attach_failures}/{}): {e}",
                                    uprobe::MAX_ATTACH_FAILURES
                                );
                                // Retry on the next scan instead of waiting for a focus change.
                                targets.clear();
                                if attach_failures >= uprobe::MAX_ATTACH_FAILURES {
                                    warn!("uprobe source unusable, giving up");
                                    return healthy;
                                }
                            }
                        }
                    }
                }
            }
        }

        let mut seen = false;
        probe.drain(|ktime| {
            if last_ktime != 0 && ktime.saturating_sub(last_ktime) < uprobe::MIN_GAP_NS {
                return;
            }
            last_ktime = ktime;
            frames += 1;
            seen = true;
        });

        if seen && !healthy {
            healthy = true;
            info!("uprobe fps source is publishing to {FPS_FILE}");
        }

        let elapsed = window.elapsed();
        if elapsed >= SF_INTERVAL {
            if healthy && active() {
                let fps = (frames as f32 / elapsed.as_secs_f32()).round() as i64;
                let _ = publish(fps);
            }
            frames = 0;
            window = Instant::now();
        }

        if !healthy
            && attached_at.is_some_and(|started| started.elapsed() >= CLIENT_STARTUP_TIMEOUT)
        {
            warn!(
                "uprobe attached but produced no frames in {}s, giving up",
                CLIENT_STARTUP_TIMEOUT.as_secs()
            );
            return false;
        }

        thread::sleep(uprobe::POLL_INTERVAL);
    }
}

/// The uprobe source needs aya, which has no 32-bit Android support.
#[cfg(not(target_pointer_width = "64"))]
fn run_uprobe(_forced: Option<Method>) -> bool {
    false
}

/// eBPF side of the uprobe source. aya does the loading; this wrapper only owns the object, the
/// links for the current pid and the ring buffer.
#[cfg(target_pointer_width = "64")]
mod uprobe {
    use std::convert::TryInto;
    use std::fs;
    use std::time::Duration;

    use aya::maps::{MapData, RingBuf};
    use aya::programs::uprobe::UProbeLinkId;
    use aya::programs::UProbe;
    use aya::Ebpf;

    use crate::log::debug;

    /// Embedded copy of `assets/fps_probe.bpf.o`, see the C source next to it. The parser needs
    /// the ELF header aligned, and the linker does not guarantee that for a plain `include_bytes!`.
    const OBJECT: &[u8] = aya::include_bytes_aligned!("../assets/fps_probe.bpf.o");
    const PROGRAM: &str = "fps_probe";
    const MAP: &str = "RING_BUF";
    const LIBGUI: &str = "/system/lib64/libgui.so";
    /// One `ktime_ns`.
    const RECORD_SIZE: usize = 8;

    /// Every Surface entry point a producer can take across Android releases. They are all
    /// attached at once because the name that actually runs depends on the app (EGL and HWUI
    /// take `queueBuffer`, the NDK takes `hook_queueBuffer`, ...). A frame that travels through
    /// more than one of them is collapsed by the caller's timestamp gate.
    const SYMBOLS: &[&str] = &[
        // Shared tail of both paths on Android 12+.
        "_ZN7android7Surface20onBufferQueuedLockedEiNS_2spINS_5FenceEEERKNS_22IGraphicBufferProducer17QueueBufferOutputE",
        // EGL / HWUI / Vulkan producers.
        "_ZN7android7Surface11queueBufferEONS_2spINS_13GraphicBufferEEEiPNS_24SurfaceQueueBufferOutputE",
        "_ZN7android7Surface11queueBufferEONS_2spINS_13GraphicBufferEEEi",
        // ANativeWindow producers.
        "_ZN7android7Surface16hook_queueBufferEP13ANativeWindowP19ANativeWindowBufferi",
        "_ZN7android7Surface19queueBufferInternalEP13ANativeWindowP19ANativeWindowBufferi",
        // Legacy direct entry points.
        "_ZN7android7Surface11queueBufferEP19ANativeWindowBufferi",
        "_ZN7android7Surface11queueBufferEP19ANativeWindowBufferiPNS_24SurfaceQueueBufferOutputE",
    ];

    /// Pause between ring buffer drains.
    pub(super) const POLL_INTERVAL: Duration = Duration::from_millis(20);
    /// Re-read the focused app at most this often.
    pub(super) const FOCUS_INTERVAL: Duration = Duration::from_secs(1);
    /// Rescan the focused package's processes at most this often, so render processes that
    /// spawn after the activity are picked up.
    pub(super) const TARGET_REFRESH: Duration = Duration::from_secs(5);
    /// Upper bound on the processes of one app we attach to.
    const MAX_PIDS: usize = 8;
    /// Records closer than this belong to the same frame: one queueBuffer can pass through
    /// several attached symbols, and no display runs above 1000 Hz.
    pub(super) const MIN_GAP_NS: u64 = 1_000_000;
    /// Consecutive attach failures before the source is declared unusable.
    pub(super) const MAX_ATTACH_FAILURES: u32 = 3;

    /// Loaded object, ring buffer and the links for the processes being watched.
    pub(super) struct FrameProbe {
        bpf: Ebpf,
        ring: RingBuf<MapData>,
        links: Vec<UProbeLinkId>,
        loaded: bool,
    }

    impl FrameProbe {
        pub(super) fn new() -> Result<Self, String> {
            bump_memlock();
            let mut bpf =
                Ebpf::load(OBJECT).map_err(|e| format!("cannot load the bpf object: {e}"))?;
            let ring = bpf
                .take_map(MAP)
                .ok_or_else(|| format!("bpf object has no {MAP} map"))?
                .try_into()
                .map_err(|e| format!("cannot open {MAP}: {e}"))?;
            Ok(Self { bpf, ring, links: Vec::new(), loaded: false })
        }

        /// Attach to every symbol this libgui has, for every process of the focused app.
        /// Returns the number of links created; an empty set means the combination cannot be
        /// probed.
        pub(super) fn attach(&mut self, pids: &[i32]) -> Result<usize, String> {
            let program: &mut UProbe = self
                .bpf
                .program_mut(PROGRAM)
                .ok_or_else(|| format!("bpf object has no {PROGRAM} program"))?
                .try_into()
                .map_err(|e| format!("{PROGRAM} is not a uprobe program: {e}"))?;
            if !self.loaded {
                program.load().map_err(|e| format!("cannot load {PROGRAM}: {e}"))?;
                self.loaded = true;
            }

            let mut links = Vec::new();
            for pid in pids {
                for symbol in SYMBOLS {
                    match program.attach(Some(symbol), 0, LIBGUI, Some(*pid)) {
                        Ok(id) => links.push(id),
                        Err(e) => {
                            debug!("uprobe symbol unavailable pid={pid} symbol={symbol}: {e}");
                        }
                    }
                }
            }
            if links.is_empty() {
                return Err("no libgui symbol could be attached".to_string());
            }

            let count = links.len();
            self.links = links;
            Ok(count)
        }

        /// Drop the links for the processes that were being watched, keeping the object loaded.
        pub(super) fn detach(&mut self) {
            if self.links.is_empty() {
                return;
            }
            let program: Option<&mut UProbe> = self
                .bpf
                .program_mut(PROGRAM)
                .and_then(|program| program.try_into().ok());
            if let Some(program) = program {
                for id in self.links.drain(..) {
                    if let Err(e) = program.detach(id) {
                        debug!("cannot detach a uprobe link: {e}");
                    }
                }
            }
        }

        /// Drain every queued record, in kernel timestamp order.
        pub(super) fn drain(&mut self, mut on_frame: impl FnMut(u64)) {
            while let Some(item) = self.ring.next() {
                if item.len() < RECORD_SIZE {
                    continue;
                }
                let mut raw = [0u8; RECORD_SIZE];
                raw.copy_from_slice(&item[..RECORD_SIZE]);
                on_frame(u64::from_ne_bytes(raw));
            }
        }
    }

    /// Package, uid and pid of the focused app from our own status file.
    pub(super) fn focused_app() -> Option<(String, i32, i32)> {
        let text = fs::read_to_string(crate::STATUS_FILE).ok()?;
        let line = text.lines().find(|line| line.starts_with("focused_app "))?;
        let mut fields = line.split_whitespace();
        fields.next()?; // "focused_app"
        let package = fields.next()?;
        let pid: i32 = fields.next()?.parse().ok()?;
        let uid: i32 = fields.next()?.parse().ok()?;
        if package == "none" || package == "unknown" || pid <= 0 {
            return None;
        }
        Some((package.to_string(), uid, pid))
    }

    /// Every live process of the focused app, most important first. Games usually render in a
    /// `:<suffix>` process, not in the one that owns the activity.
    pub(super) fn target_pids(package: &str, uid: i32, focused: i32) -> Vec<i32> {
        let mut procs: Vec<_> = crate::procs::scan_apps()
            .into_iter()
            .filter(|proc_| proc_.uid == uid && proc_.pkg == package)
            .collect();
        procs.sort_by_key(|proc_| (proc_.adj, proc_.pid));

        let mut pids: Vec<i32> = procs.into_iter().take(MAX_PIDS).map(|proc_| proc_.pid).collect();
        if focused > 0 && !pids.contains(&focused) {
            pids.push(focused);
        }
        pids.sort_unstable();
        pids.dedup();
        pids
    }

    /// Older kernels account BPF maps against RLIMIT_MEMLOCK; raise it before loading.
    fn bump_memlock() {
        let unlimited =
            libc::rlimit { rlim_cur: libc::RLIM_INFINITY, rlim_max: libc::RLIM_INFINITY };
        // SAFETY: `unlimited` is a valid rlimit and setrlimit only reads it.
        unsafe {
            libc::setrlimit(libc::RLIMIT_MEMLOCK, &unlimited);
        }
    }

    #[cfg(test)]
    mod tests {
        #[test]
        fn object_is_aligned() {
            // aya refuses to parse an unaligned ELF header, and `include_bytes!` would only
            // guarantee alignment 1.
            assert_eq!(super::OBJECT.as_ptr() as usize % 32, 0);
        }
    }
}

// ---------------- helpers ----------------

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
