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

//! Hardlink deduplication by `(st_dev, st_ino)`.

use std::collections::HashSet;

#[derive(Debug, Default)]
pub struct InodeDedup {
    seen: HashSet<(u64, u64)>,
}

impl InodeDedup {
    /// Returns `true` if `(dev, ino)` was already seen; otherwise records it and returns `false`.
    pub fn check_and_insert(&mut self, dev: u64, ino: u64) -> bool {
        !self.seen.insert((dev, ino))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn deduplicates_by_dev_and_ino() {
        let mut d = InodeDedup::default();
        assert!(!d.check_and_insert(1, 100));
        assert!(d.check_and_insert(1, 100));
        assert!(!d.check_and_insert(2, 100));
        assert!(!d.check_and_insert(1, 101));
    }
}
