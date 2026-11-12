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
//
// ---------------- focused task poll ----------------
//
// IProcessObserver only reports a process when its own foreground flag flips.
// A partial home gesture is the case where that is not enough: the gesture
// gives the launcher foreground activities, the app itself never leaves the
// foreground state in AMS, and when the gesture is released no new callback
// for the app arrives. The last event we saw is then the launcher, so
// app_status stays on it until the user switches apps again.
//
// The focused root task is the authority on what is actually on screen, so it
// is polled lightly and used to correct the state when the callbacks missed a
// switch. The reply is a RootTaskInfo (a StackInfo on Android 10-11); the
// topActivity component is written before the base intent and the other task
// fields, so the first package-like string that belongs to a running app is
// the focused app.
use std::sync::OnceLock;
use std::thread;
use std::time::{Duration, Instant};

use crate::codes::{codes, codes_opt};
use crate::log::{debug, info};
use crate::ndk::{call, n, parcel_words, service, P};
use crate::procs;
use crate::state;

const POLL: Duration = Duration::from_millis(1000);
/// /proc is only scanned when the process table does not know a candidate, and
/// at most this often.
const SCAN_EVERY: Duration = Duration::from_secs(5);

static SVC_TASK: OnceLock<usize> = OnceLock::new();

fn focused_task_bytes() -> Option<Vec<u8>> {
    let code = codes_opt()?.focused_task;
    if code == 0 { return None; }
    let b = *SVC_TASK.get()? as P;
    let o = call(b, code, |_| {})?;
    let bytes = parcel_words(o);
    unsafe { (n().AParcel_delete)(o) };
    bytes
}

/// Resolve the activity task service and remember it.
fn init_service() -> bool {
    if SVC_TASK.get().is_some() { return true; }
    let c = codes();
    if c.focused_task == 0 {
        info!("focused task poll disabled (transaction code not found)");
        return false;
    }
    let task = service(c"activity_task", c"android.app.IActivityTaskManager");
    if task.is_null() {
        info!("focused task poll disabled (activity_task service missing)");
        return false;
    }
    let _ = SVC_TASK.set(task as usize);
    true
}

/// Self test (`--focused`): print the focused task package and its live pid.
pub(crate) fn probe() -> Option<(String, i32, i32)> {
    if !init_service() { return None; }
    let bytes = focused_task_bytes()?;
    eprintln!("reply {} bytes", bytes.len());
    eprintln!("candidates: {:?}", crate::recent::packages_in(&bytes));
    let apps = procs::scan_apps();
    crate::recent::packages_in(&bytes)
        .into_iter()
        .find_map(|pkg| apps.iter().find(|a| a.pkg == pkg).map(|a| (pkg, a.pid, a.uid)))
}

pub(crate) fn start() {
    if !init_service() { return; }

    thread::spawn(|| {
        let mut last_scan: Option<Instant> = None;
        loop {
            thread::sleep(POLL);
            // With the screen off the focused task can be the launcher behind
            // the keyguard; the last app the user was in is the useful value
            // (the game profile logic relies on it).
            if !state::screen_awake() { continue; }
            let Some(bytes) = focused_task_bytes() else { continue };
            if bytes.len() < 8 { continue; }

            let known = state::known_packages();
            let mut found = crate::recent::first_known(&bytes, &known)
                .and_then(|pkg| state::proc_of_pkg(&pkg).map(|(pid, uid)| (pkg, pid, uid)));

            if found.is_none() {
                // The app may have started after the observer was registered,
                // so the cached process table does not know it yet. Scan /proc
                // at most every SCAN_EVERY seconds.
                let now = Instant::now();
                if last_scan.is_some_and(|t| now.duration_since(t) < SCAN_EVERY) {
                    continue;
                }
                last_scan = Some(now);
                let apps = procs::scan_apps();
                found = crate::recent::packages_in(&bytes).into_iter().find_map(|pkg| {
                    apps.iter().find(|a| a.pkg == pkg).map(|a| (pkg, a.pid, a.uid))
                });
            }

            let Some((pkg, pid, uid)) = found else { continue };
            let prev = state::focused_pkg();
            if prev.as_deref() == Some(pkg.as_str()) {
                continue;
            }
            debug!("focused task poll: [{}] -> [{pkg}] pid={pid} uid={uid}",
                   prev.unwrap_or_else(|| "none".to_string()));
            state::set_focused(&pkg, pid, uid, "focused task poll");
        }
    });
}
