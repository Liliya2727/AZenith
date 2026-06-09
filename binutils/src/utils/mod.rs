use std::fs; use std::thread; use std::time::Duration;
use std::process::Command;
use std::os::unix::fs::PermissionsExt;
use std::path::Path;
use glob::glob;


pub fn getprop(key: &str) -> String {
    if let Ok(output) = Command::new("getprop").arg(key).output() {
        String::from_utf8_lossy(&output.stdout).trim().to_string()
    } else {
        String::new()
    }
}

pub fn setprop(key: &str, val: &str) {
    let _ = Command::new("setprop").arg(key).arg(val).status();
}

pub fn get_debugmode() -> bool {
    getprop("persist.sys.azenith.debugmode") == "true"
}

pub fn get_fstrim_state() -> String {
    getprop("persist.sys.azenithconf.fstrim")
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
        .args(["--log", "AZenith_Utility", "1", message])
        .status();
}

pub fn chmod(path: &str, mode: u32) {
    if let Ok(metadata) = fs::metadata(path) {
        let mut perms = metadata.permissions();
        perms.set_mode(mode);
        let _ = fs::set_permissions(path, perms);
    }
}


pub fn setsgov(gov: &str) {
    if let Ok(paths) = glob("/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            chmod(p_str, 0o644);
            let _ = fs::write(p_str, gov);
            chmod(p_str, 0o444);
        }
    }
    dlog(&format!("Set current CPU Governor to {}", gov));
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
    dlog(&format!("Set current IO Scheduler to {}", scheduler));
}

pub fn setthermalcore(state: &str) {
    if state == "1" {
        // Run in background
        let _ = Command::new("sys.azenith-rianixiathermalcore").spawn();
        thread::sleep(Duration::from_secs(1));

        let output = Command::new("pgrep").args(["-f", "sys.azenith-rianixiathermalcore"]).output().unwrap();
        let pid = String::from_utf8_lossy(&output.stdout).trim().to_string();

        if !pid.is_empty() {
            dlog(&format!("Starting Thermalcore Service with pid {}", pid));
        } else {
            dlog("Thermalcore service started but PID not found");
        }
    } else {
        let _ = Command::new("pkill").args(["-9", "-f", "sys.azenith-rianixiathermalcore"]).status();
        dlog("Stopped Thermalcore service");
    }
}

pub fn fstrim() {
    if get_fstrim_state() == "1" {
        let mounts = ["/system", "/vendor", "/data", "/cache", "/metadata", "/odm", "/system_ext", "/product"];
        for mount in mounts {
            let is_mounted = Command::new("mountpoint")
                .arg("-q")
                .arg(mount)
                .status()
                .map(|s| s.success())
                .unwrap_or(false);

            if is_mounted {
                let _ = Command::new("fstrim").arg("-v").arg(mount).status();
                az_log(&format!("Trimmed: {}", mount));
            } else {
                az_log(&format!("Skipped (not mounted): {}", mount));
            }
        }
        dlog("Trimmed unused blocks");
    }
}

pub fn enable_dnd() {
    if Command::new("cmd").args(["notification", "set_dnd", "priority"]).status().map(|s| s.success()).unwrap_or(false) {
        dlog("DND enabled");
    } else {
        dlog("Failed to enable DND");
    }
}

pub fn disable_dnd() {
    if Command::new("cmd").args(["notification", "set_dnd", "off"]).status().map(|s| s.success()).unwrap_or(false) {
        dlog("DND disabled");
    } else {
        dlog("Failed to disable DND");
    }
}

pub fn setrefreshrates(rate: &str) {
    let _ = Command::new("settings")
        .args(["put", "system", "min_refresh_rate", rate])
        .status();

    let _ = Command::new("settings")
        .args(["put", "system", "peak_refresh_rate", rate])
        .status();

    let _ = Command::new("settings")
        .args(["put", "system", "user_refresh_rate", rate])
        .status();

    let _ = Command::new("settings")
        .args(["put", "secure", "miui_refresh_rate", rate])
        .status();
}



pub fn restartservice() {
    let _ = Command::new("pkill").args(["-9", "-f", "sys.azenith-rianixiathermalcore"]).status();
    let _ = Command::new("pkill").args(["-9", "-f", "sys.azenith-service"]).status();
    let _ = Command::new("pkill").args(["-9", "-f", "sys.azenith-appmonitoring"]).status();

    setprop("persist.sys.azenith.state", "stopped");

    // Spawn script in background, detached (equivalent to & disown)
    let _ = Command::new("sh").arg("/data/adb/modules/AZenith/service.sh").spawn();
}

pub fn setrender(renderer: &str) {
    match renderer {
        "skiavk" => {
            setprop("debug.hwui.renderer", "skiavk");
        }
        "skiagl" => {
            setprop("debug.hwui.renderer", "skiagl");
        }
        _ => {
            // Ignore
        }
    }
    dlog(&format!("Set current renderer to: {}", renderer));
}

pub fn savelog() {
    // Generate date string via shell to strictly mirror `date +"%Y-%m-%d_%H-%M"` without pulling in the `chrono` crate.
    let date_output = Command::new("date").arg("+%Y-%m-%d_%H-%M").output().expect("Failed to get date");
    let date_str = String::from_utf8_lossy(&date_output.stdout).trim().to_string();
    let log_file = format!("/sdcard/AZenithLog_{}.txt", date_str);

    println!("{}", log_file);

    let module_prop = fs::read_to_string("/data/adb/modules/AZenith/module.prop").unwrap_or_default();
    let module_ver = module_prop.lines()
        .find(|line| line.starts_with("version="))
        .map(|line| line.replace("version=", ""))
        .unwrap_or_default();

    let android_sdk = getprop("ro.build.version.sdk");

    let kernel_output = Command::new("uname").args(["-r", "-m"]).output().unwrap();
    let kernel_info = String::from_utf8_lossy(&kernel_output.stdout).trim().to_string();

    let fingerprint = getprop("ro.build.fingerprint");

    let mut log_content = format!(
        "##########################################\n\n\
         \x20            AZenith Process Log\n\n\
         \x20   Module: {}\n\
         \x20   Android: {}\n\
         \x20   Kernel: {}\n\
         \x20   Fingerprint: {}\n\
         ##########################################\n\n",
        module_ver, android_sdk, kernel_info, fingerprint
    );

    let previous_log = fs::read_to_string("/data/adb/.config/AZenith/debug/AZenith.log").unwrap_or_default();
    log_content.push_str(&previous_log);

    let _ = fs::write(&log_file, log_content);
}
