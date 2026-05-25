use std::collections::VecDeque;
use super::android_ffi::Logger;

// ============================================================================
// MITIGATION EFFECTIVENESS TRACKER
// ============================================================================

pub struct MitigationMetrics {
    pub start_temp: i32,
    pub start_cpu_load: f32,
    pub intensity: f32,
}

pub struct EffectivenessTracker {
    pub current_mitigation: Option<MitigationMetrics>,
    pub history: VecDeque<(f32, f32)>, // (thermal_gain, perf_loss)
}

impl EffectivenessTracker {
    pub fn new() -> Self {
        EffectivenessTracker {
            current_mitigation: None,
            history: VecDeque::with_capacity(20),
        }
    }

    pub fn start_mitigation(&mut self, intensity: f32, temp: i32, cpu_load: f32) {
        if intensity > 0.05 {
            self.current_mitigation = Some(MitigationMetrics {
                start_temp: temp,
                start_cpu_load: cpu_load,
                intensity,
            });
        }
    }

    pub fn end_mitigation(&mut self, end_temp: i32, end_cpu_load: f32, logger: &Logger) {
        if let Some(metrics) = self.current_mitigation.take() {
            let thermal_gain = ((metrics.start_temp - end_temp) as f32) / 10.0;
            let perf_loss = (metrics.start_cpu_load - end_cpu_load).abs();

            if self.history.len() >= 20 {
                self.history.pop_front();
            }
            self.history.push_back((thermal_gain, perf_loss));

            if thermal_gain > 1.0 {
                logger.debug(
                    &format!(
                        "Mitigation (Int: {:.2}) ended: ΔT={:.1}°C, ΔCPU={:.2}",
                        metrics.intensity,
                        thermal_gain,
                        perf_loss
                    )
                );
            }
        }
    }
}
