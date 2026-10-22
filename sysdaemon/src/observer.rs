// ---------------- AMS observer (IProcessObserver) ----------------
//
// The callbacks are oneway calls from system_server, they must return fast:
// they only parse the event and push it into a channel. The worker thread
// updates the state and publishes the files.
use std::sync::mpsc::{self, Sender};
use std::sync::OnceLock;
use std::thread;

use crate::codes::codes;
use crate::log::{debug, error, info, warn};
use crate::ndk::{n, str_alloc, P, UNKNOWN};
use crate::pkg::pkg_of_uid;
use crate::state;

pub(crate) enum Event {
    /// An activity of the app came to the foreground.
    Foreground { pid: i32, uid: i32 },
    /// A new app process was started.
    Started { pid: i32, uid: i32, pkg: String },
    /// An app process died.
    Died { pid: i32, uid: i32 },
}

static TX: OnceLock<Sender<Event>> = OnceLock::new();

fn send(ev: Event) {
    if let Some(tx) = TX.get()
        && tx.send(ev).is_err()
    {
        error!("event channel closed, event dropped");
    }
}

/// Start the worker that processes the observer events.
pub(crate) fn start() {
    let (tx, rx) = mpsc::channel::<Event>();
    if TX.set(tx).is_err() { return; }
    thread::spawn(move || {
        while let Ok(ev) = rx.recv() {
            match ev {
                Event::Foreground { pid, uid } => {
                    if !state::is_app(uid) { continue; }
                    // The package of the foreground process: the process table
                    // knows the package of every pid it has seen, /proc is the
                    // fallback for the pids that were started before the
                    // observer was registered, the uid lookup is the last
                    // resort.
                    let pkg = state::pkg_of_pid(pid)
                        .or_else(|| crate::procs::pkg_of(pid))
                        .or_else(|| pkg_of_uid(uid));
                    match pkg {
                        Some(pkg) => state::set_focused(&pkg, pid, uid, "foreground"),
                        None => debug!("foreground: pid={pid} uid={uid}: package name not found"),
                    }
                }
                Event::Started { pid, uid, pkg } => {
                    if !state::is_app(uid) { continue; }
                    // onProcessStarted usually carries the package name; when it
                    // is null (some processes) the uid lookup is used instead.
                    let pkg = if pkg.is_empty() {
                        match pkg_of_uid(uid) {
                            Some(p) => p,
                            None => continue,
                        }
                    } else {
                        pkg
                    };
                    debug!("process started: pid={pid} uid={uid} pkg={pkg}");
                    state::add_proc(pid, &pkg, uid);
                }
                Event::Died { pid, uid } => {
                    debug!("onProcessDied: pid={pid} uid={uid}");
                    state::remove_proc(pid, "process died");
                }
            }
        }
        info!("event worker stopped");
    });
}

// ---------------- callbacks from AMS (oneway, must be fast) ----------------
pub(crate) unsafe extern "C" fn on_observer(_b: P, code: u32, inp: P, _o: P) -> i32 {
    let (n, c) = (n(), codes());
    let (mut pid, mut uid) = (-1i32, -1i32);
    unsafe {
        if code == c.fg_act {
            let mut fg = false;
            (n.AParcel_readInt32)(inp, &mut pid);
            (n.AParcel_readInt32)(inp, &mut uid);
            (n.AParcel_readBool)(inp, &mut fg);
            if fg {
                send(Event::Foreground { pid, uid });
            }
            return 0;
        }
        if code == c.proc_started {
            (n.AParcel_readInt32)(inp, &mut pid);
            (n.AParcel_readInt32)(inp, &mut uid);
            let mut buf: Vec<u8> = Vec::new();
            let r = (n.AParcel_readString)(inp, &mut buf as *mut Vec<u8> as P, str_alloc);
            let pkg = if r == 0 {
                let end = buf.iter().position(|&b| b == 0).unwrap_or(buf.len());
                String::from_utf8_lossy(&buf[..end]).into_owned()
            } else {
                warn!("readString onProcessStarted failed, status={r}");
                String::new()
            };
            // The process name can carry a suffix ("com.foo:push"): only the
            // base package identifies the app.
            let pkg = crate::procs::base_pkg(&pkg).unwrap_or_default();
            debug!("onProcessStarted: pid={pid} uid={uid} pkg=[{pkg}]");
            // An empty package name is resolved from the uid by the worker.
            if !pkg.is_empty() || crate::state::is_app(uid) { send(Event::Started { pid, uid, pkg }); }
            return 0;
        }
        if code == c.died {
            (n.AParcel_readInt32)(inp, &mut pid);
            (n.AParcel_readInt32)(inp, &mut uid);
            send(Event::Died { pid, uid });
            return 0;
        }
        if code == c.fg_svc { return 0; }
        warn!("unknown transaction from the observer: code={code}");
    }
    UNKNOWN
}

pub(crate) unsafe extern "C" fn on_am_died(_: P) {
    error!("ActivityManager died (system_server restart), exiting so we get restarted");
    std::process::exit(1);
}
