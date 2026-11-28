// Copyright (C) 2026-2027 Zexshia
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! Shared JSON configuration for AZenith daemons and CLI utilities.

use serde::{Deserialize, Serialize};
use std::ffi::CString;
use std::fs::{self, File, OpenOptions};
use std::io::{self, Write};
use std::os::fd::AsRawFd;
use std::os::unix::fs::MetadataExt;
use std::path::Path;
use std::sync::atomic::{AtomicU32, Ordering};

pub const CONFIG_DIR: &str = "/data/adb/.config/AZenith";
pub const CONFIG_PATH: &str = "/data/adb/.config/AZenith/config.json";
pub const CONFIG_LOCK_PATH: &str = "/data/adb/.config/AZenith/.config.lock";
pub const APP_API_DIR: &str = "/data/data/zx.azenith/API";
pub const APP_CONFIG_MIRROR_PATH: &str = "/data/data/zx.azenith/API/config.json";

static TMP_SEQ: AtomicU32 = AtomicU32::new(0);

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(default)]
pub struct AZenithConfig {
    // Schema version
    pub version: u32,

    // System
    pub soc_type: Option<u8>,
    pub debug_mode: bool,
    pub disable_tweak: bool,

    // Daemon
    pub auto_mode: bool,
    pub drop_foreground: bool,
    pub profile_notifications: bool,
    pub show_toast: bool,
    pub force_sidepanel: bool,

    // CPU
    pub cpu_limit: bool,
    pub freq_offset: String,

    // CPU Governors
    pub default_cpu_gov: String,
    pub custom_default_cpu_gov: String,
    pub custom_powersave_cpu_gov: String,
    pub custom_performance_cpu_gov: String,

    // I/O Schedulers
    pub default_io: String,
    pub custom_default_io: String,
    pub custom_powersave_io: String,
    pub custom_performance_io: String,

    // Mali GPU Governors
    pub default_mali_gov: String,
    pub custom_default_mali_gov: String,
    pub custom_powersave_mali_gov: String,
    pub custom_performance_mali_gov: String,

    // Display & GPU
    pub renderer: String,
    pub scheme_config: String,
    pub sfl: bool,
    pub mali_sched: bool,

    // Features
    pub app_preload: bool,
    pub preload_budget: String,
    pub clear_bg: bool,
    pub app_priority: bool,
    pub dnd: bool,
    pub fstrim: bool,
    pub thermalcore: bool,

    // Preferences
    pub schedtunes: bool,
    pub walttunes: bool,
    pub justintime: bool,
    pub disable_trace: bool,
    pub kill_logd: bool,
    pub disable_thermal: bool,
    pub use_fpsgo: bool,
    pub fpsged: bool,

    // Standby Charge
    #[serde(alias = "bypass_chg")]
    pub standby_chg: bool,
    #[serde(alias = "bypass_path")]
    pub standby_path: String,
}

impl Default for AZenithConfig {
    fn default() -> Self {
        Self {
            // Schema version
            version: 1,

            // System
            soc_type: None,
            debug_mode: false,
            disable_tweak: false,

            // Daemon
            auto_mode: true,
            drop_foreground: false,
            profile_notifications: true,
            show_toast: false,
            force_sidepanel: false,

            // CPU
            cpu_limit: false,
            freq_offset: "Disabled".to_string(),

            // CPU Governors
            default_cpu_gov: String::new(),
            custom_default_cpu_gov: String::new(),
            custom_powersave_cpu_gov: String::new(),
            custom_performance_cpu_gov: String::new(),

            // I/O Schedulers
            default_io: String::new(),
            custom_default_io: String::new(),
            custom_powersave_io: String::new(),
            custom_performance_io: String::new(),

            // Mali GPU Governors
            default_mali_gov: String::new(),
            custom_default_mali_gov: String::new(),
            custom_powersave_mali_gov: String::new(),
            custom_performance_mali_gov: String::new(),

            // Display & GPU
            renderer: "default".to_string(),
            scheme_config: "1000 1000 1000 1000".to_string(),
            sfl: false,
            mali_sched: false,

            // Features
            app_preload: false,
            preload_budget: "500M".to_string(),
            clear_bg: false,
            app_priority: true,
            dnd: false,
            fstrim: false,
            thermalcore: false,

            // Preferences
            schedtunes: false,
            walttunes: false,
            justintime: false,
            disable_trace: false,
            kill_logd: false,
            disable_thermal: false,
            use_fpsgo: false,
            fpsged: false,

            // Standby Charge
            standby_chg: false,
            standby_path: "NEED_SETUP".to_string(),
        }
    }
}

/// Parse flexible boolean strings ("1", "true", "0", "false", etc.).
pub fn parse_bool_flexible(val: &str) -> Option<bool> {
    match val.trim().to_ascii_lowercase().as_str() {
        "1" | "true" | "yes" | "on" => Some(true),
        "0" | "false" | "no" | "off" => Some(false),
        _ => None,
    }
}

impl AZenithConfig {
    /// Get a field value by its JSON key name as a String.
    pub fn get_key(&self, key: &str) -> Result<String, String> {
        match key {
            "version" => Ok(self.version.to_string()),
            "soc_type" => Ok(self
                .soc_type
                .map(|v| v.to_string())
                .unwrap_or_else(|| "null".to_string())),
            "debug_mode" => Ok(self.debug_mode.to_string()),
            "disable_tweak" => Ok(self.disable_tweak.to_string()),
            "auto_mode" => Ok(self.auto_mode.to_string()),
            "drop_foreground" => Ok(self.drop_foreground.to_string()),
            "profile_notifications" => Ok(self.profile_notifications.to_string()),
            "show_toast" => Ok(self.show_toast.to_string()),
            "force_sidepanel" => Ok(self.force_sidepanel.to_string()),
            "cpu_limit" => Ok(self.cpu_limit.to_string()),
            "freq_offset" => Ok(self.freq_offset.clone()),
            "default_cpu_gov" => Ok(self.default_cpu_gov.clone()),
            "custom_default_cpu_gov" => Ok(self.custom_default_cpu_gov.clone()),
            "custom_powersave_cpu_gov" => Ok(self.custom_powersave_cpu_gov.clone()),
            "custom_performance_cpu_gov" => Ok(self.custom_performance_cpu_gov.clone()),
            "default_io" => Ok(self.default_io.clone()),
            "custom_default_io" => Ok(self.custom_default_io.clone()),
            "custom_powersave_io" => Ok(self.custom_powersave_io.clone()),
            "custom_performance_io" => Ok(self.custom_performance_io.clone()),
            "default_mali_gov" => Ok(self.default_mali_gov.clone()),
            "custom_default_mali_gov" => Ok(self.custom_default_mali_gov.clone()),
            "custom_powersave_mali_gov" => Ok(self.custom_powersave_mali_gov.clone()),
            "custom_performance_mali_gov" => Ok(self.custom_performance_mali_gov.clone()),
            "renderer" => Ok(self.renderer.clone()),
            "scheme_config" => Ok(self.scheme_config.clone()),
            "sfl" => Ok(self.sfl.to_string()),
            "mali_sched" => Ok(self.mali_sched.to_string()),
            "app_preload" => Ok(self.app_preload.to_string()),
            "preload_budget" => Ok(self.preload_budget.clone()),
            "clear_bg" => Ok(self.clear_bg.to_string()),
            "app_priority" => Ok(self.app_priority.to_string()),
            "dnd" => Ok(self.dnd.to_string()),
            "fstrim" => Ok(self.fstrim.to_string()),
            "thermalcore" => Ok(self.thermalcore.to_string()),
            "schedtunes" => Ok(self.schedtunes.to_string()),
            "walttunes" => Ok(self.walttunes.to_string()),
            "justintime" => Ok(self.justintime.to_string()),
            "disable_trace" => Ok(self.disable_trace.to_string()),
            "kill_logd" => Ok(self.kill_logd.to_string()),
            "disable_thermal" => Ok(self.disable_thermal.to_string()),
            "use_fpsgo" => Ok(self.use_fpsgo.to_string()),
            "fpsged" => Ok(self.fpsged.to_string()),
            "standby_chg" | "bypass_chg" => Ok(self.standby_chg.to_string()),
            "standby_path" | "bypass_path" => Ok(self.standby_path.clone()),
            _ => Err(format!("unknown config key: {key}")),
        }
    }

    /// Set a field value by its JSON key name from a string representation.
    pub fn set_key(&mut self, key: &str, val: &str) -> Result<(), String> {
        let parse_b = |k: &str, v: &str| -> Result<bool, String> {
            parse_bool_flexible(v).ok_or_else(|| format!("invalid bool for {k}: {v}"))
        };

        match key {
            "version" => {
                self.version = val
                    .trim()
                    .parse::<u32>()
                    .map_err(|_| format!("invalid u32 for version: {val}"))?;
            }
            "soc_type" => {
                let trimmed = val.trim();
                if trimmed.is_empty() || trimmed.eq_ignore_ascii_case("null") || trimmed.eq_ignore_ascii_case("none") {
                    self.soc_type = None;
                } else {
                    self.soc_type = Some(
                        trimmed
                            .parse::<u8>()
                            .map_err(|_| format!("invalid u8 for soc_type: {val}"))?,
                    );
                }
            }
            "debug_mode" => self.debug_mode = parse_b(key, val)?,
            "disable_tweak" => self.disable_tweak = parse_b(key, val)?,
            "auto_mode" => self.auto_mode = parse_b(key, val)?,
            "drop_foreground" => self.drop_foreground = parse_b(key, val)?,
            "profile_notifications" => self.profile_notifications = parse_b(key, val)?,
            "show_toast" => self.show_toast = parse_b(key, val)?,
            "force_sidepanel" => self.force_sidepanel = parse_b(key, val)?,
            "cpu_limit" => self.cpu_limit = parse_b(key, val)?,
            "freq_offset" => self.freq_offset = val.to_string(),
            "default_cpu_gov" => self.default_cpu_gov = val.to_string(),
            "custom_default_cpu_gov" => self.custom_default_cpu_gov = val.to_string(),
            "custom_powersave_cpu_gov" => self.custom_powersave_cpu_gov = val.to_string(),
            "custom_performance_cpu_gov" => self.custom_performance_cpu_gov = val.to_string(),
            "default_io" => self.default_io = val.to_string(),
            "custom_default_io" => self.custom_default_io = val.to_string(),
            "custom_powersave_io" => self.custom_powersave_io = val.to_string(),
            "custom_performance_io" => self.custom_performance_io = val.to_string(),
            "default_mali_gov" => self.default_mali_gov = val.to_string(),
            "custom_default_mali_gov" => self.custom_default_mali_gov = val.to_string(),
            "custom_powersave_mali_gov" => self.custom_powersave_mali_gov = val.to_string(),
            "custom_performance_mali_gov" => self.custom_performance_mali_gov = val.to_string(),
            "renderer" => self.renderer = val.to_string(),
            "scheme_config" => self.scheme_config = val.to_string(),
            "sfl" => self.sfl = parse_b(key, val)?,
            "mali_sched" => self.mali_sched = parse_b(key, val)?,
            "app_preload" => self.app_preload = parse_b(key, val)?,
            "preload_budget" => self.preload_budget = val.to_string(),
            "clear_bg" => self.clear_bg = parse_b(key, val)?,
            "app_priority" => self.app_priority = parse_b(key, val)?,
            "dnd" => self.dnd = parse_b(key, val)?,
            "fstrim" => self.fstrim = parse_b(key, val)?,
            "thermalcore" => self.thermalcore = parse_b(key, val)?,
            "schedtunes" => self.schedtunes = parse_b(key, val)?,
            "walttunes" => self.walttunes = parse_b(key, val)?,
            "justintime" => self.justintime = parse_b(key, val)?,
            "disable_trace" => self.disable_trace = parse_b(key, val)?,
            "kill_logd" => self.kill_logd = parse_b(key, val)?,
            "disable_thermal" => self.disable_thermal = parse_b(key, val)?,
            "use_fpsgo" => self.use_fpsgo = parse_b(key, val)?,
            "fpsged" => self.fpsged = parse_b(key, val)?,
            "standby_chg" | "bypass_chg" => self.standby_chg = parse_b(key, val)?,
            "standby_path" | "bypass_path" => self.standby_path = val.to_string(),
            _ => return Err(format!("unknown config key: {key}")),
        }
        Ok(())
    }

    /// Apply a legacy `persist.sys.azenith*` property into this config.
    /// Returns `true` if the property matched a known configuration key.
    pub fn apply_legacy_prop(&mut self, prop_name: &str, raw_val: &str) -> bool {
        let val = raw_val.trim();
        if val.is_empty() {
            return false;
        }
        match prop_name {
            "persist.sys.azenith.soctype" | "persist.sys.azenithdebug.soctype" => {
                if let Ok(v) = val.parse::<u8>() {
                    self.soc_type = Some(v);
                    return true;
                }
                false
            }
            "persist.sys.azenith.debugmode" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.debug_mode = b;
                    return true;
                }
                false
            }
            "persist.sys.azenith.disabletweak" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.disable_tweak = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.AIenabled" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.auto_mode = b;
                    return true;
                }
                false
            }
            "persist.sys.azenith.dropforeground" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.drop_foreground = b;
                    return true;
                }
                false
            }
            "persist.sys.azenith.profilenotifications" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.profile_notifications = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.showtoast" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.show_toast = b;
                    return true;
                }
                false
            }
            "persist.sys.azenith.force_sidepanel" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.force_sidepanel = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.cpulimit" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.cpu_limit = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.freqoffset" => {
                self.freq_offset = val.to_string();
                true
            }
            "persist.sys.azenith.default_cpu_gov" => {
                self.default_cpu_gov = val.to_string();
                true
            }
            "persist.sys.azenith.custom_default_cpu_gov" => {
                self.custom_default_cpu_gov = val.to_string();
                true
            }
            "persist.sys.azenith.custom_powersave_cpu_gov" => {
                self.custom_powersave_cpu_gov = val.to_string();
                true
            }
            "persist.sys.azenith.custom_performance_cpu_gov" => {
                self.custom_performance_cpu_gov = val.to_string();
                true
            }
            "persist.sys.azenith.default_balanced_IO" => {
                self.default_io = val.to_string();
                true
            }
            "persist.sys.azenith.custom_default_balanced_IO" => {
                self.custom_default_io = val.to_string();
                true
            }
            "persist.sys.azenith.custom_powersave_IO" => {
                self.custom_powersave_io = val.to_string();
                true
            }
            "persist.sys.azenith.custom_performance_IO" => {
                self.custom_performance_io = val.to_string();
                true
            }
            "persist.sys.azenith.default_maligpu_gov" => {
                self.default_mali_gov = val.to_string();
                true
            }
            "persist.sys.azenith.custom_default_maligpu_gov" => {
                self.custom_default_mali_gov = val.to_string();
                true
            }
            "persist.sys.azenith.custom_powersave_maligpu_gov" => {
                self.custom_powersave_mali_gov = val.to_string();
                true
            }
            "persist.sys.azenith.custom_performance_maligpu_gov" => {
                self.custom_performance_mali_gov = val.to_string();
                true
            }
            "persist.sys.azenithconf.renderer" => {
                self.renderer = val.to_string();
                true
            }
            "persist.sys.azenithconf.schemeconfig" => {
                self.scheme_config = val.to_string();
                true
            }
            "persist.sys.azenithconf.SFL" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.sfl = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.malisched" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.mali_sched = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.APreload" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.app_preload = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.preloadbudget" => {
                self.preload_budget = val.to_string();
                true
            }
            "persist.sys.azenithconf.clearbg" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.clear_bg = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.iosched" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.app_priority = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.dnd" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.dnd = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.fstrim" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.fstrim = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.thermalcore" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.thermalcore = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.schedtunes" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.schedtunes = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.walttunes" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.walttunes = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.justintime" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.justintime = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.disabletrace" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.disable_trace = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.logd" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.kill_logd = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.DThermal" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.disable_thermal = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.usefpsgo" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.use_fpsgo = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.fpsged" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.fpsged = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.bypasschg" => {
                if let Some(b) = parse_bool_flexible(val) {
                    self.standby_chg = b;
                    return true;
                }
                false
            }
            "persist.sys.azenithconf.bypasspath" => {
                self.standby_path = val.to_string();
                true
            }
            _ => false,
        }
    }
}

struct FlockGuard {
    file: File,
}

impl FlockGuard {
    fn acquire(lock_path: &Path) -> io::Result<Self> {
        if let Some(parent) = lock_path.parent() {
            let _ = fs::create_dir_all(parent);
        }
        let file = OpenOptions::new()
            .read(true)
            .write(true)
            .create(true)
            .truncate(false)
            .open(lock_path)?;
        let rc = unsafe { libc::flock(file.as_raw_fd(), libc::LOCK_EX) };
        if rc != 0 {
            return Err(io::Error::last_os_error());
        }
        Ok(Self { file })
    }
}

impl Drop for FlockGuard {
    fn drop(&mut self) {
        unsafe {
            libc::flock(self.file.as_raw_fd(), libc::LOCK_UN);
        }
    }
}

fn chmod_c(path: &Path, mode: u32) {
    if let Some(s) = path.to_str() {
        if let Ok(c) = CString::new(s) {
            unsafe {
                libc::chmod(c.as_ptr(), mode as libc::mode_t);
            }
        }
    }
}

/// Write `bytes` atomically to `dest` via a temp file in the same directory,
/// `fsync`, chmod `0644`, and `rename`.
pub fn write_atomic_in_dir(dest: &Path, bytes: &[u8], mode: u32) -> io::Result<()> {
    let parent = dest
        .parent()
        .ok_or_else(|| io::Error::new(io::ErrorKind::InvalidInput, "dest has no parent"))?;
    fs::create_dir_all(parent)?;

    let file_name = dest
        .file_name()
        .and_then(|n| n.to_str())
        .unwrap_or("config.json");
    let seq = TMP_SEQ.fetch_add(1, Ordering::Relaxed);
    let pid = std::process::id();
    let tmp_path = parent.join(format!(".{file_name}.{pid}.{seq}.tmp"));

    let mut tmp_file = OpenOptions::new()
        .write(true)
        .create(true)
        .truncate(true)
        .open(&tmp_path)?;

    if let Err(e) = tmp_file.write_all(bytes).and_then(|_| tmp_file.sync_all()) {
        let _ = fs::remove_file(&tmp_path);
        return Err(e);
    }
    drop(tmp_file);

    chmod_c(&tmp_path, mode);

    if let Err(e) = fs::rename(&tmp_path, dest) {
        let _ = fs::remove_file(&tmp_path);
        return Err(e);
    }

    chmod_c(dest, mode);
    Ok(())
}

/// Best-effort mirror to `/data/data/zx.azenith/API/config.json`:
/// - Atomic write (tmp in same folder + fsync + rename)
/// - `chmod 0644` (and `0777` on `/data/data/zx.azenith/API` like `API/current_profile`)
/// - `chown` and SELinux `security.selinux` context inheritance from `/data/data/zx.azenith`
pub fn mirror_to_app_api(mirror_path: &Path, bytes: &[u8]) {
    let Some(api_dir) = mirror_path.parent() else {
        return;
    };
    let Some(app_data_dir) = api_dir.parent() else {
        return;
    };

    // Only mirror if the manager app data directory exists (or if we can create API dir inside it)
    let app_meta = fs::metadata(app_data_dir).ok();
    let _ = fs::create_dir_all(api_dir);
    chmod_c(api_dir, 0o777);

    if write_atomic_in_dir(mirror_path, bytes, 0o644).is_err() {
        return;
    }

    // Inherit uid/gid and SELinux label from `/data/data/zx.azenith` when available
    // so the unprivileged manager process can read `API/config.json` under strict SELinux.
    if let Some(meta) = app_meta {
        let uid = meta.uid();
        let gid = meta.gid();
        if let Some(s) = mirror_path.to_str() {
            if let Ok(c) = CString::new(s) {
                unsafe {
                    libc::chown(c.as_ptr(), uid, gid);
                }
            }
        }
        copy_selinux_context(app_data_dir, api_dir);
        copy_selinux_context(app_data_dir, mirror_path);
    }
    chmod_c(mirror_path, 0o644);
}

fn copy_selinux_context(src: &Path, dst: &Path) {
    let (Some(src_s), Some(dst_s)) = (src.to_str(), dst.to_str()) else {
        return;
    };
    let (Ok(c_src), Ok(c_dst), Ok(c_attr)) = (
        CString::new(src_s),
        CString::new(dst_s),
        CString::new("security.selinux"),
    ) else {
        return;
    };

    let mut buf = [0u8; 256];
    let len = unsafe {
        libc::getxattr(
            c_src.as_ptr(),
            c_attr.as_ptr(),
            buf.as_mut_ptr() as *mut libc::c_void,
            buf.len(),
        )
    };
    if len > 0 {
        unsafe {
            libc::setxattr(
                c_dst.as_ptr(),
                c_attr.as_ptr(),
                buf.as_ptr() as *const libc::c_void,
                len as usize,
                0,
            );
        }
    }
}

/// Load `AZenithConfig` from `CONFIG_PATH`. Returns `AZenithConfig::default()` if the file
/// does not exist or cannot be parsed.
pub fn load() -> AZenithConfig {
    load_from(Path::new(CONFIG_PATH))
}

/// Load `AZenithConfig` from a specific path, falling back to `Default` on error.
pub fn load_from(path: &Path) -> AZenithConfig {
    try_load_from(path).unwrap_or_default()
}

/// Attempt to load and parse `AZenithConfig` from a specific path.
pub fn try_load_from(path: &Path) -> io::Result<AZenithConfig> {
    let content = fs::read_to_string(path)?;
    serde_json::from_str::<AZenithConfig>(&content)
        .map_err(|e| io::Error::new(io::ErrorKind::InvalidData, e))
}

/// Atomically update `CONFIG_PATH` under `.config.lock` (flock), and mirror to
/// `/data/data/zx.azenith/API/config.json`.
pub fn update<F>(mutator: F) -> io::Result<AZenithConfig>
where
    F: FnOnce(&mut AZenithConfig),
{
    update_at(
        Path::new(CONFIG_PATH),
        Path::new(CONFIG_LOCK_PATH),
        Some(Path::new(APP_CONFIG_MIRROR_PATH)),
        mutator,
    )
}

/// Atomically save a complete `AZenithConfig` to `CONFIG_PATH` under `.config.lock`
/// and mirror to `/data/data/zx.azenith/API/config.json`.
pub fn save(cfg: &AZenithConfig) -> io::Result<()> {
    let cloned = cfg.clone();
    update(|c| *c = cloned).map(|_| ())
}

/// Sync the current `CONFIG_PATH` to `/data/data/zx.azenith/API/config.json` (used by `--setup-access`).
pub fn sync_mirror() {
    if let Ok(bytes) = fs::read(CONFIG_PATH) {
        mirror_to_app_api(Path::new(APP_CONFIG_MIRROR_PATH), &bytes);
    }
}

/// Testable implementation of `update` with explicit paths.
pub fn update_at<F>(
    config_path: &Path,
    lock_path: &Path,
    mirror_path: Option<&Path>,
    mutator: F,
) -> io::Result<AZenithConfig>
where
    F: FnOnce(&mut AZenithConfig),
{
    let _guard = FlockGuard::acquire(lock_path)?;

    let mut cfg = try_load_from(config_path).unwrap_or_default();
    mutator(&mut cfg);

    let mut json = serde_json::to_string_pretty(&cfg)
        .map_err(|e| io::Error::new(io::ErrorKind::InvalidData, e))?;
    json.push('\n');

    write_atomic_in_dir(config_path, json.as_bytes(), 0o644)?;

    if let Some(mirror) = mirror_path {
        mirror_to_app_api(mirror, json.as_bytes());
    }

    Ok(cfg)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn field_order_and_defaults_serialize_cleanly() {
        let cfg = AZenithConfig::default();
        let json = serde_json::to_string_pretty(&cfg).unwrap();

        // Check that key order matches struct definition order
        let idx_version = json.find("\"version\"").unwrap();
        let idx_soc = json.find("\"soc_type\"").unwrap();
        let idx_auto = json.find("\"auto_mode\"").unwrap();
        let idx_cpu_limit = json.find("\"cpu_limit\"").unwrap();
        let idx_def_cpu = json.find("\"default_cpu_gov\"").unwrap();
        let idx_def_io = json.find("\"default_io\"").unwrap();
        let idx_def_mali = json.find("\"default_mali_gov\"").unwrap();
        let idx_renderer = json.find("\"renderer\"").unwrap();
        let idx_preload = json.find("\"app_preload\"").unwrap();
        let idx_sched = json.find("\"schedtunes\"").unwrap();
        let idx_standby = json.find("\"standby_chg\"").unwrap();
        let idx_path = json.find("\"standby_path\"").unwrap();

        assert!(idx_version < idx_soc);
        assert!(idx_soc < idx_auto);
        assert!(idx_auto < idx_cpu_limit);
        assert!(idx_cpu_limit < idx_def_cpu);
        assert!(idx_def_cpu < idx_def_io);
        assert!(idx_def_io < idx_def_mali);
        assert!(idx_def_mali < idx_renderer);
        assert!(idx_renderer < idx_preload);
        assert!(idx_preload < idx_sched);
        assert!(idx_sched < idx_standby);
        assert!(idx_standby < idx_path);
    }

    #[test]
    fn atomic_update_and_mirror_work() {
        let dir = std::env::temp_dir().join(format!("azenith_cfg_test_{}", std::process::id()));
        let _ = fs::remove_dir_all(&dir);
        fs::create_dir_all(&dir).unwrap();

        let cfg_path = dir.join("config.json");
        let lock_path = dir.join(".config.lock");
        let mirror_dir = dir.join("app_data").join("API");
        let mirror_path = mirror_dir.join("config.json");

        let updated = update_at(&cfg_path, &lock_path, Some(&mirror_path), |c| {
            c.soc_type = Some(2);
            c.freq_offset = "80".to_string();
            c.standby_chg = true;
        })
        .unwrap();

        assert_eq!(updated.soc_type, Some(2));
        assert_eq!(updated.freq_offset, "80");
        assert!(updated.standby_chg);

        let loaded = try_load_from(&cfg_path).unwrap();
        assert_eq!(loaded, updated);

        let mirrored = try_load_from(&mirror_path).unwrap();
        assert_eq!(mirrored, updated);

        let _ = fs::remove_dir_all(&dir);
    }
}
