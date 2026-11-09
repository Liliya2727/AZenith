# AZenith Service Documentations

Complete service documentation for `archdaemon`, the Core Daemon of
AZenith for Magisk/KernelSU modules on Android.

This document describes the **Cargo (Rust) service binary** under
`archdaemon/` — what it does, how it is built, how it is integrated
into a module image, what properties it reads, what runtime files it
writes, and how its profile engine and FPS recorder work.

> Companion project notes: this repo also ships `sysdaemon` (the app
> monitor + FPS client supervisor), `thermalcore` (thermal telemetry),
> `binprofiles` / `binutils` / `binpreferenced` (profile & tweak
> helpers), `preloadbin`, the `manager` app (sidepanel UI), and the
> `binder_resolver` APK embedded into `sysdaemon`. Those are referenced
> where relevant but are not part of this crate.

---

## 1. Overview

ArchDaemon is a single, self-contained daemon binary
(`/data/adb/modules/AZenith/system/bin/sys.azenith-service`) launched
by the module's `service.sh`. It runs as root, daemonizes, tracks the
foreground application through the App Companion API files written by
`sysdaemon`, and applies CPU/GPU/I/O performance profiles to game
packages listed in `gamelist/azenithApplist.json`.

Profile application, bypass charging, notifications, logging, process
monitoring and FPS recording live in this one binary. The other Rust
binaries (`binprofiles`, `binutils`, `binpreferenced`,
`sysdaemon`, `thermalcore`) are separate crates that ship alongside it.

Runtime behaviour is driven by system properties under
`persist.sys.azenith.*` (daemon state), `persist.sys.azenithconf.*`
(tweak toggles) and `persist.sys.rynx.*` (thermalcore), plus files
under the config base `/data/adb/.config/AZenith`.

| Capability | Detail |
|---|---|
| **Profile modes** | `0` common, `1` performance, `2` balanced, `3` eco |
| **Logcat tag** | `AZenith` / `AZenith_Profiles` |
| **Target** | `aarch64-linux-android`, `armv7-linux-androideabi` (Android 10+) |
| **Root required** | Yes (`getuid() == 0`), single instance via lock file |
| **Build tool** | `cargo ndk` (Rust edition 2024) |

---

## 2. Project layout (Cargo crate)

```
archdaemon/
├── Cargo.toml              # crate manifest: azenith-service 1.0.0, edition 2024
├── Cargo.lock
├── src/
│   ├── main.rs             # CLI entry point + daemon dispatch
│   ├── version.rs          # FULL_VERSION define (à la C #define MODULE_VERSION)
│   ├── core.rs             # shared types, path constants, DaemonContext
│   ├── daemon.rs           # main_daemon() loop
│   ├── startup.rs          # verify_system_integrity, validateprop, wait_for_java_companion
│   ├── ipc.rs              # inotify watchers (config + API files)
│   ├── monitor.rs          # app_status / gamelist cache
│   ├── profile.rs          # profile mode decision + apply_* profiles
│   ├── config/             # resolution, rendering, state, refresh_rate
│   ├── recorder/           # FPS recorder (sampler, frametime, writer, retention)
│   ├── bypass.rs           # bypass charging
│   ├── preload.rs          # game preload
│   ├── cli.rs              # --profile / --log / --version / --clearlogs …
│   └── …                   # fileops, io, log, module, proc, shell, sysprop, util
└── README.md / DAEMON.md
```

### Module map

| Crate module | Responsibility |
|---|---|
| `core` | Path constants, `DaemonContext`, enums (`ProfileMode`, `LogLevel`), game cache |
| `daemon` | `main_daemon()` — the main loop |
| `startup` | `verify_system_integrity`, `validateprop`, `wait_for_java_companion` |
| `ipc` | Raw inotify via `libc` for config/API files |
| `monitor` | `app_status` parse, `gamelist/` reload |
| `profile` | `apply_performance_profile_inner`, `apply_eco/balanced`, `run_profiler` |
| `config::resolution` / `rendering` | Resolution downscale + smart renderer |
| `config::state` | Daemon state persistence / restore |
| `recorder` | FPS Recorder: per-second CSV + session JSON, `current_frametime` deltas |
| `bypass` | Bypass charging nodes, dynamic bypass |
| `preload` | Game preload (`vmtouch`-style page-cache preloading) |
| `cli` | CLI handlers (`--profile`, `--log`, `--verboselog`, `--version`, `--hidenotifications`, …) |
| `module` | `is_kanged`, `check_module_version` (full-version define), description update |
| `log` | Logcat + file logging (`AZenith.log`, verbose log), rotation |
| `sysprop` | `__system_property_get/set` wrappers |
| `shell` | `systemv` fork/exec, direct command execution |
| `android_ffi` | Bionic FFI (`__system_property_*`, `stdout`/`stderr` globals, log) |

---

## 3. Build

### 3.1 Prerequisites

* Rust stable toolchain with targets `aarch64-linux-android` and
  `armv7-linux-androideabi` (`dtolnay/rust-toolchain@stable`).
* `cargo-ndk` (`cargo install cargo-ndk`) and the Android NDK.

### 3.2 Configure before building

Version is injected at build time by `.github/scripts/verify.sh` (see
§16). The checked-in defaults are placeholders:

```rust
// src/version.rs
pub const FULL_VERSION: &str = "1.0.0";
```

and in every binary `Cargo.toml` (except `thermalcore`):

```toml
[package]
version = "1.0.0"
```

CI's `verify.sh` rewrites both to the real release version:

* `FULL_VERSION` → `"5.6.0 (<count>-<hash>-<type>)"` — the exact string
  `module.prop` carries, so `check_module_version()` can compare exactly.
* `Cargo.toml` `version =` → semver from `./version` (`"5.6.0"`).

### 3.3 Compile

```sh
cargo ndk -t arm64-v8a -t armeabi-v7a build --release
```

The release profile (`Cargo.toml`) is optimised for size with LTO, a
single codegen unit, `panic = 'abort'` and symbol stripping:

```toml
[profile.release]
opt-level = 3
lto = true
codegen-units = 1
panic = 'abort'
strip = true
```

Output: `target/aarch64-linux-android/release/azenith-service`
(and the `armv7` counterpart) — copied by
`.github/scripts/compile_zip.sh` into the module zip.

### 3.4 Dependencies

`libc`, `nix` (poll/fs/time), `inotify`, `signal-hook`, `glob`,
`serde` + `serde_json` (gamelist / JSON). No C build phase —
`archdaemon/jni/` was removed; the daemon is pure Rust.

---

## 4. Module integration

### 4.1 Install the binary

1. Copy `azenith-service` to
   `/data/adb/modules/AZenith/system/bin/sys.azenith-service`.
2. `chmod 755` it.
3. The module's `service.sh` / `post-fs-data.sh` launch it; `main.rs`
   requires root and calls `libc::daemon(0, 0)` to background itself.

### 4.2 `module.prop` version check

`module.prop`'s `version=` holds the **full** build version,
`"5.6.0 (<count>-<hash>-<type>)"`. At startup
(`verify_system_integrity` → `check_module_version`) the daemon
compares the embedded `version::FULL_VERSION` (injected by
`verify.sh`, §3.2) against that line exactly. Mismatch → `log_fatal`,
notification, `persist.sys.azenith.state=stopped`, exit 1.

---

## 5. Startup sequence (`src/main.rs` + `src/daemon.rs`)

When launched with `--run` / `-r`, the daemon runs this sequence:

1. `verify_system_integrity()` — single-instance check
   (`API/.lock`), `dumpsys` tamper check, `GAMELIST` presence,
   `is_kanged()`, `check_module_version()`.
2. `libc::daemon(0, 0)` — daemonize. `SIGINT`/`SIGTERM` → `sighandler`.
3. `init_daemon_context()` + `restore_daemon_state()`.
4. `Recorder::recover()` — rebuild a summary from any leftover
   `frameratedata/*.csv` (`incomplete=true`) and reset
   `record_state` to `idle`.
5. `wait_for_java_companion()` — wait for `java.lock` (max 120 s).
6. Java-lock pipe + watcher thread.
7. `read_app_status` + `reload_gamelist_cache` (primes the game cache).
8. Sets `persist.sys.rynx.learning_enabled=true` and
   `persist.sys.rynx.thermalcore-bigdata.path`, `azenith.state=running`.
9. `checkstate()`, `is_kanged()`, `check_module_version()`,
   `validateprop()` (§9).
10. `runthermalcore()`, `run_profiler(0)` (initial), `update_module_description`.
11. Main loop: poll inotify (≤ 1000 ms while recording), process
    events, run `recorder.tick()`, profile checkup, bypass, power and
    rendering logic.

---

## 6. CLI (`src/main.rs`)

Usage: `sys.azenith-service [options]`

| Flag | Effect |
|---|---|
| `-r`, `--run` | Start the daemon |
| `-p`, `--profile <1\|2\|3>` | Apply performance / balanced / eco now |
| `--version`, `-V` | Print `version::FULL_VERSION` |
| `-l`, `--log <TAG> <LVL> <MSG>` | Write a log message |
| `-vl`, `--verboselog <TAG> <LVL> <MSG>` | Verbose log |
| `--clearlogs`, `-c` | Clear logs |
| `--hidenotifications`, `-hn` / `--shownotifications`, `-sn` | Toggle notifications |
| `--bypasspathlist`, `-bpl` | Print bypass node list |
| `--checkbypasschg`, `-cbc` / `--testbypass <NODE>`, `-tb` | Bypass compatibility checks |
| `--get_rr_modes`, `-getrr` | Dump refresh-rate modes |
| `--setup-access`, `-sa` | Telemetry access + app permissions |
| `--appactivity`, `-actv` | Open the manager's main activity |

---

## 7. Runtime paths (hardcoded in `src/core.rs`)

Config base: `/data/adb/.config/AZenith`.

| Constant | Path |
|---|---|
| `LOCK_FILE` | `/data/adb/.config/AZenith/API/.lock` |
| `LOG_FILE` | `/data/adb/.config/AZenith/debug/AZenith.log` |
| `PROFILE_MODE` / `PROFILE_MODE_APP` | `…/API/current_profile` (+ app mirror) |
| `GAME_INFO` / `GAME_INFO_APP` | `…/API/gameinfo` (+ app mirror) |
| `GAMELIST` | `…/gamelist/azenithApplist.json` |
| `DAEMON_MODES` | `…/API/current_modes` |
| `MODULE_PROP` | `/data/adb/modules/AZenith/module.prop` |
| `APP_MONITOR_FILE` | `…/app_status` |
| `JAVA_LOCK_PATH` | `…/java.lock` |
| Recorder API | `…/API/record_cmd`, `…/API/record_state`, `…/API/current_fps`, `…/API/current_frametime`, `…/frameratedata/` |

---

## 8. System properties

| Property | Meaning |
|---|---|
| `persist.sys.azenith.state` | `running` / `stopped` (daemon state) |
| `persist.sys.azenith.service` | Daemon pid or empty |
| `persist.sys.azenith.soctype` | SoC id (0–5, read by `binprofiles`) |
| `persist.sys.azenith.disabletweak` | Master tweak switch |
| `persist.sys.azenith.debugmode` | Verbose logging |
| `persist.sys.azenith.dropforeground` | Drop-to-balanced when game left foreground |
| `persist.sys.azenith.profilenotifications` | Profile notifications |
| `persist.sys.azenith.force_sidepanel` (+ `debug.azenith.force_sidepanel`) | Force the sidepanel overlay |
| `persist.sys.azenith.custom_*_cpu_gov` / `*_IO` / `*_maligpu_gov` | Per-profile governor / I/O / Mali overrides |
| `persist.sys.azenithconf.*` | Toggles: `AIenabled`, `APreload`, `DThermal`, `SFL`, `bypasschg`, `bypasschgthreshold`, `bypasspath`, `clearbg`, `cpulimit`, `disabletrace`, `dnd`, `fpsged`, `freqoffset`, `fstrim`, `iosched`, `justintime`, `litemode`, `logd`, `malisched`, `preloadbudget`, `renderer`, `schedtunes`, `schemeconfig`, `showtoast`, `thermalcore`, `usefpsgo`, `walttunes` |
| `persist.sys.rynx.learning_enabled`, `persist.sys.rynx.thermalcore-bigdata.path` | Set by the daemon for thermalcore |

---

## 9. `validateprop` — stale property cleanup (`src/startup.rs`)

1. Collects legacy `persist.sys.rianixia.*` props if present.
2. Walks every property via `__system_property_foreach` and flags any
   `persist.sys.azenith*` / `persist.sys.azenithconf.*` name not in
   `VALID_AZENITH_PROPS` (48 entries now).
3. Deletes flagged props via `resetprop -p --delete`.
4. Logs `prop: stale prop flagged/deleting`, or `no stale props found`.

The foreach callback must match bionic's ABI —
`cb(prop_info*, cookie*, name*, value*, source*, serial)` — a wrong
arg count silently flags nothing.

---

## 10. Profile engine (`src/profile.rs`)

### 10.1 Modes

| Mode number | Name | Use |
|---|---|---|
| `0` | `common` | PerfCommon (initial / fallthrough) |
| `1` | `performance` | Gaming — max CPU freq, app priority, renderer/resolution |
| `2` | `balanced` | Daily use — normal freq, eco renderer |
| `3` | `eco` | Power saving — low power, battery saver |

### 10.2 Decision (`run_profiler`)

```
active game in foreground && screen on  -> performance (1)
battery saver on                         -> eco (3)
everything else                          -> balanced (2)
```

Screen-off grace (10 s) keeps performance briefly after screen off.
Foreground-away from the game (30 s) drops back to balanced.
PID retries for newly spawned games (5 × 200 ms). `get_gamestart`
resolves the focused game from the `SystemStateCache`.

### 10.3 Tweak ladder

`apply_*_profile` → governor → `apply_cpu_freq` (PPM path on MediaTek,
plain sysfs otherwise) → `apply_common_tweaks` (VM, workqueue, EAS,
stune, battery saver, split lock, sched features, block I/O) → clear
background apps (performance, `clearbg=1`) → chipset-specific tweaks
→ log “… profile applied”.

---

## 11. FPS Recorder (`src/recorder/`)

Triggered from the floating monitor via `API/record_cmd` (`start`/`stop`,
inotify), the daemon writes `API/record_state` (`idle` / `recording
<file>`) for the overlay to read.

| File | Content |
|---|---|
| `frameratedata/<pkg>_<yyyyMMdd_HHmmss>.csv` | One row per second |
| `frameratedata/<pkg>_<yyyyMMdd_HHmmss>.json` | Session summary |

CSV columns (order fixed):
`t,fps,refresh_hz,ft_avg,ft_max,cpu_temp,gpu_temp,bat_temp,gpu_freq,gpu_load,cpu_freq_l,cpu_freq_b,cpu_load,ma,mv,bat_lvl`.

* `fps` from `API/current_fps` (`<fps> <epoch_ms>`, stale > 3 s → `-1`);
  `fps.avg/min` stay per-second-FPS based.
* `ft_avg` / `ft_max` from the per-second delta of
  `API/current_frametime` (`<epoch_ms> <gen> <source> <frames>
  <sum_us> <hist>`, 100 µs buckets, idx 5000 = ≥ 500 ms overflow).
  Gen change rebases; missing/stale/`none` rows write `-1`.
* Session `fps.low1` / `fps.low01` / `jank` come from the merged
  session histogram (needs ≥ 100 / ≥ 1000 valid frames, else `null`).
  `fpS.active` and temps/power are summarized as before.
* Sessions shorter than 30 s are discarded; files over 30 sessions /
  20 MB are rotated by `retention`.

---

## 12. Bypass charging (`src/bypass.rs`)

78 nodes (`BYPASS_LIST`) across common / MediaTek / Oplus / QCOM /
transsion battery/charger sysfs paths. `handle_dynamic_bypass` reacts
to charging state, `--checkbypasschg` / `--testbypass` verify nodes,
and the list is printed by `--bypasspathlist`.

---

## 13. Logging

* Tag: `AZenith` (also `AZenith_Profiles` for profile logs).
* Files: `debug/AZenith.log` (512 KB, keep 2), verbose + preload logs.
* Levels: `debug` / `info` / `warn` / `error` / `fatal` via `log::*`
  macros (`log_info!`, `log_warn!`, …) with a component tag
  (`[daemon]`, `[profile]`, `[bypass]`, `[recorder]`, `[prop]`, …).
* CLI equivalents: `--log` / `--verboselog`.

---

## 14. Credits & license

* Developed by @Zexshia and @rianixia (Telegram).
* Credits: @Kombat, @Kaminarich, @Rem01Gaming.
* Based on the legacy Encore Tweaks Daemon / AZenith C daemon.
* Licensed under the GNU General Public License v3.0 — see `LICENSE`.

---

## 15. Related documents in this repo

| Document | Purpose |
|---|---|
| `README.md` | Quick start + daemon summary |
| `../sysdaemon/README.md` | App monitor + FPS client supervisor |
| `../thermalcore/README.md` | Thermal telemetry collector |
| `../.github/workflows/build.yml` | CI build (Cargo NDK for all Rust binaries) |
| `../.github/scripts/verify.sh` | Version injection (Cargo semver + `src/version.rs` full define) |
| `../.github/scripts/compile_zip.sh` | Module packaging (copies Rust binaries into `mainfiles/libs/`) |
| `src/version.rs` | Build-time version define |
