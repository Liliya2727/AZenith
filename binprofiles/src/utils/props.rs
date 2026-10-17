//! System property access and user configuration flags.

use crate::utils::logging::log_error;
use std::fs;
use std::process::Command;

pub const CONFIG_PATH: &str = "/data/adb/.config/AZenith";

pub fn getprop(key: &str) -> String {
    if let Ok(output) = Command::new("getprop").arg(key).output() {
        String::from_utf8_lossy(&output.stdout).trim().to_string()
    } else {
        String::new()
    }
}

pub fn resetprop(key: &str, val: &str) {
    let status = if val.is_empty() {
        Command::new("resetprop").arg("--delete").arg(key).status()
    } else {
        Command::new("resetprop").arg(key).arg(val).status()
    };

    if let Err(e) = status {
        log_error("prop", &format!("resetprop failed key={} err={}", key, e));
    }
}

pub fn setprop(key: &str, val: &str) {
    if let Err(e) = Command::new("setprop").arg(key).arg(val).status() {
        log_error("prop", &format!("setprop failed key={} value={} err={}", key, val, e));
    }
}

pub fn setprop_cmd(key: &str, value: &str) {
    let _ = Command::new("setprop").arg(key).arg(value).status();
}

/// Returns the first non-empty property value, following the fallback chain order.
///
/// Every profile uses this to resolve its CPU governor, I/O scheduler and
/// Mali GPU governor with the same pattern.
pub fn prop_first(keys: &[&str]) -> String {
    keys.iter()
        .map(|key| getprop(key))
        .find(|value| !value.is_empty())
        .unwrap_or_default()
}

/// Property fallback chain, using `default` when every property is empty.
pub fn prop_or(keys: &[&str], default: &str) -> String {
    let value = prop_first(keys);
    if value.is_empty() {
        default.to_string()
    } else {
        value
    }
}

/// Frequency limit in percent. `Disabled` or empty means 100.
pub fn get_limiter() -> u64 {
    let val = getprop("persist.sys.azenithconf.freqoffset");
    if val == "Disabled" || val.is_empty() {
        100
    } else {
        val.replace('%', "").parse().unwrap_or(100)
    }
}

pub fn get_debugmode() -> bool {
    getprop("persist.sys.azenith.debugmode") == "true"
}

pub fn get_clearapps() -> bool {
    getprop("persist.sys.azenithconf.clearbg") == "1"
}

pub fn get_litemode() -> bool {
    getprop("persist.sys.azenithconf.litemode") == "1"
}

pub fn get_curprofile() -> String {
    fs::read_to_string(format!("{}/API/current_profile", CONFIG_PATH))
        .unwrap_or_default()
        .trim()
        .to_string()
}

/// Master switch: `persist.sys.azenith.disabletweak = 1` disables every tweak.
pub fn is_tweak_disabled() -> bool {
    let disable_tweak = getprop("persist.sys.azenith.disabletweak");
    disable_tweak == "1"
}
