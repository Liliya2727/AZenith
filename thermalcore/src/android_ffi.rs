use std::ffi::{ CString, CStr };
use std::os::raw::{ c_char, c_int, c_uchar };
use super::constants::PROP_AUDIT_LOGS_ENABLED;

// ============================================================================
// FFI BINDINGS - Android System Properties and Logging
// ============================================================================

pub const ANDROID_LOG_DEBUG: c_int = 3;
pub const ANDROID_LOG_INFO: c_int = 4;
pub const ANDROID_LOG_WARN: c_int = 5;
pub const ANDROID_LOG_ERROR: c_int = 6;

#[link(name = "log")]
unsafe extern "C" {
    pub fn __android_log_print(prio: c_int, tag: *const c_char, fmt: *const c_char, ...) -> c_int;
}

#[link(name = "c")]
unsafe extern "C" {
    pub fn __system_property_get(name: *const c_uchar, value: *mut c_uchar) -> c_int;
}

// ============================================================================
// LOGGING UTILITIES
// ============================================================================

pub struct Logger {
    pub tag: CString,
    pub debug_enabled: bool,
    pub audit_logs_enabled: bool,
}

impl Logger {
    pub fn new() -> Self {
        let tag = CString::new("RianixiaThermalCore").unwrap();
        let debug_enabled = Self::check_bool_property(
            "persist.sys.rianixia.thermalcore-debug",
            false
        );
        let audit_logs_enabled = Self::check_bool_property(PROP_AUDIT_LOGS_ENABLED, false);
        Logger { tag, debug_enabled, audit_logs_enabled }
    }

    pub fn check_bool_property(prop_name_str: &str, default: bool) -> bool {
        let prop_name = CString::new(prop_name_str).unwrap();
        let mut value = [0u8; 92];

        unsafe {
            let result = __system_property_get(
                prop_name.as_ptr() as *const c_uchar,
                value.as_mut_ptr() as *mut c_uchar
            );

            if result > 0 {
                let val_str = CStr::from_ptr(value.as_ptr() as *const c_char).to_string_lossy();
                if val_str == "true" {
                    return true;
                }
                if val_str == "false" {
                    return false;
                }
            }
        }
        default
    }

    pub fn log(&self, level: c_int, msg: &str) {
        if !self.debug_enabled && level == ANDROID_LOG_DEBUG {
            return;
        }

        let c_msg = CString::new(msg).unwrap_or_else(|_|
            CString::new("invalid log message").unwrap()
        );
        unsafe {
            __android_log_print(level, self.tag.as_ptr(), c_msg.as_ptr());
        }
    }

    pub fn debug(&self, msg: &str) {
        self.log(ANDROID_LOG_DEBUG, msg);
    }
    pub fn info(&self, msg: &str) {
        self.log(ANDROID_LOG_INFO, msg);
    }
    pub fn warn(&self, msg: &str) {
        self.log(ANDROID_LOG_WARN, msg);
    }
    pub fn error(&self, msg: &str) {
        self.log(ANDROID_LOG_ERROR, msg);
    }
}
