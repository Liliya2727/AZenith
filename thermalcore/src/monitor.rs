use std::io::{ self };
use std::os::unix::io::{ AsRawFd, BorrowedFd };
use std::path::Path;
use std::sync::{ atomic::{ AtomicBool, Ordering }, Arc };
use std::time::Duration;
use nix::poll::{ poll, PollFd, PollFlags, PollTimeout };
use inotify::{ Inotify, WatchMask };

use crate::android_ffi::Logger;
use crate::cooling::CoolingDevice;
use crate::learning::ThermalAI;
use crate::policy_manager::PolicyManager;
use crate::utils::{ TemperatureFilter, get_thermal_path, get_data_path };
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

pub struct ThermalMonitor {
    pub logger: Logger,
    pub learning_data: ThermalAI,
    cooling_devices: Vec<CoolingDevice>,
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
}

impl ThermalMonitor {
    pub fn new() -> Self {
        let logger = Logger::new();
        logger.info("Rianixia Thermal Core v3.0 [Contextual PID-LSTM] - Initializing");

        let data_path = get_data_path();
        let thermal_path = get_thermal_path();

        let cooling_devices = CoolingDevice::enumerate(&logger);
        let (learning_data, is_new) = ThermalAI::load(data_path.clone());

        if is_new {
            logger.info("New AI Context Profile created.");
        }

        let external_context = ExternalContext::new();
        let mut policy_manager = PolicyManager::new();

        policy_manager.update_params(learning_data.kp, learning_data.ki, learning_data.kd);

        let now = get_monotonic_time();

        ThermalMonitor {
            logger,
            cooling_devices,
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
        }
    }

    fn read_temperature(&self) -> io::Result<i32> {
        std::fs
            ::read_to_string(&self.battery_temp_path)?
            .trim()
            .parse()
            .map_err(|e| io::Error::new(io::ErrorKind::InvalidData, format!("{}", e)))
    }

    fn apply_intensity(&mut self, intensity: f32, temp: i32, cpu_load: f32) {
        if intensity > 0.1 && self.current_intensity <= 0.1 {
            self.effectiveness_tracker.start_mitigation(intensity, temp, cpu_load);
        } else if intensity <= 0.1 && self.current_intensity > 0.1 {
            self.effectiveness_tracker.end_mitigation(temp, cpu_load, &self.logger);
        }

        self.current_intensity = intensity;

        for device in &mut self.cooling_devices {
            let _ = device.apply_intensity(intensity, &self.logger);
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

            let target_temp = self.learning_data.determine_target(
                temp,
                cpu_features.load_variance,
                is_screen_on,
                &self.logger
            );

            let throttle_intensity = self.policy_manager.pid.compute(
                temp,
                target_temp,
                dt,
                &self.logger
            );

            self.apply_intensity(throttle_intensity, temp, cpu_features.cpu_load);

            self.learning_data.record_event(
                temp,
                cpu_features.load_variance,
                target_temp,
                throttle_intensity
            );

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
                        "T:{}°C | Target:{}°C | Mode:{:?} | Var:{:.3} | PID_Out:{:.2}",
                        temp / 10,
                        target_temp / 10,
                        self.learning_data.current_mode,
                        cpu_features.load_variance,
                        throttle_intensity
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
