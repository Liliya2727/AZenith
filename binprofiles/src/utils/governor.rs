//! Governor detection, fallback and installation (CPU, I/O scheduler, Mali GPU)
//! plus the HWUI renderer. All of it runs during `initialize`.

use crate::utils::logging::{log_info, log_warn};
use crate::utils::props::{getprop, resetprop, setprop, setprop_cmd};
use crate::utils::sysfs::{chmod, write_lock};
use std::fs;
use std::path::Path;

pub fn setgov(gov: &str) {
    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            chmod(p_str, 0o644);
            let _ = fs::write(p_str, gov);
            chmod(p_str, 0o444);
        }
    }

    // Lock additional policy paths
    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_governor") {
        for path in paths.flatten() {
            chmod(path.to_str().unwrap(), 0o444);
        }
    }
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
}

pub fn sets_mali_gov(gov: &str) {
    if let Ok(paths) = glob::glob("/sys/class/devfreq/*.mali/governor") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                chmod(p_str, 0o644);
                let _ = fs::write(p_str, gov);
                chmod(p_str, 0o444); 
            }
        }
    }
}

/// Apply the CPU governor, I/O scheduler and Mali GPU governor together.
pub fn apply_custom_governor_io(perf_gov: &str, perf_io: &str, mali_gov: &str) {
    setgov(perf_gov);
    log_info("cpu", &format!("applying governor gov={}", perf_gov));
    
    if !perf_io.is_empty() {
        sets_io(perf_io);
        log_info("io", &format!("applying scheduler sched={}", perf_io));
    }
    
    if !mali_gov.is_empty() {
        sets_mali_gov(mali_gov);
    }
}

pub fn init_cpu_governor() {
    let cpu_path = "/sys/devices/system/cpu/cpu0/cpufreq";
    let gov_file = format!("{}/scaling_governor", cpu_path);
    chmod(&gov_file, 0o644);

    let mut default_gov = fs::read_to_string(&gov_file)
        .unwrap_or_default()
        .trim()
        .to_string();
        
    let performance_gov = "performance";
    let powersave_gov = "powersave";

    setprop_cmd("persist.sys.azenith.default_cpu_gov", &default_gov);
    log_info("cpu", &format!("default governor detected gov={}", default_gov));

    // Handle fallback if default is 'performance'
    if default_gov == "performance" && getprop("persist.sys.azenith.custom_default_cpu_gov").is_empty() {
        log_info("cpu", "default governor is performance, searching fallback");
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
                log_info("cpu", &format!("fallback governor gov={}", gov));
                break;
            }
        }
    }

    // Apply custom governor if set
    let custom_gov = getprop("persist.sys.azenith.custom_default_cpu_gov");
    if !custom_gov.is_empty() {
        default_gov = custom_gov;
    }
    
    log_info("cpu", &format!("using governor gov={}", default_gov));
    setgov(&default_gov);

    // Set fallback props
    if getprop("persist.sys.azenith.custom_powersave_cpu_gov").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_powersave_cpu_gov", &powersave_gov);
    }
    if getprop("persist.sys.azenith.custom_performance_cpu_gov").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_performance_cpu_gov", &performance_gov);
    }
    
    log_info("cpu", "governor init complete");
}

pub fn init_io_scheduler() {
    let mut io_path = String::new();
    for dev in &["mmcblk0", "mmcblk1", "sda", "sdb", "sdc"] {
        let p = format!("/sys/block/{}/queue", dev);
        if Path::new(&format!("{}/scheduler", p)).exists() {
            io_path = p;
            log_info("io", &format!("block device detected dev={}", dev));
            break;
        }
    }

    if io_path.is_empty() {
        log_warn("io", "no block device with scheduler found");
        std::process::exit(1);
    }

    let sched_file = format!("{}/scheduler", io_path);
    chmod(&sched_file, 0o644);

    // Parse active IO scheduler (the one inside brackets [])
    let mut default_io = String::new();
    let sched_content = fs::read_to_string(&sched_file).unwrap_or_default();
    if let Some(start) = sched_content.find('[') {
        if let Some(end) = sched_content[start..].find(']') {
            default_io = sched_content[start + 1..start + end].to_string();
        }
    }

    setprop_cmd("persist.sys.azenith.default_balanced_IO", &default_io);
    log_info("io", &format!("default scheduler detected sched={}", default_io));

    // Apply custom IO if set
    let custom_io = getprop("persist.sys.azenith.custom_default_balanced_IO");
    if !custom_io.is_empty() {
        default_io = custom_io;
    }
    
    log_info("io", &format!("using scheduler sched={}", default_io));
    sets_io(&default_io);

    // Set fallback props
    if getprop("persist.sys.azenith.custom_powersave_IO").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_powersave_IO", &default_io);
    }
    if getprop("persist.sys.azenith.custom_performance_IO").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_performance_IO", &default_io);
    }
    
    log_info("io", "io scheduler init complete");
}

pub fn init_maligpu_governor() {
    let mut gpu_path = String::new();
    
    if let Ok(paths) = glob::glob("/sys/class/devfreq/*.mali") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                gpu_path = p_str.to_string();
                log_info("gpu", &format!("mali path detected path={}", gpu_path));
                break;
            }
        }
    }

    if gpu_path.is_empty() {
        return;
    }

    let gov_file = format!("{}/governor", gpu_path);
    chmod(&gov_file, 0o644);

    let mut default_maligpu_gov = fs::read_to_string(&gov_file)
        .unwrap_or_default()
        .trim()
        .to_string();

    setprop_cmd("persist.sys.azenith.default_maligpu_gov", &default_maligpu_gov);
    log_info("gpu", &format!("default governor detected gov={}", default_maligpu_gov));

    let custom_maligpu_gov = getprop("persist.sys.azenith.custom_default_maligpu_gov");
    if !custom_maligpu_gov.is_empty() {
        default_maligpu_gov = custom_maligpu_gov;
    }

    log_info("gpu", &format!("using governor gov={}", default_maligpu_gov));
    write_lock(&default_maligpu_gov, &gov_file);

    if getprop("persist.sys.azenith.custom_powersave_maligpu_gov").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_powersave_maligpu_gov", &default_maligpu_gov);
    }
    if getprop("persist.sys.azenith.custom_performance_maligpu_gov").is_empty() {
        setprop_cmd("persist.sys.azenith.custom_performance_maligpu_gov", &default_maligpu_gov);
    }

    log_info("gpu", "mali governor init complete");
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

pub fn init_renderer() {
    let renderer = getprop("persist.sys.azenithconf.renderer");
    
    if renderer.is_empty() || renderer.eq_ignore_ascii_case("default") {
        log_info("renderer", "renderer is default, skipping setup");
        return;
    }
    
    log_info("renderer", &format!("applying renderer renderer={}", renderer));
    setrender(&renderer);
}
