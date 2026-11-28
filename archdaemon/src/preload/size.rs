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

//! Size parsing and human-readable formatting matching vmtouch v1.4.1.

/// Parse a size string with optional case-insensitive `k`/`m`/`g` suffix.
///
/// Matches `preloadbin/jni/main.c:237-261` without mutating the input or
/// triggering float-to-int undefined behavior at `i64::MAX`.
pub fn parse_size(inp: &str) -> Result<i64, String> {
    if inp.is_empty() {
        return Err("bad size format".to_string());
    }

    let bytes = inp.as_bytes();
    let last = bytes[bytes.len() - 1];
    let (num_str, mult): (&str, i64) = if last.is_ascii_alphabetic() {
        let lower = last.to_ascii_lowercase();
        let m = match lower {
            b'k' => 1024_i64,
            b'm' => 1024_i64 * 1024,
            b'g' => 1024_i64 * 1024 * 1024,
            _ => return Err(format!("unknown size multiplier: {}", lower as char)),
        };
        (&inp[..inp.len() - 1], m)
    } else {
        (inp, 1_i64)
    };

    // Match C strtod: skip leading ASCII whitespace, reject empty/trailing chars/nan/inf.
    let trimmed = num_str.trim_start_matches(|c: char| c.is_ascii_whitespace());
    if trimmed.is_empty() {
        return Err("bad size format".to_string());
    }
    // Reject trailing whitespace or non-numeric words like "inf"/"nan"
    if trimmed.ends_with(|c: char| c.is_ascii_whitespace()) {
        return Err("bad size format".to_string());
    }

    let val: f64 = trimmed.parse().map_err(|_| "bad size format".to_string())?;
    if !val.is_finite() || val < 0.0 {
        return Err("bad size format".to_string());
    }

    let scaled = val * (mult as f64);
    // `i64::MAX as f64` rounds up to 2^63 (9223372036854775808.0).
    // Reject anything >= 2^63 so the cast to `i64` is always in range.
    if !scaled.is_finite() || scaled >= (i64::MAX as f64) {
        return Err("size too large".to_string());
    }

    Ok(scaled as i64)
}

/// Format a byte count into `N`, `NK`, `NM`, or `NG` matching `main.c:225-235`.
pub fn pretty_print_size(mut inp: i64) -> String {
    if inp < 1024 {
        return format!("{inp}");
    }
    inp /= 1024;
    if inp < 1024 {
        return format!("{inp}K");
    }
    inp /= 1024;
    if inp < 1024 {
        return format!("{inp}M");
    }
    inp /= 1024;
    format!("{inp}G")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_size_valid_cases() {
        assert_eq!(parse_size("0").unwrap(), 0);
        assert_eq!(parse_size("4096").unwrap(), 4096);
        assert_eq!(parse_size("4k").unwrap(), 4096);
        assert_eq!(parse_size("4K").unwrap(), 4096);
        assert_eq!(parse_size("1.5M").unwrap(), 1_572_864);
        assert_eq!(parse_size("1G").unwrap(), 1_073_741_824);
        assert_eq!(parse_size("500M").unwrap(), 500 * 1024 * 1024);
    }

    #[test]
    fn parse_size_rejects_invalid() {
        assert_eq!(parse_size("").unwrap_err(), "bad size format");
        assert_eq!(parse_size("-1").unwrap_err(), "bad size format");
        assert_eq!(parse_size("10X").unwrap_err(), "unknown size multiplier: x");
        assert_eq!(parse_size("k").unwrap_err(), "bad size format");
        assert_eq!(parse_size("inf").unwrap_err(), "unknown size multiplier: f");
        assert_eq!(parse_size("nan").unwrap_err(), "unknown size multiplier: n");
        assert_eq!(
            parse_size("9223372036854775807").unwrap_err(),
            "size too large"
        );
        assert_eq!(parse_size("999999999999G").unwrap_err(), "size too large");
    }

    #[test]
    fn pretty_print_size_cases() {
        assert_eq!(pretty_print_size(0), "0");
        assert_eq!(pretty_print_size(1023), "1023");
        assert_eq!(pretty_print_size(1024), "1K");
        assert_eq!(pretty_print_size(4096), "4K");
        assert_eq!(pretty_print_size(1024 * 1024), "1M");
        assert_eq!(pretty_print_size(500 * 1024 * 1024), "500M");
        assert_eq!(pretty_print_size(2 * 1024 * 1024 * 1024), "2G");
    }
}
