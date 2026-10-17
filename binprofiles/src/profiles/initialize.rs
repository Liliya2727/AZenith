//! `initialize` — boot phase, runs once. Deliberately outside the profile taxonomy.

use crate::utils::*;
use std::fs;
use std::process::Command;

pub fn initialize() {
    // Initial kernel panics & sync
    for param in &["panic", "panic_on_warn", "panic_on_oops", "softlockup_panic"] {
        write_lock("0", &format!("/proc/sys/kernel/{}", param));
    }
    let _ = Command::new("sync").status();
    
    // Display / SurfaceFlinger config
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
    
    // Check if tweaks are disabled
    if is_tweak_disabled() {
        return;
    }

    // Initialize CPU & I/O & Mali GPU
    init_cpu_governor();
    init_io_scheduler();
    init_maligpu_governor();
    init_renderer();
    
    // Thermal governor
    if let Ok(paths) = glob::glob("/sys/class/thermal/thermal_zone*") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                write_lock("step_wise", &format!("{}/policy", p_str));
            }
        }
    }
    
    // I/O Tweaks
    if let Ok(paths) = glob::glob("/sys/block/*") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                write_lock("0", &format!("{}/queue/iostats", p_str));
                write_lock("0", &format!("{}/queue/add_random", p_str));
            }
        }
    }

    // Networking tweaks
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

    // General Kernel & Scheduler Tweaks
    write_lock("3", "/proc/sys/kernel/perf_cpu_time_max_percent");
    write_lock("0", "/proc/sys/kernel/sched_schedstats");
    write_lock("0", "/proc/sys/kernel/task_cpustats_enable");
    write_lock("0", "/proc/sys/kernel/sched_autogroup_enabled");
    write_lock("1", "/proc/sys/kernel/sched_child_runs_first");
    write_lock("32", "/proc/sys/kernel/sched_nr_migrate");
    write_lock("50000", "/proc/sys/kernel/sched_migration_cost_ns");
    write_lock("1000000", "/proc/sys/kernel/sched_min_granularity_ns");
    write_lock("1500000", "/proc/sys/kernel/sched_wakeup_granularity_ns");

    // VM Tweaks
    write_lock("0", "/proc/sys/vm/page-cluster");
    write_lock("15", "/proc/sys/vm/stat_interval");
    write_lock("0", "/proc/sys/vm/compaction_proactiveness");

    // Vendor Bloats & Module Tweaks
    write_lock("0", "/sys/module/mmc_core/parameters/use_spi_crc");
    write_lock("0", "/sys/module/opchain/parameters/chain_on");
    write_lock("0", "/sys/module/cpufreq_bouncing/parameters/enable");
    write_lock("0", "/proc/task_info/task_sched_info/task_sched_info_enable");
    write_lock("0", "/proc/oplus_scheduler/sched_assist/sched_assist_enabled");

    // Libraries Max Perf Reporting
    let libs = "libunity.so, libil2cpp.so, libmain.so, libUE4.so, libgodot_android.so, libgdx.so, libgdx-box2d.so, libminecraftpe.so, libLive2DCubismCore.so, libyuzu-android.so, libryujinx.so, libcitra-android.so, libhdr_pro_engine.so, libandroidx.graphics.path.so, libeffect.so";
    write_lock(libs, "/proc/sys/kernel/sched_lib_name");
    write_lock("255", "/proc/sys/kernel/sched_lib_mask_force");

    systemv("sys.azenith-utilityconf FSTrim");
    let _ = Command::new("sys.azenith-preferencedtweaks").status();
    
    // Final Sync & Logs
    let _ = Command::new("sync").status();
    log_debug("profiles", "init phase complete");
    log_info("profiles", "initializing complete");
}
