use std::fs;
use std::io;
use std::path::{ Path, PathBuf };
use std::time::Duration;
use super::android_ffi::Logger;
use super::constants::SMOOTHING_ALPHA;

// ============================================================================
// COOLING DEVICES
// ============================================================================

#[derive(Debug, Clone)]
pub struct CoolingDevice {
    pub path: PathBuf,
    pub device_type: String,
    pub max_state: i32,
    pub current_blended_intensity: f32,
}

impl CoolingDevice {
    pub fn enumerate(logger: &Logger) -> Vec<Self> {
        let mut devices = Vec::new();
        if let Ok(entries) = fs::read_dir("/sys/class/thermal") {
            for entry in entries.flatten() {
                let path = entry.path();
                if entry.file_name().to_string_lossy().starts_with("cooling_device") {
                    if let Some(device) = Self::load_device(&path, logger) {
                        devices.push(device);
                    }
                }
            }
        }
        logger.info(&format!("Found {} cooling devices", devices.len()));
        devices
    }

    pub fn load_device(path: &Path, logger: &Logger) -> Option<Self> {
        let device_type = fs::read_to_string(path.join("type")).ok()?.trim().to_string();
        let max_state = fs::read_to_string(path.join("max_state")).ok()?.trim().parse().ok()?;

        let lower = device_type.to_lowercase();

        let acceptable_types = [
            "cpu",
            "thermal-devfreq",
            "gpu",
            "devfreq",
            "vcore",
            "thermal-cpufreq",
        ];
        let unacceptable_sub_strings = [
            "backlight",
            "cam",
            "tx-pwr",
            "shutdown",
            "sysrst",
            "mdoff",
        ];

        let is_acceptable = acceptable_types.iter().any(|t| lower.contains(t));
        let is_unacceptable = unacceptable_sub_strings.iter().any(|s| lower.contains(s));

        if !(is_acceptable && !is_unacceptable) {
            return None;
        }

        let initial_intensity = fs
            ::read_to_string(path.join("cur_state"))
            .ok()
            .and_then(|s| s.trim().parse::<i32>().ok())
            .map(|s| if max_state > 0 { (s as f32) / (max_state as f32) } else { 0.0 })
            .unwrap_or(0.0);

        logger.debug(
            &format!(
                "Loaded: {} (max: {}, initial: {:.2})",
                device_type,
                max_state,
                initial_intensity
            )
        );
        Some(CoolingDevice {
            path: path.to_path_buf(),
            device_type,
            max_state,
            current_blended_intensity: initial_intensity,
        })
    }

    pub fn apply_intensity(&mut self, target_intensity: f32, logger: &Logger) -> io::Result<()> {
        let diff = target_intensity - self.current_blended_intensity;
        self.current_blended_intensity += diff * SMOOTHING_ALPHA;

        if (self.current_blended_intensity - target_intensity).abs() < 0.01 {
            self.current_blended_intensity = target_intensity;
        }

        let target_state = (
            (self.max_state as f32) * self.current_blended_intensity
        ).round() as i32;
        let cur_state_path = self.path.join("cur_state");

        if let Ok(state_str) = fs::read_to_string(&cur_state_path) {
            if let Ok(current_state) = state_str.trim().parse::<i32>() {
                if current_state == target_state {
                    return Ok(());
                }
            }
        }

        for attempt in 0..3 {
            match fs::write(&cur_state_path, format!("{}", target_state)) {
                Ok(_) => {
                    return Ok(());
                }
                Err(_e) if attempt < 2 => {
                    std::thread::sleep(Duration::from_millis(10));
                }
                Err(e) => {
                    logger.error(&format!("Failed to write {}: {}", self.device_type, e));
                    return Err(e);
                }
            }
        }

        Ok(())
    }
}
