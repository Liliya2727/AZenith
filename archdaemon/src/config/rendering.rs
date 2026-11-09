//! Rendering / HWUI.

use crate::{log_debug, log_info};
use crate::shell::systemv;

use crate::sysprop::{get_or_default, set};

// ---------------------------------------------------------------------------
// apply_smart_renderer
// ---------------------------------------------------------------------------
pub fn apply_smart_renderer(target_type: &str, saved_ref: &mut [u8], saved_sys: &mut [u8]) -> bool {
    if target_type == "default" || target_type.is_empty() {
        return false;
    }

    let current_renderer = get_or_default("debug.hwui.renderer", "");
    let current_sys_renderer = get_or_default("persist.sys.azenithconf.renderer", "");

    let current_renderer = if current_renderer.is_empty() { "default".to_string() } else { current_renderer };
    let current_sys_renderer = if current_sys_renderer.is_empty() { "default".to_string() } else { current_sys_renderer };

    let saved_ref_len = saved_ref.iter().position(|&b| b == 0).unwrap_or(saved_ref.len());
    if saved_ref_len == 0 {
        let bytes = current_renderer.as_bytes();
        let n = bytes.len().min(saved_ref.len() - 1);
        saved_ref[..n].copy_from_slice(&bytes[..n]);
        saved_ref[n] = 0;
    }

    let saved_sys_len = saved_sys.iter().position(|&b| b == 0).unwrap_or(saved_sys.len());
    if saved_sys_len == 0 {
        let bytes = current_sys_renderer.as_bytes();
        let n = bytes.len().min(saved_sys.len() - 1);
        saved_sys[..n].copy_from_slice(&bytes[..n]);
        saved_sys[n] = 0;
    }

    if current_renderer != target_type {
        log_info!("render", "renderer changed from={} to={}", current_renderer, target_type);
        systemv(&format!("sys.azenith-utilityconf setrender {target_type}"));
        set("persist.sys.azenithconf.renderer", target_type);
        return true;
    }

    log_debug!("render", "renderer already matches value={}", current_renderer);
    false
}
