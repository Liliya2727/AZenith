use std::collections::VecDeque;
use std::fs;
use std::io;
use serde::{ Deserialize, Serialize };

use super::android_ffi::Logger;
use super::learning::ThermalAI;
use super::utils::TemperatureFilter;
use super::context::ExternalContext;

// ============================================================================
// SIMULATOR FOR TESTING
// ============================================================================

#[derive(Debug, Clone, Serialize, Deserialize)]
struct SimulationTrace {
    timestamp: u64,
    temperature: i32,
    cpu_load: f32,
}

pub struct ThermalSimulator {
    traces: VecDeque<SimulationTrace>,
    learning_data: ThermalAI,
    logger: Logger,
}

impl ThermalSimulator {
    pub fn new(trace_file: &str) -> io::Result<Self> {
        let logger = Logger::new();
        logger.info(&format!("Loading simulation traces from {}", trace_file));

        let content = fs::read_to_string(trace_file)?;
        let traces: VecDeque<SimulationTrace> = serde_json
            ::from_str(&content)
            .map_err(|e| io::Error::new(io::ErrorKind::InvalidData, e))?;

        logger.info(&format!("Loaded {} trace entries", traces.len()));

        Ok(ThermalSimulator {
            traces,
            learning_data: ThermalAI::new(std::path::PathBuf::from("/tmp")),
            logger,
        })
    }

    pub fn run_simulation(&mut self) {
        self.logger.info("Starting deterministic simulation");
        let mut temp_filter = TemperatureFilter::new(5);
        let mut last_time = 0;

        for trace in &self.traces {
            temp_filter.add(trace.temperature);
            let smoothed_temp = temp_filter.get_ewma(0.3);

            let variance = if trace.cpu_load > 0.8 { 0.02 } else { 0.2 };
            let target = self.learning_data.determine_target(
                smoothed_temp,
                variance,
                true,
                &self.logger
            );

            if trace.timestamp > last_time {
                self.logger.info(
                    &format!("[Sim {}s] T:{} -> Target:{}", trace.timestamp, smoothed_temp, target)
                );
                last_time = trace.timestamp;
            }
        }
    }
}
