//! CSV/JSON writers + summary computation.

use super::{Sample, Session, StopReason};
use std::path::Path;

/// Split "<pkg>_<yyyymmdd>_<hhmmss>.csv" into (pkg, start-epoch) by parsing the
/// trailing timestamp field (local time → epoch via mktime).
pub fn parse_session_name(csv_name: &str) -> (String, u64) {
    let stem = csv_name.strip_suffix(".csv").unwrap_or(csv_name);
    let parts: Vec<&str> = stem.rsplitn(3, '_').collect();
    if parts.len() == 3 {
        // parts[0] = time, parts[1] = date, parts[2] = rest of pkg
        let (date, time) = (parts[1], parts[0]);
        if date.len() == 8 && time.len() == 6 && date.chars().all(|c| c.is_ascii_digit()) && time.chars().all(|c| c.is_ascii_digit()) {
            let mut tm = unsafe { std::mem::zeroed::<libc::tm>() };
            tm.tm_year = date[0..4].parse::<i32>().unwrap_or(1970) - 1900;
            tm.tm_mon = date[4..6].parse::<i32>().unwrap_or(1) - 1;
            tm.tm_mday = date[6..8].parse::<i32>().unwrap_or(1);
            tm.tm_hour = time[0..2].parse::<i32>().unwrap_or(0);
            tm.tm_min = time[2..4].parse::<i32>().unwrap_or(0);
            tm.tm_sec = time[4..6].parse::<i32>().unwrap_or(0);
            let epoch = unsafe { libc::mktime(&mut tm as *mut _) };
            return (parts[2].to_string(), if epoch > 0 { epoch as u64 } else { 0 });
        }
    }
    (stem.to_string(), 0)
}

pub fn write_json_atomic(path: &Path, content: &str) -> std::io::Result<()> {
    let tmp = path.with_extension("json.tmp");
    std::fs::write(&tmp, content)?;
    std::fs::rename(&tmp, path)
}

pub fn build_summary_json(sess: &Session, reason: StopReason, incomplete: bool) -> String {
    let st = stats(&sess.samples);
    format!(
        "{{\n  \"version\": 2,\n  \"pkg\": {},\n  \"profile\": {},\n  \"start\": {},\n  \"duration\": {},\n  \"stop_reason\": {},\n  \"incomplete\": {},\n  \"fps\": {{\"avg\": {}, \"min\": {}, \"low1\": null, \"low01\": null}},\n  \"fps_active\": {{\"avg\": {}, \"min\": {}}},\n  \"temp\": {{\"cpu_max\": {}, \"gpu_max\": {}, \"bat_max\": {}}},\n  \"power\": {{\"avg_mw\": {}}},\n  \"jank\": null,\n  \"units\": {{\"temp\": \"C\", \"freq\": \"kHz\", \"power\": \"mW\"}}\n}}",
        json_str(&sess.pkg),
        json_str(&sess.profile),
        sess.start_epoch,
        sess.began_at.elapsed().as_secs(),
        json_str(reason.as_str()),
        incomplete,
        st.fps_avg,
        st.fps_min,
        st.fps_active_avg,
        st.fps_active_min,
        temp_json(st.cpu_max),
        temp_json(st.gpu_max),
        temp_json(st.bat_max),
        st.avg_mw
    )
}

pub fn build_summary_json_rows(csv: &str, pkg: &str, profile: &str, start_epoch: u64, duration: u64, reason: StopReason, incomplete: bool) -> String {
    let samples = parse_csv_rows(csv);
    let st = stats(&samples);
    format!(
        "{{\n  \"version\": 2,\n  \"pkg\": {},\n  \"profile\": {},\n  \"start\": {},\n  \"duration\": {},\n  \"stop_reason\": {},\n  \"incomplete\": {},\n  \"fps\": {{\"avg\": {}, \"min\": {}, \"low1\": null, \"low01\": null}},\n  \"fps_active\": {{\"avg\": {}, \"min\": {}}},\n  \"temp\": {{\"cpu_max\": {}, \"gpu_max\": {}, \"bat_max\": {}}},\n  \"power\": {{\"avg_mw\": {}}},\n  \"jank\": null,\n  \"units\": {{\"temp\": \"C\", \"freq\": \"kHz\", \"power\": \"mW\"}}\n}}",
        json_str(pkg),
        json_str(profile),
        start_epoch,
        duration,
        json_str(reason.as_str()),
        incomplete,
        st.fps_avg,
        st.fps_min,
        st.fps_active_avg,
        st.fps_active_min,
        temp_json(st.cpu_max),
        temp_json(st.gpu_max),
        temp_json(st.bat_max),
        st.avg_mw
    )
}

/// temps are tenths of °C; -1 is the missing sentinel (kept numeric).
fn temp_json(tenths: i64) -> String {
    if tenths < 0 {
        "-1".to_string()
    } else {
        format!("{}.{}", tenths / 10, tenths % 10)
    }
}

pub struct SummaryStats {
    pub fps_avg: i64,
    pub fps_min: i64,
    /// avg/min over samples with fps > 0 (excludes warmup/loading zeros).
    pub fps_active_avg: i64,
    pub fps_active_min: i64,
    pub cpu_max: i64,
    pub gpu_max: i64,
    pub bat_max: i64,
    pub avg_mw: i64,
}

fn fps_avg_min(values: &[i64]) -> (i64, i64) {
    if values.is_empty() {
        return (-1, -1);
    }
    let min = values.iter().min().copied().unwrap_or(-1);
    let avg = (values.iter().sum::<i64>() as f64 / values.len() as f64).round() as i64;
    (avg, min)
}

pub fn stats(samples: &[Sample]) -> SummaryStats {
    let all: Vec<i64> = samples.iter().map(|s| s.fps).filter(|v| *v >= 0).collect();
    let (fps_avg, fps_min) = fps_avg_min(&all);
    let active: Vec<i64> = samples.iter().map(|s| s.fps).filter(|v| *v > 0).collect();
    let (fps_active_avg, fps_active_min) = fps_avg_min(&active);
    let cpu_max = samples.iter().map(|s| s.cpu_temp).filter(|v| *v != -1).max().unwrap_or(-1);
    let gpu_max = samples.iter().map(|s| s.gpu_temp).filter(|v| *v != -1).max().unwrap_or(-1);
    let bat_max = samples.iter().map(|s| s.bat_temp).filter(|v| *v != -1).max().unwrap_or(-1);
    let power: Vec<i64> = samples
        .iter()
        .filter(|s| s.ma != -1 && s.mv != -1)
        .map(|s| s.ma * s.mv / 1000)
        .collect();
    let avg_mw = if power.is_empty() {
        -1
    } else {
        (power.iter().sum::<i64>() as f64 / power.len() as f64).round() as i64
    };
    SummaryStats {
        fps_avg,
        fps_min,
        fps_active_avg,
        fps_active_min,
        cpu_max,
        gpu_max,
        bat_max,
        avg_mw,
    }
}

/// Parse a temperature cell back to tenths of °C. Accepts both the new
/// decimal format ("103.2") and legacy milli-°C integers ("103204").
fn parse_temp(s: &str) -> i64 {
    let s = s.trim();
    if s == "-1" {
        return -1;
    }
    if s.contains('.') {
        s.parse::<f64>().map(|v| (v * 10.0).round() as i64).unwrap_or(-1)
    } else if let Ok(v) = s.parse::<i64>() {
        if v > 20000 {
            v / 100 // legacy milli-degree integers
        } else {
            v
        }
    } else {
        -1
    }
}

pub fn parse_csv_rows(content: &str) -> Vec<Sample> {
    let mut it = content.lines();
    let header = it.next().unwrap_or("");
    // v2 files carry refresh_hz after fps; v1 doesn't.
    let has_refresh = header.split(',').nth(2).is_some_and(|s| s.trim() == "refresh_hz");
    it.filter_map(|line| {
        let cols: Vec<&str> = line.split(',').collect();
        if cols.len() < 15 {
            return None;
        }
        let get = |i: usize| cols[i].trim().parse::<i64>().unwrap_or(-1);
        let idx = |base: usize| if has_refresh { base + 1 } else { base };
        Some(Sample {
            t: get(0),
            fps: get(1),
            refresh_hz: if has_refresh { get(2) } else { -1 },
            cpu_temp: parse_temp(cols[idx(4)]),
            gpu_temp: parse_temp(cols[idx(5)]),
            bat_temp: parse_temp(cols[idx(6)]),
            gpu_freq: get(idx(7)),
            gpu_load: get(idx(8)),
            cpu_freq_l: get(idx(9)),
            cpu_freq_b: get(idx(10)),
            cpu_load: get(idx(11)),
            ma: get(idx(12)),
            mv: get(idx(13)),
            bat_lvl: get(idx(14)),
        })
    }).collect()
}

fn json_str(s: &str) -> String {
    let mut out = String::from("\"");
    for c in s.chars() {
        match c {
            '"' => out.push_str("\\\""),
            '\\' => out.push_str("\\\\"),
            c if c < ' ' => out.push_str(&format!("\\u{:04x}", c as u32)),
            c => out.push(c),
        }
    }
    out.push('"');
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    fn sample(fps: i64, cpu: i64, ma: i64, mv: i64) -> Sample {
        Sample { t: 0, fps, refresh_hz: 120, cpu_temp: cpu, gpu_temp: -1, bat_temp: 400, gpu_freq: -1, gpu_load: -1, cpu_freq_l: 2_000_000, cpu_freq_b: 2_500_000, cpu_load: 50, ma, mv, bat_lvl: 50 }
    }

    #[test]
    fn stats_calculations() {
        let rows = vec![sample(60, 500, 1000, 4000), sample(90, 600, 2000, 4000), sample(-1, -1, -1, -1)];
        let st = stats(&rows);
        assert_eq!(st.fps_avg, 75);
        assert_eq!(st.fps_min, 60);
        assert_eq!(st.fps_active_avg, 75);
        assert_eq!(st.fps_active_min, 60);
        assert_eq!(st.cpu_max, 600); // 60.0°C
        assert_eq!(st.gpu_max, -1);
        assert_eq!(st.bat_max, 400); // 40.0°C
        assert_eq!(st.avg_mw, 6000); // (1000*4000/1000 + 2000*4000/1000) / 2
    }

    #[test]
    fn fps_active_excludes_zero_fps() {
        // 18 warmup zeros + real frames: plain avg is dragged down, active is not.
        let mut rows = Vec::new();
        for _ in 0..18 {
            rows.push(sample(0, 600, 1000, 4000));
        }
        for _ in 0..12 {
            rows.push(sample(120, 600, 1000, 4000));
        }
        let st = stats(&rows);
        assert_eq!(st.fps_min, 0);
        assert_eq!(st.fps_active_min, 120);
        assert_eq!(st.fps_active_avg, 120);
    }

    #[test]
    fn session_name_parse() {
        let (pkg, epoch) = parse_session_name("com.mobile.legends_20261109_091845.csv");
        assert_eq!(pkg, "com.mobile.legends");
        assert!(epoch > 0);
    }

    #[test]
    fn csv_row_roundtrip() {
        let mut rows = vec![sample(60, 1032, 1000, 4000)]; // 103.2°C
        rows[0].t = 3;
        let line = rows[0].to_csv();
        assert!(line.contains(",103.2,"), "temp rendered as decimal: {}", line);
        let csv = format!("{}\n{}", crate::recorder::SESSION_CSV_HEADER, line);
        let back = parse_csv_rows(&csv);
        assert_eq!(back.len(), 1);
        assert_eq!(back[0].fps, 60);
        assert_eq!(back[0].cpu_temp, 1032); // round-trips back to tenths
    }

    #[test]
    fn csv_row_roundtrip_legacy_mc() {
        // Pre-units files stored milli-°C integers.
        let csv = "t,fps,ft_avg,ft_max,cpu_temp,gpu_temp,bat_temp,gpu_freq,gpu_load,cpu_freq_l,cpu_freq_b,cpu_load,ma,mv,bat_lvl\n1,55,-1,-1,100322,-1,43000,350000000,-1,1450000,2284000,-1,737,3925,11";
        let back = parse_csv_rows(csv);
        assert_eq!(back.len(), 1);
        assert_eq!(back[0].cpu_temp, 1003); // 100°C read back from 100322 mc (approx)
        assert_eq!(back[0].bat_temp, 430);
        // normalize check: legacy gpu_freq is raw Hz → kept as-is on parse; new files store kHz later via sampler scaling
    }
}
