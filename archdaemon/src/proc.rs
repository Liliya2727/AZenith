//! Process utilities, mirroring `jni/src/proc/`.
use crate::sysprop::set;
use std::io::BufRead;


// ---------------------------------------------------------------------------
// get_pids_of
// ---------------------------------------------------------------------------
pub fn get_pids_of(name: &str, pids: &mut [i32], max_pids: i32) -> i32 {
    get_pids_of_at(name, "/data/adb/.config/AZenith/background_apps", pids, max_pids)
}

pub fn get_pids_of_at(name: &str, path: &str, pids: &mut [i32], max_pids: i32) -> i32 {
    if name.is_empty() || pids.is_empty() || max_pids < 1 {
        return 0;
    }

    let Ok(fp) = std::fs::File::open(path) else {
        return 0;
    };

    let mut count = 0i32;
    for line in std::io::BufReader::new(fp).lines().flatten() {
        if count >= max_pids {
            break;
        }
        let mut parts = line.split_whitespace();
        let pkg = parts.next().unwrap_or("");
        let pid = parts.next().and_then(|s| s.parse::<i32>().ok()).unwrap_or(-1);
        let _uid = parts.next().and_then(|s| s.parse::<i32>().ok()).unwrap_or(-1);

        if pkg == name && pid > 0 {
            pids[count as usize] = pid;
            count += 1;
        }
    }

    count
}

// ---------------------------------------------------------------------------
// uidof
// ---------------------------------------------------------------------------
pub fn uidof(pid: i32) -> i32 {
    if pid <= 0 {
        return -1;
    }

    let Ok(fp) = std::fs::File::open("/data/adb/.config/AZenith/background_apps") else {
        return -1;
    };

    for line in std::io::BufReader::new(fp).lines().flatten() {
        let mut parts = line.split_whitespace();
        let _pkg = parts.next().unwrap_or("");
        let current_pid = parts.next().and_then(|s| s.parse::<i32>().ok()).unwrap_or(-1);
        let current_uid = parts.next().and_then(|s| s.parse::<i32>().ok()).unwrap_or(-1);

        if current_pid == pid {
            return current_uid;
        }
    }

    -1
}

// ---------------------------------------------------------------------------
// setspid
// ---------------------------------------------------------------------------
pub fn setspid() {
    let pid = std::process::id();
    let _ = set("persist.sys.azenith.service", &pid.to_string());
}

// ---------------------------------------------------------------------------
// set_priority
// ---------------------------------------------------------------------------
pub fn set_priority(pid: i32) {
    unsafe {
        if libc::setpriority(libc::PRIO_PROCESS, pid as u32, -20) == -1 {
            crate::log_error!("priority", "setpriority failed pid={} errno={}", pid, std::io::Error::last_os_error());
        }
        if libc::syscall(libc::SYS_ioprio_set as i64, 1i64, pid as i64, (1 << 13) | 0) == -1 {
            crate::log_error!("priority", "ioprio_set failed pid={} errno={}", pid, std::io::Error::last_os_error());
        }
    }
}
