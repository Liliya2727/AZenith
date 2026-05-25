use crate::utils::*; use std::fs; use std::path::Path; use std::process::Command;
use crate::chipsets::mediatek::*;
use crate::chipsets::snapdragon::*;
use crate::chipsets::exynos::*;
use crate::chipsets::unisoc::*;
use crate::chipsets::tensor::*;

pub fn performance_profile() {
    let lite_mode = get_litemode();

    if !lite_mode {
        setgov("performance");
        dlog("Applying global governor: performance");
    } else {
        let big_policy = get_biggest_cluster();
        if !big_policy.is_empty() {
            let path = format!("/sys/devices/system/cpu/cpufreq/{}/scaling_governor", big_policy);
            chmod(&path, 0o644);
            let _ = fs::write(&path, "performance");
            chmod(&path, 0o444);
            dlog(&format!("Applying performance only to biggest cluster: {}", big_policy));
        }
    }

    let custom_perf_io = getprop("persist.sys.azenith.custom_performance_IO");
    if !custom_perf_io.is_empty() {
        sets_io(&custom_perf_io);
        dlog(&format!("Applying I/O scheduler to : {}", custom_perf_io));
    } else {
        let mut default_io = getprop("persist.sys.azenith.custom_default_balanced_IO");
        if default_io.is_empty() {
            default_io = getprop("persist.sys.azenith.default_balanced_IO");
        }
        if default_io.is_empty() {
            default_io = "none".to_string();
        }
        sets_io(&default_io);
        dlog(&format!("Applying I/O scheduler to : {}", default_io));
    }

    if Path::new("/proc/ppm").exists() {
        setgamefreqppm();
    } else {
        setgamefreq();
    }

    if !lite_mode {
        dlog("Set CPU freq to max available Frequencies");
    } else {
        dlog("Set CPU freq to normal Frequencies");
    }

    let pl_base = "/sys/devices/system/cpu/perf";
    if Path::new(pl_base).exists() {
        zeshia("1", &format!("{}/gpu_pmu_enable", pl_base));
        zeshia("1", &format!("{}/fuel_gauge_enable", pl_base));
        zeshia("1", &format!("{}/enable", pl_base));
        zeshia("1", &format!("{}/charger_enable", pl_base));
    }

    zeshia("40", "/proc/sys/vm/vfs_cache_pressure");
    zeshia("3", "/proc/sys/vm/drop_caches");
    zeshia("N", "/sys/module/workqueue/parameters/power_efficient");
    zeshia("N", "/sys/module/workqueue/parameters/disable_numa");
    zeshia("0", "/sys/kernel/eara_thermal/enable");
    zeshia("0", "/sys/devices/system/cpu/eas/enable");
    zeshia("1", "/sys/devices/system/cpu/cpu2/online");
    zeshia("1", "/sys/devices/system/cpu/cpu3/online");

    if let Ok(paths) = glob::glob("/dev/stune/*") {
        for path in paths.flatten() {
            if path.is_dir() {
                let p_str = path.to_str().unwrap();
                zeshia("30", &format!("{}/schedtune.boost", p_str));
                zeshia("1", &format!("{}/schedtune.sched_boost_enabled", p_str));
                zeshia("0", &format!("{}/schedtune.prefer_idle", p_str));
                zeshia("0", &format!("{}/schedtune.colocate", p_str));
            }
        }
    }

    zeshia("1", "/proc/sys/kernel/perf_cpu_time_max_percent");
    zeshia("1", "/proc/sys/kernel/sched_energy_aware");

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpu*") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            zeshia("0", &format!("{}/core_ctl/enable", p_str));
            zeshia("0", &format!("{}/core_ctl/core_ctl_boost", p_str));
        }
    }

    let bs_path = "/sys/module/battery_saver/parameters/enabled";
    if Path::new(bs_path).exists() {
        let content = fs::read_to_string(bs_path).unwrap_or_default();
        if content.chars().any(|c: char| c.is_ascii_digit()) {
            zeshia("0", bs_path);
        } else {
            zeshia("N", bs_path);
        }
    }

    zeshia("0", "/proc/sys/kernel/split_lock_mitigate");

    let sched_feat = "/sys/kernel/debug/sched_features";
    if Path::new(sched_feat).exists() {
        zeshia("NEXT_BUDDY", sched_feat);
        zeshia("NO_TTWU_QUEUE", sched_feat);
    }

    if get_clearapps() {
        clear_background_apps();
    }

    if !lite_mode {
        match getprop("persist.sys.azenithdebug.soctype").as_str() {
            "1" => mediatek_performance(),
            "2" => snapdragon_performance(),
            "3" => exynos_performance(),
            "4" => unisoc_performance(),
            "5" => tensor_performance(),
            _ => {}
        }
    }

    az_log("Performance Profile Applied Successfully!");
}

pub fn balanced_profile() {
    let mut default_gov = getprop("persist.sys.azenith.custom_default_cpu_gov");
    if default_gov.is_empty() {
        default_gov = getprop("persist.sys.azenith.default_cpu_gov");
    }
    if default_gov.is_empty() {
        default_gov = "schedutil".to_string();
    }
    setgov(&default_gov);
    dlog(&format!("Applying governor to : {}", default_gov));

    let mut default_io = getprop("persist.sys.azenith.custom_default_balanced_IO");
    if default_io.is_empty() {
        default_io = getprop("persist.sys.azenith.default_balanced_IO");
    }
    if default_io.is_empty() {
        default_io = "none".to_string();
    }
    sets_io(&default_io);
    dlog(&format!("Applying I/O scheduler to : {}", default_io));

    if Path::new("/proc/ppm").exists() {
        setfreqppm();
    } else {
        setfreq();
    }

    if getprop("persist.sys.azenithconf.freqoffset") == "Disabled" {
        dlog("Set CPU freq to normal Frequencies");
    } else {
        dlog("Set CPU freq to normal selected Frequencies");
    }

    let pl_base = "/sys/devices/system/cpu/perf";
    if Path::new(pl_base).exists() {
        zeshia("0", &format!("{}/gpu_pmu_enable", pl_base));
        zeshia("0", &format!("{}/fuel_gauge_enable", pl_base));
        zeshia("0", &format!("{}/enable", pl_base));
        zeshia("1", &format!("{}/charger_enable", pl_base));
    }

    zeshia("120", "/proc/sys/vm/vfs_cache_pressure");
    zeshia("Y", "/sys/module/workqueue/parameters/power_efficient");
    zeshia("Y", "/sys/module/workqueue/parameters/disable_numa");
    zeshia("1", "/sys/kernel/eara_thermal/enable");
    zeshia("1", "/sys/devices/system/cpu/eas/enable");

    if let Ok(paths) = glob::glob("/dev/stune/*") {
        for path in paths.flatten() {
            if path.is_dir() {
                let p_str = path.to_str().unwrap();
                zeshia("0", &format!("{}/schedtune.boost", p_str));
                zeshia("0", &format!("{}/schedtune.sched_boost_enabled", p_str));
                zeshia("0", &format!("{}/schedtune.prefer_idle", p_str));
                zeshia("0", &format!("{}/schedtune.colocate", p_str));
            }
        }
    }

    zeshia("100", "/proc/sys/kernel/perf_cpu_time_max_percent");
    zeshia("2", "/proc/sys/kernel/perf_cpu_time_max_percent");
    zeshia("1", "/proc/sys/kernel/sched_energy_aware");

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpu*") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            zeshia("0", &format!("{}/core_ctl/enable", p_str));
            zeshia("0", &format!("{}/core_ctl/core_ctl_boost", p_str));
        }
    }

    let bs_path = "/sys/module/battery_saver/parameters/enabled";
    if Path::new(bs_path).exists() {
        let content = fs::read_to_string(bs_path).unwrap_or_default();
        if content.chars().any(|c: char| c.is_ascii_digit()) {
            zeshia("0", bs_path);
        } else {
            zeshia("N", bs_path);
        }
    }

    zeshia("1", "/proc/sys/kernel/split_lock_mitigate");

    let sched_feat = "/sys/kernel/debug/sched_features";
    if Path::new(sched_feat).exists() {
        zeshia("NEXT_BUDDY", sched_feat);
        zeshia("TTWU_QUEUE", sched_feat);
    }

    match getprop("persist.sys.azenithdebug.soctype").as_str() {
        "1" => mediatek_balance(),
        "2" => snapdragon_balance(),
        "3" => exynos_balance(),
        "4" => unisoc_balance(),
        "5" => tensor_balance(),
        _ => {}
    }

    az_log("Balanced Profile applied successfully!");
}

pub fn eco_mode() {
    let mut powersave_gov = getprop("persist.sys.azenith.custom_powersave_cpu_gov");
    if powersave_gov.is_empty() {
        powersave_gov = "powersave".to_string();
    }
    setgov(&powersave_gov);
    dlog(&format!("Applying governor to : {}", powersave_gov));

    let mut powersave_io = getprop("persist.sys.azenith.custom_powersave_IO");
    if powersave_io.is_empty() {
        powersave_io = "none".to_string();
    }
    sets_io(&powersave_io);
    dlog(&format!("Applying I/O scheduler to : {}", powersave_io));

    if Path::new("/proc/ppm").exists() {
        setfreqppm();
    } else {
        setfreq();
    }
    dlog("Set CPU freq to low Frequencies");

    let pl_base = "/sys/devices/system/cpu/perf";
    if Path::new(pl_base).exists() {
        zeshia("0", &format!("{}/gpu_pmu_enable", pl_base));
        zeshia("0", &format!("{}/fuel_gauge_enable", pl_base));
        zeshia("0", &format!("{}/enable", pl_base));
        zeshia("1", &format!("{}/charger_enable", pl_base));
    }

    zeshia("120", "/proc/sys/vm/vfs_cache_pressure");
    zeshia("Y", "/sys/module/workqueue/parameters/power_efficient");
    zeshia("Y", "/sys/module/workqueue/parameters/disable_numa");
    zeshia("1", "/sys/kernel/eara_thermal/enable");
    zeshia("1", "/sys/devices/system/cpu/eas/enable");

    if let Ok(paths) = glob::glob("/dev/stune/*") {
        for path in paths.flatten() {
            if path.is_dir() {
                let p_str = path.to_str().unwrap();
                zeshia("0", &format!("{}/schedtune.boost", p_str));
                zeshia("0", &format!("{}/schedtune.sched_boost_enabled", p_str));
                zeshia("0", &format!("{}/schedtune.prefer_idle", p_str));
                zeshia("0", &format!("{}/schedtune.colocate", p_str));
            }
        }
    }

    zeshia("50", "/proc/sys/kernel/perf_cpu_time_max_percent");
    zeshia("0", "/proc/sys/kernel/perf_cpu_time_max_percent");
    zeshia("0", "/proc/sys/kernel/sched_energy_aware");

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpu*") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            zeshia("0", &format!("{}/core_ctl/enable", p_str));
            zeshia("0", &format!("{}/core_ctl/core_ctl_boost", p_str));
        }
    }

    let bs_path = "/sys/module/battery_saver/parameters/enabled";
    if Path::new(bs_path).exists() {
        let content = fs::read_to_string(bs_path).unwrap_or_default();
        if content.chars().any(|c: char| c.is_ascii_digit()) {
            zeshia("1", bs_path);
        } else {
            zeshia("Y", bs_path);
        }
    }

    zeshia("1", "/proc/sys/kernel/split_lock_mitigate");

    let sched_feat = "/sys/kernel/debug/sched_features";
    if Path::new(sched_feat).exists() {
        zeshia("NO_NEXT_BUDDY", sched_feat);
        zeshia("NO_TTWU_QUEUE", sched_feat);
    }

    match getprop("persist.sys.azenithdebug.soctype").as_str() {
        "1" => mediatek_powersave(),
        "2" => snapdragon_powersave(),
        "3" => exynos_powersave(),
        "4" => unisoc_powersave(),
        "5" => tensor_powersave(),
        _ => {}
    }

    az_log("ECO Mode applied successfully!");
}

pub fn initialize() {
    for param in &["panic", "panic_on_warn", "panic_on_oops", "softlockup_panic"] {
        zeshia("0", &format!("/proc/sys/kernel/{}", param));
    }

    let _ = Command::new("sync").status();

    let cpu_path = "/sys/devices/system/cpu/cpu0/cpufreq";
    let gov_file = format!("{}/scaling_governor", cpu_path);
    chmod(&gov_file, 0o644);

    let mut default_gov = fs::read_to_string(&gov_file)
        .unwrap_or_default()
        .trim()
        .to_string();

    setprop_cmd("persist.sys.azenith.default_cpu_gov", &default_gov);
    dlog(&format!("Default CPU governor detected: {}", default_gov));

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

    let custom_gov = getprop("persist.sys.azenith.custom_default_cpu_gov");
    if !custom_gov.is_empty() {
        default_gov = custom_gov;
    }
    dlog(&format!("Using CPU governor: {}", default_gov));

    setgov(&default_gov);

    if getprop("persist.sys.azenith.custom_powersave_cpu_gov").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_powersave_cpu_gov", &default_gov);
    }
    dlog("Parsing CPU Governor complete");

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

    let mut default_io = String::new();
    let sched_content = fs::read_to_string(&sched_file).unwrap_or_default();
    if let Some(start) = sched_content.find('[') {
        if let Some(end) = sched_content[start..].find(']') {
            default_io = sched_content[start + 1..start + end].to_string();
        }
    }

    setprop_cmd("persist.sys.azenith.default_balanced_IO", &default_io);
    dlog(&format!("Default IO Scheduler detected: {}", default_io));

    let custom_io = getprop("persist.sys.azenith.custom_default_balanced_IO");
    if !custom_io.is_empty() {
        default_io = custom_io;
    }

    sets_io(&default_io);

    if getprop("persist.sys.azenith.custom_powersave_IO").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_powersave_IO", &default_io);
    }
    if getprop("persist.sys.azenith.custom_performance_IO").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_performance_IO", &default_io);
    }
    dlog("Parsing IO Scheduler complete");

    let scheme = getprop("persist.sys.azenithconf.schemeconfig");
    if scheme != "1000 1000 1000 1000" && !scheme.is_empty() {
        let parts: Vec<&str> = scheme.split_whitespace().collect();
        if parts.len() >= 4 {
            let r = parts[0].parse::<f32>().unwrap_or(1000.0) / 1000.0;
            let g = parts[1].parse::<f32>().unwrap_or(1000.0) / 1000.0;
            let b = parts[2].parse::<f32>().unwrap_or(1000.0) / 1000.0;
            let s = parts[3].parse::<f32>().unwrap_or(1000.0) / 1000.0;

            let _ = Command::new("service").args([
                "call", "SurfaceFlinger", "1015", "i32", "1",
                "f", &r.to_string(), "f", "0", "f", "0", "f", "0",
                "f", "0", "f", &g.to_string(), "f", "0", "f", "0",
                "f", "0", "f", "0", "f", &b.to_string(), "f", "0",
                "f", "0", "f", "0", "f", "0", "f", "1"
            ]).status();

            let _ = Command::new("service").args([
                "call", "SurfaceFlinger", "1022", "f", &s.to_string()
            ]).status();
        }
    }

    let render = getprop("persist.sys.azenithconf.renderer");
    if !render.is_empty() && render != "Default" {
        let _ = Command::new("sys.azenith-utilityconf").args(["setrender", &render]).status();
    }

    let _ = Command::new("sync").status();
    az_log("Initializing Complete");
    dlog("Initializing Complete");
}
