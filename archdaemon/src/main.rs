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

//! AZenith Service Daemon — Rust rewrite of archdaemon.
//! CLI entry point.
//!
//! Many functions/constants are part of the public API surface but not
//! directly referenced within the crate itself (they are called from main or
//! used as FFI bindings), so dead code warnings are suppressed at crate level.
#![allow(dead_code)]

mod android_ffi;
mod bypass;
mod cli;
mod config;
mod core;
mod daemon;
mod fileops;
mod io;
mod ipc;
mod log;
mod module;
mod monitor;
mod profile;
mod preload;
mod proc;
mod recorder;
mod shell;
mod startup;
mod sysprop;
mod util;
mod version;

use crate::core::init_globals;
use crate::bypass::{
    check_bypass_compatibility, print_bypass_path_list, restore_pending, test_bypass_node_by_name,
};
use crate::cli::{
    clearlogs, dump_refresh_rate_modes, expose_telemetry_access,
    grant_app_permissions, handle_config_get, handle_config_set, handle_log,
    handle_prelaunch, handle_verboselog, handle_profile, hidenotifications, print_help,
    printversion, require_daemon_running, restart_service, shownotifications,
};
use crate::daemon::main_daemon;
use crate::shell::systemv;

use std::env;
use std::os::raw::c_int;

macro_rules! is_cmd {
    ($arg:expr, $long:expr, $short:expr) => {
        $arg == $long || $arg == $short
    };
}

fn real_main() -> c_int {
    let os_args: Vec<std::ffi::OsString> = env::args_os().collect();
    let prog = os_args
        .first()
        .and_then(|s| std::path::Path::new(s).file_name())
        .and_then(|n| n.to_str())
        .unwrap_or("");

    // Multicall compatibility shim for `sys.azenith-preloadbin` / `preloadbin` / `vmtouch`.
    if matches!(prog, "sys.azenith-preloadbin" | "preloadbin" | "vmtouch") {
        return crate::preload::cli::run_cli(
            &os_args[1..],
            crate::preload::cli::CliMode::StrictCompat,
        );
    }

    // Native `preload` subcommand (does not require root or daemon running).
    if os_args.get(1).and_then(|s| s.to_str()) == Some("preload") {
        return crate::preload::cli::run_cli(
            &os_args[2..],
            crate::preload::cli::CliMode::Subcommand,
        );
    }

    let args: Vec<String> = os_args
        .iter()
        .map(|s| s.to_string_lossy().into_owned())
        .collect();

    if unsafe { libc::getuid() } != 0 {
        eprintln!("\u{1b}[31mERROR:\u{1b}[0m Please run this program as root");
        return 1;
    }

    init_globals();

    if args.len() as i32 == 1 || is_cmd!(args[1], "--help", "-h") {
        print_help();
        return 0;
    }

    let cmd = &args[1];

    if is_cmd!(cmd, "--appactivity", "-actv") {
        open_app_main_activity();
        return 0;
    }
    if is_cmd!(cmd, "--run", "-r") {
        return main_daemon();
    }
    if is_cmd!(cmd, "--version", "-V") {
        printversion();
        return 0;
    }
    if is_cmd!(cmd, "--clearlogs", "-c") {
        clearlogs();
        return 0;
    }
    if is_cmd!(cmd, "--hidenotifications", "-hn") {
        hidenotifications();
        return 0;
    }
    if is_cmd!(cmd, "--shownotifications", "-sn") {
        shownotifications();
        return 0;
    }
    if is_cmd!(cmd, "--bypasspathlist", "-bpl") {
        print_bypass_path_list();
        return 0;
    }
    // Must not require the daemon: it is the recovery path for a dead/stuck state.
    if is_cmd!(cmd, "--restorepending", "-rp") {
        return restore_pending();
    }
    if is_cmd!(cmd, "--rerun", "-rr") {
        return restart_service();
    }
    if is_cmd!(cmd, "--setup-access", "-sa") {
        expose_telemetry_access();
        grant_app_permissions();
        return 0;
    }
    if is_cmd!(cmd, "--get_rr_modes", "-getrr") {
        return dump_refresh_rate_modes();
    }
    if cmd == "--config-get" {
        return handle_config_get(
            args.len() as i32,
            args.iter().map(|s| s.as_str()).collect::<Vec<&str>>().as_slice(),
        );
    }
    if cmd == "--config-set" {
        return handle_config_set(
            args.len() as i32,
            args.iter().map(|s| s.as_str()).collect::<Vec<&str>>().as_slice(),
        );
    }

    // Commands below require the daemon to be running
    if !require_daemon_running() {
        return 1;
    }

    if is_cmd!(cmd, "--profile", "-p") {
        return handle_profile(args.len() as i32, args.iter().map(|s| s.as_str()).collect::<Vec<&str>>().as_slice());
    }
    if is_cmd!(cmd, "--prelaunch", "-pl") {
        return handle_prelaunch(args.len() as i32, args.iter().map(|s| s.as_str()).collect::<Vec<&str>>().as_slice());
    }
    if is_cmd!(cmd, "--log", "-l") {
        return handle_log(args.len() as i32, args.iter().map(|s| s.as_str()).collect::<Vec<&str>>().as_slice());
    }
    if is_cmd!(cmd, "--verboselog", "-vl") {
        return handle_verboselog(args.len() as i32, args.iter().map(|s| s.as_str()).collect::<Vec<&str>>().as_slice());
    }
    if is_cmd!(cmd, "--checkbypasschg", "-cbc") {
        return check_bypass_compatibility();
    }
    if is_cmd!(cmd, "--testbypass", "-tb") {
        if (args.len() as i32) < 3 {
            eprintln!("\u{1b}[31mERROR:\u{1b}[0m Missing node name. Use: --testbypass <NODE_NAME> or --testbypass --path=<NODE_NAME>");
            return 1;
        }
        let mut name: &str = &args[2];
        if name.starts_with("--path=") {
            name = &name[7..];
        }
        return test_bypass_node_by_name(name);
    }

    eprintln!("\u{1b}[31mERROR:\u{1b}[0m Unknown command: {cmd}");
    1
}

fn open_app_main_activity() {
    systemv("/system/bin/am start -a android.intent.action.MAIN zx.azenith/.MainActivity");
}

fn main() {
    std::process::exit(real_main());
}
