use crate::utils::*; use std::fs; use std::path::Path; use std::process::Command;
use crate::chipsets::mediatek::*;
use crate::chipsets::snapdragon::*;
use crate::chipsets::exynos::*;
use crate::chipsets::unisoc::*;
use crate::chipsets::tensor::*;

pub fn performance_profile() {
    let mut performance_gov = getprop("persist.sys.azenith.custom_performance_cpu_gov");
    if performance_gov.is_empty() {
        performance_gov = "powersave".to_string();
    }
    setgov(&performance_gov);
    log_info(&format!("Applying governor to : {}", performance_gov));
    
    let lite_mode = get_litemode();

    // I/O Scheduler Tweaks
    let custom_perf_io = getprop("persist.sys.azenith.custom_performance_IO");
    if !custom_perf_io.is_empty() {
        sets_io(&custom_perf_io);
        log_info(&format!("Applying I/O scheduler to : {}", custom_perf_io));
    } else {
        let mut default_io = getprop("persist.sys.azenith.custom_default_balanced_IO");
        if default_io.is_empty() {
            default_io = getprop("persist.sys.azenith.default_balanced_IO");
        }
        if default_io.is_empty() {
            default_io = "none".to_string();
        }
        sets_io(&default_io);
        log_info(&format!("Applying I/O scheduler to : {}", default_io));
    }

    // Mali GPU Governor Tweaks
    let custom_perf_mali = getprop("persist.sys.azenith.custom_performance_gpu_gov");
    if !custom_perf_mali.is_empty() {
        sets_mali_gov(&custom_perf_mali);
    } else {
        let mut default_mali = getprop("persist.sys.azenith.custom_default_gpu_gov");
        if default_mali.is_empty() {
            default_mali = getprop("persist.sys.azenith.default_gpu_gov");
        }
        if !default_mali.is_empty() {
            sets_mali_gov(&default_mali);
        }
    }

    if Path::new("/proc/ppm").exists() {
        setgamefreqppm();
    } else {
        setgamefreq();
    }

    if !lite_mode {
        log_info("Set CPU freq to max available Frequencies");
    } else {
        log_info("Set CPU freq to normal Frequencies");
    }

    let pl_base = "/sys/devices/system/cpu/perf";
    if Path::new(pl_base).exists() {
        write_lock("1", &format!("{}/gpu_pmu_enable", pl_base));
        write_lock("1", &format!("{}/fuel_gauge_enable", pl_base));
        write_lock("1", &format!("{}/enable", pl_base));
        write_lock("1", &format!("{}/charger_enable", pl_base));
    }

    write_lock("80", "/proc/sys/vm/vfs_cache_pressure");
    write_lock("3", "/proc/sys/vm/drop_caches");
    write_lock("N", "/sys/module/workqueue/parameters/power_efficient");
    write_lock("0", "/sys/devices/system/cpu/eas/enable");

    if let Ok(paths) = glob::glob("/dev/stune/*") {
        for path in paths.flatten() {
            if path.is_dir() {
                let p_str = path.to_str().unwrap();
                write_lock("30", &format!("{}/schedtune.boost", p_str));
                write_lock("1", &format!("{}/schedtune.sched_boost_enabled", p_str));
                write_lock("0", &format!("{}/schedtune.prefer_idle", p_str));
                write_lock("0", &format!("{}/schedtune.colocate", p_str));
            }
        }
    }

    let bs_path = "/sys/module/battery_saver/parameters/enabled";
    if Path::new(bs_path).exists() {
        let content = fs::read_to_string(bs_path).unwrap_or_default();
        if content.chars().any(|c: char| c.is_ascii_digit()) {
            write_lock("0", bs_path);
        } else {
            write_lock("N", bs_path);
        }
    }

    write_lock("0", "/proc/sys/kernel/split_lock_mitigate");

    let sched_feat = "/sys/kernel/debug/sched_features";
    if Path::new(sched_feat).exists() {
        write_lock("NEXT_BUDDY", sched_feat);
        write_lock("NO_TTWU_QUEUE", sched_feat);
    }
    
    // I/O Tweaks
    std::thread::spawn(|| {
        if let Ok(paths) = glob::glob("/sys/block/*") {
            for path in paths.flatten() {
                if let Some(file_name) = path.file_name().and_then(|n| n.to_str()) {
                    if file_name == "mmcblk0" || file_name == "mmcblk1" || file_name.starts_with("sd") {
                        if let Some(p_str) = path.to_str() {
                            write_lock("32", &format!("{}/queue/read_ahead_kb", p_str));
                            write_lock("32", &format!("{}/queue/nr_requests", p_str));
                        }
                    }
                }
            }
        }
    });

    if get_clearapps() {
        clear_background_apps();
    }

    if !lite_mode {
        match getprop("persist.sys.azenith.soctype").as_str() {
            "1" => mediatek_performance(),
            "2" => snapdragon_performance(),
            "3" => exynos_performance(),
            "4" => unisoc_performance(),
            "5" => tensor_performance(),
            _ => {}
        }
    }

    log_verbose("Performance Profile Applied Successfully!");
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
    log_info(&format!("Applying governor to : {}", default_gov));

    // I/O Scheduler Tweaks
    let mut default_io = getprop("persist.sys.azenith.custom_default_balanced_IO");
    if default_io.is_empty() {
        default_io = getprop("persist.sys.azenith.default_balanced_IO");
    }
    if default_io.is_empty() {
        default_io = "none".to_string();
    }
    sets_io(&default_io);
    log_info(&format!("Applying I/O scheduler to : {}", default_io));

    // Mali GPU Governor Tweaks
    let mut default_mali = getprop("persist.sys.azenith.custom_default_gpu_gov");
    if default_mali.is_empty() {
        default_mali = getprop("persist.sys.azenith.default_gpu_gov");
    }
    if !default_mali.is_empty() {
        sets_mali_gov(&default_mali);
    }

    if Path::new("/proc/ppm").exists() {
        setfreqppm();
    } else {
        setfreq();
    }

    if getprop("persist.sys.azenithconf.freqoffset") == "Disabled" {
        log_info("Set CPU freq to normal Frequencies");
    } else {
        log_info("Set CPU freq to normal selected Frequencies");
    }

    let pl_base = "/sys/devices/system/cpu/perf";
    if Path::new(pl_base).exists() {
        write_lock("0", &format!("{}/gpu_pmu_enable", pl_base));
        write_lock("0", &format!("{}/fuel_gauge_enable", pl_base));
        write_lock("0", &format!("{}/enable", pl_base));
        write_lock("1", &format!("{}/charger_enable", pl_base));
    }

    write_lock("120", "/proc/sys/vm/vfs_cache_pressure");
    write_lock("Y", "/sys/module/workqueue/parameters/power_efficient");
    write_lock("1", "/sys/devices/system/cpu/eas/enable");

    if let Ok(paths) = glob::glob("/dev/stune/*") {
        for path in paths.flatten() {
            if path.is_dir() {
                let p_str = path.to_str().unwrap();
                write_lock("0", &format!("{}/schedtune.boost", p_str));
                write_lock("0", &format!("{}/schedtune.sched_boost_enabled", p_str));
                write_lock("0", &format!("{}/schedtune.prefer_idle", p_str));
                write_lock("0", &format!("{}/schedtune.colocate", p_str));
            }
        }
    }

    let bs_path = "/sys/module/battery_saver/parameters/enabled";
    if Path::new(bs_path).exists() {
        let content = fs::read_to_string(bs_path).unwrap_or_default();
        if content.chars().any(|c: char| c.is_ascii_digit()) {
            write_lock("0", bs_path);
        } else {
            write_lock("N", bs_path);
        }
    }

    write_lock("1", "/proc/sys/kernel/split_lock_mitigate");

    let sched_feat = "/sys/kernel/debug/sched_features";
    if Path::new(sched_feat).exists() {
        write_lock("NEXT_BUDDY", sched_feat);
        write_lock("TTWU_QUEUE", sched_feat);
    }
    
    // I/O Tweaks
    std::thread::spawn(|| {
        if let Ok(paths) = glob::glob("/sys/block/*") {
            for path in paths.flatten() {
                if let Some(file_name) = path.file_name().and_then(|n| n.to_str()) {
                    if file_name == "mmcblk0" || file_name == "mmcblk1" || file_name.starts_with("sd") {
                        if let Some(p_str) = path.to_str() {
                            write_lock("128", &format!("{}/queue/read_ahead_kb", p_str));
                            write_lock("64", &format!("{}/queue/nr_requests", p_str));
                        }
                    }
                }
            }
        }
    });

    match getprop("persist.sys.azenith.soctype").as_str() {
        "1" => mediatek_balance(),
        "2" => snapdragon_balance(),
        "3" => exynos_balance(),
        "4" => unisoc_balance(),
        "5" => tensor_balance(),
        _ => {}
    }

    log_verbose("Balanced Profile applied successfully!");
}

pub fn eco_mode() {
    let mut powersave_gov = getprop("persist.sys.azenith.custom_powersave_cpu_gov");
    if powersave_gov.is_empty() {
        powersave_gov = "powersave".to_string();
    }
    setgov(&powersave_gov);
    log_info(&format!("Applying governor to : {}", powersave_gov));

    // I/O Scheduler Tweaks
    let mut powersave_io = getprop("persist.sys.azenith.custom_powersave_IO");
    if powersave_io.is_empty() {
        powersave_io = "none".to_string();
    }
    sets_io(&powersave_io);
    log_info(&format!("Applying I/O scheduler to : {}", powersave_io));

    // Mali GPU Governor Tweaks
    let custom_eco_mali = getprop("persist.sys.azenith.custom_powersave_gpu_gov");
    if !custom_eco_mali.is_empty() {
        sets_mali_gov(&custom_eco_mali);
    } else {
        let mut default_mali = getprop("persist.sys.azenith.custom_default_gpu_gov");
        if default_mali.is_empty() {
            default_mali = getprop("persist.sys.azenith.default_gpu_gov");
        }
        if !default_mali.is_empty() {
            sets_mali_gov(&default_mali);
        }
    }

    if Path::new("/proc/ppm").exists() {
        setfreqppm();
    } else {
        setfreq();
    }
    log_info("Set CPU freq to low Frequencies");

    let pl_base = "/sys/devices/system/cpu/perf";
    if Path::new(pl_base).exists() {
        write_lock("0", &format!("{}/gpu_pmu_enable", pl_base));
        write_lock("0", &format!("{}/fuel_gauge_enable", pl_base));
        write_lock("0", &format!("{}/enable", pl_base));
        write_lock("1", &format!("{}/charger_enable", pl_base));
    }

    write_lock("120", "/proc/sys/vm/vfs_cache_pressure");
    write_lock("Y", "/sys/module/workqueue/parameters/power_efficient");
    write_lock("1", "/sys/devices/system/cpu/eas/enable");

    if let Ok(paths) = glob::glob("/dev/stune/*") {
        for path in paths.flatten() {
            if path.is_dir() {
                let p_str = path.to_str().unwrap();
                write_lock("0", &format!("{}/schedtune.boost", p_str));
                write_lock("0", &format!("{}/schedtune.sched_boost_enabled", p_str));
                write_lock("0", &format!("{}/schedtune.prefer_idle", p_str));
                write_lock("0", &format!("{}/schedtune.colocate", p_str));
            }
        }
    }

    let bs_path = "/sys/module/battery_saver/parameters/enabled";
    if Path::new(bs_path).exists() {
        let content = fs::read_to_string(bs_path).unwrap_or_default();
        if content.chars().any(|c: char| c.is_ascii_digit()) {
            write_lock("1", bs_path);
        } else {
            write_lock("Y", bs_path);
        }
    }

    write_lock("1", "/proc/sys/kernel/split_lock_mitigate");

    let sched_feat = "/sys/kernel/debug/sched_features";
    if Path::new(sched_feat).exists() {
        write_lock("NO_NEXT_BUDDY", sched_feat);
        write_lock("NO_TTWU_QUEUE", sched_feat);
    }
    
    // Enable battery saver module
    let bs_path = "/sys/module/battery_saver/parameters/enabled";
    if Path::new(bs_path).exists() {
        let content = fs::read_to_string(bs_path).unwrap_or_default();
        if content.chars().any(|c| c.is_ascii_digit()) {
            write_lock("1", bs_path);
        } else {
            write_lock("Y", bs_path);
        }
    }

    match getprop("persist.sys.azenith.soctype").as_str() {
        "1" => mediatek_powersave(),
        "2" => snapdragon_powersave(),
        "3" => exynos_powersave(),
        "4" => unisoc_powersave(),
        "5" => tensor_powersave(),
        _ => {}
    }

    log_verbose("ECO Mode applied successfully!");
}

pub fn initialize() {
    // 1. Initial kernel panics & sync
    for param in &["panic", "panic_on_warn", "panic_on_oops", "softlockup_panic"] {
        write_lock("0", &format!("/proc/sys/kernel/{}", param));
    }
    let _ = Command::new("sync").status();

    // 2. Initialize CPU & I/O & Mali GPU
    init_cpu_governor();
    init_io_scheduler();
    init_maligpu_governor();

    // 3. Display / SurfaceFlinger config
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
    
    // 4. Thermal governor
    if let Ok(paths) = glob::glob("/sys/class/thermal/thermal_zone*") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                write_lock("step_wise", &format!("{}/policy", p_str));
            }
        }
    }
    
    // 5. I/O Tweaks
    if let Ok(paths) = glob::glob("/sys/block/*") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                write_lock("0", &format!("{}/queue/iostats", p_str));
                write_lock("0", &format!("{}/queue/add_random", p_str));
            }
        }
    }

    // 6. Networking tweaks
    let tcp_avail = fs::read_to_string("/proc/sys/net/ipv4/tcp_available_congestion_control").unwrap_or_default();
    let algos = ["bbr3", "bbr2", "bbrplus", "bbr", "westwood", "cubic"];
    for algo in algos.iter() {
        if tcp_avail.contains(algo) {
            write_lock(algo, "/proc/sys/net/ipv4/tcp_congestion_control");
            break;
        }
    }

    write_lock("1", "/proc/sys/net/ipv4/tcp_low_latency");
    write_lock("1", "/proc/sys/net/ipv4/tcp_ecn");
    write_lock("3", "/proc/sys/net/ipv4/tcp_fastopen");
    write_lock("1", "/proc/sys/net/ipv4/tcp_sack");
    write_lock("0", "/proc/sys/net/ipv4/tcp_timestamps");

    // 7. General Kernel & Scheduler Tweaks
    write_lock("3", "/proc/sys/kernel/perf_cpu_time_max_percent");
    write_lock("0", "/proc/sys/kernel/sched_schedstats");
    write_lock("0", "/proc/sys/kernel/task_cpustats_enable");
    write_lock("0", "/proc/sys/kernel/sched_autogroup_enabled");
    write_lock("1", "/proc/sys/kernel/sched_child_runs_first");
    write_lock("32", "/proc/sys/kernel/sched_nr_migrate");
    write_lock("50000", "/proc/sys/kernel/sched_migration_cost_ns");
    write_lock("1000000", "/proc/sys/kernel/sched_min_granularity_ns");
    write_lock("1500000", "/proc/sys/kernel/sched_wakeup_granularity_ns");

    // 8. VM Tweaks
    write_lock("0", "/proc/sys/vm/page-cluster");
    write_lock("15", "/proc/sys/vm/stat_interval");
    write_lock("0", "/proc/sys/vm/compaction_proactiveness");

    // 9. Vendor Bloats & Module Tweaks
    write_lock("0", "/sys/module/mmc_core/parameters/use_spi_crc");
    write_lock("0", "/sys/module/opchain/parameters/chain_on");
    write_lock("0", "/sys/module/cpufreq_bouncing/parameters/enable");
    write_lock("0", "/proc/task_info/task_sched_info/task_sched_info_enable");
    write_lock("0", "/proc/oplus_scheduler/sched_assist/sched_assist_enabled");

    // 10. Libraries Max Perf Reporting
    let libs = "libunity.so, libil2cpp.so, libmain.so, libUE4.so, libgodot_android.so, libgdx.so, libgdx-box2d.so, libminecraftpe.so, libLive2DCubismCore.so, libyuzu-android.so, libryujinx.so, libcitra-android.so, libhdr_pro_engine.so, libandroidx.graphics.path.so, libeffect.so";
    write_lock(libs, "/proc/sys/kernel/sched_lib_name");
    write_lock("255", "/proc/sys/kernel/sched_lib_mask_force");

    systemv("sys.azenith-utilityconf FSTrim");
    systemv("sh /data/adb/modules/AZenith/preferenced-tweaks.sh");
    
    // 11. Final Sync & Logs
    let _ = Command::new("sync").status();
    log_verbose("Initializing Complete");
    log_info("Initializing Complete");
}
