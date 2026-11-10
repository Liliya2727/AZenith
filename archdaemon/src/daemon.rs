//! Main daemon loop.

use crate::sysprop::{get_into, set};
use crate::fileops;use crate::bypass::handle_dynamic_bypass;
use crate::config::resolution::{apply_resolution_target, restore_resolution_target};
use crate::config::rendering::apply_smart_renderer;
use crate::core::{
    DaemonContext, ProfileMode,
    DAEMON_MODES, JAVA_LOCK_PIPE_WRITE, JAVA_LOCK_PIPE_READ,
};
use crate::ipc::{process_inotify_events, setup_inotify_watchers};
use crate::{log_debug, log_error, log_fatal, log_info, log_warn};
use crate::module::{check_module_version, is_kanged, runthermalcore, update_module_description};
use crate::monitor::{read_app_status, reload_gamelist_cache};
use crate::config::state::restore_daemon_state;
use crate::profile::{
    apply_balanced_profile, apply_eco_profile, apply_performance_profile_inner,
    get_gamestart, get_low_power_state, get_screenstate, profile_mode_name, run_profiler,
};
use crate::proc::{get_pids_of, set_priority, setspid};
use crate::shell::systemv;
use crate::startup::{
    init_daemon_context, load_initial_config_files, validateprop, wait_for_java_companion,
};
use crate::util::{checkstate, notify, sighandler};
use crate::{IS_DEFAULT, IS_FALSE, IS_TRUE};

use std::os::raw::c_int;
use std::thread;
use std::time::Duration;

// ---------------------------------------------------------------------------
// main_daemon
// ---------------------------------------------------------------------------
pub fn main_daemon() -> c_int {
    crate::startup::verify_system_integrity();

    // Daemonize
    unsafe {
        if libc::daemon(0, 0) != 0 {
            log_fatal!("daemon", "daemonize failed errno={}", std::io::Error::last_os_error());
            set("persist.sys.azenith.service", "");
            set("persist.sys.azenith.state", "stopped");
            return 1;
        }
    }

    // Signal handlers
    unsafe {
        libc::signal(libc::SIGINT, sighandler as *const () as usize);
        libc::signal(libc::SIGTERM, sighandler as *const () as usize);
    }

    let mut ctx = DaemonContext::new();
    init_daemon_context(&mut ctx);
    restore_daemon_state(&mut ctx);

    let mut recorder = crate::recorder::Recorder::new();
    recorder.recover(&ctx);
    wait_for_java_companion(&mut ctx);

    // Java lock pipe
    let mut java_lock_pipe: [i32; 2] = [-1, -1];
    unsafe {
        if libc::pipe(java_lock_pipe.as_mut_ptr()) != 0 {
            log_error!("daemon", "java lock pipe create failed errno={}", std::io::Error::last_os_error());
        } else {
            JAVA_LOCK_PIPE_WRITE
                .get()
                .unwrap()
                .store(java_lock_pipe[1], std::sync::atomic::Ordering::Relaxed);
            JAVA_LOCK_PIPE_READ
                .get()
                .unwrap()
                .store(java_lock_pipe[0], std::sync::atomic::Ordering::Relaxed);

            let lock_path = ctx.java_lock_path.clone();
            let pipe_write = java_lock_pipe[1];
            thread::spawn(move || {
                java_lock_watcher(lock_path, pipe_write);
            });
        }
    }

    // Read initial app status cache
    log_debug!("apploader", "reading initial app status cache");
    read_app_status(&mut ctx.current_system_cache);
    reload_gamelist_cache(&mut ctx);
    log_info!("apploader", "gamelist cache primed count={}", crate::core::game_cache_count());

    // Initiate PID
    log_info!("daemon", "daemon started pid={}", std::process::id());
    set("persist.sys.rynx.learning_enabled", "true");
    set("persist.sys.rynx.thermalcore-bigdata.path", "/data/adb/.config/AZenith/debug");
    set("persist.sys.azenith.state", "running");
    notify("Initializing...", "Starting AZenith service...", false, 0, None);
    setspid();

    // Read AI state
    if let Ok(fp) = std::fs::File::open(DAEMON_MODES) {
        use std::io::BufRead;
        if let Some(line) = std::io::BufReader::new(fp).lines().next() {
            if let Ok(s) = line {
                let trimmed = s.trim().to_string();
                let bytes = trimmed.as_bytes();
                let n = bytes.len().min(ctx.prev_ai_state.len() - 1);
                ctx.prev_ai_state[..n].copy_from_slice(&bytes[..n]);
                ctx.prev_ai_state[n] = 0;
            }
        }
    }

    // Setup inotify watchers
    let inotify_fd = setup_inotify_watchers();
    load_initial_config_files(&mut ctx);

    checkstate();
    is_kanged();
    check_module_version();
    validateprop();
    log_debug!("integrity", "module integrity passed");

    log_info!("daemon", "daemon ready pid={} mode={}", std::process::id(), profile_mode_name(ctx.profile.cur_mode));
    ctx.need_profile_checkup = true;
    let mut need_loop = true;
    runthermalcore();
    run_profiler(0, &ctx); // PERFCOMMON
    update_module_description(std::process::id() as i32);
    fileops::remove_file_best_effort("/dev/.azenithSingleInstance");

    /* Main Daemon Loop */
    loop {
        let mut poll_timeout = -1i32;
        if need_loop {
            poll_timeout = 0;
        } else if ctx.power.grace_period_active {
            let now = std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap_or_default()
                .as_secs_f64();
            let elapsed = now - ctx.power.screen_off_timer as f64;
            if elapsed < 10.0 {
                poll_timeout = ((10.0 - elapsed) * 1000.0) as i32;
            } else {
                poll_timeout = 0;
            }
        } else if ctx.power.fg_away_active && ctx.game.gamestart.is_some() {
            let now = std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap_or_default()
                .as_secs_f64();
            let elapsed = now - ctx.power.fg_away_timer as f64;
            if elapsed < 30.0 {
                poll_timeout = ((30.0 - elapsed) * 1000.0) as i32;
            } else {
                poll_timeout = 0;
            }
        } else if ctx.power.fg_away_active && ctx.game.gamestart.is_none() {
            ctx.power.fg_away_active = false;
            ctx.power.fg_away_timer = 0;
        }

        // Recorder sampling: wake exactly at the next sample deadline. A
        // fixed 1000 ms poll plus loop work drifts ~10 ms/tick and every
        // ~95 ticks tick() has to write two rows from one snapshot.
        if let Some(ms) = recorder.ms_until_next_tick() {
            if poll_timeout < 0 || poll_timeout > ms {
                poll_timeout = ms;
            }
        }
        // Upper bound of the idle poll interval (poll would otherwise block
        // forever and delay the SIGTERM/SIGINT finalize until the next
        // inotify event).
        if !need_loop && (poll_timeout < 0 || poll_timeout > 5000) {
            poll_timeout = 5000;
        }

        let should_exit = process_inotify_events(inotify_fd, &mut ctx, &mut recorder, poll_timeout);
        need_loop = false;

        log_debug!("daemon", "tick focused={} screen={} mode={} game={} need_checkup={} poll_timeout={}",
            ctx.current_system_cache.focused_app_str(),
            ctx.display.prev_screen_state,
            profile_mode_name(ctx.profile.cur_mode),
            ctx.game.gamestart.as_deref().unwrap_or("-"),
            ctx.need_profile_checkup,
            poll_timeout
        );

        if crate::core::java_daemon_died() {
            log_fatal!("daemon", "java companion lock released, stopping daemon");
            recorder.finalize(&mut ctx, crate::recorder::StopReason::DaemonStop);
            notify("Daemon Error", "Java companion daemon crashed. Stopping AZenith.", false, 0, None);
            set("persist.sys.azenith.service", "");
            set("persist.sys.azenith.state", "stopped");
            break;
        }

        if should_exit {
            recorder.finalize(&mut ctx, crate::recorder::StopReason::DaemonStop);
            break;
        }

        if crate::util::exit_signal() {
            log_info!("daemon", "shutdown signal received, finalizing recorder");
            recorder.finalize(&mut ctx, crate::recorder::StopReason::DaemonStop);
            break;
        }

        // Recorder sampling tick: runs before any `continue` path below so
        // profile logic can never starve it. Flushes/stops are driven from
        // here; sampling cadence comes from the 1s poll cap while active.
        recorder.tick(&mut ctx);

        let real_screen_state = get_screenstate(&ctx.current_system_cache);

        if ctx.config_freqoffset_str() == "Disabled" {
            if ctx.last_freqoffset_str() != "Disabled" {
                systemv("sys.azenith-profilesettings applyfreqbalance");
            }
        } else if real_screen_state
            && (ctx.profile.cur_mode == ProfileMode::BalancedProfile || ctx.profile.cur_mode == ProfileMode::EcoMode)
        {
            systemv("sys.azenith-profilesettings applyfreqbalance");
        }
        // strcpy(ctx.last_freqoffset, ctx.config_freqoffset)
        let freq = ctx.config_freqoffset_str().to_string();
        let bytes = freq.as_bytes();
        let n = bytes.len().min(ctx.last_freqoffset.len() - 1);
        ctx.last_freqoffset[..n].copy_from_slice(&bytes[..n]);
        ctx.last_freqoffset[n] = 0;

        handle_dynamic_bypass(&mut ctx);

        if ctx.is_initialize_complete && ctx.prev_ai_state_str() == "0" {
            continue;
        }

        if ctx.need_profile_checkup {
            let opts = &mut ctx.opts;
            let cache = &ctx.current_system_cache;
            if let Some(current_focused_game) = get_gamestart(opts, cache) {
                if ctx.game.gamestart.as_deref() == Some(&current_focused_game) {
                    ctx.need_profile_checkup = false;
                } else {
                    let old_gs = ctx.game.gamestart.clone();
                    if let Some(gs) = old_gs.as_deref() {
                        if ctx.render.resolution_applied {
                            restore_resolution_target(&mut ctx, gs);
                        }
                    }
                    ctx.game.gamestart = Some(current_focused_game.clone());
                    ctx.game.active_app_name = Some(ctx.current_system_cache.app_name_str().to_string());
                    log_info!("profile", "game detected pkg={} name={}",
                        ctx.game.gamestart.as_deref().unwrap_or(""),
                        ctx.game.active_app_name.as_deref().unwrap_or("unknown")
                    );
                    ctx.game.game_pids = [0; crate::core::MAX_GAME_PIDS];
                    ctx.game.game_pid_count = 0;
                    ctx.pid_retries = 0;
                    ctx.render.has_applied_renderer = false;
                    ctx.need_profile_checkup = true;
                    handle_dynamic_bypass(&mut ctx);
                }
            } else if ctx.profile.cur_mode != ProfileMode::BalancedProfile && ctx.profile.cur_mode != ProfileMode::EcoMode {
                ctx.need_profile_checkup = false;
            }
        }

        let mut effective_screen_state: i32 = if real_screen_state { 1 } else { 0 };
        // Handle initial -1 (unknown) as different from any state
        let prev_is_on = ctx.display.prev_screen_state == 1;
        let prev_is_off = ctx.display.prev_screen_state == 0;
        let state_changed = (real_screen_state && !prev_is_on) || (!real_screen_state && !prev_is_off);
        if state_changed {
            log_info!("power", "screen state changed from={} to={}",
                if ctx.display.prev_screen_state == 1 { "on" } else if ctx.display.prev_screen_state == 0 { "off" } else { "unknown" },
                if real_screen_state { "on" } else { "off" }
            );
            if !real_screen_state {
                if ctx.profile.cur_mode == ProfileMode::PerformanceProfile {
                    ctx.power.screen_off_timer = std::time::SystemTime::now()
                        .duration_since(std::time::UNIX_EPOCH)
                        .unwrap_or_default()
                        .as_secs() as i64;
                    ctx.power.grace_period_active = true;
                    log_info!("power", "grace period started timeout=10s keep_mode={}", profile_mode_name(ctx.profile.cur_mode));
                }
            } else {
                if ctx.power.grace_period_active {
                    log_info!("power", "grace period aborted action=keep_performance");
                    ctx.power.grace_period_active = false;
                }
                ctx.power.screen_off_timer = 0;
            }
            ctx.display.prev_screen_state = if real_screen_state { 1 } else { 0 };
        }

        if ctx.power.grace_period_active {
            let now = std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap_or_default()
                .as_secs() as i64;
            if now - ctx.power.screen_off_timer < 10 {
                effective_screen_state = 1;
            } else {
                log_info!("power", "grace period expired action=drop_performance");
                ctx.power.grace_period_active = false;
                ctx.power.screen_off_timer = 0;
                effective_screen_state = 0;
                ctx.need_profile_checkup = true;
            }
        }

        let gs_clone = ctx.game.gamestart.clone();
        if let Some(gs) = gs_clone.as_deref() {
            if ctx.profile.cur_mode == ProfileMode::PerformanceProfile {
                let mut dropfg_val = [0u8; 92];
                let _ = get_into("persist.sys.azenith.dropforeground", &mut dropfg_val);
                let dropfg = unsafe {
                    std::ffi::CStr::from_ptr(dropfg_val.as_ptr() ).to_string_lossy()
                };
                let dropforeground_enabled = dropfg.len() > 0 && dropfg == "1";

                if dropforeground_enabled {
                    let is_focused = ctx.current_system_cache.focused_app_str() == gs;
                    if !is_focused {
                        if !ctx.power.fg_away_active {
                            ctx.power.fg_away_timer = std::time::SystemTime::now()
                                .duration_since(std::time::UNIX_EPOCH)
                                .unwrap_or_default()
                                .as_secs() as i64;
                            ctx.power.fg_away_active = true;
                            log_debug!("profile", "game left foreground pkg={} action=grace_start timeout=30s", gs);
                        } else {
                            let now = std::time::SystemTime::now()
                                .duration_since(std::time::UNIX_EPOCH)
                                .unwrap_or_default()
                                .as_secs() as i64;
                            if now - ctx.power.fg_away_timer >= 30 {
                                log_info!("profile", "foreground grace expired pkg={} action=drop_to_balanced",
                                    ctx.game.active_app_name.as_deref().unwrap_or(gs));
                                ctx.power.fg_away_active = false;
                                ctx.power.fg_away_timer = 0;
                                restore_resolution_target(&mut ctx, gs);
                                ctx.game.gamestart = None;
                                ctx.game.active_app_name = None;
                                ctx.need_profile_checkup = true;
                            }
                        }
                    } else if ctx.power.fg_away_active {
                        ctx.power.fg_away_active = false;
                        ctx.power.fg_away_timer = 0;
                    }
                } else if ctx.power.fg_away_active {
                    ctx.power.fg_away_active = false;
                    ctx.power.fg_away_timer = 0;
                }
            }
        }

        if ctx.is_initialize_complete
            && ctx.profile.cur_mode != ProfileMode::PerformanceProfile
            && ctx.game.gamestart.is_none()
            && !ctx.need_profile_checkup
        {
            let is_low_power = get_low_power_state(&ctx.current_system_cache);

            if is_low_power && ctx.profile.cur_mode != ProfileMode::EcoMode {
                apply_eco_profile(&mut ctx);
            } else if !is_low_power && ctx.profile.cur_mode == ProfileMode::EcoMode {
                apply_balanced_profile(&mut ctx);
            } else {
                continue;
            }
        }

        if ctx.is_initialize_complete
            && ctx.game.gamestart.is_some()
            && effective_screen_state != 0
        {
            let gs = ctx.game.gamestart.as_deref().unwrap().to_string();
            if !ctx.need_profile_checkup
                && ctx.profile.cur_mode == ProfileMode::PerformanceProfile
                && ctx.render.has_applied_renderer
                && ctx.game.game_pid_count > 0
            {
                continue;
            }

            if !ctx.render.has_applied_renderer {
                let mut renderer_changed = false;
                if !IS_DEFAULT!(ctx.opts_renderer()) {
                    let renderer = ctx.opts_renderer().to_string();
                    renderer_changed = apply_smart_renderer(
                        &renderer,
                        &mut ctx.profile.saved_renderer,
                        &mut ctx.profile.saved_sys_renderer,
                    );
                }

                let mut reso_changed = false;
                if !IS_DEFAULT!(ctx.opts_resolution_downscale()) {
                    let downscale = ctx.opts_resolution_downscale().to_string();
                    let fps = ctx.opts_resolution_fps().to_string();
                    reso_changed = apply_resolution_target(
                        &mut ctx,
                        &gs,
                        &downscale,
                        &fps,
                    );
                }

                if renderer_changed || reso_changed {
                    ctx.render.is_restarting_renderer = true;
                    restart_target_app_inner(&gs);
                    ctx.render.is_restarting_renderer = false;
                    ctx.game.game_pids = [0; crate::core::MAX_GAME_PIDS];
                    ctx.game.game_pid_count = 0;
                    ctx.pid_retries = 0;
                }

                ctx.render.has_applied_renderer = true;
            }

            if ctx.game.game_pid_count == 0 {
                if ctx.current_system_cache.focused_app_str() == &gs {
                    ctx.game.game_pid_count = get_pids_of(&gs, &mut ctx.game.game_pids, crate::core::MAX_GAME_PIDS as i32);
                }
                if ctx.game.game_pid_count == 0 {
                    if ctx.pid_retries < 5 {
                        ctx.pid_retries += 1;
                        log_warn!("profile", "game pkg={} not spawned yet retry={}/5",
                            ctx.game.active_app_name.as_deref().unwrap_or(&gs), ctx.pid_retries);
                        thread::sleep(Duration::from_millis(200));
                        need_loop = true;
                        continue;
                    } else {
                        log_error!("profile", "no pid found pkg={} after retries=5 action=drop",
                            ctx.game.active_app_name.as_deref().unwrap_or(&gs));
                        ctx.game.gamestart = None;
                        ctx.game.active_app_name = None;
                        ctx.pid_retries = 0;
                        ctx.need_profile_checkup = true;
                        continue;
                    }
                }
                ctx.pid_retries = 0;
                for i in 0..ctx.game.game_pid_count {
                    if IS_TRUE!(ctx.opts_app_priority()) {
                        set_priority(ctx.game.game_pids[i as usize]);
                    } else if !IS_FALSE!(ctx.opts_app_priority()) {
                        let mut val = [0u8; 92];
                        let _ = get_into("persist.sys.azenithconf.iosched", &mut val);
                        let val = unsafe {
                            std::ffi::CStr::from_ptr(val.as_ptr() ).to_string_lossy()
                        };
                        if val == "1" {
                            set_priority(ctx.game.game_pids[i as usize]);
                        }
                    }
                }
            }
            apply_performance_profile_inner(&mut ctx);
        } else if ctx.is_initialize_complete && get_low_power_state(&ctx.current_system_cache) {
            apply_eco_profile(&mut ctx);
        } else {
            apply_balanced_profile(&mut ctx);
        }
    }

    if inotify_fd >= 0 {
        unsafe { libc::close(inotify_fd) };
    }
    0
}

fn restart_target_app_inner(pkg: &str) {
    crate::util::restart_target_app(pkg);
    // Note: util::restart_target_app already sleeps 700ms, no additional sleep needed
}

fn java_lock_watcher(lock_path: String, pipe_write: i32) {
    let c_path = match std::ffi::CString::new(lock_path.as_str()) {
        Ok(p) => p,
        Err(_) => return,
    };
    let fd = unsafe { libc::open(c_path.as_ptr(), libc::O_RDWR | libc::O_CREAT, 0o600) };
    if fd >= 0 {
        unsafe {
            let mut fl = std::mem::zeroed::<libc::flock>();
            fl.l_type = libc::F_WRLCK as i16;
            fl.l_whence = libc::SEEK_SET as i16;
            if libc::fcntl(fd, libc::F_SETLKW, &mut fl as *mut libc::flock) != -1 {
                fl.l_type = libc::F_UNLCK as i16;
                libc::fcntl(fd, libc::F_SETLK, &mut fl as *mut libc::flock);
            }
            libc::close(fd);
        }
    }

    let signal_byte: u8 = 1;
    unsafe {
        libc::write(pipe_write, &signal_byte as *const u8 as *const _, 1);
    }
}
