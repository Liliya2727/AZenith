//! Startup logic.

use crate::android_ffi::__system_property_foreach;
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
use crate::util::{notify, trim_newline};

use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_uchar, c_void};
use std::thread;
use std::time::Duration;

// ---------------------------------------------------------------------------
// validateprop — stale system property cleanup (mirrors prop.c)
// ---------------------------------------------------------------------------

const AZENITH_PROPERTIES: &str = "persist.sys.azenith";

const VALID_AZENITH_PROPS: [&str; 46] = [
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
];

const LEGACY_STALE_PROPS: [&str; 2] = [
    "persist.sys.rianixia.learning_enabled",
    "persist.sys.rianixia.thermalcore-bigdata.path",
];

pub fn validateprop() {
    let mut pending: Vec<String> = Vec::new();

    // Collect legacy stale props
    for &name in LEGACY_STALE_PROPS.iter() {
        if get(name).is_some() {
            pending.push(name.to_string());
            log_warn!("prop", "legacy stale prop flagged name={}", name);
        }
    }

    // Iterate all system properties via __system_property_foreach
    extern "C" fn validateprop_foreach_cb(
        cookie: *mut c_void,
        name: *const c_uchar,
        _value: *const c_uchar,
        _serial: u32,
    ) {
        if cookie.is_null() {
            return;
        }
        let pending: &mut Vec<String> = unsafe { &mut *(cookie as *mut Vec<String>) };
        let c_name = unsafe { CStr::from_ptr(name as *const c_char) };
        let name_str = c_name.to_string_lossy();

        if !name_str.starts_with(AZENITH_PROPERTIES) {
            return;
        }
        if VALID_AZENITH_PROPS.iter().any(|&p| name_str == p) {
            return;
        }
        pending.push(name_str.to_string());
        log_warn!("prop", "stale prop flagged name={}", name_str);
    }

    unsafe {
        __system_property_foreach(
            validateprop_foreach_cb as *mut c_void,
            &mut pending as *mut Vec<String> as *mut c_void,
        );
    }

    if pending.is_empty() {
        log_info!("prop", "no stale props found");
        return;
    }

    log_info!("prop", "cleaning stale props count={}", pending.len());

    for name in pending {
        let cmd = CString::new(format!("resetprop -p --delete {}", name)).unwrap();
        let status = systemv(cmd.to_str().unwrap());
        if status != 0 {
            log_error!("prop", "failed to delete stale prop name={} status={}", name, status);
        }
        log_warn!("prop", "deleting stale prop name={}", name);
    }
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
            set("persist.sys.azenith.service", "");
            set("persist.sys.azenith.state", "stopped");
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
    let freq_path = "/data/adb/.config/AZenith/freqoffset";
    if let Ok(mut s) = fileops::read_string_result(freq_path) {
        trim_newline(&mut s);
        ctx.config_freqoffset = [0; 92];
        let bytes = s.as_bytes();
        let n = bytes.len().min(ctx.config_freqoffset.len() - 1);
        ctx.config_freqoffset[..n].copy_from_slice(&bytes[..n]);
        ctx.config_freqoffset[n] = 0;
    } else {
        let def = "Disabled";
        ctx.config_freqoffset = [0; 92];
        ctx.config_freqoffset[..def.len()].copy_from_slice(def.as_bytes());
    }

    let _bypass_path = format!("{}/bypasschgconfig/bypasspath", crate::core::BYPASSCHG_CONFIG.trim_end_matches("/bypasschgconfig"));
    // Actually BYPASSCHG_CONFIG is "/data/adb/.config/AZenith/bypasschgconfig"
    let bypass_path = "/data/adb/.config/AZenith/bypasschgconfig/bypasspath";
    if let Ok(mut s) = fileops::read_string_result(bypass_path) {
        trim_newline(&mut s);
        ctx.bypass.config_bypasspath = [0; 92];
        let bytes = s.as_bytes();
        let n = bytes.len().min(ctx.bypass.config_bypasspath.len() - 1);
        ctx.bypass.config_bypasspath[..n].copy_from_slice(&bytes[..n]);
        ctx.bypass.config_bypasspath[n] = 0;
    }

    let bypasschg = "/data/adb/.config/AZenith/bypasschgconfig/bypasschg";
    if let Ok(s) = fileops::read_string_result(bypasschg) {
        let v: i32 = s.trim().parse().unwrap_or(0);
        ctx.bypass.config_bypasschg = v;
    }

    let bypasschgthreshold = "/data/adb/.config/AZenith/bypasschgconfig/bypasschgthreshold";
    if let Ok(s) = fileops::read_string_result(bypasschgthreshold) {
        let v: i32 = s.trim().parse().unwrap_or(0);
        ctx.bypass.config_bypasschgthreshold = v;
    }

    log_info!("config", "initial configs loaded freqoffset={} bypasspath={}",
        ctx.config_freqoffset_str(), ctx.config_bypasspath_str());
}

// ---------------------------------------------------------------------------
// save_daemon_state / restore_daemon_state are in config/state.rs
// ---------------------------------------------------------------------------
