//! Standard execution taxonomy shared by every profile.
//!
//! `performance`, `balanced` and `eco` must follow the exact same step order,
//! so the only difference between profiles is the *values*, not the *order*.
//! Only `initialize` sits outside this taxonomy: it is a separate phase
//! (runs once at boot).
//!
//! The required order:
//!
//! 1. Guard `persist.sys.azenith.disabletweak`
//! 2. Resolve CPU governor, I/O scheduler and Mali GPU from each profile's props
//! 3. [`apply_custom_governor_io`]
//! 4. [`apply_cpu_freq`]
//! 5. Log frequency mode
//! 6. [`apply_common_tweaks`] — VM → workqueue → EAS → stune → battery saver →
//!    split lock → sched features → block I/O
//! 7. Clear background apps (optional, performance only)
//! 8. Chipset-specific tweaks
//! 9. Log "… profile applied"

use crate::utils::*;
use std::fs;
use std::path::Path;

/// CPU frequency target for the profile.
#[derive(Clone, Copy)]
pub enum CpuFreqMode {
    /// Every cluster pinned to its maximum frequency (performance).
    MaxAvailable,
    /// Follows the `freqoffset` limiter plus the per-profile min freq rule (balanced/eco).
    Normal,
}

/// Write CPU frequencies through the PPM path (MediaTek) or plain sysfs.
pub fn apply_cpu_freq(mode: CpuFreqMode) {
    let has_ppm = Path::new("/proc/ppm").exists();

    match (mode, has_ppm) {
        (CpuFreqMode::MaxAvailable, true) => setgamefreqppm(),
        (CpuFreqMode::MaxAvailable, false) => setgamefreq(),
        (CpuFreqMode::Normal, true) => setfreqppm(),
        (CpuFreqMode::Normal, false) => setfreq(),
    }
}

/// Block I/O queue settings (only performance & balanced tune this).
#[derive(Clone, Copy)]
pub struct BlockIoTweaks {
    pub read_ahead_kb: &'static str,
    pub nr_requests: &'static str,
}

/// Shared tweak values across profiles. The fields are documented in the same
/// order they are applied, so adding a profile = filling in this struct.
pub struct CommonTweaks {
    /// `/proc/sys/vm/vfs_cache_pressure`
    pub vfs_cache_pressure: &'static str,
    /// `/proc/sys/vm/drop_caches` — only performance touches this node.
    pub drop_caches: Option<&'static str>,
    /// `/sys/module/workqueue/parameters/power_efficient`
    pub power_efficient: &'static str,
    /// `/sys/devices/system/cpu/eas/enable`
    pub eas_enabled: &'static str,
    /// `/dev/stune/*/schedtune.boost`
    pub stune_boost: &'static str,
    /// `/dev/stune/*/schedtune.sched_boost_enabled`
    pub stune_boost_enabled: &'static str,
    /// `/sys/module/battery_saver/parameters/enabled`, numeric form (`0`/`1`)
    pub battery_saver: &'static str,
    /// The same value in boolean form (`N`/`Y`) for non-numeric nodes.
    pub battery_saver_bool: &'static str,
    /// `/proc/sys/kernel/split_lock_mitigate`
    pub split_lock_mitigate: &'static str,
    /// `/sys/kernel/debug/sched_features`, written in order.
    pub sched_features: &'static [&'static str],
    /// Block I/O queue (read ahead + nr_requests), `None` when not tuned.
    pub block_io: Option<BlockIoTweaks>,
}

/// Apply the shared tweaks in taxonomy order (step 6).
pub fn apply_common_tweaks(tweaks: &CommonTweaks) {
    // VM
    write_lock(tweaks.vfs_cache_pressure, "/proc/sys/vm/vfs_cache_pressure");
    if let Some(drop_caches) = tweaks.drop_caches {
        write_lock(drop_caches, "/proc/sys/vm/drop_caches");
    }

    // Workqueue & EAS
    write_lock(tweaks.power_efficient, "/sys/module/workqueue/parameters/power_efficient");
    write_lock(tweaks.eas_enabled, "/sys/devices/system/cpu/eas/enable");

    // schedtune boost per group
    if let Ok(paths) = glob::glob("/dev/stune/*") {
        for path in paths.flatten() {
            if path.is_dir() {
                let p_str = path.to_str().unwrap();
                write_lock(tweaks.stune_boost, &format!("{}/schedtune.boost", p_str));
                write_lock(tweaks.stune_boost_enabled, &format!("{}/schedtune.sched_boost_enabled", p_str));
                write_lock("0", &format!("{}/schedtune.prefer_idle", p_str));
                write_lock("0", &format!("{}/schedtune.colocate", p_str));
            }
        }
    }

    // Battery saver module (legacy nodes use Y/N, newer nodes use 1/0)
    let bs_path = "/sys/module/battery_saver/parameters/enabled";
    if Path::new(bs_path).exists() {
        let content = fs::read_to_string(bs_path).unwrap_or_default();
        if content.chars().any(|c: char| c.is_ascii_digit()) {
            write_lock(tweaks.battery_saver, bs_path);
        } else {
            write_lock(tweaks.battery_saver_bool, bs_path);
        }
    }

    // Kernel scheduler flags
    write_lock(tweaks.split_lock_mitigate, "/proc/sys/kernel/split_lock_mitigate");

    let sched_feat = "/sys/kernel/debug/sched_features";
    if Path::new(sched_feat).exists() {
        for feature in tweaks.sched_features {
            write_lock(feature, sched_feat);
        }
    }

    // Queue block I/O
    if let Some(block_io) = tweaks.block_io {
        let read_ahead_kb = block_io.read_ahead_kb;
        let nr_requests = block_io.nr_requests;

        std::thread::spawn(move || {
            if let Ok(paths) = glob::glob("/sys/block/*") {
                for path in paths.flatten() {
                    if let Some(file_name) = path.file_name().and_then(|n| n.to_str()) {
                        if file_name == "mmcblk0" || file_name == "mmcblk1" || file_name.starts_with("sd") {
                            if let Some(p_str) = path.to_str() {
                                write_lock(read_ahead_kb, &format!("{}/queue/read_ahead_kb", p_str));
                                write_lock(nr_requests, &format!("{}/queue/nr_requests", p_str));
                            }
                        }
                    }
                }
            }
        });
    }
}

/// Step 7: force-stop background apps when the user enables `clearbg`.
pub fn clear_background_apps_if_enabled() {
    if get_clearapps() {
        clear_background_apps();
    }
}
