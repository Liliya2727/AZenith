use crate::utils::*; use std::fs; use std::path::Path;

pub fn mediatek_balance() {
    if Path::new("/proc/ppm/policy_status").exists() {
        let content = fs::read_to_string("/proc/ppm/policy_status").unwrap_or_default();
        for line in content.lines() {
            let is_target_1 = line.contains("FORCE_LIMIT") || line.contains("PWR_THRO") || line.contains("THERMAL") || line.contains("USER_LIMIT");
            let is_target_2 = line.contains("SYS_BOOST");
            if is_target_1 || is_target_2 {
                if let Some(idx_str) = line.split('[').nth(1).and_then(|s| s.split(']').next()) {
                    if is_target_1 {
                        zeshia_def(&format!("{} 1", idx_str), "/proc/ppm/policy_status");
                    }
                    if is_target_2 {
                        zeshia_def(&format!("{} 0", idx_str), "/proc/ppm/policy_status");
                    }
                }
            }
        }
    }
    
    ppm_fix_freq("-1"); 
    
    zeshia_def("2", "/sys/kernel/fpsgo/common/force_onoff");
    zeshia_def("1", "/sys/module/sspm_v3/holders/ged/parameters/is_GED_KPI_enabled");
    
    zeshia_def("0", "/sys/devices/platform/boot_dramboost/dramboost/dramboost");

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

    zeshia_def("-1", "/sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp");
    zeshia_def("userspace", "/sys/class/devfreq/mtk-dvfsrc-devfreq/governor");
    
        // 1. Eksekusi statis untuk path /sys/kernel dan /sys/class (karena ini symlink global, tidak berubah)
    zeshia_def("-1", "/sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp");
    zeshia_def("userspace", "/sys/class/devfreq/mtk-dvfsrc-devfreq/governor");

    if let Ok(paths) = glob::glob("/sys/devices/platform/*.dvfsrc") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                zeshia_def("-1", &format!("{}/helio-dvfsrc/dvfsrc_req_ddr_opp", p_str));
            }
        }
    }

    if let Ok(paths) = glob::glob("/sys/devices/platform/soc/*.dvfsrc") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                zeshia_def("userspace", &format!("{}/mtk-dvfsrc-devfreq/devfreq/mtk-dvfsrc-devfreq/governor", p_str));
            }
        }
    }
    

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
            let is_target_1 = line.contains("FORCE_LIMIT") || line.contains("PWR_THRO") || line.contains("THERMAL") || line.contains("USER_LIMIT");
            let is_target_2 = line.contains("SYS_BOOST");
            if is_target_1 || is_target_2 {
                if let Some(idx_str) = line.split('[').nth(1).and_then(|s| s.split(']').next()) {
                    if is_target_1 {
                        zeshia_def(&format!("{} 0", idx_str), "/proc/ppm/policy_status");
                    }
                    if is_target_2 {
                        zeshia_def(&format!("{} 1", idx_str), "/proc/ppm/policy_status");
                    }
                }
            }
        }
    }

    
    ppm_fix_freq("0"); 

    let use_fpsgo = getprop("persist.sys.azenithconf.usefpsgo");
    if use_fpsgo == "0" {
        zeshia_def("0", "/sys/kernel/fpsgo/common/force_onoff");
    }
    
    zeshia_def("0", "/sys/module/sspm_v3/holders/ged/parameters/is_GED_KPI_enabled");
    
    zeshia_def("1", "/sys/devices/platform/boot_dramboost/dramboost/dramboost");

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

    zeshia_def("0", "/sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp");
    zeshia_def("performance", "/sys/class/devfreq/mtk-dvfsrc-devfreq/governor");
    
    if let Ok(paths) = glob::glob("/sys/devices/platform/*.dvfsrc") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                zeshia_def("0", &format!("{}/helio-dvfsrc/dvfsrc_req_ddr_opp", p_str));
            }
        }
    }

    if let Ok(paths) = glob::glob("/sys/devices/platform/soc/*.dvfsrc") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                zeshia_def("performance", &format!("{}/mtk-dvfsrc-devfreq/devfreq/mtk-dvfsrc-devfreq/governor", p_str));
            }
        }
    }

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
            let is_target_1 = line.contains("FORCE_LIMIT") || line.contains("PWR_THRO") || line.contains("THERMAL") || line.contains("USER_LIMIT");
            let is_target_2 = line.contains("SYS_BOOST");
            if is_target_1 || is_target_2 {
                if let Some(idx_str) = line.split('[').nth(1).and_then(|s| s.split(']').next()) {
                    if is_target_1 {
                        zeshia_def(&format!("{} 1", idx_str), "/proc/ppm/policy_status");
                    }
                    if is_target_2 {
                        zeshia_def(&format!("{} 0", idx_str), "/proc/ppm/policy_status");
                    }
                }
            }
        }
    }
    
    ppm_fix_freq("-1"); 
    zeshia_def("0", "/sys/devices/platform/boot_dramboost/dramboost/dramboost");
    zeshia_def("1", "/sys/kernel/fpsgo/common/force_onoff");
    zeshia_def("1", "/sys/module/sspm_v3/holders/ged/parameters/is_GED_KPI_enabled");

    zeshia_def("-1", "/sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp");
    zeshia_def("powersave", "/sys/class/devfreq/mtk-dvfsrc-devfreq/governor");
    
    if let Ok(paths) = glob::glob("/sys/devices/platform/*.dvfsrc") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                zeshia_def("-1", &format!("{}/helio-dvfsrc/dvfsrc_req_ddr_opp", p_str));
            }
        }
    }

    if let Ok(paths) = glob::glob("/sys/devices/platform/soc/*.dvfsrc") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                zeshia_def("powersave", &format!("{}/mtk-dvfsrc-devfreq/devfreq/mtk-dvfsrc-devfreq/governor", p_str));
            }
        }
    }

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
    zeshia_def("stop 0", "/proc/mtk_batoc_throttling/battery_oc_protect_stop");
    zeshia_def("1", "/sys/kernel/eara_thermal/enable");

    if let Ok(mut paths) = glob::glob("/sys/devices/platform/*.mali") {
        if let Some(Ok(path)) = paths.next() {
            zeshia_def("coarse_demand", &format!("{}/power_policy", path.display()));
        }
    }
}
