//! MediaTek-specific tweaks.
//!
//! All three profiles deliberately share the same step order:
//!
//! 1. `apply_ppm_policy_status`
//! 2. FPSGO
//! 3. GED KPI
//! 4. DRAM boost
//! 5. `cpufreq` cci/power mode
//! 6. `apply_gpu_opp_limit`
//! 7. EAS (`/sys/devices/system/cpu/eas/enable`) — MediaTek is the only SoC
//!    where forcing this node is safe, so it is owned here instead of in
//!    `profiles::common`. Every other chipset leaves EAS alone.
//! 8. `apply_gpu_power_limited`
//! 9. syslimiter + battery OC protect
//! 10. EARA thermal
//! 11. `apply_dvfsrc`
//! 12. `apply_mali_power_policy`

use crate::utils::*;
use std::fs;
use std::path::Path;

/// PPM policies that act as frequency limiters.
const PPM_LIMIT_POLICIES: [&str; 2] = ["PWR_THRO", "THERMAL"];

/// Knobs that ignore GPU power/thermal limits.
const GPU_POWER_LIMITED_KNOBS: [&str; 5] = [
    "ignore_batt_oc",
    "ignore_batt_percent",
    "ignore_low_batt",
    "ignore_thermal_protect",
    "ignore_pbm_limited",
];

const DVFSRC_DEVFREQ: &str = "/sys/class/devfreq/mtk-dvfsrc-devfreq";

/// DRAM (dvfsrc devfreq): `true` = pin to max (performance), `false` = full range (balanced/eco).
fn apply_dram_freq(pin_max: bool) {
    if pin_max {
        devfreq_max_perf(DVFSRC_DEVFREQ);
    } else {
        devfreq_unlock(DVFSRC_DEVFREQ);
    }
}

/// `1` = limits enforced (balanced/eco), `0` = limits lifted (performance).
fn apply_ppm_policy_status(limit: &str) {
    if !Path::new("/proc/ppm/policy_status").exists() {
        return;
    }

    let content = fs::read_to_string("/proc/ppm/policy_status").unwrap_or_default();
    for line in content.lines() {
        if !PPM_LIMIT_POLICIES.iter().any(|policy| line.contains(policy)) {
            continue;
        }

        let Some(idx_str) = line.split('[').nth(1).and_then(|s| s.split(']').next()) else {
            continue;
        };

        write_lock(&format!("{} {}", idx_str, limit), "/proc/ppm/policy_status");
    }
}

/// GPU OPP: `/proc/gpufreq` (legacy) or `/proc/gpufreqv2` (newer).
/// `max_freq` `None` means the `gpufreq_opp_freq` node is intentionally skipped.
fn apply_gpu_opp_limit(max_freq: Option<u64>, fix_index: &str) {
    if Path::new("/proc/gpufreq").exists() {
        if let Some(freq) = max_freq {
            write_lock(&freq.to_string(), "/proc/gpufreq/gpufreq_opp_freq");
        }
    } else if Path::new("/proc/gpufreqv2").exists() {
        write_lock(fix_index, "/proc/gpufreqv2/fix_target_opp_index");
    }
}

/// `1` = ignore limits (performance), `0` = respect limits (balanced/eco).
fn apply_gpu_power_limited(value: &str) {
    if !Path::new("/proc/gpufreq/gpufreq_power_limited").exists() {
        return;
    }

    for knob in GPU_POWER_LIMITED_KNOBS {
        write_lock(&format!("{} {}", knob, value), "/proc/gpufreq/gpufreq_power_limited");
    }
}

/// dvfsrc (Vcore + DDR): `force_opp` for the hard-limit node, `req_opp` for the
/// DDR request, `governor` for both dvfsrc devfreq nodes.
fn apply_dvfsrc(force_opp: &str, req_opp: &str, governor: &str) {
    write_lock(force_opp, "/sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp");
    write_lock(governor, "/sys/class/devfreq/mtk-dvfsrc-devfreq/governor");

    if let Ok(paths) = glob::glob("/sys/devices/platform/*.dvfsrc") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                write_lock(req_opp, &format!("{}/helio-dvfsrc/dvfsrc_req_ddr_opp", p_str));
            }
        }
    }

    if let Ok(paths) = glob::glob("/sys/devices/platform/soc/*.dvfsrc") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                write_lock(
                    governor,
                    &format!("{}/mtk-dvfsrc-devfreq/devfreq/mtk-dvfsrc-devfreq/governor", p_str),
                );
            }
        }
    }
}

/// Only the first GPU that is found gets touched.
fn apply_mali_power_policy(policy: &str) {
    if let Ok(mut paths) = glob::glob("/sys/devices/platform/*.mali") {
        if let Some(Ok(path)) = paths.next() {
            write_lock(policy, &format!("{}/power_policy", path.display()));
        }
    }
}

pub fn mediatek_balance() {
    apply_ppm_policy_status("1");
    write_lock("2", "/sys/kernel/fpsgo/common/force_onoff");
    write_lock("1", "/sys/module/sspm_v3/holders/ged/parameters/is_GED_KPI_enabled");
    write_lock("0", "/sys/devices/platform/boot_dramboost/dramboost/dramboost");
    write_lock("0", "/proc/cpufreq/cpufreq_cci_mode");
    write_lock("1", "/proc/cpufreq/cpufreq_power_mode");
    apply_gpu_opp_limit(Some(0), "-1");
    write_lock("1", "/sys/devices/system/cpu/eas/enable");
    apply_gpu_power_limited("0");
    
    write_lock("stop 0", "/proc/mtk_batoc_throttling/battery_oc_protect_stop");
    write_lock("1", "/sys/kernel/eara_thermal/enable");
    apply_dvfsrc("255", "-1", "userspace");
    apply_dram_freq(false);
    apply_mali_power_policy("coarse_demand");
}

pub fn mediatek_performance() {
    apply_ppm_policy_status("0");

    // FPSGO is only turned off when the user disables it via `usefpsgo`.
    if getprop("persist.sys.azenithconf.usefpsgo") == "0" {
        write_lock("0", "/sys/kernel/fpsgo/common/force_onoff");
    }

    write_lock("0", "/sys/module/sspm_v3/holders/ged/parameters/is_GED_KPI_enabled");
    write_lock("1", "/sys/devices/platform/boot_dramboost/dramboost/dramboost");
    write_lock("1", "/proc/cpufreq/cpufreq_cci_mode");
    write_lock("3", "/proc/cpufreq/cpufreq_power_mode");
    apply_gpu_opp_limit(get_mtk_gpu_max_freq(), "0");
    write_lock("0", "/sys/devices/system/cpu/eas/enable");
    apply_gpu_power_limited("1");
    
    write_lock("stop 1", "/proc/mtk_batoc_throttling/battery_oc_protect_stop");
    write_lock("0", "/sys/kernel/eara_thermal/enable");
    apply_dvfsrc("0", "0", "performance");
    apply_dram_freq(true);
    apply_mali_power_policy("always_on");
}

pub fn mediatek_powersave() {
    apply_ppm_policy_status("1");
    write_lock("2", "/sys/kernel/fpsgo/common/force_onoff");
    write_lock("1", "/sys/module/sspm_v3/holders/ged/parameters/is_GED_KPI_enabled");
    write_lock("0", "/sys/devices/platform/boot_dramboost/dramboost/dramboost");
    write_lock("0", "/proc/cpufreq/cpufreq_cci_mode");
    write_lock("1", "/proc/cpufreq/cpufreq_power_mode");
    apply_gpu_opp_limit(Some(0), "-1");
    write_lock("1", "/sys/devices/system/cpu/eas/enable");
    apply_gpu_power_limited("0");
    
    write_lock("stop 0", "/proc/mtk_batoc_throttling/battery_oc_protect_stop");
    write_lock("1", "/sys/kernel/eara_thermal/enable");
    apply_dvfsrc("255", "-1", "powersave");
    apply_dram_freq(false);
    apply_mali_power_policy("coarse_demand");
}
