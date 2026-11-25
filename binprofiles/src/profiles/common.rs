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
//! 6. [`apply_common_tweaks`] — VM → workqueue → stune → uclamp → battery saver →
//!    split lock → sched features → block I/O, then [`apply_touchpanel`]
//! 7. Clear background apps (optional, performance only)
//! 8. Chipset-specific tweaks (EAS lives here, per SoC)
//! 9. [`apply_drop_caches`] — always the last write of the profile
//! 10. Log "… profile applied"
//!
//! `eas/enable` is *not* shared: only MediaTek exposes it in a way that is safe
//! to force, so each chipset module owns that node.

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
    /// `/proc/sys/vm/drop_caches` — only performance touches this node. Applied
    /// through [`apply_drop_caches`] after the chipset step, never here.
    pub drop_caches: Option<&'static str>,
    /// `/sys/module/workqueue/parameters/power_efficient`
    pub power_efficient: &'static str,
    /// `/dev/stune/top-app/schedtune.boost`
    pub stune_boost: &'static str,
    /// `/dev/stune/top-app/schedtune.sched_boost_enabled`
    pub stune_boost_enabled: &'static str,
    /// `/dev/stune/top-app/schedtune.prefer_idle`
    pub stune_prefer_idle: &'static str,
    /// `/dev/cpuctl/top-app/cpu.uclamp.min`
    pub uclamp_top_min: &'static str,
    /// `/dev/cpuctl/top-app/cpu.uclamp.latency_sensitive`
    pub uclamp_top_latency_sensitive: &'static str,
    /// `/dev/cpuctl/foreground/cpu.uclamp.min`
    pub uclamp_fg_min: &'static str,
    /// `/dev/cpuctl/{background,system-background}/cpu.uclamp.max`
    pub uclamp_bg_max: &'static str,
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

    // Workqueue
    write_lock(tweaks.power_efficient, "/sys/module/workqueue/parameters/power_efficient");

    // schedtune: top-app only. Boosting every group gives background work the
    // same capacity as the game and costs thermal headroom for no latency gain,
    // so only the foreground group is touched (same shape as Encore).
    let top_app = "/dev/stune/top-app";
    if Path::new(top_app).is_dir() {
        write_lock(tweaks.stune_boost, &format!("{}/schedtune.boost", top_app));
        write_lock(tweaks.stune_boost_enabled, &format!("{}/schedtune.sched_boost_enabled", top_app));
        write_lock(tweaks.stune_prefer_idle, &format!("{}/schedtune.prefer_idle", top_app));
    }

    // uclamp (modern GKI replacement for schedtune, adopted from Uperf unify_sched
    // and adapted per-profile): top-app gets latency_sensitive and min boost in
    // Performance, while background groups are capped in Eco/Performance.
    let cpuctl_top = "/dev/cpuctl/top-app";
    if Path::new(cpuctl_top).is_dir() {
        write_lock(tweaks.uclamp_top_min, &format!("{}/cpu.uclamp.min", cpuctl_top));
        write_lock("max", &format!("{}/cpu.uclamp.max", cpuctl_top));
        write_lock(
            tweaks.uclamp_top_latency_sensitive,
            &format!("{}/cpu.uclamp.latency_sensitive", cpuctl_top),
        );
    }
    let cpuctl_fg = "/dev/cpuctl/foreground";
    if Path::new(cpuctl_fg).is_dir() {
        write_lock(tweaks.uclamp_fg_min, &format!("{}/cpu.uclamp.min", cpuctl_fg));
        write_lock("max", &format!("{}/cpu.uclamp.max", cpuctl_fg));
        write_lock("0", &format!("{}/cpu.uclamp.latency_sensitive", cpuctl_fg));
    }
    for group in &["background", "system-background"] {
        let dir = format!("/dev/cpuctl/{}", group);
        if Path::new(&dir).is_dir() {
            write_lock("0", &format!("{}/cpu.uclamp.min", dir));
            write_lock(tweaks.uclamp_bg_max, &format!("{}/cpu.uclamp.max", dir));
            write_lock("0", &format!("{}/cpu.uclamp.latency_sensitive", dir));
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

        // Scoped on purpose: the profiler process exits as soon as the profile
        // returns, so a detached thread is killed mid-scan and the writes are
        // lost. Scoping still keeps the glob off the main path but guarantees
        // the worker finishes before the process goes away.
        std::thread::scope(|scope| {
            scope.spawn(move || {
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
        });
    }
}

/// Oppo/Oplus/Realme touch panel latency knobs (step 6.5).
///
/// `gaming = true` lowers the touch latency limits and enables the game switch;
/// `false` restores the stock behaviour when the profile is left.
pub fn apply_touchpanel(gaming: bool) {
    let path = "/proc/touchpanel";
    if !Path::new(path).exists() {
        return;
    }

    let (switch, limit, direction) = if gaming { ("1", "0", "1") } else { ("0", "1", "0") };

    write_lock(switch, &format!("{}/game_switch_enable", path));
    write_lock(limit, &format!("{}/oplus_tp_limit_enable", path));
    write_lock(limit, &format!("{}/oppo_tp_limit_enable", path));
    write_lock(direction, &format!("{}/oplus_tp_direction", path));
    write_lock(direction, &format!("{}/oppo_tp_direction", path));
}

/// Step 9: drop the page cache, once nothing else is left to write.
///
/// `drop_caches` stalls every I/O while the kernel walks the LRU, so running it
/// in the middle of a profile makes the GPU/DRAM/thermal writes that follow land
/// on a busy kernel. It is the last write on purpose.
pub fn apply_drop_caches(value: Option<&str>) {
    if let Some(value) = value {
        write_lock(value, "/proc/sys/vm/drop_caches");
    }
}

/// Step 7: force-stop background apps when the user enables `clearbg`.
pub fn clear_background_apps_if_enabled() {
    if get_clearapps() {
        clear_background_apps();
    }
}
