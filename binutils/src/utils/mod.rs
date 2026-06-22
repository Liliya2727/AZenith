use std::fs; use std::thread; use std::time::Duration;
use std::process::Command;
use std::os::unix::fs::PermissionsExt;
use std::path::Path;
use glob::glob;
use std::collections::HashMap;

pub const MY_PATH: &str = "/system/bin:/system/xbin:/data/adb/ap/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:/sbin:/sbin/su:/su/bin:/su/xbin:/data/data/com.termux/files/usr/bin";
const SF_MAPPING_FILE: &str = "/data/adb/.config/AZenith/util_mapping.dat";


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

pub fn get_active_fps() -> Option<i32> {
    let mut samples: Vec<i32> = Vec::new();

    for _ in 0..8 {
        if let Ok(output) = Command::new("dumpsys")
            .args(["SurfaceFlinger", "--latency"])
            .output() 
        {
            let stdout = String::from_utf8_lossy(&output.stdout);
            
            if let Some(first_line) = stdout.lines().next() {
                if let Ok(period) = first_line.trim().parse::<i64>() {
                    if period > 0 {
                        let rate = (1_000_000_000 + (period / 2)) / period;
                        
                        if (30..=240).contains(&rate) {
                            samples.push(rate as i32);
                        }
                    }
                }
            }
        }
        thread::sleep(Duration::from_millis(50)); 
    }

    if !samples.is_empty() {
        samples.sort_unstable();
        let count = samples.len();
        let mid = count / 2;

        let median = if count % 2 != 0 {
            samples[mid]
        } else {
            (samples[mid - 1] + samples[mid]) / 2
        };
        
        return Some(median);
    }

    if let Ok(output) = Command::new("cmd").args(["display", "get-displays"]).output() {
        let stdout = String::from_utf8_lossy(&output.stdout);
        let keywords = ["renderFrameRate", "refreshRate", "fps="];
        
        for line in stdout.lines() {
            for &kw in &keywords {
                if let Some(idx) = line.find(kw) {
                    let start_idx = idx + kw.len();
                    let num_str: String = line[start_idx..]
                        .chars()
                        .skip_while(|c| !c.is_ascii_digit())
                        .take_while(|c| c.is_ascii_digit() || *c == '.')
                        .collect();
                    
                    if let Ok(fps) = num_str.parse::<f32>() {
                        if fps > 0.0 {
                            return Some(fps.round() as i32);
                        }
                    }
                }
            }
        }
    }

    None
}

pub fn calibrate_sf_modes() -> HashMap<i32, i32> {
    let mut map = HashMap::new();

    for idx in 0..=4 {
        let _ = Command::new("service")
            .args(["call", "SurfaceFlinger", "1035", "i32", &idx.to_string()])
            .status();

        thread::sleep(Duration::from_millis(600));

        if let Some(current_fps) = get_active_fps() {
            if !map.contains_key(&current_fps) {
                map.insert(current_fps, idx);
            }
        } else {
            dlog(&format!("Warn: Failed to parse FPS for SF Index {}", idx));
        }
    }

    let mut content = String::new();
    if map.is_empty() {
        dlog("Error: Calibration failed. Mapping data is completely empty!");
    } else {
        for (fps, idx) in &map {
            content.push_str(&format!("{}={}\n", fps, idx));
        }
    }
    
    let _ = fs::create_dir_all("/data/adb/.config/AZenith/");
    if fs::write(SF_MAPPING_FILE, content).is_err() {
        dlog("Error: Failed to save calibration data to file.");
    }
    
    map
}

pub fn check_and_calibrate_mapping() {
    dlog("Starting standalone SurfaceFlinger refresh rate mapping...");
    
    let map = calibrate_sf_modes();
    
    if !map.is_empty() {
        dlog(&format!("Successfully generated standalone mapping with {} active profiles.", map.len()));
        for (fps, idx) in &map {
            println!("{}Hz = SurfaceFlinger Index {}", fps, idx);
        }
    } else {
        dlog("Error: Standalone mapping generation failed.");
    }
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
    let target_fps = rate.parse::<i32>().unwrap_or(60);
    let mut map = HashMap::new();
    let rate_float = if rate.contains('.') {
        rate.to_string()
    } else {
        format!("{}.0", rate)
    };

    let _ = Command::new("settings")
        .args(["put", "system", "peak_refresh_rate", rate])
        .status();

    let _ = Command::new("settings")
        .args(["put", "system", "min_refresh_rate", &rate_float])
        .status();

    let _ = Command::new("settings")
        .args(["put", "system", "user_refresh_rate", rate])
        .status();

    let _ = Command::new("settings")
        .args(["put", "secure", "miui_refresh_rate", rate])
        .status();

    if let Ok(content) = fs::read_to_string(SF_MAPPING_FILE) {
        for line in content.lines() {
            let parts: Vec<&str> = line.split('=').collect();
            if parts.len() == 2 {
                if let (Ok(fps), Ok(idx)) = (parts[0].parse::<i32>(), parts[1].parse::<i32>()) {
                    map.insert(fps, idx);
                }
            }
        }
    }

    if !map.contains_key(&target_fps) {
        map = calibrate_sf_modes();
    }

    if let Some(&sf_index) = map.get(&target_fps) {
        let _ = Command::new("service")
            .args(["call", "SurfaceFlinger", "1035", "i32", &sf_index.to_string()])
            .status();

        dlog(&format!("Applied {}Hz via SurfaceFlinger (Index: {})", target_fps, sf_index));
    } else {
        dlog(&format!("Warn: Failed to apply {}Hz refresh rates.", target_fps));
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
