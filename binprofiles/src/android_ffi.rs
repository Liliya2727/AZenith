//! Android C bindings (liblog + bionic time helpers).
//!
//! Declarations only, mirroring `thermalcore/src/android_ffi.rs`. The logging
//! policy (paths, rotation, line shape) lives in `utils::logging` and follows
//! `archdaemon/jni/src/log/logger.c`.

use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int};

/// Priorities from `<android/log.h>`.
pub const ANDROID_LOG_DEBUG: c_int = 3;
pub const ANDROID_LOG_INFO: c_int = 4;
pub const ANDROID_LOG_WARN: c_int = 5;
pub const ANDROID_LOG_ERROR: c_int = 6;

#[link(name = "log")]
unsafe extern "C" {
    fn __android_log_print(prio: c_int, tag: *const c_char, fmt: *const c_char, ...) -> c_int;
}

/// Take/release the exclusive lock around a log file descriptor, matching the
/// daemon's `write2file(..., use_flock = true, ...)` contract so concurrent
/// writers can never interleave half a line.
pub fn lock_fd(fd: c_int) {
    unsafe { libc::flock(fd, libc::LOCK_EX) };
}

pub fn unlock_fd(fd: c_int) {
    unsafe { libc::flock(fd, libc::LOCK_UN) };
}

/// Mirror one line to logcat. The body goes through `%s` so a literal `%`
/// inside a message can never be treated as a format specifier.
pub fn logcat(prio: c_int, tag: &str, message: &str) {
    let Ok(c_tag) = CString::new(tag.replace('\0', "")) else { return };
    let Ok(c_msg) = CString::new(message.replace('\0', "")) else { return };
    let fmt = c"%s";
    unsafe {
        __android_log_print(prio, c_tag.as_ptr(), fmt.as_ptr(), c_msg.as_ptr());
    }
}

/// Local timestamp shaped exactly like the daemon's `timern()`:
/// `YYYY-MM-DD HH:MM:SS.mmm` (the `[TimeError]` / `[TimeFormatError]`
/// fallbacks match the C code as well).
pub fn timestamp() -> String {
    let mut ts = unsafe { std::mem::zeroed::<libc::timespec>() };
    if unsafe { libc::clock_gettime(libc::CLOCK_REALTIME, &raw mut ts) } != 0 {
        return String::from("[TimeError]");
    }

    let mut tm = unsafe { std::mem::zeroed::<libc::tm>() };
    if unsafe { libc::localtime_r(&raw const ts.tv_sec, &raw mut tm) }.is_null() {
        return String::from("[TimeError]");
    }

    let mut buf = [0 as c_char; 32];
    let fmt = c"%Y-%m-%d %H:%M:%S";
    if unsafe { libc::strftime(buf.as_mut_ptr(), buf.len(), fmt.as_ptr(), &raw const tm) } == 0 {
        return String::from("[TimeFormatError]");
    }

    let base = unsafe { CStr::from_ptr(buf.as_ptr()) }.to_string_lossy().into_owned();
    format!("{}.{:03}", base, ts.tv_nsec / 1_000_000)
}

/// Property value buffer limit from `<sys/system_properties.h>`.
const PROP_VALUE_MAX: usize = 92;

/// Read a system property through bionic (`__system_property_get` — the same
/// call the `getprop` binary makes). `None` when the property is unset or
/// empty, matching what `getprop <key>` prints for a missing property.
pub fn prop_get(name: &str) -> Option<String> {
    let c_name = CString::new(name).ok()?;
    let mut buf = [0 as c_char; PROP_VALUE_MAX];
    let len = unsafe { libc::__system_property_get(c_name.as_ptr(), buf.as_mut_ptr()) };
    if len <= 0 {
        return None;
    }
    Some(unsafe { CStr::from_ptr(buf.as_ptr()) }.to_string_lossy().into_owned())
}

/// Set a system property through bionic (`__system_property_set` — the same
/// call the `setprop` binary makes). Returns `true` on success. This cannot
/// delete a property or override an `ro.*` one; that is what the `resetprop`
/// binary is for.
pub fn prop_set(name: &str, value: &str) -> bool {
    let Ok(c_name) = CString::new(name) else { return false };
    let Ok(c_value) = CString::new(value) else { return false };
    unsafe { libc::__system_property_set(c_name.as_ptr(), c_value.as_ptr()) == 0 }
}
