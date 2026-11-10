//! CSV/JSON writers + summary computation.

use super::{frametime, Sample, Session, StopReason};
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
    // low1/low01/jank come from the merged session histogram. fps.avg /
    // fps.min stay per-second FPS based and are untouched by frametime.
    let ft = frametime::summarize(&sess.session_hist, sess.session_frames, sess.session_jank,
        sess.has_frametime, sess.session_jank_known);
    format!(
        "{{\n  \"version\": 3,\n  \"pkg\": {},\n  \"profile\": {},\n  \"start\": {},\n  \"duration\": {},\n  \"stop_reason\": {},\n  \"incomplete\": {},\n  \"fps\": {{\"avg\": {}, \"min\": {}, \"low1\": {}, \"low01\": {}}},\n  \"fps_active\": {{\"avg\": {}, \"min\": {}}},\n  \"temp\": {{\"cpu_max\": {}, \"bat_start\": {}, \"bat_end\": {}, \"bat_delta\": {}, \"bat_min\": {}, \"bat_max\": {}}},\n  \"power\": {{\"avg_mw\": {}}},\n  \"jank\": {},\n  \"units\": {{\"temp\": \"C\", \"freq\": \"kHz\", \"power\": \"mW\"}}\n}}",
        json_str(&sess.pkg),
        json_str(&sess.profile),
        sess.start_epoch,
        sess.began_at.elapsed().as_secs(),
        json_str(reason.as_str()),
        incomplete,
        st.fps_avg,
        st.fps_min,
        frametime::low_json(ft.low1),
        frametime::low_json(ft.low01),
        st.fps_active_avg,
        st.fps_active_min,
        temp_json(st.cpu_max),
        temp_json_opt(st.bat_start),
        temp_json_opt(st.bat_end),
        temp_delta_json(st.bat_delta),
        temp_json_opt(st.bat_min),
        temp_json_opt(st.bat_max),
        st.avg_mw,
        frametime::jank_json(ft.jank)
    )
}

pub fn build_summary_json_rows(csv: &str, pkg: &str, profile: &str, start_epoch: u64, duration: u64, reason: StopReason, incomplete: bool) -> String {
    // Recovery path (daemon died mid-session): the CSV holds only
    // per-second ft_avg/ft_max, not the per-frame histograms, so
    // low1/low01/jank stay null here. Only the live session path can
    // merge delta histograms. Keys/values otherwise identical to
    // build_summary_json.
    let samples = parse_csv_rows(csv);
    let st = stats(&samples);
    format!(
        "{{\n  \"version\": 3,\n  \"pkg\": {},\n  \"profile\": {},\n  \"start\": {},\n  \"duration\": {},\n  \"stop_reason\": {},\n  \"incomplete\": {},\n  \"fps\": {{\"avg\": {}, \"min\": {}, \"low1\": null, \"low01\": null}},\n  \"fps_active\": {{\"avg\": {}, \"min\": {}}},\n  \"temp\": {{\"cpu_max\": {}, \"bat_start\": {}, \"bat_end\": {}, \"bat_delta\": {}, \"bat_min\": {}, \"bat_max\": {}}},\n  \"power\": {{\"avg_mw\": {}}},\n  \"jank\": null,\n  \"units\": {{\"temp\": \"C\", \"freq\": \"kHz\", \"power\": \"mW\"}}\n}}",
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
        temp_json_opt(st.bat_start),
        temp_json_opt(st.bat_end),
        temp_delta_json(st.bat_delta),
        temp_json_opt(st.bat_min),
        temp_json_opt(st.bat_max),
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

/// Like `temp_json`, but renders a missing value as JSON `null`.
fn temp_json_opt(tenths: Option<i64>) -> String {
    match tenths {
        Some(v) => temp_json(v),
        None => "null".to_string(),
    }
}

/// Signed one-decimal delta; JSON has no leading `+`, so `+3.0` is `3.0`.
fn temp_delta_json(tenths: Option<i64>) -> String {
    match tenths {
        Some(v) => {
            let sign = if v < 0 { "-" } else { "" };
            let abs = v.abs();
            format!("{}{}.{}", sign, abs / 10, abs % 10)
        }
        None => "null".to_string(),
    }
}

pub struct SummaryStats {
    pub fps_avg: i64,
    pub fps_min: i64,
    /// avg/min over samples with fps > 0 (excludes warmup/loading zeros).
    pub fps_active_avg: i64,
    pub fps_active_min: i64,
    pub cpu_max: i64,
    /// Battery temperature (tenths of °C) across valid samples; None if none.
    pub bat_start: Option<i64>,
    pub bat_end: Option<i64>,
    pub bat_delta: Option<i64>,
    pub bat_min: Option<i64>,
    pub bat_max: Option<i64>,
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
    let bat = bat_stats(samples);
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
        bat_start: bat.start,
        bat_end: bat.end,
        bat_delta: bat.delta,
        bat_min: bat.min,
        bat_max: bat.max,
        avg_mw,
    }
}

/// `bat_temp` is stored in tenths of °C with about 1 °C resolution, so a
/// computed delta carries roughly ±1 °C of noise. The -1 sentinel and values
/// outside a sane -20..100 °C range are ignored.
fn valid_bat_temp(tenths: i64) -> Option<i64> {
    if tenths != -1 && (-200..=1000).contains(&tenths) {
        Some(tenths)
    } else {
        None
    }
}

pub struct BatStats {
    pub start: Option<i64>,
    pub end: Option<i64>,
    pub delta: Option<i64>,
    pub min: Option<i64>,
    pub max: Option<i64>,
}

/// Battery start/end/delta/min/max from the per-second samples, in sample
/// order (first and last *valid* reading, not merely the first/last row).
fn bat_stats(samples: &[Sample]) -> BatStats {
    let valid: Vec<i64> = samples.iter().filter_map(|s| valid_bat_temp(s.bat_temp)).collect();
    match (valid.first(), valid.last()) {
        (Some(&start), Some(&end)) => BatStats {
            start: Some(start),
            end: Some(end),
            delta: Some(end - start),
            min: valid.iter().copied().min(),
            max: valid.iter().copied().max(),
        },
        _ => BatStats { start: None, end: None, delta: None, min: None, max: None },
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
    // Parse by header name, not position: recovery must handle the old
    // 16-column files (with gpu_temp), the newer 15-column ones, and v1 files
    // without refresh_hz. Missing optional columns read -1.
    let names: Vec<&str> = header.split(',').map(|s| s.trim()).collect();
    let col = |name: &str| names.iter().position(|n| *n == name);
    let t_i = col("t").unwrap_or(0);
    let fps_i = col("fps").unwrap_or(1);
    let refresh_i = col("refresh_hz");
    let cpu_i = col("cpu_temp");
    let bat_i = col("bat_temp");
    let gpu_freq_i = col("gpu_freq");
    let gpu_load_i = col("gpu_load");
    let cpu_freq_l_i = col("cpu_freq_l");
    let cpu_freq_b_i = col("cpu_freq_b");
    let cpu_load_i = col("cpu_load");
    let ma_i = col("ma");
    let mv_i = col("mv");
    let bat_lvl_i = col("bat_lvl");
    it.filter_map(|line| {
        let cols: Vec<&str> = line.split(',').collect();
        if cols.len() < names.len() {
            return None;
        }
        let get = |i: usize| cols.get(i).map(|s| s.trim()).unwrap_or("").parse::<i64>().unwrap_or(-1);
        let get_opt = |i: Option<usize>| i.map(get).unwrap_or(-1);
        let temp_opt = |i: Option<usize>| i.and_then(|i| cols.get(i)).map(|s| parse_temp(s)).unwrap_or(-1);
        Some(Sample {
            t: get(t_i),
            fps: get(fps_i),
            refresh_hz: get_opt(refresh_i),
            // Per-second frametime is only needed for the live path;
            // the recovery summary cannot rebuild histograms from it.
            ft_avg_tenths: frametime::FT_MISSING,
            ft_max_tenths: frametime::FT_MISSING,
            cpu_temp: temp_opt(cpu_i),
            bat_temp: temp_opt(bat_i),
            gpu_freq: get_opt(gpu_freq_i),
            gpu_load: get_opt(gpu_load_i),
            cpu_freq_l: get_opt(cpu_freq_l_i),
            cpu_freq_b: get_opt(cpu_freq_b_i),
            cpu_load: get_opt(cpu_load_i),
            ma: get_opt(ma_i),
            mv: get_opt(mv_i),
            bat_lvl: get_opt(bat_lvl_i),
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
        Sample { t: 0, fps, refresh_hz: 120, ft_avg_tenths: -1, ft_max_tenths: -1, cpu_temp: cpu, bat_temp: 400, gpu_freq: -1, gpu_load: -1, cpu_freq_l: 2_000_000, cpu_freq_b: 2_500_000, cpu_load: 50, ma, mv, bat_lvl: 50 }
    }

    fn with_bat(mut s: Sample, bat: i64) -> Sample {
        s.bat_temp = bat;
        s
    }

    #[test]
    fn csv_row_frametime_render() {
        let mut s = sample(120, 500, 1000, 4000);
        s.t = 7;
        s.ft_avg_tenths = 167; // 16.7 ms
        s.ft_max_tenths = 335; // 33.5 ms
        let line = s.to_csv();
        let cols: Vec<&str> = line.split(',').collect();
        assert_eq!(cols.len(), 15);
        assert_eq!(cols[3], "16.7");
        assert_eq!(cols[4], "33.5");
        // Order preserved: t,fps,refresh_hz,ft_avg,ft_max,...
        assert_eq!(s.to_csv().split(',').take(5).collect::<Vec<_>>(),
            vec!["7", "120", "120", "16.7", "33.5"]);
        // Overflow max renders as >=500.0 and keeps 15 columns.
        let mut o = sample(60, 500, 1000, 4000);
        o.ft_max_tenths = frametime::FT_OVERFLOW;
        let ol = o.to_csv();
        assert!(ol.split(',').nth(4).unwrap() == ">=500.0", "{}", ol);
        assert_eq!(ol.split(',').count(), 15);
        // Missing values stay -1.
        let m = sample(60, 500, 1000, 4000).to_csv();
        let mc: Vec<&str> = m.split(',').collect();
        assert_eq!(mc[3], "-1");
        assert_eq!(mc[4], "-1");
        // Recovery still parses a row with >=500.0 in ft_max.
        let csv = format!("{}\n{}", crate::recorder::SESSION_CSV_HEADER, ol);
        let back = parse_csv_rows(&csv);
        assert_eq!(back.len(), 1);
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
        assert_eq!(st.bat_start, Some(400)); // 40.0°C
        assert_eq!(st.bat_end, Some(400));
        assert_eq!(st.bat_delta, Some(0));
        assert_eq!(st.bat_min, Some(400));
        assert_eq!(st.bat_max, Some(400));
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

    #[test]
    fn bat_stats_rising_delta() {
        let rows = vec![
            with_bat(sample(60, 500, 1000, 4000), 430),
            with_bat(sample(60, 500, 1000, 4000), 460),
        ];
        let st = stats(&rows);
        assert_eq!(st.bat_start, Some(430));
        assert_eq!(st.bat_end, Some(460));
        assert_eq!(st.bat_delta, Some(30)); // +3.0 °C
        assert_eq!(st.bat_min, Some(430));
        assert_eq!(st.bat_max, Some(460));
    }

    #[test]
    fn bat_stats_falling_delta() {
        let rows = vec![
            with_bat(sample(60, 500, 1000, 4000), 430),
            with_bat(sample(60, 500, 1000, 4000), 390),
        ];
        let st = stats(&rows);
        assert_eq!(st.bat_delta, Some(-40)); // -4.0 °C (cooler)
        assert_eq!(st.bat_min, Some(390));
        assert_eq!(st.bat_max, Some(430));
    }

    #[test]
    fn bat_stats_zero_delta() {
        let rows = vec![
            with_bat(sample(60, 500, 1000, 4000), 430),
            with_bat(sample(60, 500, 1000, 4000), 430),
        ];
        let st = stats(&rows);
        assert_eq!(st.bat_delta, Some(0));
        assert_eq!(temp_delta_json(st.bat_delta), "0.0");
    }

    #[test]
    fn bat_stats_first_last_valid_skip_sentinel() {
        let rows = vec![
            with_bat(sample(60, 500, 1000, 4000), -1),
            with_bat(sample(60, 500, 1000, 4000), 430),
            with_bat(sample(60, 500, 1000, 4000), 460),
            with_bat(sample(60, 500, 1000, 4000), -1),
        ];
        let st = stats(&rows);
        assert_eq!(st.bat_start, Some(430));
        assert_eq!(st.bat_end, Some(460));
        assert_eq!(st.bat_delta, Some(30));
    }

    #[test]
    fn bat_stats_all_missing_is_null() {
        let rows = vec![
            with_bat(sample(60, 500, 1000, 4000), -1),
            with_bat(sample(60, 500, 1000, 4000), -1),
        ];
        let st = stats(&rows);
        assert_eq!(st.bat_start, None);
        assert_eq!(st.bat_end, None);
        assert_eq!(st.bat_delta, None);
        assert_eq!(st.bat_min, None);
        assert_eq!(st.bat_max, None);
        assert_eq!(temp_json_opt(st.bat_start), "null");
        assert_eq!(temp_delta_json(st.bat_delta), "null");
    }

    #[test]
    fn bat_stats_single_sample() {
        let rows = vec![with_bat(sample(60, 500, 1000, 4000), 430)];
        let st = stats(&rows);
        assert_eq!(st.bat_start, Some(430));
        assert_eq!(st.bat_end, Some(430));
        assert_eq!(st.bat_delta, Some(0));
        assert_eq!(st.bat_min, Some(430));
        assert_eq!(st.bat_max, Some(430));
    }

    #[test]
    fn bat_stats_out_of_range_ignored() {
        // -300 and 1200 tenths are outside the sane -20..100 °C range.
        let rows = vec![
            with_bat(sample(60, 500, 1000, 4000), 430),
            with_bat(sample(60, 500, 1000, 4000), -300),
            with_bat(sample(60, 500, 1000, 4000), 1200),
            with_bat(sample(60, 500, 1000, 4000), 460),
        ];
        let st = stats(&rows);
        assert_eq!(st.bat_start, Some(430));
        assert_eq!(st.bat_end, Some(460));
        assert_eq!(st.bat_min, Some(430));
        assert_eq!(st.bat_max, Some(460));
    }

    #[test]
    fn csv_header_has_no_gpu_temp() {
        assert_eq!(crate::recorder::SESSION_CSV_HEADER.split(',').count(), 15);
        assert!(!crate::recorder::SESSION_CSV_HEADER.contains("gpu_temp"));
        assert_eq!(
            crate::recorder::SESSION_CSV_HEADER,
            "t,fps,refresh_hz,ft_avg,ft_max,cpu_temp,bat_temp,gpu_freq,gpu_load,cpu_freq_l,cpu_freq_b,cpu_load,ma,mv,bat_lvl"
        );
        let line = sample(60, 500, 1000, 4000).to_csv();
        let cols: Vec<&str> = line.split(',').collect();
        assert_eq!(cols.len(), 15);
        assert_eq!(cols[5], "50.0"); // cpu_temp
        assert_eq!(cols[6], "40.0"); // bat_temp
        assert_eq!(cols[7], "-1"); // gpu_freq stays, GPU temp is gone
    }

    #[test]
    fn recovery_parses_old_16_column_csv() {
        let csv = "t,fps,refresh_hz,ft_avg,ft_max,cpu_temp,gpu_temp,bat_temp,gpu_freq,gpu_load,cpu_freq_l,cpu_freq_b,cpu_load,ma,mv,bat_lvl\n0,60,120,-1,-1,60.0,55.0,43.0,100000,50,2000000,2500000,50,1000,4000,50\n1,60,120,-1,-1,61.0,56.0,46.0,100000,50,2000000,2500000,50,1000,4000,50";
        let back = parse_csv_rows(csv);
        assert_eq!(back.len(), 2);
        assert_eq!(back[0].cpu_temp, 600);
        assert_eq!(back[0].bat_temp, 430);
        assert_eq!(back[0].gpu_freq, 100_000);
        assert_eq!(back[1].bat_temp, 460);
    }

    #[test]
    fn recovery_parses_new_15_column_csv() {
        let csv = "t,fps,refresh_hz,ft_avg,ft_max,cpu_temp,bat_temp,gpu_freq,gpu_load,cpu_freq_l,cpu_freq_b,cpu_load,ma,mv,bat_lvl\n0,60,120,-1,-1,60.0,43.0,100000,50,2000000,2500000,50,1000,4000,50\n1,60,120,-1,-1,61.0,46.0,100000,50,2000000,2500000,50,1000,4000,50";
        let back = parse_csv_rows(csv);
        assert_eq!(back.len(), 2);
        assert_eq!(back[0].cpu_temp, 600);
        assert_eq!(back[0].bat_temp, 430);
        assert_eq!(back[1].bat_temp, 460);
    }

    #[test]
    fn json_temp_object_and_version_golden() {
        let csv = "t,fps,refresh_hz,ft_avg,ft_max,cpu_temp,bat_temp,gpu_freq,gpu_load,cpu_freq_l,cpu_freq_b,cpu_load,ma,mv,bat_lvl\n0,60,120,-1,-1,60.0,43.0,100000,50,2000000,2500000,50,1000,4000,50\n1,60,120,-1,-1,61.0,46.0,100000,50,2000000,2500000,50,1000,4000,50\n2,60,120,-1,-1,59.0,39.0,100000,50,2000000,2500000,50,1000,4000,50";
        let json = build_summary_json_rows(csv, "com.example.game", "Performance", 1_700_000_000, 3, StopReason::Manual, false);
        assert!(json.contains("\"version\": 3"), "{}", json);
        assert!(
            json.contains("\"temp\": {\"cpu_max\": 61.0, \"bat_start\": 43.0, \"bat_end\": 39.0, \"bat_delta\": -4.0, \"bat_min\": 39.0, \"bat_max\": 46.0}"),
            "{}", json
        );
        assert!(!json.contains("gpu_max"), "{}", json);
    }
}
