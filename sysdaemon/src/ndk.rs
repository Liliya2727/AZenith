// ---------------- libbinder_ndk via dlsym (like BinderNDK.cpp in encore) ----------------
use std::ffi::{c_void, CStr};
use std::os::raw::c_char;
use std::ptr::{null, null_mut};
use std::sync::OnceLock;
use std::{thread, time::Duration};

use crate::log::{error, info, warn};

pub(crate) type P = *mut c_void;
pub(crate) const UNKNOWN: i32 = i32::MIN;

macro_rules! ndk {
    ($($name:ident($($a:ty),*) $(-> $r:ty)?;)*) => {
        #[allow(non_snake_case)]
        pub(crate) struct Ndk { $(pub(crate) $name: unsafe extern "C" fn($($a),*) $(-> $r)?),* }
        fn load() -> Ndk {
            unsafe {
                let mut path = "/system/lib64/libbinder_ndk.so";
                let mut h = libc::dlopen(c"/system/lib64/libbinder_ndk.so".as_ptr(), libc::RTLD_NOW);
                if h.is_null() {
                    warn!("dlopen {path} failed, falling back to the library name only");
                    path = "libbinder_ndk.so";
                    h = libc::dlopen(c"libbinder_ndk.so".as_ptr(), libc::RTLD_NOW);
                }
                if h.is_null() {
                    error!("dlopen libbinder_ndk.so failed");
                    std::process::exit(1);
                }

                let names: &[&str] = &[$(stringify!($name)),*];
                let mut miss = Vec::new();
                for nm in names {
                    let c = std::ffi::CString::new(*nm).unwrap();
                    if libc::dlsym(h, c.as_ptr()).is_null() { miss.push(*nm) }
                }
                if !miss.is_empty() {
                    error!("missing libbinder_ndk symbols: {miss:?}");
                    std::process::exit(1);
                }
                info!("libbinder_ndk loaded from {path} ({} symbols)", names.len());

                Ndk { $($name: {
                    let p = libc::dlsym(h, concat!(stringify!($name), "\0").as_ptr() as *const c_char);
                    std::mem::transmute::<*mut c_void, unsafe extern "C" fn($($a),*) $(-> $r)?>(p)
                }),* }
            }
        }
    };
}

ndk! {
    AServiceManager_getService(*const c_char) -> P;
    AIBinder_Class_define(*const c_char, unsafe extern "C" fn(P) -> P,
        unsafe extern "C" fn(P), unsafe extern "C" fn(P, u32, P, P) -> i32) -> P;
    AIBinder_new(P, P) -> P;
    AIBinder_associateClass(P, P) -> bool;
    AIBinder_prepareTransaction(P, *mut P) -> i32;
    AIBinder_transact(P, u32, *mut P, *mut P, u32) -> i32;
    AIBinder_DeathRecipient_new(unsafe extern "C" fn(P)) -> P;
    AIBinder_linkToDeath(P, P, P) -> i32;
    AParcel_delete(P);
    AParcel_getDataSize(P) -> i32;
    AParcel_marshal(P, *mut u8, usize, usize) -> i32;
    AParcel_writeStrongBinder(P, P) -> i32;
    AParcel_writeInt32(P, i32) -> i32;
    AParcel_writeString(P, *const c_char) -> i32;
    AParcel_readInt32(P, *mut i32) -> i32;
    AParcel_readBool(P, *mut bool) -> i32;
    AParcel_readString(P, P, unsafe extern "C" fn(P, i32, *mut *mut c_char) -> bool) -> i32;
    AParcel_readStatusHeader(P, *mut P) -> i32;
    AStatus_isOk(P) -> bool;
    AStatus_getDescription(P) -> *const c_char;
    AStatus_deleteDescription(*const c_char);
    AStatus_delete(P);
    ABinderProcess_startThreadPool();
    ABinderProcess_joinThreadPool();
}

pub(crate) fn n() -> &'static Ndk {
    static N: OnceLock<Ndk> = OnceLock::new();
    N.get_or_init(load)
}

// ---------------- synchronous transactions ----------------
// Outgoing transactions carry no interface token: AParcel_writeInterfaceToken is not
// available in libbinder_ndk, and Java services accept transactions like this (verified
// directly on device).

pub(crate) fn call(b: P, code: u32, fill: impl FnOnce(P)) -> Option<P> {
    let n = n();
    unsafe {
        let (mut i, mut o): (P, P) = (null_mut(), null_mut());
        let r = (n.AIBinder_prepareTransaction)(b, &mut i);
        if r != 0 { error!("prepareTransaction failed, status={r}"); return None; }
        fill(i);
        let r = (n.AIBinder_transact)(b, code, &mut i, &mut o, 0);
        if r != 0 {
            error!("transact code={code} failed, status={r}");
            if !o.is_null() { (n.AParcel_delete)(o); }
            return None;
        }
        let mut sp: P = null_mut();
        let hr = (n.AParcel_readStatusHeader)(o, &mut sp);
        let ok = hr == 0 && !sp.is_null() && (n.AStatus_isOk)(sp);
        if !ok {
            let desc = if !sp.is_null() { (n.AStatus_getDescription)(sp) } else { null() };
            let msg = if desc.is_null() { String::new() }
                      else { format!(" ({})", CStr::from_ptr(desc).to_string_lossy()) };
            error!("reply code={code} not OK (readStatusHeader={hr}){msg}");
            if !desc.is_null() { (n.AStatus_deleteDescription)(desc); }
        }
        if !sp.is_null() { (n.AStatus_delete)(sp); }
        if ok { Some(o) } else { (n.AParcel_delete)(o); None }
    }
}

pub(crate) fn call_i32(b: P, code: u32) -> Option<i32> {
    let o = call(b, code, |_| {})?;
    let mut v = 0i32;
    let r = unsafe { (n().AParcel_readInt32)(o, &mut v) };
    unsafe { (n().AParcel_delete)(o); }
    (r == 0).then_some(v)
}

unsafe extern "C" fn noop_create(a: P) -> P { a }
unsafe extern "C" fn noop_destroy(_: P) {}
unsafe extern "C" fn noop_tx(_: P, _: u32, _: P, _: P) -> i32 { UNKNOWN }

pub(crate) fn class(desc: &CStr, tx: unsafe extern "C" fn(P, u32, P, P) -> i32) -> P {
    unsafe { (n().AIBinder_Class_define)(desc.as_ptr(), noop_create, noop_destroy, tx) }
}

pub(crate) fn service_try(name: &CStr, desc: &CStr, tries: u32) -> P {
    let cl = class(desc, noop_tx);
    for attempt in 1..=tries {
        let b = unsafe { (n().AServiceManager_getService)(name.as_ptr()) };
        if !b.is_null() && unsafe { (n().AIBinder_associateClass)(b, cl) } {
            info!("service {} acquired (attempt {attempt})", name.to_string_lossy());
            return b;
        }
        if attempt == 1 { warn!("service {} not available yet, waiting...", name.to_string_lossy()); }
        thread::sleep(Duration::from_millis(100));
    }
    error!("service {} not available after {} ms", name.to_string_lossy(), tries * 100);
    null_mut()
}

pub(crate) fn service(name: &CStr, desc: &CStr) -> P { service_try(name, desc, 100) }

// size already includes the NUL terminator; size <= 0 means a null string.
// For a null string libbinder_ndk passes buffer == null, so `out` is only
// written when it is valid.
pub(crate) unsafe extern "C" fn str_alloc(data: P, size: i32, out: *mut *mut c_char) -> bool {
    unsafe {
        if out.is_null() { return size <= 0; }
        if size <= 0 { *out = null_mut(); return true; }
        if data.is_null() { *out = null_mut(); return false; }
        let v = &mut *(data as *mut Vec<u8>);
        v.clear();
        v.resize(size as usize, 0);
        *out = v.as_mut_ptr() as *mut c_char;
    }
    true
}
