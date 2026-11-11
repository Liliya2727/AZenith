//! Build-time version define, à la C `#define MODULE_VERSION`.
//!
//! The checked-in default is a placeholder (`1.0.0`); CI's
//! `.github/scripts/verify.sh` rewrites this line to the full build
//! version (`"5.6.0 (123-abc-MoonShot-Beta)"`) from `version` +
//! git count/hash before `cargo ndk build`, so `check_module_version`
//! can diff the daemon against `module.prop` exactly.

pub const FULL_VERSION: &str = "1.0.0";
