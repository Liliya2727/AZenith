//! Android C bindings (liblog + bionic system properties + libc).
//!
//! Mirrors the declarations the C archdaemon relies on:
//! `<android/log.h>`, `<sys/system_properties.h>`, `<fcntl.h>`, `<sys/file.h>`,
//! `<unistd.h>`, `<sys/wait.h>`, `<sys/socket.h>`, `<sys/un.h>`, `<netinet/in.h>`,
//! `<arpa/inet.h>`, `<sys/select.h>`, `<sys/poll.h>`, `<sys/inotify.h>`,
//! `<dirent.h>`, `<glob.h>`, `<sys/stat.h>`, `<time.h>`, `<signal.h>`, `<sched.h>`.
//!
//! Not every declaration is used on every device/configuration, so dead code
//! warnings are suppressed for this FFI surface.
#![allow(dead_code)]
#![allow(non_camel_case_types)]

use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int, c_uchar, c_uint, c_long, c_short, c_void};

// ---------------------------------------------------------------------------
// Priorities from <android/log.h>
// ---------------------------------------------------------------------------
pub const ANDROID_LOG_DEBUG: c_int = 3;
pub const ANDROID_LOG_INFO: c_int = 4;
pub const ANDROID_LOG_WARN: c_int = 5;
pub const ANDROID_LOG_ERROR: c_int = 6;
pub const ANDROID_LOG_FATAL: c_int = 7;

// ---------------------------------------------------------------------------
// inotify masks from <sys/inotify.h>
// ---------------------------------------------------------------------------
pub const IN_ACCESS: c_uint = 0x00000001;
pub const IN_MODIFY: c_uint = 0x00000002;
pub const IN_ATTRIB: c_uint = 0x00000004;
pub const IN_CLOSE_WRITE: c_uint = 0x00000008;
pub const IN_CLOSE_NOWRITE: c_uint = 0x00000010;
pub const IN_OPEN: c_uint = 0x00000020;
pub const IN_MOVED_FROM: c_uint = 0x00000040;
pub const IN_MOVED_TO: c_uint = 0x00000080;
pub const IN_CREATE: c_uint = 0x00000100;
pub const IN_DELETE: c_uint = 0x00000200;
pub const IN_DELETE_SELF: c_uint = 0x00000400;
pub const IN_MOVE_SELF: c_uint = 0x00000800;
pub const IN_UNMOUNT: c_uint = 0x00002000;
pub const IN_Q_OVERFLOW: c_uint = 0x00004000;
pub const IN_IGNORED: c_uint = 0x00008000;
pub const IN_ONLYDIR: c_uint = 0x01000000;
pub const IN_DONT_FOLLOW: c_uint = 0x02000000;
pub const IN_EXCL_UNLINK: c_uint = 0x04000000;
pub const IN_MASK_ADD: c_uint = 0x20000000;
pub const IN_ISDIR: c_uint = 0x40000000;
pub const IN_ONESHOT: c_uint = 0x80000000;

pub const IN_CLOEXEC: c_int = 0o2000000; // O_CLOEXEC
pub const IN_NONBLOCK: c_int = 0o4000;   // O_NONBLOCK

// ---------------------------------------------------------------------------
// flock operations from <sys/file.h>
// ---------------------------------------------------------------------------
pub const LOCK_SH: c_int = 1;
pub const LOCK_EX: c_int = 2;
pub const LOCK_NB: c_int = 4;
pub const LOCK_UN: c_int = 8;

// ---------------------------------------------------------------------------
// fcntl operations
// ---------------------------------------------------------------------------
pub const F_GETLK: c_int = 5;
pub const F_SETLK: c_int = 6;
pub const F_SETLKW: c_int = 7;
pub const F_WRLCK: c_short = 1;

// ---------------------------------------------------------------------------
// O_* flags from <fcntl.h>
// ---------------------------------------------------------------------------
pub const O_RDONLY: c_int = 0;
pub const O_WRONLY: c_int = 1;
pub const O_RDWR: c_int = 2;
pub const O_CREAT: c_int = 0o100;
pub const O_APPEND: c_int = 0o2000;
pub const O_TRUNC: c_int = 0o1000;
pub const O_NONBLOCK: c_int = 0o4000;

// ---------------------------------------------------------------------------
// System property limits
// ---------------------------------------------------------------------------
pub const PROP_VALUE_MAX: usize = 92;

// ---------------------------------------------------------------------------
// PATH_MAX
// ---------------------------------------------------------------------------
pub const PATH_MAX: usize = 4096;

// ---------------------------------------------------------------------------
// time constants
// ---------------------------------------------------------------------------
pub const CLOCK_MONOTONIC: c_int = 1;

// ---------------------------------------------------------------------------
// FFI: liblog
// ---------------------------------------------------------------------------
#[link(name = "log")]
unsafe extern "C" {
    pub fn __android_log_print(
        prio: c_int,
        tag: *const c_char,
        fmt: *const c_char,
        ...
    ) -> c_int;
}

// ---------------------------------------------------------------------------
// FFI: bionic system properties
// ---------------------------------------------------------------------------
#[link(name = "c")]
unsafe extern "C" {
    pub fn __system_property_get(name: *const c_uchar, value: *mut c_uchar) -> c_int;
    pub fn __system_property_set(name: *const c_char, value: *const c_char) -> c_int;
    pub fn __system_property_foreach(callback: *mut c_void, cookie: *mut c_void);
    pub fn __system_property_read_callback(
        pi: *const c_void,
        callback: *mut c_void,
        cookie: *mut c_void,
    );
}

// prop_info is an opaque type
pub type prop_info = c_void;

// ---------------------------------------------------------------------------
// FFI: libc (declared here so we don't depend on the `libc` crate for these)
// ---------------------------------------------------------------------------
#[link(name = "c")]
unsafe extern "C" {
    pub fn daemon(nochdir: c_int, noclose: c_int) -> c_int;
    pub fn fork() -> c_int;
    pub fn waitpid(pid: c_int, stat_loc: *mut c_int, options: c_int) -> c_int;
    pub fn execle(
        path: *const c_char,
        arg0: *const c_char,
        ...
    ) -> c_int;
    pub fn _exit(status: c_int);
    pub fn getpid() -> c_int;
    pub fn getuid() -> c_uint;
    pub fn geteuid() -> c_uint;
    pub fn setpriority(which: c_int, who: c_int, prio: c_int) -> c_int;
    pub fn syscall(number: c_long, ...) -> c_long;
    pub fn pipe(fds: *mut c_int) -> c_int;
    pub fn open(path: *const c_char, flags: c_int, ...) -> c_int;
    pub fn close(fd: c_int) -> c_int;
    pub fn read(fd: c_int, buf: *mut c_void, count: usize) -> isize;
    pub fn write(fd: c_int, buf: *const c_void, count: usize) -> isize;
    pub fn access(path: *const c_char, mode: c_int) -> c_int;
    pub fn chmod(path: *const c_char, mode: c_uint) -> c_int;
    pub fn stat(path: *const c_char, buf: *mut c_void) -> c_int;
    pub fn remove(path: *const c_char) -> c_int;
    pub fn rename(old: *const c_char, new: *const c_char) -> c_int;
    pub fn sleep(seconds: c_uint) -> c_uint;
    pub fn usleep(useconds: c_uint) -> c_int;
    pub fn signal(signum: c_int, handler: *mut c_void) -> *mut c_void;
    pub fn fcntl(fd: c_int, cmd: c_int, ...) -> c_int;
    pub fn flock(fd: c_int, operation: c_int) -> c_int;
    pub fn glob(pattern: *const c_char, flags: c_int, err: *mut c_void, pglob: *mut c_void) -> c_int;
    pub fn globfree(pglob: *mut c_void);
    pub fn opendir(path: *const c_char) -> *mut c_void;
    pub fn readdir(dirp: *mut c_void) -> *mut c_void;
    pub fn closedir(dirp: *mut c_void) -> *mut c_void;
    pub fn strerror(errnum: c_int) -> *const c_char;
    pub fn popen(command: *const c_char, mode: *const c_char) -> *mut c_void;
    pub fn pclose(stream: *mut c_void) -> c_int;
    pub fn setenv(name: *const c_char, value: *const c_char, overwrite: c_int) -> c_int;
    pub fn unsetenv(name: *const c_char) -> c_int;
    pub fn getenv(name: *const c_char) -> *const c_char;
    pub fn localtime(timep: *const c_long) -> *mut c_void;
    pub fn localtime_r(timep: *const c_long, result: *mut c_void) -> *mut c_void;
    pub fn strftime(
        s: *mut c_char,
        max: usize,
        fmt: *const c_char,
        tm: *mut c_void,
    ) -> usize;
    pub fn clock_gettime(clk_id: c_int, tp: *mut c_void) -> c_int;
    pub fn gettimeofday(tv: *mut c_void, tz: *mut c_void) -> c_int;
    pub fn snprintf(
        s: *mut c_char,
        n: usize,
        fmt: *const c_char,
        ...
    ) -> c_int;
    pub fn vsnprintf(
        s: *mut c_char,
        n: usize,
        fmt: *const c_char,
        args: *mut c_void,
    ) -> c_int;
    pub fn malloc(size: usize) -> *mut c_void;
    pub fn free(ptr: *mut c_void);
    pub fn strdup(s: *const c_char) -> *mut c_char;
    pub fn strlen(s: *const c_char) -> usize;
    pub fn strcmp(s1: *const c_char, s2: *const c_char) -> c_int;
    pub fn strncmp(s1: *const c_char, s2: *const c_char, n: usize) -> c_int;
    pub fn strstr(haystack: *const c_char, needle: *const c_char) -> *const c_char;
    pub fn strchr(s: *const c_char, c: c_int) -> *const c_char;
    pub fn strrchr(s: *const c_char, c: c_int) -> *const c_char;
    pub fn strncpy(dest: *mut c_char, src: *const c_char, n: usize) -> *mut c_char;
    pub fn strcat(dest: *mut c_char, src: *const c_char) -> *mut c_char;
    pub fn memcpy(dest: *mut c_void, src: *const c_void, n: usize) -> *mut c_void;
    pub fn memset(s: *mut c_void, c: c_int, n: usize) -> *mut c_void;
    pub fn setvbuf(stream: *mut c_void, buf: *mut c_char, mode: c_int, size: usize) -> c_int;
    pub fn stdout() -> *mut c_void;
    pub fn stderr() -> *mut c_void;
    pub fn fprintf(stream: *mut c_void, fmt: *const c_char, ...) -> c_int;
    pub fn printf(fmt: *const c_char, ...) -> c_int;
    pub fn fgets(buf: *mut c_char, size: c_int, stream: *mut c_void) -> *mut c_char;
    pub fn fopen(path: *const c_char, mode: *const c_char) -> *mut c_void;
    pub fn fclose(stream: *mut c_void) -> c_int;
    pub fn fread(ptr: *mut c_void, size: usize, nmemb: usize, stream: *mut c_void) -> usize;
    pub fn fwrite(ptr: *const c_void, size: usize, nmemb: usize, stream: *mut c_void) -> usize;
    pub fn fseek(stream: *mut c_void, offset: c_long, whence: c_int) -> c_int;
    pub fn ftell(stream: *mut c_void) -> c_long;
    pub fn rewind(stream: *mut c_void);
    pub fn fflush(stream: *mut c_void) -> c_int;
    pub fn feof(stream: *mut c_void) -> c_int;
    pub fn fgetc(stream: *mut c_void) -> c_int;
    pub fn fputs(s: *const c_char, stream: *mut c_void) -> c_int;
    pub fn fputc(c: c_int, stream: *mut c_void) -> c_int;
}

// ---------------------------------------------------------------------------
// errno
// ---------------------------------------------------------------------------
#[link(name = "c")]
unsafe extern "C" {
    pub fn errno_location() -> *mut c_int;
}

// ---------------------------------------------------------------------------
// inotify via libc (used directly, mirroring the C code)
// ---------------------------------------------------------------------------
#[link(name = "c")]
unsafe extern "C" {
    pub fn inotify_init() -> c_int;
    pub fn inotify_init1(flags: c_int) -> c_int;
    pub fn inotify_add_watch(fd: c_int, pathname: *const c_char, mask: c_uint) -> c_int;
    pub fn inotify_rm_watch(fd: c_int, wd: c_int) -> c_int;
}

// ---------------------------------------------------------------------------
// Helper: property get / set (safe wrappers)
// ---------------------------------------------------------------------------

pub fn prop_get(name: &str) -> Option<String> {
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
    Some(unsafe { CStr::from_ptr(buf.as_ptr() as *const c_char) }.to_string_lossy().into_owned())
}

pub fn prop_set(name: &str, value: &str) -> bool {
    let Ok(c_name) = CString::new(name) else { return false };
    let Ok(c_value) = CString::new(value) else { return false };
    unsafe { __system_property_set(c_name.as_ptr(), c_value.as_ptr()) == 0 }
}

// ---------------------------------------------------------------------------
// Helper: Android log (safe wrapper)
// ---------------------------------------------------------------------------
pub fn logcat(prio: c_int, tag: &str, message: &str) {
    let Ok(c_tag) = CString::new(tag.replace('\0', "")) else { return };
    let Ok(c_msg) = CString::new(message.replace('\0', "")) else { return };
    let fmt = c"%s";
    unsafe {
        __android_log_print(prio, c_tag.as_ptr(), fmt.as_ptr(), c_msg.as_ptr());
    }
}

// ---------------------------------------------------------------------------
// Safe errno wrapper
// ---------------------------------------------------------------------------
pub fn errno() -> c_int {
    unsafe { *errno_location() }
}
