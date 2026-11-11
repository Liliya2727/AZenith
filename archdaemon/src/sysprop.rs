//! System property operations, centralized wrapper around `__system_property_get/set`.
//!
//! Mirrors bionic `<sys/system_properties.h>` usage from the C codebase.

use crate::android_ffi::{__system_property_get, __system_property_set, PROP_VALUE_MAX};
use std::ffi::CString;
use std::os::raw::{c_char, c_uchar};

// ---------------------------------------------------------------------------
// Internal: read a property into a byte buffer
// ---------------------------------------------------------------------------
fn prop_get_raw(name: &str) -> Option<String> {
    let c_name = match CString::new(name) {
        Ok(s) => s,
        Err(_) => return None,
    };
    let mut buf = [0u8; PROP_VALUE_MAX];
    let len = unsafe {
        __system_property_get(c_name.as_ptr() as *const c_uchar, buf.as_mut_ptr() as *mut c_uchar)
    };
    if len <= 0 {
        return None;
    }
    let cstr = unsafe { std::ffi::CStr::from_ptr(buf.as_ptr() as *const c_char) };
    Some(cstr.to_string_lossy().into_owned())
}

// ---------------------------------------------------------------------------
// Internal: write a property
// ---------------------------------------------------------------------------
fn prop_set_raw(name: &str, value: &str) -> bool {
    let Ok(c_name) = CString::new(name) else { return false };
    let Ok(c_value) = CString::new(value) else { return false };
    unsafe { __system_property_set(c_name.as_ptr(), c_value.as_ptr()) == 0 }
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/// Get a system property value as String.
pub fn get(name: &str) -> Option<String> {
    prop_get_raw(name)
}

/// Get a system property value, or a default if not set.
pub fn get_or_default(name: &str, default: &str) -> String {
    prop_get_raw(name).unwrap_or_else(|| default.to_string())
}

/// Check if a property equals a specific value.
pub fn is(name: &str, value: &str) -> bool {
    matches!(prop_get_raw(name), Some(v) if v == value)
}

/// Set a system property value.
pub fn set(name: &str, value: &str) -> bool {
    prop_set_raw(name, value)
}

/// Set multiple properties at once.
pub fn set_many(props: &[(&str, &str)]) -> bool {
    let mut ok = true;
    for (name, value) in props {
        if !prop_set_raw(name, value) {
            ok = false;
        }
    }
    ok
}

/// Read a property into a fixed-size byte buffer (mirrors C `extract_string_value`-style usage).
/// Returns the number of bytes written (excluding null terminator), or -1 on error.
pub fn get_into(name: &str, buf: &mut [u8]) -> i32 {
    let c_name = match CString::new(name) {
        Ok(s) => s,
        Err(_) => return -1,
    };
    let len = unsafe {
        __system_property_get(c_name.as_ptr() as *const c_uchar, buf.as_mut_ptr() as *mut c_uchar)
    };
    if len <= 0 {
        return -1;
    }
    // Clamp to buffer size - 1 for null terminator
    let copy_len: usize = len.min(buf.len().saturating_sub(1) as i32) as usize;
    buf[copy_len] = 0;
    copy_len as i32
}
