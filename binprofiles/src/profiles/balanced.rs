//! Profile **balanced** (daily use).
//!
//! Follows the taxonomy order defined in [`super::common`]:
//! guard → resolve governor → apply governor → freq → log freq →
//! common tweaks → touchpanel → chipset → log.

use super::common::{
    apply_common_tweaks, apply_cpu_freq, apply_touchpanel, BlockIoTweaks, CommonTweaks,
    CpuFreqMode,
};
use crate::chipsets::{apply_chipset, ChipsetProfile};
use crate::utils::*;

const BLOCK_IO: BlockIoTweaks = BlockIoTweaks {
    read_ahead_kb: "128",
    nr_requests: "64",
};

const TWEAKS: CommonTweaks = CommonTweaks {
    vfs_cache_pressure: "120",
    drop_caches: None,
    power_efficient: "Y",
    stune_boost: "0",
    stune_boost_enabled: "0",
    stune_prefer_idle: "0",
    uclamp_top_min: "0",
    uclamp_top_latency_sensitive: "0",
    uclamp_fg_min: "0",
    uclamp_bg_max: "max",
    restrict_bg_cpuset: false,
    battery_saver: "0",
    battery_saver_bool: "N",
    split_lock_mitigate: "1",
    sched_features: &["NEXT_BUDDY", "TTWU_QUEUE"],
    block_io: Some(BLOCK_IO),
};

pub fn balanced_profile() {
    // 1. Guard
    if is_tweak_disabled() {
        return;
    }

    // 2. Resolve governor CPU / I/O scheduler / Mali GPU
    let c = cfg();
    let cpu_gov = first_or(
        &[&c.custom_default_cpu_gov, &c.default_cpu_gov],
        "schedutil",
    );
    let io_scheduler = first_or(
        &[&c.custom_default_io, &c.default_io],
        "none",
    );
    let mali_gov = first_non_empty(&[
        &c.custom_default_mali_gov,
        &c.default_mali_gov,
    ]);

    // 3. Apply governor
    apply_custom_governor_io(&cpu_gov, &io_scheduler, &mali_gov);

    // 4. CPU frequency
    apply_cpu_freq(CpuFreqMode::Normal);

    // 5. Log frequency mode
    if c.freq_offset == "Disabled" || c.freq_offset.is_empty() {
        log_info("cpu", "set freq mode=normal");
    } else {
        log_info("cpu", "set freq mode=normal_selected");
    }

    // 6. Shared tweaks
    apply_common_tweaks(&TWEAKS);

    // 6.5 Touch panel: restore stock latency limits
    apply_touchpanel(false);

    // 7. Clear background apps: performance profile only

    // 8. Chipset-specific tweaks
    apply_chipset(ChipsetProfile::Balanced);

    // 9. Done
    log_info("profiles", "balanced profile applied");
}
