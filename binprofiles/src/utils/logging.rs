//! Status reporting to `sys.azenith-service`.

use crate::utils::props::get_debugmode;
use std::process::Command;

/// DEBUG detail for an external component. Only emitted when debug mode is on;
/// routed to the verbose log so the main log stays readable.
pub fn log_debug(component: &str, message: &str) {
    if get_debugmode() {
        let _ = Command::new("sys.azenith-service")
            .args(["--verboselog", component, "0", message])
            .status();
    }
}

/// A significant, low-frequency event (start/stop/profile change/config reload).
pub fn log_info(component: &str, message: &str) {
    let _ = Command::new("sys.azenith-service")
        .args(["--log", component, "1", message])
        .status();
}

/// An oddity the caller recovered from; execution continues.
pub fn log_warn(component: &str, message: &str) {
    let _ = Command::new("sys.azenith-service")
        .args(["--log", component, "2", message])
        .status();
}

/// An operation failed and needs attention. Include path/value/error context.
pub fn log_error(component: &str, message: &str) {
    let _ = Command::new("sys.azenith-service")
        .args(["--log", component, "3", message])
        .status();
}
