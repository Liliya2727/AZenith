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
// Value normalization (Stage 2)
// ---------------------------------------------------------------------------
const LEGACY_VALID_RATIOS: &[&str] = &[
    "0.3", "0.35", "0.4", "0.45", "0.5", "0.55", "0.6", "0.65", "0.7", "0.75", "0.8", "0.85", "0.9",
];

const SDK33_VALID_FPS: &[i32] = &[30, 40, 45, 60, 90, 120];

fn strip_trailing_zeros(raw: &str) -> String {
    let s = raw.trim();
    if !s.contains('.') {
        return s.to_string();
    }
    let trimmed = s.trim_end_matches('0').trim_end_matches('.');
    if trimmed.is_empty() {
        "0".to_string()
    } else {
        trimmed.to_string()
    }
}

fn normalize_ratio(raw: &str, sdk: i32) -> Option<String> {
    if IS_DEFAULT!(raw) {
        return None;
    }
    let stripped = strip_trailing_zeros(raw);
    if sdk > 33 {
        return Some(stripped);
    }

    if stripped == "0.95" {
        log_warn!(
            "resolution",
            "clamping downscale ratio raw={} -> 0.9 (sdk={} max supported is 0.9)",
            raw,
            sdk
        );
        return Some("0.9".to_string());
    }

    if LEGACY_VALID_RATIOS.contains(&stripped.as_str()) {
        return Some(stripped);
    }

    // Clamp arbitrary float on SDK <= 33 to nearest allowed step in 0.3..=0.9
    if let Ok(v) = stripped.parse::<f32>() {
        let mut best = LEGACY_VALID_RATIOS[0];
        let mut best_diff = f32::MAX;
        for &cand in LEGACY_VALID_RATIOS {
            if let Ok(cv) = cand.parse::<f32>() {
                let diff = (v - cv).abs();
                if diff < best_diff {
                    best_diff = diff;
                    best = cand;
                }
            }
        }
        log_warn!(
            "resolution",
            "clamping unsupported downscale ratio raw={} -> {} (sdk={})",
            raw,
            best,
            sdk
        );
        return Some(best.to_string());
    }

    log_error!("resolution", "invalid downscale ratio raw={} sdk={}", raw, sdk);
    None
}

fn normalize_fps(raw: &str, sdk: i32) -> Option<String> {
    if IS_DEFAULT!(raw) {
        return None;
    }
    let trimmed = raw.trim();
    if sdk != 33 {
        return Some(trimmed.to_string());
    }

    match trimmed.parse::<i32>() {
        Ok(v) => {
            if SDK33_VALID_FPS.contains(&v) {
                return Some(v.to_string());
            }
            let clamped = SDK33_VALID_FPS
                .iter()
                .copied()
                .min_by_key(|&cand| (cand - v).abs())
                .unwrap_or(60);
            log_warn!(
                "resolution",
                "clamping fps raw={} -> {} (sdk=33 allowed: 30,40,45,60,90,120)",
                raw,
                clamped
            );
            Some(clamped.to_string())
        }
        Err(_) => {
            log_error!("resolution", "invalid fps value raw={} sdk={}", raw, sdk);
            None
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

    let norm_downscale = normalize_ratio(downscale, sdk).unwrap_or_else(|| "default".to_string());
    let norm_fps = normalize_fps(fps, sdk).unwrap_or_else(|| "default".to_string());
    if IS_DEFAULT!(norm_downscale) && IS_DEFAULT!(norm_fps) {
        log_warn!("resolution", "no valid downscale/fps after normalization pkg={pkg} sdk={sdk}");
        return false;
    }

    if sdk >= 33 {
        if apply_game_set(pkg, &norm_downscale, &norm_fps) {
            ctx.render.resolution_applied = true;
            ctx.render.used_legacy_fallback = false;
            log_info!("resolution", "applied pkg={pkg} sdk={sdk} downscale={} fps={}",
                if IS_DEFAULT!(norm_downscale) { "-" } else { &norm_downscale },
                if IS_DEFAULT!(norm_fps) { "-" } else { &norm_fps });
            return true;
        }
        log_warn!("resolution", "cmd game set failed pkg={pkg}, falling back to device_config");
    }

    if !apply_legacy(pkg, &norm_downscale, &norm_fps) {
        log_warn!("resolution", "legacy apply failed pkg={pkg} sdk={sdk}");
        return false;
    }

    ctx.render.resolution_applied = true;
    ctx.render.used_legacy_fallback = true;
    log_info!("resolution", "applied pkg={pkg} sdk={sdk} mode=legacy downscale={} fps={}",
        if IS_DEFAULT!(norm_downscale) { "-" } else { &norm_downscale },
        if IS_DEFAULT!(norm_fps) { "-" } else { &norm_fps });
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
