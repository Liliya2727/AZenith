//! Startup logic.

use crate::android_ffi::{__system_property_foreach, system_property_read_callback};
use crate::core::{
    DaemonContext, GAMELIST, JAVA_LOCK_PATH,
};
use crate::io::{check_running_state, is_java_lock_held};
use crate::{log_error, log_fatal, log_info, log_warn};
use crate::module::{check_module_version, is_kanged};
use crate::profile::init_screenstate_fn;
use crate::shell::systemv;
use crate::sysprop::{get, set};
use crate::fileops;
use crate::util::notify;

use archconfig::{AZenithConfig, CONFIG_PATH, parse_bool_flexible};
use std::ffi::CStr;
use std::os::raw::{c_char, c_void};
use std::path::Path;
use std::thread;
use std::time::Duration;

// ---------------------------------------------------------------------------
// validateprop & migrate_props_to_json_once
// ---------------------------------------------------------------------------

const AZENITH_PROPERTIES: &str = "persist.sys.azenith";

const VALID_AZENITH_PROPS: [&str; 48] = [
    "persist.sys.azenith.custom_default_balanced_IO",
    "persist.sys.azenith.custom_default_cpu_gov",
    "persist.sys.azenith.custom_default_maligpu_gov",
    "persist.sys.azenith.custom_performance_IO",
    "persist.sys.azenith.custom_performance_cpu_gov",
    "persist.sys.azenith.custom_performance_maligpu_gov",
    "persist.sys.azenith.custom_powersave_IO",
    "persist.sys.azenith.custom_powersave_cpu_gov",
    "persist.sys.azenith.custom_powersave_maligpu_gov",
    "persist.sys.azenith.debugmode",
    "persist.sys.azenith.default_balanced_IO",
    "persist.sys.azenith.default_cpu_gov",
    "persist.sys.azenith.default_maligpu_gov",
    "persist.sys.azenith.disabletweak",
    "persist.sys.azenith.service",
    "persist.sys.azenith.soctype",
    "persist.sys.azenith.state",
    "persist.sys.azenith.profilenotifications",
    "persist.sys.azenith.dropforeground",
    "persist.sys.azenith.force_sidepanel",
    "persist.sys.azenithconf.AIenabled",
    "persist.sys.azenithconf.APreload",
    "persist.sys.azenithconf.DThermal",
    "persist.sys.azenithconf.SFL",
    "persist.sys.azenithconf.bypasschg",
    "persist.sys.azenithconf.bypasschgthreshold",
    "persist.sys.azenithconf.renderer",
    "persist.sys.azenithconf.bypasspath",
    "persist.sys.azenithconf.clearbg",
    "persist.sys.azenithconf.cpulimit",
    "persist.sys.azenithconf.disabletrace",
    "persist.sys.azenithconf.dnd",
    "persist.sys.azenithconf.fpsged",
    "persist.sys.azenithconf.freqoffset",
    "persist.sys.azenithconf.fstrim",
    "persist.sys.azenithconf.iosched",
    "persist.sys.azenithconf.justintime",
    "persist.sys.azenithconf.litemode",
    "persist.sys.azenithconf.logd",
    "persist.sys.azenithconf.malisched",
    "persist.sys.azenithconf.preloadbudget",
    "persist.sys.azenithconf.schedtunes",
    "persist.sys.azenithconf.schemeconfig",
    "persist.sys.azenithconf.showtoast",
    "persist.sys.azenithconf.thermalcore",
    "persist.sys.azenithconf.usefpsgo",
    "persist.sys.azenithconf.walttunes",
    "persist.sys.azenithdebug.soctype",
];

const LEGACY_STALE_PROPS: [&str; 2] = [
    "persist.sys.rianixia.learning_enabled",
    "persist.sys.rianixia.thermalcore-bigdata.path",
];

extern "C" fn collect_all_azenith_props_read_cb(
    cookie: *mut c_void,
    name: *const c_char,
    value: *const c_char,
    _serial: u32,
) {
    if cookie.is_null() || name.is_null() {
        return;
    }
    let pending: &mut Vec<(String, String)> = unsafe { &mut *(cookie as *mut Vec<(String, String)>) };
    let name_str = unsafe { CStr::from_ptr(name) }.to_string_lossy();
    if !name_str.starts_with(AZENITH_PROPERTIES) {
        return;
    }
    let val_str = if value.is_null() {
        String::new()
    } else {
        unsafe { CStr::from_ptr(value) }.to_string_lossy().into_owned()
    };
    pending.push((name_str.into_owned(), val_str));
}

extern "C" fn collect_all_azenith_props_foreach_cb(pi: *const c_void, cookie: *mut c_void) {
    if pi.is_null() || cookie.is_null() {
        return;
    }
    unsafe {
        system_property_read_callback(pi, collect_all_azenith_props_read_cb as *mut c_void, cookie);
    }
}

/// Migrate all `persist.sys.azenith*` properties and legacy config files
/// (`freqoffset`, `bypasschgconfig/*`) into `/data/adb/.config/AZenith/config.json`
/// if `config.json` does not exist yet.
/// Also moves runtime props (`state`, `service`, `litemode`) to `sys.azenith.*`
/// and deletes every `persist.sys.azenith*` property via `resetprop -p --delete`.
pub fn migrate_props_to_json_once() {
    let config_exists = Path::new(CONFIG_PATH).exists();

    let mut collected: Vec<(String, String)> = Vec::new();
    let status = unsafe {
        __system_property_foreach(
            collect_all_azenith_props_foreach_cb as *mut c_void,
            &mut collected as *mut Vec<(String, String)> as *mut c_void,
        )
    };
    if status != 0 {
        log_warn!("prop", "prop foreach failed during migration status={}", status);
    }

    // Move runtime state/service/litemode from persist.sys.azenith* to sys.azenith.* if present
    for (name, val) in &collected {
        if val.is_empty() {
            continue;
        }
        match name.as_str() {
            "persist.sys.azenith.state" => {
                if get("sys.azenith.state").unwrap_or_default().is_empty() {
                    set("sys.azenith.state", val);
                }
            }
            "persist.sys.azenith.service" => {
                if get("sys.azenith.service").unwrap_or_default().is_empty() {
                    set("sys.azenith.service", val);
                }
            }
            "persist.sys.azenithconf.litemode" => {
                if get("sys.azenith.litemode").unwrap_or_default().is_empty() {
                    set("sys.azenith.litemode", val);
                }
            }
            _ => {}
        }
    }

    if !config_exists {
        let mut cfg = AZenithConfig::default();
        let mut imported_props = 0usize;

        for (name, val) in &collected {
            if cfg.apply_legacy_prop(name, val) {
                imported_props += 1;
            }
        }

        // Also check direct get() for VALID_AZENITH_PROPS in case foreach missed any
        for &prop in VALID_AZENITH_PROPS.iter() {
            if let Some(val) = get(prop) {
                if !collected.iter().any(|(n, _)| n == prop) && cfg.apply_legacy_prop(prop, &val) {
                    imported_props += 1;
                }
            }
        }

        // Import legacy file configs (authoritative if non-empty)
        if let Ok(s) = fileops::read_string_result("/data/adb/.config/AZenith/freqoffset") {
            let trimmed = s.trim();
            if !trimmed.is_empty() {
                cfg.freq_offset = trimmed.to_string();
            }
        }
        if let Ok(s) = fileops::read_string_result("/data/adb/.config/AZenith/bypasschgconfig/bypasspath") {
            let trimmed = s.trim();
            if !trimmed.is_empty() {
                cfg.standby_path = trimmed.to_string();
            }
        }
        if let Ok(s) = fileops::read_string_result("/data/adb/.config/AZenith/bypasschgconfig/bypasschg") {
            let trimmed = s.trim();
            if let Some(b) = parse_bool_flexible(trimmed) {
                cfg.standby_chg = b;
            }
        }

        match archconfig::save(&cfg) {
            Ok(_) => {
                log_info!(
                    "config",
                    "migrated props and legacy files to config.json imported_props={}",
                    imported_props
                );
            }
            Err(e) => {
                log_error!("config", "failed to write config.json during migration err={}", e);
                return;
            }
        }
    }

    // Delete all persist.sys.azenith* properties once config.json is in place
    let mut to_delete: Vec<String> = collected.into_iter().map(|(n, _)| n).collect();
    for &legacy in LEGACY_STALE_PROPS.iter() {
        if get(legacy).is_some() && !to_delete.iter().any(|n| n == legacy) {
            to_delete.push(legacy.to_string());
        }
    }

    if !to_delete.is_empty() {
        log_info!("prop", "deleting legacy persist.sys.azenith* props count={}", to_delete.len());
        for name in to_delete {
            let cmd = format!("resetprop -p --delete {}", name);
            let status = systemv(&cmd);
            if status != 0 {
                log_error!("prop", "failed to delete legacy prop name={} status={}", name, status);
            } else {
                log_info!("prop", "deleted legacy prop name={}", name);
            }
        }
    }
}

// Bionic's `__system_property_foreach` callback is
// `(const prop_info*, void* cookie)`; the name/value pair must be read with
// `__system_property_read_callback`.
extern "C" fn validateprop_read_cb(
    cookie: *mut c_void,
    name: *const c_char,
    _value: *const c_char,
    _serial: u32,
) {
    if cookie.is_null() || name.is_null() {
        return;
    }
    let pending: &mut Vec<String> = unsafe { &mut *(cookie as *mut Vec<String>) };
    let c_name = unsafe { CStr::from_ptr(name) };
    let name_str = c_name.to_string_lossy();

    if !is_stale_azenith_prop(&name_str) {
        return;
    }
    pending.push(name_str.into_owned());
}

extern "C" fn validateprop_foreach_cb(pi: *const c_void, cookie: *mut c_void) {
    if pi.is_null() || cookie.is_null() {
        return;
    }
    unsafe {
        system_property_read_callback(pi, validateprop_read_cb as *mut c_void, cookie);
    }
}

pub fn validateprop() {
    migrate_props_to_json_once();
}

// ---------------------------------------------------------------------------
// verify_system_integrity
// ---------------------------------------------------------------------------
pub fn verify_system_integrity() {
    if !check_running_state() {
        eprintln!("\u{1b}[31mERROR:\u{1b}[0m Daemon is already running!");
        std::process::exit(1);
    }

    let _ = std::fs::File::create(crate::core::PROFILE_MODE_APP);
    let _ = std::fs::File::create(crate::core::GAME_INFO_APP);

    if is_file_empty("/system/bin/dumpsys") == 1 {
        eprintln!("\u{1b}[31mFATAL ERROR:\u{1b}[0m /system/bin/dumpsys was tampered by kill logger module.");
        log_fatal!("startup", "dumpsys tampered path=/system/bin/dumpsys");
        notify("Daemon Error", "Please remove your stupid kill logger module.", false, 0, None);
        std::process::exit(1);
    }

    if fileops::size(GAMELIST).is_none() {
        eprintln!("\u{1b}[31mFATAL ERROR:\u{1b}[0m Unable to access Gamelist, either has been removed or moved.");
        log_fatal!("startup", "critical file missing path={}", GAMELIST);
        std::process::exit(1);
    }

    is_kanged();
    check_module_version();
}

fn is_file_empty(path: &str) -> i32 {
    match std::fs::metadata(path) {
        Ok(m) if m.len() == 0 => 1,
        Ok(_) => 0,
        Err(_) => -1,
    }
}

// ---------------------------------------------------------------------------
// wait_for_java_companion
// ---------------------------------------------------------------------------
pub fn wait_for_java_companion(ctx: &mut DaemonContext) {
    log_info!("startup", "waiting for java companion");
    let mut java_check_retries = 0i32;
    const MAX_JAVA_RETRIES: i32 = 120;

    let lock_path = JAVA_LOCK_PATH;
    ctx.java_lock_path = lock_path.to_string();

    while !is_java_lock_held(lock_path) {
        java_check_retries += 1;
        if java_check_retries > MAX_JAVA_RETRIES {
            log_fatal!("startup", "java companion absent retries={}", MAX_JAVA_RETRIES);
            notify("Daemon Error", "Java companion daemon crashed or failed to start.", false, 0, None);
            set("sys.azenith.service", "");
            set("sys.azenith.state", "stopped");
            std::process::exit(1);
        }
        if java_check_retries <= 1 {
            log_warn!("startup", "java companion lock not held retry={}", java_check_retries);
        }
        thread::sleep(Duration::from_secs(1));
    }
    log_info!("startup", "java companion detected");
}

// ---------------------------------------------------------------------------
// init_daemon_context
// ---------------------------------------------------------------------------
pub fn init_daemon_context(ctx: &mut DaemonContext) {
    *ctx = DaemonContext::new();
    init_screenstate_fn();
}

// ---------------------------------------------------------------------------
// load_initial_config_files
// ---------------------------------------------------------------------------
pub fn load_initial_config_files(ctx: &mut DaemonContext) {
    ctx.config = archconfig::load();
    crate::log::set_cached_debug_mode(ctx.config.debug_mode);

    log_info!(
        "config",
        "initial config.json loaded freq_offset={} standby_path={} standby_chg={}",
        ctx.config.freq_offset,
        ctx.config.standby_path,
        ctx.config.standby_chg
    );
}

fn is_stale_azenith_prop(name: &str) -> bool {
    name.starts_with(AZENITH_PROPERTIES)
        && !VALID_AZENITH_PROPS.iter().any(|&p| name == p)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::ffi::CString;

    #[test]
    fn stale_detection_catches_old_and_new_names() {
        assert!(is_stale_azenith_prop("persist.sys.azenith.old_removed_key"));
        assert!(is_stale_azenith_prop("persist.sys.azenithconf.old_key"));
        assert!(is_stale_azenith_prop("persist.sys.azenithConf.nigger"));
        assert!(is_stale_azenith_prop("persist.sys.azenithIntg.disabletweak"));
        assert!(!is_stale_azenith_prop("persist.sys.azenith.state"));
        assert!(!is_stale_azenith_prop("persist.sys.azenith.force_sidepanel"));
        assert!(!is_stale_azenith_prop("persist.sys.azenithdebug.soctype"));
        assert!(!is_stale_azenith_prop("persist.sys.other"));
        assert!(!is_stale_azenith_prop("ro.build.version"));
        // Set by the daemon for thermalcore; not under the azenith prefix.
        assert!(!is_stale_azenith_prop("persist.sys.rynx.thermalcore-bigdata.path"));
    }

    #[test]
    fn read_callback_collects_only_stale_names() {
        let mut pending: Vec<String> = Vec::new();
        let cookie = &mut pending as *mut Vec<String> as *mut c_void;

        let stale = CString::new("persist.sys.azenithConf.nigger").unwrap();
        validateprop_read_cb(cookie, stale.as_ptr(), std::ptr::null(), 0);
        let valid = CString::new("persist.sys.azenith.state").unwrap();
        validateprop_read_cb(cookie, valid.as_ptr(), std::ptr::null(), 0);
        let other = CString::new("ro.build.version").unwrap();
        validateprop_read_cb(cookie, other.as_ptr(), std::ptr::null(), 0);

        assert_eq!(pending, vec!["persist.sys.azenithConf.nigger".to_string()]);
    }

    #[test]
    fn read_callback_ignores_null() {
        let mut pending: Vec<String> = Vec::new();
        let cookie = &mut pending as *mut Vec<String> as *mut c_void;

        validateprop_read_cb(cookie, std::ptr::null(), std::ptr::null(), 0);
        validateprop_read_cb(std::ptr::null_mut(), std::ptr::null(), std::ptr::null(), 0);

        assert!(pending.is_empty());
    }

    #[test]
    fn valid_list_size_matches_entries() {
        assert_eq!(VALID_AZENITH_PROPS.len(), 48);
        assert!(VALID_AZENITH_PROPS.contains(&"persist.sys.azenith.force_sidepanel"));
        assert!(VALID_AZENITH_PROPS.contains(&"persist.sys.azenith.state"));
    }
}

// ---------------------------------------------------------------------------
// save_daemon_state / restore_daemon_state are in config/state.rs
// ---------------------------------------------------------------------------
