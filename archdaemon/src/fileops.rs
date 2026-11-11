//! File operations, centralized wrapper around std::fs with Result-based errors.
//!
//! Provides safe file I/O primitives replacing scattered std::fs calls.

use std::fs;
use std::io::{BufRead, BufReader};

/// Read entire file as UTF-8 string.
pub fn read_string(path: &str) -> Option<String> {
    fs::read_to_string(path).ok()
}

/// Read entire file as UTF-8 string, returning Result.
pub fn read_string_result(path: &str) -> Result<String, std::io::Error> {
    fs::read_to_string(path)
}

/// Read first line of a file (trimmed of newline).
pub fn read_first_line(path: &str) -> Option<String> {
    let file = fs::File::open(path).ok()?;
    BufReader::new(file)
        .lines()
        .next()
        .and_then(|l| l.ok())
        .map(|s| s.trim_end_matches('\n').to_string())
}

/// Read a file as integer (trimmed).
pub fn read_int(path: &str) -> Option<i32> {
    read_string(path).and_then(|s| s.trim().parse::<i32>().ok())
}

/// Read a file as integer with default.
pub fn read_int_or_default(path: &str, default: i32) -> i32 {
    read_int(path).unwrap_or(default)
}

/// Write string content to file (truncate if exists).
pub fn write_string(path: &str, content: &str) -> Result<(), std::io::Error> {
    fs::write(path, content)
}

/// Write string content, ignoring errors (best-effort).
pub fn write_string_best_effort(path: &str, content: &str) {
    let _ = fs::write(path, content);
}

/// Remove a file, ignoring errors.
pub fn remove_file_best_effort(path: &str) {
    let _ = fs::remove_file(path);
}

/// Remove a file, returning Result.
pub fn remove_file_result(path: &str) -> Result<(), std::io::Error> {
    fs::remove_file(path)
}

/// Create directory and all parents (best-effort).
pub fn create_dir_all_best_effort(path: &str) {
    let _ = fs::create_dir_all(path);
}

/// Check if file exists.
pub fn exists(path: &str) -> bool {
    fs::metadata(path).is_ok()
}

/// Check if file is empty (0 bytes). Returns 1 if empty, 0 if not, -1 on error.
pub fn is_empty(path: &str) -> i32 {
    match fs::metadata(path) {
        Ok(m) if m.len() == 0 => 1,
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Get file size in bytes.
pub fn size(path: &str) -> Option<u64> {
    fs::metadata(path).ok().map(|m| m.len())
}

/// Read lines from file, returning Vec<String>.
pub fn read_lines_vec(path: &str) -> Option<Vec<String>> {
    let file = fs::File::open(path).ok()?;
    Some(
        BufReader::new(file)
            .lines()
            .map(|l| l.unwrap_or_default())
            .collect(),
    )
}
