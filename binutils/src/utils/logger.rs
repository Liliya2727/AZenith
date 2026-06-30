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

fn append(path: &str, line: &str) {
    if let Ok(mut f) = OpenOptions::new().create(true).append(true).open(path) {
        let _ = f.write_all(line.as_bytes());
    }
}

fn fmt_line(level: &str, tag: &str, message: &str) -> String {
    let short_level = match level {
        "INFO" => "I",
        "DEBUG" => "D",
        "WARN" => "W",
        "ERROR" => "E",
        _ => level,
    };
    format!("{} {} {}: {}\n", timestamp(), short_level, tag, message)
}

pub fn write_log(tag: &str, message: &str) {
    ensure_dir();
    append(LOG_FILE, &fmt_line("INFO", tag, message));
    android_log(ANDROID_LOG_INFO, tag, message);
}

pub fn write_verbose(tag: &str, message: &str) {
    ensure_dir();
    append(VERBOSE_FILE, &fmt_line("DEBUG", tag, message));
    android_log(ANDROID_LOG_DEBUG, tag, message);
}

pub fn write_warn(tag: &str, message: &str) {
    ensure_dir();
    append(LOG_FILE, &fmt_line("WARN", tag, message));
    android_log(ANDROID_LOG_WARN, tag, message);
}

pub fn write_error(tag: &str, message: &str) {
    ensure_dir();
    append(LOG_FILE, &fmt_line("ERROR", tag, message));
    android_log(ANDROID_LOG_ERROR, tag, message);
}

pub fn log_info(message: &str) {
    write_log("AZenith_Utility", message);
}

pub fn verbose(message: &str) {
    if get_debugmode() {
        write_verbose("AZenith_Utility", message);
    }
}

pub fn log_warn(message: &str) {
    write_warn("AZenith_Utility", message);
}

pub fn log_error(message: &str) {
    write_error("AZenith_Utility", message);
}