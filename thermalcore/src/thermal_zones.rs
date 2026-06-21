use std::fs;
use std::path::PathBuf;
use super::android_ffi::Logger;
use super::constants::{
    PROP_GRADIENT_ALERT_THRESHOLD,
    DEFAULT_GRADIENT_ALERT_THRESHOLD,
    GRADIENT_HIGH_SUSTAINED_SECS,
    GRADIENT_EMA_ALPHA,
};
use super::utils::{ get_int_property, get_monotonic_time };

// ============================================================================
// THERMAL ZONE FUSION
// ============================================================================

pub struct ThermalZone {
    pub path: PathBuf,
    pub zone_type: String,
    pub last_temp: Option<i32>,
}

impl ThermalZone {
    pub fn enumerate(logger: &Logger) -> Vec<Self> {
        let mut zones = Vec::new();
        if let Ok(entries) = fs::read_dir("/sys/class/thermal") {
            for entry in entries.flatten() {
                let path = entry.path();
                if entry.file_name().to_string_lossy().starts_with("thermal_zone") {
                    if let Ok(zone_type) = fs::read_to_string(path.join("type")) {
                        zones.push(ThermalZone {
                            path: path.clone(),
                            zone_type: zone_type.trim().to_string(),
                            last_temp: None,
                        });
                    }
                }
            }
        }
        logger.info(&format!("Found {} thermal zones", zones.len()));
        zones
    }

    pub fn read_temp(&mut self) -> Option<i32> {
        if let Ok(temp_str) = fs::read_to_string(self.path.join("temp")) {
            if let Ok(temp) = temp_str.trim().parse::<i32>() {
                if temp > 200000 {
                    return None;
                }
                self.last_temp = Some(temp);
                return Some(temp);
            }
        }
        None
    }
}

pub struct ThermalFusion {
    pub zones: Vec<ThermalZone>,
    pub cpu_zone_indices: Vec<usize>,
    pub avg_gradient: f32,
    gradient_alert_threshold: i32,
    high_gradient_start_time: Option<u64>,
}

impl ThermalFusion {
    pub fn new(logger: &Logger) -> Self {
        let zones = ThermalZone::enumerate(logger);
        let cpu_zone_indices: Vec<usize> = zones
            .iter()
            .enumerate()
            .filter_map(|(i, z)| {
                let lower = z.zone_type.to_lowercase();
                if lower.contains("cpu") || lower.contains("soc") || lower.contains("gpu") {
                    Some(i)
                } else {
                    None
                }
            })
            .collect();

        logger.debug(&format!("CPU-related zones: {:?}", cpu_zone_indices));

        let gradient_alert_threshold = get_int_property(
            PROP_GRADIENT_ALERT_THRESHOLD,
            DEFAULT_GRADIENT_ALERT_THRESHOLD
        );
        logger.info(
            &format!(
                "Gradient alert threshold set to: {}.{}°C",
                gradient_alert_threshold / 10,
                gradient_alert_threshold % 10
            )
        );

        ThermalFusion {
            zones,
            cpu_zone_indices,
            avg_gradient: 100.0,
            gradient_alert_threshold,
            high_gradient_start_time: None,
        }
    }

    pub fn get_max_cpu_temp(&mut self) -> Option<i32> {
        self.cpu_zone_indices
            .iter()
            .filter_map(|&i| self.zones.get_mut(i).and_then(|z| z.read_temp()))
            .max()
    }

    pub fn correlate_with_battery(&mut self, battery_temp: i32, logger: &Logger) -> (bool, f32) {
        if let Some(cpu_max) = self.get_max_cpu_temp() {
            let internal_temp = cpu_max;
            let batt_temp = battery_temp;
            let gradient = (internal_temp - batt_temp) as f32;

            self.avg_gradient += GRADIENT_EMA_ALPHA * (gradient - self.avg_gradient);

            let now = get_monotonic_time();
            if self.avg_gradient > (self.gradient_alert_threshold as f32) {
                if let Some(start_time) = self.high_gradient_start_time {
                    if now.saturating_sub(start_time) > GRADIENT_HIGH_SUSTAINED_SECS {
                        logger.warn(
                            &format!(
                                "Thermal runaway alert: Avg gradient {:.1}°C (CPU:{}°C, Batt:{}°C) exceeds {}.{}°C for >{}s",
                                self.avg_gradient / 10.0,
                                internal_temp / 10,
                                batt_temp / 10,
                                self.gradient_alert_threshold / 10,
                                self.gradient_alert_threshold % 10,
                                GRADIENT_HIGH_SUSTAINED_SECS
                            )
                        );

                        self.high_gradient_start_time = Some(now);
                    }
                } else {
                    self.high_gradient_start_time = Some(now);
                }
            } else {
                self.high_gradient_start_time = None;
            }
        }

        (true, self.avg_gradient)
    }
}
