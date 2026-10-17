//! Snapdragon-specific tweaks.
//!
//! All three profiles deliberately share the same step order:
//!
//! 1. `apply_devfreq_governors`
//! 2. `apply_bus_dcvs`
//! 3. `apply_gpu_core_clock`
//! 4. `adrenoboost`

use crate::utils::*;
use std::path::Path;
use std::thread;

const GPU_PATH: &str = "/sys/class/kgsl/kgsl-3d0/devfreq";

/// devfreq nodes that get tuned (same order for every profile).
const DEVFREQ_PATTERNS: [&str; 5] = [
    "/*cpu-ddr-latfloor*",
    "/*cpu*-lat",
    "/*cpu-cpu-ddr-bw",
    "/*cpu-cpu-llcc-bw",
    "/*gpubw*",
];

const BALANCE_DEVFREQ_GOVS: [&str; 5] = ["compute", "mem_latency", "bw_hwmon", "bw_hwmon", "bw_vbif"];
const PERFORMANCE_DEVFREQ_GOVS: [&str; 5] = ["performance"; 5];
const POWERSAVE_DEVFREQ_GOVS: [&str; 5] = ["powersave"; 5];

const DCVS_BUSES: [&str; 4] = ["LLCC", "L3", "DDR", "DDRQOS"];

#[derive(Clone, Copy)]
enum BusBound {
    Max,
    Min,
}

#[derive(Clone, Copy)]
enum GpuBound {
    Lowest,
    SecondLowest,
    Highest,
}

fn apply_devfreq_governors(governors: &[&str; 5]) {
    for (pattern, governor) in DEVFREQ_PATTERNS.iter().zip(governors.iter()) {
        if let Ok(paths) = glob::glob(&format!("/sys/class/devfreq{}", pattern)) {
            for path in paths.flatten() {
                write_lock(governor, &format!("{}/governor", path.display()));
            }
        }
    }
}

/// Bus DCVS (LLCC/L3/DDR/DDRQOS): `max_bound` for `*/max_freq`,
/// `min_bound` for `*/min_freq`.
fn apply_bus_dcvs(max_bound: BusBound, min_bound: BusBound) {
    for bus in DCVS_BUSES {
        let base = format!("/sys/devices/system/cpu/bus_dcvs/{}", bus);
        if !Path::new(&base).exists() {
            continue;
        }

        let avail = format!("{}/available_frequencies", base);
        let (Some(max_freq), Some(min_freq)) = (which_maxfreq(&avail), which_minfreq(&avail)) else {
            continue;
        };

        let max_value = match max_bound {
            BusBound::Max => max_freq,
            BusBound::Min => min_freq,
        };
        let min_value = match min_bound {
            BusBound::Max => max_freq,
            BusBound::Min => min_freq,
        };

        if let Ok(paths) = glob::glob(&format!("{}/*/max_freq", base)) {
            for path in paths.flatten() {
                if let Some(p_str) = path.to_str() {
                    write_lock(&max_value.to_string(), p_str);
                }
            }
        }
        if let Ok(paths) = glob::glob(&format!("{}/*/min_freq", base)) {
            for path in paths.flatten() {
                if let Some(p_str) = path.to_str() {
                    write_lock(&min_value.to_string(), p_str);
                }
            }
        }
    }
}

fn gpu_bound(freqs: &[u64], bound: GpuBound) -> Option<u64> {
    match bound {
        GpuBound::Lowest => freqs.first().copied(),
        GpuBound::SecondLowest => freqs.get(1).or_else(|| freqs.first()).copied(),
        GpuBound::Highest => freqs.last().copied(),
    }
}

/// Set the GPU core (KGSL) `min_freq` / `max_freq`.
fn apply_gpu_core_clock(min_bound: GpuBound, max_bound: GpuBound) {
    if !Path::new(GPU_PATH).exists() {
        return;
    }

    let freqs = read_freqs(&format!("{}/available_frequencies", GPU_PATH));
    let (Some(min_freq), Some(max_freq)) = (gpu_bound(&freqs, min_bound), gpu_bound(&freqs, max_bound))
    else {
        return;
    };

    write_lock(&min_freq.to_string(), &format!("{}/min_freq", GPU_PATH));
    write_lock(&max_freq.to_string(), &format!("{}/max_freq", GPU_PATH));
}

pub fn snapdragon_balance() {
    apply_devfreq_governors(&BALANCE_DEVFREQ_GOVS);
    apply_bus_dcvs(BusBound::Max, BusBound::Min);
    apply_gpu_core_clock(GpuBound::SecondLowest, GpuBound::Highest);
    write_lock("1", &format!("{}/adrenoboost", GPU_PATH));
}

pub fn snapdragon_performance() {
    // devfreq & bus DCVS glob scanning runs on a separate thread so the
    // device does not lag; the write order still matches the other profiles.
    thread::spawn(|| apply_devfreq_governors(&PERFORMANCE_DEVFREQ_GOVS));
    thread::spawn(|| apply_bus_dcvs(BusBound::Max, BusBound::Max));

    apply_gpu_core_clock(GpuBound::Highest, GpuBound::Highest);
    write_lock("3", &format!("{}/adrenoboost", GPU_PATH));
}

pub fn snapdragon_powersave() {
    apply_devfreq_governors(&POWERSAVE_DEVFREQ_GOVS);
    apply_bus_dcvs(BusBound::Min, BusBound::Min);
    apply_gpu_core_clock(GpuBound::Lowest, GpuBound::Lowest);
    write_lock("0", &format!("{}/adrenoboost", GPU_PATH));
}
