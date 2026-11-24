//! Page-size query and checked byte-to-page math.

/// Query the OS page size via `sysconf(_SC_PAGESIZE)`, falling back to 4096.
pub fn system_page_size() -> i64 {
    let ps = unsafe { libc::sysconf(libc::_SC_PAGESIZE) };
    if ps > 0 { ps as i64 } else { 4096 }
}

/// Checked `ceil(bytes / page_size)`.
pub fn bytes_to_pages(bytes: i64, page_size: i64) -> Result<i64, String> {
    if page_size <= 0 {
        return Err("invalid page size".to_string());
    }
    if bytes <= 0 {
        return Ok(0);
    }
    bytes
        .checked_add(page_size - 1)
        .map(|sum| sum / page_size)
        .ok_or_else(|| "page count overflow".to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn bytes_to_pages_rounding_and_overflow() {
        assert_eq!(bytes_to_pages(0, 4096).unwrap(), 0);
        assert_eq!(bytes_to_pages(1, 4096).unwrap(), 1);
        assert_eq!(bytes_to_pages(4096, 4096).unwrap(), 1);
        assert_eq!(bytes_to_pages(4097, 4096).unwrap(), 2);
        assert!(bytes_to_pages(i64::MAX, 4096).is_err());
        assert!(bytes_to_pages(100, 0).is_err());
    }
}
