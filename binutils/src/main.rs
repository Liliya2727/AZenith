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
use utils::*; use std::process::Command;

fn main() {
    let args: Vec<String> = env::args().collect();
    
    if args.len() > 1 {
        let function = args[1].as_str();
        match function {
            "setsgov" => if args.len() > 2 { setsgov(&args[2]) },
            "setsIO" => if args.len() > 2 { sets_io(&args[2]) },
            "setthermalcore" => if args.len() > 2 { setthermalcore(&args[2]) },
            "FSTrim" => fstrim(),
            "enableDND" => enable_dnd(),
            "disableDND" => disable_dnd(),
            "setrefreshrates" => if args.len() > 2 { setrefreshrates(&args[2]) },
            "restartservice" => restartservice(),
            "setrender" => if args.len() > 2 { setrender(&args[2]) },
            "saveLog" => savelog(),
            _ => {
                // Fallback to execute custom bash commands passed to it just in case
                let _ = Command::new(function).args(&args[2..]).status();
            }
        }
    }
}
