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

mod utils;

use std::env;
use std::process::Command;
use std::fs;
use utils::*;

fn get_parent_pid() -> Option<u32> {
    fs::read_to_string("/proc/self/stat")
        .ok()
        .and_then(|stat| {
            stat.split_whitespace()
                .nth(3)
                .and_then(|ppid| ppid.parse::<u32>().ok())
        })
}

fn get_process_cmdline(pid: u32) -> Option<String> {
    fs::read_to_string(format!("/proc/{}/cmdline", pid))
        .ok()
        .map(|s| s.replace('\0', " ").trim().to_string())
}

fn verify_caller() -> bool {
    if let Some(ppid) = get_parent_pid() {
        if let Some(cmdline) = get_process_cmdline(ppid) {
            return cmdline.contains("sys.azenith-service") || cmdline.contains("sys.azenith");
        }
    }
    false
}

fn main() {
    let args: Vec<String> = env::args().collect();

    if !verify_caller() {
        eprintln!("\x1b[31mError: This utility can only be called by sys.azenith-service\x1b[0m");
        std::process::exit(1);
    }
    
    if args.len() > 1 {
        let function = args[1].as_str();
                match function {
            "setsgov" => if args.len() > 2 { setsgov(&args[2]) },
            "setsIO" => if args.len() > 2 { sets_io(&args[2]) },
            "setsMaliGov" => if args.len() > 2 { sets_mali_gov(&args[2]) },
            "setthermalcore" => if args.len() > 2 { setthermalcore(&args[2]) },
            "checkmalipath" => check_mali_path(),
            "FSTrim" => fstrim(),
            "enableDND" => enable_dnd(),
            "disableDND" => disable_dnd(),
            "setrefreshrates" => if args.len() > 2 { setrefreshrates(&args[2]) },
            "restartservice" => restartservice(),
            "setrender" => if args.len() > 2 { setrender(&args[2]) },
            _ => {
                let _ = Command::new(function).args(&args[2..]).status();
            }
        }
    }
}
