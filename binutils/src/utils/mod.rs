use std::fs;
use std::thread;
use std::time::Duration;
use std::process::Command;
use std::os::unix::fs::PermissionsExt;
use std::path::Path;
use glob::glob;

pub mod logger;
use crate::utils::logger::{log_info, log_debug, log_warn, log_error};

pub const MY_PATH: &str = "/system/bin:/system/xbin:/data/adb/ap/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:/sbin:/sbin/su:/su/bin:/su/xbin:/data/data/com.termux/files/usr/bin";

pub fn getprop(key: &str) -> String {
    match Command::new("getprop").arg(key).output() {
        Ok(output) => String::from_utf8_lossy(&output.stdout).trim().to_string(),
        Err(e) => {
            log_error("prop", &format!("getprop failed key={} err={}", key, e));
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
        log_error("prop", &format!("resetprop failed key={} err={}", key, e));
    }
}

pub fn setprop(key: &str, val: &str) {
    if let Err(e) = Command::new("setprop").arg(key).arg(val).status() {
        log_error("prop", &format!("setprop failed key={} value={} err={}", key, val, e));
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
                log_error("fs", &format!("set_permissions failed path={} err={}", path, e));
            }
        },
        Err(e) => log_warn("fs", &format!("chmod metadata failed path={} err={}", path, e)),
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
            log_error("shell", &format!("systemv failed command={} err={}", command, e));
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
            log_warn("shell", &format!("command failed command={} status={}", command, output.status));
            None
        },
        Err(e) => {
            log_error("shell", &format!("execute_command failed command={} err={}", command, e));
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
                        log_error("cpu", &format!("governor write failed path={} err={}", p_str, e));
                    } else {
                        applied = true;
                    }
                    chmod(p_str, 0o444);
                }
            }
            if applied {
                log_info("cpu", &format!("governor applied gov={}", gov));
            } else {
                log_warn("cpu", &format!("governor not applied gov={} reason=no_paths", gov));
            }
        },
        Err(e) => log_error("cpu", &format!("governor glob failed err={}", e)),
    }
}

pub fn sets_io(scheduler: &str) {
    let mut applied = false;
    for block in &["sda", "sdb", "sdc", "mmcblk0", "mmcblk1"] {
        let path = format!("/sys/block/{}/queue/scheduler", block);
        if Path::new(&path).exists() {
            chmod(&path, 0o644);
            if let Err(e) = fs::write(&path, scheduler) {
                log_error("io", &format!("scheduler write failed path={} err={}", path, e));
            } else {
                applied = true;
            }
            chmod(&path, 0o444);
        }
    }
    if applied {
        log_info("io", &format!("scheduler applied sched={}", scheduler));
    } else {
        log_warn("io", &format!("scheduler not applied sched={} reason=no_paths", scheduler));
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
                        log_error("gpu", &format!("mali governor write failed path={} err={}", p_str, e));
                    } else {
                        applied = true;
                    }
                    chmod(p_str, 0o444); 
                }
            }
            if applied {
                log_info("gpu", &format!("mali governor applied gov={}", gov));
            } else {
                log_warn("gpu", "mali governor not applied reason=no_paths");
            }
        },
        Err(e) => log_error("gpu", &format!("mali governor glob failed err={}", e)),
    }
}

pub fn setthermalcore(state: &str) {
    if state == "1" {
        if systemv("sys.azenith-rianixiathermalcore &") != 0 {
            log_error("thermal", "spawn failed");
            return;
        }
        thread::sleep(Duration::from_secs(1));

        if let Some(pid) = execute_command("pgrep -f sys.azenith-rianixiathermalcore") {
            if !pid.is_empty() {
                log_info("thermal", &format!("started pid={}", pid));
            } else {
                log_warn("thermal", "started but pid not found");
            }
        } else {
            log_error("thermal", "pgrep failed");
        }
    } else {
        if systemv("pkill -9 -f sys.azenith-rianixiathermalcore") != 0 {
            log_error("thermal", "stop failed");
        } else {
            log_info("thermal", "stopped");
        }
    }
}

pub fn fstrim() {
    if get_fstrim_state() == "1" {
        log_debug("fstrim", "trying native framework");
        
        let mut status = systemv("vdc fstrim dotrim");
        
        if status != 0 {
            log_debug("fstrim", "native failed, retrying via sm");
            status = systemv("sm fstrim");
        }

        if status == 0 {
            log_info("fstrim", "trimmed unused blocks");
        } else {
            log_error("fstrim", &format!("all trim commands failed status={}", status));
        }
    }
}

pub fn enable_dnd() {
    if systemv("cmd notification set_dnd priority") == 0 {
        log_info("dnd", "enabled");
    } else {
        log_error("dnd", "enable failed");
    }
}

pub fn disable_dnd() {
    if systemv("cmd notification set_dnd off") == 0 {
        log_info("dnd", "disabled");
    } else {
        log_error("dnd", "disable failed");
    }
}

pub fn setrefreshrates(rate: &str) {
    let target_fps = rate.parse::<i32>().unwrap_or(60);

    let status = systemv(&format!(
        "am broadcast -a zx.azenith.SET_FPS -n zx.azenith/.RefreshRateReceiver --ei fps {}", 
        target_fps
    ));

    if status == 0 {
        log_info("refresh", &format!("applied fps={}", target_fps));
    } else {
        log_warn("refresh", "receiver did not report success");
    }
}

pub fn restartservice() {
    let _ = systemv("pkill -9 -f sys.azenith-rianixiathermalcore");
    let _ = systemv("pkill -9 -f sys.azenith-service");
    let _ = systemv("pkill -9 -f sys.azenith-appmonitoring");
    
    setprop("persist.sys.azenith.state", "stopped");
    
    if systemv("sh /data/adb/modules/AZenith/service.sh &") != 0 {
        log_error("daemon", "service restart failed");
    } else {
        log_info("daemon", "services restarted");
    }
}

pub fn setrender(renderer: &str) {
    if renderer == "default" || renderer.is_empty() {
        setprop("debug.hwui.renderer", "");
        setprop("debug.renderengine.backend", "");
        setprop("debug.hwui.render_thread", "");
        setprop("debug.skia.threaded_mode", "");
        resetprop("ro.hwui.use_vulkan", ""); 
        
        log_info("renderer", "resetting renderers to default");
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
    log_info("renderer", &format!("renderer applied renderer={}", renderer));
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
