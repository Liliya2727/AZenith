//! Game preload, mirroring `jni/src/preload/preload.c`.

use crate::sysprop::get_or_default;
use crate::{log_debug, log_preload, log_warn};
use crate::shell::execute_command;

use std::fs;

// ---------------------------------------------------------------------------
// GamePreload
// ---------------------------------------------------------------------------
#[allow(non_snake_case)]
pub fn GamePreload(package: &str) {
    std::thread::sleep(std::time::Duration::from_secs(5));

    if package.is_empty() {
        log_warn!("preload", "package empty, skipping");
        return;
    }

    let apk_path = match execute_command(&format!("cmd package path {package} | head -n1 | cut -d: -f2")) {
        Some(p) => p,
        None => {
            log_warn!("preload", "apk path lookup failed pkg={package}");
            return;
        }
    };

    let apk_path = apk_path.trim();
    let apk_path = if apk_path.is_empty() {
        log_warn!("preload", "apk path lookup failed pkg={package}");
        return;
    } else {
        apk_path.to_string()
    };

    let last_slash = match apk_path.rfind('/') {
        Some(p) => p,
        None => {
            log_warn!("preload", "apk folder resolve failed path={apk_path}");
            return;
        }
    };

    let lib_path = format!("{}/lib/arm64", &apk_path[..last_slash]);

    let lib_exists = fs::read_dir(&lib_path)
        .map(|dir| dir.flatten().any(|e| {
            e.path().extension().map(|ext| ext == "so").unwrap_or(false)
        }))
        .unwrap_or(false);

    let budget = get_or_default("persist.sys.azenithconf.preloadbudget", "500M");

    let target_path = if lib_exists { &lib_path } else { &apk_path };
    let target_type = if lib_exists { "libs" } else { "split apks" };

    let preload_cmd = format!("sys.azenith-preloadbin -v -t -m {budget} \"{target_path}\"");

    let output = match crate::shell::execute_command(&preload_cmd) {
        Some(o) => o,
        None => {
            log_warn!("preload", "preloadbin spawn failed pkg={package}");
            return;
        }
    };

    log_debug!("preload", "start pkg={package} type={target_type} budget={budget}");
    log_preload!("preload", "start pkg={package} type={target_type} path={target_path} budget={budget}");

    let mut total_pages = 0i32;
    let mut total_size = String::new();

    for line in output.lines() {
        let line = line.trim();

        if let Some(p_pages) = line.find("Touched Pages:") {
            let after = &line[p_pages + "Touched Pages: ".len()..];
            // Parse "N (size)"
            if let Some(paren_start) = after.find('(') {
                if let Some(paren_end) = after[paren_start..].find(')') {
                    let pages_str = after[..paren_start].trim();
                    let size_str = &after[paren_start + 1..paren_start + paren_end];
                    if let Ok(pages) = pages_str.parse::<i32>() {
                        total_pages += pages;
                        total_size = size_str.to_string();
                        log_debug!("preload", "chunk touched pages={pages} size={size_str}");
                    }
                }
            }
            continue;
        }

        if let Some(ext) = line.rfind('.') {
            let ext = &line[ext..];
            if matches!(ext, ".so" | ".apk" | ".dm" | ".odex" | ".vdex" | ".art") {
                log_preload!("preload", "touched file={line}");
            }
        }
    }

    log_preload!("preload", "done pkg={package} pages={total_pages} size={total_size}");
    log_debug!("preload", "done pkg={package} pages={total_pages} size={total_size}");
}
