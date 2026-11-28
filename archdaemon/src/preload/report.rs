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

//! Human summary, `-o kv` summary, and verbose ASCII residency chart formatting.

use super::size::pretty_print_size;
use super::stats::{ActionMode, Stats};

pub const RESIDENCY_CHART_WIDTH: usize = 60;

/// Format a C-style `%.*g` floating-point number without trailing zeros.
pub fn format_g(val: f64, sig_figs: usize) -> String {
    if !val.is_finite() {
        return format!("{val}");
    }
    if val == 0.0 {
        return "0".to_string();
    }
    let abs = val.abs();
    let exp = abs.log10().floor() as i32;
    let sig = sig_figs.max(1);
    if exp < -4 || exp >= sig as i32 {
        let prec = sig.saturating_sub(1);
        let raw = format!("{val:.prec$e}");
        if let Some((mantissa, exp_part)) = raw.split_once('e') {
            let trimmed = trim_decimal_zeros(mantissa);
            let exp_num: i32 = exp_part.parse().unwrap_or(0);
            if exp_num >= 0 {
                format!("{trimmed}e+{exp_num:02}")
            } else {
                format!("{trimmed}e-{:02}", exp_num.unsigned_abs())
            }
        } else {
            raw
        }
    } else {
        let decimals = ((sig as i32) - 1 - exp).max(0) as usize;
        let raw = format!("{val:.decimals$}");
        trim_decimal_zeros(&raw).to_string()
    }
}

fn trim_decimal_zeros(s: &str) -> &str {
    if s.contains('.') {
        s.trim_end_matches('0').trim_end_matches('.')
    } else {
        s
    }
}

/// Format the `\r[O o ] X/Y` residency chart matching `preloadbin/jni/main.c:329-350`.
pub fn format_residency_chart(mincore: &[u8]) -> String {
    let pages_in_file = mincore.len();
    let pages_per_char = if pages_in_file <= RESIDENCY_CHART_WIDTH {
        1
    } else {
        (pages_in_file / RESIDENCY_CHART_WIDTH) + 1
    };

    let mut out = String::with_capacity(RESIDENCY_CHART_WIDTH + 32);
    out.push_str("\r[");

    let mut pages_in_core: usize = 0;
    let mut curr: usize = 0;
    let mut j: usize = 0;

    for &byte in mincore {
        if (byte & 0x1) != 0 {
            curr += 1;
            pages_in_core += 1;
        }
        j += 1;
        if j == pages_per_char {
            let ch = if curr == pages_per_char {
                'O'
            } else if curr == 0 {
                ' '
            } else {
                'o'
            };
            out.push(ch);
            j = 0;
            curr = 0;
        }
    }
    if j > 0 {
        let ch = if curr == j {
            'O'
        } else if curr == 0 {
            ' '
        } else {
            'o'
        };
        out.push(ch);
    }

    out.push_str(&format!("] {pages_in_core}/{pages_in_file}"));
    out
}

/// Format the human-readable summary matching `preloadbin/jni/main.c:773-786`.
pub fn format_human_summary(
    stats: &Stats,
    action: ActionMode,
    verbose: u32,
    page_size: i64,
    elapsed: f64,
) -> String {
    let total_in_core_size = stats.total_pages_in_core.saturating_mul(page_size);
    let total_size = stats.total_pages.saturating_mul(page_size);
    let perc = if stats.total_pages > 0 {
        100.0 * (stats.total_pages_in_core as f64) / (stats.total_pages as f64)
    } else {
        0.0
    };

    let mut out = String::new();
    if verbose > 0 {
        out.push('\n');
    }
    out.push_str(&format!("           Files: {}\n", stats.total_files));
    out.push_str(&format!("     Directories: {}\n", stats.total_dirs));
    match action {
        ActionMode::Touch => {
            out.push_str(&format!(
                "   Touched Pages: {} ({})\n",
                stats.total_pages,
                pretty_print_size(total_size)
            ));
        }
        ActionMode::Evict => {
            out.push_str(&format!(
                "   Evicted Pages: {} ({})\n",
                stats.total_pages,
                pretty_print_size(total_size)
            ));
        }
        ActionMode::Query => {
            out.push_str(&format!(
                "  Resident Pages: {}/{}  {}/{}  {}%\n",
                stats.total_pages_in_core,
                stats.total_pages,
                pretty_print_size(total_in_core_size),
                pretty_print_size(total_size),
                format_g(perc, 3),
            ));
        }
    }
    out.push_str(&format!(
        "         Elapsed: {} seconds\n",
        format_g(elapsed, 5)
    ));
    out
}

/// Format the `-o kv` machine-readable summary.
///
/// Intentional deviation (Inventory §6 item 9): for `Touch` and `Evict`,
/// reports `stats.total_pages` and `total_size` instead of C's buggy
/// `total_pages_in_core` (which was pre-touch count for `-t` and `0` for `-e`).
pub fn format_kv_summary(
    stats: &Stats,
    action: ActionMode,
    page_size: i64,
    elapsed: f64,
) -> String {
    let total_size = stats.total_pages.saturating_mul(page_size);
    let (desc, action_pages, action_size, perc) = match action {
        ActionMode::Touch => {
            let p = if stats.total_pages > 0 { 100.0 } else { 0.0 };
            ("Touched", stats.total_pages, total_size, p)
        }
        ActionMode::Evict => {
            let p = if stats.total_pages > 0 { 100.0 } else { 0.0 };
            ("Evicted", stats.total_pages, total_size, p)
        }
        ActionMode::Query => {
            let in_core_size = stats.total_pages_in_core.saturating_mul(page_size);
            let p = if stats.total_pages > 0 {
                100.0 * (stats.total_pages_in_core as f64) / (stats.total_pages as f64)
            } else {
                0.0
            };
            ("Resident", stats.total_pages_in_core, in_core_size, p)
        }
    };

    format!(
        "Files={} Directories={} {}Pages={} TotalPages={} {}Size={} TotalSize={} {}Percent={} Elapsed={}\n",
        stats.total_files,
        stats.total_dirs,
        desc,
        action_pages,
        stats.total_pages,
        desc,
        action_size,
        total_size,
        desc,
        format_g(perc, 3),
        format_g(elapsed, 5),
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn chart_rendering() {
        assert_eq!(format_residency_chart(&[1, 0, 1]), "\r[O O] 2/3");
        assert_eq!(format_residency_chart(&[1, 1, 1, 1]), "\r[OOOO] 4/4");
    }

    #[test]
    fn human_touch_format_preserves_daemon_spacing() {
        let s = Stats {
            total_pages: 4,
            total_pages_in_core: 2,
            total_files: 1,
            total_dirs: 0,
        };
        let out = format_human_summary(&s, ActionMode::Touch, 0, 4096, 0.00123);
        assert!(out.contains("           Files: 1\n"));
        assert!(out.contains("     Directories: 0\n"));
        assert!(out.contains("   Touched Pages: 4 (16K)\n"));
        assert!(out.contains("         Elapsed: 0.00123 seconds\n"));
    }

    #[test]
    fn kv_touch_and_evict_report_actual_totals() {
        let s = Stats {
            total_pages: 2,
            total_pages_in_core: 0,
            total_files: 1,
            total_dirs: 0,
        };
        let touch = format_kv_summary(&s, ActionMode::Touch, 4096, 0.01);
        assert!(touch.contains(
            "TouchedPages=2 TotalPages=2 TouchedSize=8192 TotalSize=8192 TouchedPercent=100"
        ));
        let evict = format_kv_summary(&s, ActionMode::Evict, 4096, 0.01);
        assert!(evict.contains(
            "EvictedPages=2 TotalPages=2 EvictedSize=8192 TotalSize=8192 EvictedPercent=100"
        ));
    }
}
