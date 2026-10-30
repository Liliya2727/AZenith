//! Profile **eco** (power saving).
//!
//! Follows the taxonomy order defined in [`super::common`]:
//! guard → resolve governor → apply governor → freq → log freq →
//! common tweaks → touchpanel → chipset → log.

use super::common::{apply_common_tweaks, apply_cpu_freq, apply_touchpanel, CommonTweaks, CpuFreqMode};
use crate::chipsets::{apply_chipset, ChipsetProfile};
use crate::utils::*;

const TWEAKS: CommonTweaks = CommonTweaks {
    vfs_cache_pressure: "120",
    drop_caches: None,
    power_efficient: "Y",
    stune_boost: "0",
    stune_boost_enabled: "0",
    stune_prefer_idle: "0",
    battery_saver: "1",
    battery_saver_bool: "Y",
    split_lock_mitigate: "1",
    sched_features: &["NO_NEXT_BUDDY", "NO_TTWU_QUEUE"],
    block_io: None,
};

pub fn eco_mode() {
    // 1. Guard
    if is_tweak_disabled() {
        return;
    }

    // 2. Resolve governor CPU / I/O scheduler / Mali GPU
    let cpu_gov = prop_or(&["persist.sys.azenith.custom_powersave_cpu_gov"], "powersave");
    let io_scheduler = prop_or(&["persist.sys.azenith.custom_powersave_IO"], "none");
    let mali_gov = prop_first(&[
        "persist.sys.azenith.custom_powersave_maligpu_gov",
        "persist.sys.azenith.custom_default_maligpu_gov",
        "persist.sys.azenith.default_maligpu_gov",
    ]);

    // 3. Apply governor
    apply_custom_governor_io(&cpu_gov, &io_scheduler, &mali_gov);

    // 4. CPU frequency (min freq raised to 40% of max inside setfreq)
    apply_cpu_freq(CpuFreqMode::Normal);

    // 5. Log frequency mode
    log_info("cpu", "set freq mode=low");

    // 6. Shared tweaks
    apply_common_tweaks(&TWEAKS);

    // 6.5 Touch panel: restore stock latency limits
    apply_touchpanel(false);

    // 7. Clear background apps: performance profile only

    // 8. Chipset-specific tweaks
    apply_chipset(ChipsetProfile::Powersave);

    // 9. Done
    log_info("profiles", "eco profile applied");
}
