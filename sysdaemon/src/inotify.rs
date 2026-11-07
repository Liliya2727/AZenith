// Copyright (C) 2026 Zexshia
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, version 3.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
// ---------------- inotify: event-driven file watcher ----------------
//
// `watch(path, on_change)` spawns a thread that calls `on_change` once right away and then again
// every time `path` is created, rewritten, replaced or removed.
//
// The *parent directory* is watched and events are filtered by file name, not the file itself:
// the writers here use write-to-temp + rename (atomic), which swaps the inode, and a watch on the
// old inode would go silent after the first update. The same trick covers the file not existing
// yet when the watch is armed.
//
// Safety nets, because a missed event must never leave the caller with a stale state:
//   - the watch is armed *before* the first `on_change`, so nothing slips in between;
//   - `on_change` is also called when the queue overflowed (IN_Q_OVERFLOW);
//   - `on_change` is also called every `RESYNC_INTERVAL` even if nothing happened;
//   - if the watched directory is deleted/moved/unmounted the watch is re-armed;
//   - if inotify cannot be used at all, the thread falls back to calling `on_change` every
//     `FALLBACK_POLL`, and keeps trying to arm inotify in between.
//
// `on_change` must therefore be cheap and idempotent: it only re-reads the file and compares.

use std::ffi::CString;
use std::fs;
use std::io;
use std::os::fd::{AsRawFd, FromRawFd, OwnedFd};
use std::os::unix::ffi::OsStrExt;
use std::path::Path;
use std::thread;
use std::time::{Duration, Instant};

use crate::log::{info, warn};

// Kernel ABI values (<linux/inotify.h>), spelled out so the integer types never depend on the
// libc crate version.
const IN_MODIFY: u32 = 0x0000_0002;
const IN_CLOSE_WRITE: u32 = 0x0000_0008;
const IN_MOVED_FROM: u32 = 0x0000_0040;
const IN_MOVED_TO: u32 = 0x0000_0080;
const IN_CREATE: u32 = 0x0000_0100;
const IN_DELETE: u32 = 0x0000_0200;
const IN_DELETE_SELF: u32 = 0x0000_0400;
const IN_MOVE_SELF: u32 = 0x0000_0800;
// Reported by the kernel without being requested.
const IN_UNMOUNT: u32 = 0x0000_2000;
const IN_Q_OVERFLOW: u32 = 0x0000_4000;
const IN_IGNORED: u32 = 0x0000_8000;

/// IN_MODIFY covers writers that rewrite in place, IN_CLOSE_WRITE/IN_MOVED_TO the usual
/// "write then close" and "rename over" patterns.
const WATCH_MASK: u32 = IN_MODIFY
    | IN_CLOSE_WRITE
    | IN_MOVED_FROM
    | IN_MOVED_TO
    | IN_CREATE
    | IN_DELETE
    | IN_DELETE_SELF
    | IN_MOVE_SELF;

/// Size of `struct inotify_event` without the trailing name: wd, mask, cookie, len.
const EVENT_HEADER: usize = 16;

/// Call `on_change` at least this often even when no event arrived.
const RESYNC_INTERVAL: Duration = Duration::from_secs(30);
/// Poll cadence while inotify is unavailable.
const FALLBACK_POLL: Duration = Duration::from_secs(1);
/// Pause before the watch is armed again after it was lost or failed.
const REARM_DELAY: Duration = Duration::from_secs(1);

/// Why `Watcher::wait` returned.
enum Wake {
    /// The target file (or the event queue as a whole) changed.
    Changed,
    /// Nothing relevant happened within the timeout.
    Timeout,
    /// The watch is gone (directory removed, moved or unmounted); it has to be re-armed.
    Lost,
}

struct Watcher {
    fd: OwnedFd,
    /// File name inside the watched directory, as raw bytes.
    name: Vec<u8>,
}

impl Watcher {
    fn new(path: &str) -> io::Result<Self> {
        let path = Path::new(path);
        let invalid = |msg: &'static str| io::Error::new(io::ErrorKind::InvalidInput, msg);
        let dir = path.parent().ok_or_else(|| invalid("path has no parent directory"))?;
        let name = path.file_name().ok_or_else(|| invalid("path has no file name"))?;
        let cdir = CString::new(dir.as_os_str().as_bytes())
            .map_err(|_| invalid("path contains a NUL byte"))?;

        // SAFETY: plain syscall, the result is checked right below.
        let raw = unsafe { libc::inotify_init1(libc::IN_CLOEXEC | libc::IN_NONBLOCK) };
        if raw < 0 {
            return Err(io::Error::last_os_error());
        }
        // SAFETY: `raw` is a freshly created, valid descriptor that nothing else owns.
        let fd = unsafe { OwnedFd::from_raw_fd(raw) };

        // SAFETY: `cdir` is NUL terminated and outlives the call.
        let wd = unsafe { libc::inotify_add_watch(fd.as_raw_fd(), cdir.as_ptr(), WATCH_MASK) };
        if wd < 0 {
            return Err(io::Error::last_os_error());
        }

        Ok(Self { fd, name: name.as_bytes().to_vec() })
    }

    /// Block until the target file changes, the watch is lost or `timeout` passes. Events about
    /// other files in the same directory are swallowed and do not shorten the wait.
    fn wait(&self, timeout: Duration) -> io::Result<Wake> {
        let deadline = Instant::now() + timeout;

        loop {
            let left = deadline.saturating_duration_since(Instant::now());
            if left.is_zero() {
                return Ok(Wake::Timeout);
            }
            let ms = left.as_millis().clamp(1, i32::MAX as u128) as libc::c_int;

            let mut pfd = libc::pollfd { fd: self.fd.as_raw_fd(), events: libc::POLLIN, revents: 0 };
            // SAFETY: `pfd` is a valid pollfd and the count matches.
            let rc = unsafe { libc::poll(&mut pfd, 1, ms) };
            if rc < 0 {
                let e = io::Error::last_os_error();
                if e.kind() == io::ErrorKind::Interrupted {
                    continue;
                }
                return Err(e);
            }
            if rc == 0 {
                return Ok(Wake::Timeout);
            }

            if let Some(wake) = self.drain()? {
                return Ok(wake);
            }
        }
    }

    /// Read every queued event. `None` means none of them concerned the target file.
    fn drain(&self) -> io::Result<Option<Wake>> {
        let mut buf = [0u8; 4096];
        let mut changed = false;
        let mut lost = false;

        loop {
            // SAFETY: `buf` is valid for `buf.len()` bytes for the duration of the call.
            let n = unsafe { libc::read(self.fd.as_raw_fd(), buf.as_mut_ptr().cast(), buf.len()) };
            if n < 0 {
                let e = io::Error::last_os_error();
                match e.kind() {
                    io::ErrorKind::WouldBlock => break,
                    io::ErrorKind::Interrupted => continue,
                    _ => return Err(e),
                }
            }
            if n == 0 {
                break;
            }

            let data = &buf[..n as usize];
            let mut off = 0usize;
            while off + EVENT_HEADER <= data.len() {
                let mask = read_u32(data, off + 4);
                let len = read_u32(data, off + 12) as usize;
                let name_start = off + EVENT_HEADER;
                let name_end = (name_start + len).min(data.len());
                // The kernel pads the name with NULs up to `len`.
                let name = data[name_start..name_end].split(|b| *b == 0).next().unwrap_or(&[]);
                off = name_start + len;

                if mask & (IN_DELETE_SELF | IN_MOVE_SELF | IN_UNMOUNT | IN_IGNORED) != 0 {
                    lost = true;
                } else if mask & IN_Q_OVERFLOW != 0 {
                    // Events were dropped, so assume the file changed and let the caller resync.
                    changed = true;
                } else if name == self.name.as_slice() {
                    changed = true;
                }
            }
        }

        Ok(if lost {
            Some(Wake::Lost)
        } else if changed {
            Some(Wake::Changed)
        } else {
            None
        })
    }
}

fn read_u32(buf: &[u8], at: usize) -> u32 {
    let mut raw = [0u8; 4];
    raw.copy_from_slice(&buf[at..at + 4]);
    u32::from_ne_bytes(raw)
}

/// Run `on_change` now and after every change to `path`, on a thread of its own.
///
/// The parent directory is created if it does not exist, so the watch can always be armed.
pub(crate) fn watch<F>(path: &'static str, mut on_change: F)
where
    F: FnMut() + Send + 'static,
{
    thread::spawn(move || {
        let mut warned = false;

        loop {
            if let Some(dir) = Path::new(path).parent() {
                let _ = fs::create_dir_all(dir);
            }

            match Watcher::new(path) {
                Ok(watcher) => {
                    warned = false;
                    info!("inotify armed on {path}");
                    // Armed first, read second: a change in between is queued, not lost.
                    on_change();

                    loop {
                        match watcher.wait(RESYNC_INTERVAL) {
                            Ok(Wake::Changed) | Ok(Wake::Timeout) => on_change(),
                            Ok(Wake::Lost) => {
                                warn!("inotify watch on {path} was lost, re-arming");
                                break;
                            }
                            Err(e) => {
                                warn!("inotify wait on {path} failed: {e}, re-arming");
                                break;
                            }
                        }
                    }
                    on_change();
                }
                Err(e) => {
                    if !warned {
                        warned = true;
                        warn!("inotify unavailable for {path}: {e}, polling instead");
                    }
                    on_change();
                    thread::sleep(FALLBACK_POLL);
                    continue;
                }
            }

            thread::sleep(REARM_DELAY);
        }
    });
}
