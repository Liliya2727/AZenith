//! Status log writer for the profiler.
//!
//! Writes straight into the daemon's rotating log files instead of shelling
//! out to `sys.azenith-service`, keeping the exact on-disk shape produced by
//! `archdaemon/jni/src/log/logger.c`:
//!
//! ```text
//! YYYY-MM-DD HH:MM:SS.mmm LEVEL [component] message
//! ```
//!
//! DEBUG lines go to the verbose log (only when debug mode is on); every other
//! level goes to the main log. Each line is mirrored to logcat.

use crate::android_ffi;
use crate::utils::props::get_debugmode;
use std::fs::{self, OpenOptions};
use std::io::Write;
use std::os::raw::c_int;
use std::os::unix::io::AsRawFd;

/// Main rotating log — the same file the daemon appends to.
const LOG_FILE: &str = "/data/adb/.config/AZenith/debug/AZenith.log";
/// Verbose log for DEBUG detail.
const VERBOSE_FILE: &str = "/data/adb/.config/AZenith/debug/AZenithVerbose.log";
const LOG_DIR: &str = "/data/adb/.config/AZenith/debug";

/// Logcat tag the daemon uses for companion binaries (`LOG_TAG_PROFILE`).
const LOGCAT_TAG: &str = "AZenith_Profiles";

/// Rotate once a file crosses this size, keeping `<file>.1 .. <file>.N`.
/// Mirrors `LOG_MAX_BYTES` / `LOG_KEEP_FILES` from `azenith/core.h`.
const LOG_MAX_BYTES: u64 = 512 * 1024;
const LOG_KEEP_FILES: usize = 2;

/// Shift <path> -> <path>.1 -> <path>.2 once it exceeds LOG_MAX_BYTES. On any
/// failure we just keep appending; logging must never take the profiler down.
fn rotate_if_needed(path: &str) {
    let Ok(meta) = fs::metadata(path) else { return };
    if meta.len() < LOG_MAX_BYTES {
        return;
    }

    for i in (1..=LOG_KEEP_FILES).rev() {
        let src = if i == 1 { path.to_string() } else { format!("{}.{}", path, i - 1) };
        let dst = format!("{}.{}", path, i);

        let _ = fs::remove_file(&dst);
        let _ = fs::rename(&src, &dst);
    }
}

/// Append one pre-formatted line, serialized against concurrent daemon
/// writers with an exclusive `flock`.
fn append(path: &str, line: &str) {
    rotate_if_needed(path);

    let Ok(mut file) = OpenOptions::new().create(true).append(true).open(path) else { return };
    let fd = file.as_raw_fd();
    android_ffi::lock_fd(fd);
    let _ = file.write_all(line.as_bytes());
    android_ffi::unlock_fd(fd);
}

/// Single funnel for every level, so the on-disk shape stays identical.
fn write_line(level: &str, prio: c_int, verbose: bool, component: &str, message: &str) {
    let _ = fs::create_dir_all(LOG_DIR);

    // Same fallback the daemon uses for empty component tags.
    let component = if component.is_empty() { "daemon" } else { component };

    let line = format!("{} {} [{}] {}\n", android_ffi::timestamp(), level, component, message);
    append(if verbose { VERBOSE_FILE } else { LOG_FILE }, &line);

    android_ffi::logcat(prio, LOGCAT_TAG, &format!("[{component}] {message}"));
}

/// DEBUG detail for an external component. Only emitted when debug mode is on;
/// routed to the verbose log so the main log stays readable.
pub fn log_debug(component: &str, message: &str) {
    if get_debugmode() {
        write_line("DEBUG", android_ffi::ANDROID_LOG_DEBUG, true, component, message);
    }
}

/// A significant, low-frequency event (start/stop/profile change/config reload).
pub fn log_info(component: &str, message: &str) {
    write_line("INFO", android_ffi::ANDROID_LOG_INFO, false, component, message);
}

/// An oddity the caller recovered from; execution continues.
pub fn log_warn(component: &str, message: &str) {
    write_line("WARN", android_ffi::ANDROID_LOG_WARN, false, component, message);
}

/// An operation failed and needs attention. Include path/value/error context.
pub fn log_error(component: &str, message: &str) {
    write_line("ERROR", android_ffi::ANDROID_LOG_ERROR, false, component, message);
}
