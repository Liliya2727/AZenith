use std::fs;
use std::thread;
use std::time::Duration;
use std::process::Command;
use std::os::unix::fs::PermissionsExt;
use std::path::Path;
use glob::glob;

pub mod logger;
use crate::utils::logger::{log_info, verbose, log_warn, log_error};

pub const MY_PATH: &str = "/system/bin:/system/xbin:/data/adb/ap/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:/sbin:/sbin/su:/su/bin:/su/xbin:/data/data/com.termux/files/usr/bin";

pub fn getprop(key: &str) -> String {
    match Command::new("getprop").arg(key).output() {
        Ok(output) => String::from_utf8_lossy(&output.stdout).trim().to_string(),
        Err(e) => {
            log_error(&format!("Failed to getprop '{}': {}", key, e));
            String::new()
        }
    }
}

pub fn resetprop(key: &str, val: &str) {
    let status = if val.is_empty() {
        Command::new("resetprop").arg("--delete").arg(key).status()
    } else {
        Command::new("resetprop").arg(key).arg(val).status()
    };

    if let Err(e) = status {
        log_error(&format!("Failed to resetprop '{}': {}", key, e));
    }
}

pub fn setprop(key: &str, val: &str) {
    if let Err(e) = Command::new("setprop").arg(key).arg(val).status() {
        log_error(&format!("Failed to setprop '{}' to '{}': {}", key, val, e));
    }
}

pub fn get_debugmode() -> bool {
    getprop("persist.sys.azenith.debugmode") == "true"
}

pub fn get_fstrim_state() -> String {
    getprop("persist.sys.azenithconf.fstrim")
}

pub fn chmod(path: &str, mode: u32) {
    match fs::metadata(path) {
        Ok(metadata) => {
            let mut perms = metadata.permissions();
            perms.set_mode(mode);
            if let Err(e) = fs::set_permissions(path, perms) {
                log_error(&format!("Failed to set permissions for {}: {}", path, e));
            }
        },
        Err(e) => log_warn(&format!("Failed to read metadata (chmod) for {}: {}", path, e)),
    }
}

pub fn systemv(command: &str) -> i32 {
    match Command::new("/system/bin/sh")
        .arg("-c")
        .arg(command)
        .env("PATH", MY_PATH)
        .status()
    {
        Ok(status) => status.code().unwrap_or(-1),
        Err(e) => {
            log_error(&format!("systemv failed for '{}': {}", command, e));
            -1
        }
    }
}

pub fn execute_command(command: &str) -> Option<String> {
    match Command::new("/system/bin/sh")
        .arg("-c")
        .arg(command)
        .env("PATH", MY_PATH)
        .output()
    {
        Ok(output) if output.status.success() => {
            Some(String::from_utf8_lossy(&output.stdout).trim().to_string())
        },
        Ok(output) => {
            log_warn(&format!("Command '{}' failed with status: {}", command, output.status));
            None
        },
        Err(e) => {
            log_error(&format!("execute_command failed for '{}': {}", command, e));
            None
        }
    }
}

pub fn setsgov(gov: &str) {
    match glob("/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor") {
        Ok(paths) => {
            let mut applied = false;
            for path in paths.flatten() {
                if let Some(p_str) = path.to_str() {
                    chmod(p_str, 0o644);
                    if let Err(e) = fs::write(p_str, gov) {
                        log_error(&format!("Failed to write CPU governor to {}: {}", p_str, e));
                    } else {
                        applied = true;
                    }
                    chmod(p_str, 0o444);
                }
            }
            if applied {
                log_info(&format!("Set current CPU Governor to {}", gov));
            } else {
                log_warn(&format!("Failed to set CPU Governor to {}. No paths updated.", gov));
            }
        },
        Err(e) => log_error(&format!("Glob pattern failed for CPU scaling_governor: {}", e)),
    }
}

pub fn sets_io(scheduler: &str) {
    let mut applied = false;
    for block in &["sda", "sdb", "sdc", "mmcblk0", "mmcblk1"] {
        let path = format!("/sys/block/{}/queue/scheduler", block);
        if Path::new(&path).exists() {
            chmod(&path, 0o644);
            if let Err(e) = fs::write(&path, scheduler) {
                log_error(&format!("Failed to write IO scheduler to {}: {}", path, e));
            } else {
                applied = true;
            }
            chmod(&path, 0o444);
        }
    }
    if applied {
        log_info(&format!("Set current IO Scheduler to {}", scheduler));
    } else {
        log_warn(&format!("IO Scheduler path not found or failed for {}", scheduler));
    }
}

pub fn sets_mali_gov(gov: &str) {
    match glob("/sys/class/devfreq/*.mali/governor") {
        Ok(paths) => {
            let mut applied = false;
            for path in paths.flatten() {
                if let Some(p_str) = path.to_str() {
                    chmod(p_str, 0o644);
                    if let Err(e) = fs::write(p_str, gov) {
                        log_error(&format!("Failed to write Mali Governor to {}: {}", p_str, e));
                    } else {
                        applied = true;
                    }
                    chmod(p_str, 0o444); 
                }
            }
            if applied {
                log_info(&format!("Set current Mali GPU Governor to {}", gov));
            } else {
                log_warn("No Mali GPU governor paths found or updated.");
            }
        },
        Err(e) => log_error(&format!("Glob pattern failed for Mali governor: {}", e)),
    }
}

pub fn setthermalcore(state: &str) {
    if state == "1" {
        if systemv("sys.azenith-rianixiathermalcore &") != 0 {
            log_error("Failed to spawn Thermalcore service");
            return;
        }
        thread::sleep(Duration::from_secs(1));

        if let Some(pid) = execute_command("pgrep -f sys.azenith-rianixiathermalcore") {
            if !pid.is_empty() {
                log_info(&format!("Starting Thermalcore Service with pid {}", pid));
            } else {
                log_warn("Thermalcore service started but PID not found");
            }
        } else {
            log_error("Failed to execute pgrep for Thermalcore");
        }
    } else {
        if systemv("pkill -9 -f sys.azenith-rianixiathermalcore") != 0 {
            log_error("Failed to stop Thermalcore service");
        } else {
            log_info("Stopped Thermalcore service");
        }
    }
}

pub fn fstrim() {
    if get_fstrim_state() == "1" {
        verbose("Triggering Android native fstrim...");
        
        let mut status = systemv("vdc fstrim dotrim");
        
        if status != 0 {
            verbose("fstrim failed or not found, retry...");
            status = systemv("sm fstrim");
        }

        if status == 0 {
            log_info("Trimmed unused blocks successfully via Android native framework");
        } else {
            log_error(&format!("All native fstrim commands failed to execute. Status: {}", status));
        }
    }
}

pub fn enable_dnd() {
    if systemv("cmd notification set_dnd priority") == 0 {
        log_info("DND enabled");
    } else {
        log_error("Failed to enable DND");
    }
}

pub fn disable_dnd() {
    if systemv("cmd notification set_dnd off") == 0 {
        log_info("DND disabled");
    } else {
        log_error("Failed to disable DND");
    }
}

pub fn setrefreshrates(rate: &str) {
    let target_fps = rate.parse::<i32>().unwrap_or(60);

    let status = systemv(&format!(
        "am broadcast -a zx.azenith.SET_FPS -n zx.azenith/.RefreshRateReceiver --ei fps {}", 
        target_fps
    ));

    if status == 0 {
        log_info(&format!("Triggered receiver app to apply {}Hz", target_fps));
    } else {
        log_warn("Triggered receiver app, but command did not report success");
    }
}

pub fn restartservice() {
    let _ = systemv("pkill -9 -f sys.azenith-rianixiathermalcore");
    let _ = systemv("pkill -9 -f sys.azenith-service");
    let _ = systemv("pkill -9 -f sys.azenith-appmonitoring");
    
    setprop("persist.sys.azenith.state", "stopped");
    
    if systemv("sh /data/adb/modules/AZenith/service.sh &") != 0 {
        log_error("Failed to restart service script");
    } else {
        log_info("Restarted AZenith services");
    }
}

pub fn setrender(renderer: &str) {
    if renderer == "default" || renderer.is_empty() {
        setprop("debug.hwui.renderer", "");
        setprop("debug.renderengine.backend", "");
        setprop("debug.hwui.render_thread", "");
        setprop("debug.skia.threaded_mode", "");
        resetprop("ro.hwui.use_vulkan", ""); 
        
        log_info("Resetting all renderers to system default");
        return;
    }

    setprop("debug.hwui.renderer", renderer);

    if renderer.contains("threaded") {
        setprop("debug.hwui.render_thread", "true");
        if renderer.contains("skia") {
            setprop("debug.skia.threaded_mode", "true");
        } else {
            setprop("debug.skia.threaded_mode", "false");
        }
    } else {
        setprop("debug.hwui.render_thread", "false");
        setprop("debug.skia.threaded_mode", "false");
    }

    match renderer {
        "skiavk" | "skiavkthreaded" | "vulkan" => {
            setprop("debug.renderengine.backend", "vulkan");
            resetprop("ro.hwui.use_vulkan", "true"); 
        }
        "skiagl" | "skiaglthreaded" | "gles" | "opengl" | "openglthreaded" => {
            setprop("debug.renderengine.backend", "gles");
            resetprop("ro.hwui.use_vulkan", "false");
        }
        "software" => {
            setprop("debug.renderengine.backend", "");
            resetprop("ro.hwui.use_vulkan", "false");
        }
        _ => {
            if renderer.contains("vk") || renderer.contains("vulkan") {
                setprop("debug.renderengine.backend", "vulkan");
                resetprop("ro.hwui.use_vulkan", "true");
            } else if renderer.contains("gl") || renderer.contains("gles") {
                setprop("debug.renderengine.backend", "gles");
                resetprop("ro.hwui.use_vulkan", "false");
            } else {
                setprop("debug.renderengine.backend", "");
            }
        }
    }
    log_info(&format!("Successfully applied renderer: {}", renderer));
}

pub fn check_mali_path() {
    let mut found = false;
    if let Ok(paths) = glob::glob("/sys/class/devfreq/*.mali") {
        for _path in paths.flatten() {
            found = true;
            break;
        }
    }

    if found {
        println!("true");
        std::process::exit(0);
    } else {
        println!("false");
        std::process::exit(1);
    }
}
