use crate::utils::*;  use std::path::Path;  use std::thread;

pub fn snapdragon_balance() {
    let govs = [
        ("/*cpu-ddr-latfloor*", "compute"),
        ("/*cpu*-lat", "mem_latency"),
        ("/*cpu-cpu-ddr-bw", "bw_hwmon"),
        ("/*cpu-cpu-llcc-bw", "bw_hwmon"),
        ("/*gpubw*", "bw_vbif")
    ];

    for (pattern, gov) in govs {
        if let Ok(paths) = glob::glob(&format!("/sys/class/devfreq{}", pattern)) {
            for path in paths.flatten() {
                zeshia_def(gov, &format!("{}/governor", path.display()));
            }
        }
    }

    for bus in &["LLCC", "L3", "DDR", "DDRQOS"] {
        let base = format!("/sys/devices/system/cpu/bus_dcvs/{}", bus);
        if Path::new(&base).exists() {
            let avail = format!("{}/available_frequencies", base);
            if let (Some(max), Some(min)) = (which_maxfreq(&avail), which_minfreq(&avail)) {
                if let Ok(paths) = glob::glob(&format!("{}/*/max_freq", base)) {
                    for p in paths.flatten() {
                        zeshia_def(&max.to_string(), p.to_str().unwrap());
                    }
                }
                if let Ok(paths) = glob::glob(&format!("{}/*/min_freq", base)) {
                    for p in paths.flatten() {
                        zeshia_def(&min.to_string(), p.to_str().unwrap());
                    }
                }
            }
        }
    }

    let gpu_path = "/sys/class/kgsl/kgsl-3d0/devfreq";
    if Path::new(gpu_path).exists() {
        let freqs = read_freqs(&format!("{}/available_frequencies", gpu_path));
        if freqs.len() >= 2 {
            zeshia_def(&freqs[1].to_string(), &format!("{}/min_freq", gpu_path));
            zeshia_def(&freqs[freqs.len() - 1].to_string(), &format!("{}/max_freq", gpu_path));
        } else if freqs.len() == 1 {
            zeshia_def(&freqs[0].to_string(), &format!("{}/min_freq", gpu_path));
            zeshia_def(&freqs[0].to_string(), &format!("{}/max_freq", gpu_path));
        }
    }

    zeshia_def("1", "/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost");
}

pub fn snapdragon_performance() {
    // Jalankan pencarian devfreq di thread terpisah agar tidak membuat device lag/freeze
    thread::spawn(|| {
        let govs = [
            ("/*cpu-ddr-latfloor*", "performance"),
            ("/*cpu*-lat", "performance"),
            ("/*cpu-cpu-ddr-bw", "performance"),
            ("/*cpu-cpu-llcc-bw", "performance"),
            ("/*gpubw*", "performance")
        ];

        for (pattern, gov) in govs {
            if let Ok(paths) = glob::glob(&format!("/sys/class/devfreq{}", pattern)) {
                for path in paths.flatten() {
                    zeshia_def(gov, path.to_str().unwrap());
                }
            }
        }
    });

    // Jalankan urusan bus DCVS di thread terpisah juga
    thread::spawn(|| {
        for bus in &["LLCC", "L3", "DDR", "DDRQOS"] {
            let base = format!("/sys/devices/system/cpu/bus_dcvs/{}", bus);
            if Path::new(&base).exists() {
                let avail = format!("{}/available_frequencies", base);
                if let Some(max) = which_maxfreq(&avail) {
                    if let Ok(paths) = glob::glob(&format!("{}/*/max_freq", base)) {
                        for p in paths.flatten() { zeshia_def(&max.to_string(), p.to_str().unwrap()); }
                    }
                    if let Ok(paths) = glob::glob(&format!("{}/*/min_freq", base)) {
                        for p in paths.flatten() { zeshia_def(&max.to_string(), p.to_str().unwrap()); }
                    }
                }
            }
        }
    });

    // GPU core clock tetap di main thread tidak apa-apa karena jalurnya absolut (cepat)
    let gpu_path = "/sys/class/kgsl/kgsl-3d0/devfreq";
    if Path::new(gpu_path).exists() {
        if let Some(freq) = which_maxfreq(&format!("{}/available_frequencies", gpu_path)) {
            zeshia_def(&freq.to_string(), &format!("{}/min_freq", gpu_path));
            zeshia_def(&freq.to_string(), &format!("{}/max_freq", gpu_path));
        }
    }

    zeshia_def("3", "/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost");
}

pub fn snapdragon_powersave() {
    let govs = [
        ("/*cpu-ddr-latfloor*", "powersave"),
        ("/*cpu*-lat", "powersave"),
        ("/*cpu-cpu-ddr-bw", "powersave"),
        ("/*cpu-cpu-llcc-bw", "powersave"),
        ("/*gpubw*", "powersave")
    ];

    for (pattern, gov) in govs {
        if let Ok(paths) = glob::glob(&format!("/sys/class/devfreq{}", pattern)) {
            for path in paths.flatten() {
                zeshia_def(gov, &format!("{}/governor", path.display()));
            }
        }
    }

    for bus in &["LLCC", "L3", "DDR", "DDRQOS"] {
        let base = format!("/sys/devices/system/cpu/bus_dcvs/{}", bus);
        if Path::new(&base).exists() {
            let avail = format!("{}/available_frequencies", base);
            if let Some(min) = which_minfreq(&avail) {
                if let Ok(paths) = glob::glob(&format!("{}/*/max_freq", base)) {
                    for p in paths.flatten() { zeshia_def(&min.to_string(), p.to_str().unwrap()); }
                }
                if let Ok(paths) = glob::glob(&format!("{}/*/min_freq", base)) {
                    for p in paths.flatten() { zeshia_def(&min.to_string(), p.to_str().unwrap()); }
                }
            }
        }
    }

    let gpu_path = "/sys/class/kgsl/kgsl-3d0/devfreq";
    if Path::new(gpu_path).exists() {
        if let Some(freq) = which_minfreq(&format!("{}/available_frequencies", gpu_path)) {
            zeshia_def(&freq.to_string(), &format!("{}/min_freq", gpu_path));
            zeshia_def(&freq.to_string(), &format!("{}/max_freq", gpu_path));
        }
    }

    zeshia_def("0", "/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost");
}
