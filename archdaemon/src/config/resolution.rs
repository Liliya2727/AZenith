//! Resolution / game-mode.

use crate::core::{DaemonContext, GameSnapshot};
use crate::IS_DEFAULT;
use crate::sysprop::get_or_default;
use crate::{log_debug, log_error, log_info, log_warn};
use crate::shell::{CmdResult, systemv_capture_both};
use std::sync::OnceLock;

// ---------------------------------------------------------------------------
// Capability probe (Stage 3)
// ---------------------------------------------------------------------------
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum GameCapability {
    /// Android 14+: `cmd game set` with custom mode + `list-configs`.
    SetCustom,
    /// Android 13: `cmd game set --mode [2|3|...]`.
    SetMode,
    /// Android 12 / 12L: `cmd game downscale [ratio|disable] <pkg>`.
    Downscale,
    /// Service missing or unrecognized.
    None,
}

static GAME_CAPABILITY: OnceLock<GameCapability> = OnceLock::new();

fn parse_help_capability(help_out: &str) -> GameCapability {
    if help_out.is_empty()
        || help_out.contains("Can't find service")
        || help_out.contains("Unknown command")
    {
        return GameCapability::None;
    }
    if help_out.contains("list-configs") || help_out.contains("set [intervention configs]") {
        return GameCapability::SetCustom;
    }
    if help_out.contains("set --mode") {
        return GameCapability::SetMode;
    }
    if help_out.contains("downscale [") {
        return GameCapability::Downscale;
    }
    GameCapability::None
}

fn fallback_capability_from_sdk(sdk: i32) -> GameCapability {
    if sdk >= 34 {
        GameCapability::SetCustom
    } else if sdk == 33 {
        GameCapability::SetMode
    } else if sdk >= 31 {
        GameCapability::Downscale
    } else {
        GameCapability::None
    }
}

/// Probes `cmd game help` once at startup (cached). Never uses `--help`,
/// and ignores rc=255 because AOSP's `ShellCommand.handleDefaultCommands("help")`
/// returns `-1`.
pub fn probe_game_capability() -> GameCapability {
    *GAME_CAPABILITY.get_or_init(|| {
        // Never use `--help`; `cmd game help` returns rc=255 (-1) even on success.
        let res = systemv_capture_both("cmd game help");
        let parsed = parse_help_capability(&res.output);
        if parsed != GameCapability::None {
            log_info!(
                "resolution",
                "capability probe ok cap={:?} rc={}",
                parsed,
                res.exit_code
            );
            return parsed;
        }

        let sdk = get_prop_int("ro.build.version.sdk");
        let fb = fallback_capability_from_sdk(sdk);
        log_warn!(
            "resolution",
            "capability probe failed rc={} out={:?}, falling back to sdk={} cap={:?}",
            res.exit_code,
            res.output,
            sdk,
            fb
        );
        fb
    })
}

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
// Snapshot & Restore helpers (Stage 4)
// ---------------------------------------------------------------------------
fn parse_mode_block(configs_out: &str, mode_num: u8) -> (bool, Option<String>, Option<String>) {
    let needle = format!("Game Mode:{mode_num},");
    let Some(start) = configs_out.find(&needle) else {
        return (false, None, None);
    };
    let rest = &configs_out[start + needle.len()..];
    let block = match rest.find(']') {
        Some(end) => &rest[..end],
        None => rest,
    };

    let mut scaling = None;
    let mut fps = None;
    for part in block.split(',') {
        let part = part.trim();
        if let Some(val) = part.strip_prefix("Scaling:") {
            let v = val.trim();
            if !v.is_empty() {
                scaling = Some(v.to_string());
            }
        } else if let Some(val) = part.strip_prefix("Fps:") {
            let v = val.trim();
            if !v.is_empty() {
                fps = Some(v.to_string());
            }
        }
    }
    (true, scaling, fps)
}

fn parse_current_mode(modes_out: &str) -> Option<String> {
    let needle = "current mode:";
    let idx = modes_out.find(needle)?;
    let rest = modes_out[idx + needle.len()..].trim_start();
    let mode: String = rest
        .chars()
        .take_while(|c| c.is_ascii_alphanumeric() || *c == '_')
        .collect();
    if mode.is_empty() { None } else { Some(mode) }
}

fn capture_game_snapshot(pkg: &str, cap: GameCapability) -> GameSnapshot {
    let mut snap = GameSnapshot::default();

    // Capture any pre-existing device_config game_overlay entry so we never
    // wipe a value AZenith did not create.
    let dc_res = systemv_capture_both(&format!("cmd device_config get game_overlay {pkg}"));
    if dc_res.exit_code == 0 {
        let v = dc_res.output.trim();
        if !v.is_empty() && v != "null" && !output_has_error(v) {
            snap.prev_device_config = Some(v.to_string());
        }
    }

    if cap != GameCapability::SetCustom {
        return snap;
    }

    let modes_res = systemv_capture_both(&format!("cmd game list-modes {pkg}"));
    let configs_res = systemv_capture_both(&format!("cmd game list-configs {pkg}"));

    if modes_res.exit_code != 0 || configs_res.exit_code != 0 {
        log_warn!(
            "resolution",
            "snapshot capture failed pkg={pkg} modes_rc={} configs_rc={}",
            modes_res.exit_code,
            configs_res.exit_code
        );
        return snap;
    }

    let current_mode = parse_current_mode(&modes_res.output);
    let no_interventions = configs_res.output.contains("No interventions found");
    let has_interventions = configs_res.output.contains("interventions:");

    if current_mode.is_none() || (!no_interventions && !has_interventions) {
        log_warn!(
            "resolution",
            "snapshot parse failed pkg={pkg} modes_out={:?} configs_out={:?}",
            modes_res.output,
            configs_res.output
        );
        return snap;
    }

    snap.current_mode = current_mode;
    if !no_interventions {
        let (h2, s2, f2) = parse_mode_block(&configs_res.output, 2);
        snap.had_mode2_config = h2;
        snap.mode2_scaling = s2;
        snap.mode2_fps = f2;

        let (h4, s4, f4) = parse_mode_block(&configs_res.output, 4);
        snap.had_mode4_config = h4;
        snap.mode4_scaling = s4;
        snap.mode4_fps = f4;
    }
    snap.parsed_ok = true;
    log_debug!(
        "resolution",
        "snapshot captured pkg={pkg} mode={:?} had_m2={} m2_scale={:?} m2_fps={:?}",
        snap.current_mode,
        snap.had_mode2_config,
        snap.mode2_scaling,
        snap.mode2_fps
    );
    snap
}

fn restore_device_config(pkg: &str, prev: Option<&str>) {
    match prev {
        Some(val) if !val.is_empty() => {
            let _ = run_res_cmd(&format!("cmd device_config put game_overlay {pkg} {val}"));
        }
        _ => {
            let _ = run_res_cmd(&format!("cmd device_config delete game_overlay {pkg}"));
        }
    }
}

fn get_current_user_id() -> String {
    let res = systemv_capture_both("am get-current-user");
    let trimmed = res.output.trim();
    if res.exit_code == 0 && !trimmed.is_empty() && trimmed.chars().all(|c| c.is_ascii_digit()) {
        return trimmed.to_string();
    }
    log_warn!(
        "resolution",
        "am get-current-user failed rc={} out={:?}, defaulting to user 0",
        res.exit_code,
        res.output
    );
    "0".to_string()
}

fn restore_from_snapshot(pkg: &str, snap: Option<&GameSnapshot>, cap: GameCapability) {
    let user_id = get_current_user_id();
    let Some(snap) = snap else {
        log_warn!("resolution", "no snapshot for pkg={pkg}, falling back to safe reset");
        if cap == GameCapability::SetCustom {
            let _ = run_res_cmd(&format!(
                "cmd game set --user {user_id} --downscale disable --fps 0 {pkg}"
            ));
            let _ = run_res_cmd(&format!("cmd game mode --user {user_id} 1 {pkg}"));
        } else {
            let _ = run_res_cmd(&format!("cmd game reset --mode 2 --user {user_id} {pkg}"));
        }
        return;
    };

    if !snap.parsed_ok {
        log_warn!("resolution", "unparsed snapshot for pkg={pkg}, falling back to safe reset");
        if cap == GameCapability::SetCustom {
            let _ = run_res_cmd(&format!(
                "cmd game set --user {user_id} --downscale disable --fps 0 {pkg}"
            ));
            let _ = run_res_cmd(&format!("cmd game mode --user {user_id} 1 {pkg}"));
        } else {
            let _ = run_res_cmd(&format!("cmd game reset --mode 2 --user {user_id} {pkg}"));
        }
        return;
    }

    if cap == GameCapability::SetCustom {
        // A14+ uses custom mode (mode 4) so ROM mode-2 configs are never touched.
        if snap.had_mode4_config {
            let mut cmd = format!("cmd game set --user {user_id}");
            if let Some(ref s) = snap.mode4_scaling {
                if s.starts_with('-') {
                    cmd.push_str(" --downscale disable");
                } else {
                    cmd.push_str(&format!(" --downscale {s}"));
                }
            } else {
                cmd.push_str(" --downscale disable");
            }
            if let Some(ref f) = snap.mode4_fps {
                cmd.push_str(&format!(" --fps {f}"));
            } else {
                cmd.push_str(" --fps 0");
            }
            cmd.push_str(&format!(" {pkg}"));
            let _ = run_res_cmd(&cmd);
        } else {
            let _ = run_res_cmd(&format!(
                "cmd game set --user {user_id} --downscale disable --fps 0 {pkg}"
            ));
        }
    } else if snap.had_mode2_config {
        let mut cmd = format!("cmd game set --mode 2 --user {user_id}");
        if let Some(ref s) = snap.mode2_scaling {
            if s.starts_with('-') {
                cmd.push_str(" --downscale disable");
            } else {
                cmd.push_str(&format!(" --downscale {s}"));
            }
        }
        if let Some(ref f) = snap.mode2_fps {
            cmd.push_str(&format!(" --fps {f}"));
        }
        cmd.push_str(&format!(" {pkg}"));
        let _ = run_res_cmd(&cmd);
    } else {
        // AZenith created the mode-2 override where none existed previously.
        let _ = run_res_cmd(&format!("cmd game reset --mode 2 --user {user_id} {pkg}"));
    }

    if let Some(ref mode) = snap.current_mode {
        if mode != "unsupported" {
            let _ = run_res_cmd(&format!("cmd game mode --user {user_id} {mode} {pkg}"));
        }
    }
}

// ---------------------------------------------------------------------------
// apply_game_set_custom (Android 14+, custom mode 4, preserves ROM mode 2)
// ---------------------------------------------------------------------------
fn apply_game_set_custom(pkg: &str, user_id: &str, downscale: &str, fps: &str) -> bool {
    let mut cmd = format!("cmd game set --user {user_id}");

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
// apply_game_set_mode2 (Android 13, requires --mode 2 first)
// ---------------------------------------------------------------------------
fn apply_game_set_mode2(pkg: &str, user_id: &str, downscale: &str, fps: &str) -> bool {
    let mut cmd = format!("cmd game set --mode 2 --user {user_id}");

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
fn apply_legacy(pkg: &str, downscale: &str, fps: &str, prev_dc: Option<&str>) -> bool {
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
    if !ok_mode {
        // Clean up orphan device_config entry on partial failure.
        log_warn!(
            "resolution",
            "apply_legacy partial failure pkg={pkg}, reverting device_config"
        );
        restore_device_config(pkg, prev_dc);
        return false;
    }
    true
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

    let cap = probe_game_capability();
    if cap == GameCapability::None {
        if first_for_pkg {
            log_info!("resolution", "skip pkg={pkg} sdk={sdk} cap=None reason=unsupported");
        } else {
            log_debug!("resolution", "game mode API unsupported sdk={sdk} cap=None");
        }
        return false;
    }

    if first_for_pkg {
        log_info!(
            "resolution",
            "init pkg={pkg} sdk={sdk} cap={:?} downscale={downscale} fps={fps}",
            cap
        );
    }

    let norm_downscale = normalize_ratio(downscale, sdk).unwrap_or_else(|| "default".to_string());
    let norm_fps = normalize_fps(fps, sdk).unwrap_or_else(|| "default".to_string());
    if IS_DEFAULT!(norm_downscale) && IS_DEFAULT!(norm_fps) {
        log_warn!("resolution", "no valid downscale/fps after normalization pkg={pkg} sdk={sdk}");
        return false;
    }

    let snapshot = capture_game_snapshot(pkg, cap);
    let user_id = get_current_user_id();

    if cap == GameCapability::SetCustom {
        if apply_game_set_custom(pkg, &user_id, &norm_downscale, &norm_fps) {
            ctx.render.resolution_applied = true;
            ctx.render.used_legacy_fallback = false;
            ctx.render.resolution_snapshot = Some(snapshot);
            log_info!("resolution", "applied pkg={pkg} sdk={sdk} cap={:?} user={user_id} downscale={} fps={}",
                cap,
                if IS_DEFAULT!(norm_downscale) { "-" } else { &norm_downscale },
                if IS_DEFAULT!(norm_fps) { "-" } else { &norm_fps });
            return true;
        }
        log_warn!("resolution", "cmd game set (custom) failed pkg={pkg}, falling back to device_config");
    } else if cap == GameCapability::SetMode {
        if apply_game_set_mode2(pkg, &user_id, &norm_downscale, &norm_fps) {
            ctx.render.resolution_applied = true;
            ctx.render.used_legacy_fallback = false;
            ctx.render.resolution_snapshot = Some(snapshot);
            log_info!("resolution", "applied pkg={pkg} sdk={sdk} cap={:?} user={user_id} downscale={} fps={}",
                cap,
                if IS_DEFAULT!(norm_downscale) { "-" } else { &norm_downscale },
                if IS_DEFAULT!(norm_fps) { "-" } else { &norm_fps });
            return true;
        }
        log_warn!("resolution", "cmd game set --mode 2 failed pkg={pkg}, falling back to device_config");
    }

    if !apply_legacy(pkg, &norm_downscale, &norm_fps, snapshot.prev_device_config.as_deref()) {
        log_warn!("resolution", "legacy apply failed pkg={pkg} sdk={sdk}");
        if snapshot.parsed_ok {
            restore_from_snapshot(pkg, Some(&snapshot), cap);
        }
        return false;
    }

    ctx.render.resolution_applied = true;
    ctx.render.used_legacy_fallback = true;
    ctx.render.resolution_snapshot = Some(snapshot);
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

    let snap = ctx.render.resolution_snapshot.take();
    let cap = probe_game_capability();

    if ctx.render.used_legacy_fallback {
        let prev_dc = snap.as_ref().and_then(|s| s.prev_device_config.as_deref());
        restore_device_config(pkg, prev_dc);
        if snap.as_ref().is_some_and(|s| s.parsed_ok) {
            restore_from_snapshot(pkg, snap.as_ref(), cap);
        } else {
            let (_, ok_reset) = run_res_cmd(&format!("cmd game reset {pkg}"));
            if !ok_reset {
                let _ = run_res_cmd(&format!("cmd game mode 1 {pkg}"));
            }
        }
    } else {
        restore_from_snapshot(pkg, snap.as_ref(), cap);
    }

    log_info!("resolution", "reset pkg={pkg} legacy={}", if ctx.render.used_legacy_fallback { 1 } else { 0 });
    ctx.render.resolution_applied = false;
}
