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
// ---------------- /proc scan: app process table + initial foreground guess ----------------
use std::fs;
use std::os::unix::fs::MetadataExt;

use crate::state::is_app;

pub(crate) struct AppProc {
    pub(crate) pid: i32,
    pub(crate) pkg: String,
    pub(crate) uid: i32,
    /// oom_score_adj: 0 means "top app" (the app the user is looking at).
    pub(crate) adj: i32,
    /// True for the package's main process (the process name has no ":suffix").
    /// Only its death means the app has no UI left; an auxiliary process
    /// (":push", ":service") dying is normal and says nothing about the app.
    pub(crate) main: bool,
}

/// Base package of a process name: "com.foo.bar:remote" -> "com.foo.bar".
/// Shells, native binaries and zygote helpers are rejected.
///
/// The same helper is used for the names that come from AMS (onProcessStarted)
/// and for the ones read from /proc, so both sources always agree on the
/// package of a pid. A process name with a suffix (":push", ":service") would
/// otherwise show up as a second, bogus app in app_status/background_apps.
pub(crate) fn base_pkg(name: &str) -> Option<String> {
    let name = name.trim();
    if name.is_empty() || name.contains('/') { return None; }
    let base = name.split(':').next().unwrap_or(name);
    if !base.contains('.') || base.starts_with('.') || base.ends_with("_zygote") { return None; }
    Some(base.to_string())
}

/// First field of /proc/<pid>/cmdline (the process name), if still alive.
fn process_name(pid: i32) -> Option<String> {
    let raw = fs::read(format!("/proc/{pid}/cmdline")).ok()?;
    let first = raw.split(|&b| b == 0).next()?;
    if first.is_empty() { return None; }
    Some(String::from_utf8_lossy(first).into_owned())
}

/// Package of a running process, read from /proc/<pid>/cmdline.
pub(crate) fn pkg_of(pid: i32) -> Option<String> {
    base_pkg(&process_name(pid)?)
}

/// Whether the process still alive in /proc/<pid> is the app's main process.
/// Used when onProcessStarted did not carry a usable process name.
pub(crate) fn is_main_process(pid: i32) -> bool {
    process_name(pid).is_some_and(|name| !name.contains(':'))
}

fn adj_of(pid: i32) -> i32 {
    fs::read_to_string(format!("/proc/{pid}/oom_score_adj"))
        .ok()
        .and_then(|v| v.trim().parse().ok())
        .unwrap_or(999)
}

/// Every running process that belongs to a user app.
pub(crate) fn scan_apps() -> Vec<AppProc> {
    let mut out = Vec::new();
    let Ok(rd) = fs::read_dir("/proc") else { return out; };
    for e in rd.flatten() {
        let Ok(pid) = e.file_name().to_string_lossy().parse::<i32>() else { continue };
        let Ok(md) = e.metadata() else { continue };
        let uid = md.uid() as i32;
        if !is_app(uid) { continue; }
        let Some(name) = process_name(pid) else { continue };
        let Some(pkg) = base_pkg(&name) else { continue };
        out.push(AppProc { pid, pkg, uid, adj: adj_of(pid), main: !name.contains(':') });
    }
    out
}

#[cfg(test)]
mod tests {
    use super::base_pkg;

    #[test]
    fn the_process_suffix_is_stripped() {
        assert_eq!(base_pkg("com.foo.bar"), Some("com.foo.bar".into()));
        assert_eq!(base_pkg("com.foo.bar:push"), Some("com.foo.bar".into()));
        assert_eq!(base_pkg("com.foo.bar:s5"), Some("com.foo.bar".into()));
        assert_eq!(base_pkg("com.foo.bar:remote:extra"), Some("com.foo.bar".into()));
    }

    #[test]
    fn non_app_processes_are_rejected() {
        assert_eq!(base_pkg(""), None);
        assert_eq!(base_pkg("   "), None);
        assert_eq!(base_pkg("sh"), None);
        assert_eq!(base_pkg("/system/bin/surfaceflinger"), None);
        assert_eq!(base_pkg(".hidden"), None);
        assert_eq!(base_pkg("com.foo_zygote"), None);
    }
}
