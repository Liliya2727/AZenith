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

//! Configuration, statistics, and observer traits for the preload engine.

use std::path::Path;

use super::filters::FilterList;

/// How `-t` touches file pages into the OS page cache.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum TouchStrategy {
    /// `mmap(PROT_READ, MAP_SHARED)` + 1-byte-per-page volatile read.
    /// Exact parity with C `vmtouch` (note: prone to `SIGBUS` if the file is
    /// truncated concurrently by another process).
    #[default]
    Mmap,
    /// `pread(2)` in fixed-size chunks. Brings the same pages into page cache
    /// without `SIGBUS` risk or virtual-address pressure (recommended inside
    /// long-running daemons).
    Pread,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ActionMode {
    #[default]
    Query,
    Touch,
    Evict,
}

#[derive(Debug, Clone)]
pub struct PreloadConfig {
    pub action: ActionMode,
    pub touch_strategy: TouchStrategy,
    pub verbose: u32,
    pub quiet: bool,
    pub follow_symlinks: bool,
    pub single_fs: bool,
    pub ignore_hardlinks: bool,
    pub lock: bool,
    pub lockall: bool,
    pub max_file_size: u64,
    pub offset: i64,
    pub max_len: i64,
    pub ignores: FilterList,
    pub filters: FilterList,
    pub page_size: i64,
}

impl PreloadConfig {
    pub fn new(page_size: i64) -> Self {
        Self {
            action: ActionMode::Query,
            touch_strategy: TouchStrategy::Mmap,
            verbose: 0,
            quiet: false,
            follow_symlinks: false,
            single_fs: false,
            ignore_hardlinks: false,
            lock: false,
            lockall: false,
            max_file_size: u64::MAX,
            offset: 0,
            max_len: 0,
            ignores: FilterList::default(),
            filters: FilterList::default(),
            page_size,
        }
    }
}

#[derive(Debug, Default, Clone, Copy, PartialEq, Eq)]
pub struct Stats {
    pub total_pages: i64,
    pub total_pages_in_core: i64,
    pub total_files: i64,
    pub total_dirs: i64,
}

/// Callback sink for warnings, verbose progress charts, and per-file events.
pub trait PreloadObserver {
    fn warning(&mut self, msg: &str);
    fn on_evict_file(&mut self, _path: &Path) {}
    fn on_file_start(&mut self, _path: &Path) {}
    fn on_chart_update(&mut self, _mincore: &[u8]) {}
    fn on_file_done(&mut self, _path: &Path, _mincore: &[u8]) {}
}

/// No-op observer for silent/unit-test runs.
#[cfg(test)]
#[derive(Debug, Default)]
pub struct NullObserver {
    pub warnings: Vec<String>,
}

#[cfg(test)]
impl PreloadObserver for NullObserver {
    fn warning(&mut self, msg: &str) {
        self.warnings.push(msg.to_string());
    }
}
