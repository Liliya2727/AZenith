//! IPC / inotify watchers.
//! Uses raw inotify syscalls via libc (same as the C implementation).

use crate::android_ffi::{
    inotify_add_watch, inotify_init1, IN_CLOEXEC, IN_CREATE, IN_CLOSE_WRITE, IN_MOVED_TO,
    IN_MODIFY, IN_DELETE, IN_DELETE_SELF, IN_IGNORED,
};
use crate::core::{
    DaemonContext, ProfileMode, PROFILE_MODE, DAEMON_MODES,
};
use crate::log_info;
use crate::monitor::{read_app_status, reload_gamelist_cache};
use crate::profile::apply_balanced_profile;
use crate::recorder::{Recorder, RECORD_CMD_PATH};
use crate::sysprop::set;
use crate::fileops;

use nix::poll::{poll, PollFlags, PollFd, PollTimeout};
use std::os::fd::BorrowedFd;
use std::sync::atomic::{AtomicI32, Ordering};
use std::thread;
use std::time::Duration;

// inotify_event structure from <sys/inotify.h>
#[repr(C)]
struct InotifyEvent {
    wd: i32,
    mask: u32,
    cookie: u32,
    len: u32,
    // name follows (variable length)
}

// C uses `char buf[4096] __attribute__((aligned(__alignof__(struct inotify_event))))`.
// Plain [u8; 4096] plus `&*event_ptr` at offsets like 33 (after a tmp-file event)
// is UB on strict-alignment targets and can corrupt len/mask on ARM64.
#[repr(C, align(8))]
struct AlignedBuf([u8; 4096]);

// Deviation from C (deliberate): the C watch set covers the whole
// `/data/adb/.config/AZenith/API/` directory, which is wrong for us —
// sysdaemon republishes `API/current_fps` every 1s (tmp CREATE + MODIFY +
// MOVED_TO), so the daemon would wake every iteration for file events it
// never reads. `current_fps`/`gameinfo` don't matter to the daemon at all.
//
// Fix: watch only the two files the daemon actually reads, as file watches:
//   - `current_profile` (written in-place by the daemon itself)
//   - `current_modes`   (rewritten atomically by sysdaemon / the app)
// File watches report the inode that was watched, so an atomic rename
// (write tmp + rename) replaces the watched inode: the old watch gets
// DELETE_SELF + IGNORED. We re-add the watch on IN_IGNORED and immediately
// re-process the new content (same behavior as a name event).
//
// The watch descriptors are needed in the read loop, so they live here.
static API_WATCH_FD: AtomicI32 = AtomicI32::new(-1);
static WD_API_PROFILE: AtomicI32 = AtomicI32::new(-1);
static WD_API_MODES: AtomicI32 = AtomicI32::new(-1);
static WD_REC_CMD: AtomicI32 = AtomicI32::new(-1);

const API_PROFILE_PATH: &str = "/data/adb/.config/AZenith/API/current_profile";
const API_MODES_PATH: &str = "/data/adb/.config/AZenith/API/current_modes";
const API_FILE_MASK: u32 = IN_MODIFY | IN_CLOSE_WRITE | IN_DELETE_SELF | IN_IGNORED;

// ---------------------------------------------------------------------------
// setup_inotify_watchers
// ---------------------------------------------------------------------------
pub fn setup_inotify_watchers() -> i32 {
    let fd = unsafe { inotify_init1(IN_CLOEXEC | 0o4000) }; // IN_NONBLOCK = O_NONBLOCK
    if fd < 0 {
        crate::log_error!("inotify", "init failed errno={}", std::io::Error::last_os_error());
        return -1;
    }

    let targets: &[(&str, u32)] = &[
        ("/data/adb/.config/AZenith/", IN_MODIFY | IN_CREATE | IN_MOVED_TO),
        ("/data/adb/.config/AZenith/gamelist/", IN_MODIFY | IN_CLOSE_WRITE | IN_MOVED_TO | IN_CREATE),
        ("/data/adb/.config/AZenith/bypasschgconfig/", IN_MODIFY | IN_CREATE | IN_MOVED_TO),
        ("/data/adb/modules/AZenith/", IN_MODIFY | IN_CREATE | IN_MOVED_TO | IN_DELETE),
    ];

    for (path, mask) in targets {
        let c_path = std::ffi::CString::new(*path).unwrap();
        let wd = unsafe { inotify_add_watch(fd, c_path.as_ptr(), *mask) };
        if wd < 0 {
            crate::log_warn!("inotify", "watch FAILED path={} wd={} errno={} mask=0x{:x}", path, wd, std::io::Error::last_os_error(), mask);
        } else {
            crate::log_debug!("inotify", "watch ok path={} wd={} mask=0x{:x}", path, wd, mask);
        }
    }

    // File watches for the only files in API/ the daemon reads. The directory
    // watch deliberately omits API/ (current_fps is republished every 1s).
    API_WATCH_FD.store(fd, Ordering::Relaxed);
    let wd_profile = add_file_watch(fd, API_PROFILE_PATH);
    WD_API_PROFILE.store(wd_profile, Ordering::Relaxed);
    let wd_modes = add_file_watch(fd, API_MODES_PATH);
    WD_API_MODES.store(wd_modes, Ordering::Relaxed);
    let wd_rec_cmd = add_file_watch(fd, RECORD_CMD_PATH);
    WD_REC_CMD.store(wd_rec_cmd, Ordering::Relaxed);

    crate::log_info!("inotify", "watchers setup fd={} watches={} api_file_watches profile={} modes={} rec_cmd={}", fd, targets.len() + 3, wd_profile, wd_modes, wd_rec_cmd);
    fd
}

fn add_file_watch(fd: i32, path: &str) -> i32 {
    let c_path = match std::ffi::CString::new(path) {
        Ok(p) => p,
        Err(_) => return -1,
    };
    let wd = unsafe { inotify_add_watch(fd, c_path.as_ptr(), API_FILE_MASK) };
    if wd < 0 {
        // record_cmd may not exist yet (created by the first overlay tap) —
        // retrying quietly; everything else is a real problem.
        if path == RECORD_CMD_PATH {
            crate::log_debug!("inotify", "file watch not yet possible path={} wd={} errno={}", path, wd, std::io::Error::last_os_error());
        } else {
            crate::log_warn!("inotify", "file watch FAILED path={} wd={} errno={} mask=0x{:x}",
                path, wd, std::io::Error::last_os_error(), API_FILE_MASK);
        }
    } else {
        crate::log_debug!("inotify", "file watch ok path={} wd={} mask=0x{:x}", path, wd, API_FILE_MASK);
    }
    wd
}

// ---------------------------------------------------------------------------
// process_inotify_events
// ---------------------------------------------------------------------------
pub fn process_inotify_events(
    inotify_fd: i32,
    ctx: &mut DaemonContext,
    rec: &mut Recorder,
    timeout_ms: i32,
) -> bool {
    if inotify_fd < 0 {
        return false;
    }

    // Re-arm API file watches whose inode was replaced (rename-based writes)
    // or missing at setup. No-op when both are live. A watch that appears
    // late (first `record_cmd`) is processed immediately on arming.
    retry_api_file_watches(inotify_fd, ctx, rec);

    let pipe_read = crate::core::JAVA_LOCK_PIPE_READ
        .get()
        .map(|p| p.load(std::sync::atomic::Ordering::Relaxed))
        .unwrap_or(-1);

    let mut fds = [
        unsafe { PollFd::new(BorrowedFd::borrow_raw(inotify_fd), PollFlags::POLLIN) },
        unsafe { PollFd::new(BorrowedFd::borrow_raw(pipe_read), PollFlags::POLLIN) },
    ];

    let timeout = if timeout_ms < 0 {
        None
    } else {
        Some(PollTimeout::from(timeout_ms as u16))
    };
    crate::log_debug!("inotify", "poll start timeout={:?} inotify_fd={} pipe_fd={}", timeout, inotify_fd, pipe_read);
    let poll_result = poll(&mut fds, timeout);
    crate::log_debug!("inotify", "poll returned result={:?}", poll_result);
    match poll_result {
        Ok(n) => {
            if n > 0 {
                crate::log_debug!("inotify", "poll ready n={} inotify_revents={:?} pipe_revents={:?}",
                    n, fds[0].revents(), fds[1].revents());
            }
        }
        Err(_) => return false,
    }

    // Check java lock pipe
    if fds[1].revents().map_or(false, |r| r & PollFlags::POLLIN != PollFlags::empty()) {
        crate::core::set_java_daemon_died(true);
        return true;
    }

    // Check inotify
    if fds[0].revents().map_or(true, |r| r & PollFlags::POLLIN == PollFlags::empty()) {
        return false;
    }

    // Read all pending inotify events
    let mut aligned = AlignedBuf([0u8; 4096]);
    let mut event_count = 0;
    loop {
        let len = unsafe {
            libc::read(inotify_fd, aligned.0.as_mut_ptr() as *mut _, aligned.0.len())
        };
        if len <= 0 {
            break;
        }
        event_count += 1;
        let buf = &aligned.0;
        for_each_inotify_event(buf, len as usize, |wd, name, mask| {
            process_event(ctx, wd, name, mask, rec);
        });
    }
    if event_count > 0 {
        crate::log_debug!("inotify", "processed {} batches", event_count);
    }

    false
}

/// Iterate a packed inotify byte batch exactly like the C loop:
/// `for (ptr = buf; ptr < buf+len; ptr += sizeof(ev)+ev->len)` with
/// strcmp-style name handling (stop at the first NUL).
/// Events with `len == 0` are file-watch events (no name) — they are
/// reported too so the API file watches can be routed by `wd`.
fn for_each_inotify_event<F: FnMut(i32, &str, u32)>(buf: &[u8], len: usize, mut f: F) {
    let mut ptr = 0usize;
    while ptr + std::mem::size_of::<InotifyEvent>() <= len {
        // read_unaligned: events after the first are not 4-aligned.
        let event = unsafe {
            std::ptr::read_unaligned(buf[ptr..].as_ptr() as *const InotifyEvent)
        };

        if event.len > 0 {
            let name_start = ptr + std::mem::size_of::<InotifyEvent>();
            let name_end = name_start + event.len as usize;
            if name_end <= len {
                let raw = &buf[name_start..name_end];
                let nul = raw.iter().position(|&b| b == 0).unwrap_or(raw.len());
                let name = std::str::from_utf8(&raw[..nul]).unwrap_or("");
                crate::log_debug!("inotify", "event name={} mask=0x{:x} wd={}", name, event.mask, event.wd);
                f(event.wd, name, event.mask);
            }
        } else {
            crate::log_debug!("inotify", "event name=<file> mask=0x{:x} wd={}", event.mask, event.wd);
            f(event.wd, "", event.mask);
        }

        ptr += std::mem::size_of::<InotifyEvent>() + event.len as usize;
    }
}

/// Re-add the API file watches whose inode was replaced (rename-based
/// writers) or was missing at setup.
fn retry_api_file_watches(inotify_fd: i32, ctx: &mut DaemonContext, rec: &mut Recorder) {
    if WD_API_PROFILE.load(Ordering::Relaxed) < 0 {
        let wd = add_file_watch(inotify_fd, API_PROFILE_PATH);
        WD_API_PROFILE.store(wd, Ordering::Relaxed);
    }
    if WD_API_MODES.load(Ordering::Relaxed) < 0 {
        let wd = add_file_watch(inotify_fd, API_MODES_PATH);
        WD_API_MODES.store(wd, Ordering::Relaxed);
    }
    if WD_REC_CMD.load(Ordering::Relaxed) < 0 {
        let wd = add_file_watch(inotify_fd, RECORD_CMD_PATH);
        WD_REC_CMD.store(wd, Ordering::Relaxed);
        if wd >= 0 {
            // The file was created while we had no watch (e.g. the very first
            // command): process its content now.
            if let Ok(s) = std::fs::read_to_string(RECORD_CMD_PATH) {
                rec.handle_cmd(ctx, s.trim());
            }
        }
    }
}

/// Handle one API file-watch event (no name): read & apply the file's fresh
/// content, then re-arm the watch if the inode was replaced (rename).
fn process_api_file_event(ctx: &mut DaemonContext, wd: i32, mask: u32, rec: &mut Recorder) {
    // NOTE: process content FIRST — re-arming updates the stored wd, and the
    // event's wd refers to the OLD inode watch.
    if wd == WD_API_PROFILE.load(Ordering::Relaxed) {
        if let Ok(fp) = std::fs::File::open(PROFILE_MODE) {
            use std::io::BufRead;
            if let Some(line) = std::io::BufReader::new(fp).lines().next() {
                if let Ok(s) = line {
                    let trimmed = s.trim().to_string();
                    ctx.profile.cur_mode = ProfileMode::from_i32(trimmed.parse().unwrap_or(0));
                }
            }
        }
    } else if  wd == WD_REC_CMD.load(Ordering::Relaxed) {
        if let Ok(s) = crate::fileops::read_string_result(RECORD_CMD_PATH) {
            rec.handle_cmd(ctx, s.trim());
        }
    } else if wd == WD_API_MODES.load(Ordering::Relaxed) {
        if let Ok(fp) = std::fs::File::open(DAEMON_MODES) {
            use std::io::BufRead;
            if let Some(line) = std::io::BufReader::new(fp).lines().next() {
                if let Ok(s) = line {
                    let trimmed = s.trim().to_string();
                    let ai_state = trimmed;
                    if ctx.is_initialize_complete && ctx.prev_ai_state_str() != ai_state {
                        log_info!("inotify", "dynamic profile toggled action=reapply_balanced");
                        ctx.profile.cur_mode = ProfileMode::PerfCommon;
                        apply_balanced_profile(ctx);
                        let bytes = ai_state.as_bytes();
                        let n = bytes.len().min(ctx.prev_ai_state.len() - 1);
                        ctx.prev_ai_state[..n].copy_from_slice(&bytes[..n]);
                        ctx.prev_ai_state[n] = 0;
                        if ai_state == "1" {
                            ctx.game.gamestart = None;
                            ctx.game.active_app_name = None;
                            ctx.game.game_pids = [0; super::core::MAX_GAME_PIDS];
                            ctx.game.game_pid_count = 0;
                            ctx.need_profile_checkup = true;
                        } else {
                            ctx.need_profile_checkup = true;
                        }
                    }
                }
            }
        }
    }

    // (Re-)add the watch after an inode replacement.
    if (mask & (IN_DELETE_SELF | IN_IGNORED)) != 0 {
        let fd = API_WATCH_FD.load(Ordering::Relaxed);
        if fd >= 0 {
            if wd == WD_API_PROFILE.load(Ordering::Relaxed) {
                let nwd = add_file_watch(fd, API_PROFILE_PATH);
                WD_API_PROFILE.store(nwd, Ordering::Relaxed);
            }
            if wd == WD_API_MODES.load(Ordering::Relaxed) {
                let nwd = add_file_watch(fd, API_MODES_PATH);
                WD_API_MODES.store(nwd, Ordering::Relaxed);
            }
            if wd == WD_REC_CMD.load(Ordering::Relaxed) {
                let nwd = add_file_watch(fd, RECORD_CMD_PATH);
                WD_REC_CMD.store(nwd, Ordering::Relaxed);
            }
        }
    }
}

fn process_event(
    ctx: &mut DaemonContext,
    wd: i32,
    name: &str,
    mask: u32,
    rec: &mut Recorder,
) {
    // File-watch events (API/ files) have no name — route by wd.
    if name.is_empty() {
        process_api_file_event(ctx, wd, mask, rec);
        return;
    }

    const CLOSE_WRITE: u32 = 0x00000008;
    const MOVED_TO: u32 = 0x00000080;
    const CREATE: u32 = 0x00000100;

    // Check for IN_CLOSE_WRITE | IN_MOVED_TO | IN_CREATE on azenithApplist.json
    if (mask & (CLOSE_WRITE | MOVED_TO | CREATE)) != 0 && name.contains("azenithApplist.json") {
        thread::sleep(Duration::from_millis(50));
        reload_gamelist_cache(ctx);
        ctx.need_profile_checkup = true;
    }

    match name {
        "app_status" => {
            crate::log_debug!("inotify", "app_status event received mask=0x{:x}, re-reading", mask);
            read_app_status(&mut ctx.current_system_cache);
            crate::log_debug!("inotify", "app_status re-read focused={} app_name={}", ctx.current_system_cache.focused_app_str(), ctx.current_system_cache.app_name_str());
            ctx.need_profile_checkup = true;
        }
        "background_apps" => {
            super::bypass::handle_background_apps_event(ctx);
            if ctx.game.gamestart.is_none() {
                ctx.need_profile_checkup = true;
            }
        }
        "current_profile" => {
            if let Ok(fp) = std::fs::File::open(PROFILE_MODE) {
                use std::io::BufRead;
                if let Some(line) = std::io::BufReader::new(fp).lines().next() {
                    if let Ok(s) = line {
                        let trimmed = s.trim().to_string();
                        ctx.profile.cur_mode = ProfileMode::from_i32(trimmed.parse().unwrap_or(0));
                    }
                }
            }
        }
        "current_modes" => {
            if let Ok(fp) = std::fs::File::open(DAEMON_MODES) {
                use std::io::BufRead;
                if let Some(line) = std::io::BufReader::new(fp).lines().next() {
                    if let Ok(s) = line {
                        let trimmed = s.trim().to_string();
                        let ai_state = trimmed;
                        if ctx.is_initialize_complete && ctx.prev_ai_state_str() != ai_state {
                            log_info!("inotify", "dynamic profile toggled action=reapply_balanced");
                            ctx.profile.cur_mode = ProfileMode::PerfCommon;
                            apply_balanced_profile(ctx);
                            let bytes = ai_state.as_bytes();
                            let n = bytes.len().min(ctx.prev_ai_state.len() - 1);
                            ctx.prev_ai_state[..n].copy_from_slice(&bytes[..n]);
                            ctx.prev_ai_state[n] = 0;
                            if ai_state == "1" {
                                ctx.game.gamestart = None;
                                ctx.game.active_app_name = None;
                                ctx.game.game_pids = [0; super::core::MAX_GAME_PIDS];
                                ctx.game.game_pid_count = 0;
                                ctx.need_profile_checkup = true;
                            } else {
                                // Manual mode: keep current game state but
                                // re-enable game detection so gameinfo stays
                                // current and sidepanel can show for new games.
                                ctx.need_profile_checkup = true;
                            }
                        }
                    }
                }
            }
        }
        "update" => {
            log_info!("inotify", "module update detected action=exit");
            notify("Module Update", "Please reboot your device to complete module update.", false, 0, None);
            set("persist.sys.azenith.service", "");
            set("persist.sys.azenith.state", "stopped");
            crate::core::set_java_daemon_died(true);
        }
        "remove" => {
            log_info!("inotify", "module removed action=exit");
            notify("Module Removed", "Please reboot your device to complete module uninstallation.", false, 0, None);
            crate::core::set_java_daemon_died(true);
        }
        "module.prop" => {
            log_info!("inotify", "module.prop modified");
            crate::module::is_kanged();
            crate::module::check_module_version();
        }
        "reboot" => {
            notify("Daemon Info", "Configuration updated. Please reboot your device to take full effect.", false, 0, None);
        }
        "freqoffset" => {
            let path = "/data/adb/.config/AZenith/freqoffset";
            if let Ok(s) = fileops::read_string_result(path) {
                let trimmed = s.trim().to_string();
                let bytes = trimmed.as_bytes();
                let n = bytes.len().min(ctx.config_freqoffset.len() - 1);
                ctx.config_freqoffset[..n].copy_from_slice(&bytes[..n]);
                ctx.config_freqoffset[n] = 0;
                log_info!("inotify", "freqoffset updated value={}", ctx.config_freqoffset_str());
            }
        }
        "bypasspath" => {
            let path = "/data/adb/.config/AZenith/bypasschgconfig/bypasspath";
            if let Ok(s) = fileops::read_string_result(path) {
                let trimmed = s.trim().to_string();
                let bytes = trimmed.as_bytes();
                let n = bytes.len().min(ctx.bypass.config_bypasspath.len() - 1);
                ctx.bypass.config_bypasspath[..n].copy_from_slice(&bytes[..n]);
                ctx.bypass.config_bypasspath[n] = 0;
            }
        }
        "bypasschg" => {
            let path = "/data/adb/.config/AZenith/bypasschgconfig/bypasschg";
            if let Ok(s) = fileops::read_string_result(path) {
                let val: i32 = s.trim().parse().unwrap_or(0);
                ctx.bypass.config_bypasschg = val;
            }
        }
        "bypasschgthreshold" => {
            let path = "/data/adb/.config/AZenith/bypasschgconfig/bypasschgthreshold";
            if let Ok(s) = fileops::read_string_result(path) {
                let val: i32 = s.trim().parse().unwrap_or(0);
                ctx.bypass.config_bypasschgthreshold = val;
            }
        }
        _ => {}
    }
}

fn notify(title: &str, text: &str, chrono: bool, timeout_ms: i32, _args: Option<&[&str]>) {
    crate::util::notify(title, text, chrono, timeout_ms, None);
}

#[cfg(test)]
mod tests {
    use super::for_each_inotify_event;

    fn pack_event(buf: &mut Vec<u8>, wd: i32, mask: u32, name: &[u8]) {
        // Header (wd, mask, cookie, len) + name with its NUL terminator.
        buf.extend_from_slice(&wd.to_ne_bytes());
        buf.extend_from_slice(&mask.to_ne_bytes());
        buf.extend_from_slice(&0u32.to_ne_bytes());
        buf.extend_from_slice(&((name.len() + 1) as u32).to_ne_bytes());
        buf.extend_from_slice(name);
        buf.push(0);
    }

    #[test]
    fn tmp_rename_batch_decodes_both_events() {
        // Mimics sysdaemon write_atomic: CREATE app_status.0.tmp ... then
        // MOVED_TO app_status. The second event starts at offset 33
        // (unaligned) — the exact case that broke the old `&*ptr` parsing.
        let mut batch = Vec::new();
        pack_event(&mut batch, 1, 0x100, b"app_status.0.tmp"); // CREATE
        pack_event(&mut batch, 1, 0x80, b"app_status"); // MOVED_TO

        let mut got = Vec::new();
        for_each_inotify_event(&batch, batch.len(), |wd, name, mask| {
            got.push((wd, name.to_string(), mask));
        });

        assert_eq!(
            got,
            vec![
                (1, "app_status.0.tmp".to_string(), 0x100),
                (1, "app_status".to_string(), 0x80),
            ]
        );
    }
}
