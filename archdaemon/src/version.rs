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

//! Build-time version define, à la C `#define MODULE_VERSION`.
//!
//! The checked-in default is a placeholder (`1.0.0`); CI's
//! `.github/scripts/verify.sh` rewrites this line to the full build
//! version (`"5.6.0 (123-abc-MoonShot-Beta)"`) from `version` +
//! git count/hash before `cargo ndk build`, so `check_module_version`
//! can diff the daemon against `module.prop` exactly.

pub const FULL_VERSION: &str = "1.0.0";
