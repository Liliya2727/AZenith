// Copyright (C) 2026 Zexshia
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
// ---------------- app monitor state + publishing (app_status / background_apps) ----------------
//
// The daemon has no profile logic any more, it only observes:
//   * app_status      -> focused app + screen/saver/zen/battery state
//   * background_apps -> recently used user apps and their live pids
use std::collections::{BTreeMap, BTreeSet};
use std::fs;
use std::os::unix::fs::PermissionsExt;
use std::sync::atomic::{AtomicU32, Ordering};
use std::sync::{LazyLock, Mutex, MutexGuard, OnceLock};
use std::time::{Duration, Instant};

use crate::log::{debug, error, info};
use crate::pkg::label_of;
use crate::{BACKGROUND_FILE, STATUS_FILE};

/// How many packages are kept in the "recently used" list.
pub(crate) const MAX_RECENT: usize = 30;

/// How long an app may be without its main process before it is treated as
/// closed. The grace lets a restart (crash recovery, renderer restart) spawn a
/// new main process and cancel the close instead of dropping the app.
pub(crate) const MAIN_DEATH_GRACE: Duration = Duration::from_secs(2);

/// Screen on/off, battery saver, zen mode, battery level and charging state.
#[derive(Clone, Copy, PartialEq, Debug)]
pub(crate) struct SysStat {
    pub(crate) screen_awake: bool,
    pub(crate) battery_saver: bool,
    /// 0=off, 1=priority, 2=none, 3=alarms
    pub(crate) zen_mode: i32,
    /// 0..=100, -1 = unknown
    pub(crate) battery_level: i32,
    pub(crate) is_charging: bool,
    /// current display refresh rate, -1 = unknown
    pub(crate) refresh_rate: i32,
}

impl Default for SysStat {
    fn default() -> Self {
        SysStat {
            screen_awake: false,
            battery_saver: false,
            zen_mode: 0,
            battery_level: -1,
            is_charging: false,
            refresh_rate: -1,
        }
    }
}

pub(crate) struct St {
    /// package, pid, uid of the app in the foreground
    focused: Option<(String, i32, i32)>,
    /// packages in the order they were used (most recent first)
    recent: Vec<String>,
    /// pid -> (package, uid) of every known app process
    procs: BTreeMap<i32, (String, i32)>,
    /// pids of the packages' main processes (no ":suffix" in the process name).
    /// The map above holds every process, main and auxiliary; only a main
    /// process death means the app has no UI left.
    main_pids: BTreeSet<i32>,
    /// package -> instant its main process died. The app is removed from the
    /// recent list once the grace expires, unless a new main process starts.
    pending_close: BTreeMap<String, Instant>,
    /// package -> last seen (pid, uid). Kept when the process dies so a task
    /// that is still in the recents stays in background_apps.
    apps: BTreeMap<String, (i32, i32)>,
    /// true once the task manager returned its task list; from then on the
    /// task list, not process death, decides what leaves background_apps.
    recents_seen: bool,
    sys: SysStat,
    last_status: String,
    last_background: String,
}

static ST: LazyLock<Mutex<St>> = LazyLock::new(|| {
    Mutex::new(St {
        focused: None,
        recent: Vec::new(),
        procs: BTreeMap::new(),
        main_pids: BTreeSet::new(),
        pending_close: BTreeMap::new(),
        apps: BTreeMap::new(),
        recents_seen: false,
        sys: SysStat::default(),
        last_status: String::new(),
        last_background: String::new(),
    })
});

pub(crate) fn st() -> MutexGuard<'static, St> { ST.lock().unwrap_or_else(|e| e.into_inner()) }

/// The packages of the known app processes (currently alive).
pub(crate) fn known_packages() -> Vec<String> {
    let s = st();
    let mut v: Vec<String> = s.procs.values().map(|(p, _)| p.clone()).collect();
    v.sort();
    v.dedup();
    v
}

/// Every package the daemon has seen as a user app, alive or not. Used to
/// recognise the package names inside a task parcel.
pub(crate) fn known_apps() -> Vec<String> {
    st().apps.keys().cloned().collect()
}

/// Drop recent entries whose processes are all gone.
///
/// This is only the fallback for ROMs without a usable task list. When the
/// task manager works, a cached process may be killed while its task stays in
/// the recents; removing it here would drop background_apps before the user
/// actually clears the task.
pub(crate) fn cleanup_stale() {
    let mut s = st();
    let mut changed = false;
    let live: std::collections::HashSet<String> = s.procs.values().map(|(p, _)| p.clone()).collect();
    let focused = s.focused.as_ref().map(|(p, _, _)| p.clone());
    if !s.recents_seen {
        s.recent.retain(|p| {
            let keep = live.contains(p) || focused.as_ref().is_some_and(|f| f == p);
            if !keep { changed = true; }
            keep
        });
    }
    if let Some(ref f) = focused {
        if !live.contains(f) {
            s.focused = None;
            changed = true;
        }
    }
    drop(s);
    if changed {
        publish("stale");
    }
}

/// Replace the recent list with the task manager list. A package is removed
/// only when its task is gone from the recents, not when its process dies.
///
/// The task-manager reply is only trusted as a full recents snapshot when it
/// actually contains more than the focused app. On Android 16 (and several
/// ROMs) getRecentTasks / getTasks return only the focused task, so blindly
/// replacing `recent` with that reply would drop every background app the
/// moment the user switches focus — instead of waiting until the user clears
/// the task from the recents. An empty or focused-only reply is therefore
/// treated as "no useful recents data" and `recent` is left untouched.
pub(crate) fn set_recent(list: Vec<String>) {
    let focused = st().focused.as_ref().map(|(p, _, _)| p.clone());
    let live: std::collections::HashSet<String> = st().procs.values().map(|(p, _)| p.clone()).collect();
    let mut s = st();
    s.recents_seen = true;
    // Only replace `recent` when we have a real multi-task snapshot. A focused-
    // only (or empty) reply is not a full recents list; wiping on it would
    // remove background apps on every focus change.
    if list.len() > 1 {
        s.recent.clear();
        for p in list {
            if s.recent.len() >= MAX_RECENT {
                break;
            }
            if !s.recent.contains(&p) {
                s.recent.push(p);
            }
        }
    }
    if let Some(ref f) = focused {
        if live.contains(f) {
            if let Some(pos) = s.recent.iter().position(|x| x == f) {
                s.recent.remove(pos);
            }
            s.recent.insert(0, f.clone());
        } else {
            s.focused = None;
        }
    }
    drop(s);
    publish("recent");
}

/// True for the uid range of the user apps (10000..19999).
pub(crate) fn is_app(uid: i32) -> bool { (10000..20000).contains(&(uid % 100000)) }

/// Package of a known process (from the process table).
pub(crate) fn pkg_of_pid(pid: i32) -> Option<String> {
    st().procs.get(&pid).map(|(p, _)| p.clone())
}

/// Package of the app that is currently in the foreground.
pub(crate) fn focused_pkg() -> Option<String> {
    st().focused.as_ref().map(|(p, _, _)| p.clone())
}

/// A live pid/uid of an app, from the process table.
pub(crate) fn proc_of_pkg(pkg: &str) -> Option<(i32, i32)> {
    st().procs.iter().find(|(_, (p, _))| p == pkg)
        .map(|(pid, (_, uid))| (*pid, *uid))
}

/// Screen on/off, as reported by IPowerManager.
pub(crate) fn screen_awake() -> bool {
    st().sys.screen_awake
}

// ---------------- writing ----------------

/// Write a file atomically (tmp file + rename), so a reader never sees a half
/// written file. The tmp name is unique per call: with a fixed name two
/// threads could eat each other's tmp file during the rename.
pub(crate) fn write_atomic(path: &str, data: &str) -> bool {
    static SEQ: AtomicU32 = AtomicU32::new(0);
    let tmp = format!("{path}.{}.tmp", SEQ.fetch_add(1, Ordering::Relaxed));
    let r = fs::write(&tmp, data)
        .and_then(|_| fs::set_permissions(&tmp, fs::Permissions::from_mode(0o666)))
        .and_then(|_| fs::rename(&tmp, path));
    if let Err(e) = r {
        error!("failed to write {path}: {e}");
        let _ = fs::remove_file(&tmp);
        return false;
    }
    true
}

/// A consistent snapshot of the state: the focused app, the recent list, the
/// process table and the system state all belong to the same moment.
///
/// The label of the focused app is read from system_server, that takes ~50 ms.
/// In that time the user can switch to another app, so the files are never
/// built from two different moments (that would publish the name of one app
/// together with the pid of another one).
#[derive(Clone)]
struct Snapshot {
    focused: Option<(String, i32, i32)>,
    recent: Vec<String>,
    procs: BTreeMap<i32, (String, i32)>,
    apps: BTreeMap<String, (i32, i32)>,
    sys: SysStat,
}

impl Snapshot {
    fn take() -> Snapshot {
        let s = st();
        Snapshot {
            focused: s.focused.clone(),
            recent: s.recent.clone(),
            procs: s.procs.clone(),
            apps: s.apps.clone(),
            sys: s.sys,
        }
    }

    /// False when the focused app changed in the meantime: the label then
    /// belongs to another app and must not be published with this pid.
    fn valid_for(&self, s: &St) -> bool {
        s.focused == self.focused
    }

    fn status(&self, name: &str) -> String {
        let (pkg, pid, uid) = match &self.focused {
            Some((p, pid, uid)) => (p.as_str(), *pid, *uid),
            None => ("none", 0, 0),
        };
        format!(
            "focused_app {pkg} {pid} {uid}\n\
             screen_awake {}\n\
             battery_saver {}\n\
             zen_mode {}\n\
             battery_level {}\n\
             is_charging {}\n\
             app_name {name}\n\
             refresh_rate {}\n",
            self.sys.screen_awake as u8,
            self.sys.battery_saver as u8,
            self.sys.zen_mode,
            self.sys.battery_level,
            self.sys.is_charging as u8,
            self.sys.refresh_rate,
        )
    }

    fn background(&self) -> String {
        // Only the apps the user has opened recently, with the pids that are
        // still alive. Every running app is NOT listed here.
        // The focused app is always the first entry of the list.
        let mut order: Vec<&String> = Vec::new();
        if let Some((pkg, _, _)) = &self.focused {
            order.push(pkg);
        }
        for pkg in &self.recent {
            if !order.iter().any(|p| **p == *pkg) {
                order.push(pkg);
            }
        }

        let mut out = String::new();
        for pkg in order {
            if let Some((f, fpid, fuid)) = &self.focused {
                if f == pkg {
                    out.push_str(pkg);
                    out.push(' ');
                    out.push_str(&fpid.to_string());
                    out.push(' ');
                    out.push_str(&fuid.to_string());
                    out.push('\n');
                    continue;
                }
            }
            // A live process is preferred. If the app's cached process was
            // killed but its task is still in the recents, keep the last known
            // pid so the task does not disappear from background_apps.
            if let Some((pid, (_p, uid))) = self.procs.iter().find(|(_pid, (p, _uid))| p == pkg) {
                out.push_str(pkg);
                out.push(' ');
                out.push_str(&pid.to_string());
                out.push(' ');
                out.push_str(&uid.to_string());
                out.push('\n');
            } else if let Some(&(pid, uid)) = self.apps.get(pkg) {
                out.push_str(pkg);
                out.push(' ');
                out.push_str(&pid.to_string());
                out.push(' ');
                out.push_str(&uid.to_string());
                out.push('\n');
            }
        }
        out
    }
}

/// Publish both files. The content is only written when it actually changed, so
/// an idle daemon does not touch the disk.
pub(crate) fn publish(why: &str) {
    let snap = Snapshot::take();
    // The label lookup is a synchronous call to system_server, it must not be
    // done while the state lock is held.
    let name = match &snap.focused {
        Some((p, _, _)) => label_of(p),
        None => "Unknown".to_string(),
    };

    let mut s = st();
    // The focused app changed while the label was read: publishing now would
    // mix the name of the old app with the pid of the new one. The event of
    // the new app triggers a new publish.
    if !snap.valid_for(&s) {
        debug!("publish skipped ({why}), the focused app changed while the label was read");
        return;
    }
    let status = snap.status(&name);
    let background = snap.background();
    if status != s.last_status {
        if write_atomic(STATUS_FILE, &status) {
            s.last_status = status;
            info!("app_status updated ({why})");
        }
    } else {
        debug!("app_status unchanged ({why})");
    }
    if background != s.last_background {
        let lines = background.lines().count();
        if write_atomic(BACKGROUND_FILE, &background) {
            s.last_background = background;
            info!("background_apps updated ({why}): {lines} lines");
        }
    } else {
        debug!("background_apps unchanged ({why})");
    }
}

// ---------------- state updates ----------------

fn push_recent(s: &mut St, pkg: &str) {
    s.recent.retain(|p| p != pkg);
    s.recent.insert(0, pkg.to_string());
    s.recent.truncate(MAX_RECENT);
}

/// A new app came to the foreground.
pub(crate) fn set_focused(pkg: &str, pid: i32, uid: i32, why: &str) {
    // A process name with a suffix would be a package that does not exist.
    let pkg = crate::procs::base_pkg(pkg).unwrap_or_else(|| pkg.to_string());
    {
        let mut s = st();
        let prev = s.focused.as_ref().map(|(p, _, _)| p.clone());
        match prev {
            Some(ref p) if *p == pkg => {
                if let Some(f) = s.focused.as_mut() {
                    f.1 = pid;
                    f.2 = uid;
                }
            }
            _ => {
                info!(
                    "focused app: [{}] -> [{pkg}] ({why})",
                    prev.unwrap_or_else(|| "none".to_string())
                );
                s.focused = Some((pkg.to_string(), pid, uid));
            }
        }
        // Only an app the user really opened goes into the "recent" list, a
        // process that starts in the background does not.
        push_recent(&mut s, &pkg);
        // The focused app must always be known in the process table, even when
        // its onProcessStarted event was missed.
        s.procs.insert(pid, (pkg.to_string(), uid));
        // A process that hosts an activity is the app's main process.
        s.main_pids.insert(pid);
        s.pending_close.remove(&pkg);
        s.apps.insert(pkg, (pid, uid));
    }
    publish(why);
}

/// A process was started.
pub(crate) fn add_proc(pid: i32, pkg: &str, uid: i32, main: bool) {
    let Some(pkg) = crate::procs::base_pkg(pkg) else { return };
    {
        let mut s = st();
        let e = s.procs.entry(pid).or_insert_with(|| (pkg.clone(), uid));
        if e.0 != pkg || e.1 != uid { *e = (pkg.clone(), uid); }
        s.apps.insert(pkg.clone(), (pid, uid));
        if main {
            s.main_pids.insert(pid);
            // A new main process means the app restarted: cancel the close.
            s.pending_close.remove(&pkg);
        } else {
            s.main_pids.remove(&pid);
        }
    }
    publish("process started");
}

/// A process died. When it was the focused one, the focus moves to another
/// live process of the same app, or it is cleared when the app is gone.
///
/// A dead main process schedules the app close after `MAIN_DEATH_GRACE`. An
/// auxiliary process (`:push`, `:service`) dying never closes the app: the user
/// can still be inside it.
pub(crate) fn remove_proc(pid: i32, why: &str) {
    let log = {
        let mut s = st();
        let (pkg, was_main, should_publish) = remove_proc_from(&mut s, pid, Instant::now());
        if !should_publish { return; }
        pkg.map(|pkg| (pkg, was_main))
    };
    if let Some((pkg, was_main)) = log {
        info!("process died pid={pid} pkg={pkg} main={was_main}");
    }
    publish(why);
}

/// Remove `pid` from `s`. Returns (package, was it the main process, publish).
fn remove_proc_from(s: &mut St, pid: i32, now: Instant) -> (Option<String>, bool, bool) {
    let removed = s.procs.remove(&pid).map(|(p, _)| p);
    let was_main = s.main_pids.remove(&pid);
    if let Some(ref pkg) = removed {
        // The UI process is gone. Auxiliary processes may keep running, but an
        // app without its main process has no window left, so close it after a
        // grace that a restart can cancel.
        let main_alive = s
            .procs
            .iter()
            .any(|(p, (name, _))| name == pkg && s.main_pids.contains(p));
        if was_main && !main_alive {
            s.pending_close.insert(pkg.clone(), now);
        }
        // When it was the last process of the app, remove it from
        // background_apps immediately instead of waiting for the user to clear
        // the task from the recents: Android kills cached processes while their
        // task is still in recents, but a destroyed app should not linger in
        // background_apps.
        if !s.procs.values().any(|(p, _)| p == pkg) {
            s.recent.retain(|x| x != pkg);
            s.apps.remove(pkg);
            s.pending_close.remove(pkg);
        }
    }
    let existed = removed.is_some();
    let mut cleared = false;
    if let Some((pkg, fpid, _)) = s.focused.clone()
        && fpid == pid
    {
        let alt = s
            .procs
            .iter()
            .find(|(_, (p, _))| *p == pkg)
            .map(|(p, (_, u))| (*p, *u));
        match alt {
            Some((np, nu)) => s.focused = Some((pkg, np, nu)),
            None => {
                info!("focused app [{pkg}] is gone");
                s.focused = None;
                cleared = true;
            }
        }
    }
    let should_publish = existed || cleared;
    (removed, was_main, should_publish)
}

/// Remove apps whose main process has been gone for `MAIN_DEATH_GRACE`.
///
/// Only `recent` is touched; `apps` keeps the last pid so the task parser can
/// still recognise the package. Returns true when the recent list changed.
fn close_main_dead_apps(s: &mut St, now: Instant) -> bool {
    let mut closed = false;
    let pkgs: Vec<String> = s.pending_close.keys().cloned().collect();
    for pkg in pkgs {
        let Some(at) = s.pending_close.get(&pkg).copied() else { continue };
        if now.duration_since(at) < MAIN_DEATH_GRACE {
            continue;
        }
        let main_alive = s
            .procs
            .iter()
            .any(|(p, (name, _))| name == &pkg && s.main_pids.contains(p));
        s.pending_close.remove(&pkg);
        if main_alive {
            continue;
        }
        if s.recent.iter().any(|p| p == &pkg) {
            s.recent.retain(|p| p != &pkg);
            closed = true;
        }
    }
    closed
}

/// Apply the pending main-process-death closes. Runs on the recents tick.
pub(crate) fn process_pending_closes() {
    let changed = {
        let mut s = st();
        close_main_dead_apps(&mut s, Instant::now())
    };
    if changed {
        publish("main process died");
    }
}

/// Update one of the system status values, republish if something changed.
pub(crate) fn sys_update(why: &str, f: impl FnOnce(&mut SysStat)) {
    let changed = {
        let mut s = st();
        let before = s.sys;
        f(&mut s.sys);
        before != s.sys
    };
    if !changed { return; }
    publish(why);
}

static PREV: OnceLock<Option<(String, i32, i32)>> = OnceLock::new();
static PREV_REFRESH: OnceLock<i32> = OnceLock::new();

/// Remember the focused app, the recent list and the refresh rates of the
/// previous run. Must be called before the first publish, otherwise the files
/// are already overwritten.
pub(crate) fn capture_previous() {
    let _ = PREV.set(previous_focus());
    let _ = PREV_REFRESH.set(previous_refresh());
}

/// refresh_rate of the previous run, -1 if unknown.
pub(crate) fn previous_refresh() -> i32 {
    let mut cur = -1;
    let Ok(text) = fs::read_to_string(STATUS_FILE) else { return cur };
    for line in text.lines() {
        let mut it = line.split_whitespace();
        match (it.next(), it.next()) {
            (Some("refresh_rate"), Some(v)) => cur = v.parse().unwrap_or(-1),
            _ => {}
        }
    }
    cur
}

/// Focused app of the previous run (written by us or by the java companion), so
/// a restart keeps the app that is really on screen.
fn previous_focus() -> Option<(String, i32, i32)> {
    let text = fs::read_to_string(STATUS_FILE).ok()?;
    let line = text.lines().find(|l| l.starts_with("focused_app "))?;
    let mut it = line.split_whitespace();
    it.next()?; // "focused_app"
    let pkg = it.next()?.to_string();
    let pid: i32 = it.next()?.parse().ok()?;
    let uid: i32 = it.next()?.parse().ok()?;
    if pkg == "none" || pkg == "unknown" || pid <= 0 { return None; }
    Some((pkg, pid, uid))
}

/// Initial state: the running app processes and the app that is on screen.
pub(crate) fn seed() {
    let apps = crate::procs::scan_apps();
    {
        let mut s = st();
        for a in &apps {
            s.procs.insert(a.pid, (a.pkg.clone(), a.uid));
            s.apps.insert(a.pkg.clone(), (a.pid, a.uid));
            if a.main {
                s.main_pids.insert(a.pid);
            }
        }

        // The "recently used" list starts empty on purpose: it is filled by the
        // apps the user really opens (the foreground events). It is NOT restored
        // from the old background_apps file, because that file only says which
        // apps were alive - restoring it would turn the list into "everything
        // that runs" again.

        let mut running: Vec<&crate::procs::AppProc> = apps.iter().collect();
        running.sort_by_key(|a| (std::cmp::Reverse(a.adj), a.pid));

        match PREV.get().cloned().flatten() {
            // The very same process is still alive -> it is still the app the
            // user was looking at (or at least a sane starting point).
            Some((pkg, pid, uid)) if s.procs.contains_key(&pid) => {
                info!("initial state: [{pkg}] is still in the foreground (pid {pid}, previous run)");
                s.focused = Some((pkg.clone(), pid, uid));
                push_recent(&mut s, &pkg);
            }
            _ => {
                // oom_score_adj == 0 is the "top app" slot, but foreground
                // services use it as well -> only trust an unambiguous result.
                let top: Vec<&crate::procs::AppProc> = running.iter().copied().filter(|a| a.adj == 0).collect();
                if top.len() == 1 {
                    let a = top[0];
                    info!("initial state: [{}] is in the foreground (pid {}, oom_score_adj 0)", a.pkg, a.pid);
                    s.focused = Some((a.pkg.clone(), a.pid, a.uid));
                    push_recent(&mut s, &a.pkg);
                } else {
                    debug!("initial state: no unambiguous foreground app ({} candidates), waiting for AMS", top.len());
                }
            }
        }
        info!("process table seeded with {} app processes", apps.len());
    }
    publish("startup");
}

#[cfg(test)]
mod tests {
    use super::*;

    fn st(focused: Option<&str>, recent: &[&str], procs: &[(i32, &str)]) -> St {
        St {
            focused: focused.map(|p| {
                if p == "com.b" {
                    (p.to_string(), 2, 10002)
                } else {
                    (p.to_string(), 1, 10001)
                }
            }),
            recent: recent.iter().map(|p| p.to_string()).collect(),
            procs: procs
                .iter()
                .map(|(pid, pkg)| (*pid, (pkg.to_string(), 10000 + pid)))
                .collect(),
            // Tests treat every process as a main process unless they say
            // otherwise (an auxiliary one is removed from main_pids by hand).
            main_pids: procs.iter().map(|(pid, _)| *pid).collect(),
            pending_close: BTreeMap::new(),
            apps: procs
                .iter()
                .map(|(pid, pkg)| (pkg.to_string(), (*pid, 10000 + pid)))
                .collect(),
            recents_seen: false,
            sys: SysStat::default(),
            last_status: String::new(),
            last_background: String::new(),
        }
    }

    fn snap(focused: Option<&str>, recent: &[&str], procs: &[(i32, &str)]) -> Snapshot {
        let s = st(focused, recent, procs);
        Snapshot {
            focused: s.focused,
            recent: s.recent,
            procs: s.procs,
            apps: s.apps,
            sys: s.sys,
        }
    }

    #[test]
    fn only_the_recent_apps_are_listed() {
        let s = snap(
            Some("com.a"),
            &["com.a", "com.b"],
            &[(1, "com.a"), (2, "com.b"), (3, "com.c")],
        );
        // com.c is running but was never opened -> it is not a background app.
        assert_eq!(s.background(), "com.a 1 10001\ncom.b 2 10002\n");
    }

    #[test]
    fn the_focused_app_comes_first() {
        let s = snap(Some("com.b"), &["com.a", "com.b"], &[(1, "com.a"), (2, "com.b")]);
        // The focused app uses its own pid/uid (from focused state), not the
        // first process found in the map.
        assert_eq!(s.background(), "com.b 2 10002\ncom.a 1 10001\n");
    }

    #[test]
    fn an_app_that_was_never_seen_is_not_listed() {
        let s = snap(Some("com.a"), &["com.a", "com.b"], &[(1, "com.a")]);
        assert_eq!(s.background(), "com.a 1 10001\n");
    }

    #[test]
    fn a_task_without_a_live_process_keeps_its_last_pid() {
        let mut state = st(Some("com.a"), &["com.a", "com.b"], &[(1, "com.a")]);
        // com.b was seen before, its process is gone but its task is still in
        // the recents.
        state.apps.insert("com.b".to_string(), (2, 10002));
        let s = Snapshot {
            focused: state.focused,
            recent: state.recent,
            procs: state.procs,
            apps: state.apps,
            sys: state.sys,
        };
        assert_eq!(s.background(), "com.a 1 10001\ncom.b 2 10002\n");
    }

    #[test]
    fn the_name_and_the_pid_belong_to_the_same_app() {
        let s = snap(Some("com.a"), &["com.a"], &[(1, "com.a")]);
        let status = s.status("App A");
        assert!(status.starts_with("focused_app com.a 1 10001\n"));
        assert!(status.contains("app_name App A\n"));
    }

    #[test]
    fn a_changed_focus_invalidates_the_snapshot() {
        let s = st(Some("com.a"), &["com.a"], &[(1, "com.a")]);
        let snap = Snapshot {
            focused: s.focused.clone(),
            recent: s.recent.clone(),
            procs: s.procs.clone(),
            apps: s.apps.clone(),
            sys: s.sys,
        };
        assert!(snap.valid_for(&s));

        // The user switched to another app while the label was read: the
        // snapshot must not be published any more.
        let mut other = st(Some("com.b"), &["com.b"], &[(2, "com.b")]);
        other.focused = Some(("com.b".to_string(), 2, 10002));
        assert!(!snap.valid_for(&other));
    }

    #[test]
    fn an_auxiliary_process_death_keeps_the_app() {
        let mut s = st(None, &["com.a"], &[(1, "com.a"), (2, "com.a")]);
        // pid 2 is a ":service" process, only pid 1 is the main process.
        s.main_pids.remove(&2);
        let (pkg, was_main, publish) = remove_proc_from(&mut s, 2, Instant::now());
        assert_eq!(pkg.as_deref(), Some("com.a"));
        assert!(!was_main);
        assert!(publish);
        assert!(s.recent.contains(&"com.a".to_string()));
        assert!(s.pending_close.is_empty());
    }

    #[test]
    fn a_main_process_death_closes_the_app_after_the_grace() {
        let mut s = st(None, &["com.a"], &[(1, "com.a"), (2, "com.a")]);
        s.main_pids.remove(&2);
        let t0 = Instant::now();
        let (_, was_main, _) = remove_proc_from(&mut s, 1, t0);
        assert!(was_main);
        // The auxiliary process is still alive: the app stays for now...
        assert!(s.recent.contains(&"com.a".to_string()));
        assert!(s.pending_close.contains_key("com.a"));
        assert!(!close_main_dead_apps(&mut s, t0 + MAIN_DEATH_GRACE / 2));
        assert!(s.recent.contains(&"com.a".to_string()));
        // ...and is closed once the grace has passed.
        assert!(close_main_dead_apps(
            &mut s,
            t0 + MAIN_DEATH_GRACE + Duration::from_millis(1)
        ));
        assert!(!s.recent.contains(&"com.a".to_string()));
    }

    #[test]
    fn a_restarted_main_process_cancels_the_close() {
        let mut s = st(None, &["com.a"], &[(1, "com.a"), (2, "com.a")]);
        s.main_pids.remove(&2);
        let t0 = Instant::now();
        remove_proc_from(&mut s, 1, t0);
        // A new main process started before the grace expired.
        s.procs.insert(3, ("com.a".to_string(), 10003));
        s.main_pids.insert(3);
        s.pending_close.remove("com.a");
        assert!(!close_main_dead_apps(
            &mut s,
            t0 + MAIN_DEATH_GRACE + Duration::from_millis(1)
        ));
        assert!(s.recent.contains(&"com.a".to_string()));
    }

    #[test]
    fn a_last_process_death_removes_the_app_immediately() {
        let mut s = st(None, &["com.a"], &[(1, "com.a")]);
        let (_, was_main, _) = remove_proc_from(&mut s, 1, Instant::now());
        assert!(was_main);
        assert!(!s.recent.contains(&"com.a".to_string()));
        assert!(s.pending_close.is_empty());
    }
}
