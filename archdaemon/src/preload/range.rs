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

//! `-p <range>` parser matching `preloadbin/jni/main.c:267-285`.

use super::size::parse_size;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct PageRange {
    pub offset: i64,
    pub max_len: i64,
}

/// Parse `<upper>`, `<lower>-`, `-<upper>`, or `<lower>-<upper>`.
///
/// Rounds `lower` down to a multiple of `page_size` before checking ordering
/// against `upper`, preserving exact `vmtouch` v1.4.1 semantics.
pub fn parse_range(inp: &str, page_size: i64) -> Result<PageRange, String> {
    if page_size <= 0 {
        return Err("invalid page size".to_string());
    }

    let mut parts = inp.split('-');
    let first = parts.next().unwrap_or("");
    let second = parts.next();
    if parts.next().is_some() {
        return Err("malformed range: multiple hyphens".to_string());
    }

    let (lower_range, upper_range) = match second {
        None => (0_i64, parse_size(first)?),
        Some(upper_str) => {
            let lower = if first.is_empty() {
                0
            } else {
                parse_size(first)?
            };
            let upper = if upper_str.is_empty() {
                0
            } else {
                parse_size(upper_str)?
            };
            (lower, upper)
        }
    };

    let offset = (lower_range / page_size) * page_size;
    let max_len = if upper_range != 0 {
        if upper_range <= offset {
            return Err("range limits out of order".to_string());
        }
        upper_range - offset
    } else {
        0
    };

    Ok(PageRange { offset, max_len })
}

#[cfg(test)]
mod tests {
    use super::*;

    const PG: i64 = 4096;

    #[test]
    fn range_valid_cases() {
        assert_eq!(
            parse_range("4K", PG).unwrap(),
            PageRange {
                offset: 0,
                max_len: 4096
            }
        );
        assert_eq!(
            parse_range("4K-8K", PG).unwrap(),
            PageRange {
                offset: 4096,
                max_len: 4096
            }
        );
        assert_eq!(
            parse_range("4K-", PG).unwrap(),
            PageRange {
                offset: 4096,
                max_len: 0
            }
        );
        assert_eq!(
            parse_range("-8K", PG).unwrap(),
            PageRange {
                offset: 0,
                max_len: 8192
            }
        );
        // Sub-page lower rounds down to 0, so 10-5 yields offset=0, max_len=5 (matches C).
        assert_eq!(
            parse_range("10-5", PG).unwrap(),
            PageRange {
                offset: 0,
                max_len: 5
            }
        );
    }

    #[test]
    fn range_invalid_cases() {
        assert_eq!(
            parse_range("8K-4K", PG).unwrap_err(),
            "range limits out of order"
        );
        assert_eq!(
            parse_range("4K-4K", PG).unwrap_err(),
            "range limits out of order"
        );
        assert_eq!(
            parse_range("1-2-3", PG).unwrap_err(),
            "malformed range: multiple hyphens"
        );
    }
}
