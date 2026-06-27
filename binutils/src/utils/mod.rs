use std::fs;
use std::thread;
use std::time::Duration;
use std::process::Command;
use std::os::unix::fs::PermissionsExt;
use std::path::Path;
use glob::glob;

pub const MY_PATH: &str = "/system/bin:/system/xbin:/data/adb/ap/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:/sbin:/sbin/su:/su/bin:/su/xbin:/data/data/com.termux/files/usr/bin";

pub fn getprop(key: &str) -> String {
    if let Ok(output) = Command::new("getprop").arg(key).output() {
        String::from_utf8_lossy(&output.stdout).trim().to_string()
    } else {
        String::new()
    }
}

pub fn resetprop(key: &str, val: &str) {
    if val.is_empty() {
        let _ = Command::new("resetprop").arg("--delete").arg(key).status();
    } else {
        let _ = Command::new("resetprop").arg(key).arg(val).status();
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
    dlog(&format!("Set current Mali GPU Governor to {}", gov));
}

pub fn setthermalcore(state: &str) {
    if state == "1" {
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
    let target_fps = rate.parse::<i32>().unwrap_or(60);

    let status = Command::new("am")
        .args([
            "broadcast", 
            "-a", "zx.azenith.SET_FPS", 
            "-n", "zx.azenith/.RefreshRateReceiver", 
            "--ei", "fps", &target_fps.to_string()
        ])
        .status();

    if status.map(|s| s.success()).unwrap_or(false) {
        dlog(&format!("Triggered receiver app to apply {}Hz", target_fps));
    } else {
        dlog("Failed to trigger native app");
    }
}


pub fn restartservice() {
    let _ = Command::new("pkill").args(["-9", "-f", "sys.azenith-rianixiathermalcore"]).status();
    let _ = Command::new("pkill").args(["-9", "-f", "sys.azenith-service"]).status();
    let _ = Command::new("pkill").args(["-9", "-f", "sys.azenith-appmonitoring"]).status();
    setprop("persist.sys.azenith.state", "stopped");
    let _ = Command::new("sh").arg("/data/adb/modules/AZenith/service.sh").spawn();
}

pub fn setrender(renderer: &str) {
    if renderer == "default" || renderer.is_empty() {
        setprop("debug.hwui.renderer", "");
        setprop("debug.renderengine.backend", "");

        setprop("debug.hwui.render_thread", "");
        setprop("debug.skia.threaded_mode", "");

        resetprop("ro.hwui.use_vulkan", ""); 
        
        dlog("Resetting all renderers to system default");
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
    dlog(&format!("Successfully applied renderer: {}", renderer));
}

pub fn savelog() {

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
