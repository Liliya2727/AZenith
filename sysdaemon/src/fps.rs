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
// one is active. The same loop also publishes cumulative per-frame intervals to
// <cfg>/API/current_frametime ("<epoch_ms> <gen> <source> <frames> <sum_us> <hist>", one
// line about once per second) for archdaemon's low1/low01/jank. windowmanager has no
// intervals and publishes source "none"; gate closed writes NO_ACTIVE_GAME to both files.
//
// <cfg>/debug/explicit_fps_mthd pins the source:
//
//   missing / empty          automatic: try uprobe, then surfaceflinger, then
//                            windowmanager (last resort, rate only), and stay on
//                            the last one that published
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

use std::collections::{BTreeMap, HashMap};
use std::fs;
use std::io::{self, BufReader};
use std::os::unix::fs::PermissionsExt;
use std::process::{Command, Stdio};
use std::sync::{Condvar, Mutex, MutexGuard};
use std::thread;
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

use crate::codes::RESOLVER_APK;
use crate::inotify;
use crate::log::{debug, error, info, warn};

/// Staged copy of the dex the client runs from. The resolver's own copy under /data/local/tmp is
/// deleted after every transaction-code refresh, so this one has to be separate.
const FPS_CLIENT_APK: &str = "/data/adb/.config/AZenith/fpsclient.apk";
/// "<fps> <epoch_ms>", read by the manager's live monitor. Holds `NO_GAME_MARKER` while the
/// gate is closed.
pub(crate) const FPS_FILE: &str = "/data/adb/.config/AZenith/API/current_fps";
/// "<epoch_ms> <gen> <source> <frames> <sum_us> <hist>", cumulative per-frame intervals
/// for archdaemon. Holds `NO_GAME_MARKER` while the gate is closed (same pattern). The
/// `windowmanager` source publishes source `none` with empty counters (rate only).
pub(crate) const FT_FILE: &str = "/data/adb/.config/AZenith/API/current_frametime";
/// Directory `FPS_FILE` lives in; the surfaceflinger source may be the first writer.
const FPS_DIR: &str = "/data/adb/.config/AZenith/API";
/// "<package> <pid> <uid>" while a game runs, "NULL 0 0" otherwise. Watched with inotify.
const GAMEINFO_FILE: &str = "/data/adb/.config/AZenith/API/gameinfo";
/// "true" keeps the fps counter on without a game. Watched with inotify.
const ALWAYS_ON_FLAG: &str = "/data/adb/.config/AZenith/debug/fps_counter_alwayson";
/// Content of `FPS_FILE` while no game runs and always-on is off.
pub(crate) const NO_GAME_MARKER: &str = "NO_ACTIVE_GAME";
/// Bucket width for the frametime histogram, in microseconds (contract).
const FT_BUCKET_US: u64 = 100;
/// Overflow bucket index: intervals >= 500 ms land here.
const FT_OVERFLOW_IDX: usize = 5000;
/// Number of histogram buckets (0..=5000).
const FT_HIST_LEN: usize = 5001;
/// Intervals at or above this many microseconds go to the overflow bucket.
const FT_OVERFLOW_US: u64 = 500_000;
/// Column index of `actualPresentTime` in `dumpsys SurfaceFlinger --latency` rows
/// (8 int64/int columns: frameIdx start end deadline expected actual ready wait).
/// UNVERIFIED on this device (dumpsys needs root, unavailable in the build
/// shell); the parser logs the first raw row parsed so one on-device run
/// confirms or corrects this value.
const SF_LATENCY_ACTUAL_COL: usize = 5;
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
/// Publish interval of the polling sources. Kept below archdaemon's 1 s sample cadence: a 1.0 s
/// publisher (about 1.01 s with loop overhead) beats against the 1.0 s consumer and one tick every
/// ~95 s sees no new snapshot. The manager treats a stamp older than 3 s as 0.
const PUBLISH_INTERVAL: Duration = Duration::from_millis(800);
/// Warn when two consecutive frametime writes are farther apart than this.
/// The gap is measured on the monotonic clock, so device suspend does not count as a gap.
const PUBLISH_GAP_WARN_MS: u128 = 1500;
/// At most one publish-gap warning per this interval, so one long stall cannot flood the log.
const PUBLISH_GAP_WARN_COOLDOWN: Duration = Duration::from_secs(30);
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
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
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
    /// Frametime-capable sources first: uprobe (100us per-surface), then
    /// surfaceflinger (1ms global via presentToPresent), then windowmanager
    /// as last resort (rate only, publishes source "none").
    fn first_auto() -> Self {
        if UPROBE_SUPPORTED {
            Method::Uprobe
        } else {
            Method::SurfaceFlinger
        }
    }

    /// Next source to try in automatic mode, in order of preference:
    /// uprobe -> surfaceflinger -> windowmanager -> uprobe (uprobe skipped
    /// on 32-bit builds where aya is unavailable).
    fn next_auto(self) -> Self {
        match self {
            Method::Uprobe => Method::SurfaceFlinger,
            Method::SurfaceFlinger => Method::WindowManager,
            Method::WindowManager if UPROBE_SUPPORTED => Method::Uprobe,
            Method::WindowManager => Method::SurfaceFlinger,
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

/// Replace current_frametime with the same no-game marker (archdaemon parses it as
/// missing and writes -1 rows). Never leave a stale numeric line that looks live.
fn mark_frametime_idle() {
    let _ = fs::create_dir_all(FPS_DIR);
    let _ = crate::state::write_atomic(FT_FILE, &format!("{NO_GAME_MARKER}\n"));
}

// ---------------- frametime accumulator ----------------
//
// One cumulative histogram for the active source (contract: 5001 x u64 buckets,
// bucket width 100 us, overflow idx 5000, hist-sum == frames, never decreases
// within a gen). Owned by the supervisor thread and shared across its run_*
// calls so it survives source changes; begin_source() bumps gen and zeroes it
// on every (re)start/switch so archdaemon's gen-diff guard rebases cleanly.
// gen is seeded from wall-clock ms at daemon start, so a daemon restart can
// never reuse a gen archdaemon already saw.

struct FrameTimeInner {
    gen_id: u64,
    frames: u64,
    sum_us: u64,
    hist: [u64; FT_HIST_LEN],
    /// Monotonic timestamp of the last `current_frametime` write, for the
    /// publish-gap warning (None before the first write). `Instant` is
    /// CLOCK_MONOTONIC, which stops while the device is suspended: a sleeping
    /// device is not a stalled publisher and must not be reported as one.
    last_publish: Option<Instant>,
    /// Monotonic timestamp of the last emitted gap warning, for rate limiting.
    last_gap_warn: Option<Instant>,
}

/// Shared frametime publisher. Interior mutex keeps every increment and each
/// published line consistent (single lock per snapshot); publishing is one
/// atomic file write per ~1 s on the supervisor thread only.
struct FrameTime {
    inner: Mutex<FrameTimeInner>,
}

/// Bucket holding `interval_us` (0-99 us -> 0, ..., >= 500_000 us -> overflow).
fn ft_bucket_of(interval_us: u64) -> usize {
    if interval_us >= FT_OVERFLOW_US {
        FT_OVERFLOW_IDX
    } else {
        ((interval_us / FT_BUCKET_US) as usize).min(FT_OVERFLOW_IDX)
    }
}

/// Sparse serialization: ascending non-zero "idx:count" pairs, "-" when empty.
fn ft_serialize_hist(hist: &[u64]) -> String {
    let mut out = String::new();
    for (i, c) in hist.iter().enumerate() {
        if *c == 0 {
            continue;
        }
        if !out.is_empty() {
            out.push(',');
        }
        out.push_str(&i.to_string());
        out.push(':');
        out.push_str(&c.to_string());
    }
    if out.is_empty() {
        "-".to_string()
    } else {
        out
    }
}

/// One contracted line: "<epoch_ms> <gen> <source> <frames> <sum_us> <hist>".
fn ft_format_line(
    epoch_ms: u128,
    gen_id: u64,
    source: &str,
    frames: u64,
    sum_us: u64,
    hist: &[u64],
) -> String {
    format!(
        "{} {} {} {} {} {}\n",
        epoch_ms, gen_id, source, frames, sum_us, ft_serialize_hist(hist)
    )
}

/// Advance the publish stamp and return the gap worth warning about, if any.
///
/// The stamp always moves; the warning is suppressed below `PUBLISH_GAP_WARN_MS` and by
/// `PUBLISH_GAP_WARN_COOLDOWN`. `Instant` (CLOCK_MONOTONIC) deliberately excludes suspend
/// time, so a device that slept is not mistaken for a publisher that stalled.
fn note_publish(g: &mut FrameTimeInner, now: Instant) -> Option<u128> {
    let gap = g
        .last_publish
        .map(|last| now.duration_since(last).as_millis());
    g.last_publish = Some(now);
    let gap = gap?;
    if gap <= PUBLISH_GAP_WARN_MS {
        return None;
    }
    let allowed = match g.last_gap_warn {
        Some(last) if now.duration_since(last) < PUBLISH_GAP_WARN_COOLDOWN => false,
        _ => {
            g.last_gap_warn = Some(now);
            true
        }
    };
    allowed.then_some(gap)
}

impl FrameTime {
    fn new() -> Self {
        FrameTime {
            inner: Mutex::new(FrameTimeInner {
                gen_id: now_ms() as u64,
                frames: 0,
                sum_us: 0,
                hist: [0; FT_HIST_LEN],
                last_publish: None,
                last_gap_warn: None,
            }),
        }
    }

    /// Start a source: monotonic gen bump, counters zeroed, fresh empty line
    /// published immediately so archdaemon sees the new gen before the next
    /// 1 s tick. One lock for snapshot, one atomic write.
    fn begin_source(&self, source: &str, reason: &str) {
        let (gen_id, line, gap_ms) = {
            let mut g = self.inner.lock().unwrap_or_else(|e| e.into_inner());
            g.gen_id = g.gen_id.wrapping_add(1);
            if g.gen_id == 0 {
                g.gen_id = 1;
            }
            g.frames = 0;
            g.sum_us = 0;
            g.hist = [0; FT_HIST_LEN];
            let gen_id = g.gen_id;
            let now = Instant::now();
            let gap_ms = note_publish(&mut g, now);
            let line = ft_format_line(now_ms(), gen_id, source, g.frames, g.sum_us, &g.hist);
            (gen_id, line, gap_ms)
        };
        let _ = fs::create_dir_all(FPS_DIR);
        let _ = crate::state::write_atomic(FT_FILE, &line);
        if let Some(gap) = gap_ms {
            warn!("frametime publish gap={}ms source={source} reason={reason}", gap);
        }
        info!("frametime gen bump source={source} gen={gen_id} reason={reason}");
    }

    /// Record one interval in microseconds (0 allowed -> bucket 0).
    fn add_interval_us(&self, interval_us: u64) {
        let mut g = self.inner.lock().unwrap_or_else(|e| e.into_inner());
        let b = ft_bucket_of(interval_us);
        g.hist[b] += 1;
        g.frames += 1;
        g.sum_us += interval_us;
    }

    /// Bulk variant for histogram deltas (SF timestats): `count` intervals
    /// of the same `interval_us` under one lock.
    fn add_interval_us_bulk(&self, interval_us: u64, count: u64) {
        if count == 0 {
            return;
        }
        let mut g = self.inner.lock().unwrap_or_else(|e| e.into_inner());
        let b = ft_bucket_of(interval_us);
        g.hist[b] += count;
        g.frames += count;
        g.sum_us += interval_us * count;
    }

    /// Write one consistent snapshot line for `source` (uprobe/sf/none).
    fn publish_source(&self, source: &str) {
        let (line, gap_ms) = {
            let mut g = self.inner.lock().unwrap_or_else(|e| e.into_inner());
            let now = Instant::now();
            let gap_ms = note_publish(&mut g, now);
            let line = ft_format_line(now_ms(), g.gen_id, source, g.frames, g.sum_us, &g.hist);
            (line, gap_ms)
        };
        let _ = crate::state::write_atomic(FT_FILE, &line);
        if let Some(gap) = gap_ms {
            warn!("frametime publish gap={}ms source={source}", gap);
        }
    }
}

/// Parse `dumpsys SurfaceFlinger --latency "<layer>"` output into
/// `actualPresentTime` values (ns) in row order, dropping sentinels
/// (`0`, `i64::MAX`) and non-numeric header lines. Rows carrying fewer
/// than `SF_LATENCY_ACTUAL_COL + 1` integers are skipped. See the const
/// for why the column index still needs one on-device confirmation.
fn parse_sf_latency(output: &str) -> Vec<i64> {
    let mut values = Vec::new();
    for line in output.lines() {
        let mut cols: Vec<i64> = Vec::new();
        let mut all_numeric = true;
        for tok in line.split_whitespace() {
            match tok.parse::<i64>() {
                Ok(v) => cols.push(v),
                Err(_) => {
                    all_numeric = false;
                    break;
                }
            }
        }
        if !all_numeric || cols.len() <= SF_LATENCY_ACTUAL_COL {
            continue;
        }
        let actual = cols[SF_LATENCY_ACTUAL_COL];
        if actual == 0 || actual == i64::MAX {
            continue;
        }
        values.push(actual);
    }
    values
}

/// De-duplicates `--latency` polls by timestamp and emits intervals.
/// Each call is one poll (up to 127 rows); only timestamps greater than
/// the greatest seen so far are new. The very first poll of a layer only
/// establishes state (no interval into history). A fully-new window (no
/// timestamp overlaps the previous window) drops the boundary interval
/// (likely dropped frames between polls) instead of bridging a fake gap.
struct SfLatReducer {
    last_max: Option<i64>,
}

impl SfLatReducer {
    fn new() -> Self {
        SfLatReducer { last_max: None }
    }

    /// Emit intervals for `values` (already sentinel-filtered, in row order).
    fn ingest(&mut self, values: &[i64], acc: &FrameTime) {
        if values.is_empty() {
            return;
        }
        let Some(last_max) = self.last_max else {
            // First poll: state only, no interval into pre-gen history.
            self.last_max = values.iter().max().copied();
            return;
        };
        let accepted: Vec<i64> = values.iter().copied().filter(|v| *v > last_max).collect();
        if accepted.is_empty() {
            return;
        }
        // Fully-new window: every returned row is newer than last_max,
        // i.e. no overlap with the previous poll. Drop the cross-poll
        // gap (unseen frames) and only diff within this window.
        let fully_new = accepted.len() == values.len();
        let mut prev = if fully_new { accepted[0] } else { last_max };
        let skip = if fully_new { 1 } else { 0 };
        for &v in &accepted[skip..] {
            if v > prev {
                acc.add_interval_us(((v - prev) / 1000).max(0) as u64);
            }
            prev = v;
        }
        self.last_max = accepted.iter().max().copied().or(self.last_max);
    }
}

/// Parse the `presentToPresent histogram is as below:` section of
/// `dumpsys SurfaceFlinger --timestats -dump` into `(ms, count)` pairs.
///
/// On this device `--latency "<layer>"` only returns the frame period
/// (`8333333`) instead of timestamped rows, so the latency path above
/// stays empty. This histogram carries *real* present-to-present
/// intervals (1 ms bins, global, cumulative since `statsStart`) which
/// we diff per poll into the same accumulator contract as uprobe, at
/// lower resolution. Bin centre is used (`ms*1000+500` us) instead of
/// the bin edge, so average bias stays within half a bin.
fn parse_present_hist(dump: &str) -> Vec<(u32, u64)> {
    let mut in_section = false;
    let mut out = Vec::new();
    for line in dump.lines() {
        let t = line.trim();
        if t.starts_with("presentToPresent histogram is as below:") {
            in_section = true;
            continue;
        }
        if in_section {
            // Section ends at the first line after it that is not a
            // "Nms=M" token list (e.g. "averageFrameDuration …").
            let mut any = false;
            for tok in t.split_whitespace() {
                let Some((ms, count)) = tok.split_once('=') else {
                    continue;
                };
                let Some(ms) = ms.strip_suffix("ms") else { continue };
                let (Ok(ms), Ok(count)) =
                    (ms.trim().parse::<u32>(), count.trim().parse::<u64>())
                else {
                    continue;
                };
                out.push((ms, count));
                any = true;
            }
            if !any && !t.is_empty() {
                in_section = false;
            }
        }
    }
    out
}

/// Differences successive timestats `presentToPresent` snapshots.
/// The histogram is cumulative since `statsStart`, so only the delta
/// between polls is new data; the very first poll only sets the
/// baseline (no interval into pre-run history). A count that ever
/// decreases means the stats window reset — re-baseline silently and
/// emit nothing for that tick (never a negative interval).
struct SfHistDiffer {
    prev: Option<BTreeMap<u32, u64>>,
}

impl SfHistDiffer {
    fn new() -> Self {
        SfHistDiffer { prev: None }
    }

    /// Feed one poll's histogram; emits intervals for new frames only.
    fn ingest(&mut self, hist: &[(u32, u64)], acc: &FrameTime) {
        if hist.is_empty() {
            return;
        }
        let cur: BTreeMap<u32, u64> = hist.iter().copied().collect();
        let Some(prev) = self.prev.clone() else {
            self.prev = Some(cur);
            return;
        };
        // Reset detection: any bin went backwards -> whole window reset.
        let reset = cur.iter().any(|(ms, &c)| prev.get(ms).is_some_and(|p| c < *p));
        if reset {
            self.prev = Some(cur);
            return;
        }
        for (ms, &cnt) in &cur {
            let delta = cnt.saturating_sub(prev.get(ms).copied().unwrap_or(0));
            if delta == 0 {
                continue;
            }
            let us = (*ms as u64) * 1000 + 500;
            acc.add_interval_us_bulk(us, delta);
        }
        self.prev = Some(cur);
    }
}

/// Per-surface uprobe stream tracker (pure, kernel-free so it is
/// unit-testable). Events are `queueBuffer` calls keyed by the Surface
/// `this` pointer (BPF x0; 0 = ctx read unavailable -> one global
/// stream). Only the busiest surface of the previous 1 s window is the
/// active stream: `on_event` counts every event per key, but intervals
/// and the fps window count only active-stream events. The first event
/// of any (re)selected stream yields no interval (reset, never bridge).
struct StreamTracker {
    counts: HashMap<u64, u64>,
    active: Option<u64>,
    last_ktime: HashMap<u64, u64>,
    window_frames: u32,
    /// Min gap collapsing double dispatch of one frame through several
    /// attached symbols (no display runs above 1000 Hz).
    min_gap_ns: u64,
}

impl StreamTracker {
    fn new() -> Self {
        StreamTracker {
            counts: HashMap::new(),
            active: None,
            last_ktime: HashMap::new(),
            window_frames: 0,
            min_gap_ns: 1_000_000,
        }
    }

    /// Full reset (attach, focus/pid change, gate gap).
    fn reset(&mut self) {
        *self = StreamTracker::new();
    }

    /// Feed one record. Returns the interval in microseconds when this
    /// event produced one (active stream, not first, not collapsed),
    /// `None` otherwise.
    fn on_event(&mut self, ktime_ns: u64, key: u64) -> Option<u64> {
        // Collapse same-frame double dispatch (one queueBuffer passing
        // through several attached symbols): < 1 ms after the previous
        // event *of that same stream* is the same frame.
        if let Some(prev) = self.last_ktime.get(&key) {
            if ktime_ns.saturating_sub(*prev) < self.min_gap_ns {
                return None;
            }
        }
        let prev = self.last_ktime.insert(key, ktime_ns);
        *self.counts.entry(key).or_insert(0) += 1;
        if self.active != Some(key) {
            return None;
        }
        self.window_frames += 1;
        // First frame of the (re)selected stream yields no interval.
        let prev = prev?;
        if ktime_ns < prev {
            return None;
        }
        Some((ktime_ns - prev) / 1_000)
    }

    /// End a 1 s window: return its fps event count, pick the busiest
    /// surface as the next window's active stream, clear window counts.
    /// Changing the active stream resets its timestamp history so no
    /// interval bridges across streams.
    fn close_window(&mut self) -> u32 {
        let fps = self.window_frames;
        self.window_frames = 0;
        let next = self.counts.iter().max_by_key(|(_, c)| **c).map(|(k, _)| *k);
        if next != self.active {
            if let Some(k) = next {
                self.last_ktime.remove(&k);
            }
        }
        self.active = next;
        self.counts.clear();
        fps
    }
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
    // One accumulator for the supervisor's lifetime so gen survives restarts
    // of individual sources; begin_source() bumps it per (re)start.
    let frametime = FrameTime::new();

    loop {
        // Gate: without a game (and without always-on) there is no source, and current_fps
        // holds NO_ACTIVE_GAME.
        if !active() {
            mark_idle();
            mark_frametime_idle();
            info!("no game running, fps source idle");
            wait_for_active();
            info!("fps source wanted, starting");
        }

        let forced = explicit_method();
        let method = forced.unwrap_or(auto);

        let healthy = match method {
            Method::WindowManager => run_windmanager(forced, &frametime),
            Method::Uprobe => run_uprobe(forced, &frametime),
            Method::SurfaceFlinger => run_surfaceflinger(forced, &frametime),
        };

        // The gate closed: not a source failure, so no fallback and no retry delay. The source
        // is fully stopped by now, nothing can write current_fps behind our back.
        if !active() {
            mark_idle();
            mark_frametime_idle();
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
fn run_windmanager(forced: Option<Method>, frametime: &FrameTime) -> bool {
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

    // windowmanager pushes a rate only (registerTaskFpsCallback), never per-frame
    // timestamps: publish the "none" line, never fabricate intervals from fps.
    frametime.begin_source("none", "windowmanager_start");
    info!("frametime: windowmanager publishes no intervals (source=none)");

    let mut healthy = false;
    let mut last_flag_check = Instant::now();
    let mut last_ft_publish = Instant::now();

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

        // Keep the "none" line fresh for archdaemon's 3 s staleness window.
        if last_ft_publish.elapsed() >= PUBLISH_INTERVAL {
            last_ft_publish = Instant::now();
            if active() {
                frametime.publish_source("none");
            }
        }

        nap(POLL_INTERVAL);
    }
}

/// Sample SurfaceFlinger's cumulative frame counter. Returns `false` when `dumpsys` never yields
/// a usable counter for `SF_MAX_MISSES` consecutive ticks.
fn run_surfaceflinger(forced: Option<Method>, frametime: &FrameTime) -> bool {
    let _ = fs::create_dir_all(FPS_DIR);

    info!("fps source starting method=surfaceflinger out={FPS_FILE}");
    sf_timestats("-enable");
    frametime.begin_source("sf", "sf_start");

    let mut prev: Option<(u64, Instant)> = None;
    let mut healthy = false;
    let mut misses = 0u32;
    let mut last_flag_check = Instant::now();
    let mut lat = SfLatReducer::new();
    let mut lat_layer: Option<String> = None;
    let mut lat_pkg: Option<String> = None;
    let mut lat_warned = false;
    let mut latency_raw_logged = false;
    let mut hist = SfHistDiffer::new();
    let mut hist_warned = false;

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

        match read_sf_timestats() {
            Some(st) => {
                misses = 0;
                let total = st.total;
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

                // ---- frametime fallback: presentToPresent histogram ----
                // Preferred when --latency yields nothing (as on this ROM,
                // where --latency only returns the period "8333333").
                // The histogram is cumulative since statsStart, so the
                // differ emits only new frames; fps/total already proven.
                if !st.present.is_empty() {
                    // Use histogram only when latency is unavailable/empty
                    // this tick, to avoid double counting on ROMs where
                    // --latency actually works.
                    let pkg = read_game_package();
                    if pkg != lat_pkg {
                        lat_pkg = pkg.clone();
                        lat_layer = pkg.as_deref().and_then(resolve_sf_layer);
                        lat = SfLatReducer::new();
                        if let Some(layer) = lat_layer.clone() {
                            info!("frametime: sf latency layer={layer}");
                        } else {
                            debug!("frametime: sf latency using global --latency (layer unresolved)");
                        }
                    }
                    let lat_values: Vec<i64> = read_sf_latency(lat_layer.as_deref())
                        .map(|raw| {
                            if !latency_raw_logged {
                                if let Some(first) = raw.lines().next() {
                                    debug!("frametime: sf latency raw first row: {first}");
                                }
                                latency_raw_logged = true;
                            }
                            parse_sf_latency(&raw)
                        })
                        .unwrap_or_default();
                    if !lat_values.is_empty() {
                        lat.ingest(&lat_values, frametime);
                    } else if !lat_warned {
                        warn!("frametime: sf --latency empty (period-only ROM); using presentToPresent histogram (1ms bins, global)");
                        lat_warned = true;
                    }
                    if lat_values.is_empty() {
                        hist.ingest(&st.present, frametime);
                        if !hist_warned {
                            debug!("frametime: sf present histogram buckets={}", st.present.len());
                            hist_warned = true;
                        }
                    }
                    if active() {
                        frametime.publish_source("sf");
                    }
                } else if active() {
                    // No histogram yet (early enable or dump without the
                    // section): still keep the line fresh for archdaemon.
                    if !hist_warned {
                        warn!("frametime: sf timestats presentToPresent histogram empty");
                        hist_warned = true;
                    }
                    frametime.publish_source("sf");
                }
            }
            None => {
                misses += 1;
                warn!("surfaceflinger timestats unavailable ({misses}/{SF_MAX_MISSES})");
                if misses >= SF_MAX_MISSES {
                    return healthy;
                }
            }
        }

        nap(PUBLISH_INTERVAL);
    }
}

/// Count frames from the uprobe ring. Returns `false` when the kernel/libgui cannot be probed
/// (or when the first attach never produces a frame), so automatic mode can fall back.
#[cfg(target_pointer_width = "64")]
fn run_uprobe(forced: Option<Method>, frametime: &FrameTime) -> bool {
    let mut probe = match uprobe::FrameProbe::new() {
        Ok(probe) => probe,
        Err(e) => {
            warn!("uprobe fps source unavailable: {e}");
            return false;
        }
    };

    info!("fps source starting method=uprobe out={FPS_FILE}");
    let _ = fs::create_dir_all(FPS_DIR);
    frametime.begin_source("uprobe", "uprobe_start");

    let mut focused: Option<(String, i32, i32)> = None;
    let mut targets: Vec<i32> = Vec::new();
    let mut attached_at: Option<Instant> = None;
    let mut attach_failures = 0u32;
    let mut tracked = StreamTracker::new();
    let mut window = Instant::now();
    let mut healthy = false;
    let mut last_focus = Instant::now();
    let mut last_scan = Instant::now();
    let mut last_flag_check = Instant::now();

    loop {
        let iter_t = Instant::now();
        let mut scan_ms = 0u128;
        let mut reattach_ms = 0u128;
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
                let scan_t = Instant::now();
                let pids = match &focused {
                    Some((package, uid, pid)) => uprobe::target_pids(package, *uid, *pid),
                    None => Vec::new(),
                };
                scan_ms = scan_t.elapsed().as_millis();

                if changed || pids != targets {
                    let reattach_t = Instant::now();
                    probe.detach();
                    // Records still queued belong to the process that just went away.
                    probe.drain(|_, _| {});

                    targets = pids;
                    attached_at = None;
                    tracked.reset();
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
                    reattach_ms = reattach_t.elapsed().as_millis();
                }
            }
        }

        let drain_t = Instant::now();
        let mut seen = false;
        probe.drain(|ktime, key| {
            seen = true;
            if let Some(us) = tracked.on_event(ktime, key) {
                frametime.add_interval_us(us);
            }
        });
        let drain_ms = drain_t.elapsed().as_millis();

        if seen && !healthy {
            healthy = true;
            info!("uprobe fps source is publishing to {FPS_FILE}");
        }

        let elapsed = window.elapsed();
        if elapsed >= PUBLISH_INTERVAL {
            let fps_frames = tracked.close_window();
            let pub_t = Instant::now();
            if healthy && active() {
                let fps = (fps_frames as f32 / elapsed.as_secs_f32()).round() as i64;
                let _ = publish(fps);
                frametime.publish_source("uprobe");
            }
            let pub_ms = pub_t.elapsed().as_millis();
            if pub_ms > 200 {
                warn!("uprobe slow publish={pub_ms}ms");
            }
            window = Instant::now();
        }
        let iter_ms = iter_t.elapsed().as_millis();
        if iter_ms > 300 {
            warn!("uprobe slow iter={iter_ms}ms scan={scan_ms}ms reattach={reattach_ms}ms drain={drain_ms}ms");
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
fn run_uprobe(_forced: Option<Method>, _frametime: &FrameTime) -> bool {
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
    /// One `frame_record` from `assets/fps_probe.bpf.c`:
    /// `ktime_ns:u64, surface_key:u64, tgid:u32, pad:u32`.
    const RECORD_SIZE: usize = 24;

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
        /// Callback gets `(ktime_ns, surface_key)`; `surface_key` isolates
        /// one presenting stream (0 = ctx unavailable -> global fallback).
        pub(super) fn drain(&mut self, mut on_frame: impl FnMut(u64, u64)) {
            while let Some(item) = self.ring.next() {
                if item.len() < RECORD_SIZE {
                    continue;
                }
                let mut raw = [0u8; RECORD_SIZE];
                raw.copy_from_slice(&item[..RECORD_SIZE]);
                let ktime = u64::from_ne_bytes(raw[0..8].try_into().unwrap());
                let key = u64::from_ne_bytes(raw[8..16].try_into().unwrap());
                on_frame(ktime, key);
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

/// Focused game package from gameinfo (first token of the first line, NULL ->
/// `None`). Used for SF layer resolution; works on all pointer widths unlike
/// the uprobe focus helper.
fn read_game_package() -> Option<String> {
    let raw = fs::read_to_string(GAMEINFO_FILE).ok()?;
    let pkg = raw.lines().next()?.split_whitespace().next()?;
    if pkg.eq_ignore_ascii_case("null") {
        None
    } else {
        Some(pkg.to_string())
    }
}

/// Find the focused app's SurfaceFlinger layer by `dumpsys SurfaceFlinger
/// --list`, returning the first layer line containing the package name.
/// The layer list is otherwise volatile (surface numbering per open), so it
/// must not be assumed; `None` falls back to a global `--latency` dump.
fn resolve_sf_layer(package: &str) -> Option<String> {
    let output = Command::new("dumpsys")
        .args(["SurfaceFlinger", "--list"])
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .output()
        .ok()?;
    let text = String::from_utf8_lossy(&output.stdout);
    text.lines()
        .map(str::trim)
        .find(|line| line.contains(package) && !line.is_empty())
        .map(str::to_string)
}

/// `--latency` for one layer, or global `--latency` when unresolved.
fn read_sf_latency(layer: Option<&str>) -> Option<String> {
    let mut cmd = Command::new("dumpsys");
    cmd.args(["SurfaceFlinger", "--latency"]);
    if let Some(layer) = layer {
        cmd.arg(layer);
    }
    let output = cmd.stdout(Stdio::piped()).stderr(Stdio::null()).output().ok()?;
    if !output.status.success() {
        return None;
    }
    Some(String::from_utf8_lossy(&output.stdout).to_string())
}

/// Cumulative frame counter + `presentToPresent` histogram from one
/// `dumpsys SurfaceFlinger --timestats -dump` call.
struct SfTimestats {
    total: u64,
    present: Vec<(u32, u64)>,
}

fn read_sf_timestats() -> Option<SfTimestats> {
    let mut child = Command::new("dumpsys")
        .args(["SurfaceFlinger", "--timestats", "-dump"])
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .spawn()
        .ok()?;

    let mut text = String::new();
    if let Some(out) = child.stdout.take() {
        use std::io::Read;
        let mut reader = BufReader::new(out);
        let _ = reader.read_to_string(&mut text);
    }
    let _ = child.wait();
    if text.is_empty() {
        return None;
    }
    let total = text
        .lines()
        .filter_map(|l| l.trim_start().strip_prefix(SF_TOTAL_FRAMES))
        .filter_map(|v| v.trim().parse::<u64>().ok())
        .next()?;
    Some(SfTimestats { total, present: parse_present_hist(&text) })
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
    crate::codes::apply_boot_cp_env(&mut c);
    c.env_remove("LD_LIBRARY_PATH").env_remove("LD_PRELOAD");
    c.env("ANDROID_ROOT", "/system").env("ANDROID_DATA", "/data");
    c.env("ANDROID_RUNTIME_ROOT", "/apex/com.android.runtime").env("ANDROID_ART_ROOT", "/apex/com.android.art");
    c.env("ANDROID_I18N_ROOT", "/apex/com.android.i18n").env("ANDROID_TZDATA_ROOT", "/apex/com.android.tzdata");
    c.arg(format!("-Djava.class.path={FPS_CLIENT_APK}"))
        .arg("/")
        .arg("--nice-name=sys.azenith-fpsclient")
        // MainKt is the entry the transaction resolver already proves loads
        // under app_process; it forwards --fps to FpsClient.main. Launching
        // the Kotlin `object` (FpsClient) directly makes app_process abort
        // before main runs, so the client never publishes.
        .arg("zx.binder.MainKt")
        .arg("--fps")
        .arg(format!("--out={FPS_FILE}"));
    // Append so crash output survives retries (truncating hid the cause of
    // the spawn -> SIGABRT loops).
    if let Ok(f) = std::fs::OpenOptions::new().create(true).append(true).open(FPS_LOG) {
        if let Ok(g) = f.try_clone() {
            c.stdout(g);
        }
        c.stderr(f);
    }
    c
}

fn stage_apk() -> bool {
    // Cheap sanity: the staged file must be a ZIP/APK (PK magic). If it is
    // empty or garbage, app_process dies during dex load with SIGABRT —
    // always rewrite it from the embedded copy in that case.
    if let Ok(v) = fs::read(FPS_CLIENT_APK) {
        if v == RESOLVER_APK && v.starts_with(b"PK") {
            return true;
        }
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

// ---------------- tests ----------------

#[cfg(test)]
mod tests {
    use super::*;

    fn new_acc() -> FrameTime {
        FrameTime::new()
    }

    fn snap_frames(acc: &FrameTime) -> (u64, u64, u64) {
        let g = acc.inner.lock().unwrap_or_else(|e| e.into_inner());
        (g.gen_id, g.frames, g.sum_us)
    }

    fn hist_sum(acc: &FrameTime) -> u64 {
        let g = acc.inner.lock().unwrap_or_else(|e| e.into_inner());
        g.hist.iter().sum()
    }

    /// Test-only inverse of `ft_serialize_hist` (archdaemon parses the
    /// same contract; kept here only for round-trip checks).
    fn parse_hist(s: &str) -> std::collections::BTreeMap<u32, u64> {
        let mut map = std::collections::BTreeMap::new();
        if s.trim() == "-" {
            return map;
        }
        for part in s.split(',') {
            let mut kv = part.split(':');
            let idx: u32 = kv.next().unwrap().parse().unwrap();
            let count: u64 = kv.next().unwrap().parse().unwrap();
            map.insert(idx, count);
        }
        map
    }

    #[test]
    fn bucket_boundaries() {
        assert_eq!(ft_bucket_of(0), 0);
        assert_eq!(ft_bucket_of(99), 0);
        assert_eq!(ft_bucket_of(100), 1);
        assert_eq!(ft_bucket_of(199), 1);
        assert_eq!(ft_bucket_of(200), 2);
        assert_eq!(ft_bucket_of(499_999), 4999);
        assert_eq!(ft_bucket_of(500_000), FT_OVERFLOW_IDX);
        assert_eq!(ft_bucket_of(u64::MAX), FT_OVERFLOW_IDX);
        // 120 Hz -> idx 83 (8300 us), 60 Hz -> idx 166 (16600 us).
        assert_eq!(ft_bucket_of(8_333), 83);
        assert_eq!(ft_bucket_of(16_667), 166);
    }

    #[test]
    fn overflow_and_zero() {
        let acc = new_acc();
        acc.add_interval_us(0);
        acc.add_interval_us(500_000);
        acc.add_interval_us(600_000);
        let g = acc.inner.lock().unwrap_or_else(|e| e.into_inner());
        assert_eq!(g.hist[0], 1);
        assert_eq!(g.hist[FT_OVERFLOW_IDX], 2);
        assert_eq!(g.sum_us, 1_100_000);
        assert_eq!(g.frames, 3);
        assert_eq!(g.hist.iter().sum::<u64>(), g.frames);
    }

    #[test]
    fn sparse_serialization_ascending_and_dash() {
        let mut hist = [0u64; FT_HIST_LEN];
        assert_eq!(ft_serialize_hist(&hist), "-");
        hist[166] = 5;
        hist[83] = 2;
        hist[FT_OVERFLOW_IDX] = 1;
        assert_eq!(ft_serialize_hist(&hist), "83:2,166:5,5000:1");
    }

    #[test]
    fn serialization_round_trip() {
        let mut hist = [0u64; FT_HIST_LEN];
        hist[1] = 7;
        hist[83] = 42;
        hist[FT_OVERFLOW_IDX] = 3;
        let back = parse_hist(&ft_serialize_hist(&hist));
        assert_eq!(back.len(), 3);
        assert_eq!(back[&1], 7);
        assert_eq!(back[&83], 42);
        assert_eq!(back[&5000], 3);
        assert!(parse_hist("-").is_empty());
    }

    #[test]
    fn line_contract_and_invariant() {
        let mut hist = [0u64; FT_HIST_LEN];
        hist[83] = 4;
        let line = ft_format_line(1_700_000_009_000, 77, "uprobe", 4, 4 * 8_333, &hist);
        assert_eq!(line, "1700000009000 77 uprobe 4 33332 83:4\n");
        // hist-sum == frames invariant under a mixed stream of intervals.
        let acc = new_acc();
        for us in [0u64, 100, 8_333, 16_667, 499_999, 500_000, 999_999] {
            acc.add_interval_us(us);
        }
        let (_, frames, _) = snap_frames(&acc);
        assert_eq!(frames, hist_sum(&acc));
    }

    #[test]
    fn gen_bump_resets_everything_without_writing() {
        // begin_source bumps gen and zeroes counters (it also attempts a file
        // write, which is best-effort and ignored here).
        let acc = new_acc();
        acc.add_interval_us(8_333);
        let (gen0, frames0, _) = snap_frames(&acc);
        assert_eq!(frames0, 1);
        acc.begin_source("sf", "test");
        let (gen1, frames1, sum1) = snap_frames(&acc);
        assert_eq!(gen1, gen0 + 1, "gen must be monotonic");
        assert_eq!((frames1, sum1), (0, 0));
        assert_eq!(hist_sum(&acc), 0);
        acc.begin_source("uprobe", "test");
        let (gen2, _, _) = snap_frames(&acc);
        assert_eq!(gen2, gen1 + 1);
    }

    #[test]
    fn sf_parser_sentinels_and_header() {
        let good = "100 0 0 0 0 8000 0 0";
        let out = format!(
            "Timestamp ... header line\n{good}\n0 0 0 0 0 0 0 0\n0 0 0 0 0 9223372036854775807 0 0\nnon-numeric header\n12345 0 0\n"
        );
        // Only first row has a usable actualPresentTime (col 5 == 8000).
        assert_eq!(parse_sf_latency(&out), vec![8000]);
    }

    #[test]
    fn sf_parser_empty_output() {
        assert!(parse_sf_latency("").is_empty());
        assert!(parse_sf_latency("Permission Denial: can't dump\n").is_empty());
    }

    #[test]
    fn sf_reducer_dedup_across_polls() {
        let acc = new_acc();
        let mut lat = SfLatReducer::new();
        // First poll: state only.
        lat.ingest(&[1_000_000, 2_000_000, 3_000_000], &acc);
        let (_, frames0, _) = snap_frames(&acc);
        assert_eq!(frames0, 0);
        // Second poll overlaps (2M,3M again) + one new: exactly one interval.
        lat.ingest(&[2_000_000, 3_000_000, 4_000_000], &acc);
        let (_, frames1, sum1) = snap_frames(&acc);
        assert_eq!(frames1, 1);
        assert_eq!(sum1, 1_000);
    }

    #[test]
    fn sf_reducer_fully_new_window_drops_bridge() {
        let acc = new_acc();
        let mut lat = SfLatReducer::new();
        lat.ingest(&[1_000_000, 2_000_000], &acc);
        // Fully new window far ahead: bridge from 2M must NOT be recorded;
        // only 10M->11M diffs inside the window count.
        lat.ingest(&[10_000_000, 11_000_000], &acc);
        let (_, frames, sum) = snap_frames(&acc);
        assert_eq!(frames, 1);
        assert_eq!(sum, 1_000, "must not bridge 2M -> 10M");
    }

    #[test]
    fn uprobe_tracker_busy_stream_and_intervals() {
        let mut t = StreamTracker::new();
        // Window 1: key A busier than key B; intervals not yet emitted
        // (no active stream), but counts accumulate.
        for i in 0..5 {
            assert!(t.on_event(1_000_000 + i * 16_700_000, 0xA).is_none());
        }
        for i in 0..2 {
            assert!(t.on_event(1_000_000 + i * 16_700_000, 0xB).is_none());
        }
        let fps_frames = t.close_window();
        // fps counts only the active stream of the *previous* window; on the
        // first window there is none, so 0, and A becomes active.
        assert_eq!(fps_frames, 0);
        // Window 2: intervals from A only; B events produce no interval.
        let us1 = t.on_event(1_000_000 + 100 * 16_700_000, 0xA);
        assert!(us1.is_none(), "first frame of (re)selected stream = no interval");
        let us2 = t.on_event(1_000_000 + 101 * 16_700_000, 0xA);
        assert_eq!(us2, Some(16_700));
        assert!(t.on_event(1_000_000 + 101 * 16_700_000 + 1_000, 0xB).is_none());
        assert_eq!(t.window_frames, 2);
    }

    #[test]
    fn uprobe_tracker_reset_on_pid_or_stream_change() {
        let mut t = StreamTracker::new();
        t.active = Some(0xA);
        t.on_event(10_000_000, 0xA); // first of A -> None
        assert_eq!(t.on_event(20_000_000, 0xA), Some(10_000));
        // Simulate pid/focus change: full reset must drop history.
        t.reset();
        assert!(t.on_event(30_000_000, 0xA).is_none());
        assert!(t.on_event(30_000_000 + 16_700_000, 0xB).is_none(), "inactive stream");
        // MIN_GAP collapse: two events < 1 ms apart on same stream = one frame.
        let mut t2 = StreamTracker::new();
        t2.active = Some(0xA);
        t2.on_event(1_000_000, 0xA);
        t2.on_event(1_000_500, 0xA);
        assert_eq!(t2.window_frames, 1, "collapsed duplicate must not count");
    }

    #[test]
    fn format_none_line() {
        let hist = [0u64; FT_HIST_LEN];
        let line = ft_format_line(1_700_000_009_000, 9, "none", 0, 0, &hist);
        assert_eq!(line, "1700000009000 9 none 0 0 -\n");
    }

    #[test]
    fn fallback_order_uprobe_sf_windowmanager() {
        // Order is uprobe -> surfaceflinger -> windowmanager -> uprobe
        // (frametime-capable sources first; windowmanager is last resort).
        let first = Method::first_auto();
        assert_eq!(first, Method::Uprobe, "64-bit builds try uprobe first");
        assert_eq!(Method::Uprobe.next_auto(), Method::SurfaceFlinger);
        assert_eq!(Method::SurfaceFlinger.next_auto(), Method::WindowManager);
        assert_eq!(Method::WindowManager.next_auto(), Method::Uprobe);
    }

    #[test]
    fn present_hist_parsing() {
        let dump = "SurfaceFlinger TimeStats:\nstatsStart = 1794225493\ntotalFrames = 335560\npresentToPresent histogram is as below:\n0ms=0 1ms=0 8ms=284387 16ms=26853 33ms=9640 500ms=934 1000ms=499\naverageFrameDuration = 3.470 ms\n";
        let hist = parse_present_hist(dump);
        assert_eq!(hist.len(), 7);
        assert_eq!(hist[0], (0, 0));
        assert_eq!(hist[2], (8, 284387));
        assert_eq!(hist[4], (33, 9640));
        assert_eq!(hist[6], (1000, 499));
        assert!(parse_present_hist("no histogram here").is_empty());
    }

    #[test]
    fn hist_differ_emits_deltas_only() {
        let acc = new_acc();
        let mut d = SfHistDiffer::new();
        // First poll: baseline only, no intervals into pre-run history.
        d.ingest(&[(8, 10), (16, 2)], &acc);
        assert_eq!(snap_frames(&acc).1, 0);
        // Second poll: 3 new 8ms + 1 new 16ms.
        d.ingest(&[(8, 13), (16, 3)], &acc);
        let (_, frames, sum) = snap_frames(&acc);
        assert_eq!(frames, 4);
        // 8ms -> bucket 85 (8500us), 16ms -> 165 (16500us).
        assert_eq!(sum, 3 * 8_500 + 16_500);
        assert_eq!(hist_sum(&acc), 4);
    }

    #[test]
    fn hist_differ_rebases_on_reset() {
        let acc = new_acc();
        let mut d = SfHistDiffer::new();
        d.ingest(&[(8, 10)], &acc);
        d.ingest(&[(8, 12)], &acc);
        assert_eq!(snap_frames(&acc).1, 2);
        // Count went backwards: window reset -> no negative interval, re-baseline.
        d.ingest(&[(8, 3)], &acc);
        assert_eq!(snap_frames(&acc).1, 2);
        d.ingest(&[(8, 5)], &acc);
        assert_eq!(snap_frames(&acc).1, 4);
    }

    #[test]
    fn publish_gap_threshold_and_cooldown() {
        let acc = new_acc();
        let mut g = acc.inner.lock().unwrap_or_else(|e| e.into_inner());
        let t0 = Instant::now();
        g.last_publish = Some(t0);
        // Normal cadence: no warning.
        assert_eq!(note_publish(&mut g, t0 + Duration::from_millis(800)), None);
        // Real stall: one warning carrying the gap.
        let t1 = t0 + Duration::from_millis(800);
        assert_eq!(
            note_publish(&mut g, t1 + Duration::from_secs(5)),
            Some(5000)
        );
        // A second stall inside the cooldown is suppressed.
        let t2 = t1 + Duration::from_secs(5);
        assert_eq!(note_publish(&mut g, t2 + Duration::from_secs(5)), None);
        // Once the cooldown has passed it warns again.
        let t3 = t2 + Duration::from_secs(5);
        assert_eq!(
            note_publish(&mut g, t3 + PUBLISH_GAP_WARN_COOLDOWN),
            Some(PUBLISH_GAP_WARN_COOLDOWN.as_millis())
        );
    }
}
