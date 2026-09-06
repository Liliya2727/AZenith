//
// Copyright (C) 2026-2027 Zexshia
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

use crate::utils::*;
use std::process::Command;
use std::thread;

const LIST_LOGGER: [&str; 8] = [
    "logd",
    "traced",
    "statsd",
    "tcpdump",
    "cnss_diag",
    "subsystem_ramdump",
    "charge_logger",
    "wlan_logging",
];

fn get_state(key: &str) -> String {
    let val = getprop(key);
    if val.is_empty() {
        "0".to_string()
    } else {
        val
    }
}

fn state_enabled(state: &str) -> bool {
    state == "1"
}

fn top_freqs(available: &str, max: usize) -> Vec<u64> {
    let mut freqs: Vec<u64> = available
        .split_whitespace()
        .filter_map(|s| s.parse().ok())
        .collect();
    freqs.sort_unstable();
    freqs.reverse();
    freqs.truncate(max);
    freqs
}

fn join_u64(nums: &[u64]) -> String {
    nums.iter()
        .map(|n| n.to_string())
        .collect::<Vec<String>>()
        .join(" ")
}

fn apply_jit() {
    log_info("Applying JIT Compiler");

    let output = Command::new("cmd")
        .args(["package", "list", "packages", "-3"])
        .output();

    let packages = match output {
        Ok(o) => String::from_utf8_lossy(&o.stdout)
            .lines()
            .filter_map(|line| {
                line.split_once(':').map(|(_, pkg)| pkg.trim().to_string())
            })
            .collect::<Vec<String>>(),
        Err(_) => Vec::new(),
    };

    let mut handles = Vec::new();
    for pkg in packages {
        handles.push(thread::spawn(move || {
            let ok = Command::new("cmd")
                .args(["package", "compile", "-m", "speed-profile", &pkg])
                .stdout(std::process::Stdio::null())
                .stderr(std::process::Stdio::null())
                .status()
                .map(|s| s.success())
                .unwrap_or(false);
            if ok {
                log_verbose(&format!("{} | Success", pkg));
            }
        }));
    }
    for handle in handles {
        let _ = handle.join();
    }
}

fn settunes(policy_path: &str) {
    if !std::path::Path::new(policy_path).is_dir() {
        return;
    }

    let freqs = std::fs::read_to_string(format!("{}/scaling_available_frequencies", policy_path))
        .unwrap_or_default();
    if freqs.trim().is_empty() {
        return;
    }

    let selected = top_freqs(&freqs, 6);
    if selected.is_empty() {
        return;
    }
    let selected_str = join_u64(&selected);

    let up_delay = (1..=selected.len())
        .map(|i| (50 * i).to_string())
        .collect::<Vec<String>>()
        .join(" ");

    let up_rate = 6500usize;
    let down_rate = 12000usize;
    let rate_limit = 7000usize;

    let schedhorizon = format!("{}/schedhorizon", policy_path);
    if std::path::Path::new(&schedhorizon).is_dir() {
        write_val(&up_delay, &format!("{}/up_delay", schedhorizon), true);
        write_val(&selected_str, &format!("{}/efficient_freq", schedhorizon), true);

        if std::path::Path::new(&format!("{}/up_rate_limit_us", schedhorizon)).exists() {
            write_val(&up_rate.to_string(), &format!("{}/up_rate_limit_us", schedhorizon), true);
        } else if std::path::Path::new(&format!("{}/rate_limit_us", schedhorizon)).exists() {
            write_val(&rate_limit.to_string(), &format!("{}/rate_limit_us", schedhorizon), true);
        }

        if std::path::Path::new(&format!("{}/down_rate_limit_us", schedhorizon)).exists() {
            write_val(&down_rate.to_string(), &format!("{}/down_rate_limit_us", schedhorizon), true);
        }
    }

    let schedutil = format!("{}/schedutil", policy_path);
    if std::path::Path::new(&schedutil).is_dir() {
        if std::path::Path::new(&format!("{}/up_rate_limit_us", schedutil)).exists() {
            write_val(&up_rate.to_string(), &format!("{}/up_rate_limit_us", schedutil), true);
        } else if std::path::Path::new(&format!("{}/rate_limit_us", schedutil)).exists() {
            write_val(&rate_limit.to_string(), &format!("{}/rate_limit_us", schedutil), true);
        }

        if std::path::Path::new(&format!("{}/down_rate_limit_us", schedutil)).exists() {
            write_val(&down_rate.to_string(), &format!("{}/down_rate_limit_us", schedutil), true);
        }
    }
}

fn apply_schedtunes() {
    log_info("Applying Schedtunes for Schedutil and Schedhorizon");

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                settunes(p_str);
            }
        }
    }
}

#[allow(clippy::too_many_arguments)]
fn setwalt(policy_path: &str, params: &WaltParams) {
    let walt = format!("{}/walt", policy_path);
    let walt_path = std::path::Path::new(&walt);
    if !walt_path.is_dir() {
        log_verbose(&format!("Skipped: {} (WALT NA)", policy_path));
        return;
    }

    let available_freqs = std::fs::read_to_string(format!("{}/scaling_available_frequencies", policy_path))
        .unwrap_or_default();
    if available_freqs.trim().is_empty() {
        return;
    }

    let all_freqs: Vec<u64> = available_freqs
        .split_whitespace()
        .filter_map(|s| s.parse().ok())
        .collect();
    if all_freqs.is_empty() {
        return;
    }

    let mut sorted = all_freqs.clone();
    sorted.sort_unstable();

    let selected: Vec<u64> = sorted
        .iter()
        .rev()
        .take(params.wt_cnt)
        .copied()
        .collect();
    if selected.is_empty() {
        return;
    }
    let selected_str = join_u64(&selected);

    let highest = selected[0];
    let second = if selected.len() > 1 { selected[1] } else { highest };

    let num_freqs = selected.len();
    let mut tloads = Vec::new();
    let mut cur = params.wt_st;
    for _ in 0..num_freqs {
        tloads.push(cur.to_string());
        cur = if cur >= params.wt_sp { cur - params.wt_sp } else { 0 };
        if cur < 10 {
            cur = 10;
        }
    }
    let tloads_str = tloads.join(" ");

    write_val(&params.wl_hi.to_string(), &format!("{}/hispeed_load", walt), true);
    write_val(&second.to_string(), &format!("{}/hispeed_freq", walt), true);
    write_val(&highest.to_string(), &format!("{}/rtg_boost_freq", walt), true);
    write_val(&tloads_str, &format!("{}/target_loads", walt), true);
    write_val(&selected_str, &format!("{}/efficient_freq", walt), true);
    write_val(&params.wr_up.to_string(), &format!("{}/up_rate_limit_us", walt), true);
    write_val(&params.wr_dn.to_string(), &format!("{}/down_rate_limit_us", walt), true);

    log_info(&format!("WALT Tuning Applied on {}", policy_path.rsplit('/').next().unwrap_or(policy_path)));
}

struct WaltParams {
    wr_up: u64,
    wr_dn: u64,
    wl_hi: u64,
    wt_cnt: usize,
    wt_st: i64,
    wt_sp: i64,
}

fn apply_walt() {
    log_info("Applying WALT governor tuning");

    let params = WaltParams {
        wr_up: 8000,
        wr_dn: 12000,
        wl_hi: 92,
        wt_cnt: 6,
        wt_st: 95,
        wt_sp: 8,
    };

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                setwalt(p_str, &params);
            }
        }
    }
}

fn apply_fpsged() {
    log_info("Applying FPSGO Parameters");

    let ged_params = [
        ("ged_smart_boost", "1"),
        ("boost_upper_bound", "100"),
        ("enable_gpu_boost", "1"),
        ("enable_cpu_boost", "1"),
        ("ged_boost_enable", "1"),
        ("boost_gpu_enable", "1"),
        ("gpu_dvfs_enable", "1"),
        ("gx_frc_mode", "1"),
        ("gx_dfps", "1"),
        ("gx_force_cpu_boost", "1"),
        ("gx_boost_on", "1"),
        ("gx_game_mode", "1"),
        ("gx_3D_benchmark_on", "1"),
        ("gpu_loading", "0"),
        ("cpu_boost_policy", "1"),
        ("boost_extra", "1"),
        ("is_GED_KPI_enabled", "0"),
    ];

    for (name, value) in ged_params {
        write_val(value, &format!("/sys/module/ged/parameters/{}", name), true);
    }

    let fpsgo_dir = "/sys/kernel/fpsgo";
    write_val("0", &format!("{}/fbt/boost_ta", fpsgo_dir), true);
    write_val("1", &format!("{}/fbt/enable_switch_down_throttle", fpsgo_dir), true);
    write_val("1", &format!("{}/fstb/adopt_low_fps", fpsgo_dir), true);
    write_val("1", &format!("{}/fstb/fstb_self_ctrl_fps_enable", fpsgo_dir), true);
    write_val("0", &format!("{}/fstb/boost_ta", fpsgo_dir), true);
    write_val("1", &format!("{}/fstb/enable_switch_sync_flag", fpsgo_dir), true);
    write_val("0", &format!("{}/fbt/boost_VIP", fpsgo_dir), true);
    write_val("1", &format!("{}/fstb/gpu_slowdown_check", fpsgo_dir), true);
    write_val("1", &format!("{}/fbt/thrm_limit_cpu", fpsgo_dir), true);
    write_val("0", &format!("{}/fbt/thrm_temp_th", fpsgo_dir), true);
    write_val("0", &format!("{}/fbt/llf_task_policy", fpsgo_dir), true);

    write_val("100", "/sys/module/mtk_fpsgo/parameters/uboost_enhance_f", true);
    write_val("0", "/sys/module/mtk_fpsgo/parameters/isolation_limit_cap", true);
    write_val("1", "/sys/pnpmgr/fpsgo_boost/boost_enable", true);
    write_val("1", "/sys/pnpmgr/fpsgo_boost/boost_mode", true);
    write_val("1", "/sys/pnpmgr/install", true);
    write_val("100", "/sys/kernel/ged/hal/gpu_boost_level", true);
}

fn apply_mali_sched() {
    log_info("Applying GPU Mali Sched");

    let m_sched = glob::glob("/sys/devices/platform/soc/*mali*/scheduling")
        .ok()
        .and_then(|mut paths| paths.find_map(Result::ok).map(|p| p.to_string_lossy().into_owned()));

    let m_base = glob::glob("/sys/devices/platform/soc/*mali*")
        .ok()
        .and_then(|mut paths| paths.find_map(Result::ok).map(|p| p.to_string_lossy().into_owned()));

    if let Some(sched) = m_sched {
        write_val("full", &format!("{}/serialize_jobs", sched), true);
    }
    if let Some(base) = m_base {
        write_val("1", &format!("{}/js_ctx_scheduling_mode", base), true);
    }
}

fn get_stable_refresh_rate() -> u64 {
    let mut samples: Vec<u64> = Vec::new();

    for _ in 0..5 {
        let output = Command::new("dumpsys")
            .args(["SurfaceFlinger", "--latency"])
            .output();

        let period = match output {
            Ok(o) => String::from_utf8_lossy(&o.stdout)
                .lines()
                .next()
                .unwrap_or_default()
                .trim()
                .split_whitespace()
                .next()
                .unwrap_or_default()
                .to_string(),
            Err(_) => String::new(),
        };

        if let Ok(period_num) = period.parse::<u64>() {
            if period_num > 0 {
                let rate = (1_000_000_000u64 + (period_num / 2)) / period_num;
                if (30..=240).contains(&rate) {
                    samples.push(rate);
                }
            }
        }

        std::thread::sleep(std::time::Duration::from_millis(50));
    }

    if samples.is_empty() {
        return 60;
    }

    samples.sort_unstable();
    let count = samples.len();
    if count % 2 == 1 {
        samples[count / 2]
    } else {
        (samples[count / 2 - 1] + samples[count / 2]) / 2
    }
}

fn get_cpu_load() -> f64 {
    let output = Command::new("top")
        .args(["-n", "1", "-b"])
        .output();

    let stdout = match output {
        Ok(o) => String::from_utf8_lossy(&o.stdout).into_owned(),
        Err(_) => return 0.0,
    };

    for line in stdout.lines() {
        if line.contains("Cpu(s)") {
            let fields: Vec<&str> = line.split_whitespace().collect();
            let load = fields
                .get(1)
                .and_then(|f| f.parse::<f64>().ok())
                .unwrap_or(0.0)
                + fields
                    .get(3)
                    .and_then(|f| f.parse::<f64>().ok())
                    .unwrap_or(0.0);
            return load;
        }
    }
    0.0
}

fn apply_surfaceflinger() {
    log_info("Applying SurfaceFlinger Latency");

    let refresh_rate = get_stable_refresh_rate();
    let frame_duration_ns = (1_000_000_000f64 / refresh_rate as f64).round() as i64;

    let cpu_load = get_cpu_load();
    let base_margin = 0.07f64;
    let margin_ratio = if cpu_load > 70.0 { base_margin + 0.01 } else { base_margin };
    let min_margin = (frame_duration_ns as f64 * margin_ratio).round() as i64;

    let (app_phase_ratio, sf_phase_ratio, app_duration_ratio, sf_duration_ratio) = match refresh_rate {
        r if r >= 120 => (0.68, 0.85, 0.58, 0.32),
        r if r >= 90 => (0.66, 0.82, 0.60, 0.30),
        r if r >= 75 => (0.64, 0.80, 0.62, 0.28),
        _ => (0.62, 0.75, 0.65, 0.25),
    };

    let app_phase_offset_ns = -(frame_duration_ns as f64 * app_phase_ratio).round() as i64;
    let sf_phase_offset_ns = -(frame_duration_ns as f64 * sf_phase_ratio).round() as i64;
    let mut app_duration = (frame_duration_ns as f64 * app_duration_ratio).round() as i64;
    let mut sf_duration = (frame_duration_ns as f64 * sf_duration_ratio).round() as i64;

    let app_end_time = app_phase_offset_ns + app_duration;
    let dead_time = -(app_end_time + sf_phase_offset_ns);

    if dead_time < min_margin {
        let adjustment = min_margin - dead_time;
        log_verbose(&format!(
            "Optimization: Adjusted app duration by -{}ns for dynamic margin",
            adjustment
        ));
        let new_app_duration = app_duration - adjustment;
        app_duration = if new_app_duration > 0 { new_app_duration } else { 0 };
    }

    let min_phase_duration = (frame_duration_ns as f64 * 0.12).round() as i64;
    if app_duration < min_phase_duration {
        app_duration = min_phase_duration;
    }
    if sf_duration < min_phase_duration {
        sf_duration = min_phase_duration;
    }

    let app_duration_str = app_duration.to_string();
    let sf_duration_str = sf_duration.to_string();
    let app_phase_str = app_phase_offset_ns.to_string();
    let sf_phase_str = sf_phase_offset_ns.to_string();

    resetprop("debug.sf.early.app.duration", &app_duration_str);
    resetprop("debug.sf.earlyGl.app.duration", &app_duration_str);
    resetprop("debug.sf.late.app.duration", &app_duration_str);

    resetprop("debug.sf.early.sf.duration", &sf_duration_str);
    resetprop("debug.sf.earlyGl.sf.duration", &sf_duration_str);
    resetprop("debug.sf.late.sf.duration", &sf_duration_str);

    resetprop("debug.sf.early_app_phase_offset_ns", &app_phase_str);
    resetprop("debug.sf.high_fps_early_app_phase_offset_ns", &app_phase_str);
    resetprop("debug.sf.high_fps_late_app_phase_offset_ns", &app_phase_str);
    resetprop("debug.sf.early_phase_offset_ns", &sf_phase_str);
    resetprop("debug.sf.high_fps_early_phase_offset_ns", &sf_phase_str);
    resetprop("debug.sf.high_fps_late_sf_phase_offset_ns", &sf_phase_str);

    let threshold_ratio = match refresh_rate {
        r if r >= 120 => 0.28,
        r if r >= 90 => 0.32,
        r if r >= 75 => 0.35,
        _ => 0.38,
    };

    let mut phase_offset_threshold_ns = (frame_duration_ns as f64 * threshold_ratio).round() as i64;
    let max_threshold = (frame_duration_ns as f64 * 0.45).round() as i64;
    let min_threshold = (frame_duration_ns as f64 * 0.22).round() as i64;

    if phase_offset_threshold_ns > max_threshold {
        phase_offset_threshold_ns = max_threshold;
    } else if phase_offset_threshold_ns < min_threshold {
        phase_offset_threshold_ns = min_threshold;
    }

    resetprop(
        "debug.sf.phase_offset_threshold_for_next_vsync_ns",
        &phase_offset_threshold_ns.to_string(),
    );

    resetprop("debug.sf.enable_advanced_sf_phase_offset", "1");
    resetprop("debug.sf.predict_hwc_composition_strategy", "1");
    resetprop("debug.sf.use_phase_offsets_as_durations", "1");
    resetprop("debug.sf.disable_hwc_vds", "1");
    resetprop("debug.sf.show_refresh_rate_overlay_spinner", "0");
    resetprop("debug.sf.show_refresh_rate_overlay_render_rate", "0");
    resetprop("debug.sf.show_refresh_rate_overlay_in_middle", "0");
    resetprop("debug.sf.kernel_idle_timer_update_overlay", "0");
    resetprop("debug.sf.dump.enable", "0");
    resetprop("debug.sf.dump.external", "0");
    resetprop("debug.sf.dump.primary", "0");
    resetprop("debug.sf.treat_170m_as_sRGB", "0");
    resetprop("debug.sf.luma_sampling", "0");
    resetprop("debug.sf.showupdates", "0");
    resetprop("debug.sf.disable_client_composition_cache", "0");
    resetprop("debug.sf.treble_testing_override", "false");
    resetprop("debug.sf.enable_layer_caching", "false");
    resetprop("debug.sf.enable_cached_set_render_scheduling", "true");
    resetprop("debug.sf.layer_history_trace", "false");
    resetprop("debug.sf.edge_extension_shader", "false");
    resetprop("debug.sf.enable_egl_image_tracker", "false");
    resetprop("debug.sf.use_phase_offsets_as_durations", "false");
    resetprop("debug.sf.layer_caching_highlight", "false");
    resetprop("debug.sf.enable_hwc_vds", "false");
    resetprop("debug.sf.vsp_trace", "false");
    resetprop("debug.sf.enable_transaction_tracing", "false");
    resetprop("debug.hwui.filter_test_overhead", "false");
    resetprop("debug.hwui.show_layers_updates", "false");
    resetprop("debug.hwui.capture_skp_enabled", "false");
    resetprop("debug.hwui.trace_gpu_resources", "false");
    resetprop("debug.hwui.skia_tracing_enabled", "false");
    resetprop("debug.hwui.nv_profiling", "false");
    resetprop("debug.hwui.skia_use_perfetto_track_events", "false");
    resetprop("debug.hwui.show_dirty_regions", "false");
    resetprop("debug.hwui.profile", "false");
    resetprop("debug.hwui.overdraw", "false");
    resetprop("debug.hwui.show_non_rect_clip", "hide");
    resetprop("debug.hwui.webview_overlays_enabled", "false");
    resetprop("debug.hwui.skip_empty_damage", "true");
    resetprop("debug.hwui.use_gpu_pixel_buffers", "true");
    resetprop("debug.hwui.use_buffer_age", "true");
    resetprop("debug.hwui.use_partial_updates", "true");
    resetprop("debug.hwui.skip_eglmanager_telemetry", "true");
    resetprop("debug.hwui.level", "0");
}

fn find_thermal_init_services() -> Vec<String> {
    let mut services = Vec::new();
    for dir in ["/system/etc/init", "/vendor/etc/init", "/odm/etc/init"] {
        if let Ok(read_dir) = std::fs::read_dir(dir) {
            for entry in read_dir.flatten() {
                if let Ok(content) = std::fs::read_to_string(entry.path()) {
                    for line in content.lines() {
                        let trimmed = line.trim_start();
                        if trimmed.starts_with("service") && line.contains("thermal") {
                            if let Some(name) = trimmed.split_whitespace().nth(1) {
                                services.push(name.to_string());
                            }
                        }
                    }
                }
            }
        }
    }
    services
}

fn thermal_prop_matches(key: &str) -> bool {
    key.contains("init.svc.thermal")
        || key.contains("thermal-cutoff")
        || (key.contains("ro.vendor.") && key.contains("thermal"))
        || key.contains("debug.thermal")
        || (key.contains("debug_pid") && key.contains("thermal"))
        || (key.contains("boottime") && key.contains("thermal"))
        || (key.contains("thermal") && key.contains("running"))
}

fn apply_disable_thermal() {
    log_info("Disabling Thermal Engine");

    let _ = Command::new("pkill")
        .args(["-9", "-f", "thermald|thermal-engine|mtk_thermal"])
        .status();

    for svc in find_thermal_init_services() {
        let _ = Command::new("stop").arg(&svc).status();
    }

    if let Ok(output) = Command::new("getprop").output() {
        let stdout = String::from_utf8_lossy(&output.stdout);
        for line in stdout.lines() {
            if let Some(key) = bracket_key(line) {
                if thermal_prop_matches(&key) {
                    resetprop(&key, "suspended");
                }
            }
        }
    }

    if let Ok(paths) = glob::glob("/sys/class/thermal/thermal_zone*/mode") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                write_val("disabled", p_str, false);
            }
        }
    }

    if let Ok(paths) = glob::glob("/sys/class/thermal/thermal_zone*/policy") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                write_val("userspace", p_str, false);
            }
        }
    }

    chmod_glob("/sys/devices/virtual/thermal/thermal_zone*/temp", 0o000);
    chmod_glob("/sys/devices/virtual/thermal/thermal_zone*/trip_point_*", 0o000);

    if let Ok(content) = std::fs::read_to_string("/proc/ppm/policy_status") {
        for line in content.lines() {
            if line.contains("FORCE_LIMIT") || line.contains("PWR_THRO") || line.contains("THERMAL") {
                if let Some(idx) = bracket_key(line) {
                    write_val(&format!("{} 0", idx), "/proc/ppm/policy_status", true);
                }
            }
        }
    }

    let gpu_limit = "/proc/gpufreq/gpufreq_power_limited";
    if std::path::Path::new(gpu_limit).exists() {
        for key in [
            "ignore_batt_oc",
            "ignore_batt_percent",
            "ignore_low_batt",
            "ignore_thermal_protect",
            "ignore_pbm_limited",
        ] {
            write_val(&format!("{} 1", key), gpu_limit, true);
        }
    }

    let _ = Command::new("cmd")
        .args(["thermalservice", "override-status", "0"])
        .status();

    write_val("stop 1", "/proc/mtk_batoc_throttling/battery_oc_protect_stop", true);

    log_verbose("Thermal is disabled");
}

fn bracket_key(line: &str) -> Option<String> {
    let start = line.find('[')?;
    let rest = &line[start + 1..];
    let end = rest.find(']')?;
    Some(rest[..end].to_string())
}

fn truncate_tracing_files(dir: &std::path::Path) {
    let Ok(read_dir) = std::fs::read_dir(dir) else {
        return;
    };
    for entry in read_dir.flatten() {
        let path = entry.path();
        if path.is_dir() {
            truncate_tracing_files(&path);
        } else if path.file_name().and_then(|n| n.to_str()) == Some("trace") {
            let _ = std::fs::write(&path, "");
        }
    }
}

fn apply_disable_trace() {
    log_info("Applying disable trace");

    truncate_tracing_files(std::path::Path::new("/sys/kernel/tracing"));

    write_val("0", "/sys/kernel/tracing/options/overwrite", true);
    write_val("0", "/sys/kernel/tracing/options/record-tgids", true);

    let trace_cmds: Vec<&[&str]> = vec![
        &["accessibility", "stop-trace"],
        &["input_method", "tracing", "stop"],
        &["window", "tracing", "stop"],
        &["window", "tracing", "size", "0"],
        &["migard", "dump-trace", "false"],
        &["migard", "start-trace", "false"],
        &["migard", "stop-trace", "true"],
        &["migard", "trace-buffer-size", "0"],
    ];

    for args in &trace_cmds {
        cmd_split(args);
    }
}

fn apply_logd() {
    if state_enabled(&get_state("persist.sys.azenithconf.logd")) {
        log_info("Applying Kill Logd");
        for logger in &LIST_LOGGER {
            let _ = Command::new("stop").arg(logger).status();
        }
    } else {
        for logger in &LIST_LOGGER {
            let _ = Command::new("start").arg(logger).status();
        }
    }
}

pub fn prefsettings() -> bool {
    let walt_state = get_state("persist.sys.azenithconf.walttunes");
    let dthermal_state = get_state("persist.sys.azenithconf.DThermal");
    let sfl_state = get_state("persist.sys.azenithconf.SFL");
    let malisched_state = get_state("persist.sys.azenithconf.malisched");
    let fpsged_state = get_state("persist.sys.azenithconf.fpsged");
    let schedtunes_state = get_state("persist.sys.azenithconf.schedtunes");
    let justintime_state = get_state("persist.sys.azenithconf.justintime");
    let disabletrace_state = get_state("persist.sys.azenithconf.disabletrace");

    if state_enabled(&justintime_state) {
        apply_jit();
    }

    if state_enabled(&schedtunes_state) {
        apply_schedtunes();
    }

    if state_enabled(&walt_state) {
        apply_walt();
    }

    if state_enabled(&fpsged_state) {
        apply_fpsged();
    }

    if state_enabled(&malisched_state) {
        apply_mali_sched();
    }

    if state_enabled(&sfl_state) {
        apply_surfaceflinger();
    }

    if state_enabled(&dthermal_state) {
        apply_disable_thermal();
    }

    if state_enabled(&disabletrace_state) {
        apply_disable_trace();
    }

    apply_logd();

    true
}