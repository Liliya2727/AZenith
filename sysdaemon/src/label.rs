// ---------------- app labels read straight from the APK ----------------
//
// Android 16 dropped getApplicationLabel() from IPackageManager.aidl: the label
// is resolved by the caller from the package resources (PackageItemInfo.loadLabel).
// This module does the same, without any Android API:
//
//   base.apk -> AndroidManifest.xml (binary XML) -> <application android:label>
//            -> resources.arsc -> the string value
//
// Only those two zip entries are read (the manifest is deflated and inflated
// into memory, resources.arsc is stored and mapped read-only), so a lookup
// costs a few ms the first time and nothing afterwards (the caller caches the
// result). No fork, no extra dependency: raw deflate is done by the system libz
// through dlopen.
use std::collections::HashMap;
use std::ffi::{c_char, c_int, c_uint, c_void};
use std::fs::File;
use std::io::{Read, Seek, SeekFrom};
use std::os::unix::io::AsRawFd;
use std::path::{Path, PathBuf};
use std::sync::{LazyLock, Mutex, OnceLock};

use crate::log::{debug, warn};

/// android:label attribute id (0x01010001).
const ANDROID_LABEL_ATTR: u32 = 0x0101_0001;
const ANDROID_NS: &str = "http://schemas.android.com/apk/res/android";
/// Reference chains longer than this are considered broken.
const MAX_REF_DEPTH: u8 = 6;
/// Sanity caps for the entries we have to decompress.
const MAX_MANIFEST_BYTES: u64 = 4 * 1024 * 1024;

/// Human readable label of a package, read from its base APK.
/// Returns `None` if the APK is unknown/unreadable or the label cannot be
/// resolved, in which case the caller falls back to the package name.
pub(crate) fn apk_label(pkg: &str) -> Option<String> {
    let apk = apk_path(pkg)?;
    let manifest = zip_entry(&apk, "AndroidManifest.xml")?;
    match axml_label(manifest.as_slice())? {
        Label::Text(s) => clean(s.as_str()),
        Label::Res(id) => {
            let arsc = zip_entry(&apk, "resources.arsc")?;
            clean(arsc_string(arsc.as_slice(), id, 0)?.as_str())
        }
    }
}

fn clean(s: &str) -> Option<String> {
    // app_status is line based: a label with a newline would corrupt the file.
    let s: String = s
        .chars()
        .map(|c| if c.is_control() { ' ' } else { c })
        .collect();
    let s = s.split_whitespace().collect::<Vec<_>>().join(" ");
    (!s.is_empty()).then_some(s)
}

// ---------------- package -> base.apk ----------------

static APK_PATHS: LazyLock<Mutex<HashMap<String, PathBuf>>> = LazyLock::new(|| Mutex::new(HashMap::new()));

/// base.apk of a package. The index is (re)scanned when a package is not in it,
/// so apps installed while the daemon runs are found too.
fn apk_path(pkg: &str) -> Option<PathBuf> {
    let mut map = APK_PATHS.lock().unwrap_or_else(|e| e.into_inner());
    if map.is_empty() || !map.contains_key(pkg) {
        scan_app_root(Path::new("/data/app"), &mut map);
        debug!("apk index: {} packages", map.len());
    }
    map.get(pkg).cloned()
}

/// /data/app/~~<hash>==/<pkg>-<hash>==/base.apk (and the flat legacy layout).
fn scan_app_root(root: &Path, out: &mut HashMap<String, PathBuf>) {
    let Ok(rd) = root.read_dir() else { return };
    for e in rd.flatten() {
        let dir = e.path();
        if !dir.is_dir() {
            continue;
        }
        if let Some(pkg) = dir_pkg(&dir) {
            let apk = dir.join("base.apk");
            if apk.is_file() {
                out.insert(pkg, apk);
                continue;
            }
        }
        let Ok(inner) = dir.read_dir() else { continue };
        for e2 in inner.flatten() {
            let d = e2.path();
            if !d.is_dir() {
                continue;
            }
            let Some(pkg) = dir_pkg(&d) else { continue };
            let apk = d.join("base.apk");
            if apk.is_file() {
                out.insert(pkg, apk);
            }
        }
    }
}

/// "<pkg>-<hash>==" -> "<pkg>" (package names never contain '-').
fn dir_pkg(dir: &Path) -> Option<String> {
    let name = dir.file_name()?.to_str()?;
    let (pkg, _) = name.split_once('-')?;
    (!pkg.is_empty()).then(|| pkg.to_string())
}

// ---------------- zip ----------------

struct ZipEntry {
    method: u16,
    comp: u64,
    uncomp: u64,
    local: u64,
}

fn zip_entry(path: &Path, name: &str) -> Option<Bytes> {
    let mut f = File::open(path).ok()?;
    let size = f.metadata().ok()?.len();
    let tail_len = size.min(66_000) as usize;
    f.seek(SeekFrom::Start(size - tail_len as u64)).ok()?;
    let mut tail = vec![0u8; tail_len];
    f.read_exact(&mut tail).ok()?;

    // End of central directory record (last one wins, comments may hide it).
    let mut eocd = None;
    let mut i = tail_len.checked_sub(22)?;
    loop {
        if u32at(&tail, i) == Some(0x0605_4b50) {
            eocd = Some(i);
            break;
        }
        if i == 0 {
            break;
        }
        i -= 1;
    }
    let e = eocd?;
    let count = u16at(&tail, e + 10)? as usize;
    let cd_size = u32at(&tail, e + 12)? as usize;
    let cd_off = u32at(&tail, e + 16)? as u64;
    if cd_size > 32 * 1024 * 1024 {
        return None;
    }

    let mut cd = vec![0u8; cd_size];
    f.seek(SeekFrom::Start(cd_off)).ok()?;
    f.read_exact(&mut cd).ok()?;

    let mut off = 0usize;
    for _ in 0..count {
        if u32at(&cd, off)? != 0x0201_4b50 {
            return None;
        }
        let ename_len = u16at(&cd, off + 28)? as usize;
        let extra_len = u16at(&cd, off + 30)? as usize;
        let comment_len = u16at(&cd, off + 32)? as usize;
        let ename = cd.get(off + 46..off + 46 + ename_len)?;
        if ename == name.as_bytes() {
            let ent = ZipEntry {
                method: u16at(&cd, off + 10)?,
                comp: u32at(&cd, off + 20)? as u64,
                uncomp: u32at(&cd, off + 24)? as u64,
                local: u32at(&cd, off + 42)? as u64,
            };
            return read_zip_data(&mut f, &ent);
        }
        off += 46 + ename_len + extra_len + comment_len;
    }
    None
}

fn read_zip_data(f: &mut File, e: &ZipEntry) -> Option<Bytes> {
    let mut lh = [0u8; 30];
    f.seek(SeekFrom::Start(e.local)).ok()?;
    f.read_exact(&mut lh).ok()?;
    if u32at(&lh, 0)? != 0x0403_4b50 {
        return None;
    }
    let name_len = u16at(&lh, 26)? as u64;
    let extra_len = u16at(&lh, 28)? as u64;
    let data_off = e.local + 30 + name_len + extra_len;

    match e.method {
        // Stored entries (resources.arsc is always stored): map the bytes
        // instead of copying them, some resource tables are 100 MB+.
        0 => {
            if e.comp != e.uncomp || e.uncomp == 0 {
                return None;
            }
            mmap_range(f, data_off, e.uncomp as usize).map(Bytes::Mapped)
        }
        8 => {
            if e.comp > MAX_MANIFEST_BYTES || e.uncomp > MAX_MANIFEST_BYTES {
                return None;
            }
            let mut data = vec![0u8; e.comp as usize];
            f.seek(SeekFrom::Start(data_off)).ok()?;
            f.read_exact(&mut data).ok()?;
            inflate_raw(&data, e.uncomp as usize).map(Bytes::Owned)
        }
        _ => None,
    }
}

/// One zip entry: either decompressed into memory or mapped from the APK.
enum Bytes {
    Owned(Vec<u8>),
    Mapped(Mapped),
}

impl Bytes {
    fn as_slice(&self) -> &[u8] {
        match self {
            Bytes::Owned(v) => v,
            Bytes::Mapped(m) => m.as_slice(),
        }
    }
}

struct Mapped {
    ptr: *mut c_void,
    len: usize,
    skip: usize,
}

impl Mapped {
    fn as_slice(&self) -> &[u8] {
        // SAFETY: the range was mapped read-only and lives as long as `self`.
        unsafe { std::slice::from_raw_parts((self.ptr as *const u8).add(self.skip), self.len) }
    }
}

impl Drop for Mapped {
    fn drop(&mut self) {
        unsafe { libc::munmap(self.ptr, self.skip + self.len) };
    }
}

fn mmap_range(f: &File, off: u64, len: usize) -> Option<Mapped> {
    let page = unsafe { libc::sysconf(libc::_SC_PAGESIZE) } as u64;
    if page == 0 {
        return None;
    }
    let aligned = off & !(page - 1);
    let skip = (off - aligned) as usize;
    let total = skip.checked_add(len)?;
    let ptr = unsafe {
        libc::mmap(
            std::ptr::null_mut(),
            total,
            libc::PROT_READ,
            libc::MAP_PRIVATE,
            f.as_raw_fd(),
            aligned as libc::off_t,
        )
    };
    if ptr == libc::MAP_FAILED {
        return None;
    }
    Some(Mapped { ptr, len, skip })
}

// ---------------- zlib (dlopen) ----------------

#[repr(C)]
struct ZStream {
    next_in: *const u8,
    avail_in: c_uint,
    total_in: libc::c_ulong,
    next_out: *mut u8,
    avail_out: c_uint,
    total_out: libc::c_ulong,
    msg: *const c_char,
    state: *mut c_void,
    zalloc: *const c_void,
    zfree: *const c_void,
    opaque: *mut c_void,
    data_type: c_int,
    adler: libc::c_ulong,
    reserved: libc::c_ulong,
}

type InflateInit2 = unsafe extern "C" fn(*mut ZStream, c_int, *const c_char, c_int) -> c_int;
type Inflate = unsafe extern "C" fn(*mut ZStream, c_int) -> c_int;
type InflateEnd = unsafe extern "C" fn(*mut ZStream) -> c_int;

struct Zlib {
    init2: InflateInit2,
    inflate: Inflate,
    end: InflateEnd,
    version: *const c_char,
}
// The function pointers and the version string are immutable for the lifetime
// of the process.
unsafe impl Send for Zlib {}
unsafe impl Sync for Zlib {}

fn zlib() -> Option<&'static Zlib> {
    static Z: OnceLock<Option<Zlib>> = OnceLock::new();
    Z.get_or_init(|| unsafe {
        let mut h = libc::dlopen(c"libz.so".as_ptr(), libc::RTLD_NOW);
        if h.is_null() {
            h = libc::dlopen(c"libz.so.1".as_ptr(), libc::RTLD_NOW);
        }
        if h.is_null() {
            warn!("libz.so is not loadable, labels are read without decompression");
            return None;
        }
        let sym = |n: &std::ffi::CStr| libc::dlsym(h, n.as_ptr());
        let init2 = sym(c"inflateInit2_");
        let inflate = sym(c"inflate");
        let end = sym(c"inflateEnd");
        let ver = sym(c"zlibVersion");
        if init2.is_null() || inflate.is_null() || end.is_null() || ver.is_null() {
            warn!("libz.so has no inflate symbols");
            return None;
        }
        let version: unsafe extern "C" fn() -> *const c_char = std::mem::transmute(ver);
        let init2: InflateInit2 = std::mem::transmute(init2);
        let inflate: Inflate = std::mem::transmute(inflate);
        let end: InflateEnd = std::mem::transmute(end);
        Some(Zlib { init2, inflate, end, version: version() })
    })
    .as_ref()
}

/// Raw deflate (no zlib header), output size taken from the zip directory.
fn inflate_raw(data: &[u8], out_len: usize) -> Option<Vec<u8>> {
    let z = zlib()?;
    if data.len() > c_uint::MAX as usize || out_len > c_uint::MAX as usize {
        return None;
    }
    let mut st: ZStream = unsafe { std::mem::zeroed() };
    let mut out = vec![0u8; out_len.max(1)];
    st.next_in = data.as_ptr();
    st.avail_in = data.len() as c_uint;
    st.next_out = out.as_mut_ptr();
    st.avail_out = out.len() as c_uint;

    let r = unsafe { (z.init2)(&mut st, -15, z.version, std::mem::size_of::<ZStream>() as c_int) };
    if r != 0 {
        return None;
    }
    let r = unsafe { (z.inflate)(&mut st, 4) }; // Z_FINISH
    unsafe { (z.end)(&mut st) };
    if r != 1 {
        // Z_STREAM_END
        return None;
    }
    out.truncate(st.total_out as usize);
    Some(out)
}

// ---------------- string pools ----------------

struct Pool<'a> {
    b: &'a [u8],
    offs: usize,
    start: usize,
    count: usize,
    utf8: bool,
}

impl<'a> Pool<'a> {
    fn new(b: &'a [u8], off: usize) -> Option<Self> {
        if u16at(b, off)? != 0x0001 {
            return None;
        }
        let hdr = u16at(b, off + 2)? as usize;
        let count = u32at(b, off + 8)? as usize;
        let flags = u32at(b, off + 16)?;
        let start = off.checked_add(u32at(b, off + 20)? as usize)?;
        let offs = off.checked_add(hdr)?;
        if count > 1 << 20 || offs.checked_add(count * 4)? > b.len() {
            return None;
        }
        Some(Pool { b, offs, start, count, utf8: flags & 0x100 != 0 })
    }

    fn get(&self, i: u32) -> Option<String> {
        let i = i as usize;
        if i >= self.count {
            return None;
        }
        let p = self.start.checked_add(u32at(self.b, self.offs + i * 4)? as usize)?;
        if self.utf8 { self.utf8_at(p) } else { self.utf16_at(p) }
    }

    fn utf8_at(&self, p: usize) -> Option<String> {
        let (_, n1) = len8(self.b, p)?;
        let (bytes, n2) = len8(self.b, p + n1)?;
        let s = self.b.get(p + n1 + n2..p + n1 + n2 + bytes)?;
        Some(String::from_utf8_lossy(s).into_owned())
    }

    fn utf16_at(&self, p: usize) -> Option<String> {
        let v = u16at(self.b, p)?;
        let (len, hdr) = if v & 0x8000 != 0 {
            ((((v & 0x7fff) as usize) << 16) | u16at(self.b, p + 2)? as usize, 4)
        } else {
            (v as usize, 2)
        };
        if len > 1 << 20 {
            return None;
        }
        let units = (0..len)
            .map(|i| u16at(self.b, p + hdr + i * 2))
            .collect::<Option<Vec<u16>>>()?;
        Some(String::from_utf16_lossy(&units))
    }
}

/// UTF-8 pool length: 1 byte, or 2 bytes with the high bit of the first set.
fn len8(b: &[u8], p: usize) -> Option<(usize, usize)> {
    let b0 = *b.get(p)?;
    if b0 & 0x80 != 0 {
        Some(((((b0 & 0x7f) as usize) << 8) | *b.get(p + 1)? as usize, 2))
    } else {
        Some((b0 as usize, 1))
    }
}

// ---------------- binary AndroidManifest.xml ----------------

enum Label {
    Text(String),
    Res(u32),
}

const CHUNK_STRING_POOL: u16 = 0x0001;
const CHUNK_XML: u16 = 0x0003;
const CHUNK_XML_RESOURCE_MAP: u16 = 0x0180;
const CHUNK_XML_START_ELEMENT: u16 = 0x0102;

fn axml_label(xml: &[u8]) -> Option<Label> {
    if u16at(xml, 0)? != CHUNK_XML {
        return None;
    }
    let mut pool: Option<Pool> = None;
    let mut resmap: &[u8] = &[];
    let mut off = u16at(xml, 2)? as usize;

    while off + 8 <= xml.len() {
        let ty = u16at(xml, off)?;
        let hdr = u16at(xml, off + 2)? as usize;
        let size = u32at(xml, off + 4)? as usize;
        if size < 8 || off.checked_add(size)? > xml.len() {
            return None;
        }
        match ty {
            CHUNK_STRING_POOL => {
                if pool.is_none() {
                    pool = Pool::new(xml, off);
                }
            }
            CHUNK_XML_RESOURCE_MAP => {
                if hdr <= size {
                    resmap = &xml[off + hdr..off + size];
                }
            }
            CHUNK_XML_START_ELEMENT => {
                let p = pool.as_ref()?;
                if let Some(l) = label_attr(xml, off, hdr, p, resmap) {
                    return Some(l);
                }
            }
            _ => {}
        }
        off += size;
    }
    None
}

fn label_attr(xml: &[u8], off: usize, hdr: usize, p: &Pool, resmap: &[u8]) -> Option<Label> {
    let ext = off + hdr; // ResXMLTree_attrExt
    if p.get(u32at(xml, ext + 4)?).as_deref() != Some("application") {
        return None;
    }
    let attr_start = u16at(xml, ext + 8)? as usize;
    let attr_size = u16at(xml, ext + 10)? as usize;
    let attr_count = u16at(xml, ext + 12)? as usize;
    let base = ext + attr_start;

    for k in 0..attr_count {
        let a = base + k * attr_size;
        if a + 20 > xml.len() {
            return None;
        }
        let ns = u32at(xml, a)?;
        let name = u32at(xml, a + 4)?;
        let raw = u32at(xml, a + 8)?;
        let dtype = *xml.get(a + 15)?;
        let data = u32at(xml, a + 16)?;

        let by_id = resmap_value(resmap, name) == Some(ANDROID_LABEL_ATTR);
        let by_name = p.get(name).as_deref() == Some("label")
            && p.get(ns).as_deref() == Some(ANDROID_NS);
        if !by_id && !by_name {
            continue;
        }

        match dtype {
            0x03 => {
                // TYPE_STRING
                let idx = if raw != u32::MAX { raw } else { data };
                return p.get(idx).map(Label::Text);
            }
            0x01 | 0x07 => {
                // TYPE_REFERENCE / TYPE_DYNAMIC_REFERENCE
                if data != 0 {
                    return Some(Label::Res(data));
                }
            }
            _ => {
                if raw != u32::MAX
                    && let Some(s) = p.get(raw)
                {
                    return Some(Label::Text(s));
                }
            }
        }
    }
    None
}

fn resmap_value(map: &[u8], idx: u32) -> Option<u32> {
    u32at(map, idx as usize * 4).filter(|&v| v != 0)
}

// ---------------- resources.arsc ----------------

const CHUNK_TABLE: u16 = 0x0002;
const CHUNK_TABLE_PACKAGE: u16 = 0x0200;
const CHUNK_TABLE_TYPE: u16 = 0x0201;

/// Resolve a resource id to its string value, preferring the device locale and
/// falling back to the default (locale-less) configuration.
fn arsc_string(arsc: &[u8], id: u32, depth: u8) -> Option<String> {
    if depth > MAX_REF_DEPTH || u16at(arsc, 0)? != CHUNK_TABLE {
        return None;
    }
    let pkg_id = (id >> 24) as u8;
    let type_id = ((id >> 16) & 0xff) as u8;
    let entry_idx = (id & 0xffff) as usize;

    let mut pool: Option<Pool> = None;
    let mut best: Option<(i32, String)> = None;
    let mut off = u16at(arsc, 2)? as usize;

    while off + 8 <= arsc.len() {
        let ty = u16at(arsc, off)?;
        let hdr = u16at(arsc, off + 2)? as usize;
        let size = u32at(arsc, off + 4)? as usize;
        if size < 8 || off.checked_add(size)? > arsc.len() {
            return None;
        }
        match ty {
            CHUNK_STRING_POOL => {
                if pool.is_none() {
                    pool = Pool::new(arsc, off);
                }
            }
            CHUNK_TABLE_PACKAGE if (u32at(arsc, off + 8)? as u8) == pkg_id => {
                scan_package(arsc, off, hdr, size, type_id, entry_idx, &pool, depth, &mut best);
            }
            _ => {}
        }
        off += size;
    }
    best.map(|(_, s)| s)
}

#[allow(clippy::too_many_arguments)]
fn scan_package(
    b: &[u8],
    pkg_off: usize,
    pkg_hdr: usize,
    pkg_size: usize,
    type_id: u8,
    entry_idx: usize,
    pool: &Option<Pool>,
    depth: u8,
    best: &mut Option<(i32, String)>,
) {
    let end = (pkg_off + pkg_size).min(b.len());
    let mut off = pkg_off + pkg_hdr;
    while off + 8 <= end {
        let Some(ty) = u16at(b, off) else { return };
        let Some(hdr) = u16at(b, off + 2) else { return };
        let Some(size) = u32at(b, off + 4) else { return };
        let size = size as usize;
        if size < 8 || off + size > end {
            return;
        }
        if ty == CHUNK_TABLE_TYPE
            && b.get(off + 8).copied() == Some(type_id)
            && let Some((score, dtype, data)) = type_entry(b, off, hdr as usize, entry_idx)
            && score > 0
            && best.as_ref().is_none_or(|(s, _)| score > *s)
        {
            let value = match dtype {
                0x03 => pool.as_ref().and_then(|p| p.get(data)),
                0x01 | 0x07 => arsc_string(b, data, depth + 1),
                _ => None,
            };
            if let Some(v) = value {
                *best = Some((score, v));
            }
        }
        off += size;
    }
}

/// Value of entry `entry_idx` in a ResTable_type chunk: (config score, data
/// type, data). Score 0 means "config does not apply to this device".
fn type_entry(b: &[u8], off: usize, hdr: usize, entry_idx: usize) -> Option<(i32, u8, u32)> {
    let flags = *b.get(off + 9)?;
    let count = u32at(b, off + 12)? as usize;
    let entries_start = u32at(b, off + 16)? as usize;
    let score = config_score(b, off + 20)?;

    let rel = if flags & 0x01 != 0 {
        // sparse: (u16 index, u16 offset/4) pairs, sorted by index
        let mut found = None;
        for k in 0..count {
            let e = u32at(b, off + hdr + k * 4)?;
            let idx = (e & 0xffff) as usize;
            if idx == entry_idx {
                found = Some((e >> 16) as usize * 4);
                break;
            }
            if idx > entry_idx {
                break;
            }
        }
        found?
    } else if flags & 0x02 != 0 {
        // 16 bit offsets, in units of 4 bytes
        let v = u16at(b, off + hdr + entry_idx * 2)?;
        if v == 0xffff {
            return None;
        }
        v as usize * 4
    } else {
        let v = u32at(b, off + hdr + entry_idx * 4)?;
        if v == 0xffff_ffff {
            return None;
        }
        v as usize
    };

    let e = off + entries_start + rel;
    let eflags = u16at(b, e + 2)?;
    if eflags & 0x0001 != 0 {
        return None; // complex (bag) entry, app labels are never bags
    }
    let (dtype, data) = if eflags & 0x0008 != 0 {
        // compact entry: data type in the high byte of flags, value inline
        (((eflags >> 8) & 0xff) as u8, u32at(b, e + 4)?)
    } else {
        let esize = u16at(b, e)? as usize;
        let v = e.checked_add(esize)?;
        (*b.get(v + 3)?, u32at(b, v + 4)?)
    };
    Some((score, dtype, data))
}

/// 3 = exact locale, 2 = same language, 1 = default config, 0 = other locale.
fn config_score(b: &[u8], cfg: usize) -> Option<i32> {
    let size = u32at(b, cfg)? as usize;
    if size < 12 {
        return Some(1);
    }
    let lang = b.get(cfg + 8..cfg + 10)?;
    let country = b.get(cfg + 10..cfg + 12)?;
    let (dl, dc) = device_locale();
    if lang == [0, 0] {
        return Some(1);
    }
    let l = String::from_utf8_lossy(lang).to_ascii_lowercase();
    if l == dl {
        let c = String::from_utf8_lossy(country).to_ascii_lowercase();
        return Some(if c == dc { 3 } else { 2 });
    }
    Some(0)
}

fn device_locale() -> (&'static str, &'static str) {
    static L: OnceLock<(String, String)> = OnceLock::new();
    L.get_or_init(|| {
        let mut v = prop(c"persist.sys.locale");
        if v.is_empty() {
            v = prop(c"ro.product.locale");
        }
        let mut it = v.split(['-', '_']);
        let lang = it.next().unwrap_or("").to_ascii_lowercase();
        let country = it.next().unwrap_or("").to_ascii_uppercase();
        (lang, country)
    });
    let (l, c) = L.get().unwrap();
    (l.as_str(), c.as_str())
}

fn prop(name: &std::ffi::CStr) -> String {
    let mut buf = [0u8; 128];
    let n = unsafe { libc::__system_property_get(name.as_ptr(), buf.as_mut_ptr() as *mut c_char) };
    if n <= 0 {
        return String::new();
    }
    String::from_utf8_lossy(&buf[..n as usize]).into_owned()
}

/// Value of a system property ("" when it is not set).
pub(crate) fn prop_str(name: &str) -> String {
    let Ok(c) = std::ffi::CString::new(name) else { return String::new() };
    prop(&c)
}

// ---------------- little endian helpers ----------------

fn u16at(b: &[u8], o: usize) -> Option<u16> {
    Some(u16::from_le_bytes(b.get(o..o + 2)?.try_into().ok()?))
}

fn u32at(b: &[u8], o: usize) -> Option<u32> {
    Some(u32::from_le_bytes(b.get(o..o + 4)?.try_into().ok()?))
}
