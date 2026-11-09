//! File IO and locking, mirroring `jni/src/io/`.

use crate::core::LOCK_FILE;

use std::fs;
use std::io::Write;
use std::os::fd::AsRawFd;
use std::os::raw::c_int;
use std::sync::OnceLock;

// Held for the daemon's lifetime so the lock is not released after
// `check_running_state()` returns (mirrors C behavior where fd stays open).
static DAEMON_LOCK: OnceLock<std::os::fd::OwnedFd> = OnceLock::new();

// ---------------------------------------------------------------------------
// write2file
// ---------------------------------------------------------------------------
pub fn write2file(
    filename: &str,
    append: bool,
    use_flock: bool,
    data: &str,
) -> c_int {
    let content = data.as_bytes();
    let mut options = fs::OpenOptions::new();
    options.write(true).create(true);
    if append {
        options.append(true);
    } else {
        options.truncate(true);
    }

    let mut file = match options.open(filename) {
        Ok(f) => f,
        Err(_) => return -1,
    };

    if use_flock {
        if flock_exclusive(&mut file).is_err() {
            return -1;
        }
    }

    let written = file.write(content);
    if use_flock {
        let _ = file.unlock();
    }

    match written {
        Ok(n) if n == content.len() => 0,
        _ => -1,
    }
}

fn flock_exclusive(file: &mut fs::File) -> std::io::Result<()> {
    use std::os::fd::AsRawFd;
    unsafe { libc::flock(file.as_raw_fd(), libc::LOCK_EX) };
    Ok(())
}

trait FileUnlock {
    fn unlock(&mut self);
}

impl FileUnlock for fs::File {
    fn unlock(&mut self) {
        use std::os::fd::AsRawFd;
        unsafe { libc::flock(self.as_raw_fd(), libc::LOCK_UN) };
    }
}

// ---------------------------------------------------------------------------
// is_file_empty
// ---------------------------------------------------------------------------
pub fn is_file_empty(filename: &str) -> i32 {
    let file = match fs::File::open(filename) {
        Ok(f) => f,
        Err(_) => return -1,
    };
    let metadata = match file.metadata() {
        Ok(m) => m,
        Err(_) => return -1,
    };
    if metadata.len() == 0 {
        1
    } else {
        0
    }
}

// ---------------------------------------------------------------------------
// is_java_lock_held
// ---------------------------------------------------------------------------
pub fn is_java_lock_held(lock_path: &str) -> bool {
    let fd = match open_rdonly(lock_path) {
        Ok(fd) => fd,
        Err(_) => return false,
    };

    let held = unsafe {
        let mut fl = std::mem::zeroed::<libc::flock>();
        fl.l_type = libc::F_WRLCK as i16;
        fl.l_whence = libc::SEEK_SET as i16;
        fl.l_start = 0;
        fl.l_len = 0;
        libc::fcntl(fd.as_raw_fd(), libc::F_GETLK, &mut fl as *mut libc::flock) != -1 && fl.l_type != libc::F_UNLCK as i16
    };

    unsafe { libc::close(fd.as_raw_fd()) };
    held
}

fn open_rdonly(path: &str) -> std::io::Result<std::os::fd::OwnedFd> {
    use std::os::fd::FromRawFd;
    let c = std::ffi::CString::new(path).unwrap();
    let fd = unsafe { libc::open(c.as_ptr(), libc::O_RDONLY) };
    if fd < 0 {
        return Err(std::io::Error::last_os_error());
    }
    unsafe { Ok(std::os::fd::OwnedFd::from_raw_fd(fd)) }
}

// ---------------------------------------------------------------------------
// check_running_state
// ---------------------------------------------------------------------------
pub fn check_running_state() -> bool {
    let fd = match open_rwcreate(LOCK_FILE) {
        Ok(fd) => fd,
        Err(_) => return false,
    };

    let result = unsafe {
        libc::flock(fd.as_raw_fd(), libc::LOCK_EX | libc::LOCK_NB) == 0
    };

    if result {
        // Hold the fd for the daemon's lifetime so the lock is not released.
        let _ = DAEMON_LOCK.set(fd);
    }

    result
}

fn open_rwcreate(path: &str) -> std::io::Result<std::os::fd::OwnedFd> {
    use std::os::fd::FromRawFd;
    let c = std::ffi::CString::new(path).unwrap();
    let fd = unsafe { libc::open(c.as_ptr(), libc::O_WRONLY | libc::O_CREAT, 0o644) };
    if fd < 0 {
        return Err(std::io::Error::last_os_error());
    }
    unsafe { Ok(std::os::fd::OwnedFd::from_raw_fd(fd)) }
}
