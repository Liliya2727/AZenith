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

//! Refresh rate helpers.
use std::io::BufRead;

use crate::core::APP_MONITOR_FILE;
use crate::{log_info, log_warn};
use crate::shell::systemv;

// ---------------------------------------------------------------------------
// get_current_refresh_rate
// ---------------------------------------------------------------------------
pub fn get_current_refresh_rate() -> i32 {
    let Ok(fp) = std::fs::File::open(APP_MONITOR_FILE) else {
        return -1;
    };

    let mut refresh_rate = -1i32;
    for line in std::io::BufReader::new(fp).lines().flatten() {
        if let Some(rest) = line.strip_prefix("refresh_rate ") {
            refresh_rate = rest.trim().parse::<i32>().unwrap_or(-1);
            break;
        }
    }

    refresh_rate
}

// ---------------------------------------------------------------------------
// get_max_refresh_rate
// ---------------------------------------------------------------------------
pub fn get_max_refresh_rate() -> i32 {
    let Ok(fp) = std::fs::File::open(APP_MONITOR_FILE) else {
        log_warn!("refresh", "app monitor file missing path={} default_max=60Hz", APP_MONITOR_FILE);
        return 60;
    };

    let mut max_rr = 60i32;
    for line in std::io::BufReader::new(fp).lines().flatten() {
        if let Some(rest) = line.strip_prefix("max_refresh_rate ") {
            max_rr = rest.trim().parse::<i32>().unwrap_or(60);
            break;
        }
    }

    if max_rr > 0 { max_rr } else { 60 }
}

// ---------------------------------------------------------------------------
// apply_dynamic_refresh_rate
// ---------------------------------------------------------------------------
pub fn apply_dynamic_refresh_rate(target_rr: i32) {
    // No hardware-max capping (explicitly dropped per user request): the
    // value requested is applied as-is. sys.azenith-utilityconf clamps
    // unsupported rates itself if needed.
    log_info!("refresh", "refresh rate changed target={}Hz applied={}Hz", target_rr, target_rr);
    systemv(&format!("sys.azenith-utilityconf setrefreshrates {target_rr}"));
}
