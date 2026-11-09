//! Profile application, mirroring `jni/src/profile/`.

use crate::sysprop::{get_or_default, get_into, set};
use crate::fileops;
use crate::config::refresh_rate::{apply_dynamic_refresh_rate, get_current_refresh_rate};
use crate::core::{
    DaemonContext, GameConfig, ProfileMode,
    SystemStateCache, GAME_INFO, GAME_INFO_APP, DAEMON_STATE_FILE, PROFILE_MODE, PROFILE_MODE_APP,
};
use crate::{IS_DEFAULT, IS_FALSE, IS_TRUE};
use crate::config::state::save_daemon_state;
use crate::io::write2file;
use crate::log_info;
use crate::monitor::get_visible_package;
use crate::preload::GamePreload;
use crate::proc::uidof;
use crate::shell::systemv;
use crate::util::{notify, toast};

use std::ffi::CString;
use std::thread;

// ---------------------------------------------------------------------------
// Function pointers (mirroring utility.c)
// ---------------------------------------------------------------------------
static GET_SCREENSTATE: std::sync::OnceLock<Box<dyn Fn(&SystemStateCache) -> bool + Send + Sync>> = std::sync::OnceLock::new();
static GET_LOW_POWER_STATE: std::sync::OnceLock<Box<dyn Fn(&SystemStateCache) -> bool + Send + Sync>> = std::sync::OnceLock::new();

pub fn init_screenstate_fn() {
    let _ = GET_SCREENSTATE.set(Box::new(get_screenstate_normal));
    let _ = GET_LOW_POWER_STATE.set(Box::new(get_low_power_state_normal));
}

pub fn get_screenstate(cache: &SystemStateCache) -> bool {
    GET_SCREENSTATE.get().map(|f| f(cache)).unwrap_or(true)
}

pub fn get_low_power_state(cache: &SystemStateCache) -> bool {
    GET_LOW_POWER_STATE.get().map(|f| f(cache)).unwrap_or(false)
}

fn get_screenstate_normal(cache: &SystemStateCache) -> bool {
    cache.screen_awake != 0
}

fn get_low_power_state_normal(cache: &SystemStateCache) -> bool {
    cache.battery_saver != 0
}

// ---------------------------------------------------------------------------
// profile_mode_name
// ---------------------------------------------------------------------------
pub fn profile_mode_name(mode: ProfileMode) -> &'static str {
    mode.name()
}

// ---------------------------------------------------------------------------
// run_profiler (takes ctx for gamestart/pids access)
// ---------------------------------------------------------------------------
pub fn run_profiler(profile: i32, ctx: &DaemonContext) {
    crate::module::is_kanged();

    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default();
    let secs = now.as_secs() as i64;
    let time_str = unsafe {
        let mut tm = std::mem::zeroed::<libc::tm>();
        if libc::localtime_r(&secs, &mut tm).is_null() {
            String::from("00:00:00")
        } else {
            let mut buf = [0u8; 16];
            let fmt = CString::new("%H:%M:%S").unwrap();
            libc::strftime(
                buf.as_mut_ptr() ,
                buf.len(),
                fmt.as_ptr(),
                &mut tm as *mut libc::tm,
            );
            std::ffi::CStr::from_ptr(buf.as_ptr() )
                .to_string_lossy()
                .into_owned()
        }
    };

    let gamestart = ctx.game.gamestart.as_deref().unwrap_or("");
    let main_pid = if ctx.game.game_pid_count > 0 { ctx.game.game_pids[0] } else { 0 };
    let uid = uidof(main_pid);

    if profile == 1 {
        let _ = write2file(GAME_INFO, false, false, &format!("{} {} {}\nTime: {}\n", gamestart, main_pid, uid, time_str));
        let _ = write2file(GAME_INFO_APP, false, false, &format!("{} {} {}\nTime: {}\n", gamestart, main_pid, uid, time_str));
    } else {
        let _ = write2file(GAME_INFO, false, false, &format!("NULL 0 0\nTime: {}\n", time_str));
        let _ = write2file(GAME_INFO_APP, false, false, &format!("NULL 0 0\nTime: {}\n", time_str));
    }

    let _ = write2file(PROFILE_MODE, false, false, &format!("{profile}\n"));
    let _ = write2file(PROFILE_MODE_APP, false, false, &format!("{profile}\n"));

    let _ = systemv(&format!("sys.azenith-profilesettings {profile}"));
}

// ---------------------------------------------------------------------------
// apply_performance_profile_inner
// ---------------------------------------------------------------------------
pub fn apply_performance_profile_inner(ctx: &mut DaemonContext) {
    toast("Applying Performance Profile");

    let prev_mode = ctx.profile.cur_mode;
    ctx.profile.cur_mode = ProfileMode::PerformanceProfile;
    ctx.need_profile_checkup = false;

    let app_name = ctx.game.active_app_name.as_deref().unwrap_or(ctx.game.gamestart.as_deref().unwrap_or("unknown"));
    let pkg = ctx.game.gamestart.as_deref().unwrap_or("unknown");
    notify("Performance Profile", &format!("Running at {app_name}"), false, 0, None);
    log_info!("profile", "profile changed from={} to=performance app={} pkg={}",
        profile_mode_name(prev_mode), app_name, pkg);

    if IS_TRUE!(ctx.opts_perf_lite_mode()) {
        set("persist.sys.azenithconf.litemode", "1");
    } else if IS_FALSE!(ctx.opts_perf_lite_mode()) {
        set("persist.sys.azenithconf.litemode", "0");
    } else {
        let lite_prop = get_or_default("persist.sys.azenithconf.cpulimit", "");
        set("persist.sys.azenithconf.litemode", if lite_prop == "1" { "1" } else { "0" });
    }

    if ctx.display.saved_zen_mode < 0 {
        ctx.display.saved_zen_mode = ctx.current_system_cache.zen_mode;
    }

    if IS_TRUE!(ctx.opts_dnd_on_gaming()) {
        if ctx.display.saved_zen_mode == 0 {
            systemv("sys.azenith-utilityconf enableDND");
        }
        ctx.dnd_enabled = true;
    } else if !IS_FALSE!(ctx.opts_dnd_on_gaming()) {
        let dnd_state = get_or_default("persist.sys.azenithconf.dnd", "");
        if dnd_state == "1" {
            if ctx.display.saved_zen_mode == 0 {
                systemv("sys.azenith-utilityconf enableDND");
            }
            ctx.dnd_enabled = true;
        }
    }

    let start = std::time::Instant::now();
    run_profiler(1, ctx);
    let elapsed = start.elapsed().as_secs_f64();
    log_info!("profile", "profile applied name=performance elapsed={:.4}s", elapsed);

    if !IS_DEFAULT!(ctx.opts_refresh_rate()) {
        let rr: i32 = ctx.opts_refresh_rate().parse().unwrap_or(0);
        if ctx.display.saved_refresh_rate < 0 {
            ctx.display.saved_refresh_rate = get_current_refresh_rate();
        }
        apply_dynamic_refresh_rate(rr);
    }

    let is_preload_active = if !IS_FALSE!(ctx.opts_game_preload()) {
        let preload_active = get_or_default("persist.sys.azenithconf.APreload", "");
        preload_active == "1"
    } else {
        false
    };

    if IS_TRUE!(ctx.opts_game_preload()) || is_preload_active {
        notify("AZenith Preload", &format!("Preloading initiated for: {app_name}"), true, 10000, None);

        let package = ctx.game.gamestart.as_deref().unwrap_or("");
        let mut p_args = PreloadArgs::default();
        let bytes = package.as_bytes();
        let n = bytes.len().min(p_args.package.len() - 1);
        p_args.package[..n].copy_from_slice(&bytes[..n]);
        p_args.package[n] = 0;

        let pkg_for_thread = c_string_from_fixed(&p_args.package);
        thread::spawn(move || {
            GamePreload(pkg_for_thread.to_str().unwrap_or(""));
        });
    }

    save_daemon_state(ctx);
}

// ---------------------------------------------------------------------------
// apply_eco_profile
// ---------------------------------------------------------------------------
pub fn apply_eco_profile(ctx: &mut DaemonContext) {
    if ctx.profile.cur_mode == ProfileMode::EcoMode {
        return;
    }

    toast("Applying Eco Mode");

    let prev_mode = ctx.profile.cur_mode;
    ctx.profile.cur_mode = ProfileMode::EcoMode;
    ctx.need_profile_checkup = false;

    notify("ECO Mode", "System is now at Endurance state", false, 0, None);
    log_info!("profile", "profile changed from={} to=eco reason=low_power", profile_mode_name(prev_mode));

    if ctx.display.saved_refresh_rate > 0 {
        apply_dynamic_refresh_rate(ctx.display.saved_refresh_rate);
        ctx.display.saved_refresh_rate = -1;
    }

    if ctx.dnd_enabled {
        if ctx.display.saved_zen_mode == 0 {
            systemv("sys.azenith-utilityconf disableDND");
        }
        ctx.dnd_enabled = false;
    }
    ctx.display.saved_zen_mode = -1;

    if !ctx.saved_renderer_str().is_empty() {
        let mut current_now = [0u8; 92];
        let _ = get_into("debug.hwui.renderer", &mut current_now);
        let current_now = unsafe {
            std::ffi::CStr::from_ptr(current_now.as_ptr() ).to_string_lossy().into_owned()
        };
        let current_now = if current_now.is_empty() { "default".to_string() } else { current_now };

        if current_now != ctx.saved_renderer_str() {
            log_info!("render", "renderer restored value={}", ctx.saved_renderer_str());
            if ctx.saved_renderer_str() == "default" {
                systemv("sys.azenith-utilityconf setrender default");
                set("persist.sys.azenithconf.renderer", "default");
            } else {
                systemv(&format!("sys.azenith-utilityconf setrender {}", ctx.saved_renderer_str()));
                set("persist.sys.azenithconf.renderer", ctx.saved_sys_renderer_str());
            }
        }
        ctx.profile.saved_renderer = [0; 92];
        ctx.profile.saved_sys_renderer = [0; 92];
    }

    let start = std::time::Instant::now();
    run_profiler(3, ctx);
    let elapsed = start.elapsed().as_secs_f64();
    log_info!("profile", "profile applied name=eco elapsed={:.4}s", elapsed);

    fileops::remove_file_best_effort(DAEMON_STATE_FILE);
}

// ---------------------------------------------------------------------------
// apply_balanced_profile
// ---------------------------------------------------------------------------
pub fn apply_balanced_profile(ctx: &mut DaemonContext) {
    if ctx.is_initialize_complete && ctx.profile.cur_mode == ProfileMode::BalancedProfile {
        return;
    }

    toast("Applying Balanced Profile");

    let prev_mode = ctx.profile.cur_mode;
    ctx.profile.cur_mode = ProfileMode::BalancedProfile;
    ctx.need_profile_checkup = false;

    notify("Balanced Profile", "System is now at Optimal state", false, 0, None);
    log_info!("profile", "profile changed from={} to=balanced reason={}",
        profile_mode_name(prev_mode),
        if ctx.is_initialize_complete { "idle" } else { "startup" }
    );

    if ctx.display.saved_refresh_rate > 0 {
        apply_dynamic_refresh_rate(ctx.display.saved_refresh_rate);
        ctx.display.saved_refresh_rate = -1;
    }

    if ctx.dnd_enabled {
        if ctx.display.saved_zen_mode == 0 {
            systemv("sys.azenith-utilityconf disableDND");
        }
        ctx.dnd_enabled = false;
    }
    ctx.display.saved_zen_mode = -1;

    if !ctx.saved_renderer_str().is_empty() {
        let mut current_now = [0u8; 92];
        let _ = get_into("debug.hwui.renderer", &mut current_now);
        let current_now = unsafe {
            std::ffi::CStr::from_ptr(current_now.as_ptr() ).to_string_lossy().into_owned()
        };
        let current_now = if current_now.is_empty() { "default".to_string() } else { current_now };

        if current_now != ctx.saved_renderer_str() {
            log_info!("render", "renderer restored value={}", ctx.saved_renderer_str());
            if ctx.saved_renderer_str() == "default" {
                systemv("sys.azenith-utilityconf setrender default");
                set("persist.sys.azenithconf.renderer", "default");
            } else {
                systemv(&format!("sys.azenith-utilityconf setrender {}", ctx.saved_renderer_str()));
                set("persist.sys.azenithconf.renderer", ctx.saved_sys_renderer_str());
            }
        }
        ctx.profile.saved_renderer = [0; 92];
        ctx.profile.saved_sys_renderer = [0; 92];
    }

    let start = std::time::Instant::now();
    run_profiler(2, ctx);
    let elapsed = start.elapsed().as_secs_f64();
    log_info!("profile", "profile applied name=balanced elapsed={:.4}s", elapsed);

    if !ctx.is_initialize_complete {
        notify("Daemon Info", "AZenith is running successfully", false, 60000, None);
        ctx.is_initialize_complete = true;
    }

    fileops::remove_file_best_effort(DAEMON_STATE_FILE);
}

// ---------------------------------------------------------------------------
// get_gamestart
// ---------------------------------------------------------------------------
pub fn get_gamestart(options: &mut GameConfig, cache: &SystemStateCache) -> Option<String> {
    let pkg = match get_visible_package(cache) {
        Some(p) => p,
        None => {
            // Debug: log why visible package is None (only if focused is non-empty)
            let focused = cache.focused_app_str();
            let screen_ok = get_screenstate(cache);
            if !focused.is_empty() {
                crate::log_debug!("profile", "game check: focused={} screen_ok={} but get_visible_package returned None", focused, screen_ok);
            }
            return None;
        }
    };

    let _mutex = crate::core::CACHE_MUTEX.get().unwrap().lock().unwrap();

    let cache_lock = match crate::core::G_GAME_CACHE.get() {
        Some(v) => v,
        None => return None,
    };
    let cache_vec = cache_lock.read().unwrap();
    if cache_vec.is_empty() {
        return None;
    }

    let mut match_found = false;
    let mut perf_lite_mode = String::new();
    let mut dnd_on_gaming = String::new();
    let mut app_priority = String::new();
    let mut game_preload = String::new();
    let mut refresh_rate = String::new();
    let mut renderer = String::new();
    let mut resolution_downscale = String::new();
    let mut resolution_fps = String::new();
    let mut bypass_charging = String::new();

    for entry in cache_vec.iter() {
        let entry_pkg = entry.field_as_str(&entry.package);
        if entry_pkg == pkg {
            match_found = true;
            perf_lite_mode = entry.field_as_str(&entry.perf_lite_mode).to_string();
            dnd_on_gaming = entry.field_as_str(&entry.dnd_on_gaming).to_string();
            app_priority = entry.field_as_str(&entry.app_priority).to_string();
            game_preload = entry.field_as_str(&entry.game_preload).to_string();
            refresh_rate = entry.field_as_str(&entry.refresh_rate).to_string();
            renderer = entry.field_as_str(&entry.renderer).to_string();
            resolution_downscale = entry.field_as_str(&entry.resolution_downscale).to_string();
            resolution_fps = entry.field_as_str(&entry.resolution_fps).to_string();
            bypass_charging = entry.field_as_str(&entry.bypass_charging).to_string();
            break;
        }
    }

    if match_found {
        GameConfig::set_field(&mut options.perf_lite_mode, &perf_lite_mode);
        GameConfig::set_field(&mut options.dnd_on_gaming, &dnd_on_gaming);
        GameConfig::set_field(&mut options.app_priority, &app_priority);
        GameConfig::set_field(&mut options.game_preload, &game_preload);
        GameConfig::set_field(&mut options.refresh_rate, &refresh_rate);
        GameConfig::set_field(&mut options.renderer, &renderer);
        GameConfig::set_field(&mut options.resolution_downscale, &resolution_downscale);
        GameConfig::set_field(&mut options.resolution_fps, &resolution_fps);
        GameConfig::set_field(&mut options.bypass_charging, &bypass_charging);
    }

    if !match_found {
        crate::log_debug!("profile", "game check: pkg={} not in gamelist (count={})", pkg, cache_vec.len());
        return None;
    }

    Some(pkg)
}

// ---------------------------------------------------------------------------
// PreloadArgs
// ---------------------------------------------------------------------------
#[derive(Clone)]
pub struct PreloadArgs {
    pub package: [u8; 256],
}

impl Default for PreloadArgs {
    fn default() -> Self {
        Self { package: [0; 256] }
    }
}

fn c_string_from_fixed(arr: &[u8]) -> CString {
    let len = arr.iter().position(|&b| b == 0).unwrap_or(arr.len());
    CString::new(&arr[..len]).unwrap_or_default()
}
