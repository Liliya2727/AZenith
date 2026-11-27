//! CLI command handlers.

use crate::core::{
    LogLevel, ProfileMode, PROFILE_MODE,
    RR_MODES_PATH,
};
use crate::io::check_running_state;
use crate::log::{external_log, external_vlog};
use crate::{log_error, log_fatal, log_info, log_warn};
use crate::shell::systemv;
use crate::sysprop::set;
use crate::fileops;
use crate::util::notify;

use std::os::raw::c_int;

// ---------------------------------------------------------------------------
// handle_config_get / handle_config_set
// ---------------------------------------------------------------------------
pub fn handle_config_get(argc: i32, argv: &[&str]) -> c_int {
    if argc < 3 || argv[2].is_empty() {
        eprintln!("Usage: --config-get <KEY>");
        return 1;
    }
    if !std::path::Path::new(archconfig::CONFIG_PATH).exists() {
        crate::startup::migrate_props_to_json_once();
    }
    let key = argv[2];
    let cfg = archconfig::load();
    match cfg.get_key(key) {
        Ok(val) => {
            println!("{val}");
            0
        }
        Err(err) => {
            eprintln!("ERROR: {err}");
            1
        }
    }
}

pub fn handle_config_set(argc: i32, argv: &[&str]) -> c_int {
    if argc < 4 || argv[2].is_empty() {
        eprintln!("Usage: --config-set <KEY> <VALUE>");
        return 1;
    }
    if !std::path::Path::new(archconfig::CONFIG_PATH).exists() {
        crate::startup::migrate_props_to_json_once();
    }
    let key = argv[2];
    let val = argv[3];

    let mut set_err: Option<String> = None;
    let res = archconfig::update(|cfg| {
        if let Err(e) = cfg.set_key(key, val) {
            set_err = Some(e);
        }
    });

    if let Some(err) = set_err {
        eprintln!("ERROR: {err}");
        return 1;
    }
    if let Err(err) = res {
        eprintln!("ERROR: failed to write config.json: {err}");
        return 1;
    }
    0
}

// ---------------------------------------------------------------------------
// handle_prelaunch
// ---------------------------------------------------------------------------
pub fn handle_prelaunch(argc: i32, argv: &[&str]) -> c_int {
    if argc < 3 || argv[2].trim().is_empty() {
        eprintln!("Usage: --prelaunch <PACKAGE_NAME>");
        return 1;
    }
    let pkg = argv[2].trim();
    let target = crate::core::PRELAUNCH_FILE;
    let tmp = format!("{target}.tmp");
    if let Err(e) = std::fs::write(&tmp, format!("{pkg}\n")).and_then(|_| std::fs::rename(&tmp, target)) {
        let _ = std::fs::remove_file(&tmp);
        eprintln!("ERROR: failed to trigger prelaunch for {pkg}: {e}");
        return 1;
    }

    // Wait for archdaemon's inotify handler to apply renderer/downscale/per-app overrides
    // and unlink PRELAUNCH_FILE as acknowledgement before `am start` runs.
    let start = std::time::Instant::now();
    let timeout = std::time::Duration::from_millis(2500);
    while std::path::Path::new(target).exists() {
        if start.elapsed() >= timeout {
            let _ = std::fs::remove_file(target);
            break;
        }
        std::thread::sleep(std::time::Duration::from_millis(10));
    }
    0
}

// ---------------------------------------------------------------------------
// handle_profile
// ---------------------------------------------------------------------------
pub fn handle_profile(argc: i32, argv: &[&str]) -> c_int {
    if argc < 3 || argv[2].is_empty() {
        eprintln!("ERROR: Missing profile number. Use --profile <1|2|3>");
        return 1;
    }

    let cfg = archconfig::load();

    if cfg.auto_mode {
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
            if cfg.cpu_limit {
                set("sys.azenith.litemode", "1");
            } else {
                set("sys.azenith.litemode", "0");
            }
            // Need DaemonContext for run_profiler - but this is CLI-only
            // For CLI, we call run_profiler with a temporary context
            let mut ctx = crate::core::DaemonContext::new();
            ctx.config = cfg;
            crate::profile::run_profiler(ProfileMode::PerformanceProfile as i32, &mut ctx);
            notify("Performance Profile", "System is now at Powerful state", false, 0, None);
            println!("Applying Performance Profile");
        }
        "2" => {
            log_info!("cli", "profile apply requested profile=balanced source=cli");
            let mut ctx = crate::core::DaemonContext::new();
            ctx.config = cfg;
            crate::profile::run_profiler(ProfileMode::BalancedProfile as i32, &mut ctx);
            notify("Balanced Profile", "System is now at Optimal state", false, 0, None);
            println!("Applying Balanced Profile");
        }
        "3" => {
            log_info!("cli", "profile apply requested profile=eco source=cli");
            let mut ctx = crate::core::DaemonContext::new();
            ctx.config = cfg;
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
    println!("     -cbc,  --checkbypasschg   Check standby charge compatibility");
    println!("     -tb,   --testbypass <NODE> Test specific standby charge node for 10 seconds");
    println!("                                (or --testbypass --path=NODE_NAME)");
    println!();
    println!("     -bpl,  --bypasspathlist   Show all embedded standby charge paths");
    println!("     -rp,   --restorepending   Undo a standby charge node left ON by an interrupted test");
    println!();
    println!("            --config-get <KEY> Read a configuration field from config.json");
    println!("            --config-set <KEY> <VAL>");
    println!("                               Update a configuration field in config.json");
    println!("     -pl,   --prelaunch <PKG>  Apply per-app config before launching a game");
    println!();
    println!("     preload [OPTIONS] <PATHS> Run vmtouch-compatible page-cache preloader");
    println!("                               Use 'sys.azenith-service preload --help' for options");
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
// require_daemon_running
// ---------------------------------------------------------------------------
pub fn require_daemon_running() -> bool {
    // check_running_state() returns true when it can acquire the lock,
    // i.e. when no daemon holds it (daemon not running). It returns false
    // when the daemon holds the lock (daemon running).
    if check_running_state() {
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
    archconfig::sync_mirror();
    let mirror_pairs = [
        ("/data/adb/.config/AZenith/API/current_profile", "/data/data/zx.azenith/API/current_profile"),
        ("/data/adb/.config/AZenith/API/gameinfo", "/data/data/zx.azenith/API/gameinfo"),
        ("/data/adb/.config/AZenith/API/current_modes", "/data/data/zx.azenith/API/current_modes"),
        ("/data/adb/.config/AZenith/API/record_state", "/data/data/zx.azenith/API/record_state"),
        ("/data/adb/.config/AZenith/API/current_fps", "/data/data/zx.azenith/API/current_fps"),
        ("/data/adb/.config/AZenith/app_status", "/data/data/zx.azenith/API/app_status"),
        ("/data/adb/.config/AZenith/gamelist/azenithApplist.json", "/data/data/zx.azenith/API/azenithApplist.json"),
    ];
    for (src, dst) in &mirror_pairs {
        if let Ok(content) = std::fs::read_to_string(src) {
            let _ = crate::io::write2file(dst, false, false, &content);
        }
    }
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
    unsafe { libc::chmod(c.as_ptr(), mode as libc::mode_t) };
}

fn chmodg(pattern: &str, mode: u32) {
    use glob::glob;
    for entry in glob(pattern).into_iter().flatten() {
        if let Ok(path) = entry {
            let _ = chmod(path.to_str().unwrap_or(""), mode);
        }
    }
}
