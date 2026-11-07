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
// ---------------- recent tasks (the recents of the task manager) ----------------
//
// The task manager keeps a list of the tasks the user opened. An app that is
// swiped away from the recents disappears from background_apps.
//
// getRecentTasks is not usable for that on Android 16: the task manager only
// returns the focused task (or the caller's own tasks), so replacing the list
// with its reply drops every background app. getTasks returns the full list of
// running/recent tasks and is used first; getRecentTasks stays as the fallback
// for ROMs that do not expose it.
//
// Reading the whole struct would need the exact field order of
// TaskInfo/RunningTaskInfo (it changes with every Android version), so the
// reply is scanned as raw bytes: the base intent and the component names are
// plain strings ("com.foo.bar", "com.foo.bar/.MainActivity").
use crate::log::debug;
use crate::ndk::{call, n, parcel_words, P};

/// Maximum number of tasks, same as the java companion.
const MAX_TASKS: u32 = 30;
/// User 0 (the owner), the daemon runs as root and reads the owner recents.
const USER_ID: u32 = 0;

/// The packages of the recent tasks, most recent first (getTasks).
///
/// `known` are the packages of the apps the daemon has seen: a task also
/// contains strings like "android.intent.action.MAIN", which look like a
/// package name but are not one.
pub(crate) fn task_packages(task: P, code: u32, known: &[String]) -> Option<Vec<String>> {
    let o = call(task, code, |i| unsafe {
        let n = n();
        (n.AParcel_writeInt32)(i, MAX_TASKS as i32);
        (n.AParcel_writeInt32)(i, 1); // filterOnlyVisibleRecents
        (n.AParcel_writeInt32)(i, 0); // keepIntentExtra
        (n.AParcel_writeInt32)(i, 0); // displayId (default display)
    })?;
    let bytes = parcel_words(o);
    unsafe { (n().AParcel_delete)(o) };
    let bytes = bytes?;
    if std::env::var_os("AZ_DEBUG_RECENTS").is_some() {
        eprintln!("getTasks reply: {} bytes", bytes.len());
        eprintln!("candidates: {:?}", packages_in(&bytes));
        eprintln!("known: {known:?}");
    }
    debug!("getTasks reply: {} bytes", bytes.len());
    Some(known_packages_in(&bytes, known))
}

/// Fallback for older ROMs / ROMs without the getTasks transaction code.
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
    Some(known_packages_in(&bytes, known))
}

/// The package names of a task parcel that are in `known`, in parcel order.
fn known_packages_in(bytes: &[u8], known: &[String]) -> Vec<String> {
    let mut out: Vec<String> = Vec::new();
    for p in packages_in(bytes) {
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
    out
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
