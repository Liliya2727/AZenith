//! Chipset-specific tweaks.
//!
//! The chipset is selected through `persist.sys.azenith.soctype`:
//! `1` MediaTek, `2` Snapdragon, `3` Exynos, `4` UNISOC, `5` Tensor.
//!
//! Each chipset has three variants sharing the *same step order* — only the
//! written values differ, not which node gets touched first.

pub mod exynos;
pub mod mediatek;
pub mod snapdragon;
pub mod tensor;
pub mod unisoc;

use crate::utils::getprop;

/// Profile variant applied to the chipset.
#[derive(Clone, Copy, Debug)]
pub enum ChipsetProfile {
    Balanced,
    Performance,
    Powersave,
}

/// `(balanced, performance, powersave)` tuple for a single SoC.
type ChipsetFns = (fn(), fn(), fn());

/// Dispatch chipset tweaks for the SoC that is currently running.
pub fn apply_chipset(profile: ChipsetProfile) {
    let soc = getprop("persist.sys.azenith.soctype");

    let (balanced, performance, powersave): ChipsetFns = match soc.as_str() {
        "1" => (
            mediatek::mediatek_balance,
            mediatek::mediatek_performance,
            mediatek::mediatek_powersave,
        ),
        "2" => (
            snapdragon::snapdragon_balance,
            snapdragon::snapdragon_performance,
            snapdragon::snapdragon_powersave,
        ),
        "3" => (
            exynos::exynos_balance,
            exynos::exynos_performance,
            exynos::exynos_powersave,
        ),
        "4" => (
            unisoc::unisoc_balance,
            unisoc::unisoc_performance,
            unisoc::unisoc_powersave,
        ),
        "5" => (
            tensor::tensor_balance,
            tensor::tensor_performance,
            tensor::tensor_powersave,
        ),
        _ => return,
    };

    match profile {
        ChipsetProfile::Balanced => balanced(),
        ChipsetProfile::Performance => performance(),
        ChipsetProfile::Powersave => powersave(),
    }
}
