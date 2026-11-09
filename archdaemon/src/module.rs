//! Module integrity.

use crate::core::{MODULE_PROP};
use crate::log_fatal;
use crate::sysprop::set;
use crate::util::notify;
use std::fs;

fn read_module_prop_lines() -> Vec<String> {
    match fs::read_to_string(MODULE_PROP) {
        Ok(content) => content.lines().map(|l| l.to_string()).collect(),
        Err(_) => Vec::new(),
    }
}

fn line_starts_with(lines: &[String], prefix: &str) -> bool {
    lines.iter().any(|l| l.starts_with(prefix))
}

// ---------------------------------------------------------------------------
// is_kanged
// ---------------------------------------------------------------------------
pub fn is_kanged() {
    let lines = read_module_prop_lines();

    if !line_starts_with(&lines, "name=AZenith火") {
        doorprize();
    }
    if !line_starts_with(&lines, "author=ArchHaven Developers") {
        doorprize();
    }
}

fn doorprize() -> ! {
    log_fatal!("integrity", "module modified by 3rd party path={}", MODULE_PROP);
    notify("Daemon Error", "Trying to rename me?", true, 0, None);
    set("persist.sys.azenith.service", "");
    set("persist.sys.azenith.state", "stopped");
    std::process::exit(1);
}

// ---------------------------------------------------------------------------
// check_module_version
// ---------------------------------------------------------------------------
// module.prop's `version=` carries the full build version
// ("5.6.0 (123-abc-MoonShot-Beta)"), written by CI from `version` +
// git count/hash/type. The daemon embeds the identical string from
// `archdaemon/full_version.txt` (overwritten by .github/scripts/verify.sh
// at build time), so an exact match is possible instead of comparing
// the bare semver.
pub fn full_version() -> &'static str {
    include_str!("../full_version.txt").trim()
}

pub fn check_module_version() {
    let daemon_version = full_version();
    let expected = format!("version={daemon_version}");

    let lines = read_module_prop_lines();

    if !line_starts_with(&lines, &expected) {
        log_fatal!("integrity", "version mismatch expected={} path={}", daemon_version, MODULE_PROP);
        notify("Daemon Error", "AZenith version mismatch, please reinstall!", true, 0, None);
        set("persist.sys.azenith.service", "");
        set("persist.sys.azenith.state", "stopped");
        std::process::exit(1);
    }
}

// ---------------------------------------------------------------------------
// runthermalcore (also in util, but called from module context)
// ---------------------------------------------------------------------------
pub fn runthermalcore() {
    crate::util::runthermalcore();
}

// ---------------------------------------------------------------------------
// update_module_description
// ---------------------------------------------------------------------------
pub fn update_module_description(pid: i32) {
    crate::util::update_module_description(pid);
}
