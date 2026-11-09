//! Monitor: app status + gamelist.

use crate::core::{
    GameConfig, SystemStateCache, APP_MONITOR_FILE, GAMELIST,
};
use crate::{log_error, log_info, log_warn};

use std::fs;
use std::io::BufRead;

// ---------------------------------------------------------------------------
// read_app_status
// ---------------------------------------------------------------------------
pub fn read_app_status(cache: &mut SystemStateCache) {
    read_app_status_at(APP_MONITOR_FILE, cache)
}

pub fn read_app_status_at(path: &str, cache: &mut SystemStateCache) {
    let Ok(fp) = fs::File::open(path) else {
        crate::log_debug!("monitor", "read_app_status: file open failed");
        return;
    };

    cache.focused_app = [0; 128];
    cache.focused_pid = 0;
    cache.app_name = [0; 256];
    cache.app_name[..7].copy_from_slice(b"Unknown");
    cache.screen_awake = 0;
    cache.battery_saver = 0;
    cache.zen_mode = 0;
    cache.battery_level = -1;
    cache.is_charging = 0;

    let mut lines_read = 0;
    let mut found_focused = false;

    for line in std::io::BufReader::new(fp).lines().flatten() {
        lines_read += 1;
        if let Some(rest) = line.strip_prefix("focused_app ") {
            let mut parts = rest.split_whitespace();
            if let Some(pkg) = parts.next() {
                let bytes = pkg.as_bytes();
                let n = bytes.len().min(cache.focused_app.len() - 1);
                cache.focused_app[..n].copy_from_slice(&bytes[..n]);
                cache.focused_app[n] = 0;
                crate::log_debug!("monitor", "read_app_status: focused_app={}", pkg);
                found_focused = true;
            }
            if let Some(pid) = parts.next().and_then(|s| s.parse::<i32>().ok()) {
                cache.focused_pid = pid;
            }
            // The third field (uid) is intentionally ignored, matching C.
        } else if let Some(rest) = line.strip_prefix("screen_awake ") {
            cache.screen_awake = rest.trim().parse::<i32>().unwrap_or(0);
        } else if let Some(rest) = line.strip_prefix("battery_saver ") {
            cache.battery_saver = rest.trim().parse::<i32>().unwrap_or(0);
        } else if let Some(rest) = line.strip_prefix("zen_mode ") {
            cache.zen_mode = rest.trim().parse::<i32>().unwrap_or(0);
        } else if let Some(rest) = line.strip_prefix("app_name ") {
            let bytes = rest.trim_end_matches('\r').as_bytes();
            let n = bytes.len().min(cache.app_name.len() - 1);
            cache.app_name[..n].copy_from_slice(&bytes[..n]);
            cache.app_name[n] = 0;
        } else if let Some(rest) = line.strip_prefix("battery_level ") {
            cache.battery_level = rest.trim().parse::<i32>().unwrap_or(-1);
        } else if let Some(rest) = line.strip_prefix("is_charging ") {
            cache.is_charging = rest.trim().parse::<i32>().unwrap_or(0);
        }
    }
    crate::log_debug!("monitor", "read_app_status: {} lines, focused_found={} focused={}", lines_read, found_focused, cache.focused_app_str());
}

// ---------------------------------------------------------------------------
// get_visible_package
// ---------------------------------------------------------------------------
pub fn get_visible_package(cache: &SystemStateCache) -> Option<String> {
    use crate::profile::get_screenstate;

    if !get_screenstate(cache) {
        return None;
    }

    let focused = cache.focused_app_str();
    if focused.is_empty() {
        return None;
    }

    Some(focused.to_string())
}

// ---------------------------------------------------------------------------
// free_gamelist_cache
// ---------------------------------------------------------------------------
pub fn free_gamelist_cache() {
    let _mutex = crate::core::CACHE_MUTEX.get().unwrap().lock().unwrap();
    if let Some(cache) = crate::core::G_GAME_CACHE.get() {
        cache.write().unwrap().clear();
    }
    crate::core::set_game_cache_count(0);
}

// ---------------------------------------------------------------------------
// reload_gamelist_cache
// ---------------------------------------------------------------------------
pub fn reload_gamelist_cache(ctx: &mut crate::core::DaemonContext) {
    reload_gamelist_cache_at(ctx, GAMELIST)
}

pub fn reload_gamelist_cache_at(ctx: &mut crate::core::DaemonContext, path: &str) {
    let _mutex = crate::core::CACHE_MUTEX.get().unwrap().lock().unwrap();
    let cache_lock = crate::core::G_GAME_CACHE.get().expect("game cache initialized");
    let mut cache = cache_lock.write().unwrap();
    cache.clear();
    crate::core::set_game_cache_count(0);

    let content = match fs::read_to_string(path) {
        Ok(c) => c,
        Err(e) => {
            log_error!("apploader", "gamelist open failed path={} errno={}", path, e.raw_os_error().unwrap_or(-1));
            return;
        }
    };

    if content.is_empty() {
        log_warn!("apploader", "gamelist empty path={}", path);
        crate::core::set_game_cache_count(0);
        return;
    }

    let root: serde_json::Value = match serde_json::from_str(&content) {
        Ok(value) => value,
        Err(error) => {
            log_error!("apploader", "gamelist JSON parse failed path={} error={}", path, error);
            return;
        }
    };
    let Some(games) = root.as_object() else {
        log_warn!("apploader", "gamelist root must be a JSON object path={}", path);
        return;
    };
    for (package, value) in games {
        let Some(fields) = value.as_object() else { continue };
        if package.is_empty() { continue; }
        let mut entry = GameConfig::default();
        GameConfig::set_field(&mut entry.package, package);
        macro_rules! set {
            ($field:ident, $key:literal) => {
                GameConfig::set_field(
                    &mut entry.$field,
                    fields.get($key).and_then(serde_json::Value::as_str).unwrap_or("default"),
                );
            };
        }
        set!(perf_lite_mode, "perf_lite_mode");
        set!(dnd_on_gaming, "dnd_on_gaming");
        set!(app_priority, "app_priority");
        set!(game_preload, "game_preload");
        set!(refresh_rate, "refresh_rate");
        set!(renderer, "renderer");
        set!(resolution_downscale, "resolution_downscale");
        set!(resolution_fps, "resolution_fps");
        set!(bypass_charging, "bypass_charging");
        cache.push(entry);
    }
    crate::core::set_game_cache_count(cache.len() as i32);
    if !ctx.is_initialize_complete {
        log_info!("apploader", "gamelist cached games={}", cache.len());
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::core::DaemonContext;
    use crate::profile::get_gamestart;
    use std::sync::atomic::{AtomicU32, Ordering};

    static SEQ: AtomicU32 = AtomicU32::new(0);

    fn tmp_file(tag: &str, content: &str) -> std::path::PathBuf {
        let n = SEQ.fetch_add(1, Ordering::Relaxed);
        let p = std::env::temp_dir().join(format!(
            "azenith_test_{}_{}_{}",
            std::process::id(),
            tag,
            n
        ));
        std::fs::write(&p, content).unwrap();
        p
    }

    const APP_STATUS_GAME: &str = "focused_app com.test.game 4242 10001\nscreen_awake 1\nbattery_saver 0\nzen_mode 0\nbattery_level 80\nis_charging 1\napp_name Test Game\nrefresh_rate 120\nmax_refresh_rate 120\n";

    const APP_STATUS_NONGAME: &str = "focused_app bin.mt.plus.canary 5555 10002\nscreen_awake 1\nbattery_saver 0\nzen_mode 0\nbattery_level 80\nis_charging 1\napp_name MT Manager\nrefresh_rate 120\nmax_refresh_rate 120\n";

    const GAMELIST_JSON: &str = r#"{ "com.test.game": { "perf_lite_mode": "true", "dnd_on_gaming": "true", "app_priority": "true", "game_preload": "false", "refresh_rate": "120", "renderer": "default", "resolution_downscale": "default", "resolution_fps": "default", "bypass_charging": "false" }, "com.other.game": { "perf_lite_mode": "false", "dnd_on_gaming": "false", "app_priority": "false", "game_preload": "false", "refresh_rate": "default", "renderer": "default", "resolution_downscale": "default", "resolution_fps": "default", "bypass_charging": "true" } }"#;

    // Full chain: app_status file -> SystemStateCache -> visible package ->
    // gamelist match -> options copied. Repeated 3x like a real session of
    // foreground switches (game -> manager -> game).
    #[test]
    fn detection_chain_game_and_nongame() {
        crate::core::init_globals();
        crate::profile::init_screenstate_fn();

        let gamelist_path = tmp_file("gamelist", GAMELIST_JSON);
        let status_path = tmp_file("appstatus", APP_STATUS_GAME);

        for round in 0..3 {
            let mut ctx = DaemonContext::new();
            reload_gamelist_cache_at(
                &mut ctx,
                gamelist_path.to_str().unwrap(),
            );
            assert_eq!(crate::core::game_cache_count(), 2, "round {round}");

            // Game in foreground -> detected, options copied like C.
            read_app_status_at(status_path.to_str().unwrap(), &mut ctx.current_system_cache);
            assert_eq!(ctx.current_system_cache.focused_app_str(), "com.test.game");
            assert_eq!(ctx.current_system_cache.focused_pid, 4242);
            assert_eq!(ctx.current_system_cache.battery_level, 80);
            assert_eq!(ctx.current_system_cache.is_charging, 1);
            assert_eq!(ctx.current_system_cache.app_name_str(), "Test Game");

            let mut opts = GameConfig::default();
            let gs = get_gamestart(&mut opts, &ctx.current_system_cache);
            assert_eq!(gs.as_deref(), Some("com.test.game"), "round {round}");
            assert_eq!(opts.field_as_str(&opts.refresh_rate), "120");
            assert_eq!(opts.field_as_str(&opts.dnd_on_gaming), "true");

            // Non-game in foreground -> not detected.
            let nongame_path = tmp_file("appstatus", APP_STATUS_NONGAME);
            read_app_status_at(nongame_path.to_str().unwrap(), &mut ctx.current_system_cache);
            let mut opts2 = GameConfig::default();
            let gs2 = get_gamestart(&mut opts2, &ctx.current_system_cache);
            assert_eq!(gs2, None, "round {round}");

            std::fs::remove_file(&status_path).ok();
            std::fs::write(&status_path, APP_STATUS_GAME).unwrap();
        }
    }
}
