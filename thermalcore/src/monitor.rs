use std::io::{ self };
use std::os::unix::io::{ AsRawFd, BorrowedFd };
use std::path::Path;
use std::sync::{ atomic::{ AtomicBool, Ordering }, Arc };
use std::time::Duration;
use nix::poll::{ poll, PollFd, PollFlags, PollTimeout };
use inotify::{ Inotify, WatchMask };

use crate::android_ffi::Logger;
use crate::cooling::CoolingDevice;
use crate::dvfsrc::DvfsrcActuator;
use crate::learning::ThermalAI;
use crate::policy_manager::PolicyManager;
use crate::prediction::PredictiveModel;
use crate::thermal_zones::ThermalFusion;
use crate::utils::{ get_system_property, TemperatureFilter, get_thermal_path, get_data_path };
use crate::cpu::CpuMonitor;
use crate::context::{ ExternalContext, ScreenState };
use crate::effectiveness::EffectivenessTracker;
use crate::utils::{ get_monotonic_time };
use crate::constants::*;

// ============================================================================
// THERMAL MONITOR
// ============================================================================

enum RunMode {
    Inotify,
    Poll,
}

#[derive(Debug, Clone, Copy, PartialEq)]
enum ActuatorMode {
    Dvfsrc,
    Soft,
}

pub struct ThermalMonitor {
    pub logger: Logger,
    pub learning_data: ThermalAI,
    soft_devices: Vec<CoolingDevice>,
    broad_devices: Vec<CoolingDevice>,
    temp_filter: TemperatureFilter,
    cpu_monitor: CpuMonitor,
    last_temp: i32,
    last_loop_time: u64,
    last_log_time: u64,
    battery_temp_path: String,
    events_since_last_save: usize,
    last_save_time: u64,

    current_intensity: f32,

    external_context: ExternalContext,
    effectiveness_tracker: EffectivenessTracker,
    policy_manager: PolicyManager,

    predictive_model: PredictiveModel,
    predict_log_counter: usize,

    thermal_fusion: ThermalFusion,

    dvfsrc_actuator: DvfsrcActuator,
    actuator_mode: ActuatorMode,
}

impl ThermalMonitor {
    pub fn new() -> Self {
        let logger = Logger::new();
        logger.info("Rynx Thermal Core v3.1 [PID-Predictive+DVFSRC] - Initializing");

        let data_path = get_data_path();
        let thermal_path = get_thermal_path();

        // Split cooling devices into soft-safe (DVFSRC-compatible) and broad.
        // Soft = thermal-devfreq, cpu_adaptive, gpu, devfreq, vcore.
        // Broad = everything else accepted (cpuNN, mutt, etc.) — only used in
        // soft mode as fallback, never in dvfsrc mode.
        let all_devices = CoolingDevice::enumerate(&logger);
        let mut soft_devices = Vec::new();
        let mut broad_devices = Vec::new();
        for d in all_devices {
            if d.is_soft {
                soft_devices.push(d);
            } else {
                broad_devices.push(d);
            }
        }

        let dvfsrc_actuator = DvfsrcActuator::new(&logger);
        logger.info(
            &format!(
                "Cooling devices: {} soft-safe, {} broad (MTK HAL-compatible)",
                soft_devices.len(),
                broad_devices.len()
            )
        );

        // Read actuator mode from system property. Empty/unset triggers
        // auto-detection on first run: probe the DVFSRC path itself, then
        // fall back through soft devices to the generic broad-device path.
        let mode_str = get_system_property(PROP_ACTUATOR_MODE, "");
        let actuator_mode = if mode_str == ACTUATOR_MODE_DVFSRC {
            if dvfsrc_actuator.available {
                logger.info("Actuator: DVFSRC (coordinated SoC OPP)");
                ActuatorMode::Dvfsrc
            } else {
                logger.warn("DVFSRC requested via property but node unavailable, using SOFT fallback");
                ActuatorMode::Soft
            }
        } else if mode_str == ACTUATOR_MODE_SOFT {
            logger.info("Actuator: SOFT (thermal-devfreq + cpu_adaptive only)");
            ActuatorMode::Soft
        } else {
            // First run / prop empty: probe the DVFSRC path directly.
            if dvfsrc_actuator.available {
                logger.info("Actuator: auto-detected DVFSRC (coordinated SoC OPP)");
                ActuatorMode::Dvfsrc
            } else if !soft_devices.is_empty() {
                logger.warn("DVFSRC not found, using SOFT fallback");
                ActuatorMode::Soft
            } else {
                logger.warn(
                    "DVFSRC and soft cooling devices not found, using generic fallback"
                );
                ActuatorMode::Soft
            }
        };
        let (learning_data, is_new) = ThermalAI::load(data_path.clone());

        if is_new {
            logger.info("New AI Context Profile created.");
        }

        let external_context = ExternalContext::new();
        let thermal_fusion = ThermalFusion::new(&logger);
        let mut policy_manager = PolicyManager::new();

        // Clamp persisted gains to sane bounds. learning.dat can carry a
        // pathological PID state from a previous session; never trust it
        // blindly or the daemon will permanently over-react.
        let kp = learning_data.kp.clamp(PID_KP_MIN, PID_KP_MAX);
        let ki = learning_data.ki.clamp(PID_KI_MIN, PID_KI_MAX);
        let kd = learning_data.kd.clamp(PID_KD_MIN, PID_KD_MAX);
        policy_manager.update_params(kp, ki, kd);

        let now = get_monotonic_time();

        ThermalMonitor {
            logger,
            soft_devices,
            broad_devices,
            learning_data,
            temp_filter: TemperatureFilter::new(5),
            cpu_monitor: CpuMonitor::new(),
            last_temp: 0,
            last_loop_time: now,
            last_log_time: 0,
            battery_temp_path: thermal_path,
            events_since_last_save: 0,
            last_save_time: now,
            current_intensity: 0.0,
            external_context,
            effectiveness_tracker: EffectivenessTracker::new(),
            policy_manager,

            predictive_model: PredictiveModel::new(),
            predict_log_counter: 0,

            thermal_fusion,

            dvfsrc_actuator,
            actuator_mode,
        }
    }

    fn read_temperature(&self) -> io::Result<i32> {
        std::fs
            ::read_to_string(&self.battery_temp_path)?
            .trim()
            .parse()
            .map_err(|e| io::Error::new(io::ErrorKind::InvalidData, format!("{}", e)))
    }

    fn estimate_temp_gradient(&self) -> f32 {
        // Rolling slope proxy: EWMA of per-sample temperature deltas over the
        // filter window. Used as the "gradient" feature for the predictor.
        if self.temp_filter.values.len() < 2 {
            return 0.0;
        }
        let mut delta_sum = 0.0;
        let mut count = 0;
        let mut prev = *self.temp_filter.values.front().unwrap();
        for &v in self.temp_filter.values.iter().skip(1) {
            delta_sum += (v - prev) as f32;
            prev = v;
            count += 1;
        }
        if count == 0 {
            0.0
        } else {
            delta_sum / count as f32
        }
    }

    fn apply_intensity(&mut self, intensity: f32, temp: i32, cpu_load: f32) {
        if intensity > 0.1 && self.current_intensity <= 0.1 {
            self.effectiveness_tracker.start_mitigation(intensity, temp, cpu_load);
        } else if intensity <= 0.1 && self.current_intensity > 0.1 {
            self.effectiveness_tracker.end_mitigation(temp, cpu_load, &self.logger);
        }

        self.current_intensity = intensity;

        match self.actuator_mode {
            ActuatorMode::Dvfsrc => {
                // Primary lever: coordinated SoC OPP via DVFSRC.
                let _ = self.dvfsrc_actuator.apply(intensity, &self.logger);
                // Also tick soft devices (gpu devfreq, cpu_adaptive) for
                // secondary knobs that DVFSRC alone doesn't cover.
                for device in &mut self.soft_devices {
                    let _ = device.apply_intensity(intensity, &self.logger);
                }
            }
            ActuatorMode::Soft => {
                // Prefer soft-safe knobs that don't race the vendor thermal
                // stack. Fall back to broad devices (cpuNN etc.) only if the
                // device has no soft-safe cooling devices at all.
                let targets = if self.soft_devices.is_empty() {
                    &mut self.broad_devices
                } else {
                    &mut self.soft_devices
                };
                for device in targets {
                    let _ = device.apply_intensity(intensity, &self.logger);
                }
            }
        }
    }

    fn handle_temperature_change(&mut self) {
        if let Ok(raw_temp) = self.read_temperature() {
            let now = get_monotonic_time();

            let dt = now.saturating_sub(self.last_loop_time) as f32;
            let dt = if dt < 0.1 { 1.0 } else { dt };
            self.last_loop_time = now;

            self.temp_filter.add(raw_temp);
            let temp = self.temp_filter.get_ewma(0.3);

            self.external_context.update(&self.logger);
            let cpu_features = self.cpu_monitor.get_features(&self.logger);
            let is_screen_on = self.external_context.screen_state == ScreenState::On;

            // Fuse CPU/SoC zone temp so we react to raw silicon heat, which
            // lags battery temp by a wide margin on MTK (e.g. ~11C on MT6893).
            // The control temp blends the smooth battery reading with the
            // leading SoC max, keeping us responsive without chasing spikes.
            //
            // NOTE units: battery/temp (power_supply) is deci-degrees (310 =
            // 31.0C) matching our control targets (360 = 36.0C), while
            // thermal_zone*/temp is millidegrees (31000 = 31.0C). Normalize
            // the zone reading to deci-degrees before blending.
            let soc_temp_deg10 = self.thermal_fusion
                .get_max_cpu_temp()
                .map(|t| t / 100)
                .unwrap_or(temp);
            let control_temp = if soc_temp_deg10 > temp {
                (temp as f32 + SOC_TEMP_BLEND * (soc_temp_deg10 - temp) as f32) as i32
            } else {
                temp
            };

            let target_temp = self.learning_data.determine_target(
                control_temp,
                cpu_features.load_variance,
                is_screen_on,
                &self.logger
            );

            let throttle_intensity = self.policy_manager.pid.compute(
                control_temp,
                target_temp,
                dt,
                &self.logger
            );

            // Feed the predictive model with current state, then blend its
            // proactive bias into the throttle so we act before temp climbs.
            let gradient_proxy = self.estimate_temp_gradient();
            self.predictive_model.observe(
                now,
                control_temp as f32,
                cpu_features.cpu_load,
                cpu_features.load_variance,
                self.external_context.power_features.is_charging,
                self.external_context.power_features.current_now_abs,
                gradient_proxy,
            );

            let predict_bias = self.predictive_model.predictive_bias(PREDICT_BIAS_HORIZON_SECS);
            let blended_intensity = (throttle_intensity + PREDICT_BIAS_WEIGHT * predict_bias).min(1.0);

            self.apply_intensity(blended_intensity, temp, cpu_features.cpu_load);

            self.learning_data.record_event(
                temp,
                cpu_features.load_variance,
                target_temp,
                blended_intensity
            );

            self.predict_log_counter += 1;
            if self.predict_log_counter >= PREDICT_STATS_LOG_INTERVAL {
                self.predict_log_counter = 0;
                self.predictive_model.log_stats(&self.logger);
            }

            self.events_since_last_save += 1;
            if
                self.events_since_last_save >= SAVE_INTERVAL_EVENTS ||
                now.saturating_sub(self.last_save_time) >= SAVE_INTERVAL_SECS
            {
                if let Err(e) = self.learning_data.save() {
                    self.logger.error(&format!("Failed to save AI data: {}", e));
                }
                self.events_since_last_save = 0;
                self.last_save_time = now;
            }

            if now.saturating_sub(self.last_log_time) >= LOG_RATE_LIMIT_NORMAL_SECS {
                self.logger.debug(
                    &format!(
                        "Batt:{}°C SoC:{}°C Ctrl:{}°C | Tgt:{}°C | {:?} | Var:{:.3} | PID:{:.2} Bias:{:.2} Out:{:.2}",
                        temp / 10,
                        soc_temp_deg10 / 10,
                        control_temp / 10,
                        target_temp / 10,
                        self.learning_data.current_mode,
                        cpu_features.load_variance,
                        throttle_intensity,
                        predict_bias,
                        blended_intensity
                    )
                );
                self.last_log_time = now;
            }

            self.last_temp = temp;
        } else {
            self.logger.error("Failed to read temperature");
        }
    }

    fn setup_inotify(&self) -> io::Result<Inotify> {
        let inotify = Inotify::init()?;
        inotify.watches().add(&self.battery_temp_path, WatchMask::MODIFY | WatchMask::ATTRIB)?;
        self.logger.info(&format!("Monitoring: {}", self.battery_temp_path));
        Ok(inotify)
    }

    pub fn run(&mut self, term_flag: Arc<AtomicBool>) -> io::Result<()> {
        let mut mode = if Path::new(&self.battery_temp_path).exists() {
            RunMode::Inotify
        } else {
            RunMode::Poll
        };

        let mut inotify: Option<Inotify> = None;
        if let RunMode::Inotify = mode {
            inotify = Some(self.setup_inotify()?);
        }

        let mut buffer = [0u8; 4096];
        let poll_delay = Duration::from_secs(3);

        self.handle_temperature_change();

        loop {
            if term_flag.load(Ordering::Relaxed) {
                return Ok(());
            }

            match mode {
                RunMode::Inotify => {
                    let inotify_ref = inotify.as_mut().unwrap();
                    let fd = inotify_ref.as_raw_fd();
                    let borrowed_fd = unsafe { BorrowedFd::borrow_raw(fd) };
                    let poll_fd = PollFd::new(borrowed_fd, PollFlags::POLLIN);

                    match poll(&mut [poll_fd], PollTimeout::from(1000u16)) {
                        Ok(0) => {
                            self.handle_temperature_change();
                            continue;
                        }
                        Ok(n) if n > 0 => {
                            if let Ok(events) = inotify_ref.read_events(&mut buffer) {
                                if events.count() > 0 {
                                    self.handle_temperature_change();
                                }
                            }
                        }
                        Err(_) => {
                            mode = RunMode::Poll;
                            inotify = None;
                        }
                        _ => {}
                    }
                }
                RunMode::Poll => {
                    self.handle_temperature_change();
                    std::thread::sleep(poll_delay);

                    if Path::new(&self.battery_temp_path).exists() {
                        if let Ok(new_inotify) = self.setup_inotify() {
                            inotify = Some(new_inotify);
                            mode = RunMode::Inotify;
                        }
                    }
                }
            }
        }
    }
}
