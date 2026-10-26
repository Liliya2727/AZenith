// ---------------- transaction codes (cache + resolver embedded) ----------------
use std::fs;
use std::io::Write;
use std::os::unix::fs::PermissionsExt;
use std::process::{Command, Stdio};
use std::sync::OnceLock;

use crate::log::{debug, error, info, warn};

// The resolver APK is only needed to look up the transaction codes of the
// running ROM. It is written to a temporary path and removed afterwards.
const APK_PATH: &str = "/data/local/tmp/zenith_resolver.apk";
pub(crate) const RESOLVER_APK: &[u8] = include_bytes!("../binder_resolver.apk");

pub(crate) struct Codes {
    pub(crate) reg: u32, pub(crate) fg_act: u32, pub(crate) fg_svc: u32,
    pub(crate) died: u32, pub(crate) pm_name: u32,
    pub(crate) is_interactive: u32, pub(crate) is_saver: u32, pub(crate) get_zen: u32,
    pub(crate) disp_reg: u32, pub(crate) bat_prop: u32,
    /// optional
    pub(crate) proc_started: u32, pub(crate) app_label: u32,
    /// IActivityManager.getRecentTasks (the recents of the task manager)
    pub(crate) recent_tasks: u32,
}
static CODES: OnceLock<Codes> = OnceLock::new();
pub(crate) fn codes() -> &'static Codes { CODES.get().unwrap() }
/// `None` before [`load_codes`] ran (used by the `--label` self test).
pub(crate) fn codes_opt() -> Option<&'static Codes> { CODES.get() }
pub(crate) fn set_codes(c: Codes) { let _ = CODES.set(c); }

const Q: [&str; 12] = [
    "android.app.IActivityManager.Stub::TRANSACTION_registerProcessObserver",
    "android.app.IProcessObserver.Stub::TRANSACTION_onForegroundActivitiesChanged",
    "android.app.IProcessObserver.Stub::TRANSACTION_onForegroundServicesChanged",
    "android.app.IProcessObserver.Stub::TRANSACTION_onProcessDied",
    "android.content.pm.IPackageManager.Stub::TRANSACTION_getNameForUid",
    "android.os.IPowerManager.Stub::TRANSACTION_isInteractive",
    "android.os.IPowerManager.Stub::TRANSACTION_isPowerSaveMode",
    "android.app.INotificationManager.Stub::TRANSACTION_getZenMode",
    "android.hardware.display.IDisplayManager.Stub::TRANSACTION_registerCallback",
    "android.os.IBatteryPropertiesRegistrar.Stub::TRANSACTION_getProperty",
    "android.app.IProcessObserver.Stub::TRANSACTION_onProcessStarted",
    "android.app.IActivityManager.Stub::TRANSACTION_getRecentTasks",
];

// Removed from the AIDL in Android 16, so it is only queried on older ROMs
// (asking for a field that does not exist makes the resolver abort).
const LABEL_Q: &str = "android.content.pm.IPackageManager.Stub::TRANSACTION_getApplicationLabel";

fn prop(k: &str) -> String {
    Command::new("getprop").arg(k).output().ok()
        .map(|o| String::from_utf8_lossy(&o.stdout).trim().to_string()).unwrap_or_default()
}

// Entry point of the binder_resolver APK. The apk built from ./binder_resolver uses
// zx.binder.MainKt, the older prebuilt (encore) uses com.rem01gaming.binderresolver.MainKt.
const ENTRY_CLASSES: [&str; 2] = ["zx.binder.MainKt", "com.rem01gaming.binderresolver.MainKt"];

fn run_resolver(apk: &str, entry: &str, queries: &[&str]) -> String {
    let Ok(mut ch) = Command::new("/system/bin/app_process")
        .env_remove("LD_LIBRARY_PATH")
        .env_remove("LD_PRELOAD")
        .env("ANDROID_ROOT", "/system").env("ANDROID_DATA", "/data")
        .env("ANDROID_RUNTIME_ROOT", "/apex/com.android.runtime")
        .env("ANDROID_ART_ROOT", "/apex/com.android.art")
        .env("ANDROID_I18N_ROOT", "/apex/com.android.i18n")
        .env("ANDROID_TZDATA_ROOT", "/apex/com.android.tzdata")
        .arg(format!("-Djava.class.path={apk}")).arg("/")
        .arg(entry)
        .stdin(Stdio::piped()).stdout(Stdio::piped()).stderr(Stdio::inherit())
        .spawn() else {
            error!("failed to spawn app_process");
            return String::new();
        };
    if let Some(mut si) = ch.stdin.take() {
        let _ = si.write_all(queries.join("\n").as_bytes());
        let _ = si.write_all(b"\n");
    }
    ch.wait_with_output().map(|o| String::from_utf8_lossy(&o.stdout).into_owned()).unwrap_or_default()
}

fn parse_codes(text: &str, with_label: bool) -> Option<Codes> {
    let get = |q: &str| text.lines().find_map(|l| {
        let (k, v) = l.rsplit_once(' ')?;
        if k == q { v.parse::<u32>().ok() } else { None }
    }).unwrap_or(0);    let c = Codes {
        reg: get(Q[0]), fg_act: get(Q[1]), fg_svc: get(Q[2]),
        died: get(Q[3]), pm_name: get(Q[4]),
        is_interactive: get(Q[5]), is_saver: get(Q[6]), get_zen: get(Q[7]),
        disp_reg: get(Q[8]),
        bat_prop: get(Q[9]),
        proc_started: get(Q[10]),
        app_label: if with_label { get(LABEL_Q) } else { 0 },
        recent_tasks: get(Q[11]),
    };
    // bat_prop is optional (the battery field becomes "unknown" if it is missing)
    let req = [c.reg, c.fg_act, c.died, c.pm_name,
               c.is_interactive, c.is_saver, c.get_zen, c.disp_reg];
    req.iter().all(|&v| v != 0).then_some(c)
}

fn log_codes(c: &Codes) {
    info!("transaction codes: registerProcessObserver={} fgActivities={} fgServices={} processDied={} getNameForUid={}",
          c.reg, c.fg_act, c.fg_svc, c.died, c.pm_name);
    info!("transaction codes: isInteractive={} isPowerSaveMode={} getZenMode={} registerDisplayCallback={} getProperty={}",
          c.is_interactive, c.is_saver, c.get_zen, c.disp_reg, c.bat_prop);
    info!("transaction codes: processStarted={} getApplicationLabel={} getRecentTasks={}",
          c.proc_started, c.app_label, c.recent_tasks);
}

/// Marker of the cache file: the hash of the query list. A new query (a new
/// transaction) needs a new resolver run, so an old cache is not used.
fn cache_marker(with_label: bool) -> String {
    let mut h: u64 = 0xcbf2_9ce4_8422_2325;
    let mut feed = |s: &str| {
        for b in s.as_bytes() {
            h ^= *b as u64;
            h = h.wrapping_mul(0x0000_0100_0000_01b3);
        }
    };
    for q in Q.iter() {
        feed(q);
    }
    if with_label {
        feed(LABEL_Q);
    }
    format!("#{:016x}", h)
}

/// The resolver output with the marker in front of it.
fn with_marker(text: &str, with_label: bool) -> String {
    format!("{}\n{}", cache_marker(with_label), text)
}

/// True when the cache file was written with the same queries.
fn cache_is_current(text: &str, with_label: bool) -> bool {
    text.lines().next().map(|l| l.trim()) == Some(cache_marker(with_label).as_str())
}

pub(crate) fn load_codes() -> Option<Codes> {
    let sdk = prop("ro.build.version.sdk");
    let inc: String = prop("ro.build.version.incremental").chars()
        .map(|c| if c.is_ascii_alphanumeric() { c } else { '_' }).collect();
    let cache = format!("{}/debug/codes.{sdk}.{inc}", crate::CFG_DIR);
    info!("device: sdk={sdk} incremental={inc}");

    // Android 16 dropped getApplicationLabel from the AIDL entirely.
    let want_label = sdk.parse::<u32>().map(|v| v < 36).unwrap_or(true);
    let mut queries: Vec<&str> = Q.to_vec();
    if want_label { queries.push(LABEL_Q); }

    if let Some(t) = fs::read_to_string(&cache).ok()
        && cache_is_current(&t, want_label)
        && let Some(c) = parse_codes(&t, want_label)
    {
        info!("transaction codes from cache: {cache}");
        log_codes(&c);
        return Some(c);
    }

    info!("cache missing/invalid, running resolver ({} byte APK)", RESOLVER_APK.len());
    let _ = fs::remove_file(APK_PATH);
    if let Err(e) = fs::write(APK_PATH, RESOLVER_APK) {
        error!("failed to write {APK_PATH}: {e}");
        return None;
    }
    if let Err(e) = fs::set_permissions(APK_PATH, fs::Permissions::from_mode(0o444)) {
        error!("chmod failed for {APK_PATH}: {e}");
        return None;
    }
    let t0 = std::time::Instant::now();
    let mut out = String::new();
    for entry in ENTRY_CLASSES {
        out = run_resolver(APK_PATH, entry, &queries);
        if parse_codes(&out, want_label).is_some() {
            debug!("resolver entry point: {entry}");
            break;
        }
        warn!("resolver produced no usable output with {entry}, trying the next entry point");
    }
    let _ = fs::remove_file(APK_PATH);
    debug!("resolver finished in {} ms, output:\n{}", t0.elapsed().as_millis(), out.trim_end());

    let Some(c) = parse_codes(&out, want_label) else {
        error!("resolver output incomplete, required codes not found");
        return None;
    };
    match fs::write(&cache, with_marker(&out, want_label)) {
        Ok(()) => info!("transaction codes saved to cache: {cache}"),
        Err(e) => warn!("failed to save cache {cache}: {e}"),
    }
    log_codes(&c);
    if c.bat_prop == 0 {
        warn!("getProperty code not found, battery field not monitored");
    }
    if c.proc_started == 0 {
        warn!("onProcessStarted code not found, the process table only learns about new pids from the /proc scan");
    }
    if c.app_label == 0 {
        warn!("getApplicationLabel is not available, app labels are read from the APK resources");
    }
    Some(c)
}
