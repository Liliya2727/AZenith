// ---------------- instance lock (java.lock) ----------------
//
// The lock file is the same one the Java companion daemon used, so the C daemon
// (is_java_lock_held) keeps detecting "a java companion is alive".
//
// fcntl(F_SETLK) is used on purpose: it is the very same lock type as
// FileChannel.tryLock() in Java, so a still running java companion is detected
// as well. flock() would NOT be detected by Java (and Java locks are not
// detected by flock()).
//
// POSIX record locks are not inherited over fork(), that is why the lock is
// taken after daemon() - see main().
use std::ffi::CString;
use std::os::unix::io::RawFd;
use std::time::Duration;

use crate::log::{error, info, warn};

pub(crate) struct InstanceLock {
    fd: RawFd,
}

impl Drop for InstanceLock {
    fn drop(&mut self) {
        // Closing the fd releases the fcntl lock as well.
        unsafe { libc::close(self.fd) };
    }
}

fn errno() -> i32 { unsafe { *libc::__errno() } }

fn open(path: &str) -> Option<RawFd> {
    let Ok(c) = CString::new(path) else { return None };
    let fd = unsafe {
        libc::open(c.as_ptr(), libc::O_RDWR | libc::O_CREAT | libc::O_CLOEXEC, 0o666)
    };
    if fd < 0 {
        error!("cannot open {path}: errno={}", errno());
        return None;
    }
    Some(fd)
}

fn flock_of(kind: i16) -> libc::flock {
    libc::flock {
        l_type: kind,
        l_whence: libc::SEEK_SET as i16,
        l_start: 0,
        l_len: 0,
        l_pid: 0,
    }
}

/// True when another process holds the lock (F_GETLK only reports locks of
/// other processes, our own lock is invisible to it).
pub(crate) fn held_by_other(path: &str) -> bool {
    let Some(fd) = open(path) else { return false };
    let mut fl = flock_of(libc::F_WRLCK as i16);
    let r = unsafe { libc::fcntl(fd, libc::F_GETLK, &mut fl) };
    unsafe { libc::close(fd) };
    if r < 0 {
        warn!("F_GETLK failed on {path}: errno={}", errno());
        return false;
    }
    fl.l_type != libc::F_UNLCK as i16
}

/// Take the exclusive lock. Non blocking, with a few retries so that a
/// just starting instance does not lose the race.
pub(crate) fn acquire(path: &str) -> Option<InstanceLock> {
    let fd = open(path)?;
    for _ in 0..20 {
        let mut fl = flock_of(libc::F_WRLCK as i16);
        let r = unsafe { libc::fcntl(fd, libc::F_SETLK, &mut fl) };
        if r == 0 {
            info!("instance lock acquired: {path}");
            return Some(InstanceLock { fd });
        }
        let e = errno();
        if e != libc::EAGAIN && e != libc::EACCES { break; }
        std::thread::sleep(Duration::from_millis(100));
    }
    error!("cannot acquire the instance lock {path}: errno={}", errno());
    unsafe { libc::close(fd) };
    None
}
