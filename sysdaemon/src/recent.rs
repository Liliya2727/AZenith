// ---------------- recent tasks (the recents of the task manager) ----------------
//
// The task manager keeps a list of the tasks the user opened. The java
// companion used ActivityManager.getRecentTasks(30) for it, so an app that is
// swiped away from the recents disappears from background_apps.
//
// Android 16 changed the AIDL to
//     ParceledListSlice getRecentTasks(int maxNum, int flags, int userId)
// The reply is a list of RunningTaskInfo. Reading the whole struct would need
// the exact field order of TaskInfo/RunningTaskInfo (it changes with every
// Android version), so the items are read as raw byte arrays and the package
// name is searched in them: the base intent and the component names are plain
// strings ("com.foo.bar", "com.foo.bar/.MainActivity").
use crate::log::debug;
use crate::ndk::{call, n, parcel_words, P};

/// Maximum number of tasks, same as the java companion.
const MAX_TASKS: u32 = 30;
/// User 0 (the owner), the daemon runs as root and reads the owner recents.
const USER_ID: u32 = 0;

/// The packages of the recent tasks, most recent first.
///
/// `known` are the packages of the running apps: a task also contains strings
/// like "android.intent.action.MAIN", which look like a package name but are
/// not one. A task whose app is not running is skipped, it has no pids to
/// publish anyway.
pub(crate) fn recent_packages(am: P, code: u32, known: &[String]) -> Option<Vec<String>> {
    let o = call(am, code, |i| unsafe {
        let n = n();
        (n.AParcel_writeInt32)(i, MAX_TASKS as i32);
        (n.AParcel_writeInt32)(i, 0); // flags
        (n.AParcel_writeInt32)(i, USER_ID as i32);
    })?;
    let bytes = parcel_words(o);
    unsafe { (n().AParcel_delete)(o) };
    let bytes = bytes?;
    debug!("getRecentTasks reply: {} bytes", bytes.len());

    let mut out: Vec<String> = Vec::new();
    for p in packages_in(&bytes) {
        if !known.iter().any(|k| *k == p) {
            continue;
        }
        if !out.contains(&p) {
            out.push(p);
        }
        if out.len() >= MAX_TASKS as usize {
            break;
        }
    }
    Some(out)
}

/// The first known (running) package in a task parcel. A RootTaskInfo writes
/// the focused activity before the base intent, so this is the app on screen.
pub(crate) fn first_known(bytes: &[u8], known: &[String]) -> Option<String> {
    packages_in(bytes).into_iter().find(|p| known.iter().any(|k| k == p))
}

/// The package names inside the bytes of a task.
///
/// The strings of a parcel are UTF-16, so every character is followed by a zero
/// byte. A package name is a sequence of ASCII letters, digits, dots and
/// underscores with at least one dot.
pub(crate) fn packages_in(blob: &[u8]) -> Vec<String> {
    let mut out: Vec<String> = Vec::new();
    let mut i = 0;
    while i + 1 < blob.len() {
        // The start of a string: a printable char, then a zero byte, and the
        // previous byte pair is the end of another string (or the length
        // prefix of this one).
        if blob[i + 1] != 0 || !is_pkg_char(blob[i]) {
            i += 1;
            continue;
        }
        let start = i;
        let mut j = i;
        while j + 1 < blob.len() && blob[j + 1] == 0 && is_pkg_char(blob[j]) {
            j += 2;
        }
        let text: String = blob[start..j].iter().step_by(2).map(|&c| c as char).collect();
        // "com.foo.bar/.MainActivity" -> "com.foo.bar"
        let pkg = text.split('/').next().unwrap_or_default();
        if is_package(pkg) && !out.iter().any(|p| p == pkg) {
            out.push(pkg.to_string());
        }
        i = j.max(i + 1);
    }
    out
}

fn is_pkg_char(c: u8) -> bool {
    c.is_ascii_alphanumeric() || c == b'.' || c == b'_' || c == b'-'
}

/// A package name: at least one dot, only the allowed characters and no empty
/// part.
fn is_package(s: &str) -> bool {
    if !s.contains('.') || s.len() > 255 {
        return false;
    }
    let mut parts = s.split('.');
    let mut count = 0;
    for p in parts.by_ref() {
        if p.is_empty() || !p.chars().all(|c| c.is_ascii_alphanumeric() || c == '_' || c == '-') {
            return false;
        }
        count += 1;
    }
    count >= 2
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A string of a parcel: int32 length + UTF-16 + the terminator.
    fn parcel_str(s: &str) -> Vec<u8> {
        let mut v = Vec::new();
        v.extend_from_slice(&(s.len() as i32).to_le_bytes());
        for c in s.encode_utf16() {
            v.extend_from_slice(&c.to_le_bytes());
        }
        v.extend_from_slice(&[0, 0]);
        v
    }

    #[test]
    fn the_package_is_found_in_the_task() {
        let mut blob = Vec::new();
        blob.extend_from_slice(&parcel_str("android.intent.action.MAIN"));
        blob.extend_from_slice(&parcel_str("com.instagram.android"));
        blob.extend_from_slice(&parcel_str("com.instagram.android/.MainActivity"));
        let p = packages_in(&blob);
        // The action looks like a package name, only the known packages are
        // used, so the caller picks "com.instagram.android".
        assert!(p.contains(&"com.instagram.android".to_string()));
        assert!(p.contains(&"android.intent.action.MAIN".to_string()));
    }

    #[test]
    fn the_component_name_is_shortened() {
        let mut blob = Vec::new();
        blob.extend_from_slice(&parcel_str("com.foo.bar/.MainActivity"));
        assert_eq!(packages_in(&blob), vec!["com.foo.bar".to_string()]);
    }

    #[test]
    fn no_package_no_result() {
        let mut blob = Vec::new();
        blob.extend_from_slice(&parcel_str("hello world"));
        blob.extend_from_slice(&parcel_str("2024-01-01"));
        assert!(packages_in(&blob).is_empty());
    }
}
