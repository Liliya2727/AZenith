//! Utilities.

use crate::sysprop::{get_or_default, set};
use crate::core::MODULE_PROP;
use std::io::BufRead;
use crate::{log_error, log_fatal, log_info, log_warn};
use crate::shell::systemv;

use std::ffi::CString;
use std::os::raw::c_int;

// ---------------------------------------------------------------------------
// trim_newline
// ---------------------------------------------------------------------------
pub fn trim_newline(string: &mut String) {
    if string.ends_with('\n') {
        string.pop();
    }
    if string.ends_with('\r') {
        string.pop();
    }
}

// ---------------------------------------------------------------------------
// escape_shell_string
// ---------------------------------------------------------------------------
pub fn escape_shell_string(src: &str) -> String {
    let mut dest = String::with_capacity(src.len() + 8);
    for ch in src.chars() {
        if ch == '\'' {
            dest.push_str("'\\''");
        } else {
            dest.push(ch);
        }
    }
    dest
}

// ---------------------------------------------------------------------------
// timern
// ---------------------------------------------------------------------------
pub fn timern() -> String {
    use std::time::{SystemTime, UNIX_EPOCH};
    let now = SystemTime::now();
    let duration = now.duration_since(UNIX_EPOCH).unwrap_or_default();
    let secs = duration.as_secs() as i64;

    // Use localtime via libc
    unsafe {
        let mut tm = std::mem::zeroed::<libc::tm>();
        if libc::localtime_r(&secs, &mut tm).is_null() {
            return String::from("[TimeError]");
        }

        let mut buf = [0u8; 32];
        let fmt = CString::new("%Y-%m-%d %H:%M:%S").unwrap();
        let written = libc::strftime(
            buf.as_mut_ptr() ,
            buf.len(),
            fmt.as_ptr(),
            &mut tm as *mut libc::tm,
        );
        if written == 0 {
            return String::from("[TimeFormatError]");
        }

        let base = std::ffi::CStr::from_ptr(buf.as_ptr() )
            .to_string_lossy()
            .into_owned();
        let millis = duration.subsec_millis();
        format!("{base}.{millis:03}")
    }
}

// ---------------------------------------------------------------------------
// sighandler
// ---------------------------------------------------------------------------
// Async-signal-safe: only sets a flag. The main loop observes it each
// iteration and performs the shutdown work (e.g. recorder finalize) there —
// the previous _exit() made finalize-on-SIGTERM impossible.
pub static EXIT_SIGNAL: std::sync::atomic::AtomicBool = std::sync::atomic::AtomicBool::new(false);

pub fn exit_signal() -> bool {
    EXIT_SIGNAL.load(std::sync::atomic::Ordering::SeqCst)
}

pub extern "C" fn sighandler(_signal: c_int) {
    EXIT_SIGNAL.store(true, std::sync::atomic::Ordering::SeqCst);
}

// ---------------------------------------------------------------------------
// checkstate
// ---------------------------------------------------------------------------
pub fn checkstate() {
    let state = get_or_default("persist.sys.azenith.state", "");

    if state.is_empty() || state == "stopped" {
        log_fatal!("daemon", "service killed reason=state_empty_or_stopped");
        set("persist.sys.azenith.service", "");
        set("persist.sys.azenith.state", "stopped");
        std::process::exit(1);
    }
}

// ---------------------------------------------------------------------------
// notify
// ---------------------------------------------------------------------------
pub fn notify(title: &str, fmt: &str, chrono: bool, timeout_ms: i32, args: Option<&[&str]>) {
    let message = if let Some(a) = args {
        if a.is_empty() {
            fmt.to_string()
        } else {
            a.join(" ")
        }
    } else {
        fmt.to_string()
    };

    let safe_title = escape_shell_string(title);
    let safe_message = escape_shell_string(&message);

    let action = "zx.azenith.ACTION_MANAGE";
    let component = "zx.azenith/zx.azenith.receiver.ZenithReceiver";
    let chrono_str = if chrono { "true" } else { "false" };

    let status = if timeout_ms > 0 {
        systemv(&format!(
            "su -c \"am broadcast -a {action} -n {component} --es notifytitle '{safe_title}' --es notifytext '{safe_message}' --ez chrono_bool {chrono_str} --es timeout '{timeout_ms}' >/dev/null 2>&1\""
        ))
    } else {
        systemv(&format!(
            "su -c \"am broadcast -a {action} -n {component} --es notifytitle '{safe_title}' --es notifytext '{safe_message}' --ez chrono_bool {chrono_str} >/dev/null 2>&1\""
        ))
    };
    if status != 0 {
        log_warn!("notify", "broadcast failed exit={status}");
    }
}

// ---------------------------------------------------------------------------
// toast
// ---------------------------------------------------------------------------
pub fn toast(message: &str) {
    let val = get_or_default("persist.sys.azenithconf.showtoast", "");
    if val == "1" {
        let safe_message = escape_shell_string(message);
        let exit = systemv(&format!(
            "su -c \"am broadcast -a zx.azenith.ACTION_MANAGE -n zx.azenith/.receiver.ZenithReceiver --es toasttext '{safe_message}' >/dev/null 2>&1\""
        ));
        if exit != 0 {
            log_warn!("notify", "toast broadcast failed message={message}");
        }
    }
}

// ---------------------------------------------------------------------------
// restart_target_app
// ---------------------------------------------------------------------------
pub fn restart_target_app(pkg: &str) {
    log_info!("daemon", "restarting app pkg={} reason=pending_changes", pkg);
    let safe_pkg = escape_shell_string(pkg);
    systemv(&format!(
        "am force-stop '{safe_pkg}' && am start -n \"$(cmd package resolve-activity --brief '{safe_pkg}' | tail -n 1)\""
    ));
    std::thread::sleep(std::time::Duration::from_millis(700));
}

// ---------------------------------------------------------------------------
// update_module_description
// ---------------------------------------------------------------------------
pub fn update_module_description(pid: i32) {
    let fp = match std::fs::File::open(MODULE_PROP) {
        Ok(f) => f,
        Err(e) => {
             log_error!("daemon", "module.prop open failed path={} errno={}", MODULE_PROP, e.raw_os_error().unwrap_or(-1));
            return;
        }
    };

    let lines: Vec<String> = std::io::BufReader::new(fp)
        .lines()
        .filter_map(|l| l.ok())
        .take(128)
        .collect();

    let new_desc = format!(
        "description=[\u{2705}AZenith is Alive with PID : {}] One, Two, Three! AZenith has arrived!\n",
        pid
    );

    let mut replaced = false;
    let mut out = String::new();
    for line in &lines {
        if line.starts_with("description=") {
            out.push_str(&new_desc);
            replaced = true;
        } else {
            out.push_str(line);
            out.push('\n');
        }
    }
    if !replaced {
        out.push_str(&new_desc);
    }

    if let Err(e) = std::fs::write(MODULE_PROP, &out) {
         log_error!("daemon", "module.prop write failed path={} errno={}", MODULE_PROP, e.raw_os_error().unwrap_or(-1));
    }
}

// ---------------------------------------------------------------------------
// runthermalcore
// ---------------------------------------------------------------------------
pub fn runthermalcore() {
    let thermalcore = get_or_default("persist.sys.azenithconf.thermalcore", "");
    if thermalcore == "1" {
        systemv("sys.azenith-rianixiathermalcore &");
        let output = crate::shell::execute_command("pidof sys.azenith-rianixiathermalcore");

        if let Some(pid_str) = output {
            // pidof can return multiple PIDs separated by space; take the first like C's atoi
            if let Some(first) = pid_str.split_whitespace().next() {
                if let Ok(pid) = first.parse::<i32>() {
                    log_info!("thermal", "thermalcore started pid={pid}");
                } else {
                    log_warn!("thermal", "thermalcore started but pid not found");
                }
            } else {
                log_warn!("thermal", "thermalcore started but pid not found");
            }
        } else {
            log_warn!("thermal", "thermalcore spawn failed resource=pidof");
        }
    }
}

// ---------------------------------------------------------------------------
// extract_string_value
// ---------------------------------------------------------------------------
pub fn extract_string_value(dest: &mut [u8], key_pos: &str, max_len: usize) {
    let dest_len = dest.len().min(max_len);
    let colon = match key_pos.find(':') {
        Some(c) => c,
        None => {
            let def = b"default";
            let n = def.len().min(dest_len);
            dest[..n].copy_from_slice(&def[..n]);
            return;
        }
    };

    let mut start = &key_pos[colon + 1..];
    start = start.trim_start_matches(|c: char| c == ' ' || c == '\t');

    let start = if start.starts_with('"') { &start[1..] } else { start };

    let end = match start.find('"') {
        Some(e) => e,
        None => {
            let def = b"default";
            let n = def.len().min(dest_len);
            dest[..n].copy_from_slice(&def[..n]);
            return;
        }
    };

    let len = end.min(dest_len - 1);
    dest[..len].copy_from_slice(&start.as_bytes()[..len]);
    dest[len] = 0;
}

// ---------------------------------------------------------------------------
// log_fatal helper (used by checkstate)
// ---------------------------------------------------------------------------
pub fn log_fatal(component: &str, message: &str, args: std::fmt::Arguments<'_>) {
    crate::log::log_fatal(component, message, args);
}
