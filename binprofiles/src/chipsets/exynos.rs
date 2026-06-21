use crate::utils::*;  use std::path::Path;

pub fn exynos_balance() {
    let gpu_path = "/sys/kernel/gpu";
    if Path::new(gpu_path).exists() {
        let avail = format!("{}/gpu_available_frequencies", gpu_path);
        if let (Some(max_freq), Some(min_freq)) = (which_maxfreq(&avail), which_minfreq(&avail)) {
            zeshia_def(&max_freq.to_string(), &format!("{}/gpu_max_clock", gpu_path));
            zeshia_def(&min_freq.to_string(), &format!("{}/gpu_min_clock", gpu_path));
        }
    }

    if let Ok(mut paths) = glob::glob("/sys/devices/platform/**/*.mali") {
        if let Some(Ok(path)) = paths.next() {
            zeshia_def("coarse_demand", &format!("{}/power_policy", path.display()));
        }
    }

    if let Ok(paths) = glob::glob("/sys/class/devfreq/*devfreq_mif*") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                devfreq_unlock(p_str);
            }
        }
    }
}

pub fn exynos_performance() {
    let lite_mode = get_litemode();
    let gpu_path = "/sys/kernel/gpu";

    if Path::new(gpu_path).exists() {
        let avail = format!("{}/gpu_available_frequencies", gpu_path);
        if let Some(max_freq) = which_maxfreq(&avail) {
            zeshia_def(&max_freq.to_string(), &format!("{}/gpu_max_clock", gpu_path));

            if lite_mode {
                if let Some(mid_freq) = which_midfreq(&avail) {
                    zeshia_def(&mid_freq.to_string(), &format!("{}/gpu_min_clock", gpu_path));
                }
            } else {
                zeshia_def(&max_freq.to_string(), &format!("{}/gpu_min_clock", gpu_path));
            }
        }
    }

    if let Ok(mut paths) = glob::glob("/sys/devices/platform/**/*.mali") {
        if let Some(Ok(path)) = paths.next() {
            zeshia_def("always_on", &format!("{}/power_policy", path.display()));
        }
    }

    if let Ok(paths) = glob::glob("/sys/class/devfreq/*devfreq_mif*") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                if lite_mode { devfreq_mid_perf(p_str); } else { devfreq_max_perf(p_str); }
            }
        }
    }
}

pub fn exynos_powersave() {
    let gpu_path = "/sys/kernel/gpu";
    if Path::new(gpu_path).exists() {
        let avail = format!("{}/gpu_available_frequencies", gpu_path);
        if let Some(freq) = which_minfreq(&avail) {
            zeshia_def(&freq.to_string(), &format!("{}/gpu_min_clock", gpu_path));
            zeshia_def(&freq.to_string(), &format!("{}/gpu_max_clock", gpu_path));
        }
    }
}
