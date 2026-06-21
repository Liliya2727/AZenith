use std::fs::{ self, File };
use std::io::{ self, Read };
use std::path::PathBuf;
use std::collections::VecDeque;
use serde::{ Deserialize, Serialize };
use bincode::{ config, serde::decode_from_slice, serde::encode_to_vec };

use super::android_ffi::Logger;
use super::constants::*;
use super::utils::get_monotonic_time;

// ============================================================================
// AI CONTEXT & TARGET SELECTOR
// ============================================================================

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
pub enum OperationalMode {
    Idle, // Screen off or low load
    Sustained, // Consistent load (Gaming/Video)
    Boost, // High Variance (UI Interaction)
    Critical, // Overheating
}

#[derive(Serialize, Deserialize, Clone)]
pub struct ThermalEvent {
    pub timestamp: u64,
    pub temperature: i16,
    pub load_variance: f32,
    pub target_temp: i16,
    pub pid_output: f32,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct ThermalAI {
    pub events: VecDeque<ThermalEvent>,

    pub kp: f32,
    pub ki: f32,
    pub kd: f32,

    pub current_mode: OperationalMode,
    pub last_target: i32,

    #[serde(skip)]
    data_path: PathBuf,
}

impl ThermalAI {
    pub fn new(data_path: PathBuf) -> Self {
        ThermalAI {
            events: VecDeque::with_capacity(HISTORY_WINDOW),
            kp: PID_KP,
            ki: PID_KI,
            kd: PID_KD,
            current_mode: OperationalMode::Idle,
            last_target: TARGET_TEMP_IDLE,
            data_path,
        }
    }

    pub fn load(base_path: PathBuf) -> (Self, bool) {
        let file_path = base_path.join(LEARNING_DATA_FILENAME);
        if let Ok(mut file) = File::open(&file_path) {
            let mut bytes = Vec::new();
            if file.read_to_end(&mut bytes).is_ok() {
                let config = config::standard();
                if let Ok((mut data, _)) = decode_from_slice::<ThermalAI, _>(&bytes, config) {
                    data.data_path = base_path;
                    return (data, false);
                }
            }
        }
        (Self::new(base_path), true)
    }

    pub fn save(&self) -> io::Result<()> {
        let config = config::standard();
        let bytes = encode_to_vec(self, config).map_err(|e|
            io::Error::new(io::ErrorKind::Other, e.to_string())
        )?;

        let file_path = self.data_path.join(LEARNING_DATA_FILENAME);
        let tmp_path = file_path.with_extension("dat.tmp");

        fs::write(&tmp_path, &bytes)?;
        fs::rename(&tmp_path, file_path)?;
        Ok(())
    }

    pub fn determine_target(
        &mut self,
        current_temp: i32,
        load_variance: f32,
        is_screen_on: bool,
        logger: &Logger
    ) -> i32 {
        if current_temp >= TARGET_TEMP_CRITICAL {
            self.current_mode = OperationalMode::Critical;
            return TARGET_TEMP_IDLE; // Force cool down
        }

        let (new_mode, target) = if !is_screen_on {
            (OperationalMode::Idle, TARGET_TEMP_IDLE)
        } else {
            if load_variance > VAR_HIGH_THRESHOLD {
                (OperationalMode::Boost, TARGET_TEMP_BOOST)
            } else if load_variance < VAR_LOW_THRESHOLD && current_temp > TARGET_TEMP_IDLE {
                (OperationalMode::Sustained, TARGET_TEMP_SUSTAINED)
            } else {
                (OperationalMode::Sustained, TARGET_TEMP_SUSTAINED)
            }
        };
        if self.current_mode != new_mode {
            logger.info(
                &format!(
                    "AI Mode Change: {:?} -> {:?} (Var: {:.3}) Target: {} -> {}",
                    self.current_mode,
                    new_mode,
                    load_variance,
                    self.last_target,
                    target
                )
            );
            self.current_mode = new_mode;
        }

        self.last_target = target;
        target
    }

    pub fn record_event(&mut self, temp: i32, variance: f32, target: i32, pid_out: f32) {
        if self.events.len() >= HISTORY_WINDOW {
            self.events.pop_front();
        }
        self.events.push_back(ThermalEvent {
            timestamp: get_monotonic_time(),
            temperature: temp as i16,
            load_variance: variance,
            target_temp: target as i16,
            pid_output: pid_out,
        });
    }
}
