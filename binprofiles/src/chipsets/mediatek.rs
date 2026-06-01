use crate::utils::*; use std::fs; use std::path::Path;

pub fn mediatek_balance() {
    if Path::new("/proc/ppm/policy_status").exists() {
        let content = fs::read_to_string("/proc/ppm/policy_status").unwrap_or_default();
        for line in content.lines() {
            if line.contains("FORCE_LIMIT") || line.contains("PWR_THRO") || line.contains("THERMAL") || line.contains("USER_LIMIT") {
                if let Some(idx) = line.split('[').nth(2).and_then(|s: &str| s.split(']').next()) {
                    zeshia_def(&format!("{} 1", idx), "/proc/ppm/policy_status");
                }
            }
            if line.contains("SYS_BOOST") {
                if let Some(idx) = line.split('[').nth(2).and_then(|s: &str| s.split(']').next()) {
                    zeshia_def(&format!("{} 0", idx), "/proc/ppm/policy_status");
                }
            }
        }
    }

    zeshia_def("0", "/proc/cpufreq/cpufreq_cci_mode");
    zeshia_def("1", "/proc/cpufreq/cpufreq_power_mode");

    if Path::new("/proc/gpufreq").exists() {
        zeshia_def("0", "/proc/gpufreq/gpufreq_opp_freq");
    } else if Path::new("/proc/gpufreqv2").exists() {
        zeshia_def("-1", "/proc/gpufreqv2/fix_target_opp_index");
    }

    zeshia_def("1", "/sys/devices/system/cpu/eas/enable");

    if Path::new("/proc/gpufreq/gpufreq_power_limited").exists() {
        let settings = [
            "ignore_batt_oc", "ignore_batt_percent", "ignore_low_batt",
            "ignore_thermal_protect", "ignore_pbm_limited"
        ];
        for setting in settings {
            zeshia_def(&format!("{} 0", setting), "/proc/gpufreq/gpufreq_power_limited");
        }
    }

    zeshia_def("0", "/proc/perfmgr/syslimiter/syslimiter_force_disable");
    zeshia_def("stop 0", "/proc/mtk_batoc_throttling/battery_oc_protect_stop");
    zeshia_def("stop 0", "/proc/pbm/pbm_stop");
    zeshia_def("1", "/sys/kernel/eara_thermal/enable");

    zeshia_def("-1", "/sys/devices/platform/10012000.dvfsrc/helio-dvfsrc/dvfsrc_req_ddr_opp");
    zeshia_def("-1", "/sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp");
    zeshia_def("userspace", "/sys/class/devfreq/mtk-dvfsrc-devfreq/governor");
    zeshia_def("userspace", "/sys/devices/platform/soc/1c00f000.dvfsrc/mtk-dvfsrc-devfreq/devfreq/mtk-dvfsrc-devfreq/governor");

    if let Ok(mut paths) = glob::glob("/sys/devices/platform/*.mali") {
        if let Some(Ok(path)) = paths.next() {
            zeshia_def("coarse_demand", &format!("{}/power_policy", path.display()));
        }
    }
}

pub fn mediatek_performance() {
    if Path::new("/proc/ppm/policy_status").exists() {
        let content = fs::read_to_string("/proc/ppm/policy_status").unwrap_or_default();
        for line in content.lines() {
            if line.contains("FORCE_LIMIT") || line.contains("PWR_THRO") || line.contains("THERMAL") || line.contains("USER_LIMIT") {
                if let Some(idx) = line.split('[').nth(2).and_then(|s: &str| s.split(']').next()) {
                    zeshia_def(&format!("{} 0", idx), "/proc/ppm/policy_status");
                }
            }
            if line.contains("SYS_BOOST") {
                if let Some(idx) = line.split('[').nth(2).and_then(|s: &str| s.split(']').next()) {
                    zeshia_def(&format!("{} 1", idx), "/proc/ppm/policy_status");
                }
            }
        }
    }

    zeshia_def("1", "/proc/cpufreq/cpufreq_cci_mode");
    zeshia_def("3", "/proc/cpufreq/cpufreq_power_mode");

    if Path::new("/proc/gpufreq").exists() {
        if let Some(freq) = get_mtk_gpu_max_freq() {
            zeshia_def(&freq.to_string(), "/proc/gpufreq/gpufreq_opp_freq");
        }
    } else if Path::new("/proc/gpufreqv2").exists() {
        zeshia_def("0", "/proc/gpufreqv2/fix_target_opp_index");
    }

    zeshia_def("0", "/sys/devices/system/cpu/eas/enable");

    if Path::new("/proc/gpufreq/gpufreq_power_limited").exists() {
        let settings = [
            "ignore_batt_oc", "ignore_batt_percent", "ignore_low_batt",
            "ignore_thermal_protect", "ignore_pbm_limited"
        ];
        for setting in settings {
            zeshia_def(&format!("{} 1", setting), "/proc/gpufreq/gpufreq_power_limited");
        }
    }

    zeshia_def("0", "/proc/perfmgr/syslimiter/syslimiter_force_disable");
    zeshia_def("stop 1", "/proc/mtk_batoc_throttling/battery_oc_protect_stop");
    zeshia_def("0", "/sys/kernel/eara_thermal/enable");

    zeshia_def("0", "/sys/devices/platform/10012000.dvfsrc/helio-dvfsrc/dvfsrc_req_ddr_opp");
    zeshia_def("0", "/sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp");
    zeshia_def("performance", "/sys/class/devfreq/mtk-dvfsrc-devfreq/governor");
    zeshia_def("performance", "/sys/devices/platform/soc/1c00f000.dvfsrc/mtk-dvfsrc-devfreq/devfreq/mtk-dvfsrc-devfreq/governor");

    if let Ok(mut paths) = glob::glob("/sys/devices/platform/*.mali") {
        if let Some(Ok(path)) = paths.next() {
            zeshia_def("always_on", &format!("{}/power_policy", path.display()));
        }
    }
}

pub fn mediatek_powersave() {
    if Path::new("/proc/ppm/policy_status").exists() {
        let content = fs::read_to_string("/proc/ppm/policy_status").unwrap_or_default();
        for line in content.lines() {
            if line.contains("FORCE_LIMIT") || line.contains("PWR_THRO") || line.contains("THERMAL") || line.contains("USER_LIMIT") {
                if let Some(idx) = line.split('[').nth(2).and_then(|s: &str| s.split(']').next()) {
                    zeshia_def(&format!("{} 1", idx), "/proc/ppm/policy_status");
                }
            }
            if line.contains("SYS_BOOST") {
                if let Some(idx) = line.split('[').nth(2).and_then(|s: &str| s.split(']').next()) {
                    zeshia_def(&format!("{} 0", idx), "/proc/ppm/policy_status");
                }
            }
        }
    }

    zeshia_def("0", "/sys/devices/platform/10012000.dvfsrc/helio-dvfsrc/dvfsrc_req_ddr_opp");
    zeshia_def("0", "/sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp");
    zeshia_def("powersave", "/sys/class/devfreq/mtk-dvfsrc-devfreq/governor");
    zeshia_def("powersave", "/sys/devices/platform/soc/1c00f000.dvfsrc/mtk-dvfsrc-devfreq/devfreq/mtk-dvfsrc-devfreq/governor");

    if Path::new("/proc/gpufreq/gpufreq_power_limited").exists() {
        let settings = [
            "ignore_batt_oc", "ignore_batt_percent", "ignore_low_batt",
            "ignore_thermal_protect", "ignore_pbm_limited"
        ];
        for setting in settings {
            zeshia_def(&format!("{} 1", setting), "/proc/gpufreq/gpufreq_power_limited");
        }
    }

    zeshia_def("0", "/proc/perfmgr/syslimiter/syslimiter_force_disable");
    zeshia_def("stop 0", "/proc/mtk_batoc_throttling/battery_oc_protect_stop");
    zeshia_def("stop 0", "/proc/pbm/pbm_stop");
    zeshia_def("stop 0", "/proc/mtk_batoc_throttling/battery_oc_protect_stop");
    zeshia_def("1", "/sys/kernel/eara_thermal/enable");

    if let Ok(mut paths) = glob::glob("/sys/devices/platform/*.mali") {
        if let Some(Ok(path)) = paths.next() {
            zeshia_def("coarse_demand", &format!("{}/power_policy", path.display()));
        }
    }
}
