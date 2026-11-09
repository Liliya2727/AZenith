//! Shell execution.

use crate::core::MY_PATH;
use crate::log_error;

use std::os::raw::{c_char, c_int};
use std::os::unix::fs::PermissionsExt;

// ---------------------------------------------------------------------------
// systemv — fork + execle("/system/bin/sh", "sh", "-c", command, NULL, env)
// ---------------------------------------------------------------------------
pub fn systemv(command: &str) -> c_int {
    unsafe {
        let c_command = match std::ffi::CString::new(command) {
            Ok(c) => c,
            Err(_) => return -1,
        };
        let c_command_ptr = c_command.as_ptr();

        let pid = libc::fork();
        if pid == -1 {
            log_error!("shell", "fork failed errno={}", std::io::Error::last_os_error());
            return -1;
        }

        if pid == 0 {
            let env = std::ffi::CString::new(MY_PATH).unwrap();
            let envp = [env.as_ptr(), std::ptr::null()];
            // execle: path, arg0, arg1, ..., argn, NULL, envp
            libc::execle(
                b"/system/bin/sh\0".as_ptr() as *const c_char,
                b"sh\0".as_ptr() as *const c_char,
                b"-c\0".as_ptr() as *const c_char,
                c_command_ptr,
                std::ptr::null::<c_char>(),
                envp.as_ptr(),
            );
            libc::_exit(127);
        }

        let mut status: c_int = 0;
        if libc::waitpid(pid, &mut status, 0) == -1 {
            return -1;
        }

        if libc::WIFEXITED(status) {
            libc::WEXITSTATUS(status)
        } else {
            -1
        }
    }
}

// ---------------------------------------------------------------------------
// execute_command — capture shell command output
// ---------------------------------------------------------------------------
pub fn execute_command(command: &str) -> Option<String> {
    systemv_capture(command)
}

// ---------------------------------------------------------------------------
// execute_direct — execute binary directly, capture stdout
// ---------------------------------------------------------------------------
pub fn execute_direct(path: &str, arg0: &str, args: &[&str]) -> Option<String> {
    let mut argv: Vec<std::ffi::CString> = vec![
        std::ffi::CString::new(arg0).unwrap(),
    ];
    for a in args {
        argv.push(std::ffi::CString::new(*a).unwrap());
    }
    argv.push(std::ffi::CString::new("").unwrap());

    let mut c_ptrs: Vec<*const c_char> = argv.iter().map(|a| a.as_ptr()).collect();
    c_ptrs.push(std::ptr::null());

    let (pipe_r, pipe_w) = match create_pipe() {
        Some(p) => p,
        None => return None,
    };

    let pid = unsafe { libc::fork() };
    if pid == -1 {
        log_error!("shell", "execute_direct fork failed errno={}", std::io::Error::last_os_error());
        unsafe { libc::close(pipe_r); libc::close(pipe_w); }
        return None;
    }

    if pid == 0 {
        unsafe {
            libc::dup2(pipe_w, 1);
            libc::close(pipe_r);
            libc::close(pipe_w);
            let c_path = match std::ffi::CString::new(path) {
                Ok(path) => path,
                Err(_) => std::process::exit(127),
            };
            libc::execv(
                c_path.as_ptr(),
                c_ptrs.as_ptr() as *const *const c_char,
            );
        }
        unsafe { libc::_exit(127) };
    }

    unsafe { libc::close(pipe_w); }

    let mut output = Vec::new();
    let mut buf = [0u8; 4096];
    loop {
        match unsafe { libc::read(pipe_r, buf.as_mut_ptr() as *mut _, buf.len()) } {
            -1 => break,
            0 => break,
            n => output.extend_from_slice(&buf[..n as usize]),
        }
    }
    unsafe { libc::close(pipe_r); }

    let mut status: c_int = 0;
    unsafe { libc::waitpid(pid, &mut status, 0); }

    if libc::WEXITSTATUS(status) != 0 {
        return None;
    }

    String::from_utf8(output).ok().map(|s| {
        let s = s.trim_end_matches('\n');
        s.to_string()
    })
}

// ---------------------------------------------------------------------------
// chmodg — apply chmod to glob-matched paths
// ---------------------------------------------------------------------------
pub fn chmodg(pattern: &str, mode: u32) {
    for entry in glob::glob(pattern).into_iter().flatten() {
        if let Ok(path) = entry {
            let _ = std::fs::set_permissions(&path, std::fs::Permissions::from_mode(mode));
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
fn systemv_capture(command: &str) -> Option<String> {
    let (pipe_r, pipe_w) = match create_pipe() {
        Some(p) => p,
        None => return None,
    };

    let pid = unsafe { libc::fork() };
    if pid == -1 {
        log_error!("shell", "execute_command fork failed errno={}", std::io::Error::last_os_error());
        unsafe { libc::close(pipe_r); libc::close(pipe_w); }
        return None;
    }

    if pid == 0 {
        unsafe {
            libc::dup2(pipe_w, 1);
            libc::close(pipe_r);
            libc::close(pipe_w);

            let env = std::ffi::CString::new(MY_PATH).unwrap();
            let envp = [env.as_ptr(), std::ptr::null()];
            let c_command = match std::ffi::CString::new(command) {
                Ok(command) => command,
                Err(_) => std::process::exit(127),
            };
            libc::execle(
                b"/system/bin/sh\0".as_ptr() as *const c_char,
                b"sh\0".as_ptr() as *const c_char,
                b"-c\0".as_ptr() as *const c_char,
                c_command.as_ptr(),
                std::ptr::null::<c_char>(),
                envp.as_ptr(),
            );
        }
        unsafe { libc::_exit(127) };
    }

    unsafe { libc::close(pipe_w); }

    let mut output = Vec::new();
    let mut buf = [0u8; 4096];
    loop {
        match unsafe { libc::read(pipe_r, buf.as_mut_ptr() as *mut _, buf.len()) } {
            -1 => break,
            0 => break,
            n => output.extend_from_slice(&buf[..n as usize]),
        }
    }
    unsafe { libc::close(pipe_r); }

    let mut status: c_int = 0;
    unsafe { libc::waitpid(pid, &mut status, 0); }

    if libc::WEXITSTATUS(status) != 0 {
        crate::log_debug!("shell", "execute_command exit_status={} cmd_prefix={:.60}", libc::WEXITSTATUS(status), command);
        return None;
    }

    match String::from_utf8(output) {
        Ok(s) => {
            let s = s.trim_end_matches('\n');
            crate::log_debug!("shell", "execute_command ok out_len={} cmd_prefix={:.60}", s.len(), command);
            Some(s.to_string())
        }
        Err(e) => {
            crate::log_debug!("shell", "execute_command utf8 error={} cmd_prefix={:.60}", e, command);
            None
        }
    }
}

fn create_pipe() -> Option<(i32, i32)> {
    let mut fds: [i32; 2] = [0, 0];
    if unsafe { libc::pipe(fds.as_mut_ptr()) } == -1 {
        return None;
    }
    Some((fds[0], fds[1]))
}
