// Copyright (C) 2026-2027 Zexshia
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! Iterative directory walker (`MAX_CRAWL_DEPTH = 1024`, `(dev, ino)` loop detection).

use std::ffi::{CStr, CString, OsStr, OsString};
use std::io;
use std::os::unix::ffi::{OsStrExt, OsStringExt};
use std::path::{Path, PathBuf};

use super::dedup::InodeDedup;
use super::fileops::{increment_nofile_rlimit, process_file};
use super::stats::{PreloadConfig, PreloadObserver, Stats};

pub const MAX_CRAWL_DEPTH: usize = 1024;
const MAX_OPENDIR_RETRIES: usize = 8;

#[allow(clippy::unnecessary_cast)]
const fn mode_bits(m: libc::mode_t) -> u32 {
    m as u32
}

/// Stateful crawler shared across batch entries and positional arguments.
pub struct Crawler {
    dedup: InodeDedup,
    orig_device: Option<u64>,
    pub stats: Stats,
}

impl Crawler {
    pub fn new() -> Self {
        Self {
            dedup: InodeDedup::default(),
            orig_device: None,
            stats: Stats::default(),
        }
    }

    /// Crawl a single file or directory path iteratively (no stack recursion).
    pub fn crawl(
        &mut self,
        path: &Path,
        cfg: &PreloadConfig,
        obs: &mut dyn PreloadObserver,
    ) -> Result<(), String> {
        enum Frame {
            Visit(PathBuf),
            ExitDir,
        }

        let mut stack: Vec<Frame> = vec![Frame::Visit(path.to_path_buf())];
        // Active ancestor directories on the current traversal path: (st_dev, st_ino).
        // Intentional deviation (Inventory §6 item 11): tracks both dev and ino.
        let mut active_dirs: Vec<(u64, u64)> = Vec::new();

        while let Some(frame) = stack.pop() {
            match frame {
                Frame::ExitDir => {
                    active_dirs.pop();
                }
                Frame::Visit(raw_path) => {
                    let clean = strip_single_trailing_slash(&raw_path);
                    if !cfg.ignores.is_empty() && cfg.ignores.matches_basename(&clean) {
                        continue;
                    }

                    let clean_disp = clean.to_string_lossy();
                    let Ok(c_path) = CString::new(clean.as_os_str().as_bytes()) else {
                        obs.warning(&format!("unable to stat {clean_disp} (invalid path)"));
                        continue;
                    };

                    let mut sb: libc::stat = unsafe { std::mem::zeroed() };
                    let stat_rc = if cfg.follow_symlinks {
                        unsafe { libc::stat(c_path.as_ptr(), &mut sb) }
                    } else {
                        unsafe { libc::lstat(c_path.as_ptr(), &mut sb) }
                    };
                    if stat_rc != 0 {
                        let err = io::Error::last_os_error();
                        obs.warning(&format!(
                            "unable to stat {clean_disp} ({})",
                            os_err_str(&err)
                        ));
                        continue;
                    }

                    let file_type = sb.st_mode & mode_bits(libc::S_IFMT);
                    if file_type == mode_bits(libc::S_IFLNK) {
                        obs.warning(&format!("not following symbolic link {clean_disp}"));
                        continue;
                    }

                    let dev = sb.st_dev as u64;
                    let ino = sb.st_ino as u64;

                    if cfg.single_fs {
                        match self.orig_device {
                            None => self.orig_device = Some(dev),
                            Some(orig) if dev != orig => {
                                obs.warning(&format!(
                                    "not recursing into separate filesystem {clean_disp}"
                                ));
                                continue;
                            }
                            Some(_) => {}
                        }
                    }

                    if !cfg.ignore_hardlinks
                        && sb.st_nlink > 1
                        && self.dedup.check_and_insert(dev, ino)
                    {
                        continue;
                    }

                    if file_type == mode_bits(libc::S_IFDIR) {
                        if active_dirs.contains(&(dev, ino)) {
                            obs.warning(&format!("symbolic link loop detected: {clean_disp}"));
                            continue;
                        }
                        if active_dirs.len() >= MAX_CRAWL_DEPTH {
                            return Err(format!(
                                "maximum directory crawl depth reached: {clean_disp}"
                            ));
                        }

                        self.stats.total_dirs = self.stats.total_dirs.saturating_add(1);

                        let children = match read_dir_entries(&c_path, &clean)? {
                            Ok(list) => list,
                            Err(err) => {
                                obs.warning(&format!(
                                    "unable to opendir {clean_disp} ({}), skipping",
                                    os_err_str(&err)
                                ));
                                continue;
                            }
                        };

                        active_dirs.push((dev, ino));
                        stack.push(Frame::ExitDir);
                        // Push children in reverse so stack pops them in the exact readdir order.
                        for child in children.into_iter().rev() {
                            stack.push(Frame::Visit(child));
                        }
                    } else if file_type == mode_bits(libc::S_IFREG)
                        || file_type == mode_bits(libc::S_IFBLK)
                    {
                        if cfg.filters.is_empty() || cfg.filters.matches_basename(&clean) {
                            self.stats.total_files = self.stats.total_files.saturating_add(1);
                            process_file(&clean, cfg, &mut self.stats, obs)?;
                        }
                    } else {
                        obs.warning(&format!("skipping non-regular file: {clean_disp}"));
                    }
                }
            }
        }

        Ok(())
    }
}

impl Default for Crawler {
    fn default() -> Self {
        Self::new()
    }
}

fn strip_single_trailing_slash(path: &Path) -> PathBuf {
    let bytes = path.as_os_str().as_bytes();
    if bytes.len() > 1 && bytes.last() == Some(&b'/') {
        PathBuf::from(OsString::from_vec(bytes[..bytes.len() - 1].to_vec()))
    } else {
        path.to_path_buf()
    }
}

fn read_dir_entries(
    c_path: &CStr,
    dir_path: &Path,
) -> Result<Result<Vec<PathBuf>, io::Error>, String> {
    let mut retries = 0_usize;
    let dirp = loop {
        let ptr = unsafe { libc::opendir(c_path.as_ptr()) };
        if !ptr.is_null() {
            break ptr;
        }
        let err = io::Error::last_os_error();
        let raw = err.raw_os_error();
        if (raw == Some(libc::EMFILE) || raw == Some(libc::ENFILE)) && retries < MAX_OPENDIR_RETRIES
        {
            increment_nofile_rlimit()?;
            retries += 1;
            continue;
        }
        return Ok(Err(err));
    };

    let mut children = Vec::new();
    let dir_bytes = dir_path.as_os_str().as_bytes();

    loop {
        let de = unsafe { libc::readdir(dirp) };
        if de.is_null() {
            break;
        }
        let name = unsafe { CStr::from_ptr((*de).d_name.as_ptr()) }.to_bytes();
        if name == b"." || name == b".." {
            continue;
        }
        let mut full = Vec::with_capacity(dir_bytes.len() + 1 + name.len());
        full.extend_from_slice(dir_bytes);
        full.push(b'/');
        full.extend_from_slice(name);
        children.push(PathBuf::from(OsStr::from_bytes(&full)));
    }

    unsafe {
        libc::closedir(dirp);
    }

    Ok(Ok(children))
}

fn os_err_str(err: &io::Error) -> String {
    match err.raw_os_error() {
        Some(code) => {
            let ptr = unsafe { libc::strerror(code) };
            if ptr.is_null() {
                err.to_string()
            } else {
                unsafe { CStr::from_ptr(ptr) }
                    .to_string_lossy()
                    .into_owned()
            }
        }
        None => err.to_string(),
    }
}

#[cfg(test)]
mod tests {
    use super::super::pages::system_page_size;
    use super::super::stats::{ActionMode, NullObserver, TouchStrategy};
    use super::*;
    use std::fs;
    use std::os::unix::fs::symlink;

    fn temp_dir(name: &str) -> PathBuf {
        let dir =
            std::env::temp_dir().join(format!("preload_walker_test_{name}_{}", std::process::id()));
        let _ = fs::remove_dir_all(&dir);
        fs::create_dir_all(&dir).unwrap();
        dir
    }

    #[test]
    fn walks_temp_tree_and_detects_symlink_loop() {
        let root = temp_dir("loop");
        fs::create_dir_all(root.join("sub")).unwrap();
        fs::write(root.join("a.so"), vec![0_u8; 4096]).unwrap();
        fs::write(root.join("sub/b.so"), vec![0_u8; 8192]).unwrap();
        symlink(&root, root.join("sub/back")).unwrap();

        let mut cfg = PreloadConfig::new(system_page_size());
        cfg.action = ActionMode::Touch;
        cfg.touch_strategy = TouchStrategy::Pread;
        cfg.follow_symlinks = true;
        cfg.ignore_hardlinks = true;

        let mut crawler = Crawler::new();
        let mut obs = NullObserver::default();
        crawler.crawl(&root, &cfg, &mut obs).unwrap();

        assert_eq!(crawler.stats.total_files, 2);
        assert_eq!(crawler.stats.total_dirs, 2);
        assert!(
            obs.warnings
                .iter()
                .any(|w| w.contains("symbolic link loop detected"))
        );

        let _ = fs::remove_dir_all(&root);
    }
}
