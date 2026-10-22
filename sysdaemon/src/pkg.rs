// ---------------- package manager helpers: uid -> package, package -> label ----------------
use std::collections::HashMap;
use std::ffi::CString;
use std::sync::{LazyLock, Mutex, OnceLock};

use crate::codes::{codes, codes_opt};
use crate::log::{debug, warn};
use crate::ndk::{call, n, str_alloc, P};

static PM: OnceLock<usize> = OnceLock::new();

static UID_CACHE: LazyLock<Mutex<HashMap<i32, String>>> =
    LazyLock::new(|| Mutex::new(HashMap::new()));
static LABEL_CACHE: LazyLock<Mutex<HashMap<String, String>>> =
    LazyLock::new(|| Mutex::new(HashMap::new()));

/// Keep the label cache small, it only grows with the number of apps the user
/// ever opened.
const MAX_LABEL_CACHE: usize = 128;

pub(crate) fn set_pm(b: P) { let _ = PM.set(b as usize); }

fn pm() -> Option<P> { PM.get().map(|v| *v as P) }

/// Package name of a uid (IPackageManager.getNameForUid).
pub(crate) fn pkg_of_uid(uid: i32) -> Option<String> {
    if let Some(p) = UID_CACHE.lock().unwrap_or_else(|e| e.into_inner()).get(&uid) {
        return Some(p.clone());
    }
    let pm = pm()?;
    let n = n();
    // The cache lock is NOT held during the synchronous call to system_server.
    let o = call(pm, codes().pm_name, |i| unsafe { (n.AParcel_writeInt32)(i, uid); })?;
    let mut buf: Vec<u8> = Vec::new();
    let r = unsafe { (n.AParcel_readString)(o, &mut buf as *mut Vec<u8> as P, str_alloc) };
    unsafe { (n.AParcel_delete)(o) };
    if r != 0 { warn!("readString getNameForUid failed, status={r}"); return None; }
    let end = buf.iter().position(|&b| b == 0).unwrap_or(buf.len());
    let name = String::from_utf8_lossy(&buf[..end]).into_owned();
    if name.is_empty() { return None; }
    UID_CACHE.lock().unwrap_or_else(|e| e.into_inner()).insert(uid, name.clone());
    Some(name)
}

/// Human readable label of a package ("MT Manager" for "bin.mt.plus").
///
/// Android 16 removed getApplicationLabel() from the AIDL, so the label is read
/// from the APK resources instead (see [`crate::label`]); the package name is
/// only used when even that fails.
pub(crate) fn label_of(pkg: &str) -> String {
    if pkg.is_empty() || pkg == "none" || pkg == "unknown" { return "Unknown".to_string(); }
    if let Some(l) = LABEL_CACHE.lock().unwrap_or_else(|e| e.into_inner()).get(pkg) {
        return l.clone();
    }
    let label = fetch_label(pkg)
        .or_else(|| crate::label::apk_label(pkg))
        .map(|l| sanitize(&l))
        .unwrap_or_else(|| {
            debug!("no label for {pkg}, using the package name");
            pkg.to_string()
        });
    let mut g = LABEL_CACHE.lock().unwrap_or_else(|e| e.into_inner());
    if g.len() >= MAX_LABEL_CACHE { g.clear(); }
    g.insert(pkg.to_string(), label.clone());
    label
}

/// app_status is line based, so a label must stay on one line.
fn sanitize(s: &str) -> String {
    let s: String = s.chars().map(|c| if c.is_control() { ' ' } else { c }).collect();
    s.split_whitespace().collect::<Vec<_>>().join(" ")
}

/// IPackageManager.getApplicationLabel(packageName, userId).
fn fetch_label(pkg: &str) -> Option<String> {
    let code = codes_opt()?.app_label;
    if code == 0 { return None; } // not available on this ROM
    let pm = pm()?;
    let n = n();
    let Ok(c) = CString::new(pkg) else { return None };
    let o = call(pm, code, |i| unsafe {
        (n.AParcel_writeString)(i, c.as_ptr());
        (n.AParcel_writeInt32)(i, 0); // user 0 (owner)
    })?;
    // CharSequence reply: int32 kind (0=null, 1=String, 2=SpannedString,
    // 3=CharSequenceParcelable) followed by the value itself.
    let mut kind = 0i32;
    if unsafe { (n.AParcel_readInt32)(o, &mut kind) } != 0 {
        unsafe { (n.AParcel_delete)(o) };
        return None;
    }
    let mut label = String::new();
    if kind == 1 {
        let mut buf: Vec<u8> = Vec::new();
        let r = unsafe { (n.AParcel_readString)(o, &mut buf as *mut Vec<u8> as P, str_alloc) };
        if r == 0 {
            let end = buf.iter().position(|&b| b == 0).unwrap_or(buf.len());
            label = String::from_utf8_lossy(&buf[..end]).trim().to_string();
        }
    } else if kind != 0 {
        debug!("getApplicationLabel: unsupported CharSequence kind {kind}");
    }
    unsafe { (n.AParcel_delete)(o) };
    (!label.is_empty()).then_some(label)
}
