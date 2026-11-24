//! Basename glob filters (`-i` ignore and `-I` include) backed by `libc::fnmatch`.

use std::ffi::{CString, OsStr};
use std::os::unix::ffi::OsStrExt;
use std::path::Path;

pub const MAX_NUMBER_OF_IGNORES: usize = 1024;
pub const MAX_NUMBER_OF_FILENAME_FILTERS: usize = 1024;
pub const MAX_FILENAME_LENGTH: usize = 1024;

#[derive(Debug, Default, Clone)]
pub struct FilterList {
    patterns: Vec<CString>,
}

impl FilterList {
    pub fn is_empty(&self) -> bool {
        self.patterns.is_empty()
    }

    pub fn push_ignore(&mut self, pat: &OsStr) -> Result<(), String> {
        let bytes = pat.as_bytes();
        if bytes.len() > MAX_FILENAME_LENGTH {
            return Err(format!(
                "pattern too long for -i: {}",
                pat.to_string_lossy()
            ));
        }
        if self.patterns.len() >= MAX_NUMBER_OF_IGNORES {
            return Err("too many -i patterns".to_string());
        }
        let c = CString::new(bytes).map_err(|_| "invalid NUL byte in -i pattern".to_string())?;
        self.patterns.push(c);
        Ok(())
    }

    pub fn push_filter(&mut self, pat: &OsStr) -> Result<(), String> {
        let bytes = pat.as_bytes();
        if bytes.len() > MAX_FILENAME_LENGTH {
            return Err(format!(
                "pattern too long for -I: {}",
                pat.to_string_lossy()
            ));
        }
        if self.patterns.len() >= MAX_NUMBER_OF_FILENAME_FILTERS {
            return Err("too many -I patterns".to_string());
        }
        let c = CString::new(bytes).map_err(|_| "invalid NUL byte in -I pattern".to_string())?;
        self.patterns.push(c);
        Ok(())
    }

    /// Match any pattern against the POSIX basename of `path` using `fnmatch(..., 0)`.
    pub fn matches_basename(&self, path: &Path) -> bool {
        if self.patterns.is_empty() {
            return false;
        }
        let fname = posix_basename(path.as_os_str().as_bytes());
        let Ok(c_fname) = CString::new(fname) else {
            return false;
        };
        self.patterns
            .iter()
            .any(|pat| unsafe { libc::fnmatch(pat.as_ptr(), c_fname.as_ptr(), 0) == 0 })
    }
}

/// Extract the POSIX `basename` slice without allocation.
fn posix_basename(mut bytes: &[u8]) -> &[u8] {
    while bytes.len() > 1 && bytes.last() == Some(&b'/') {
        bytes = &bytes[..bytes.len() - 1];
    }
    if bytes.is_empty() {
        return b".";
    }
    if bytes == b"/" {
        return b"/";
    }
    match bytes.iter().rposition(|&b| b == b'/') {
        Some(pos) => &bytes[pos + 1..],
        None => bytes,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fnmatch_ignore_and_include() {
        let mut fl = FilterList::default();
        fl.push_ignore(OsStr::new("*.tmp")).unwrap();
        fl.push_ignore(OsStr::new("skipdir")).unwrap();

        assert!(fl.matches_basename(Path::new("/a/b/foo.tmp")));
        assert!(fl.matches_basename(Path::new("/a/b/skipdir/")));
        assert!(!fl.matches_basename(Path::new("/a/b/foo.so")));
    }

    #[test]
    fn enforces_max_length_and_count() {
        let mut fl = FilterList::default();
        let long = "a".repeat(MAX_FILENAME_LENGTH + 1);
        assert!(fl.push_ignore(OsStr::new(&long)).is_err());

        for i in 0..MAX_NUMBER_OF_IGNORES {
            fl.push_ignore(OsStr::new(&format!("p{i}"))).unwrap();
        }
        assert!(fl.push_ignore(OsStr::new("overflow")).is_err());
    }
}
