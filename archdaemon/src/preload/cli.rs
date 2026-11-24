//! Hand-written POSIX `getopt`-compatible CLI frontend for `vmtouch` v1.4.1 parity.
//!
//! Zero external CLI dependencies. Supports two modes:
//! - `CliMode::StrictCompat` (`argv[0]` = `sys.azenith-preloadbin` / `preloadbin` / `vmtouch`):
//!   exact original C behavior (`--help` and `-?` exit 1).
//! - `CliMode::Subcommand` (`sys.azenith-service preload ...`):
//!   `--help` and `-?` print usage to stdout and exit 0, with only the `Usage:`
//!   line changed to `Usage: sys.azenith-service preload [OPTIONS] ... FILES OR DIRECTORIES ...`.
//!   `-h` is ALWAYS hardlink counting, never help.

use std::ffi::{OsStr, OsString};
use std::fs::File;
use std::io::{self, BufRead, BufReader, Write};
use std::os::unix::ffi::OsStrExt;
use std::path::{Path, PathBuf};
use std::time::Instant;

use super::pages::system_page_size;
use super::range::parse_range;
use super::report::{format_human_summary, format_kv_summary, format_residency_chart};
use super::size::parse_size;
use super::stats::{ActionMode, PreloadConfig, PreloadObserver, TouchStrategy};
use super::walker::Crawler;

pub const VMTOUCH_VERSION: &str = "1.4.1";

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CliMode {
    StrictCompat,
    Subcommand,
}

#[derive(Debug)]
struct ParsedCli {
    cfg: PreloadConfig,
    touch: bool,
    evict: bool,
    lock: bool,
    lockall: bool,
    daemon: bool,
    wait: bool,
    pidfile: Option<OsString>,
    batch_file: Option<OsString>,
    batch_0_delim: bool,
    output_type: Option<String>,
    positionals: Vec<OsString>,
}

fn print_usage(mode: CliMode) {
    let usage_line = match mode {
        CliMode::StrictCompat => "Usage: vmtouch [OPTIONS] ... FILES OR DIRECTORIES ...",
        CliMode::Subcommand => {
            "Usage: sys.azenith-service preload [OPTIONS] ... FILES OR DIRECTORIES ..."
        }
    };
    let mut out = io::stdout().lock();
    let _ = writeln!(
        out,
        "\nvmtouch v{VMTOUCH_VERSION} - the Virtual Memory Toucher"
    );
    let _ = writeln!(out, "Portable file system cache diagnostics and control\n");
    let _ = writeln!(out, "{usage_line}\n\nOptions:");
    let _ = writeln!(out, "  -t touch pages into memory");
    let _ = writeln!(out, "  -e evict pages from memory");
    let _ = writeln!(out, "  -l lock pages in physical memory with mlock(2)");
    let _ = writeln!(out, "  -L lock pages in physical memory with mlockall(2)");
    let _ = writeln!(out, "  -d daemon mode");
    let _ = writeln!(out, "  -m <size> max file size to touch");
    let _ = writeln!(
        out,
        "  -p <range> use the specified portion instead of the entire file"
    );
    let _ = writeln!(out, "  -f follow symbolic links");
    let _ = writeln!(out, "  -F don't crawl different filesystems");
    let _ = writeln!(out, "  -h also count hardlinked copies");
    let _ = writeln!(
        out,
        "  -i <pattern> ignores files and directories that match this pattern"
    );
    let _ = writeln!(
        out,
        "  -I <pattern> only process files that match this pattern"
    );
    let _ = writeln!(
        out,
        "  -b <list file> get files or directories from the list file"
    );
    let _ = writeln!(
        out,
        "  -0 in batch mode (-b) separate paths with NUL byte instead of newline"
    );
    let _ = writeln!(
        out,
        "  -w wait until all pages are locked (only useful together with -d)"
    );
    let _ = writeln!(
        out,
        "  -P <pidfile> write a pidfile (only useful together with -l or -L)"
    );
    let _ = writeln!(
        out,
        "  -o <type> output in machine friendly format. 'kv' for key=value pairs."
    );
    let _ = writeln!(out, "  -v verbose");
    let _ = writeln!(out, "  -q quiet");
}

fn fatal_msg(msg: &str) -> i32 {
    let mut err = io::stderr().lock();
    let _ = writeln!(err, "vmtouch: FATAL: {msg}");
    1
}

fn warn_msg(quiet: bool, msg: &str) {
    if quiet {
        return;
    }
    let mut err = io::stderr().lock();
    let _ = writeln!(err, "vmtouch: WARNING: {msg}");
}

enum ParseOutcome {
    Ok(ParsedCli),
    Help(i32),
    Fatal(String),
    UsageError(Option<String>),
}

/// Parse CLI arguments with POSIX `getopt`-compatible permutation, combined
/// short flags (`-vt`, `-m500M`, `-vtm 500M`), `--` terminator, and last-value-wins.
fn parse_args(args: &[OsString], mode: CliMode, page_size: i64) -> ParseOutcome {
    let mut parsed = ParsedCli {
        cfg: PreloadConfig::new(page_size),
        touch: false,
        evict: false,
        lock: false,
        lockall: false,
        daemon: false,
        wait: false,
        pidfile: None,
        batch_file: None,
        batch_0_delim: false,
        output_type: None,
        positionals: Vec::new(),
    };

    let mut idx = 0_usize;
    let mut after_double_dash = false;

    while idx < args.len() {
        let arg = &args[idx];
        let bytes = arg.as_bytes();

        if after_double_dash || bytes.len() <= 1 || bytes[0] != b'-' {
            parsed.positionals.push(arg.clone());
            idx += 1;
            continue;
        }

        if bytes == b"--" {
            after_double_dash = true;
            idx += 1;
            continue;
        }

        if bytes == b"--help" {
            if mode == CliMode::Subcommand {
                return ParseOutcome::Help(0);
            }
            return ParseOutcome::UsageError(Some("unknown option -- -".to_string()));
        }

        if bytes.starts_with(b"--") {
            return ParseOutcome::UsageError(Some("unknown option -- -".to_string()));
        }

        // Short option cluster: `-abc` or `-m500M` or `-vtm 500M`
        let mut pos = 1_usize;
        while pos < bytes.len() {
            let opt = bytes[pos];
            let takes_arg = matches!(opt, b'i' | b'I' | b'p' | b'b' | b'm' | b'P' | b'o');

            if takes_arg {
                let val: OsString = if pos + 1 < bytes.len() {
                    OsStr::from_bytes(&bytes[pos + 1..]).to_os_string()
                } else if idx + 1 < args.len() {
                    idx += 1;
                    args[idx].clone()
                } else {
                    return ParseOutcome::UsageError(Some(format!(
                        "option requires an argument -- {}",
                        opt as char
                    )));
                };

                match opt {
                    b'p' => {
                        let s = val.to_string_lossy();
                        match parse_range(&s, page_size) {
                            Ok(r) => {
                                parsed.cfg.offset = r.offset;
                                parsed.cfg.max_len = r.max_len;
                            }
                            Err(e) => return ParseOutcome::Fatal(e),
                        }
                    }
                    b'i' => {
                        if let Err(e) = parsed.cfg.ignores.push_ignore(&val) {
                            return ParseOutcome::Fatal(e);
                        }
                    }
                    b'I' => {
                        if let Err(e) = parsed.cfg.filters.push_filter(&val) {
                            return ParseOutcome::Fatal(e);
                        }
                    }
                    b'm' => {
                        let s = val.to_string_lossy();
                        match parse_size(&s) {
                            Ok(sz) => parsed.cfg.max_file_size = sz as u64,
                            Err(e) => return ParseOutcome::Fatal(e),
                        }
                    }
                    b'b' => parsed.batch_file = Some(val),
                    b'P' => parsed.pidfile = Some(val),
                    b'o' => parsed.output_type = Some(val.to_string_lossy().into_owned()),
                    _ => {}
                }
                break;
            }

            match opt {
                b't' => parsed.touch = true,
                b'e' => parsed.evict = true,
                b'q' => parsed.cfg.quiet = true,
                b'v' => parsed.cfg.verbose = parsed.cfg.verbose.saturating_add(1),
                b'l' => {
                    parsed.lock = true;
                    parsed.touch = true;
                }
                b'L' => {
                    parsed.lockall = true;
                    parsed.touch = true;
                }
                b'd' => parsed.daemon = true,
                b'f' => parsed.cfg.follow_symlinks = true,
                b'F' => parsed.cfg.single_fs = true,
                b'h' => parsed.cfg.ignore_hardlinks = true,
                b'0' => parsed.batch_0_delim = true,
                b'w' => parsed.wait = true,
                b'?' => {
                    if mode == CliMode::Subcommand {
                        return ParseOutcome::Help(0);
                    }
                    return ParseOutcome::UsageError(Some("unknown option -- ?".to_string()));
                }
                other => {
                    return ParseOutcome::UsageError(Some(format!(
                        "unknown option -- {}",
                        other as char
                    )));
                }
            }
            pos += 1;
        }

        idx += 1;
    }

    ParseOutcome::Ok(parsed)
}

struct CliObserver {
    quiet: bool,
    verbose: u32,
}

impl PreloadObserver for CliObserver {
    fn warning(&mut self, msg: &str) {
        warn_msg(self.quiet, msg);
    }

    fn on_evict_file(&mut self, path: &Path) {
        if self.verbose > 0 {
            let mut out = io::stdout().lock();
            let _ = writeln!(out, "Evicting {}", path.to_string_lossy());
        }
    }

    fn on_file_start(&mut self, path: &Path) {
        if self.verbose > 0 {
            let mut out = io::stdout().lock();
            let _ = writeln!(out, "{}", path.to_string_lossy());
        }
    }

    fn on_chart_update(&mut self, mincore: &[u8]) {
        if self.verbose > 0 {
            let chart = format_residency_chart(mincore);
            let mut out = io::stdout().lock();
            let _ = write!(out, "{chart}");
            let _ = out.flush();
        }
    }

    fn on_file_done(&mut self, _path: &Path, mincore: &[u8]) {
        if self.verbose > 0 {
            let chart = format_residency_chart(mincore);
            let mut out = io::stdout().lock();
            let _ = writeln!(out, "{chart}");
            let _ = out.flush();
        }
    }
}

fn crawl_batch(
    batch_path: &OsStr,
    nul_delim: bool,
    crawler: &mut Crawler,
    cfg: &PreloadConfig,
    obs: &mut dyn PreloadObserver,
) -> Result<(), String> {
    let reader: Box<dyn BufRead> = if batch_path.as_bytes() == b"-" {
        Box::new(BufReader::new(io::stdin()))
    } else {
        match File::open(Path::new(batch_path)) {
            Ok(f) => Box::new(BufReader::new(f)),
            Err(err) => {
                obs.warning(&format!(
                    "unable to open {} ({err}), skipping",
                    batch_path.to_string_lossy()
                ));
                return Ok(());
            }
        }
    };

    let delim = if nul_delim { b'\0' } else { b'\n' };
    let mut reader = reader;
    let mut buf = Vec::new();

    loop {
        buf.clear();
        match reader.read_until(delim, &mut buf) {
            Ok(0) => break,
            Ok(_) => {
                // Intentional deviation (Inventory §6 item 10): only strip trailing
                // delimiter byte matching the active delimiter (do NOT strip '\n' in -0 mode).
                if buf.last() == Some(&delim) {
                    buf.pop();
                }
                let p = PathBuf::from(OsStr::from_bytes(&buf));
                crawler.crawl(&p, cfg, obs)?;
            }
            Err(_) => break,
        }
    }

    Ok(())
}

/// Entry point for both `StrictCompat` (`argv[0]` shim) and `Subcommand` (`preload`).
pub fn run_cli(args: &[OsString], mode: CliMode) -> i32 {
    let page_size = system_page_size();

    let mut parsed = match parse_args(args, mode, page_size) {
        ParseOutcome::Ok(p) => p,
        ParseOutcome::Help(rc) => {
            print_usage(mode);
            return rc;
        }
        ParseOutcome::Fatal(msg) => return fatal_msg(&msg),
        ParseOutcome::UsageError(err_opt) => {
            if let Some(msg) = err_opt {
                let mut err = io::stderr().lock();
                let _ = writeln!(err, "vmtouch: {msg}");
            }
            print_usage(mode);
            return 1;
        }
    };

    // Validation rules matching preloadbin/jni/main.c:731-744
    if parsed.touch && parsed.evict {
        return fatal_msg("invalid combination: -t and -e");
    }
    if parsed.evict && parsed.lock {
        return fatal_msg("invalid combination: -e and -l");
    }
    if parsed.lock && parsed.lockall {
        return fatal_msg("invalid combination: -l and -L");
    }
    if parsed.daemon {
        if !parsed.lock && !parsed.lockall {
            return fatal_msg("daemon mode needs -l or -L");
        }
        if !parsed.wait {
            parsed.cfg.quiet = true;
            parsed.cfg.verbose = 0;
        }
    }
    if parsed.wait && !parsed.daemon {
        return fatal_msg("wait mode needs -d");
    }
    if parsed.cfg.quiet && parsed.cfg.verbose > 0 {
        return fatal_msg("invalid combination: -q and -v");
    }
    if parsed.pidfile.is_some() && !parsed.lock && !parsed.lockall {
        return fatal_msg("pidfile needs -l or -L");
    }
    if parsed.positionals.is_empty() && parsed.batch_file.is_none() {
        let mut out = io::stdout().lock();
        let _ = writeln!(out, "no files or directories specified");
        drop(out);
        print_usage(mode);
        return 1;
    }

    // Stage 2: locking / daemon modes are deferred to Stage 3.
    if parsed.lock || parsed.lockall || parsed.daemon || parsed.wait || parsed.pidfile.is_some() {
        return fatal_msg("locking/daemon options (-l/-L/-d/-w/-P) are not supported yet");
    }

    parsed.cfg.action = if parsed.evict {
        ActionMode::Evict
    } else if parsed.touch {
        ActionMode::Touch
    } else {
        ActionMode::Query
    };
    parsed.cfg.touch_strategy = TouchStrategy::Mmap;

    let start = Instant::now();
    let mut crawler = Crawler::new();
    let mut obs = CliObserver {
        quiet: parsed.cfg.quiet,
        verbose: parsed.cfg.verbose,
    };

    if let Some(ref batch) = parsed.batch_file
        && let Err(e) = crawl_batch(
            batch,
            parsed.batch_0_delim,
            &mut crawler,
            &parsed.cfg,
            &mut obs,
        )
    {
        return fatal_msg(&e);
    }

    for pos in &parsed.positionals {
        if let Err(e) = crawler.crawl(Path::new(pos), &parsed.cfg, &mut obs) {
            return fatal_msg(&e);
        }
    }

    let elapsed = start.elapsed().as_secs_f64();

    if !parsed.cfg.quiet {
        let mut out = io::stdout().lock();
        match parsed.output_type.as_deref() {
            None => {
                let summary = format_human_summary(
                    &crawler.stats,
                    parsed.cfg.action,
                    parsed.cfg.verbose,
                    page_size,
                    elapsed,
                );
                let _ = write!(out, "{summary}");
            }
            Some(o) if o.starts_with("kv") => {
                let summary =
                    format_kv_summary(&crawler.stats, parsed.cfg.action, page_size, elapsed);
                let _ = write!(out, "{summary}");
            }
            Some(_) => {}
        }
    }

    0
}
