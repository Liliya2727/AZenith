//! Resolution / game-mode.

use crate::core::DaemonContext;
use crate::IS_DEFAULT;
use crate::sysprop::get_or_default;
use crate::{log_debug, log_info, log_warn};
use crate::shell::systemv;

// ---------------------------------------------------------------------------
// get_prop_int
// ---------------------------------------------------------------------------
fn get_prop_int(prop: &str) -> i32 {
    let val = get_or_default(prop, "0");
    val.parse::<i32>().unwrap_or(0)
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

    systemv(&cmd) == 0
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

    if systemv(&format!("cmd device_config put game_overlay {pkg} {config}")) != 0 {
        return false;
    }

    systemv(&format!("cmd game mode 2 {pkg}")) == 0
}

// ---------------------------------------------------------------------------
// apply_resolution_target
// ---------------------------------------------------------------------------
pub fn apply_resolution_target(ctx: &mut DaemonContext, pkg: &str, downscale: &str, fps: &str) -> bool {
    if IS_DEFAULT!(downscale) && IS_DEFAULT!(fps) {
        return false;
    }

    if ctx.render.resolution_applied {
        log_debug!("resolution", "already applied pkg={pkg}, skipping");
        return false;
    }

    let sdk = get_prop_int("ro.build.version.sdk");
    if sdk < 31 {
        log_debug!("resolution", "game mode API unsupported sdk={sdk}");
        return false;
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
        log_warn!("resolution", "cmd game set failed pkg={pkg}, falling back to device_config",);
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
        systemv(&format!("cmd device_config delete game_overlay {pkg}"));
        if systemv(&format!("cmd game reset {pkg}")) != 0 {
            systemv(&format!("cmd game mode 1 {pkg}"));
        }
    } else {
        systemv(&format!("cmd game reset --mode 2 {pkg}"));
    }

    log_info!("resolution", "reset pkg={pkg} legacy={}", if ctx.render.used_legacy_fallback { 1 } else { 0 });
    ctx.render.resolution_applied = false;
}
