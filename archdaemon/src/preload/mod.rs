//! Native game preload engine and daemon integration.

pub mod cli;
pub mod dedup;
pub mod fileops;
pub mod filters;
pub mod pages;
pub mod range;
pub mod report;
pub mod size;
pub mod stats;
pub mod walker;

use crate::shell::execute_command;
use crate::{log_debug, log_preload, log_warn};

use std::fs;
use std::path::Path;

use self::pages::system_page_size;
use self::size::{parse_size, pretty_print_size};
use self::stats::{ActionMode, PreloadConfig, PreloadObserver, TouchStrategy};
use self::walker::Crawler;

const DEFAULT_BUDGET_BYTES: u64 = 500 * 1024 * 1024;

struct DaemonPreloadObserver;

impl PreloadObserver for DaemonPreloadObserver {
    fn warning(&mut self, msg: &str) {
        log_debug!("preload", "warn msg={msg}");
    }

    fn on_file_done(&mut self, path: &Path, _mincore: &[u8]) {
        if let Some(ext) = path.extension().and_then(|e| e.to_str()) {
            if matches!(ext, "so" | "apk" | "dm" | "odex" | "vdex" | "art") {
                log_preload!("preload", "touched file={}", path.to_string_lossy());
            }
        }
    }
}

// ---------------------------------------------------------------------------
// GamePreload (native in-process execution, SIGBUS-safe Pread strategy)
// ---------------------------------------------------------------------------
#[allow(non_snake_case)]
pub fn GamePreload(package: &str) {
    std::thread::sleep(std::time::Duration::from_secs(5));

    if package.is_empty() {
        log_warn!("preload", "package empty, skipping");
        return;
    }

    let apk_path = match execute_command(&format!(
        "cmd package path {package} | head -n1 | cut -d: -f2"
    )) {
        Some(p) => p,
        None => {
            log_warn!("preload", "apk path lookup failed pkg={package}");
            return;
        }
    };

    let apk_path = apk_path.trim();
    if apk_path.is_empty() {
        log_warn!("preload", "apk path lookup failed pkg={package}");
        return;
    }

    let Some(last_slash) = apk_path.rfind('/') else {
        log_warn!("preload", "apk folder resolve failed path={apk_path}");
        return;
    };

    let lib_path = format!("{}/lib/arm64", &apk_path[..last_slash]);

    let lib_exists = fs::read_dir(&lib_path)
        .map(|dir| {
            dir.flatten()
                .any(|e| e.path().extension().map(|ext| ext == "so").unwrap_or(false))
        })
        .unwrap_or(false);

    let mut budget = azenith_config::load().preload_budget;
    if budget.trim().is_empty() {
        budget = "500M".to_string();
    }
    let budget_bytes = match parse_size(&budget) {
        Ok(v) if v >= 0 => v as u64,
        _ => {
            log_warn!("preload", "invalid preloadbudget={budget}, using 500M");
            DEFAULT_BUDGET_BYTES
        }
    };

    let target_path = if lib_exists {
        lib_path.as_str()
    } else {
        apk_path
    };
    let target_type = if lib_exists { "libs" } else { "split apks" };

    log_debug!(
        "preload",
        "start pkg={package} type={target_type} budget={budget}"
    );
    log_preload!(
        "preload",
        "start pkg={package} type={target_type} path={target_path} budget={budget}"
    );

    let page_size = system_page_size();
    let mut cfg = PreloadConfig::new(page_size);
    cfg.action = ActionMode::Touch;
    cfg.touch_strategy = TouchStrategy::Pread;
    cfg.max_file_size = budget_bytes;

    let mut crawler = Crawler::new();
    let mut obs = DaemonPreloadObserver;

    if let Err(err) = crawler.crawl(Path::new(target_path), &cfg, &mut obs) {
        log_warn!("preload", "native preload failed pkg={package} err={err}");
        return;
    }

    let total_pages = crawler.stats.total_pages;
    let total_size = pretty_print_size(total_pages.saturating_mul(page_size));

    log_preload!(
        "preload",
        "done pkg={package} pages={total_pages} size={total_size}"
    );
    log_debug!(
        "preload",
        "done pkg={package} pages={total_pages} size={total_size}"
    );
}
