//! Profile **performance** (gaming).
//!
//! Follows the taxonomy order defined in [`super::common`]:
//! guard → resolve governor → apply governor → freq → log freq →
//! common tweaks → clear bg apps → chipset → log.

use super::common::{
    apply_common_tweaks, apply_cpu_freq, clear_background_apps_if_enabled, BlockIoTweaks,
    CommonTweaks, CpuFreqMode,
};
use crate::chipsets::{apply_chipset, ChipsetProfile};
use crate::utils::*;

const BLOCK_IO: BlockIoTweaks = BlockIoTweaks {
    read_ahead_kb: "32",
    nr_requests: "32",
};

const TWEAKS: CommonTweaks = CommonTweaks {
    vfs_cache_pressure: "80",
    drop_caches: Some("3"),
    power_efficient: "N",
    eas_enabled: "0",
    stune_boost: "30",
    stune_boost_enabled: "1",
    battery_saver: "0",
    battery_saver_bool: "N",
    split_lock_mitigate: "0",
    sched_features: &["NEXT_BUDDY", "NO_TTWU_QUEUE"],
    block_io: Some(BLOCK_IO),
};

pub fn performance_profile() {
    // 1. Guard
    if is_tweak_disabled() {
        return;
    }

    let lite_mode = get_litemode();

    // 2. Resolve governor CPU / I/O scheduler / Mali GPU
    let cpu_gov = prop_or(&["persist.sys.azenith.custom_performance_cpu_gov"], "powersave");
    let io_scheduler = prop_or(
        &[
            "persist.sys.azenith.custom_performance_IO",
            "persist.sys.azenith.custom_default_balanced_IO",
            "persist.sys.azenith.default_balanced_IO",
        ],
        "none",
    );
    let mali_gov = prop_first(&[
        "persist.sys.azenith.custom_performance_maligpu_gov",
        "persist.sys.azenith.custom_default_maligpu_gov",
        "persist.sys.azenith.default_maligpu_gov",
    ]);

    // 3. Apply governor
    apply_custom_governor_io(&cpu_gov, &io_scheduler, &mali_gov);

    // 4. CPU frequency
    apply_cpu_freq(CpuFreqMode::MaxAvailable);

    // 5. Log frequency mode
    if !lite_mode {
        log_info("cpu", "set freq mode=max_available");
    } else {
        log_info("cpu", "set freq mode=normal");
    }

    // 6. Shared tweaks
    apply_common_tweaks(&TWEAKS);

    // 7. Clear background apps
    clear_background_apps_if_enabled();

    // 8. Chipset-specific tweaks (skipped in Lite Mode)
    if !lite_mode {
        apply_chipset(ChipsetProfile::Performance);
    }

    // 9. Done
    log_info("profiles", "performance profile applied");
}
