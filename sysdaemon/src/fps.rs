// ---------------- fps client: root app_process hosting WindowManager's TaskFpsCallback ----------------
//
// The presented frame rate has to come from SurfaceFlinger, and the only route in that does not
// need a platform signature is WindowManager.registerTaskFpsCallback. That call is gated on
// ACCESS_FPS_COUNTER (signature|privileged), which AMS hands to uid 0 for free, so the client has
// to run as root - hence an app_process this daemon spawns rather than code in the manager APK.
// Everything past the permission check is the framework's job: it resolves the focused task id,
// the SurfaceFlinger transaction code and the FrameTimeline arithmetic, and pushes the result at
// roughly 2 Hz into <cfg>/API/current_fps.

use std::fs;
use std::os::unix::fs::PermissionsExt;
use std::process::Command;
use std::thread;
use std::time::Duration;

use crate::codes::RESOLVER_APK;
use crate::log::{error, info, warn};

/// Staged copy of the dex the client runs from. The resolver's own copy under /data/local/tmp is
/// deleted after every transaction-code refresh, so this one has to be separate.
const FPS_CLIENT_APK: &str = "/data/adb/.config/AZenith/fpsclient.apk";
/// "<fps> <epoch_ms>", read by the manager's live monitor.
pub(crate) const FPS_FILE: &str = "/data/adb/.config/AZenith/API/current_fps";
/// stdout/stderr of the client, so its diagnostics do not vanish with the daemon's stdio.
const FPS_LOG: &str = "/data/adb/.config/AZenith/fpsclient.log";

/// Keep one app_process alive. Runs on its own thread: a dead client must never take the daemon
/// down with it, and the daemon must not block waiting for it.
pub(crate) fn start() {
    thread::spawn(|| loop {
        if !stage_apk() {
            return;
        }
        info!("fps client starting, publishing to {FPS_FILE}");
        match client().spawn() {
            Ok(mut child) => match child.wait() {
                Ok(status) => warn!("fps client exited ({status}), restarting in 5 s"),
                Err(e) => warn!("fps client wait failed ({e}), restarting in 5 s"),
            },
            Err(e) => error!("cannot spawn the fps client: {e}"),
        }
        thread::sleep(Duration::from_secs(5));
    });
}

fn client() -> Command {
    let mut c = Command::new("/system/bin/app_process");
    c.env_remove("LD_LIBRARY_PATH").env_remove("LD_PRELOAD");
    c.env("ANDROID_ROOT", "/system").env("ANDROID_DATA", "/data");
    c.env("ANDROID_RUNTIME_ROOT", "/apex/com.android.runtime").env("ANDROID_ART_ROOT", "/apex/com.android.art");
    c.env("ANDROID_I18N_ROOT", "/apex/com.android.i18n").env("ANDROID_TZDATA_ROOT", "/apex/com.android.tzdata");
    c.arg(format!("-Djava.class.path={FPS_CLIENT_APK}"))
        .arg("/")
        .arg("--nice-name=sys.azenith-fpsclient")
        .arg("zx.binder.FpsClient")
        .arg("--fps")
        .arg(format!("--out={FPS_FILE}"));
    if let Ok(f) = fs::File::create(FPS_LOG) {
        if let Ok(g) = f.try_clone() {
            c.stdout(g);
        }
        c.stderr(f);
    }
    c
}

fn stage_apk() -> bool {
    if fs::read(FPS_CLIENT_APK).is_ok_and(|v| v == RESOLVER_APK) {
        return true;
    }
    match fs::write(FPS_CLIENT_APK, RESOLVER_APK) {
        Ok(()) => {
            let _ = fs::set_permissions(FPS_CLIENT_APK, fs::Permissions::from_mode(0o644));
            true
        }
        Err(e) => {
            error!("failed to stage {FPS_CLIENT_APK}: {e}");
            false
        }
    }
}
