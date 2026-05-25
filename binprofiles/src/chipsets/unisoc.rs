use crate::utils::*;

pub fn unisoc_balance() {
    if let Ok(mut paths) = glob::glob("/sys/class/devfreq/**/*.gpu") {
        if let Some(Ok(path)) = paths.next() {
            if let Some(p_str) = path.to_str() {
                devfreq_unlock(p_str);
            }
        }
    }
}

pub fn unisoc_performance() {
    let lite_mode = get_litemode();
    if let Ok(mut paths) = glob::glob("/sys/class/devfreq/**/*.gpu") {
        if let Some(Ok(path)) = paths.next() {
            if let Some(p_str) = path.to_str() {
                if lite_mode { devfreq_mid_perf(p_str); } else { devfreq_max_perf(p_str); }
            }
        }
    }
}

pub fn unisoc_powersave() {
    if let Ok(mut paths) = glob::glob("/sys/class/devfreq/**/*.gpu") {
        if let Some(Ok(path)) = paths.next() {
            if let Some(p_str) = path.to_str() {
                devfreq_min_perf(p_str);
            }
        }
    }
}
