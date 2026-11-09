//! Daemon state persistence.

use crate::core::{GameConfig, 
    DAEMON_STATE_FILE, ProfileMode,
};
use crate::{log_debug, log_info, log_error};
use crate::fileops;

fn profile_mode_name(mode: ProfileMode) -> &'static str {
    match mode {
        ProfileMode::PerformanceProfile => "performance",
        ProfileMode::BalancedProfile => "balanced",
        ProfileMode::EcoMode => "eco",
        ProfileMode::PerfCommon => "common",
    }
}

// Called from profile.rs via crate::config::state::save_daemon_state
pub fn save_daemon_state(ctx: &crate::core::DaemonContext) {
    let content = format!(
        "saved_renderer={}\n\
         saved_sys_renderer={}\n\
         saved_refresh_rate={}\n\
         saved_zen_mode={}\n\
         dnd_enabled={}\n\
         cur_mode={}\n\
         bypass_applied={}\n\
         bypass_charging={}\n",
        ctx.saved_renderer_str(),
        ctx.saved_sys_renderer_str(),
        ctx.display.saved_refresh_rate,
        ctx.display.saved_zen_mode,
        if ctx.dnd_enabled { 1 } else { 0 },
        ctx.profile.cur_mode as i32,
        if ctx.bypass.bypass_applied { 1 } else { 0 },
        ctx.opts_bypass_charging()
    );

    match fileops::write_string(DAEMON_STATE_FILE, &content) {
        Ok(_) => {
            log_debug!("state", "state saved renderer={} sys_renderer={} refresh={} zen={} mode={} bypass={} bypass_charging={}",
                ctx.saved_renderer_str(), ctx.saved_sys_renderer_str(), ctx.display.saved_refresh_rate,
                ctx.display.saved_zen_mode, profile_mode_name(ctx.profile.cur_mode),
                if ctx.bypass.bypass_applied { 1 } else { 0 },
                ctx.opts_bypass_charging()
            );
        }
        Err(e) => {
            log_error!("state", "state file open failed path={} errno={}", DAEMON_STATE_FILE, e.raw_os_error().unwrap_or(-1));
        }
    }
}

pub fn restore_daemon_state(ctx: &mut crate::core::DaemonContext) {
    let Ok(content) = fileops::read_string_result(DAEMON_STATE_FILE) else {
        return; // No pending state, normal cold start
    };

    for line in content.lines() {
        let line = line.trim();
        if let Some(eq_pos) = line.find('=') {
            let key = &line[..eq_pos];
            let val = &line[eq_pos + 1..];

            match key {
                "saved_renderer" => {
                    let bytes = val.as_bytes();
                    let n = bytes.len().min(ctx.profile.saved_renderer.len() - 1);
                    ctx.profile.saved_renderer[..n].copy_from_slice(&bytes[..n]);
                    ctx.profile.saved_renderer[n] = 0;
                }
                "saved_sys_renderer" => {
                    let bytes = val.as_bytes();
                    let n = bytes.len().min(ctx.profile.saved_sys_renderer.len() - 1);
                    ctx.profile.saved_sys_renderer[..n].copy_from_slice(&bytes[..n]);
                    ctx.profile.saved_sys_renderer[n] = 0;
                }
                "saved_refresh_rate" => {
                    ctx.display.saved_refresh_rate = val.parse().unwrap_or(-1);
                }
                "saved_zen_mode" => {
                    ctx.display.saved_zen_mode = val.parse().unwrap_or(-1);
                }
                "dnd_enabled" => {
                    ctx.dnd_enabled = val == "1";
                }
                "cur_mode" => {
                    ctx.profile.cur_mode = ProfileMode::from_i32(val.parse().unwrap_or(0));
                }
                "bypass_applied" => {
                    ctx.bypass.bypass_applied = val == "1";
                }
                "bypass_charging" => {
                    if !val.is_empty() {
                        GameConfig::set_field(&mut ctx.opts.bypass_charging, val);
                    } else {
                        GameConfig::set_field(&mut ctx.opts.bypass_charging, "default");
                    }
                }
                _ => {}
            }
        }
    }

    fileops::remove_file_best_effort(DAEMON_STATE_FILE);

    log_info!("state", "state restored renderer={} refresh={} mode={} bypass={} bypass_charging={}",
        ctx.saved_renderer_str(), ctx.display.saved_refresh_rate,
        profile_mode_name(ctx.profile.cur_mode),
        if ctx.bypass.bypass_applied { 1 } else { 0 },
        ctx.opts_bypass_charging()
    );
}
