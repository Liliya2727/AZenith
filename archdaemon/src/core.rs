//! Core types shared across the daemon.

use std::os::raw::c_int;

pub const MAX_DATA_LENGTH: usize = 2048;
pub const MAX_COMMAND_LENGTH: usize = 600;
pub const MAX_OUTPUT_LENGTH: usize = 256;
pub const MAX_PATH_LENGTH: usize = 256;
pub const MAX_LINE: usize = 512;
pub const MAX_PACKAGE: usize = 128;
pub const MAX_GAME_PIDS: usize = 8;

pub const NOTIFY_TITLE: &str = "AZenith";
pub const LOG_TAG: &str = "AZenith";
pub const LOG_TAG_PROFILE: &str = "AZenith_Profiles";

pub const LOCK_FILE: &str = "/data/adb/.config/AZenith/API/.lock";
pub const LOG_FILE: &str = "/data/adb/.config/AZenith/debug/AZenith.log";
pub const LOG_VFILE: &str = "/data/adb/.config/AZenith/debug/AZenithVerbose.log";
pub const LOG_FILE_PRELOAD: &str = "/data/adb/.config/AZenith/preload/AZenithPR.log";

pub const LOG_MAX_BYTES: usize = 512 * 1024;
pub const LOG_KEEP_FILES: i32 = 2;

pub const PROFILE_MODE: &str = "/data/adb/.config/AZenith/API/current_profile";
pub const PROFILE_MODE_APP: &str = "/data/data/zx.azenith/API/current_profile";
pub const GAME_INFO: &str = "/data/adb/.config/AZenith/API/gameinfo";
pub const GAME_INFO_APP: &str = "/data/data/zx.azenith/API/gameinfo";
pub const GAMELIST: &str = "/data/adb/.config/AZenith/gamelist/azenithApplist.json";
pub const DAEMON_MODES: &str = "/data/adb/.config/AZenith/API/current_modes";
pub const MODULE_PROP: &str = "/data/adb/modules/AZenith/module.prop";
pub const MODULE_UPDATE: &str = "/data/adb/modules/AZenith/update";
pub const MODULE_REMOVE: &str = "/data/adb/modules/AZenith/remove";
pub const BYPASSCHG_CONFIG: &str = "/data/adb/.config/AZenith/bypasschgconfig";
pub const APP_MONITOR_FILE: &str = "/data/adb/.config/AZenith/app_status";
pub const DAEMON_STATE_FILE: &str = "/data/adb/.config/AZenith/daemon_state";
pub const JAVA_LOCK_PATH: &str = "/data/adb/.config/AZenith/java.lock";
pub const RR_MODES_PATH: &str = "/data/adb/.config/AZenith/rr_modes.json";

pub const AZENITH_PROPERTIES: &str = "persist.sys.azenith";

pub const MY_PATH: &str =
    "PATH=/system/bin:/system/xbin:/data/adb/ap/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:/sbin:/sbin/su:/su/bin:/su/xbin:/data/data/com.termux/files/usr/bin";

// ---------------------------------------------------------------------------
// Helper macros (operate on &str)
// ---------------------------------------------------------------------------
#[macro_export]
macro_rules! IS_TRUE {
    ($v:expr) => {
        !$v.is_empty() && $v == "true"
    };
}

#[macro_export]
macro_rules! IS_FALSE {
    ($v:expr) => {
        !$v.is_empty() && $v == "false"
    };
}

#[macro_export]
macro_rules! IS_DEFAULT {
    ($v:expr) => {
        $v.is_empty() || $v == "default"
    };
}

// ---------------------------------------------------------------------------
// LogLevel
// ---------------------------------------------------------------------------
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(u8)]
pub enum LogLevel {
    Debug,
    Info,
    Warn,
    Error,
    Fatal,
}

impl LogLevel {
    pub fn as_c_int(self) -> c_int {
        match self {
            LogLevel::Debug => crate::android_ffi::ANDROID_LOG_DEBUG,
            LogLevel::Info => crate::android_ffi::ANDROID_LOG_INFO,
            LogLevel::Warn => crate::android_ffi::ANDROID_LOG_WARN,
            LogLevel::Error => crate::android_ffi::ANDROID_LOG_ERROR,
            LogLevel::Fatal => crate::android_ffi::ANDROID_LOG_FATAL,
        }
    }
}

// ---------------------------------------------------------------------------
// ProfileMode
// ---------------------------------------------------------------------------
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
#[repr(i32)]
pub enum ProfileMode {
    #[default]
    PerfCommon = 0,
    PerformanceProfile = 1,
    BalancedProfile = 2,
    EcoMode = 3,
}

impl ProfileMode {
    pub fn from_i32(v: i32) -> Self {
        match v {
            1 => ProfileMode::PerformanceProfile,
            2 => ProfileMode::BalancedProfile,
            3 => ProfileMode::EcoMode,
            _ => ProfileMode::PerfCommon,
        }
    }

    pub fn name(self) -> &'static str {
        match self {
            ProfileMode::PerformanceProfile => "performance",
            ProfileMode::BalancedProfile => "balanced",
            ProfileMode::EcoMode => "eco",
            ProfileMode::PerfCommon => "common",
        }
    }
}

// ---------------------------------------------------------------------------
// GameConfig
// ---------------------------------------------------------------------------
#[derive(Debug, Clone)]
pub struct GameConfig {
    pub package: [u8; 128],
    pub perf_lite_mode: [u8; 16],
    pub dnd_on_gaming: [u8; 16],
    pub app_priority: [u8; 16],
    pub game_preload: [u8; 16],
    pub refresh_rate: [u8; 16],
    pub renderer: [u8; 64],
    pub resolution_downscale: [u8; 8],
    pub resolution_fps: [u8; 8],
    pub bypass_charging: [u8; 16],
}

impl Default for GameConfig {
    fn default() -> Self {
        Self {
            package: [0; 128],
            perf_lite_mode: [0; 16],
            dnd_on_gaming: [0; 16],
            app_priority: [0; 16],
            game_preload: [0; 16],
            refresh_rate: [0; 16],
            renderer: [0; 64],
            resolution_downscale: [0; 8],
            resolution_fps: [0; 8],
            bypass_charging: [0; 16],
        }
    }
}

impl GameConfig {
    pub fn field_as_str<'a>(&self, field: &'a [u8]) -> &'a str {
        let len = field.iter().position(|&b| b == 0).unwrap_or(field.len());
        unsafe { std::str::from_utf8_unchecked(&field[..len]) }
    }

    pub fn set_field(field: &mut [u8], value: &str) {
        let bytes = value.as_bytes();
        let n = bytes.len().min(field.len().saturating_sub(1));
        field[..n].copy_from_slice(&bytes[..n]);
        field[n] = 0;
        // clear the rest
        for b in &mut field[n + 1..] {
            *b = 0;
        }
    }
}

// ---------------------------------------------------------------------------
// SystemStateCache
// ---------------------------------------------------------------------------
#[derive(Debug, Clone)]
pub struct SystemStateCache {
    pub focused_app: [u8; 128],
    pub app_name: [u8; 256],
    pub focused_pid: i32,
    pub zen_mode: i32,
    pub screen_awake: i32,
    pub battery_saver: i32,
    pub battery_level: i32,
    pub is_charging: i32,
}

impl Default for SystemStateCache {
    fn default() -> Self {
        Self {
            focused_app: [0; 128],
            app_name: [0; 256],
            focused_pid: 0,
            zen_mode: 0,
            screen_awake: 0,
            battery_saver: 0,
            battery_level: 0,
            is_charging: 0,
        }
    }
}

impl SystemStateCache {
    pub fn focused_app_str(&self) -> &str {
        let len = self.focused_app.iter().position(|&b| b == 0).unwrap_or(self.focused_app.len());
        unsafe { std::str::from_utf8_unchecked(&self.focused_app[..len]) }
    }

    pub fn app_name_str(&self) -> &str {
        let len = self.app_name.iter().position(|&b| b == 0).unwrap_or(self.app_name.len());
        unsafe { std::str::from_utf8_unchecked(&self.app_name[..len]) }
    }
}

// ---------------------------------------------------------------------------
// BypassNode
// ---------------------------------------------------------------------------
#[derive(Debug, Clone)]
pub struct BypassNode {
    pub name: &'static str,
    pub path: &'static str,
    pub on_val: &'static str,
    pub off_val: &'static str,
}

// ---------------------------------------------------------------------------
// PreloadArgs
// ---------------------------------------------------------------------------
#[derive(Debug)]
pub struct PreloadArgs {
    pub package: [u8; 256],
}

// ---------------------------------------------------------------------------
// Sub-structs for DaemonContext (modularity)
// ---------------------------------------------------------------------------

/// Display-related state (screen, refresh rate, zen mode)
#[derive(Debug, Clone)]
pub struct DisplayState {
    pub prev_screen_state: i32,
    pub saved_refresh_rate: i32,
    pub saved_zen_mode: i32,
}

impl Default for DisplayState {
    fn default() -> Self {
        Self {
            prev_screen_state: -1,
            saved_refresh_rate: -1,
            saved_zen_mode: -1,
        }
    }
}

/// Profile-related state (current mode, saved renderer)
#[derive(Debug, Clone)]
pub struct ProfileState {
    pub cur_mode: ProfileMode,
    pub saved_renderer: [u8; crate::android_ffi::PROP_VALUE_MAX],
    pub saved_sys_renderer: [u8; crate::android_ffi::PROP_VALUE_MAX],
}

impl Default for ProfileState {
    fn default() -> Self {
        Self {
            cur_mode: ProfileMode::PerfCommon,
            saved_renderer: [0; crate::android_ffi::PROP_VALUE_MAX],
            saved_sys_renderer: [0; crate::android_ffi::PROP_VALUE_MAX],
        }
    }
}

/// Bypass charging state
#[derive(Debug, Clone)]
pub struct BypassState {
    pub bypass_applied: bool,
    pub config_bypasspath: [u8; crate::android_ffi::PROP_VALUE_MAX],
    pub config_bypasschg: i32,
    pub config_bypasschgthreshold: i32,
}

impl Default for BypassState {
    fn default() -> Self {
        Self {
            bypass_applied: false,
            config_bypasspath: [0; crate::android_ffi::PROP_VALUE_MAX],
            config_bypasschg: 0,
            config_bypasschgthreshold: 0,
        }
    }
}

/// Game-related state (detected game, PIDs)
#[derive(Debug, Clone, Default)]
pub struct GameState {
    pub gamestart: Option<String>,
    pub active_app_name: Option<String>,
    pub game_pids: [i32; MAX_GAME_PIDS],
    pub game_pid_count: i32,
}

/// Power-related state (grace period, foreground away)
#[derive(Debug, Clone, Default)]
pub struct PowerState {
    pub grace_period_active: bool,
    pub screen_off_timer: i64,
    pub fg_away_active: bool,
    pub fg_away_timer: i64,
}

/// Renderer/resolution state
#[derive(Debug, Clone, Default)]
pub struct RenderState {
    pub has_applied_renderer: bool,
    pub resolution_applied: bool,
    pub used_legacy_fallback: bool,
    pub is_restarting_renderer: bool,
    /// True once the manual-mode sidepanel has been broadcast for the
    /// active game; reset whenever the tracked game changes or is dropped.
    pub manual_sidepanel_shown: bool,
}

// ---------------------------------------------------------------------------
// DaemonContext
// ---------------------------------------------------------------------------
#[derive(Debug, Clone)]
pub struct DaemonContext {
    pub is_initialize_complete: bool,
    pub dnd_enabled: bool,
    pub need_profile_checkup: bool,
    pub pid_retries: i32,
    pub java_lock_path: String,
    pub config_freqoffset: [u8; crate::android_ffi::PROP_VALUE_MAX],
    pub last_freqoffset: [u8; crate::android_ffi::PROP_VALUE_MAX],
    pub prev_ai_state: [u8; 16],
    pub opts: GameConfig,
    pub current_system_cache: SystemStateCache,

    // Composed sub-states
    pub display: DisplayState,
    pub profile: ProfileState,
    pub bypass: BypassState,
    pub game: GameState,
    pub power: PowerState,
    pub render: RenderState,
}

impl DaemonContext {
    pub fn new() -> Self {
        let mut ctx = Self {
            is_initialize_complete: false,
            dnd_enabled: false,
            need_profile_checkup: false,
            pid_retries: 0,
            java_lock_path: JAVA_LOCK_PATH.to_string(),
            config_freqoffset: [0; crate::android_ffi::PROP_VALUE_MAX],
            last_freqoffset: [0; crate::android_ffi::PROP_VALUE_MAX],
            prev_ai_state: [0; 16],
            opts: GameConfig::default(),
            current_system_cache: SystemStateCache::default(),
            display: DisplayState::default(),
            profile: ProfileState::default(),
            bypass: BypassState::default(),
            game: GameState::default(),
            power: PowerState::default(),
            render: RenderState::default(),
        };
        // last_freqoffset = "Initial"
        let init = b"Initial";
        ctx.last_freqoffset[..init.len()].copy_from_slice(init);
        ctx.last_freqoffset[init.len()] = 0;
        // prev_ai_state = "0"
        ctx.prev_ai_state[0] = b'0';
        ctx.prev_ai_state[1] = 0;
        ctx
    }

    pub fn config_freqoffset_str(&self) -> &str {
        let len = self.config_freqoffset.iter().position(|&b| b == 0).unwrap_or(self.config_freqoffset.len());
        unsafe { std::str::from_utf8_unchecked(&self.config_freqoffset[..len]) }
    }

    pub fn config_bypasspath_str(&self) -> &str {
        let len = self.bypass.config_bypasspath.iter().position(|&b| b == 0).unwrap_or(self.bypass.config_bypasspath.len());
        unsafe { std::str::from_utf8_unchecked(&self.bypass.config_bypasspath[..len]) }
    }

    pub fn saved_renderer_str(&self) -> &str {
        let len = self.profile.saved_renderer.iter().position(|&b| b == 0).unwrap_or(self.profile.saved_renderer.len());
        unsafe { std::str::from_utf8_unchecked(&self.profile.saved_renderer[..len]) }
    }

    pub fn saved_sys_renderer_str(&self) -> &str {
        let len = self.profile.saved_sys_renderer.iter().position(|&b| b == 0).unwrap_or(self.profile.saved_sys_renderer.len());
        unsafe { std::str::from_utf8_unchecked(&self.profile.saved_sys_renderer[..len]) }
    }

    pub fn last_freqoffset_str(&self) -> &str {
        let len = self.last_freqoffset.iter().position(|&b| b == 0).unwrap_or(self.last_freqoffset.len());
        unsafe { std::str::from_utf8_unchecked(&self.last_freqoffset[..len]) }
    }

    pub fn prev_ai_state_str(&self) -> &str {
        let len = self.prev_ai_state.iter().position(|&b| b == 0).unwrap_or(self.prev_ai_state.len());
        unsafe { std::str::from_utf8_unchecked(&self.prev_ai_state[..len]) }
    }

    pub fn opts_perf_lite_mode(&self) -> &str {
        self.opts.field_as_str(&self.opts.perf_lite_mode)
    }
    pub fn opts_dnd_on_gaming(&self) -> &str {
        self.opts.field_as_str(&self.opts.dnd_on_gaming)
    }
    pub fn opts_app_priority(&self) -> &str {
        self.opts.field_as_str(&self.opts.app_priority)
    }
    pub fn opts_game_preload(&self) -> &str {
        self.opts.field_as_str(&self.opts.game_preload)
    }
    pub fn opts_refresh_rate(&self) -> &str {
        self.opts.field_as_str(&self.opts.refresh_rate)
    }
    pub fn opts_renderer(&self) -> &str {
        self.opts.field_as_str(&self.opts.renderer)
    }
    pub fn opts_resolution_downscale(&self) -> &str {
        self.opts.field_as_str(&self.opts.resolution_downscale)
    }
    pub fn opts_resolution_fps(&self) -> &str {
        self.opts.field_as_str(&self.opts.resolution_fps)
    }
    pub fn opts_bypass_charging(&self) -> &str {
        self.opts.field_as_str(&self.opts.bypass_charging)
    }

    pub fn set_opts_field(&mut self, field: &mut [u8], value: &str) {
        GameConfig::set_field(field, value);
    }
}

// ---------------------------------------------------------------------------
// Globals (mirroring context.c)
// ---------------------------------------------------------------------------
use std::sync::OnceLock;

pub static G_GAME_CACHE: OnceLock<std::sync::RwLock<Vec<GameConfig>>> = OnceLock::new();
pub static G_GAME_CACHE_COUNT: OnceLock<std::sync::atomic::AtomicI32> = OnceLock::new();
pub static CACHE_MUTEX: OnceLock<std::sync::Mutex<()>> = OnceLock::new();
pub static JAVA_LOCK_PIPE_WRITE: OnceLock<std::sync::atomic::AtomicI32> = OnceLock::new();
pub static JAVA_LOCK_PIPE_READ: OnceLock<std::sync::atomic::AtomicI32> = OnceLock::new();
pub static JAVA_DAEMON_DIED: OnceLock<std::sync::atomic::AtomicBool> = OnceLock::new();
pub static CUSTOM_LOG_TAG: OnceLock<std::sync::Mutex<Option<String>>> = OnceLock::new();

pub fn init_globals() {
    let _ = G_GAME_CACHE.set(std::sync::RwLock::new(Vec::new()));
    let _ = G_GAME_CACHE_COUNT.set(std::sync::atomic::AtomicI32::new(0));
    let _ = CACHE_MUTEX.set(std::sync::Mutex::new(()));
    let _ = JAVA_LOCK_PIPE_WRITE.set(std::sync::atomic::AtomicI32::new(-1));
    let _ = JAVA_LOCK_PIPE_READ.set(std::sync::atomic::AtomicI32::new(-1));
    let _ = JAVA_DAEMON_DIED.set(std::sync::atomic::AtomicBool::new(false));
    let _ = CUSTOM_LOG_TAG.set(std::sync::Mutex::new(None));
}

pub fn game_cache() -> Option<&'static std::sync::RwLock<Vec<GameConfig>>> {
    G_GAME_CACHE.get()
}

pub fn game_cache_count() -> i32 {
    G_GAME_CACHE_COUNT
        .get()
        .map(|c| c.load(std::sync::atomic::Ordering::Relaxed))
        .unwrap_or(0)
}

pub fn set_game_cache_count(v: i32) {
    if let Some(c) = G_GAME_CACHE_COUNT.get() {
        c.store(v, std::sync::atomic::Ordering::Relaxed);
    }
}

pub fn java_daemon_died() -> bool {
    JAVA_DAEMON_DIED
        .get()
        .map(|c| c.load(std::sync::atomic::Ordering::Relaxed))
        .unwrap_or(false)
}

pub fn set_java_daemon_died(v: bool) {
    if let Some(c) = JAVA_DAEMON_DIED.get() {
        c.store(v, std::sync::atomic::Ordering::Relaxed);
    }
}
