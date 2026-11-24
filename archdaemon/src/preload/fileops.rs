//! Per-file OS operations: `open` with `O_NOATIME`, `RLIMIT_NOFILE` bump,
//! `BLKGETSIZE64`, `mmap`, `mincore`, `posix_fadvise`, and `Mmap`/`Pread` touch.

use std::ffi::{CStr, CString};
use std::io;
use std::os::fd::{AsRawFd, FromRawFd, OwnedFd};
use std::os::unix::ffi::OsStrExt;
use std::path::Path;
use std::time::Instant;

use super::pages::bytes_to_pages;
use super::stats::{ActionMode, PreloadConfig, PreloadObserver, Stats, TouchStrategy};

const CHART_UPDATE_INTERVAL_SECS: f64 = 0.1;
const MAX_NOFILE_RETRIES: usize = 8;
const PREAD_CHUNK_BYTES: usize = 64 * 1024;
const BLKGETSIZE64_REQ: u64 = 0x8008_1272;

/// Raise `RLIMIT_NOFILE` safely, guarding against `RLIM_INFINITY`.
pub fn increment_nofile_rlimit() -> Result<(), String> {
    let mut r = libc::rlimit {
        rlim_cur: 0,
        rlim_max: 0,
    };
    if unsafe { libc::getrlimit(libc::RLIMIT_NOFILE, &mut r) } != 0 {
        return Err(format!("getrlimit: {}", io::Error::last_os_error()));
    }

    if r.rlim_cur == libc::RLIM_INFINITY {
        return Err("open file limit reached".to_string());
    }

    if r.rlim_cur < r.rlim_max {
        r.rlim_cur = r.rlim_max;
    } else if r.rlim_max != libc::RLIM_INFINITY {
        let next = r.rlim_max.saturating_add(64);
        r.rlim_cur = next;
        r.rlim_max = next;
    } else {
        return Err("open file limit reached".to_string());
    }

    if unsafe { libc::setrlimit(libc::RLIMIT_NOFILE, &r) } != 0 {
        let err = io::Error::last_os_error();
        if err.raw_os_error() == Some(libc::EPERM) {
            let is_root = unsafe { libc::getuid() == 0 || libc::geteuid() == 0 };
            if is_root {
                return Err("system open file limit reached".to_string());
            }
            return Err("open file limit reached, retry as root".to_string());
        }
        return Err(format!("setrlimit: {err}"));
    }

    Ok(())
}

/// Check whether `mincore(2)` is expected to report accurate residency on Linux >= 5.2.
pub fn can_do_mincore(st: &libc::stat) -> bool {
    let mut uts: libc::utsname = unsafe { std::mem::zeroed() };
    if unsafe { libc::uname(&mut uts) } == 0 {
        let rel = unsafe { CStr::from_ptr(uts.release.as_ptr()) }.to_bytes();
        let mut ver = [0_u64; 2];
        let mut idx = 0_usize;
        let mut i = 0_usize;
        while i < rel.len() && idx < 2 {
            if rel[i].is_ascii_digit() {
                let mut v = 0_u64;
                while i < rel.len() && rel[i].is_ascii_digit() {
                    v = v.saturating_mul(10).saturating_add((rel[i] - b'0') as u64);
                    i += 1;
                }
                ver[idx] = v;
                idx += 1;
            } else {
                i += 1;
            }
        }
        if ver[0] < 5 || (ver[0] == 5 && ver[1] < 2) {
            return true;
        }
    }

    let uid = unsafe { libc::geteuid() };
    let gid = unsafe { libc::getegid() };
    st.st_uid == uid
        || (st.st_gid == gid && (st.st_mode & libc::S_IWGRP) != 0)
        || (st.st_mode & libc::S_IWOTH) != 0
        || uid == 0
}

fn open_with_retry(c_path: &CStr) -> Result<Result<OwnedFd, io::Error>, String> {
    let mut flags = libc::O_RDONLY | libc::O_NOATIME;
    let mut retries = 0_usize;

    loop {
        let mut fd = unsafe { libc::open(c_path.as_ptr(), flags, 0) };
        if fd == -1 && io::Error::last_os_error().raw_os_error() == Some(libc::EPERM) {
            flags &= !libc::O_NOATIME;
            fd = unsafe { libc::open(c_path.as_ptr(), flags, 0) };
        }

        if fd >= 0 {
            return Ok(Ok(unsafe { OwnedFd::from_raw_fd(fd) }));
        }

        let err = io::Error::last_os_error();
        let raw = err.raw_os_error();
        if (raw == Some(libc::EMFILE) || raw == Some(libc::ENFILE)) && retries < MAX_NOFILE_RETRIES
        {
            increment_nofile_rlimit()?;
            retries += 1;
            continue;
        }
        return Ok(Err(err));
    }
}

fn os_err_str(err: &io::Error) -> String {
    match err.raw_os_error() {
        Some(code) => {
            let ptr = unsafe { libc::strerror(code) };
            if ptr.is_null() {
                err.to_string()
            } else {
                unsafe { CStr::from_ptr(ptr) }
                    .to_string_lossy()
                    .into_owned()
            }
        }
        None => err.to_string(),
    }
}

/// Process a single regular file or block device according to `cfg`.
///
/// Returns `Ok(())` on normal completion or skippable per-file warnings, and
/// `Err(msg)` only on fatal errors (matching `vmtouch_file` in `main.c:377-512`).
pub fn process_file(
    path: &Path,
    cfg: &PreloadConfig,
    stats: &mut Stats,
    obs: &mut dyn PreloadObserver,
) -> Result<(), String> {
    let path_disp = path.to_string_lossy();
    let Ok(c_path) = CString::new(path.as_os_str().as_bytes()) else {
        obs.warning(&format!(
            "unable to open {path_disp} (invalid path), skipping"
        ));
        return Ok(());
    };

    let fd = match open_with_retry(&c_path)? {
        Ok(fd) => fd,
        Err(err) => {
            obs.warning(&format!(
                "unable to open {path_disp} ({}), skipping",
                os_err_str(&err)
            ));
            return Ok(());
        }
    };

    let mut sb: libc::stat = unsafe { std::mem::zeroed() };
    if unsafe { libc::fstat(fd.as_raw_fd(), &mut sb) } != 0 {
        let err = io::Error::last_os_error();
        obs.warning(&format!(
            "unable to fstat {path_disp} ({}), skipping",
            os_err_str(&err)
        ));
        return Ok(());
    }

    let len_of_file: i64 = if (sb.st_mode & libc::S_IFMT) == libc::S_IFBLK {
        let mut blk_size: u64 = 0;
        let rc = unsafe {
            libc::ioctl(
                fd.as_raw_fd(),
                BLKGETSIZE64_REQ as _,
                &mut blk_size as *mut u64,
            )
        };
        if rc != 0 {
            let err = io::Error::last_os_error();
            obs.warning(&format!(
                "unable to ioctl {path_disp} ({}), skipping",
                os_err_str(&err)
            ));
            return Ok(());
        }
        i64::try_from(blk_size).unwrap_or(i64::MAX)
    } else {
        sb.st_size as i64
    };

    if len_of_file <= 0 || (len_of_file as u64) > cfg.max_file_size {
        if len_of_file > 0 && (len_of_file as u64) > cfg.max_file_size {
            obs.warning(&format!("file {path_disp} too large, skipping"));
        }
        return Ok(());
    }

    if cfg.offset >= len_of_file {
        obs.warning(&format!("file {path_disp} smaller than offset, skipping"));
        return Ok(());
    }

    let len_of_range = if cfg.max_len > 0 {
        match cfg.offset.checked_add(cfg.max_len) {
            Some(end) if end < len_of_file => cfg.max_len,
            _ => len_of_file - cfg.offset,
        }
    } else {
        len_of_file - cfg.offset
    };

    let pages_in_range = bytes_to_pages(len_of_range, cfg.page_size)?;
    stats.total_pages = stats.total_pages.saturating_add(pages_in_range);

    if cfg.action == ActionMode::Evict {
        if cfg.verbose > 0 {
            obs.on_evict_file(path);
        }
        let rc = unsafe {
            libc::posix_fadvise(
                fd.as_raw_fd(),
                cfg.offset as libc::off_t,
                len_of_range as libc::off_t,
                libc::POSIX_FADV_DONTNEED,
            )
        };
        if rc != 0 {
            let err = io::Error::from_raw_os_error(rc);
            obs.warning(&format!(
                "unable to posix_fadvise file {path_disp} ({})",
                os_err_str(&err)
            ));
        }
        return Ok(());
    }

    if cfg.action == ActionMode::Touch && cfg.touch_strategy == TouchStrategy::Pread {
        return touch_via_pread(path, &fd, cfg, len_of_range, pages_in_range, obs);
    }

    // Mmap path (Query, or Touch with TouchStrategy::Mmap).
    // Note: Mmap touch matches C vmtouch parity; concurrent file truncation can raise SIGBUS.
    let map_len = usize::try_from(len_of_range).map_err(|_| {
        format!("unable to mmap file {path_disp} (range too large for address space)")
    })?;

    let mem = unsafe {
        libc::mmap(
            std::ptr::null_mut(),
            map_len,
            libc::PROT_READ,
            libc::MAP_SHARED,
            fd.as_raw_fd(),
            cfg.offset as libc::off_t,
        )
    };
    if mem == libc::MAP_FAILED {
        let err = io::Error::last_os_error();
        obs.warning(&format!(
            "unable to mmap file {path_disp} ({}), skipping",
            os_err_str(&err)
        ));
        // Roll back page count? C actually keeps total_pages only AFTER mmap succeeds (main.c:439-447).
        stats.total_pages = stats.total_pages.saturating_sub(pages_in_range);
        return Ok(());
    }

    let pages_usize = usize::try_from(pages_in_range)
        .map_err(|_| "Failed to allocate memory for mincore array".to_string())?;
    let mut mincore_array = vec![0_u8; pages_usize];

    if unsafe { libc::mincore(mem, map_len, mincore_array.as_mut_ptr() as *mut _) } != 0 {
        let err = io::Error::last_os_error();
        unsafe {
            libc::munmap(mem, map_len);
        }
        return Err(format!("mincore {path_disp} ({})", os_err_str(&err)));
    }

    for &p in &mincore_array {
        if (p & 0x1) != 0 {
            stats.total_pages_in_core = stats.total_pages_in_core.saturating_add(1);
        }
    }

    let mut last_chart = Instant::now();
    if cfg.verbose > 0 {
        obs.on_file_start(path);
        if !can_do_mincore(&sb) {
            obs.warning("No write permission, residency chart may be inaccurate");
        }
        obs.on_chart_update(&mincore_array);
    }

    if cfg.action == ActionMode::Touch {
        let base = mem as *const u8;
        let page_stride = cfg.page_size as usize;
        let mut junk: u32 = 0;

        for i in 0..mincore_array.len() {
            let byte = unsafe { std::ptr::read_volatile(base.add(i * page_stride)) };
            junk = junk.wrapping_add(u32::from(byte));
            mincore_array[i] = 1;

            if cfg.verbose > 0 && last_chart.elapsed().as_secs_f64() > CHART_UPDATE_INTERVAL_SECS {
                last_chart = Instant::now();
                obs.on_chart_update(&mincore_array);
            }
        }
        std::hint::black_box(junk);
    }

    if cfg.verbose > 0 || cfg.action == ActionMode::Touch {
        obs.on_file_done(path, &mincore_array);
    }

    if cfg.lock && unsafe { libc::mlock(mem, map_len) } != 0 {
        let err = io::Error::last_os_error();
        return Err(format!("mlock: {path_disp} ({})", os_err_str(&err)));
    }

    if !cfg.lock && !cfg.lockall && unsafe { libc::munmap(mem, map_len) } != 0 {
        let err = io::Error::last_os_error();
        obs.warning(&format!(
            "unable to munmap file {path_disp} ({})",
            os_err_str(&err)
        ));
    }

    Ok(())
}

/// SIGBUS-safe page touching via `pread(2)` in 64 KiB chunks.
fn touch_via_pread(
    path: &Path,
    fd: &OwnedFd,
    cfg: &PreloadConfig,
    len_of_range: i64,
    pages_in_range: i64,
    obs: &mut dyn PreloadObserver,
) -> Result<(), String> {
    let mut buf = [0_u8; PREAD_CHUNK_BYTES];
    let mut remaining = len_of_range;
    let mut off = cfg.offset;

    if cfg.verbose > 0 {
        obs.on_file_start(path);
    }

    while remaining > 0 {
        let to_read = (remaining as usize).min(buf.len());
        let n = unsafe {
            libc::pread(
                fd.as_raw_fd(),
                buf.as_mut_ptr() as *mut libc::c_void,
                to_read,
                off as libc::off_t,
            )
        };
        if n < 0 {
            let err = io::Error::last_os_error();
            if err.raw_os_error() == Some(libc::EINTR) {
                continue;
            }
            obs.warning(&format!(
                "unable to pread file {} ({}), skipping",
                path.to_string_lossy(),
                os_err_str(&err)
            ));
            break;
        }
        if n == 0 {
            break;
        }
        remaining -= n as i64;
        off += n as i64;
    }

    let pages_usize = usize::try_from(pages_in_range).unwrap_or(0);
    let resident = vec![1_u8; pages_usize];
    obs.on_file_done(path, &resident);
    Ok(())
}
