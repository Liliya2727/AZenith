// ---------------- app monitor state + publishing (app_status / background_apps) ----------------
//
// The daemon has no profile logic any more, it only observes:
//   * app_status      -> focused app + screen/saver/zen/battery state
//   * background_apps -> recently used user apps and their live pids
use std::collections::BTreeMap;
use std::fs;
use std::os::unix::fs::PermissionsExt;
use std::sync::atomic::{AtomicU32, Ordering};
use std::sync::{LazyLock, Mutex, MutexGuard, OnceLock};

use crate::log::{debug, error, info};
use crate::pkg::label_of;
use crate::{BACKGROUND_FILE, STATUS_FILE};

/// How many packages are kept in the "recently used" list.
pub(crate) const MAX_RECENT: usize = 30;

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
    /// highest usable display refresh rate (used by the service as a cap)
    pub(crate) max_refresh_rate: i32,
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
            max_refresh_rate: 60,
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
    sys: SysStat,
    last_status: String,
    last_background: String,
}

static ST: LazyLock<Mutex<St>> = LazyLock::new(|| {
    Mutex::new(St {
        focused: None,
        recent: Vec::new(),
        procs: BTreeMap::new(),
        sys: SysStat::default(),
        last_status: String::new(),
        last_background: String::new(),
    })
});

pub(crate) fn st() -> MutexGuard<'static, St> { ST.lock().unwrap_or_else(|e| e.into_inner()) }

/// The packages of the known app processes.
pub(crate) fn known_packages() -> Vec<String> {
    let s = st();
    let mut v: Vec<String> = s.procs.values().map(|(p, _)| p.clone()).collect();
    v.sort();
    v.dedup();
    v
}

/// Drop recent entries whose processes are all gone.
pub(crate) fn cleanup_stale() {
    let mut s = st();
    let mut changed = false;
    let live: std::collections::HashSet<String> = s.procs.values().map(|(p, _)| p.clone()).collect();
    let focused = s.focused.as_ref().map(|(p, _, _)| p.clone());
    s.recent.retain(|p| {
        let keep = live.contains(p) || focused.as_ref().is_some_and(|f| f == p);
        if !keep { changed = true; }
        keep
    });
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

/// Replace the recent list with the AMS recents (the task manager recents).
pub(crate) fn set_recent(list: Vec<String>) {
    let focused = st().focused.as_ref().map(|(p, _, _)| p.clone());
    let live: std::collections::HashSet<String> = st().procs.values().map(|(p, _)| p.clone()).collect();
    let mut s = st();
    s.recent.clear();
    for p in list {
        if !live.contains(&p) {
            continue;
        }
        if s.recent.len() >= MAX_RECENT {
            break;
        }
        if !s.recent.contains(&p) {
            s.recent.push(p);
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
    sys: SysStat,
}

impl Snapshot {
    fn take() -> Snapshot {
        let s = st();
        Snapshot {
            focused: s.focused.clone(),
            recent: s.recent.clone(),
            procs: s.procs.clone(),
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
             refresh_rate {}\n\
             max_refresh_rate {}\n",
            self.sys.screen_awake as u8,
            self.sys.battery_saver as u8,
            self.sys.zen_mode,
            self.sys.battery_level,
            self.sys.is_charging as u8,
            self.sys.refresh_rate,
            self.sys.max_refresh_rate,
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
            if let Some((pid, (_p, uid))) = self.procs.iter().find(|(_pid, (p, _uid))| p == pkg) {
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
    }
    publish(why);
}

/// A process was started.
pub(crate) fn add_proc(pid: i32, pkg: &str, uid: i32) {
    let Some(pkg) = crate::procs::base_pkg(pkg) else { return };
    {
        let mut s = st();
        let e = s.procs.entry(pid).or_insert_with(|| (pkg.to_string(), uid));
        if e.0 != pkg || e.1 != uid { *e = (pkg.to_string(), uid); }
    }
    publish("process started");
}

/// A process died. When it was the focused one, the focus moves to another
/// live process of the same app, or it is cleared when the app is gone.
pub(crate) fn remove_proc(pid: i32, why: &str) {
    {
        let mut s = st();
        let removed = s.procs.remove(&pid).map(|(p, _)| p);
        if let Some(ref pkg) = removed {
            // The process that was just listed died -> remove the package
            // from background_apps entirely (no fallback to other pids).
            if !s.focused.as_ref().is_some_and(|(f, _, _)| f == pkg) {
                s.recent.retain(|x| x != pkg);
            } else {
                if !s.procs.values().any(|(p, _)| p == pkg) {
                    s.focused = None;
                }
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
        if !existed && !cleared { return; }
    }
    publish(why);
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
static PREV_REFRESH: OnceLock<(i32, i32)> = OnceLock::new();

/// Remember the focused app, the recent list and the refresh rates of the
/// previous run. Must be called before the first publish, otherwise the files
/// are already overwritten.
pub(crate) fn capture_previous() {
    let _ = PREV.set(previous_focus());
    let _ = PREV_REFRESH.set(previous_refresh());
}

/// (refresh_rate, max_refresh_rate) of the previous run, (-1, 60) if unknown.
pub(crate) fn previous_refresh() -> (i32, i32) {
    let mut cur = -1;
    let mut max = 60;
    let Ok(text) = fs::read_to_string(STATUS_FILE) else { return (cur, max) };
    for line in text.lines() {
        let mut it = line.split_whitespace();
        match (it.next(), it.next()) {
            (Some("refresh_rate"), Some(v)) => cur = v.parse().unwrap_or(-1),
            (Some("max_refresh_rate"), Some(v)) => max = v.parse().unwrap_or(60),
            _ => {}
        }
    }
    (cur, max)
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
        for a in &apps { s.procs.insert(a.pid, (a.pkg.clone(), a.uid)); }

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
    fn a_dead_app_is_not_listed() {
        let s = snap(Some("com.a"), &["com.a", "com.b"], &[(1, "com.a")]);
        assert_eq!(s.background(), "com.a 1 10001\n");
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
            sys: s.sys,
        };
        assert!(snap.valid_for(&s));

        // The user switched to another app while the label was read: the
        // snapshot must not be published any more.
        let mut other = st(Some("com.b"), &["com.b"], &[(2, "com.b")]);
        other.focused = Some(("com.b".to_string(), 2, 10002));
        assert!(!snap.valid_for(&other));
    }
}
