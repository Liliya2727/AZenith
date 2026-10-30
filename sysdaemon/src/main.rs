// AZenith app monitor daemon — pure binder, no shell fork, no profile logic.
//
// This is the native replacement for the Java companion daemon (AppMonitor.kt).
// It only observes and publishes:
//
//   <cfg>/app_status      focused app (package/pid/uid) + screen/battery/zen state
//   <cfg>/background_apps recently used user apps + their live pids
//
//   <cfg>/java.lock       instance lock (also used by the C daemon to detect us)
//   <cfg>/sysmon.log      log, crash information, bounded in size (rotated)
//
// Module layout:
//   ndk       libbinder_ndk via dlopen
//   codes     transaction code resolver (binder_resolver.apk)
//   fps       frame rate for the focused task -> current_fps: eBPF uprobe on libgui frames, a
//             root app_process hosting WindowManager's TaskFpsCallback, or a SurfaceFlinger
//             timestats fallback
//   pkg       uid -> package (IPackageManager) + package -> label
//   label     package -> label from the APK resources (AXML + resources.arsc)
//   procs     /proc scan (process table + initial foreground guess)
//   observer  IProcessObserver callbacks -> event channel -> worker
//   status    screen/battery saver/zen mode/battery level & charging
//   state     the monitor state + atomic publishing of app_status/background_apps
mod codes;
mod fps;
mod label;
mod lock;
mod log;
mod ndk;
mod observer;
mod pkg;
mod procs;
mod recent;
mod state;
mod status;

use std::fs;

use std::ptr::null_mut;
use std::thread;

use crate::codes::{codes, load_codes, set_codes};
use crate::log::{debug, error, info, level_name, log_init, warn};
use crate::ndk::{call, class, n, service};
use crate::observer::{on_am_died, on_observer};
use crate::pkg::{label_of, set_pm};
use crate::state::seed;
use crate::status::init_status;

/// Configuration directory of the module.
pub(crate) const CFG_DIR: &str = "/data/adb/.config/AZenith";
/// Focused app + system status.
pub(crate) const STATUS_FILE: &str = "/data/adb/.config/AZenith/app_status";
/// Recently used user apps and their pids.
pub(crate) const BACKGROUND_FILE: &str = "/data/adb/.config/AZenith/background_apps";
/// Instance lock (java.lock: the same file the Java companion used).
pub(crate) const LOCK_FILE: &str = "/data/adb/.config/AZenith/java.lock";

fn main() {
    if unsafe { libc::getuid() } != 0 {
        eprintln!("run as root");
        std::process::exit(1);
    }
    if std::env::args().any(|a| a == "--clearlogs") {
        log::clear_logs();
        return;
    }
    // Self test: print the app_name that would be written to app_status.
    let args: Vec<String> = std::env::args().collect();
    if let Some(i) = args.iter().position(|a| a == "--label") {
        let Some(pkg) = args.get(i + 1) else {
            eprintln!("usage: {} --label <package>", args[0]);
            std::process::exit(2);
        };
        let t0 = std::time::Instant::now();
        println!("{}", label_of(pkg));
        eprintln!("({} ms)", t0.elapsed().as_millis());
        return;
    }
    // Self test: print the recents of the task manager.
    if args.iter().any(|a| a == "--recents") {
        let Some(c) = load_codes() else {
            eprintln!("load_codes_failed");
            std::process::exit(1);
        };
        set_codes(c);
        let am = service(c"activity", c"android.app.IActivityManager");
        if am.is_null() {
            eprintln!("am_null");
            std::process::exit(1);
        }
        let code = codes().recent_tasks;
        eprintln!("code={code}");
        let known: Vec<String> = procs::scan_apps().iter().map(|a| a.pkg.clone()).collect();
        eprintln!("known={}", known.len());
        match recent::recent_packages(am, code, &known) {
            Some(list) => {
                eprintln!("OK_LIST={}", list.len());
                for p in &list {
                    println!("{p}");
                }
            }
            None => {
                eprintln!("failed {code}");
                std::process::exit(1);
            }
        }
        return;
    }
    let _ = fs::create_dir_all(CFG_DIR);
    let _ = fs::create_dir_all(format!("{CFG_DIR}/debug"));

    // stdin -> /dev/null, stdout/stderr -> sysmon.log. From here on every panic
    // message (and every abort from the C side) ends up in the log file.
    log::redirect_stdio();

    // Single instance: check before forking so the caller gets a real exit code.
    if lock::held_by_other(LOCK_FILE) {
        eprintln!("another instance is already running ({LOCK_FILE})");
        std::process::exit(1);
    }

    // Detach: fork + setsid + chdir("/"), no stdio reopen (already redirected).
    // The parent exits immediately with success.
    if unsafe { libc::daemon(0, 1) } != 0 {
        eprintln!("daemon() failed");
        std::process::exit(1);
    }

    // Take the instance lock for real. POSIX record locks are NOT inherited over
    // fork(), so this has to happen after daemon().
    let _lock = match lock::acquire(LOCK_FILE) {
        Some(l) => l,
        None => std::process::exit(1),
    };

    log_init();
    log::install_panic_hook();
    info!("daemon starting (pid {}, log level {})", std::process::id(), level_name());
    fps::start();
    state::capture_previous();
    ensure_current_modes();

    let Some(c) = load_codes() else {
        error!("failed to resolve the transaction codes (binder_resolver.apk)");
        std::process::exit(1);
    };
    set_codes(c);

    let n = n();
    let am = service(c"activity", c"android.app.IActivityManager");
    if am.is_null() {
        error!("activity service unavailable");
        std::process::exit(1);
    }
    let pm = service(c"package", c"android.content.pm.IPackageManager");
    if pm.is_null() {
        error!("package service unavailable");
        std::process::exit(1);
    }
    set_pm(pm);

    unsafe {
        let rec = (n.AIBinder_DeathRecipient_new)(on_am_died);
        let r = (n.AIBinder_linkToDeath)(am, rec, null_mut());
        if r == 0 { debug!("linkToDeath registered on ActivityManager"); }
        else { warn!("linkToDeath failed, status={r}"); }
    }

    // The event worker has to be running before the observer is registered,
    // otherwise the first events would be dropped.
    observer::start();

    // Keep the recent list in sync with the task manager's recents.
    thread::spawn(move || {
        loop {
            thread::sleep(std::time::Duration::from_secs(2));
            state::cleanup_stale();
            let am = service(c"activity", c"android.app.IActivityManager");
            if am.is_null() {
                continue;
            }
            let known = state::known_packages();
            if let Some(list) = recent::recent_packages(am, codes().recent_tasks, &known) {
                let list = list.into_iter().filter(|p| known.contains(p)).collect();
                state::set_recent(list);
            }
        }
    });

    let obs = unsafe { (n.AIBinder_new)(class(c"android.app.IProcessObserver", on_observer), null_mut()) };
    if obs.is_null() {
        error!("failed to create the observer binder");
        std::process::exit(1);
    }
    let Some(reply) = call(am, codes().reg, |i| unsafe { (n.AParcel_writeStrongBinder)(i, obs); }) else {
        error!("registerProcessObserver denied/failed (wrong code for this ROM?)");
        std::process::exit(1);
    };
    unsafe { (n.AParcel_delete)(reply); }
    info!("IProcessObserver registered with ActivityManager");

    // SIGTERM/SIGINT -> clean shutdown (the kernel releases the lock).
    thread::spawn(|| {
        let Ok(mut signals) = signal_hook::iterator::Signals::new([libc::SIGTERM, libc::SIGINT]) else {
            warn!("cannot install the signal handlers");
            return;
        };
        let Some(sig) = signals.forever().next() else { return };
        info!("received signal {sig}, shutting down");
        std::process::exit(0);
    });

    unsafe { (n.ABinderProcess_startThreadPool)(); }
    init_status(); // display callback + screen/saver/zen/battery
    seed();        // process table + initial app_status/background_apps
    info!("ready, waiting for callbacks from AMS");
    unsafe { (n.ABinderProcess_joinThreadPool)(); }
}

/// The C daemon (sys.azenith-service) skips the whole game detection when
/// API/current_modes is missing or "0" (jni/src/daemon/loop.c:139), so it
/// stays idle forever. The file is written by the manager app, but
/// customize.sh only creates it when persist.sys.azenithconf.AIenabled is
/// still empty - after a re-install the property is set but the file is
/// gone, and nothing recreates it. Mirror the property, the app writes both
/// together.
fn ensure_current_modes() {
    let dir = format!("{CFG_DIR}/API");
    let path = format!("{dir}/current_modes");
    // A non-empty file is managed by the app, do not touch it.
    if fs::read_to_string(&path).is_ok_and(|v| !v.trim().is_empty()) {
        return;
    }
    let _ = fs::create_dir_all(&dir);
    let prop = label::prop_str("persist.sys.azenithconf.AIenabled");
    let state = if prop.is_empty() { "1" } else { prop.as_str() };
    if state::write_atomic(&path, &format!("{state}\n")) {
        info!("created API/current_modes = {state} (the service needs it to detect games)");
    } else {
        warn!("failed to create API/current_modes");
    }
}
