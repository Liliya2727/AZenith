//! Resolution / game-mode.

use crate::core::DaemonContext;
use crate::IS_DEFAULT;
use crate::sysprop::get_or_default;
use crate::{log_debug, log_error, log_info, log_warn};
use crate::shell::{CmdResult, systemv_capture_both};

const ERROR_PATTERNS: &[&str] = &[
    "Invalid",
    "not supported",
    "not of game type",
    "Unknown command",
    "No intervention",
    "Error",
];

fn output_has_error(output: &str) -> bool {
    ERROR_PATTERNS.iter().any(|pat| output.contains(pat))
}

fn run_res_cmd(cmd: &str) -> (CmdResult, bool) {
    let res = systemv_capture_both(cmd);
    let has_err = res.exit_code != 0 || output_has_error(&res.output);
    if has_err {
        log_error!(
            "resolution",
            "cmd failed rc={} cmd=\"{}\" out=\"{}\"",
            res.exit_code,
            cmd,
            res.output
        );
    }
    (res, !has_err)
}

// ---------------------------------------------------------------------------
// get_prop_int
// ---------------------------------------------------------------------------
fn get_prop_int(prop: &str) -> i32 {
    let val = get_or_default(prop, "");
    match val.trim().parse::<i32>() {
        Ok(v) => v,
        Err(_) => {
            log_warn!("resolution", "failed to parse prop={} raw={:?}", prop, val);
            0
        }
    }
}

// ---------------------------------------------------------------------------
// apply_game_set (Android 13+)
// ---------------------------------------------------------------------------
fn apply_game_set(pkg: &str, downscale: &str, fps: &str) -> bool {
    let mut cmd = String::from("cmd game set --mode 2");

    if !IS_DEFAULT!(downscale) {
        cmd.push_str(&format!(" --downscale {downscale}"));
    }
    if !IS_DEFAULT!(fps) {
        cmd.push_str(&format!(" --fps {fps}"));
    }
    cmd.push_str(&format!(" {pkg}"));

    let (_, ok) = run_res_cmd(&cmd);
    ok
}

// ---------------------------------------------------------------------------
// apply_legacy (Android 12 fallback)
// ---------------------------------------------------------------------------
fn apply_legacy(pkg: &str, downscale: &str, fps: &str) -> bool {
    let mut config = String::from("mode=2");

    if !IS_DEFAULT!(downscale) {
        config.push_str(&format!(",downscaleFactor={downscale}"));
    }
    if !IS_DEFAULT!(fps) {
        config.push_str(&format!(",fps={fps}"));
    }

    let (_, ok_put) = run_res_cmd(&format!("cmd device_config put game_overlay {pkg} {config}"));
    if !ok_put {
        return false;
    }

    let (_, ok_mode) = run_res_cmd(&format!("cmd game mode 2 {pkg}"));
    ok_mode
}

// ---------------------------------------------------------------------------
// apply_resolution_target
// ---------------------------------------------------------------------------
pub fn apply_resolution_target(ctx: &mut DaemonContext, pkg: &str, downscale: &str, fps: &str) -> bool {
    let first_for_pkg = ctx.render.last_resolution_log_pkg.as_deref() != Some(pkg);
    if first_for_pkg {
        ctx.render.last_resolution_log_pkg = Some(pkg.to_string());
    }

    if IS_DEFAULT!(downscale) && IS_DEFAULT!(fps) {
        if first_for_pkg {
            log_info!("resolution", "skip pkg={pkg} reason=default_settings");
        }
        return false;
    }

    if ctx.render.resolution_applied {
        log_debug!("resolution", "already applied pkg={pkg}, skipping");
        return false;
    }

    let sdk = get_prop_int("ro.build.version.sdk");
    if sdk < 31 {
        if first_for_pkg {
            log_info!("resolution", "skip pkg={pkg} sdk={sdk} reason=sdk_below_31");
        } else {
            log_debug!("resolution", "game mode API unsupported sdk={sdk}");
        }
        return false;
    }

    if first_for_pkg {
        log_info!("resolution", "init pkg={pkg} sdk={sdk} downscale={downscale} fps={fps}");
    }

    if sdk >= 33 {
        if apply_game_set(pkg, downscale, fps) {
            ctx.render.resolution_applied = true;
            ctx.render.used_legacy_fallback = false;
            log_info!("resolution", "applied pkg={pkg} sdk={sdk} downscale={} fps={}",
                if IS_DEFAULT!(downscale) { "-" } else { downscale },
                if IS_DEFAULT!(fps) { "-" } else { fps });
            return true;
        }
        log_warn!("resolution", "cmd game set failed pkg={pkg}, falling back to device_config");
    }

    if !apply_legacy(pkg, downscale, fps) {
        log_warn!("resolution", "legacy apply failed pkg={pkg} sdk={sdk}");
        return false;
    }

    ctx.render.resolution_applied = true;
    ctx.render.used_legacy_fallback = true;
    log_info!("resolution", "applied pkg={pkg} sdk={sdk} mode=legacy downscale={} fps={}",
        if IS_DEFAULT!(downscale) { "-" } else { downscale },
        if IS_DEFAULT!(fps) { "-" } else { fps });
    true
}

// ---------------------------------------------------------------------------
// restore_resolution_target
// ---------------------------------------------------------------------------
pub fn restore_resolution_target(ctx: &mut DaemonContext, pkg: &str) {
    if !ctx.render.resolution_applied {
        return;
    }

    if ctx.render.used_legacy_fallback {
        let _ = run_res_cmd(&format!("cmd device_config delete game_overlay {pkg}"));
        let (_, ok_reset) = run_res_cmd(&format!("cmd game reset {pkg}"));
        if !ok_reset {
            let _ = run_res_cmd(&format!("cmd game mode 1 {pkg}"));
        }
    } else {
        let _ = run_res_cmd(&format!("cmd game reset --mode 2 {pkg}"));
    }

    log_info!("resolution", "reset pkg={pkg} legacy={}", if ctx.render.used_legacy_fallback { 1 } else { 0 });
    ctx.render.resolution_applied = false;
}
