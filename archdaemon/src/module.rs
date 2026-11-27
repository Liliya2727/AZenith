// Copyright (C) 2026-2027 Zexshia
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
    set("sys.azenith.service", "");
    set("sys.azenith.state", "stopped");
    std::process::exit(1);
}

// ---------------------------------------------------------------------------
// check_module_version
// ---------------------------------------------------------------------------
// module.prop's `version=` carries the full build version
// ("5.6.0 (123-abc-MoonShot-Beta)"), written by CI from `version` +
// git count/hash/type into `src/version.rs` (like the old C
// `#define MODULE_VERSION`), so an exact match is possible instead of
// comparing the bare semver.
pub fn full_version() -> &'static str {
    crate::version::FULL_VERSION
}

pub fn check_module_version() {
    let daemon_version = full_version();
    let expected = format!("version={daemon_version}");

    let lines = read_module_prop_lines();

    if !line_starts_with(&lines, &expected) {
        log_fatal!("integrity", "version mismatch expected={} path={}", daemon_version, MODULE_PROP);
        notify("Daemon Error", "AZenith version mismatch, please reinstall!", true, 0, None);
        set("sys.azenith.service", "");
        set("sys.azenith.state", "stopped");
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
