//! System property access and user configuration flags (`config.json` via `archconfig`).
//!
//! `getprop` / `setprop` go through bionic directly (see `crate::android_ffi`)
//! instead of spawning their binaries. `resetprop` still spawns, because only
//! the Magisk/KSU tool can delete properties and override `ro.*` ones.

use crate::android_ffi;
use crate::utils::logging::log_error;
use archconfig::AZenithConfig;
use std::fs;
use std::sync::OnceLock;

pub const CONFIG_PATH: &str = "/data/adb/.config/AZenith";

static CACHED_CFG: OnceLock<AZenithConfig> = OnceLock::new();

/// Access the cached `AZenithConfig` loaded once per one-shot invocation.
pub fn cfg() -> &'static AZenithConfig {
    CACHED_CFG.get_or_init(archconfig::load)
}

/// Returns the property value, or `""` when unset/empty.
pub fn getprop(key: &str) -> String {
    android_ffi::prop_get(key).unwrap_or_default()
}

pub fn resetprop(key: &str, val: &str) {
    use std::process::Command;

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
    if !android_ffi::prop_set(key, val) {
        log_error("prop", &format!("setprop failed key={} value={}", key, val));
    }
}

/// Returns the first non-empty string slice from `values`.
pub fn first_non_empty<'a>(values: &[&'a str]) -> String {
    values
        .iter()
        .copied()
        .find(|v| !v.is_empty())
        .unwrap_or_default()
        .to_string()
}

/// Returns the first non-empty string slice from `values`, falling back to `default`.
pub fn first_or(values: &[&str], default: &str) -> String {
    values
        .iter()
        .copied()
        .find(|v| !v.is_empty())
        .unwrap_or(default)
        .to_string()
}

/// Frequency limit in percent. `Disabled` or empty means 100.
pub fn get_limiter() -> u64 {
    let val = cfg().freq_offset.trim();
    if val.eq_ignore_ascii_case("Disabled") || val.is_empty() {
        100
    } else {
        val.replace('%', "").parse().unwrap_or(100)
    }
}

pub fn get_debugmode() -> bool {
    cfg().debug_mode
}

pub fn get_clearapps() -> bool {
    cfg().clear_bg
}

pub fn get_litemode() -> bool {
    getprop("sys.azenith.litemode") == "1"
}

pub fn get_curprofile() -> String {
    fs::read_to_string(format!("{}/API/current_profile", CONFIG_PATH))
        .unwrap_or_default()
        .trim()
        .to_string()
}

/// Master switch: `disable_tweak = true` disables every tweak.
pub fn is_tweak_disabled() -> bool {
    cfg().disable_tweak
}
