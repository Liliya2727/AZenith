//! Sysfs node writes. Standard pattern: chmod 644 → write → lock 444.

use crate::utils::logging::{log_debug, log_warn};
use std::fs;
use std::os::unix::fs::PermissionsExt;
use std::path::Path;

pub fn chmod(path: &str, mode: u32) {
    if let Ok(metadata) = fs::metadata(path) {
        let mut perms = metadata.permissions();
        perms.set_mode(mode);
        let _ = fs::set_permissions(path, perms);
    }
}

pub fn write_unlock_core(value: &str, path_str: &str, lock: bool) {
    let path = Path::new(path_str);
    let parent_name = path.parent().and_then(|p| p.file_name()).unwrap_or_default().to_string_lossy();
    let file_name = path.file_name().unwrap_or_default().to_string_lossy();
    let pathname = if parent_name.is_empty() { file_name.into_owned() } else { format!("{}/{}", parent_name, file_name) };

    if !path.exists() { return; }

    chmod(path_str, 0o644);

    let val_with_newline = format!("{}\n", value);
    
    if fs::write(path, val_with_newline).is_err() {
        log_warn("prop", &format!("write failed path=/{} reason=permission_denied", pathname));
        if lock { chmod(path_str, 0o444); }
        return;
    }

    log_debug("prop", &format!("set path=/{} value={}", pathname, value));
    if lock { chmod(path_str, 0o444); }
}

pub fn write_unlock(value: &str, path_str: &str) {
    write_unlock_core(value, path_str, false);
}

pub fn write_lock(value: &str, path_str: &str) {
    write_unlock_core(value, path_str, true);
}
