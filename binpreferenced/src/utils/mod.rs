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

use std::os::unix::fs::PermissionsExt;
use std::fs;
use std::path::Path;
use std::process::{Command, Stdio};
use std::sync::atomic::{AtomicBool, Ordering};

static DEBUGMODE: AtomicBool = AtomicBool::new(false);

pub fn init_debugmode() {
    DEBUGMODE.store(get_debugmode(), Ordering::Relaxed);
}

pub fn get_debugmode() -> bool {
    getprop("persist.sys.azenith.debugmode") == "true"
}

pub fn debugmode() -> bool {
    DEBUGMODE.load(Ordering::Relaxed)
}

pub fn getprop(key: &str) -> String {
    if let Ok(output) = Command::new("getprop").arg(key).output() {
        String::from_utf8_lossy(&output.stdout).trim().to_string()
    } else {
        String::new()
    }
}

pub fn resetprop(key: &str, val: &str) {
    let _ = Command::new("resetprop")
        .arg("-n")
        .arg(key)
        .arg(val)
        .status();
}

pub fn log_verbose(message: &str) {
    if debugmode() {
        let _ = Command::new("sys.azenith-service")
            .args(["--verboselog", "AZenith_Prefs", "0", message])
            .status();
    }
}

pub fn log_info(message: &str) {
    let _ = Command::new("sys.azenith-service")
        .args(["--log", "AZenith_Prefs", "1", message])
        .status();
}

pub fn chmod(path: &str, mode: u32) {
    if let Ok(metadata) = fs::metadata(path) {
        let mut perms = metadata.permissions();
        perms.set_mode(mode);
        let _ = fs::set_permissions(path, perms);
    }
}

pub fn chmod_glob(pattern: &str, mode: u32) {
    if let Ok(paths) = glob::glob(pattern) {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                chmod(p_str, mode);
            }
        }
    }
}

pub fn write_val(value: &str, path_str: &str, lock: bool) -> bool {
    let path = Path::new(path_str);
    let parent_name = path.parent().and_then(|p| p.file_name()).unwrap_or_default().to_string_lossy();
    let file_name = path.file_name().unwrap_or_default().to_string_lossy();
    let pathname = if parent_name.is_empty() { file_name.into_owned() } else { format!("{}/{}", parent_name, file_name) };

    if !path.exists() {
        log_verbose(&format!("File /{} not found, skipping...", pathname));
        return false;
    }

    chmod(path_str, 0o644);

    let val_with_newline = format!("{}\n", value);

    if fs::write(path, val_with_newline).is_err() {
        log_verbose(&format!("Cannot write to /{} (permission denied)", pathname));
        if lock { chmod(path_str, 0o444); }
        return false;
    }

    log_verbose(&format!("Set /{} to {}", pathname, value));
    if lock { chmod(path_str, 0o444); }
    true
}

pub fn cmd_split(parts: &[&str]) {
    if parts.is_empty() {
        return;
    }
    let _ = Command::new(parts[0])
        .args(&parts[1..])
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .status();
}
