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

/// Package of a running process, read from /proc/<pid>/cmdline.
pub(crate) fn pkg_of(pid: i32) -> Option<String> {
    let raw = fs::read(format!("/proc/{pid}/cmdline")).ok()?;
    let name = String::from_utf8_lossy(raw.split(|&b| b == 0).next()?).into_owned();
    base_pkg(&name)
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
        let Some(pkg) = pkg_of(pid) else { continue };
        out.push(AppProc { pid, pkg, uid, adj: adj_of(pid) });
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
