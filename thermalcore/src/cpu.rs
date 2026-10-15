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

/// Aggregate + per-core snapshot from a single `/proc/stat` sample.
/// Unlike [`CpuFeatures`], this carries the per-core busy fractions so a
/// 1 Hz dataset collector can log every core without re-reading procfs.
#[derive(Debug, Clone, Default)]
pub struct CpuSnapshot {
    pub cpu_load: f32,
    pub core_load_max: f32,
    pub load_variance: f32,
    /// `(core id, busy fraction 0.0..1.0)`, e.g. `("cpu0", 0.42)`,
    /// sorted by core index. Empty until the first successful sample.
    pub per_core: Vec<(String, f32)>,
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

    /// Full per-core snapshot for 1 Hz dataset logging. Reuses the same
    /// `/proc/stat` read and delta math as [`Self::get_features`], but also
    /// returns every accepted per-core busy fraction for raw storage.
    pub fn get_snapshot(&mut self, logger: &Logger) -> CpuSnapshot {
        let content = match self.refresh_snapshot(logger) {
            Some(c) => c,
            None => {
                return CpuSnapshot {
                    cpu_load: self.last_features.cpu_load,
                    core_load_max: self.last_features.core_load_max,
                    load_variance: self.last_features.load_variance,
                    per_core: Vec::new(),
                };
            }
        };

        let mut features = match self.aggregate_from(&content) {
            Ok(f) => {
                self.last_features = f;
                f
            }
            Err(_) => self.last_features,
        };

        let mut per_core: Vec<(String, f32)> = content
            .lines()
            .filter(|line| {
                line.starts_with("cpu") &&
                    !line.starts_with("cpu ") &&
                    !line.starts_with("intr")
            })
            .filter_map(|line| {
                let core_id = line.split_whitespace().next()?.to_string();
                let stat = ProcStatValues::from_line(line)?;
                let last = self.core_delta_states.entry(core_id.clone()).or_default();
                let (load, state) = Self::calculate_load(&stat, last);
                *last = state;
                Some((core_id, load))
            })
            .collect();
        per_core.sort_by(|a, b| a.0.cmp(&b.0));

        if !per_core.is_empty() {
            features.core_load_max = per_core
                .iter()
                .map(|(_, load)| *load)
                .fold(features.core_load_max, f32::max);
        }

        CpuSnapshot {
            cpu_load: features.cpu_load,
            core_load_max: features.core_load_max,
            load_variance: features.load_variance,
            per_core,
        }
    }

    pub fn get_features(&mut self, logger: &Logger) -> CpuFeatures {
        if self.refresh_and_parse(logger).is_err() {
            return self.last_features;
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

    fn refresh_and_parse(&mut self, logger: &Logger) -> Result<CpuFeatures, &'static str> {
        let now = get_monotonic_time();
        if now.saturating_sub(self.last_sample_time) < 1 {
            return Err("Throttled: sample interval not elapsed");
        }
        self.last_sample_time = now;
        match self.read_and_parse_stats(logger) {
            Ok(features) => {
                self.last_features = features;
                Ok(features)
            }
            Err(e) => {
                logger.debug(&format!("Failed to parse /proc/stat: {}", e));
                self.proc_stat_file = File::open("/proc/stat").ok();
                self.read_buffer.clear();
                Err(e)
            }
        }
    }

    /// Same 1 s throttle as [`Self::get_features`]; returns the raw
    /// `/proc/stat` bytes on success without touching derived features.
    fn refresh_snapshot(&mut self, logger: &Logger) -> Option<String> {
        let now = get_monotonic_time();
        if now.saturating_sub(self.last_sample_time) < 1 {
            return None;
        }
        self.last_sample_time = now;
        match self.read_proc_stat_file(logger) {
            Ok(_) => Some(self.read_buffer.clone()),
            Err(e) => {
                logger.debug(&format!("Failed to parse /proc/stat: {}", e));
                self.proc_stat_file = File::open("/proc/stat").ok();
                self.read_buffer.clear();
                None
            }
        }
    }

    /// Aggregate `cpu ` line + variance history, without the per-core walk.
    /// Used by [`Self::get_snapshot`] so the collector can keep the existing
    /// delta math while storing every per-core fraction itself.
    fn aggregate_from(&mut self, content: &str) -> Result<CpuFeatures, &'static str> {
        let mut agg_load = 0.0;
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
                break;
            // "/proc/stat" header lines before the aggregate ("btime", …)
            // carry no counters; lines after its first line start are the
            // per-core rows, handled by get_snapshot's own walk.
            } else if !line.is_empty() && !line.starts_with("cpu") {
                continue;
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
            core_load_max: 0.0,
            load_variance: self.calculate_variance(),
        })
    }

    /// Full `/proc/stat` parse (aggregate + per-core) for the daemon's
    /// control tick. [`Self::get_snapshot`] instead uses [`Self::aggregate_from`]
    /// plus its own per-core walk so the collector can keep the raw fractions.
    fn read_and_parse_stats(&mut self, _logger: &Logger) -> Result<CpuFeatures, &'static str> {
        let content = self.read_proc_stat_file(_logger)?;

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
