use std::fs::File;
use std::io::{ Read, Seek, SeekFrom };
use std::collections::{ HashMap, VecDeque };
use super::utils::get_monotonic_time;
use super::android_ffi::Logger;
use super::constants::CPU_LOAD_WINDOW;

// ============================================================================
// CPU STATISTICS STRUCTS
// ============================================================================

#[derive(Debug, Default, Clone, Copy)]
struct ProcStatValues {
    user: u64,
    nice: u64,
    system: u64,
    idle: u64,
    iowait: u64,
    irq: u64,
    softirq: u64,
    steal: u64,
    guest: u64,
    guest_nice: u64,
}

impl ProcStatValues {
    fn from_line(line: &str) -> Option<Self> {
        let mut parts = line.split_whitespace();
        parts.next()?; // Skip "cpu"

        let mut stat = ProcStatValues::default();
        stat.user = parts
            .next()
            .and_then(|v| v.parse().ok())
            .unwrap_or(0);
        stat.nice = parts
            .next()
            .and_then(|v| v.parse().ok())
            .unwrap_or(0);
        stat.system = parts
            .next()
            .and_then(|v| v.parse().ok())
            .unwrap_or(0);
        stat.idle = parts
            .next()
            .and_then(|v| v.parse().ok())
            .unwrap_or(0);
        stat.iowait = parts
            .next()
            .and_then(|v| v.parse().ok())
            .unwrap_or(0);
        stat.irq = parts
            .next()
            .and_then(|v| v.parse().ok())
            .unwrap_or(0);
        stat.softirq = parts
            .next()
            .and_then(|v| v.parse().ok())
            .unwrap_or(0);
        stat.steal = parts
            .next()
            .and_then(|v| v.parse().ok())
            .unwrap_or(0);
        stat.guest = parts
            .next()
            .and_then(|v| v.parse().ok())
            .unwrap_or(0);
        stat.guest_nice = parts
            .next()
            .and_then(|v| v.parse().ok())
            .unwrap_or(0);

        Some(stat)
    }

    fn get_idle_time(&self) -> u64 {
        self.idle.saturating_add(self.iowait)
    }

    fn get_total_time(&self) -> u64 {
        self.user
            .saturating_add(self.nice)
            .saturating_add(self.system)
            .saturating_add(self.idle)
            .saturating_add(self.iowait)
            .saturating_add(self.irq)
            .saturating_add(self.softirq)
            .saturating_add(self.steal)
            .saturating_add(self.guest)
            .saturating_add(self.guest_nice)
    }
}

#[derive(Debug, Default, Clone, Copy)]
struct CpuDeltaState {
    last_total: u64,
    last_idle: u64,
}

#[derive(Debug, Clone, Copy)]
pub struct CpuFeatures {
    pub cpu_load: f32,
    pub core_load_max: f32,
    pub load_variance: f32,
}

// ============================================================================
// CPU MONITOR
// ============================================================================

pub struct CpuMonitor {
    last_sample_time: u64,
    last_features: CpuFeatures,
    proc_stat_file: Option<File>,
    read_buffer: String,

    agg_delta_state: CpuDeltaState,
    core_delta_states: HashMap<String, CpuDeltaState>,

    load_history: VecDeque<f32>,
}

impl CpuMonitor {
    pub fn new() -> Self {
        let file = File::open("/proc/stat").ok();
        CpuMonitor {
            last_sample_time: 0,
            last_features: CpuFeatures { cpu_load: 0.0, core_load_max: 0.0, load_variance: 0.0 },
            proc_stat_file: file,
            read_buffer: String::with_capacity(2048),

            agg_delta_state: CpuDeltaState::default(),
            core_delta_states: HashMap::new(),
            load_history: VecDeque::with_capacity(CPU_LOAD_WINDOW),
        }
    }

    pub fn get_features(&mut self, logger: &Logger) -> CpuFeatures {
        let now = get_monotonic_time();
        if now.saturating_sub(self.last_sample_time) < 1 {
            return self.last_features;
        }
        self.last_sample_time = now;

        match self.read_and_parse_stats(logger) {
            Ok(features) => {
                self.last_features = features;
            }
            Err(e) => {
                logger.debug(&format!("Failed to parse /proc/stat: {}", e));
                self.proc_stat_file = File::open("/proc/stat").ok();
            }
        }

        self.last_features
    }

    fn calculate_load(
        current_stat: &ProcStatValues,
        last_state: &CpuDeltaState
    ) -> (f32, CpuDeltaState) {
        let current_total = current_stat.get_total_time();
        let current_idle = current_stat.get_idle_time();

        let new_state = CpuDeltaState {
            last_total: current_total,
            last_idle: current_idle,
        };

        if last_state.last_total == 0 {
            return (0.0, new_state);
        }

        let total_delta = current_total.saturating_sub(last_state.last_total);
        let idle_delta = current_idle.saturating_sub(last_state.last_idle);

        let load = if total_delta == 0 {
            0.0
        } else {
            1.0 - (idle_delta as f32) / (total_delta as f32)
        };

        (load.clamp(0.0, 1.0), new_state)
    }

    fn calculate_variance(&self) -> f32 {
        if self.load_history.len() < 2 {
            return 0.0;
        }
        let mean: f32 = self.load_history.iter().sum::<f32>() / (self.load_history.len() as f32);
        let variance: f32 =
            self.load_history
                .iter()
                .map(|value| {
                    let diff = mean - value;
                    diff * diff
                })
                .sum::<f32>() / (self.load_history.len() as f32);

        variance.sqrt()
    }

    fn read_and_parse_stats(&mut self, logger: &Logger) -> Result<CpuFeatures, &'static str> {
        let content = self.read_proc_stat_file(logger)?;

        let mut agg_load = 0.0;
        let mut max_core_load = 0.0;
        let mut found_agg = false;

        for line in content.lines() {
            if line.starts_with("cpu ") {
                if let Some(current_stat) = ProcStatValues::from_line(line) {
                    let (load, new_state) = Self::calculate_load(
                        &current_stat,
                        &self.agg_delta_state
                    );
                    self.agg_delta_state = new_state;
                    agg_load = load;
                    found_agg = true;
                }
            } else if line.starts_with("cpu") {
                let core_id = line.split_whitespace().next().unwrap_or("").to_string();
                if core_id.is_empty() {
                    continue;
                }

                if let Some(current_stat) = ProcStatValues::from_line(line) {
                    let last_state = self.core_delta_states.entry(core_id).or_default();
                    let (load, new_state) = Self::calculate_load(&current_stat, last_state);
                    *last_state = new_state;
                    if load > max_core_load {
                        max_core_load = load;
                    }
                }
            } else if line.starts_with("intr") {
                break;
            }
        }

        if !found_agg {
            return Err("Could not find aggregate 'cpu ' line");
        }

        if self.load_history.len() >= CPU_LOAD_WINDOW {
            self.load_history.pop_front();
        }
        self.load_history.push_back(agg_load);

        Ok(CpuFeatures {
            cpu_load: agg_load,
            core_load_max: max_core_load,
            load_variance: self.calculate_variance(),
        })
    }

    fn read_proc_stat_file(&mut self, _logger: &Logger) -> Result<String, &'static str> {
        if self.proc_stat_file.is_none() {
            self.proc_stat_file = File::open("/proc/stat").ok();
            if self.proc_stat_file.is_none() {
                return Err("Failed to open /proc/stat");
            }
        }

        if let Some(file) = self.proc_stat_file.as_mut() {
            self.read_buffer.clear();
            if file.seek(SeekFrom::Start(0)).is_err() {
                self.proc_stat_file = File::open("/proc/stat").ok();
                return Err("Failed to seek /proc/stat");
            }
            if file.read_to_string(&mut self.read_buffer).is_err() {
                self.proc_stat_file = None;
                return Err("Failed to read /proc/stat");
            }
            return Ok(self.read_buffer.clone());
        }

        Err("File handle is None")
    }
}
