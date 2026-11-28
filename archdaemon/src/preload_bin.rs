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

//! Lightweight standalone test binary for `preload` (Stage 2 differential harness).
//!
//! Compiles only the pure `std + libc` preload modules without pulling in
//! Android-only `liblog` / bionic system-property FFI, so `cargo test --bin preload`
//! and `tests/preload_diff.sh` work on plain Linux hosts as well as Termux.

#[path = "preload/cli.rs"]
mod cli;
#[path = "preload/dedup.rs"]
mod dedup;
#[path = "preload/fileops.rs"]
mod fileops;
#[path = "preload/filters.rs"]
mod filters;
#[path = "preload/pages.rs"]
mod pages;
#[path = "preload/range.rs"]
mod range;
#[path = "preload/report.rs"]
mod report;
#[path = "preload/size.rs"]
mod size;
#[path = "preload/stats.rs"]
mod stats;
#[path = "preload/walker.rs"]
mod walker;

use std::env;
use std::ffi::OsString;
use std::path::Path;

fn main() {
    let args: Vec<OsString> = env::args_os().collect();
    let prog = args
        .first()
        .and_then(|s| Path::new(s).file_name())
        .and_then(|n| n.to_str())
        .unwrap_or("");

    let rc = if matches!(prog, "sys.azenith-preloadbin" | "preloadbin" | "vmtouch") {
        cli::run_cli(&args[1..], cli::CliMode::StrictCompat)
    } else if args.get(1).and_then(|s| s.to_str()) == Some("preload") {
        cli::run_cli(&args[2..], cli::CliMode::Subcommand)
    } else if prog == "preload-subcmd" {
        cli::run_cli(&args[1..], cli::CliMode::Subcommand)
    } else {
        cli::run_cli(&args[1..], cli::CliMode::StrictCompat)
    };

    std::process::exit(rc);
}
