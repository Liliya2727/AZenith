//! Logging, mirroring `jni/src/log/logger.c`.

use crate::sysprop::get_or_default;
use crate::android_ffi::{ANDROID_LOG_DEBUG, ANDROID_LOG_ERROR, ANDROID_LOG_FATAL, ANDROID_LOG_INFO, ANDROID_LOG_WARN, logcat};
use crate::core::{
    LogLevel, LOG_FILE, LOG_FILE_PRELOAD, LOG_MAX_BYTES, LOG_KEEP_FILES, LOG_TAG, LOG_TAG_PROFILE, LOG_VFILE,
};
use crate::io::write2file;
use crate::util::timern;

use std::fs;

fn safe_component(component: &str) -> &str {
    if component.is_empty() { "daemon" } else { component }
}

fn level_str(level: LogLevel) -> &'static str {
    match level {
        LogLevel::Debug => "DEBUG",
        LogLevel::Info => "INFO",
        LogLevel::Warn => "WARN",
        LogLevel::Error => "ERROR",
        LogLevel::Fatal => "FATAL",
    }
}

fn rotate_log_if_needed(path: &str) {
    let Ok(metadata) = fs::metadata(path) else { return };
    if metadata.len() < LOG_MAX_BYTES as u64 {
        return;
    }

    for i in (1..=LOG_KEEP_FILES).rev() {
        let src = if i == 1 {
            path.to_string()
        } else {
            format!("{path}.{}", i - 1)
        };
        let dst = format!("{path}.{i}");

        let _ = fs::remove_file(&dst);
        let _ = fs::rename(&src, &dst);
    }
}

fn write_line(path: &str, level: LogLevel, component: &str, body: &str) {
    let _ = rotate_log_if_needed(path);
    let timestamp = timern();
    let comp = safe_component(component);
    let line = format!("{} {} [{}] {}\n", timestamp, level_str(level), comp, body);
    let _ = write2file(path, true, true, &line);
}

/// Core file+logcat writer.
pub fn log_write(level: LogLevel, component: &str, _message: &str, args: std::fmt::Arguments<'_>) {
    let body = format!("{args}");
    write_line(LOG_FILE, level, component, &body);
    let comp = safe_component(component);
    logcat(level.as_c_int(), LOG_TAG, &format!("[{comp}] {body}"));
}

pub fn log_debug(component: &str, _message: &str, args: std::fmt::Arguments<'_>) {
    if !debug_enabled() {
        return;
    }
    let body = format!("{args}");
    write_line(LOG_VFILE, LogLevel::Debug, component, &body);
    let comp = safe_component(component);
    logcat(ANDROID_LOG_DEBUG, LOG_TAG, &format!("[{comp}] {body}"));
}

pub fn log_info(component: &str, _message: &str, args: std::fmt::Arguments<'_>) {
    let body = format!("{args}");
    write_line(LOG_FILE, LogLevel::Info, component, &body);
    let comp = safe_component(component);
    logcat(ANDROID_LOG_INFO, LOG_TAG, &format!("[{comp}] {body}"));
}

pub fn log_warn(component: &str, _message: &str, args: std::fmt::Arguments<'_>) {
    let body = format!("{args}");
    write_line(LOG_FILE, LogLevel::Warn, component, &body);
    let comp = safe_component(component);
    logcat(ANDROID_LOG_WARN, LOG_TAG, &format!("[{comp}] {body}"));
}

pub fn log_error(component: &str, _message: &str, args: std::fmt::Arguments<'_>) {
    let body = format!("{args}");
    write_line(LOG_FILE, LogLevel::Error, component, &body);
    let comp = safe_component(component);
    logcat(ANDROID_LOG_ERROR, LOG_TAG, &format!("[{comp}] {body}"));
}

pub fn log_fatal(component: &str, _message: &str, args: std::fmt::Arguments<'_>) {
    let body = format!("{args}");
    write_line(LOG_FILE, LogLevel::Fatal, component, &body);
    let comp = safe_component(component);
    logcat(ANDROID_LOG_FATAL, LOG_TAG, &format!("[{comp}] {body}"));
}

/// Preload-pipeline logging (only when debug mode is on).
pub fn log_preload(component: &str, _message: &str, args: std::fmt::Arguments<'_>) {
    if !debug_enabled() {
        return;
    }
    let body = format!("{args}");
    write_line(LOG_FILE_PRELOAD, LogLevel::Debug, component, &body);
    let comp = safe_component(component);
    logcat(ANDROID_LOG_DEBUG, LOG_TAG, &format!("[{comp}] {body}"));
}

/// External logging interface for companion binaries.
pub fn external_log(level: i32, tag: &str, _message: &str, args: std::fmt::Arguments<'_>) {
    let body = format!("{args}");
    let lvl = match level {
        0 => LogLevel::Debug,
        1 => LogLevel::Info,
        2 => LogLevel::Warn,
        3 => LogLevel::Error,
        4 => LogLevel::Fatal,
        _ => LogLevel::Info,
    };

    if lvl == LogLevel::Debug {
        write_line(LOG_VFILE, lvl, tag, &body);
    } else {
        write_line(LOG_FILE, lvl, tag, &body);
    }

    let comp = safe_component(tag);
    logcat(lvl.as_c_int(), LOG_TAG_PROFILE, &format!("[{comp}] {body}"));
}

/// External verbose logging interface for companion binaries.
pub fn external_vlog(_level: i32, tag: &str, _message: &str, args: std::fmt::Arguments<'_>) {
    let body = format!("{args}");
    write_line(LOG_VFILE, LogLevel::Info, tag, &body);
    let comp = safe_component(tag);
    logcat(ANDROID_LOG_INFO, LOG_TAG_PROFILE, &format!("[{comp}] {body}"));
}

fn debug_enabled() -> bool {
    get_or_default("persist.sys.azenith.debugmode", "false").as_str() == "true"
}

// ---------------------------------------------------------------------------
// Convenience macros for call sites (mirror C variadic style)
// ---------------------------------------------------------------------------

#[macro_export]
macro_rules! log_debug {
    ($comp:expr, $fmt:expr $(,)?) => {
        crate::log::log_debug($comp, $fmt, format_args!($fmt))
    };
    ($comp:expr, $fmt:expr, $($arg:tt)*) => {
        crate::log::log_debug($comp, $fmt, format_args!($fmt, $($arg)*))
    };
}

#[macro_export]
macro_rules! log_info {
    ($comp:expr, $fmt:expr $(,)?) => {
        crate::log::log_info($comp, $fmt, format_args!($fmt))
    };
    ($comp:expr, $fmt:expr, $($arg:tt)*) => {
        crate::log::log_info($comp, $fmt, format_args!($fmt, $($arg)*))
    };
}

#[macro_export]
macro_rules! log_warn {
    ($comp:expr, $fmt:expr $(,)?) => {
        crate::log::log_warn($comp, $fmt, format_args!($fmt))
    };
    ($comp:expr, $fmt:expr, $($arg:tt)*) => {
        crate::log::log_warn($comp, $fmt, format_args!($fmt, $($arg)*))
    };
}

#[macro_export]
macro_rules! log_error {
    ($comp:expr, $fmt:expr $(,)?) => {
        crate::log::log_error($comp, $fmt, format_args!($fmt))
    };
    ($comp:expr, $fmt:expr, $($arg:tt)*) => {
        crate::log::log_error($comp, $fmt, format_args!($fmt, $($arg)*))
    };
}

#[macro_export]
macro_rules! log_fatal {
    ($comp:expr, $fmt:expr $(,)?) => {
        crate::log::log_fatal($comp, $fmt, format_args!($fmt))
    };
    ($comp:expr, $fmt:expr, $($arg:tt)*) => {
        crate::log::log_fatal($comp, $fmt, format_args!($fmt, $($arg)*))
    };
}

#[macro_export]
macro_rules! log_preload {
    ($comp:expr, $fmt:expr $(,)?) => {
        crate::log::log_preload($comp, $fmt, format_args!($fmt))
    };
    ($comp:expr, $fmt:expr, $($arg:tt)*) => {
        crate::log::log_preload($comp, $fmt, format_args!($fmt, $($arg)*))
    };
}
