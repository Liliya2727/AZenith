//
// Copyright (C) 2026-2027 Zexshia
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

use std::ffi::CString;
use std::os::raw::c_char;
use std::fs::{self, OpenOptions};
use std::io::Write;
use std::process::Command;
use crate::utils::get_debugmode;

pub const LOG_FILE:     &str = "/data/adb/.config/AZenith/debug/AZenith.log";
pub const VERBOSE_FILE: &str = "/data/adb/.config/AZenith/debug/AZenithVerbose.log";
const     LOG_DIR:      &str = "/data/adb/.config/AZenith/debug";

/// Rotate once a file crosses this size, keeping LOG_KEEP_FILES generations
/// (<file>.1 .. <file>.N). Mirrors the C daemon's rotation policy.
const LOG_MAX_BYTES: u64 = 512 * 1024;
const LOG_KEEP_FILES: usize = 2;

const ANDROID_LOG_DEBUG: i32 = 3;
const ANDROID_LOG_INFO:  i32 = 4;
const ANDROID_LOG_WARN:  i32 = 5;
const ANDROID_LOG_ERROR: i32 = 6;

#[link(name = "log")]
unsafe extern "C" {
    fn __android_log_write(prio: i32, tag: *const c_char, text: *const c_char) -> i32;
}

fn android_log(priority: i32, tag: &str, message: &str) {
    let safe_tag = tag.replace('\0', "");
    let safe_msg = message.replace('\0', "");

    let Ok(c_tag) = CString::new(safe_tag) else { return };
    let Ok(c_msg) = CString::new(safe_msg) else { return };

    unsafe {
        __android_log_write(priority, c_tag.as_ptr(), c_msg.as_ptr());
    }
}

fn timestamp() -> String {
    Command::new("date")
        .arg("+%Y-%m-%d %H:%M:%S.%3N")
        .output()
        .map(|o| String::from_utf8_lossy(&o.stdout).trim().to_string())
        .unwrap_or_else(|_| String::from("0000-00-00 00:00:00.000"))
}

fn ensure_dir() {
    let _ = fs::create_dir_all(LOG_DIR);
}

/// Shift <path> -> <path>.1 -> <path>.2 once it exceeds LOG_MAX_BYTES. On any
/// failure we just keep appending; logging must never crash the utility.
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

fn append(path: &str, line: &str) {
    rotate_if_needed(path);
    if let Ok(mut f) = OpenOptions::new().create(true).append(true).open(path) {
        let _ = f.write_all(line.as_bytes());
    }
}

fn fmt_line(level: &str, component: &str, message: &str) -> String {
    format!("{} {} [{}] {}\n", timestamp(), level, component, message)
}

pub fn write_log(component: &str, message: &str) {
    ensure_dir();
    append(LOG_FILE, &fmt_line("INFO", component, message));
    android_log(ANDROID_LOG_INFO, component, message);
}

pub fn write_verbose(component: &str, message: &str) {
    ensure_dir();
    append(VERBOSE_FILE, &fmt_line("DEBUG", component, message));
    android_log(ANDROID_LOG_DEBUG, component, message);
}

pub fn write_warn(component: &str, message: &str) {
    ensure_dir();
    append(LOG_FILE, &fmt_line("WARN", component, message));
    android_log(ANDROID_LOG_WARN, component, message);
}

pub fn write_error(component: &str, message: &str) {
    ensure_dir();
    append(LOG_FILE, &fmt_line("ERROR", component, message));
    android_log(ANDROID_LOG_ERROR, component, message);
}

/// A significant, low-frequency event (start/stop/profile change/config reload).
pub fn log_info(component: &str, message: &str) {
    write_log(component, message);
}

/// DEBUG detail for an external component. Only emitted when debug mode is on;
/// routed to the verbose log so the main log stays readable.
pub fn log_debug(component: &str, message: &str) {
    if get_debugmode() {
        write_verbose(component, message);
    }
}

/// An oddity the caller recovered from; execution continues.
pub fn log_warn(component: &str, message: &str) {
    write_warn(component, message);
}

/// An operation failed and needs attention. Include path/value/error context.
pub fn log_error(component: &str, message: &str) {
    write_error(component, message);
}
