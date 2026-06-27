use std::os::unix::fs::PermissionsExt;
use std::fs; use std::path::Path; use std::process::Command;

use glob::glob;
use std::collections::HashSet;

pub const CONFIG_PATH: &str = "/data/adb/.config/AZenith";
pub const MY_PATH: &str = "/system/bin:/system/xbin:/data/adb/ap/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:/sbin:/sbin/su:/su/bin:/su/xbin:/data/data/com.termux/files/usr/bin";


pub fn getprop(key: &str) -> String {
    if let Ok(output) = Command::new("getprop").arg(key).output() {
        String::from_utf8_lossy(&output.stdout).trim().to_string()
    } else {
        String::new()
    }
}


pub fn az_log(message: &str) {
    if get_debugmode() {
        let _ = Command::new("sys.azenith-service")
            .args(["--verboselog", "AZLog", "0", message])
            .status();
    }
}

pub fn dlog(message: &str) {
    let _ = Command::new("sys.azenith-service")
        .args(["--log", "AZenith_Profiler", "1", message])
        .status();
}

pub fn chmod(path: &str, mode: u32) {
    if let Ok(metadata) = fs::metadata(path) {
        let mut perms = metadata.permissions();
        perms.set_mode(mode);
        let _ = fs::set_permissions(path, perms);
    }
}

pub fn zeshia_core(value: &str, path_str: &str, lock: bool) {
    let path = Path::new(path_str);
    let parent_name = path.parent().and_then(|p| p.file_name()).unwrap_or_default().to_string_lossy();
    let file_name = path.file_name().unwrap_or_default().to_string_lossy();
    let pathname = if parent_name.is_empty() { file_name.into_owned() } else { format!("{}/{}", parent_name, file_name) };

    if !path.exists() { return; }

    chmod(path_str, 0o644);

    let val_with_newline = format!("{}\n", value);
    
    if fs::write(path, val_with_newline).is_err() {
        az_log(&format!("Cannot write to /{} (permission denied)", pathname));
        if lock { chmod(path_str, 0o444); }
        return;
    }

    az_log(&format!("Set /{} to {}", pathname, value));
    if lock { chmod(path_str, 0o444); }
}

pub fn zeshia(value: &str, path_str: &str) {
    zeshia_core(value, path_str, false);
}

pub fn zeshia_def(value: &str, path_str: &str) {
    zeshia_core(value, path_str, true);
}

pub fn get_limiter() -> u64 {
    let val = getprop("persist.sys.azenithconf.freqoffset");
    if val == "Disabled" || val.is_empty() {
        100
    } else {
        val.replace('%', "").parse().unwrap_or(100)
    }
}

pub fn get_debugmode() -> bool {
    getprop("persist.sys.azenith.debugmode") == "true"
}

pub fn get_clearapps() -> bool {
    getprop("persist.sys.azenithconf.clearbg") == "1"
}

pub fn get_litemode() -> bool {
    getprop("persist.sys.azenithconf.litemode") == "1"
}

pub fn get_curprofile() -> String {
    fs::read_to_string(format!("{}/API/current_profile", CONFIG_PATH))
        .unwrap_or_default()
        .trim()
        .to_string()
}

pub fn setprop_cmd(key: &str, value: &str) {
    let _ = Command::new("setprop").arg(key).arg(value).status();
}

pub fn applyfreqbalance() {
    if Path::new("/proc/ppm").exists() {
        dsetfreqppm();
    } else {
        dsetfreq();
    }
}

pub fn applyfreqgame() {
    if Path::new("/proc/ppm").exists() {
        dsetgamefreqppm();
    } else {
        dsetgamefreq();
    }
}

pub fn applyppmnfreqsets(value: &str, path: &str) {
    if Path::new(path).exists() {
        chmod(path, 0o644);
        // FIX: Tambahkan \n
        let val_with_newline = format!("{}\n", value);
        let _ = fs::write(path, val_with_newline);
        chmod(path, 0o444);
    }
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
    // Ekuivalen dengan logika awk mengambil nilai tengah
    Some(freqs[freqs.len() / 2])
}

pub fn setfreqs(file: &str, target: u64) -> u64 {
    let freqs = read_freqs(file);
    if freqs.is_empty() {
        return target;
    }
    // Mencari frekuensi yang selisihnya paling sedikit dengan target (closest match)
    *freqs
        .iter()
        .min_by_key(|&&f| (f as i64 - target as i64).abs())
        .unwrap_or(&target)
}

pub fn setgov(gov: &str) {
    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            chmod(p_str, 0o644);
            let _ = fs::write(p_str, gov);
            chmod(p_str, 0o444);
        }
    }

    // Lock additional policy paths
    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_governor") {
        for path in paths.flatten() {
            chmod(path.to_str().unwrap(), 0o444);
        }
    }
}

pub fn sets_io(scheduler: &str) {
    for block in &["sda", "sdb", "sdc", "mmcblk0", "mmcblk1"] {
        let path = format!("/sys/block/{}/queue/scheduler", block);
        if Path::new(&path).exists() {
            chmod(&path, 0o644);
            let _ = fs::write(&path, scheduler);
            chmod(&path, 0o444);
        }
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
                // Eco Mode (Profile 3)
                let target_min_target = cpu_maxfreq * 40 / 100;
                let new_minfreq = setfreqs(&avail_file, target_min_target);

                zeshia_def(&format!("{} {}", cluster, new_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                zeshia_def(&format!("{} {}", cluster, new_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                dlog(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_maxfreq, new_minfreq));
            } else {
                // Default Mode
                zeshia_def(&format!("{} {}", cluster, new_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                zeshia_def(&format!("{} {}", cluster, cpu_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                dlog(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_maxfreq, cpu_minfreq));
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

            // Di Bash, `basename /.../cpu0/cpufreq` menghasilkan "cpufreq".
            // Di Rust, agar log lebih informatif (menampilkan "cpu0"), kita ambil parent-nya.
            let policy_name = path.parent()
                .and_then(|p: &std::path::Path| p.file_name())
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
                // Eco Mode (Profile 3)
                let target_min_target = cpu_maxfreq * 40 / 100;
                let new_minfreq = setfreqs(&avail_file, target_min_target);

                zeshia_def(&new_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                zeshia_def(&new_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                dlog(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_maxfreq, new_minfreq));
                // Setara dengan `continue` di bash
            } else {
                // Default Mode
                zeshia_def(&new_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                zeshia_def(&cpu_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                dlog(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_maxfreq, cpu_minfreq));

                // Logika chmod ini hanya berjalan jika BUKAN di profile 3 (sama persis dengan bash)
                if let Ok(sc_paths) = glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq") {
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

                zeshia_def(&format!("{} {}", cluster, new_midfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                zeshia_def(&format!("{} {}", cluster, cpu_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                dlog(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_midfreq, cpu_minfreq));
            } else {
                zeshia_def(&format!("{} {}", cluster, cpu_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");             
                zeshia_def(&format!("{} {}", cluster, cpu_maxfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                dlog(&format!("Set {} maxfreq={} minfreq={}", policy_name, cpu_maxfreq, new_midfreq));
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
                .and_then(|p: &std::path::Path| p.file_name())
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

                // Bugfix: Menulis ke jalur sysfs standar, bukan ke /proc/ppm
                zeshia(&new_midfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                zeshia(&cpu_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                dlog(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_midfreq, cpu_minfreq));
            } else {
                zeshia(&cpu_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                zeshia(&cpu_maxfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                dlog(&format!("Set {} maxfreq={} minfreq={}", policy_name, cpu_maxfreq, new_midfreq));

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

                if let Ok(sc_paths) = glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq") {
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

                zeshia(&format!("{} {}", cluster, new_midfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                zeshia(&format!("{} {}", cluster, cpu_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                dlog(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_midfreq, cpu_minfreq));
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
                .and_then(|p: &std::path::Path| p.file_name())
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

                zeshia(&new_midfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                zeshia(&cpu_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                dlog(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_midfreq, cpu_minfreq));
            } else {
                applyppmnfreqsets(&cpu_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                applyppmnfreqsets(&new_midfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                if let Ok(sc_paths) = glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq") {
                    for sp in sc_paths.flatten() {
                        chmod(sp.to_str().unwrap(), 0o444);
                    }
                }
            }
        }
    }
}

pub fn devfreq_max_perf(path: &str) {
    let avail = format!("{}/available_frequencies", path);
    if !Path::new(&avail).exists() { return; }

    if let Some(max_freq) = which_maxfreq(&avail) {
        zeshia(&max_freq.to_string(), &format!("{}/max_freq", path));
        zeshia(&max_freq.to_string(), &format!("{}/min_freq", path));
    }
}

pub fn devfreq_mid_perf(path: &str) {
    let avail = format!("{}/available_frequencies", path);
    if !Path::new(&avail).exists() { return; }

    if let (Some(max_freq), Some(mid_freq)) = (which_maxfreq(&avail), which_midfreq(&avail)) {
        zeshia_def(&max_freq.to_string(), &format!("{}/max_freq", path));
        zeshia(&mid_freq.to_string(), &format!("{}/min_freq", path));
    }
}

pub fn devfreq_unlock(path: &str) {
    let avail = format!("{}/available_frequencies", path);
    if !Path::new(&avail).exists() { return; }

    if let (Some(max_freq), Some(min_freq)) = (which_maxfreq(&avail), which_minfreq(&avail)) {
        zeshia_def(&max_freq.to_string(), &format!("{}/max_freq", path));
        zeshia_def(&min_freq.to_string(), &format!("{}/min_freq", path));
    }
}

pub fn devfreq_min_perf(path: &str) {
    let avail = format!("{}/available_frequencies", path);
    if !Path::new(&avail).exists() { return; }

    if let Some(freq) = which_minfreq(&avail) {
        zeshia_def(&freq.to_string(), &format!("{}/min_freq", path));
        zeshia_def(&freq.to_string(), &format!("{}/max_freq", path));
    }
}


pub fn clear_background_apps() {
    // Menjalankan dumpsys window displays
    let output = Command::new("dumpsys").args(["window", "displays"]).output();
    let stdout = match output {
        Ok(o) => String::from_utf8_lossy(&o.stdout).into_owned(),
        Err(_) => return,
    };

    let mut visible_pkgs = HashSet::new();
    let mut invisible_pkgs = HashSet::new();

    // Membaca output per baris (setara grep "Task{")
    for line in stdout.lines() {
        if line.contains("Task{") {
            // Replikasi logika sed: 's/.*A=[0-9]*:\([^ ]*\).*/\1/p'
            if let Some(a_idx) = line.find("A=") {
                let part = &line[a_idx..];
                if let Some(colon_idx) = part.find(':') {
                    if let Some(space_idx) = part.find(' ') {
                        if colon_idx < space_idx {
                            let pkg = &part[colon_idx + 1..space_idx];

                            if line.contains("visible=true") {
                                visible_pkgs.insert(pkg.to_string());
                            } else if line.contains("visible=false") {
                                invisible_pkgs.insert(pkg.to_string());
                            }
                        }
                    }
                }
            }
        }
    }

    let exclude = ["com.android.systemui", "com.android.settings", "android", "system"];

    for pkg in invisible_pkgs {
        if visible_pkgs.contains(&pkg) {
            continue;
        }

        let is_excluded = exclude.iter().any(|&ex| pkg.contains(ex));

        if !is_excluded {
            let _ = Command::new("am")
                .args(["force-stop", &pkg])
                .status();

            az_log(&format!("Stopped app: {}", pkg));
        }
    }

    dlog("Cleared background apps");
}

pub fn get_mtk_gpu_max_freq() -> Option<u64> {
    let content = fs::read_to_string("/proc/gpufreq/gpufreq_opp_dump").unwrap_or_default();
    content.lines()
        .filter(|line: &&str| line.contains("freq = "))
        .filter_map(|line: &str| line.split("freq = ").nth(1)?.split_whitespace().next()?.parse::<u64>().ok())
        .max()
}

pub fn read_freqs(path: &str) -> Vec<u64> {
    let mut freqs: Vec<u64> = fs::read_to_string(path)
        .unwrap_or_default()
        .split_whitespace()
        .filter_map(|s: &str| s.parse().ok())
        .collect();
    freqs.sort_unstable();
    freqs
}

pub fn ppm_fix_freq(target_index: &str) {
    let ppm_path = "/proc/ppm/policy/ut_fix_freq_idx";

    if !Path::new(ppm_path).exists() {
        return;
    }

    let mut cluster_count = 0;
    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*") {
        cluster_count = paths.filter_map(Result::ok).count();
    }

    if cluster_count > 0 {
        let payload = vec![target_index; cluster_count].join(" ");

        zeshia_def(&payload, ppm_path);
        
    }
}

pub fn init_cpu_governor() {
    let cpu_path = "/sys/devices/system/cpu/cpu0/cpufreq";
    let gov_file = format!("{}/scaling_governor", cpu_path);
    chmod(&gov_file, 0o644);

    let mut default_gov = fs::read_to_string(&gov_file)
        .unwrap_or_default()
        .trim()
        .to_string();

    setprop_cmd("persist.sys.azenith.default_cpu_gov", &default_gov);
    dlog(&format!("Default CPU governor detected: {}", default_gov));

    // Handle fallback if default is 'performance'
    if default_gov == "performance" && getprop("persist.sys.azenith.custom_default_cpu_gov").is_empty() {
        dlog("Default governor is 'performance'");
        let avail_govs = fs::read_to_string(format!("{}/scaling_available_governors", cpu_path)).unwrap_or_default();
        let fallbacks = [
            "scx", "schedhorizon", "walt", "sched_pixel", "sugov_ext", "uag",
            "schedplus", "energy_step", "ondemand", "schedutil", "interactive",
            "conservative", "powersave"
        ];

        for gov in &fallbacks {
            if avail_govs.contains(gov) {
                setprop_cmd("persist.sys.azenith.default_cpu_gov", gov);
                default_gov = gov.to_string();
                dlog(&format!("Fallback governor to: {}", gov));
                break;
            }
        }
    }

    // Apply custom governor if set
    let custom_gov = getprop("persist.sys.azenith.custom_default_cpu_gov");
    if !custom_gov.is_empty() {
        default_gov = custom_gov;
    }
    
    dlog(&format!("Using CPU governor: {}", default_gov));
    setgov(&default_gov);

    // Set fallback props
    if getprop("persist.sys.azenith.custom_powersave_cpu_gov").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_powersave_cpu_gov", &default_gov);
    }
    if getprop("persist.sys.azenith.custom_performance_cpu_gov").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_performance_cpu_gov", &default_gov);
    }
    
    dlog("Parsing CPU Governor complete");
}

pub fn init_io_scheduler() {
    let mut io_path = String::new();
    for dev in &["mmcblk0", "mmcblk1", "sda", "sdb", "sdc"] {
        let p = format!("/sys/block/{}/queue", dev);
        if Path::new(&format!("{}/scheduler", p)).exists() {
            io_path = p;
            dlog(&format!("Detected valid block device: {}", dev));
            break;
        }
    }

    if io_path.is_empty() {
        dlog("No valid block device with scheduler found");
        std::process::exit(1);
    }

    let sched_file = format!("{}/scheduler", io_path);
    chmod(&sched_file, 0o644);

    // Parse active IO scheduler (the one inside brackets [])
    let mut default_io = String::new();
    let sched_content = fs::read_to_string(&sched_file).unwrap_or_default();
    if let Some(start) = sched_content.find('[') {
        if let Some(end) = sched_content[start..].find(']') {
            default_io = sched_content[start + 1..start + end].to_string();
        }
    }

    setprop_cmd("persist.sys.azenith.default_balanced_IO", &default_io);
    dlog(&format!("Default IO Scheduler detected: {}", default_io));

    // Apply custom IO if set
    let custom_io = getprop("persist.sys.azenith.custom_default_balanced_IO");
    if !custom_io.is_empty() {
        default_io = custom_io;
    }
    
    dlog(&format!("Using IO Scheduler: {}", default_io));
    sets_io(&default_io);

    // Set fallback props
    if getprop("persist.sys.azenith.custom_powersave_IO").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_powersave_IO", &default_io);
    }
    if getprop("persist.sys.azenith.custom_performance_IO").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_performance_IO", &default_io);
    }
    
    dlog("Parsing IO Scheduler complete");
}

pub fn init_maligpu_governor() {
    let mut gpu_path = String::new();
    
    if let Ok(paths) = glob::glob("/sys/class/devfreq/*.mali") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                gpu_path = p_str.to_string();
                dlog(&format!("Detected Mali GPU path: {}", gpu_path));
                break;
            }
        }
    }

    if gpu_path.is_empty() {
        return;
    }

    let gov_file = format!("{}/governor", gpu_path);
    chmod(&gov_file, 0o644);

    let mut default_maligpu_gov = fs::read_to_string(&gov_file)
        .unwrap_or_default()
        .trim()
        .to_string();

    setprop_cmd("persist.sys.azenith.default_maligpu_gov", &default_maligpu_gov);
    dlog(&format!("Default Mali GPU governor detected: {}", default_maligpu_gov));

    let custom_maligpu_gov = getprop("persist.sys.azenith.custom_default_maligpu_gov");
    if !custom_maligpu_gov.is_empty() {
        default_maligpu_gov = custom_maligpu_gov;
    }

    dlog(&format!("Using Mali GPU governor: {}", default_maligpu_gov));
    zeshia_def(&default_maligpu_gov, &gov_file);

    if getprop("persist.sys.azenith.custom_powersave_maligpu_gov").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_powersave_maligpu_gov", &default_maligpu_gov);
    }
    if getprop("persist.sys.azenith.custom_performance_maligpu_gov").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_performance_maligpu_gov", &default_maligpu_gov);
    }

    dlog("Parsing Mali GPU Governor complete");
}

pub fn sets_mali_gov(gov: &str) {
    if let Ok(paths) = glob("/sys/class/devfreq/*.mali/governor") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                chmod(p_str, 0o644);
                let _ = fs::write(p_str, gov);
                chmod(p_str, 0o444); 
            }
        }
    }
}
