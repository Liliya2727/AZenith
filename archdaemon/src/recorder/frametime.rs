//! Frametime stats from sysdaemon's `API/current_frametime`.
//!
//! Input contract (one line, rewritten ~once per second by sysdaemon):
//!   `<epoch_ms> <gen> <source> <frames> <sum_us> <hist>`
//! All numbers are CUMULATIVE since the source started. `gen` changes when
//! the source restarts / counters reset. `hist` is sparse `"idx:count,..."`
//! with 100 us buckets (`idx = interval_us / 100`); idx 5000 is the overflow
//! bucket (>= 500 ms); `"-"` means the source has no frame times yet.
//! `source` is `uprobe` | `sf` | `none` (none = no frametime data).
//!
//! archdaemon diffs consecutive snapshots of the same `gen` once per sample
//! tick to get per-second `ft_avg` / `ft_max`, and merges the delta
//! histograms into one bounded session histogram (5001 x u64) to compute
//! `low1` / `low01` at stop.
//!
//! Format helpers (CSV cell rendering / parsing) live here so both
//! sampler and writer share one definition; `Sample` itself stays in
//! `super` to avoid import cycles.

use super::Session;
use std::collections::BTreeMap;

pub const BUCKET_US: u64 = 100;
pub const OVERFLOW_IDX: u32 = 5000;
pub const HIST_LEN: usize = 5001;
pub const STALE_MS: i64 = 3000;
pub const MIN_FRAMES_LOW1: u64 = 100;
pub const MIN_FRAMES_LOW01: u64 = 1000;

/// Missing-value sentinel for `ft_avg_tenths` / `ft_max_tenths`.
pub const FT_MISSING: i64 = -1;
/// `ft_max` sentinel: the worst second hit the >= 500 ms overflow bucket.
pub const FT_OVERFLOW: i64 = -2;

/// One cumulative `current_frametime` snapshot.
#[derive(Debug, Clone, PartialEq)]
pub struct FrametimeSnapshot {
    pub epoch_ms: u64,
    pub gen_id: u64,
    pub source: String,
    pub frames: u64,
    pub sum_us: u64,
    /// Sparse `(bucket idx, cumulative count)` pairs, as published.
    pub hist: Vec<(u32, u64)>,
}

/// Delta for one tick: counts + sum accumulated since the previous
/// snapshot of the same generation.
#[derive(Debug, Clone, PartialEq)]
pub struct Delta {
    pub dt_frames: u64,
    pub dt_sum_us: u64,
    /// Non-zero `(idx, count)` pairs in this interval.
    pub buckets: Vec<(u32, u64)>,
}

impl Delta {
    pub fn hist_total(&self) -> u64 {
        self.buckets.iter().map(|(_, c)| *c).sum()
    }
    pub fn max_idx(&self) -> Option<u32> {
        self.buckets.iter().map(|(i, _)| *i).max()
    }
}

/// Parse the single-line `current_frametime` payload.
///
/// Returns `None` on any unparsable content (caller writes `-1` and adds
/// nothing to the session aggregate). `source == "none"` still parses;
/// the caller treats it as invalid data and discards the row.
pub fn parse_line(line: &str) -> Option<FrametimeSnapshot> {
    let mut it = line.split_whitespace();
    let epoch_ms: u64 = it.next()?.parse().ok()?;
    let gen_id: u64 = it.next()?.parse().ok()?;
    let source = it.next()?.to_string();
    let frames: u64 = it.next()?.parse().ok()?;
    let sum_us: u64 = it.next()?.parse().ok()?;
    let hist_raw = it.next()?;
    // Reject trailing junk rather than silently ignoring half a line.
    if it.next().is_some() {
        return None;
    }
    let hist = if hist_raw == "-" {
        Vec::new()
    } else {
        let mut out = Vec::new();
        for part in hist_raw.split(',') {
            if part.is_empty() {
                return None;
            }
            let mut kv = part.split(':');
            let idx: u32 = kv.next()?.parse().ok()?;
            let count: u64 = kv.next()?.parse().ok()?;
            if kv.next().is_some() {
                return None;
            }
            // Clamp defensive: anything above the overflow bucket is
            // overflow by definition.
            let idx = idx.min(OVERFLOW_IDX);
            out.push((idx, count));
        }
        out
    };
    Some(FrametimeSnapshot { epoch_ms, gen_id, source, frames, sum_us, hist })
}

/// True when the snapshot is too old to trust for this tick.
/// Mirrors the `current_fps` rule: stale if `now - epoch_ms > 3000`
/// (epoch 0 = never published).
pub fn is_stale(epoch_ms: u64, now_ms: i64) -> bool {
    if epoch_ms == 0 {
        return true;
    }
    let diff = now_ms as i128 - epoch_ms as i128;
    diff > STALE_MS as i128
}

/// Diff `cur` against `prev` (same gen only).
///
/// `None` means: different generation, a cumulative counter moved
/// backwards, a histogram count decreased, or no new frames arrived —
/// in every case the caller rebases (the current snapshot becomes the
/// new baseline) and emits no delta for that tick.
pub fn diff(prev: &FrametimeSnapshot, cur: &FrametimeSnapshot) -> Option<Delta> {
    if prev.gen_id != cur.gen_id {
        return None;
    }
    if cur.frames < prev.frames || cur.sum_us < prev.sum_us {
        return None;
    }
    let dt_frames = cur.frames - prev.frames;
    let dt_sum_us = cur.sum_us - prev.sum_us;
    if dt_frames == 0 {
        return None;
    }
    let prev_map: BTreeMap<u32, u64> = prev.hist.iter().copied().collect();
    let cur_map: BTreeMap<u32, u64> = cur.hist.iter().copied().collect();
    // Any bucket missing from `cur`, or smaller than in `prev`, means the
    // counters went backwards despite an unchanged gen — rebase.
    for (idx, p) in &prev_map {
        match cur_map.get(idx) {
            Some(c) if c >= p => {}
            _ => return None,
        }
    }
    let mut buckets = Vec::new();
    for (idx, c) in &cur_map {
        let p = prev_map.get(idx).copied().unwrap_or(0);
        if c < &p {
            return None;
        }
        let d = c - p;
        if d > 0 {
            buckets.push((*idx, d));
        }
    }
    if buckets.is_empty() {
        return None;
    }
    Some(Delta { dt_frames, dt_sum_us, buckets })
}

/// `ft_avg` for a tick in tenths of a ms (-1 when there is no data).
pub fn delta_ft_avg_tenths(d: &Delta) -> i64 {
    if d.dt_frames == 0 {
        return FT_MISSING;
    }
    let avg_ms = (d.dt_sum_us as f64) / (d.dt_frames as f64) / 1000.0;
    (avg_ms * 10.0).round() as i64
}

/// `ft_max` for a tick in tenths of a ms: the upper edge of the highest
/// non-empty bucket, `FT_OVERFLOW` (-2) for the >= 500 ms bucket, or
/// `FT_MISSING` when the delta is empty.
pub fn delta_ft_max_tenths(d: &Delta) -> i64 {
    match d.max_idx() {
        None => FT_MISSING,
        Some(i) if i >= OVERFLOW_IDX => FT_OVERFLOW,
        // Bucket i covers [i*100, (i+1)*100) us; the upper edge in
        // tenths of a ms is exactly i + 1 (e.g. i=83 -> 8.4 ms).
        Some(i) => (i + 1) as i64,
    }
}

/// Bucket midpoint in ms. The overflow bucket reports 500.0 ms as a
/// conservative lower bound (true intervals there are >= 500 ms).
pub fn bucket_midpoint_ms(idx: u32) -> f64 {
    if idx >= OVERFLOW_IDX {
        500.0
    } else {
        (idx as f64 * BUCKET_US as f64 + 50.0) / 1000.0
    }
}

/// Count jank frames in one delta histogram.
///
/// A frame is jank when its interval exceeds twice the refresh period
/// (`2 * 1000 / refresh_hz` ms). Buckets are counted fully when their
/// lower edge (`idx * 100` us) is at or above the threshold — a
/// conservative rule that never counts a bucket that might contain a
/// non-jank frame. Returns 0 when `refresh_hz` is unknown (caller
/// tracks whether jank is known at all for the session).
pub fn count_jank(buckets: &[(u32, u64)], refresh_hz: i64) -> u64 {
    if refresh_hz <= 0 {
        return 0;
    }
    let threshold_us = 2_000_000u64 / (refresh_hz as u64);
    buckets
        .iter()
        .filter(|(idx, _)| (*idx as u64) * BUCKET_US >= threshold_us)
        .map(|(_, c)| *c)
        .sum()
}

/// Session-level summary computed at stop from the merged histogram.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct FrameSummary {
    pub low1: Option<f64>,
    pub low01: Option<f64>,
    /// `None` when the session has no valid frametime data (or no
    /// tick ever had a usable refresh rate to judge jank against).
    pub jank: Option<u64>,
    pub total_frames: u64,
}

/// fps of the worst `frac` of frames by count (low1: 0.01, low01: 0.001).
///
/// The worst `ceil(total * frac)` frames are averaged by their bucket
/// midpoints; the cutoff bucket contributes only the frames still
/// needed (partial-bucket rule), so the mean is exact for the
/// histogram resolution. `None` when `total < min_frames` — never a
/// fabricated value.
pub fn low_fps(hist: &[u64], total: u64, frac: f64, min_frames: u64) -> Option<f64> {
    if total < min_frames {
        return None;
    }
    let need = (total as f64 * frac).ceil() as u64;
    if need == 0 {
        return None;
    }
    let mut remaining = need;
    let mut weighted_ms = 0.0;
    for i in (0..hist.len()).rev() {
        let count = hist[i];
        if count == 0 {
            continue;
        }
        let take = count.min(remaining);
        weighted_ms += take as f64 * bucket_midpoint_ms(i as u32);
        remaining -= take;
        if remaining == 0 {
            break;
        }
    }
    if remaining != 0 {
        return None;
    }
    let mean_ms = weighted_ms / need as f64;
    if mean_ms <= 0.0 {
        return None;
    }
    Some(1000.0 / mean_ms)
}

pub fn summarize(hist: &[u64], total: u64, jank: u64, has: bool, jank_known: bool) -> FrameSummary {
    if !has || total == 0 {
        return FrameSummary { low1: None, low01: None, jank: None, total_frames: total };
    }
    FrameSummary {
        low1: low_fps(hist, total, 0.01, MIN_FRAMES_LOW1),
        low01: low_fps(hist, total, 0.001, MIN_FRAMES_LOW01),
        jank: if jank_known { Some(jank) } else { None },
        total_frames: total,
    }
}

/// Read `current_frametime` once and advance the session state.
///
/// Returns the `(ft_avg_tenths, ft_max_tenths)` pair for the CSV row:
/// real values only for a valid same-gen delta, `-1`/`-1` for missing,
/// stale, unparsable, `source == "none"`, gen-reset, zero-frame, or
/// backward-counter ticks — and nothing is added to the session
/// aggregate in those cases (a rebase tick only moves the baseline).
/// `refresh_hz` is this tick's display rate, used for jank.
pub fn update_session(sess: &mut Session, refresh_hz: i64, now_ms: i64) -> (i64, i64) {
    let raw = std::fs::read_to_string(super::CURRENT_FRAMETIME_PATH).ok();
    let line = match raw.as_deref().and_then(|s| s.lines().next()) {
        Some(l) => l.trim().to_string(),
        None => return (FT_MISSING, FT_MISSING),
    };
    if line.is_empty() {
        return (FT_MISSING, FT_MISSING);
    }
    let cur = match parse_line(&line) {
        Some(c) => c,
        None => return (FT_MISSING, FT_MISSING),
    };
    if cur.source == "none" || is_stale(cur.epoch_ms, now_ms) {
        return (FT_MISSING, FT_MISSING);
    }
    let prev = match sess.prev_frametime.clone() {
        Some(p) => p,
        None => {
            sess.prev_frametime = Some(cur);
            return (FT_MISSING, FT_MISSING);
        }
    };
    if prev.gen_id != cur.gen_id {
        sess.prev_frametime = Some(cur);
        return (FT_MISSING, FT_MISSING);
    }
    let delta = match diff(&prev, &cur) {
        Some(d) => d,
        None => {
            sess.prev_frametime = Some(cur);
            return (FT_MISSING, FT_MISSING);
        }
    };
    let ft_avg = delta_ft_avg_tenths(&delta);
    let ft_max = delta_ft_max_tenths(&delta);
    for (idx, c) in &delta.buckets {
        let i = (*idx as usize).min(HIST_LEN - 1);
        sess.session_hist[i] += *c;
    }
    let hist_total = delta.hist_total();
    sess.session_frames += hist_total;
    if refresh_hz > 0 {
        sess.session_jank += count_jank(&delta.buckets, refresh_hz);
        sess.session_jank_known = true;
    }
    sess.has_frametime = true;
    sess.prev_frametime = Some(cur);
    (ft_avg, ft_max)
}

/// CSV cell for an `ft_avg` value in tenths of a ms.
pub fn ft_avg_csv(tenths: i64) -> String {
    if tenths < 0 {
        "-1".to_string()
    } else {
        format!("{}.{}", tenths / 10, tenths.abs() % 10)
    }
}

/// CSV cell for an `ft_max` value. The overflow bucket (>= 500 ms) is
/// rendered as `>=500.0` so it stays distinguishable from a true
/// 500.0 ms edge (bucket 4999).
pub fn ft_max_csv(tenths: i64) -> String {
    if tenths == FT_OVERFLOW {
        ">=500.0".to_string()
    } else if tenths < 0 {
        "-1".to_string()
    } else {
        format!("{}.{}", tenths / 10, tenths.abs() % 10)
    }
}

/// JSON fragment for a `low1` / `low01` value (one decimal, or `null`).
pub fn low_json(v: Option<f64>) -> String {
    match v {
        Some(x) => format!("{:.1}", x),
        None => "null".to_string(),
    }
}

/// JSON fragment for `jank` (integer count, or `null`).
pub fn jank_json(v: Option<u64>) -> String {
    match v {
        Some(x) => x.to_string(),
        None => "null".to_string(),
    }
}

// ---------------------------------------------------------------------------
// Unit tests (fixed inputs, no filesystem beyond the units under test).
// ---------------------------------------------------------------------------
#[cfg(test)]
mod tests {
    use super::*;

    fn snap(epoch_ms: u64, gen_id: u64, source: &str, frames: u64, sum_us: u64, hist: Vec<(u32, u64)>) -> FrametimeSnapshot {
        FrametimeSnapshot { epoch_ms, gen_id, source: source.to_string(), frames, sum_us, hist }
    }

    fn hist_of(pairs: &[(u32, u64)]) -> Vec<u64> {
        let mut h = vec![0u64; HIST_LEN];
        for (i, c) in pairs {
            h[*i as usize] += *c;
        }
        h
    }

    #[test]
    fn parse_contract() {
        let s = parse_line("1700000000000 3 uprobe 12345 206000000 166:10000,167:2000,5000:5").unwrap();
        assert_eq!(s.epoch_ms, 1_700_000_000_000);
        assert_eq!(s.gen_id, 3);
        assert_eq!(s.frames, 12345);
        assert_eq!(s.sum_us, 206_000_000);
        assert_eq!(s.hist, vec![(166, 10000), (167, 2000), (5000, 5)]);
        let none = parse_line("1700000000000 1 none 0 0 -").unwrap();
        assert_eq!(none.source, "none");
        assert!(none.hist.is_empty());
        assert!(parse_line("garbage").is_none());
        assert!(parse_line("1700000000000 1 uprobe 10 100000").is_none()); // hist missing
        assert!(parse_line("1700000000000 1 uprobe 10 100000 10:1 trailing").is_none());
        assert!(parse_line("1700000000000 1 uprobe 10 100000 10:1,broken").is_none());
    }

    #[test]
    fn stale_boundary() {
        let now = 1_700_000_010_000i64;
        assert!(!is_stale(1_700_000_009_000, now)); // 1s old
        assert!(!is_stale(1_700_000_007_000, now)); // exactly 3s old -> fresh
        assert!(is_stale(1_700_000_006_999, now)); // just over 3s -> stale
        assert!(is_stale(0, now));
    }

    #[test]
    fn diff_basic_and_ft_values() {
        let prev = snap(1, 7, "uprobe", 100, 1_700_000, vec![(166, 100)]);
        let cur = snap(2, 7, "uprobe", 160, 1_700_000 + 50 * 16_700 + 10 * 33_400, vec![(166, 150), (334, 10)]);
        let d = diff(&prev, &cur).unwrap();
        assert_eq!(d.dt_frames, 60);
        assert_eq!(d.dt_sum_us, 50 * 16_700 + 10 * 33_400);
        assert_eq!(d.buckets, vec![(166, 50), (334, 10)]);
        // avg = (835000 + 334000)/60 us -> 19.4833 ms -> 194 or 195 tenths
        let avg = delta_ft_avg_tenths(&d);
        assert!((194 - avg).abs() <= 1, "avg tenths {}", avg);
        // highest bucket 334 -> upper edge 33.5 ms -> 335 tenths
        assert_eq!(delta_ft_max_tenths(&d), 335);
        assert_eq!(d.max_idx(), Some(334));
        assert_eq!(d.hist_total(), 60);
    }

    #[test]
    fn diff_gen_reset_rebases() {
        let prev = snap(1, 1, "uprobe", 100, 1_700_000, vec![(166, 100)]);
        let cur = snap(2, 2, "uprobe", 10, 167_000, vec![(166, 10)]);
        assert!(diff(&prev, &cur).is_none());
    }

    #[test]
    fn diff_backward_counter_rebases() {
        let prev = snap(1, 1, "uprobe", 100, 1_700_000, vec![(166, 80), (167, 20)]);
        // frame counter went backwards without a gen bump
        let cur = snap(2, 1, "uprobe", 50, 800_000, vec![(166, 50)]);
        assert!(diff(&prev, &cur).is_none());
        // bucket count decreased
        let cur2 = snap(2, 1, "uprobe", 110, 1_900_000, vec![(166, 70), (167, 30)]);
        assert!(diff(&prev, &cur2).is_none());
        // zero new frames -> no delta
        let same = snap(2, 1, "uprobe", 100, 1_700_000, vec![(166, 80), (167, 20)]);
        assert!(diff(&prev, &same).is_none());
    }

    #[test]
    fn stutter_spikes_max_and_low() {
        // 990 frames at ~16.7 ms (idx 166), 10 frames at ~200 ms (idx 2000)
        let h = hist_of(&[(166, 990), (2000, 10)]);
        let s = summarize(&h, 1000, 10, true, true);
        // worst 1% = 10 frames, all at 200 ms midpoint (200.05 ms) -> ~5.0 fps
        let low1 = s.low1.unwrap();
        assert!((low1 - 1000.0 / 200.05).abs() < 0.15, "low1 {}", low1);
        assert!(low1 < 6.0, "low1 must expose the stutter, got {}", low1);
        // low01: total 1000 exactly meets the threshold
        assert!(s.low01.is_some());
        // per-tick ft_max for a spike second reports the spike bucket edge
        let d = Delta { dt_frames: 10, dt_sum_us: 10 * 200_000, buckets: vec![(2000, 10)] };
        assert_eq!(delta_ft_max_tenths(&d), 2001); // 200.1 ms
        // overflow bucket renders separately
        let ov = Delta { dt_frames: 1, dt_sum_us: 600_000, buckets: vec![(OVERFLOW_IDX, 1)] };
        assert_eq!(delta_ft_max_tenths(&ov), FT_OVERFLOW);
        assert_eq!(ft_max_csv(FT_OVERFLOW), ">=500.0");
    }

    #[test]
    fn all_equal_intervals() {
        let h = hist_of(&[(166, 2000)]);
        let s = summarize(&h, 2000, 0, true, true);
        // every frame 16.65 ms midpoint -> 60.06 fps for both lows
        assert!((s.low1.unwrap() - 1000.0 / 16.65).abs() < 0.1);
        assert!((s.low01.unwrap() - 1000.0 / 16.65).abs() < 0.1);
        assert_eq!(s.jank, Some(0)); // known refresh, nothing janked
        // 16.7 ms is not jank at 120 Hz (threshold 16.67 ms) nor at 60 Hz
        // (threshold 33.33 ms); 33.4 ms+ is jank at 60 Hz.
        assert_eq!(count_jank(&[(166, 50)], 60), 0);
        assert_eq!(count_jank(&[(166, 50)], 120), 0);
        assert_eq!(count_jank(&[(334, 50)], 60), 50);
        assert_eq!(count_jank(&[(334, 50)], 120), 50);
        assert_eq!(count_jank(&[(166, 50)], 0), 0);
    }

    #[test]
    fn too_short_session_nulls() {
        // 99 frames -> neither low qualifies
        let h = hist_of(&[(166, 99)]);
        let s = summarize(&h, 99, 0, true, true);
        assert_eq!(s.low1, None);
        assert_eq!(s.low01, None);
        // 150 frames -> low1 only
        let h2 = hist_of(&[(166, 150)]);
        let s2 = summarize(&h2, 150, 0, true, true);
        assert!(s2.low1.is_some());
        assert_eq!(s2.low01, None);
        // no valid data at all -> everything null (incl. jank)
        let empty = vec![0u64; HIST_LEN];
        let s3 = summarize(&empty, 0, 0, false, false);
        assert_eq!(s3.low1, None);
        assert_eq!(s3.low01, None);
        assert_eq!(s3.jank, None);
        let s4 = summarize(&empty, 0, 0, true, false);
        assert_eq!(s4.jank, None);
    }

    #[test]
    fn partial_bucket_cutoff() {
        // 1000 frames: 20 at 50 ms (idx 500), 980 at 10 ms (idx 100).
        // worst 1% = 10 frames, split across the 50 ms bucket only -> 20 fps.
        let h = hist_of(&[(100, 980), (500, 20)]);
        let low1 = low_fps(&h, 1000, 0.01, MIN_FRAMES_LOW1).unwrap();
        assert!((low1 - 1000.0 / 50.05).abs() < 0.1, "low1 {}", low1);
        // Now: worst 1% straddles two buckets. 5 at 50 ms + 5 at 20 ms.
        // mean = (5*50.05 + 5*20.05)/10 = 35.05 ms.
        let h2 = hist_of(&[(100, 990), (200, 5), (500, 5)]);
        let low1b = low_fps(&h2, 1000, 0.01, MIN_FRAMES_LOW1).unwrap();
        assert!((low1b - 1000.0 / 35.05).abs() < 0.2, "low1b {}", low1b);
        // Tie across the cutoff bucket: worst 1% is 10 frames but only 6
        // worst-bucket frames exist; the other 4 come from the next bucket.
        let h3 = hist_of(&[(100, 994), (500, 6)]);
        let low1c = low_fps(&h3, 1000, 0.01, MIN_FRAMES_LOW1).unwrap();
        let expect = (6.0 * 50.05 + 4.0 * 10.05) / 10.0;
        assert!((low1c - 1000.0 / expect).abs() < 0.2, "low1c {}", low1c);
    }

    #[test]
    fn gen_reset_mid_session_merges_only_valid_deltas() {
        // Simulate two generations: deltas after a rebase merge correctly.
        let mut hist = vec![0u64; HIST_LEN];
        let mut total = 0u64;
        // gen 1: baseline -> 100 frames at idx 166
        let p1 = snap(1, 1, "sf", 0, 0, vec![]);
        let c1 = snap(2, 1, "sf", 100, 1_670_000, vec![(166, 100)]);
        let d1 = diff(&p1, &c1).unwrap();
        for (i, c) in &d1.buckets { hist[*i as usize] += *c; }
        total += d1.hist_total();
        // gen 2 rebase: first snapshot is a baseline, no delta
        let p2 = snap(3, 2, "sf", 500, 9_000_000, vec![(166, 500)]);
        let c2 = snap(4, 2, "sf", 560, 9_000_000 + 60 * 16_700, vec![(166, 560)]);
        assert!(diff(&c1, &p2).is_none()); // gen differs
        let d2 = diff(&p2, &c2).unwrap();
        for (i, c) in &d2.buckets { hist[*i as usize] += *c; }
        total += d2.hist_total();
        assert_eq!(total, 160);
        assert_eq!(hist[166], 160);
        let s = summarize(&hist, total, 0, true, true);
        assert!(s.low1.is_some());
        assert_eq!(s.low01, None); // 160 < 1000
    }

    #[test]
    fn invalid_ticks_source_none_stale_unparsable() {
        // source "none": parses but must be treated as invalid (-1, nothing merged).
        let n = parse_line("1700000010000 9 none 0 0 -").unwrap();
        assert_eq!(n.source, "none");
        assert!(n.source == "none" || is_stale(n.epoch_ms, 1_700_000_010_000));
        // stale: epoch > 3s before now.
        let old = parse_line("1700000000000 9 uprobe 10 167000 166:10").unwrap();
        assert!(is_stale(old.epoch_ms, 1_700_000_010_000));
        // unparsable / missing hist -> None (caller writes -1).
        assert!(parse_line("").is_none());
        assert!(parse_line("not a line").is_none());
        assert!(parse_line("1700000010000 9 uprobe 10").is_none());
    }

    #[test]
    fn csv_render() {
        assert_eq!(ft_avg_csv(-1), "-1");
        assert_eq!(ft_avg_csv(167), "16.7");
        assert_eq!(ft_avg_csv(200), "20.0");
        assert_eq!(ft_avg_csv(0), "0.0");
        assert_eq!(ft_max_csv(-1), "-1");
        assert_eq!(ft_max_csv(335), "33.5");
        assert_eq!(low_json(Some(53.24)), "53.2");
        assert_eq!(low_json(None), "null");
        assert_eq!(jank_json(Some(7)), "7");
        assert_eq!(jank_json(None), "null");
    }
}
