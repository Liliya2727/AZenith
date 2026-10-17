//! devfreq helpers: unlock / pin device frequencies (GPU, MIF, etc).

use crate::utils::freq::{which_maxfreq, which_midfreq, which_minfreq};
use crate::utils::sysfs::{write_lock, write_unlock};
use std::path::Path;

/// max = highest frequency, min = lowest frequency (full range, locked).
pub fn devfreq_unlock(path: &str) {
    let avail = format!("{}/available_frequencies", path);
    if !Path::new(&avail).exists() { return; }

    if let (Some(max_freq), Some(min_freq)) = (which_maxfreq(&avail), which_minfreq(&avail)) {
        write_lock(&max_freq.to_string(), &format!("{}/max_freq", path));
        write_lock(&min_freq.to_string(), &format!("{}/min_freq", path));
    }
}

/// max = min = highest frequency (unlocked, the device is free to scale).
pub fn devfreq_max_perf(path: &str) {
    let avail = format!("{}/available_frequencies", path);
    if !Path::new(&avail).exists() { return; }

    if let Some(max_freq) = which_maxfreq(&avail) {
        write_unlock(&max_freq.to_string(), &format!("{}/max_freq", path));
        write_unlock(&max_freq.to_string(), &format!("{}/min_freq", path));
    }
}

/// max = highest (locked), min = middle frequency (unlocked).
pub fn devfreq_mid_perf(path: &str) {
    let avail = format!("{}/available_frequencies", path);
    if !Path::new(&avail).exists() { return; }

    if let (Some(max_freq), Some(mid_freq)) = (which_maxfreq(&avail), which_midfreq(&avail)) {
        write_lock(&max_freq.to_string(), &format!("{}/max_freq", path));
        write_unlock(&mid_freq.to_string(), &format!("{}/min_freq", path));
    }
}

/// max = min = lowest frequency (locked, power saving mode).
pub fn devfreq_min_perf(path: &str) {
    let avail = format!("{}/available_frequencies", path);
    if !Path::new(&avail).exists() { return; }

    if let Some(freq) = which_minfreq(&avail) {
        write_lock(&freq.to_string(), &format!("{}/min_freq", path));
        write_lock(&freq.to_string(), &format!("{}/max_freq", path));
    }
}
