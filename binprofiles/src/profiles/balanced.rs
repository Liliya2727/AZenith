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
    let cpu_gov = prop_or(
        &[
            "persist.sys.azenith.custom_default_cpu_gov",
            "persist.sys.azenith.default_cpu_gov",
        ],
        "schedutil",
    );
    let io_scheduler = prop_or(
        &[
            "persist.sys.azenith.custom_default_balanced_IO",
            "persist.sys.azenith.default_balanced_IO",
        ],
        "none",
    );
    let mali_gov = prop_first(&[
        "persist.sys.azenith.custom_default_maligpu_gov",
        "persist.sys.azenith.default_maligpu_gov",
    ]);

    // 3. Apply governor
    apply_custom_governor_io(&cpu_gov, &io_scheduler, &mali_gov);

    // 4. CPU frequency
    apply_cpu_freq(CpuFreqMode::Normal);

    // 5. Log frequency mode
    if getprop("persist.sys.azenithconf.freqoffset") == "Disabled" {
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
