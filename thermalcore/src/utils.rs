use std::collections::VecDeque;
use std::ffi::{ CString, CStr };
use std::os::raw::{ c_char, c_uchar };
use nix::time::{ clock_gettime, ClockId };
use std::fs;
use std::path::PathBuf;

use super::android_ffi::__system_property_get; // Import FFI
use super::constants::*; // Import constants

// ============================================================================
// MONOTONIC TIME UTILITIES
// ============================================================================

pub fn get_monotonic_time() -> u64 {
    clock_gettime(ClockId::CLOCK_MONOTONIC)
        .map(|ts| ts.tv_sec() as u64)
        .unwrap_or(0)
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

pub fn get_system_property(key: &str, default: &str) -> String {
    let prop_name = CString::new(key).unwrap();
    let mut value = [0u8; 92];

    unsafe {
        let len = __system_property_get(
            prop_name.as_ptr() as *const c_uchar,
            value.as_mut_ptr() as *mut c_uchar
        );

        if len > 0 {
            let val_str = CStr::from_ptr(value.as_ptr() as *const c_char)
                .to_string_lossy()
                .into_owned();
            if !val_str.is_empty() {
                return val_str;
            }
        }
    }
    default.to_string()
}

pub fn get_data_path() -> PathBuf {
    let path_str = get_system_property(PROP_BIGDATA_PATH, "");

    let path = if path_str.is_empty() {
        PathBuf::from(DEFAULT_DATA_PATH)
    } else {
        PathBuf::from(path_str)
    };

    if let Err(e) = fs::create_dir_all(&path) {
        eprintln!("Failed to create data dir {:?}: {}. Falling back.", path, e);
        let fallback_path = PathBuf::from("/data/local/tmp/rianixia_thermal_data");
        if fs::create_dir_all(&fallback_path).is_ok() {
            return fallback_path;
        }
        return PathBuf::from(DEFAULT_DATA_PATH);
    }

    path
}

pub fn get_thermal_path() -> String {
    get_system_property(SYS_PROP_PATH, BATTERY_TEMP_PATH)
}

pub fn get_bool_property(key: &str, default: bool) -> bool {
    let val = get_system_property(key, if default { "true" } else { "false" });
    val == "true" || val == "1"
}

pub fn get_int_property(key: &str, default: i32) -> i32 {
    let val = get_system_property(key, &default.to_string());
    val.parse().unwrap_or(default)
}

// ============================================================================
// TEMPERATURE FILTER
// ============================================================================

pub struct TemperatureFilter {
    pub values: VecDeque<i32>,
    pub window_size: usize,
}

impl TemperatureFilter {
    pub fn new(window_size: usize) -> Self {
        TemperatureFilter {
            values: VecDeque::with_capacity(window_size),
            window_size,
        }
    }

    pub fn add(&mut self, temp: i32) {
        if self.values.len() >= self.window_size {
            self.values.pop_front();
        }
        self.values.push_back(temp);
    }

    pub fn get_ewma(&self, alpha: f32) -> i32 {
        if self.values.is_empty() {
            return 0;
        }
        let mut ewma = self.values.front().copied().unwrap() as f32;
        for &val in self.values.iter().skip(1) {
            ewma += alpha * ((val as f32) - ewma);
        }
        ewma as i32
    }

    pub fn detect_anomaly(&self, new_temp: i32) -> bool {
        if self.values.is_empty() {
            return false;
        }
        let last = *self.values.back().unwrap();
        (new_temp - last).abs() > ANOMALY_TEMP_JUMP_THRESHOLD
    }
}
