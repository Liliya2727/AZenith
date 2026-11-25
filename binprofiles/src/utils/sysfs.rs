//! Sysfs node writes. Standard pattern: chmod 644 → write → lock 444.

use crate::utils::logging::{log_debug, log_warn};
use std::fs;
use std::os::unix::fs::PermissionsExt;
use std::path::Path;
use std::process::{Command, Stdio};

pub fn chmod(path: &str, mode: u32) {
    if let Ok(metadata) = fs::metadata(path) {
        let mut perms = metadata.permissions();
        perms.set_mode(mode);
        let _ = fs::set_permissions(path, perms);
    }
}

fn run_in_all_mnt_ns(args: &[&str]) -> bool {
    let local = Command::new(args[0])
        .args(&args[1..])
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .status()
        .map(|s| s.success())
        .unwrap_or(false);
    let global = Command::new("nsenter")
        .args(["-t", "1", "-m", "--"])
        .args(args)
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .status()
        .map(|s| s.success())
        .unwrap_or(false);
    local || global
}

/// Bind-mount a snapshot of `path` from `/cache` over `path` after writing
/// `value`, adopted from Uperf `miui_migt.sh::hide_value()`. Mounted in both
/// the caller's mount namespace and `init`'s global mount namespace (`pid 1`)
/// so all daemons see the shielded file.
pub fn hide_val(value: &str, path_str: &str) {
    let path = Path::new(path_str);
    unhide_val(path_str);

    if !path.exists() {
        return;
    }

    write_lock(value, path_str);

    let cache_path = format!("/cache{}", path_str);
    if let Some(parent) = Path::new(&cache_path).parent() {
        let _ = fs::create_dir_all(parent);
    }
    let snapshot = fs::read_to_string(path).unwrap_or_else(|_| format!("{}\n", value));
    if fs::write(&cache_path, snapshot).is_ok() {
        chmod(&cache_path, 0o444);
        let _ = run_in_all_mnt_ns(&["mount", "--bind", &cache_path, path_str]);
    }
}

/// Write directly to the underlying kernel file even if it was previously bind-mounted,
/// then bind-mount a read-only snapshot from `/cache` on top so other processes
/// cannot overwrite it.
pub fn hide_write_multi(values: &[String], path_str: &str) {
    let path = Path::new(path_str);
    unhide_val(path_str);

    if !path.exists() {
        return;
    }

    for val in values {
        write_lock(val, path_str);
    }

    let cache_path = format!("/cache{}", path_str);
    if let Some(parent) = Path::new(&cache_path).parent() {
        let _ = fs::create_dir_all(parent);
    }
    let snapshot = fs::read_to_string(path).unwrap_or_default();
    if fs::write(&cache_path, snapshot).is_ok() {
        chmod(&cache_path, 0o444);
        let _ = run_in_all_mnt_ns(&["mount", "--bind", &cache_path, path_str]);
    }
}

/// Unmount any bind-mounts previously placed on `path_str` by [`hide_val`] in
/// both local and global (`init`) mount namespaces.
pub fn unhide_val(path_str: &str) {
    for _ in 0..8 {
        if !run_in_all_mnt_ns(&["umount", "-l", path_str]) {
            break;
        }
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
