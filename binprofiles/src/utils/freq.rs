//! CPU frequency reads/writes (plain sysfs and the MediaTek PPM path).

use crate::utils::logging::log_debug;
use crate::utils::props::{get_curprofile, get_limiter, get_litemode};
use crate::utils::sysfs::{chmod, write_lock, write_unlock};
use std::fs;
use std::path::Path;

pub fn read_freqs(path: &str) -> Vec<u64> {
    let mut freqs: Vec<u64> = fs::read_to_string(path)
        .unwrap_or_default()
        .split_whitespace()
        .filter_map(|s: &str| s.parse().ok())
        .collect();
    freqs.sort_unstable();
    freqs
}

pub fn which_maxfreq(path: &str) -> Option<u64> {
    read_freqs(path).into_iter().max()
}

pub fn which_minfreq(path: &str) -> Option<u64> {
    read_freqs(path).into_iter().min()
}

pub fn which_midfreq(path: &str) -> Option<u64> {
    let freqs = read_freqs(path);
    if freqs.is_empty() {
        return None;
    }
    // Equivalent to the awk middle-value logic
    Some(freqs[freqs.len() / 2])
}

/// Find the frequency in `file` that is closest to `target`.
pub fn setfreqs(file: &str, target: u64) -> u64 {
    let freqs = read_freqs(file);
    if freqs.is_empty() {
        return target;
    }
    *freqs
        .iter()
        .min_by_key(|&&f| (f as i64 - target as i64).abs())
        .unwrap_or(&target)
}

/// Maximum GPU frequency parsed from the MediaTek OPP dump.
pub fn get_mtk_gpu_max_freq() -> Option<u64> {
    let content = fs::read_to_string("/proc/gpufreq/gpufreq_opp_dump").unwrap_or_default();
    content.lines()
        .filter(|line: &&str| line.contains("freq = "))
        .filter_map(|line: &str| line.split("freq = ").nth(1)?.split_whitespace().next()?.parse::<u64>().ok())
        .max()
}

/// Re-apply the balanced frequency at runtime (user changed the freq offset).
pub fn applyfreqbalance() {
    if Path::new("/proc/ppm").exists() {
        dsetfreqppm();
    } else {
        dsetfreq();
    }
}

/// Re-apply the performance frequency at runtime.
pub fn applyfreqgame() {
    if Path::new("/proc/ppm").exists() {
        dsetgamefreqppm();
    } else {
        dsetgamefreq();
    }
}

/// Quiet variant (no per-write log) for the runtime paths above.
pub fn applyppmnfreqsets(value: &str, path: &str) {
    if Path::new(path).exists() {
        chmod(path, 0o644);
        // FIX: append \n
        let val_with_newline = format!("{}\n", value);
        let _ = fs::write(path, val_with_newline);
        chmod(path, 0o444);
    }
}

pub fn setfreqppm() {
    if !Path::new("/proc/ppm").exists() {
        return;
    }

    let limiter = get_limiter();
    let curprofile = get_curprofile();
    let mut cluster = 0;

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let policy_name = path.file_name().unwrap_or_default().to_string_lossy();

            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);
            let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_max_target = cpu_maxfreq * limiter / 100;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_maxfreq = setfreqs(&avail_file, new_max_target);

            if curprofile == "3" {
            
                let new_maxfreq = which_midfreq(&avail_file).unwrap_or(new_maxfreq);
            
                write_lock(&format!("{} {}", cluster, new_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                write_lock(&format!("{} {}", cluster, cpu_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");
            
                log_debug("cpu", &format!("set policy={} maxfreq={} minfreq={}", policy_name, new_maxfreq, cpu_minfreq));
            } else {
                write_lock(&format!("{} {}", cluster, new_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                write_lock(&format!("{} {}", cluster, cpu_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                log_debug("cpu", &format!("set policy={} maxfreq={} minfreq={}", policy_name, new_maxfreq, cpu_minfreq));
            }
            cluster += 1;
        }
    }
}

pub fn setfreq() {
    let limiter = get_limiter();
    let curprofile = get_curprofile();

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/*/cpufreq") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();

            let policy_name = path.parent()
                .and_then(|p: &Path| p.file_name())
                .unwrap_or_default()
                .to_string_lossy();

            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);
            let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_max_target = cpu_maxfreq * limiter / 100;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_maxfreq = setfreqs(&avail_file, new_max_target);

            if curprofile == "3" {
            
                let new_maxfreq = which_midfreq(&avail_file).unwrap_or(new_maxfreq);
            
                write_lock(&new_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                write_lock(&cpu_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));
            
                log_debug("cpu", &format!("set policy={} maxfreq={} minfreq={}", policy_name, new_maxfreq, cpu_minfreq));
            } else {

                write_lock(&new_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                write_lock(&cpu_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                log_debug("cpu", &format!("set policy={} maxfreq={} minfreq={}", policy_name, new_maxfreq, cpu_minfreq));

                if let Ok(sc_paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq") {
                    for sp in sc_paths.flatten() {
                        chmod(sp.to_str().unwrap(), 0o444);
                    }
                }
            }
        }
    }
}

pub fn setgamefreqppm() {
    if !Path::new("/proc/ppm").exists() {
        return;
    }

    let litemode = get_litemode();
    let mut cluster = 0;

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let policy_name = path.file_name().unwrap_or_default().to_string_lossy();

            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_midtarget = cpu_maxfreq;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_midfreq = setfreqs(&avail_file, new_midtarget);

            if litemode {
                let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                    .unwrap_or_default().trim().parse().unwrap_or(0);

                write_lock(&format!("{} {}", cluster, new_midfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                write_lock(&format!("{} {}", cluster, cpu_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                log_debug("cpu", &format!("set policy={} maxfreq={} minfreq={}", policy_name, new_midfreq, cpu_minfreq));
            } else {
                write_lock(&format!("{} {}", cluster, cpu_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");             
                write_lock(&format!("{} {}", cluster, cpu_maxfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                log_debug("cpu", &format!("set policy={} maxfreq={} minfreq={}", policy_name, cpu_maxfreq, new_midfreq));
            }
            cluster += 1;
        }
    }
}

pub fn setgamefreq() {
    let litemode = get_litemode();

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/*/cpufreq") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let policy_name = path.parent()
                .and_then(|p: &Path| p.file_name())
                .unwrap_or_default()
                .to_string_lossy();

            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_midtarget = cpu_maxfreq;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_midfreq = setfreqs(&avail_file, new_midtarget);

            if litemode {
                let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                    .unwrap_or_default().trim().parse().unwrap_or(0);

                // Bugfix: write to the plain sysfs paths, not to /proc/ppm
                write_unlock(&new_midfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                write_unlock(&cpu_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                log_debug("cpu", &format!("set policy={} maxfreq={} minfreq={}", policy_name, new_midfreq, cpu_minfreq));
            } else {
                write_unlock(&cpu_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                write_unlock(&cpu_maxfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                log_debug("cpu", &format!("set policy={} maxfreq={} minfreq={}", policy_name, cpu_maxfreq, new_midfreq));

            }
        }
    }
}

pub fn dsetfreqppm() {
    if !Path::new("/proc/ppm").exists() {
        return;
    }

    let limiter = get_limiter();
    let curprofile = get_curprofile();
    let mut cluster = 0;

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);
            let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_max_target = cpu_maxfreq * limiter / 100;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_maxfreq = setfreqs(&avail_file, new_max_target);

            if curprofile == "3" {
                let target_min_target = cpu_maxfreq * 40 / 100;
                let new_minfreq = setfreqs(&avail_file, target_min_target);

                applyppmnfreqsets(&format!("{} {}", cluster, new_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                applyppmnfreqsets(&format!("{} {}", cluster, new_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");
            } else {
                applyppmnfreqsets(&format!("{} {}", cluster, new_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                applyppmnfreqsets(&format!("{} {}", cluster, cpu_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");
            }
            cluster += 1;
        }
    }
}

pub fn dsetfreq() {
    let limiter = get_limiter();
    let curprofile = get_curprofile();

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/*/cpufreq") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);
            let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_max_target = cpu_maxfreq * limiter / 100;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_maxfreq = setfreqs(&avail_file, new_max_target);

            if curprofile == "3" {
                let target_min_target = cpu_maxfreq * 40 / 100;
                let new_minfreq = setfreqs(&avail_file, target_min_target);

                applyppmnfreqsets(&new_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                applyppmnfreqsets(&new_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));
            } else {
                applyppmnfreqsets(&new_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                applyppmnfreqsets(&cpu_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                if let Ok(sc_paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq") {
                    for sp in sc_paths.flatten() {
                        chmod(sp.to_str().unwrap(), 0o444);
                    }
                }
            }
        }
    }
}

pub fn dsetgamefreqppm() {
    if !Path::new("/proc/ppm").exists() {
        return;
    }

    let litemode = get_litemode();
    let mut cluster = 0;

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let policy_name = path.file_name().unwrap_or_default().to_string_lossy();

            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_midtarget = cpu_maxfreq;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_midfreq = setfreqs(&avail_file, new_midtarget);

            if litemode {
                let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                    .unwrap_or_default().trim().parse().unwrap_or(0);

                write_unlock(&format!("{} {}", cluster, new_midfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                write_unlock(&format!("{} {}", cluster, cpu_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                log_debug("cpu", &format!("set policy={} maxfreq={} minfreq={}", policy_name, new_midfreq, cpu_minfreq));
            } else {
                applyppmnfreqsets(&format!("{} {}", cluster, new_midfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");
            }
            cluster += 1;
        }
    }
}

pub fn dsetgamefreq() {
    let litemode = get_litemode();

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/*/cpufreq") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let policy_name = path.parent()
                .and_then(|p: &Path| p.file_name())
                .unwrap_or_default()
                .to_string_lossy();

            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_midtarget = cpu_maxfreq;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_midfreq = setfreqs(&avail_file, new_midtarget);

            if litemode {
                let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                    .unwrap_or_default().trim().parse().unwrap_or(0);

                write_unlock(&new_midfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                write_unlock(&cpu_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                log_debug("cpu", &format!("set policy={} maxfreq={} minfreq={}", policy_name, new_midfreq, cpu_minfreq));
            } else {
                applyppmnfreqsets(&cpu_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                applyppmnfreqsets(&new_midfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                if let Ok(sc_paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq") {
                    for sp in sc_paths.flatten() {
                        chmod(sp.to_str().unwrap(), 0o444);
                    }
                }
            }
        }
    }
}
