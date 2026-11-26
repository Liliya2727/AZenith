//! Governor detection, fallback and installation (CPU, I/O scheduler, Mali GPU)
//! plus the HWUI renderer. All of it runs during `initialize`.

use crate::utils::logging::{log_info, log_warn};
use crate::utils::props::{cfg, resetprop, setprop};
use crate::utils::sysfs::{chmod, hide_val, unhide_val, write_lock};
use std::fs;
use std::path::Path;

pub fn setgov(gov: &str) {
    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_governor") {
        for path in paths.flatten() {
            unhide_val(path.to_str().unwrap());
        }
    }

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            chmod(p_str, 0o644);
            let _ = fs::write(p_str, gov);
            chmod(p_str, 0o444);
        }
    }

    // Write and lock policy paths directly so SELinux/owner checks on cpu* symlinks
    // cannot leave a cluster on its old governor. When pinning to `performance`,
    // bind-mount shield the node so competing userspace daemons cannot revert it.
    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_governor") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            if gov == "performance" {
                hide_val(gov, p_str);
            } else {
                chmod(p_str, 0o644);
                let _ = fs::write(p_str, gov);
                chmod(p_str, 0o444);
            }
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

    let current_cfg = archconfig::load();
    log_info("cpu", &format!("default governor detected gov={}", default_gov));

    // Handle fallback if default is 'performance'
    if default_gov == "performance" && current_cfg.custom_default_cpu_gov.is_empty() {
        log_info("cpu", "default governor is performance, searching fallback");
        let avail_govs = fs::read_to_string(format!("{}/scaling_available_governors", cpu_path)).unwrap_or_default();
        let fallbacks = [
            "scx", "schedhorizon", "walt", "sched_pixel", "sugov_ext", "uag",
            "schedplus", "energy_step", "ondemand", "schedutil", "interactive",
            "conservative", "powersave"
        ];

        for gov in &fallbacks {
            if avail_govs.contains(gov) {
                default_gov = gov.to_string();
                log_info("cpu", &format!("fallback governor gov={}", gov));
                break;
            }
        }
    }

    let detected_default_gov = default_gov.clone();

    // Apply custom governor if set
    if !current_cfg.custom_default_cpu_gov.is_empty() {
        default_gov = current_cfg.custom_default_cpu_gov.clone();
    }

    log_info("cpu", &format!("using governor gov={}", default_gov));
    setgov(&default_gov);

    let _ = archconfig::update(|c| {
        c.default_cpu_gov = detected_default_gov;
        if c.custom_powersave_cpu_gov.is_empty() {
            c.custom_powersave_cpu_gov = powersave_gov.to_string();
        }
        if c.custom_performance_cpu_gov.is_empty() {
            c.custom_performance_cpu_gov = performance_gov.to_string();
        }
    });

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

    log_info("io", &format!("default scheduler detected sched={}", default_io));
    let detected_default_io = default_io.clone();
    let current_cfg = archconfig::load();

    // Apply custom IO if set
    if !current_cfg.custom_default_io.is_empty() {
        default_io = current_cfg.custom_default_io.clone();
    }

    log_info("io", &format!("using scheduler sched={}", default_io));
    sets_io(&default_io);

    let _ = archconfig::update(|c| {
        c.default_io = detected_default_io.clone();
        if c.custom_powersave_io.is_empty() {
            c.custom_powersave_io = detected_default_io.clone();
        }
        if c.custom_performance_io.is_empty() {
            c.custom_performance_io = detected_default_io;
        }
    });

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

    log_info("gpu", &format!("default governor detected gov={}", default_maligpu_gov));
    let detected_default_mali = default_maligpu_gov.clone();
    let current_cfg = archconfig::load();

    if !current_cfg.custom_default_mali_gov.is_empty() {
        default_maligpu_gov = current_cfg.custom_default_mali_gov.clone();
    }

    log_info("gpu", &format!("using governor gov={}", default_maligpu_gov));
    write_lock(&default_maligpu_gov, &gov_file);

    let _ = archconfig::update(|c| {
        c.default_mali_gov = detected_default_mali.clone();
        if c.custom_powersave_mali_gov.is_empty() {
            c.custom_powersave_mali_gov = detected_default_mali.clone();
        }
        if c.custom_performance_mali_gov.is_empty() {
            c.custom_performance_mali_gov = detected_default_mali;
        }
    });

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
    let renderer = &cfg().renderer;

    if renderer.is_empty() || renderer.eq_ignore_ascii_case("default") {
        log_info("renderer", "renderer is default, skipping setup");
        return;
    }

    log_info("renderer", &format!("applying renderer renderer={}", renderer));
    setrender(renderer);
}
