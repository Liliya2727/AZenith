// ---------------- logging ----------------
//
// Everything goes to one file:
//
//   /data/adb/.config/AZenith/sysmon.log
//
//   YYYY-MM-DD HH:MM:SS.mmm LEVEL [component] message
//
// The file is rotated (and therefore bounded in size), stdout/stderr are
// redirected into it as well, so a panic or a C level abort ends up there too.
use std::ffi::CString;
use std::fs::{self, OpenOptions};
use std::io::Write;
use std::os::raw::{c_char, c_int};
use std::os::unix::fs::PermissionsExt;
use std::os::unix::io::AsRawFd;
use std::sync::atomic::{AtomicU8, Ordering};

pub(crate) const LOG_DIR: &str = "/data/adb/.config/AZenith";
pub(crate) const LOG_FILE: &str = "/data/adb/.config/AZenith/sysmon.log";

/// Component tag bracketed in every line.
const COMPONENT: &str = "daemon";

/// Rotate once the file crosses this size, keeping LOG_KEEP_FILES generations
/// (<file>.1 .. <file>.N). The log can never grow bigger than
/// LOG_MAX_BYTES * (LOG_KEEP_FILES + 1).
const LOG_MAX_BYTES: u64 = 128 * 1024;
const LOG_KEEP_FILES: usize = 2;

const ANDROID_LOG_DEBUG: c_int = 3;
const ANDROID_LOG_INFO: c_int = 4;
const ANDROID_LOG_WARN: c_int = 5;
const ANDROID_LOG_ERROR: c_int = 6;

#[link(name = "log")]
unsafe extern "C" {
    fn __android_log_print(prio: c_int, tag: *const c_char, fmt: *const c_char, ...) -> c_int;
}

#[derive(Clone, Copy)]
#[repr(u8)]
pub(crate) enum Lv { Debug = 0, Info = 1, Warn = 2, Error = 3 }

static LEVEL: AtomicU8 = AtomicU8::new(Lv::Info as u8);

pub(crate) fn level_name() -> &'static str {
    ["debug", "info", "warn", "error"][LEVEL.load(Ordering::Relaxed) as usize]
}

macro_rules! debug { ($($a:tt)*) => { $crate::log::log_write($crate::log::Lv::Debug, &format!($($a)*)) }; }
macro_rules! info  { ($($a:tt)*) => { $crate::log::log_write($crate::log::Lv::Info,  &format!($($a)*)) }; }
// different internal name: a plain `use warn` re-export clashes with the built-in `#[warn]` attribute
macro_rules! warn_internal { ($($a:tt)*) => { $crate::log::log_write($crate::log::Lv::Warn,  &format!($($a)*)) }; }
macro_rules! error { ($($a:tt)*) => { $crate::log::log_write($crate::log::Lv::Error, &format!($($a)*)) }; }
pub(crate) use debug;
pub(crate) use error;
pub(crate) use info;
pub(crate) use warn_internal as warn;

fn ts() -> String {
    unsafe {
        let mut t: libc::timespec = std::mem::zeroed();
        libc::clock_gettime(libc::CLOCK_REALTIME, &mut t);
        let mut tm: libc::tm = std::mem::zeroed();
        libc::localtime_r(&t.tv_sec, &mut tm);
        let mut buf = [0u8; 32];
        let n = libc::strftime(buf.as_mut_ptr() as *mut c_char, buf.len(), c"%Y-%m-%d %H:%M:%S".as_ptr(), &tm);
        format!("{}.{:03}", String::from_utf8_lossy(&buf[..n]), t.tv_nsec / 1_000_000)
    }
}

fn prio(lv: Lv) -> c_int {
    match lv {
        Lv::Debug => ANDROID_LOG_DEBUG,
        Lv::Info => ANDROID_LOG_INFO,
        Lv::Warn => ANDROID_LOG_WARN,
        Lv::Error => ANDROID_LOG_ERROR,
    }
}

/// Mirror one line to logcat. The body goes through `%s` so a literal `%`
/// inside a message can never be treated as a format specifier.
fn logcat(lv: Lv, message: &str) {
    let Ok(tag) = CString::new(COMPONENT) else { return };
    let Ok(msg) = CString::new(message.replace('\0', "")) else { return };
    let fmt = c"%s";
    unsafe {
        (__android_log_print)(prio(lv), tag.as_ptr(), fmt.as_ptr(), msg.as_ptr());
    }
}

/// Shift <path> -> <path>.1 -> <path>.N once it exceeds LOG_MAX_BYTES. On any
/// failure we just keep appending; logging must never take the daemon down.
fn rotate_if_needed(path: &str) {
    let Ok(meta) = fs::metadata(path) else { return };
    if meta.len() < LOG_MAX_BYTES {
        return;
    }

    for i in (1..=LOG_KEEP_FILES).rev() {
        let src = if i == 1 { path.to_string() } else { format!("{}.{}", path, i - 1) };
        let dst = format!("{}.{}", path, i);

        let _ = fs::remove_file(&dst);
        let _ = fs::rename(&src, &dst);
    }
}

/// Append one line, serialized against concurrent writers (binder thread
/// pool, event worker) with an exclusive `flock`.
fn append(path: &str, line: &str) {
    rotate_if_needed(path);

    let Ok(mut file) = OpenOptions::new().create(true).append(true).open(path) else { return };
    let fd = file.as_raw_fd();
    unsafe { libc::flock(fd, libc::LOCK_EX) };
    let _ = file.write_all(line.as_bytes());
    unsafe { libc::flock(fd, libc::LOCK_UN) };
}

pub(crate) fn log_init() {
    let lv = match std::env::var("LIBBINDING_LOG").ok().as_deref() {
        Some("debug") => Lv::Debug,
        Some("info") => Lv::Info,
        Some("warn") => Lv::Warn,
        Some("error") => Lv::Error,
        _ => Lv::Info,
    };
    LEVEL.store(lv as u8, Ordering::Relaxed);
}

pub(crate) fn log_write(lv: Lv, msg: &str) {
    if (lv as u8) < LEVEL.load(Ordering::Relaxed) { return; }
    let level = match lv { Lv::Debug => "DEBUG", Lv::Info => "INFO", Lv::Warn => "WARN", Lv::Error => "ERROR" };
    let _ = fs::create_dir_all(LOG_DIR);
    let line = format!("{} {} [{}] {}\n", ts(), level, COMPONENT, msg);
    append(LOG_FILE, &line);
    logcat(lv, &format!("[{COMPONENT}] {msg}"));
}

/// Send stdout/stderr to the log file and stdin to /dev/null, so the daemon
/// does not keep any terminal or pipe open and every panic message is
/// captured.
pub(crate) fn redirect_stdio() {
    let Ok(log) = OpenOptions::new().create(true).append(true).open(LOG_FILE) else { return };
    // same permissions as the manager/config files, so the app can read it
    let _ = fs::set_permissions(LOG_FILE, fs::Permissions::from_mode(0o666));
    let log_fd = log.as_raw_fd();
    unsafe {
        libc::dup2(log_fd, 1);
        libc::dup2(log_fd, 2);
    }
    if let Ok(null) = OpenOptions::new().write(true).open("/dev/null") {
        let null_fd = null.as_raw_fd();
        unsafe { libc::dup2(null_fd, 0) };
    }
}

/// Write the crash information of a panic into the log file.
pub(crate) fn crash(msg: &str) {
    let line = format!("{} FATAL [{}] {}\n", ts(), COMPONENT, msg);
    append(LOG_FILE, &line);
    logcat(Lv::Error, &format!("[{COMPONENT}] FATAL {msg}"));
}

/// Panic hook: log the panic (and its location) before the process dies.
pub(crate) fn install_panic_hook() {
    let previous = std::panic::take_hook();
    std::panic::set_hook(Box::new(move |info| {
        let loc = info
            .location()
            .map(|l| format!("{}:{}:{}", l.file(), l.line(), l.column()))
            .unwrap_or_else(|| "unknown location".to_string());
        crash(&format!("panic at {loc}: {info}"));
        previous(info);
    }));
}

/// Drop every generation of the log file (`--clearlogs`).
pub(crate) fn clear_logs() {
    let _ = fs::remove_file(LOG_FILE);
    for i in 1..=LOG_KEEP_FILES {
        let _ = fs::remove_file(format!("{LOG_FILE}.{i}"));
    }
}
