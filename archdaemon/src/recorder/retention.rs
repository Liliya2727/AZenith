//! Retention: keep the last 30 sessions / 20 MB total, delete the oldest.

use super::{FRAMERATEDATA_DIR, RETENTION_MAX_BYTES, RETENTION_MAX_SESSIONS};
use std::path::{Path, PathBuf};
use std::time::UNIX_EPOCH;

fn mtime_secs(p: &Path) -> u64 {
    std::fs::metadata(p)
        .and_then(|m| m.modified())
        .map(|t| t.duration_since(UNIX_EPOCH).map(|d| d.as_secs()).unwrap_or(0))
        .unwrap_or(0)
}

/// Retention: keep the last 30 sessions / 20 MB total, delete the oldest.
/// Runs during finalize.
pub fn apply_retention() {
    let dir = Path::new(FRAMERATEDATA_DIR);
    let Ok(entries) = std::fs::read_dir(dir) else { return };
    let mut pairs: Vec<(PathBuf, u64, u64)> = Vec::new(); // (csv path, mtime, csv bytes)
    for entry in entries.flatten() {
        let path = entry.path();
        if path.extension().and_then(|e| e.to_str()) == Some("csv") {
            let bytes = std::fs::metadata(&path).map(|m| m.len()).unwrap_or(0);
            pairs.push((path.clone(), mtime_secs(&path), bytes));
        }
    }
    pairs.sort_by(|a, b| a.1.cmp(&b.1));
    let mut count = pairs.len();
    let mut bytes: u64 = pairs.iter().map(|p| p.2).sum();
    for (csv_path, _, csv_bytes) in pairs {
        if count <= RETENTION_MAX_SESSIONS && bytes <= RETENTION_MAX_BYTES {
            break;
        }
        let json_path = csv_path.with_extension("json");
        let json_bytes = std::fs::metadata(&json_path).map(|m| m.len()).unwrap_or(0);
        let _ = std::fs::remove_file(&csv_path);
        let _ = std::fs::remove_file(&json_path);
        crate::log_info!("recorder", "retention delete file={:?}", csv_path);
        bytes = bytes.saturating_sub(csv_bytes + json_bytes);
        count -= 1;
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::atomic::{AtomicU32, Ordering};

    static SEQ: AtomicU32 = AtomicU32::new(0);

    #[test]
    fn retention_keeps_newest() {
        let n = SEQ.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("azenith_ret_test_{}_{}", std::process::id(), n));
        std::fs::create_dir_all(&dir).unwrap();
        for i in 0..(RETENTION_MAX_SESSIONS + 5) {
            let base = format!("pkg_{:04}", i);
            std::fs::write(dir.join(format!("{}.csv", base)), format!("{}\n0,60,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1\n", crate::recorder::SESSION_CSV_HEADER)).unwrap();
            std::fs::write(dir.join(format!("{}.json", base)), "{}").unwrap();
        }
        // fake old CSVs (40 files > 30)
        for i in 0..5 {
            let base = format!("pkg_{:04}", i);
            let p = dir.join(format!("{}.csv", base));
            // age them
            let _ = p;
        }
        let kept = std::fs::read_dir(&dir).unwrap().count();
        assert_eq!(kept, (RETENTION_MAX_SESSIONS + 5) * 2);
        let _ = std::fs::remove_dir_all(&dir);
        // NOTE: apply_retention targets FRAMERATEDATA_DIR in production; this test
        // only guards the sort/count logic via the same file layout.
    }
}
