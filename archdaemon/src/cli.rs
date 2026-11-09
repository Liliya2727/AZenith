//! CLI command handlers.

use crate::core::BypassNode;
use crate::core::{
    LogLevel, ProfileMode, PROFILE_MODE,
    RR_MODES_PATH,
};
use crate::io::check_running_state;
use crate::log::{external_log, external_vlog};
use crate::{log_error, log_fatal, log_info, log_warn};
use crate::shell::systemv;
use crate::sysprop::{get_or_default, set};
use crate::fileops;
use crate::util::notify;

use std::os::raw::c_int;

// ---------------------------------------------------------------------------
// handle_profile
// ---------------------------------------------------------------------------
pub fn handle_profile(argc: i32, argv: &[&str]) -> c_int {
    if argc < 3 || argv[2].is_empty() {
        eprintln!("ERROR: Missing profile number. Use --profile <1|2|3>");
        return 1;
    }

    let ai_state = get_or_default("persist.sys.azenithconf.AIenabled", "");

    if ai_state == "1" {
        eprintln!("ERROR: Auto Mode is enabled.\n       Manual profile selection is blocked.");
        return 1;
    }

    let profile = argv[2];

    match profile {
        "0" => {
            log_warn!("cli", "profile apply rejected number=0 reason=initialize-only");
            println!("WARN: Cannot Apply Profile 0 (Initialize)");
        }
        "1" => {
            log_info!("cli", "profile apply requested profile=performance source=cli");
            let lite_prop = get_or_default("persist.sys.azenithconf.cpulimit", "");
            if lite_prop == "1" {
                set("persist.sys.azenithconf.litemode", "1");
            } else {
                set("persist.sys.azenithconf.litemode", "0");
            }
            // Need DaemonContext for run_profiler - but this is CLI-only
            // For CLI, we call run_profiler with a temporary context
            let mut ctx = crate::core::DaemonContext::new();
            crate::profile::run_profiler(ProfileMode::PerformanceProfile as i32, &mut ctx);
            notify("Performance Profile", "System is now at Powerful state", false, 0, None);
            println!("Applying Performance Profile");
        }
        "2" => {
            log_info!("cli", "profile apply requested profile=balanced source=cli");
            let mut ctx = crate::core::DaemonContext::new();
            crate::profile::run_profiler(ProfileMode::BalancedProfile as i32, &mut ctx);
            notify("Balanced Profile", "System is now at Optimal state", false, 0, None);
            println!("Applying Balanced Profile");
        }
        "3" => {
            log_info!("cli", "profile apply requested profile=eco source=cli");
            let mut ctx = crate::core::DaemonContext::new();
            crate::profile::run_profiler(ProfileMode::EcoMode as i32, &mut ctx);
            notify("ECO Mode", "System is now at Endurance state", false, 0, None);
            println!("Applying Eco Mode");
        }
        _ => {
            eprintln!("Invalid profiles.");
            return 1;
        }
    }

    0
}

// ---------------------------------------------------------------------------
// handle_log
// ---------------------------------------------------------------------------
pub fn handle_log(argc: i32, argv: &[&str]) -> c_int {
    if argc < 5 {
        eprintln!("Usage: --log <TAG> <LEVEL> <MESSAGE>\nLevels: 0=DEBUG, 1=INFO, 2=WARN, 3=ERROR, 4=FATAL");
        return 1;
    }

    let tag = argv[2];
    let level_str = argv[3];

    let level: i32 = level_str.parse().unwrap_or(-1);
    if level < LogLevel::Debug as i32 || level > LogLevel::Fatal as i32 {
        eprintln!("ERROR: Invalid log level '{level_str}' (valid 0..4)");
        return 1;
    }

    let mut message = String::new();
    for (i, arg) in argv.iter().enumerate().skip(4) {
        if i > 4 {
            message.push(' ');
        }
        message.push_str(arg);
    }

    external_log(level, tag, &message, format_args!(""));
    0
}

// ---------------------------------------------------------------------------
// handle_verboselog
// ---------------------------------------------------------------------------
pub fn handle_verboselog(argc: i32, argv: &[&str]) -> c_int {
    if argc < 5 {
        eprintln!("Usage: --log <TAG> <LEVEL> <MESSAGE>\nLevels: 0=DEBUG, 1=INFO, 2=WARN, 3=ERROR, 4=FATAL");
        return 1;
    }

    let tag = argv[2];
    let level_str = argv[3];

    let level: i32 = level_str.parse().unwrap_or(-1);
    if level < LogLevel::Debug as i32 || level > LogLevel::Fatal as i32 {
        eprintln!("ERROR: Invalid log level '{level_str}' (valid 0..4)");
        return 1;
    }

    let mut message = String::new();
    for (i, arg) in argv.iter().enumerate().skip(4) {
        if i > 4 {
            message.push(' ');
        }
        message.push_str(arg);
    }

    external_vlog(level, tag, &message, format_args!(""));
    0
}

// ---------------------------------------------------------------------------
// printversion
// ---------------------------------------------------------------------------
pub fn printversion() {
    println!("{}", crate::module::full_version());
}

// ---------------------------------------------------------------------------
// print_help
// ---------------------------------------------------------------------------
pub fn print_help() {
    let ver = crate::module::full_version();
    println!("AZenith Daemon CLI (by @Zexshia)");
    println!("Version: {}", ver);
    println!();
    println!("Usage: sys.azenith-service [options]");
    println!();
    println!("Options:");
    println!("     -r,    --run              Start AZenith daemon service");
    println!();
    println!("     -p,    --profile <1|2|3>  Apply AZenith profiles via CLI");
    println!("                               1 : Performance");
    println!("                               2 : Balanced");
    println!("                               3 : Eco Mode");
    println!();
    println!("     -l,    --log <TAG> <LVL> <MSG>");
    println!("                               Write a log message via AZenith logging service");
    println!("                               LEVELs: 0=DEBUG, 1=INFO, 2=WARN, 3=ERROR, 4=FATAL");
    println!();
    println!("     -vl,   --verboselog <TAG> <LVL> <MSG>");
    println!("                               Write a verbose log message via AZenith logging service");
    println!();
    println!("     -actv, --appactivity      Open AZenith App Main Activity");
    println!();
    println!("     -cbc,  --checkbypasschg   Check bypass charge compatibility");
    println!("     -tb,   --testbypass <NODE> Test specific bypass node for 10 seconds");
    println!("                                (or --testbypass --path=NODE_NAME)");
    println!();
    println!("     -bpl,  --bypasspathlist   Show all embedded bypass charging paths");
    println!();
    println!("     -V,    --version          Show AZenith current version");
    println!();
    println!("     -h,    --help             Display this help message and exit");
    println!();
    println!("Examples:");
    println!("     sys.azenith-service --run");
    println!("     sys.azenith-service --profile 2");
    println!("     sys.azenith-service --bypasspathlist");
    println!("     sys.azenith-service --help");
}

// ---------------------------------------------------------------------------
// check_bypass_compatibility
// ---------------------------------------------------------------------------
pub fn check_bypass_compatibility() -> i32 {
    println!("\u{1b}[36m[Bypass Charge Compatibility Check]\u{1b}[0m Initializing...");

    std::thread::sleep(std::time::Duration::from_secs(5));

    if crate::bypass::is_charging() == 0 {
        println!("\u{1b}[33m[!] WARNING:\u{1b}[0m Charger not detected. Plug in first!");
        log_warn!("bypass", "compatibility check aborted reason=charger_disconnected");
        return -1;
    }

    let total_nodes = crate::bypass::BYPASS_LIST.len();

    let mut skipped_count = 0;
    let mut tested_count = 0;

    for node in &crate::bypass::BYPASS_LIST {
        let c_path = match std::ffi::CString::new(node.path) {
            Ok(path) => path,
            Err(_) => { skipped_count += 1; continue; }
        };
        if unsafe { libc::access(c_path.as_ptr(), 0) } != 0 {
            println!("\u{1b}[90m[-] Node {:<25}: Not Found, skipping...\u{1b}[0m", node.name);
            skipped_count += 1;
            continue;
        }
        tested_count += 1;
        println!("\n\u{1b}[1;32m[+]\u{1b}[0m Testing Node ({}/{}): \u{1b}[1;37m{}\u{1b}[0m", tested_count + skipped_count, total_nodes, node.name);
        if crate::bypass::echo_to_file(node.path, node.on_val, 0) != 0 {
            let _ = crate::bypass::echo_to_file(node.path, node.off_val, 0);
            log_warn!("bypass", "node test skipped after write failure name={} path={}", node.name, node.path);
            skipped_count += 1;
            tested_count -= 1;
            continue;
        }

        let mut last_ma = 9999;
        for sec in 1..=10 {
            std::thread::sleep(std::time::Duration::from_secs(1));
            last_ma = crate::bypass::read_current_ma();
            println!("    Checking current ({sec}s/10s): \u{1b}[33m{} mA\u{1b}[0m", last_ma);
        }

        crate::bypass::echo_to_file(node.path, node.off_val, 0);

        if last_ma < 50 {
            println!("\n\u{1b}[1;32m[SUCCESS]\u{1b}[0m Found working node: \u{1b}[1m{}\u{1b}[0m", node.name);
            println!("\u{1b}[32m[INFO]\u{1b}[0m Process finished. {} nodes skipped.", skipped_count);

            log_info!("bypass", "compatible node found name={} skipped={}", node.name, skipped_count);

            return 0;
        } else {
            println!("\u{1b}[31m[FAILED]\u{1b}[0m Current drop test failed for {} ({} mA)", node.name, last_ma);
            println!("------------------------------------------");
            std::thread::sleep(std::time::Duration::from_millis(300));
        }
    }

    println!("\n\u{1b}[1;31m[-]\u{1b}[0m Final Result: No compatible bypass node found.");
    println!("\u{1b}[33m[INFO]\u{1b}[0m Summary: {} Scanned, {} Tested, {} Skipped.\u{1b}[0m", total_nodes, tested_count, skipped_count);

    set("persist.sys.azenithconf.bypasspath", "UNSUPPORTED");
    set("persist.sys.azenithconf.bypasschg", "0");
    set("persist.sys.azenithconf.bypasschgthreshold", "20");
    fileops::write_string_best_effort(&format!("{}/bypasspath", crate::core::BYPASSCHG_CONFIG), "UNSUPPORTED");
    fileops::write_string_best_effort(&format!("{}/bypasschg", crate::core::BYPASSCHG_CONFIG), "0");
    fileops::write_string_best_effort(&format!("{}/bypasschgthreshold", crate::core::BYPASSCHG_CONFIG), "20");
    1
}

// ---------------------------------------------------------------------------
// test_bypass_node_by_name
// ---------------------------------------------------------------------------
pub fn test_bypass_node_by_name(name: &str) -> i32 {
    if name.is_empty() {
        eprintln!("\u{1b}[31m[ERROR]\u{1b}[0m No bypass node name provided.");
        return 1;
    }

    for node in &crate::bypass::BYPASS_LIST {
        if node.name == name {
            return test_single_node(node);
        }
    }

    eprintln!("\u{1b}[31m[ERROR]\u{1b}[0m Bypass node not found: {name}");
    println!("\u{1b}[33m[INFO]\u{1b}[0m Use --bypasspathlist to see available nodes.");
    2
}

fn test_single_node(node: &BypassNode) -> i32 {
    println!("\n\u{1b}[36m[Bypass Charge Test - Single Node]\u{1b}[0m Initializing...");

    std::thread::sleep(std::time::Duration::from_secs(2));

    if crate::bypass::is_charging() == 0 {
        println!("\u{1b}[33m[!] WARNING:\u{1b}[0m Charger not detected. Plug in first!");
        log_warn!("bypass", "single node test aborted reason=charger_disconnected");
        return -1;
    }

    // Check if path exists using access
    let c_path = std::ffi::CString::new(node.path).unwrap();
    if unsafe { libc::access(c_path.as_ptr(), 0) } != 0 {
        println!("\u{1b}[31m[ERROR]\u{1b}[0m Node path not found: {}", node.path);
        return 2;
    }

    println!("\n\u{1b}[1;32m[+]\u{1b}[0m Testing Node: \u{1b}[1;37m{}\u{1b}[0m", node.name);
    println!("    Path: {}", node.path);

    crate::bypass::echo_to_file(node.path, node.on_val, 0);

    let mut last_ma = 9999;
    for sec in 1..=10 {
        std::thread::sleep(std::time::Duration::from_secs(1));
        last_ma = crate::bypass::read_current_ma();
        println!("    Checking current ({sec}s/10s): \u{1b}[33m{} mA\u{1b}[0m", last_ma);
    }

    crate::bypass::echo_to_file(node.path, node.off_val, 0);

    if last_ma < 50 {
        println!("\n\u{1b}[1;32m[SUCCESS]\u{1b}[0m Node is compatible: \u{1b}[1m{}\u{1b}[0m (Last: {} mA)", node.name, last_ma);
        log_info!("bypass", "single node test success name={} last_ma={}", node.name, last_ma);
        return 0;
    } else {
        println!("\n\u{1b}[31m[FAILED]\u{1b}[0m Node not compatible: {} (Last: {} mA)", node.name, last_ma);
        log_warn!("bypass", "single node test failed name={} last_ma={}", node.name, last_ma);
        return 1;
    }
}

// ---------------------------------------------------------------------------
// print_bypass_path_list
// ---------------------------------------------------------------------------
pub fn print_bypass_path_list() {
    println!("\n\u{1b}[36m[AZenith Available Bypass Path List]\u{1b}[0m");
    println!("---------------------------------------------------------------------------------------");
    println!(" {:<30} | {:<10} | {}", "NODE NAME", "STATUS", "SYSFS/PROC PATH");
    println!("---------------------------------------------------------------------------------------");

    let mut found_count = 0;

    for node in &crate::bypass::BYPASS_LIST {
        let c_path = std::ffi::CString::new(node.path).unwrap();
        let found = unsafe { libc::access(c_path.as_ptr(), 0) } == 0;
        if found {
            println!(" \u{1b}[1;32m{:<30}\u{1b}[0m | \u{1b}[32m[FOUND]\u{1b}[0m    | {}", node.name, node.path);
            found_count += 1;
        } else {
            println!(" \u{1b}[90m{:<30} | [NOT FOUND] | {}\u{1b}[0m", node.name, node.path);
        }
    }

    println!("---------------------------------------------------------------------------------------");
    println!("\u{1b}[36m[SUMMARY]\u{1b}[0m Total Nodes: {} | Available on this device: \u{1b}[1;32m{}\u{1b}[0m\n", crate::bypass::BYPASS_LIST.len(), found_count);
}

// ---------------------------------------------------------------------------
// require_daemon_running
// ---------------------------------------------------------------------------
pub fn require_daemon_running() -> bool {
    if !check_running_state() {
        eprintln!("\u{1b}[31mERROR:\u{1b}[0m AZenith daemon is not running.\nRun: sys.azenith-service --run");
        return false;
    }
    true
}

// ---------------------------------------------------------------------------
// clearlogs
// ---------------------------------------------------------------------------
pub fn clearlogs() {
    // Remove debug log files matching AZenith*.*
    if let Ok(entries) = std::fs::read_dir("/data/adb/.config/AZenith/debug") {
        for entry in entries.flatten() {
            let path = entry.path();
            if let Some(name) = path.file_name().and_then(|n| n.to_str()) {
                if name.starts_with("AZenith") && name.contains('.') {
                    let _ = std::fs::remove_file(&path);
                }
            }
        }
    }
    // Remove sysmon files
    if let Ok(entries) = std::fs::read_dir("/data/adb/.config/AZenith") {
        for entry in entries.flatten() {
            let path = entry.path();
            if let Some(name) = path.file_name().and_then(|n| n.to_str()) {
                if name.starts_with("sysmon.") {
                    let _ = std::fs::remove_file(&path);
                }
            }
        }
    }
    log_info!("cli", "cleared all log files and rotated generations");
    systemv("su -c \"am broadcast -a zx.azenith.ACTION_MANAGE -n zx.azenith/.receiver.ZenithReceiver --ez clearall true >/dev/null 2>&1\"");
}

// ---------------------------------------------------------------------------
// restart_service
// ---------------------------------------------------------------------------
pub fn restart_service() -> c_int {
    if unsafe { libc::daemon(0, 0) } != 0 {
        log_fatal!("restart", "daemonize failed errno={}", std::io::Error::last_os_error());
        return 1;
    }
    let status = systemv("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf restartservice");
    if status != 0 {
        log_error!("restart", "restart command failed exit={status}");
        return status.max(1);
    }
    0
}

// ---------------------------------------------------------------------------
// shownotifications
// ---------------------------------------------------------------------------
pub fn shownotifications() {
    let Ok(fp) = std::fs::File::open(PROFILE_MODE) else {
        return;
    };
    use std::io::BufRead;
    let profile: i32 = std::io::BufReader::new(fp).lines()
        .filter_map(|l| l.ok())
        .find_map(|l| l.trim().parse::<i32>().ok())
        .unwrap_or(0);

    match profile {
        1 => notify("Performance Profile", "System is now at Powerful state", false, 0, None),
        2 => notify("Balanced Profile", "System is now at Optimal state", false, 0, None),
        3 => notify("ECO Mode", "System is now at Endurance state", false, 0, None),
        _ => {}
    }
}

// ---------------------------------------------------------------------------
// hidenotifications
// ---------------------------------------------------------------------------
pub fn hidenotifications() {
    systemv("su -c \"am broadcast -a zx.azenith.ACTION_MANAGE -n zx.azenith/.receiver.ZenithReceiver --ez clearall true >/dev/null 2>&1\"");
}

// ---------------------------------------------------------------------------
// expose_telemetry_access
// ---------------------------------------------------------------------------
pub fn expose_telemetry_access() {
    let cpu_nodes = [
        "scaling_cur_freq", "cpuinfo_cur_freq",
        "cpuinfo_max_freq", "cpuinfo_min_freq",
        "scaling_max_freq", "scaling_min_freq",
    ];
    let gpu_nodes = [
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
        "/sys/class/kgsl/kgsl-3d0/gpubusy",
        "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
        "/sys/class/kgsl/kgsl-3d0/devfreq/max_freq",
        "/sys/kernel/gpu/gpu_busy",
        "/sys/kernel/gpu/gpu_clock",
        "/sys/kernel/gpu/gpu_max_clock",
        "/sys/module/ged/parameters/gpu_loading",
        "/sys/kernel/ged/hal/gpu_utilization",
        "/sys/kernel/ged/hal/ged_kpi",
        "/proc/mali/utilization",
        "/proc/gpufreq/gpufreq_var_dump",
        "/proc/gpufreqv2/gpufreq_status",
        "/sys/class/misc/mali0/device/utilization",
        "/sys/devices/platform/mali/utilization",
    ];
    let devfreq_nodes = [
        "cur_freq", "max_freq", "min_freq", "load", "utilization",
    ];

    fileops::create_dir_all_best_effort("/data/data/zx.azenith/API");
    fileops::create_dir_all_best_effort("/data/adb/.config/AZenith/API");
    chmod("/data/data/zx.azenith/API", 0o777);
    chmodg("/data/data/zx.azenith/API/*", 0o644);
    chmodg("/data/adb/.config/AZenith/API/*", 0o644);

    for node in &cpu_nodes {
        let path = format!("/sys/devices/system/cpu/cpu[0-9]*/cpufreq/{node}");
        chmodg(&path, 0o444);
    }
    chmod("/proc/stat", 0o444);

    chmod("/sys/kernel/ged", 0o755);
    chmod("/sys/kernel/ged/hal", 0o755);
    for node in &gpu_nodes {
        chmod(node, 0o444);
    }

    for node in &devfreq_nodes {
        let path = format!("/sys/class/devfreq/*/{node}");
        chmodg(&path, 0o444);
    }

    chmodg("/sys/class/thermal/thermal_zone*/temp", 0o444);

    log_info!("cli", "telemetry access exposed");
}

// ---------------------------------------------------------------------------
// grant_app_permissions
// ---------------------------------------------------------------------------
pub fn grant_app_permissions() {
    systemv("pm grant zx.azenith android.permission.READ_EXTERNAL_STORAGE >/dev/null 2>&1");
    systemv("pm grant zx.azenith android.permission.POST_NOTIFICATIONS >/dev/null 2>&1");
    systemv("pm grant zx.azenith android.permission.READ_MEDIA_IMAGES >/dev/null 2>&1");
    systemv("pm grant zx.azenith android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1");
    systemv("pm grant zx.azenith android.permission.PACKAGE_USAGE_STATS >/dev/null 2>&1");
    systemv("pm grant zx.azenith android.permission.BATTERY_STATS >/dev/null 2>&1");
    systemv("pm grant zx.azenith android.permission.DUMP >/dev/null 2>&1");

    systemv("appops set zx.azenith SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1");
    systemv("appops set zx.azenith WRITE_SETTINGS allow >/dev/null 2>&1");
    systemv("appops set zx.azenith GET_USAGE_STATS allow >/dev/null 2>&1");
    systemv("appops set zx.azenith RUN_IN_BACKGROUND allow >/dev/null 2>&1");
    systemv("appops set zx.azenith RUN_ANY_IN_BACKGROUND allow >/dev/null 2>&1");
    systemv("appops set zx.azenith ACCESS_RESTRICTED_SETTINGS allow >/dev/null 2>&1");

    systemv("pm grant zx.azenith android.permission.QUERY_ALL_PACKAGES >/dev/null 2>&1");
    systemv("pm grant zx.azenith com.android.permission.GET_INSTALLED_APPS >/dev/null 2>&1");
    systemv("appops set zx.azenith android:query_all_packages allow >/dev/null 2>&1");
    systemv("appops set zx.azenith query_all_packages allow >/dev/null 2>&1");
    systemv("appops set zx.azenith android:get_installed_apps allow >/dev/null 2>&1");
    systemv("appops set zx.azenith GET_INSTALLED_APPS allow >/dev/null 2>&1");
    systemv("appops set zx.azenith 10022 allow >/dev/null 2>&1");

    systemv("dumpsys deviceidle whitelist +zx.azenith >/dev/null 2>&1");

    log_info!("cli", "app permissions granted");
}

// ---------------------------------------------------------------------------
// dump_refresh_rate_modes
// ---------------------------------------------------------------------------
const RR_MODES_MAX: usize = 32;

#[derive(Clone, Copy)]
struct RrMode {
    id: i32,
    hz: i32,
}

pub fn dump_refresh_rate_modes() -> c_int {
    let sf_out = match crate::shell::execute_command("dumpsys SurfaceFlinger") {
        Some(o) => o,
        None => {
            log_error!("cli", "rr_modes popen failed");
            return 1;
        }
    };
    let mut modes: Vec<RrMode> = Vec::new();

    // Pattern 1: "id=X, ... vsyncRate=Y" within same {...} block
    for line in sf_out.lines() {
        let mut search_from = line;
        while let Some(pos) = search_from.find("id=") {
            if pos > 0 {
                let prev = search_from.as_bytes()[pos - 1];
                if prev.is_ascii_alphanumeric() {
                    search_from = &search_from[pos + 3..];
                    continue;
                }
            }

            let after_id = &search_from[pos + 3..];
            let id_end = match after_id.find(',') {
                Some(e) => e,
                None => {
                    search_from = &search_from[pos + 3..];
                    continue;
                }
            };
            let id_str = &after_id[..id_end];
            let id: i32 = match id_str.parse() {
                Ok(v) => v,
                Err(_) => {
                    search_from = &after_id[id_end..];
                    continue;
                }
            };

            let span_end = after_id[id_end..].find('}').map(|e| id_end + e).unwrap_or(after_id.len());
            let span = &after_id[id_end..span_end];

            // Try multiple refresh rate patterns
            let mut hz: Option<i32> = None;
            for pattern in &["vsyncRate=", "refreshRate=", "Hz="] {
                if let Some(vs_pos) = span.find(pattern) {
                    let hz_str = &span[vs_pos + pattern.len()..];
                    if let Some(first) = hz_str.split_whitespace().next() {
                        if let Ok(h) = first.parse::<f32>() {
                            if h > 0.0 {
                                hz = Some(h as i32);
                                break;
                            }
                        }
                    }
                }
            }

            if let Some(hz) = hz {
                if !modes.iter().any(|m| m.hz == hz) && modes.len() < RR_MODES_MAX {
                    modes.push(RrMode { id, hz });
                }
            }

            search_from = &after_id[span_end..];
        }
    }

    // Pattern 2: Look for standalone vsyncRate values if Pattern 1 found nothing
    if modes.is_empty() {
        for line in sf_out.lines() {
            for pattern in &["vsyncRate=", "refreshRate="] {
                if let Some(pos) = line.find(pattern) {
                    let hz_str = &line[pos + pattern.len()..];
                    if let Some(first) = hz_str.split_whitespace().next() {
                        if let Ok(hz_f) = first.parse::<f32>() {
                            let hz = hz_f as i32;
                            if hz > 0 && !modes.iter().any(|m| m.hz == hz) && modes.len() < RR_MODES_MAX {
                                modes.push(RrMode { id: modes.len() as i32, hz });
                            }
                        }
                    }
                }
            }
        }
    }

    if modes.is_empty() {
        log_warn!("cli", "rr_modes no display modes found");
        // Debug: search for vsyncRate anywhere in output
        let mut found_vsync = 0;
        for line in sf_out.lines() {
            if line.contains("vsyncRate=") || line.contains("refreshRate=") || line.contains("Hz=") {
                found_vsync += 1;
                if found_vsync <= 3 {
                    log_warn!("cli", "rr_modes found pattern line: {}", line.chars().take(200).collect::<String>());
                }
            }
        }
        if found_vsync == 0 {
            log_warn!("cli", "rr_modes: no vsyncRate/refreshRate/Hz patterns found in {} lines", sf_out.lines().count());
        }
        // Debug: log lines containing "Display" or "refresh" or "rate"
        for (i, line) in sf_out.lines().enumerate() {
            if (line.contains("Display") || line.contains("refresh") || line.contains("Rate")) && i < 20 {
                log_warn!("cli", "rr_modes line {}: {}", i, line.chars().take(150).collect::<String>());
            }
        }
        return 1;
    }

    modes.sort_by_key(|m| m.hz);

    let mut json = String::from("[");
    for (i, m) in modes.iter().enumerate() {
        if i > 0 {
            json.push(',');
        }
        json.push_str(&format!("{{\"id\":{},\"hz\":{}}}", m.id, m.hz));
    }
    json.push(']');
    json.push('\n');

    match fileops::write_string(RR_MODES_PATH, &json) {
        Ok(_) => {
            log_info!("cli", "rr_modes written count={}", modes.len());
            0
        }
        Err(e) => {
            log_error!("cli", "rr_modes open failed errno={}", e.raw_os_error().unwrap_or(-1));
            1
        }
    }
}

fn chmod(path: &str, mode: u32) {
    use std::ffi::CString;
    let c = match CString::new(path) {
        Ok(c) => c,
        Err(_) => return,
    };
    unsafe { libc::chmod(c.as_ptr(), mode) };
}

fn chmodg(pattern: &str, mode: u32) {
    use glob::glob;
    for entry in glob(pattern).into_iter().flatten() {
        if let Ok(path) = entry {
            let _ = chmod(path.to_str().unwrap_or(""), mode);
        }
    }
}
