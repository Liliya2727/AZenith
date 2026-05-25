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
mod chipsets;
mod profiles;

use std::env;  use std::path::Path; use std::process::Command;
use utils::*;
use profiles::*;

fn main() {
    let args: Vec<String> = env::args().collect();
    
    if args.len() > 1 {
        match args[1].as_str() {
            "0" | "initialize" => initialize(),
            "1" | "performance_profile" => performance_profile(),
            "2" | "balanced_profile" => balanced_profile(),
            "3" | "eco_mode" => eco_mode(),
            "applyfreqbalance" => applyfreqbalance(),
            "applyfreqgame" => applyfreqgame(),
            _ => {
                // Batasi eksekusi liar, pastikan argumennya valid sebelum spawn process
                if Path::new(&args[1]).exists() || args[1].contains('.') {
                    let _ = Command::new(&args[1])
                        .args(&args[2..])
                        .status();
                }
            }
        }
    }
}

