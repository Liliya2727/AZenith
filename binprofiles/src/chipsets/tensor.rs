use crate::utils::*;

pub fn tensor_balance() {
    if let Ok(mut paths) = glob::glob("/sys/devices/platform/**/*.mali") {
        if let Some(Ok(path)) = paths.next() {
            if let Some(gpu_path) = path.to_str() {
                let avail = format!("{}/available_frequencies", gpu_path);
                if let (Some(max_freq), Some(min_freq)) = (which_maxfreq(&avail), which_minfreq(&avail)) {
                    zeshia_def(&max_freq.to_string(), &format!("{}/scaling_max_freq", gpu_path));
                    zeshia_def(&min_freq.to_string(), &format!("{}/scaling_min_freq", gpu_path));
                }
            }
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

pub fn tensor_performance() {
    let lite_mode = get_litemode();

    if let Ok(mut paths) = glob::glob("/sys/devices/platform/**/*.mali") {
        if let Some(Ok(path)) = paths.next() {
            if let Some(gpu_path) = path.to_str() {
                let avail = format!("{}/available_frequencies", gpu_path);
                if let Some(max_freq) = which_maxfreq(&avail) {
                    zeshia_def(&max_freq.to_string(), &format!("{}/scaling_max_freq", gpu_path));

                    if lite_mode {
                        if let Some(mid_freq) = which_midfreq(&avail) {
                            zeshia_def(&mid_freq.to_string(), &format!("{}/scaling_min_freq", gpu_path));
                        }
                    } else {
                        zeshia_def(&max_freq.to_string(), &format!("{}/scaling_min_freq", gpu_path));
                    }
                }
            }
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

pub fn tensor_powersave() {
    if let Ok(mut paths) = glob::glob("/sys/devices/platform/**/*.mali") {
        if let Some(Ok(path)) = paths.next() {
            if let Some(gpu_path) = path.to_str() {
                let avail = format!("{}/available_frequencies", gpu_path);
                if let Some(freq) = which_minfreq(&avail) {
                    zeshia_def(&freq.to_string(), &format!("{}/scaling_min_freq", gpu_path));
                    zeshia_def(&freq.to_string(), &format!("{}/scaling_max_freq", gpu_path));
                }
            }
        }
    }
}
