# AZenith Service Documentations

Complete service documentation for `sysdaemon`, the app-monitor
daemon of AZenith for Magisk/KernelSU modules on Android.

This document describes the **Cargo (Rust) service binary** under
`sysdaemon/` — what it does, how it is built, what it observes, what
files it publishes, and how its FPS client supervisor and frametime
publisher work.

> Companion project notes: this repo also ships `archdaemon` (the core
> performance daemon that consumes `app_status`, `current_fps` and
> `current_frametime`), `thermalcore` (thermal telemetry),
> `binprofiles` / `binutils` / `binpreferenced` (profile & tweak
> helpers), `preloadbin`, the `manager` app (sidepanel UI), and the
> `binder_resolver` APK embedded into this binary. Those are referenced
> where relevant but are not part of this crate.

---

## 1. Overview

sysdaemon is a single, self-contained daemon binary
(`/data/adb/modules/AZenith/system/bin/sys.azenith-appmonitoring`)
launched by the module's `service.sh`. It runs as root, daemonizes,
registers an `IProcessObserver` with ActivityManager, polls
`IPowerManager` / `INotificationManager` /
`IBatteryPropertiesRegistrar` over binder every 500 ms, reads the task
manager's recents list, and atomically publishes `app_status` /
`background_apps` whenever something actually changes. An idle daemon
does not touch the disk.

It is **observation only**: it applies no CPU governors, kills no
background apps, and changes no refresh rates. `archdaemon` and the
manager app consume the published files to make decisions.

It also supervises the FPS client for the focused task. All sources
publish `<fps> <epoch_ms>` to `<cfg>/API/current_fps` (format frozen —
the manager and `archdaemon` parse it), and cumulative per-frame
intervals to `<cfg>/API/current_frametime` for `archdaemon`'s FPS
Recorder (low1 / low01 / jank).

| Capability | Detail |
|---|---|
| **Focus tracking** | `IProcessObserver` + `getFocusedRootTaskInfo` poll (corrects missed AMS callbacks) |
| **Recents** | `getTasks` / `getRecentTasks` every 2 s |
| **FPS sources** | `uprobe` → `surfaceflinger` → `windowmanager` (automatic, frametime-capable first) |
| **Logcat tag** | `daemon` (mirrored from `sysmon.log`) |
| **Target** | `aarch64-linux-android`, `armv7-linux-androideabi` (Android 10+) |
| **Root required** | Yes (`getuid() == 0`), single instance via `java.lock` |

---

## 2. Project layout (Cargo crate)

```
sysdaemon/
├── Cargo.toml              # crate manifest: azenith-appmonitoring 1.0.0, edition 2024
├── Cargo.lock
├── binder_resolver.apk     # embedded resolver APK (staged by CI from binder_resolver/)
├── assets/
│   ├── fps_probe.bpf.c     # eBPF source for the uprobe FPS source
│   └── fps_probe.bpf.o     # prebuilt object (committed, consumers need no clang)
└── src/
    ├── main.rs             # daemon entry point + self-test dispatch
    ├── observer.rs         # IProcessObserver -> event channel -> worker
    ├── procs.rs            # /proc scan (process table + foreground guess)
    ├── status.rs           # screen/battery saver/zen/refresh-rate polling
    ├── recent.rs           # task-manager recents parcel parsing
    ├── focused.rs          # getFocusedRootTaskInfo poll correction
    ├── state.rs            # monitor state + atomic publishing
    ├── pkg.rs / label.rs   # uid -> package, APK label parsing (AXML + resources.arsc)
    ├── ndk.rs              # libbinder_ndk via dlopen
    ├── codes.rs            # transaction-code resolver (embedded APK + cache)
    ├── fps.rs              # FPS supervisor + frametime publisher
    ├── inotify.rs          # gated-poll wakeup for gameinfo/always-on
    ├── lock.rs             # java.lock single-instance lock
    └── log.rs              # sysmon.log writer + logcat mirror
```

### Module map

| Crate module | Responsibility |
|---|---|
| `main` | Root check, daemonize, startup orchestration, self-tests |
| `observer` | `IProcessObserver` callbacks → channel → worker |
| `procs` | `/proc` process table scan (`pid`, `pkg`, `uid`, `oom_score_adj`) |
| `status` | `IPowerManager` / `INotificationManager` / battery / display polling |
| `recent` | Recents parcel parsing (`getTasks` / `getRecentTasks`) |
| `focused` | Focused-root-task poll (corrects missed AMS focus events) |
| `state` | Focused app, recent list, process table, `SysStat`; atomic publish |
| `pkg` / `label` | UID → package (`IPackageManager`), package → label (APK resources) |
| `ndk` | `libbinder_ndk.so` via `dlsym` (raw binder transactions) |
| `codes` | Resolves binder transaction codes from the embedded resolver APK; caches per `(SDK, incremental)` |
| `fps` | FPS supervisor (game gate, source fallback) + frametime histogram publisher |
| `inotify` | Watches `API/gameinfo` + `debug/fps_counter_alwayson` to wake the supervisor |
| `lock` | `java.lock` POSIX record lock (shared with `archdaemon`) |
| `log` | `sysmon.log` (128 KB, keep 2) + logcat mirror, panic hook |

---

## 3. Build

### 3.1 Prerequisites

* Rust stable toolchain with targets `aarch64-linux-android` and
  `armv7-linux-androideabi`.
* `cargo-ndk` and the Android NDK.
* `binder_resolver.apk` present at `sysdaemon/binder_resolver.apk`
  before `cargo` runs (embedded via `include_bytes!`). CI builds it
  from `binder_resolver/` and copies it there.

### 3.2 Configure before building

Version is injected at build time by `.github/scripts/verify.sh`
(see `archdaemon/DAEMON.md` §3.2). The checked-in `Cargo.toml`
version is the placeholder `1.0.0`; CI rewrites it to the semver from
`./version` (e.g. `5.6.0`) for every binary except `thermalcore`.

### 3.3 Compile

```sh
cargo ndk -t arm64-v8a -t armeabi-v7a build --release
```

The release profile is optimised for size:

```toml
[profile.release]
opt-level = 'z'
lto = true
codegen-units = 1
panic = 'abort'
strip = true
```

Output: `target/aarch64-linux-android/release/azenith-appmonitoring`
(and the `armv7` counterpart) — copied by
`.github/scripts/compile_zip.sh` into the module zip as
`sys.azenith-appmonitoring`.

### 3.4 Dependencies

`libc`, `signal-hook`, and — on 64-bit only —
`aya = "0.13"` (eBPF uprobe; aya has no 32-bit Android support, so
the uprobe source is `#[cfg(target_pointer_width = "64")]` gated).
No async runtime; the hot observation path never forks a shell.

---

## 4. Startup sequence (`src/main.rs`)

1. Root check — exits with `run as root` when `getuid() != 0`.
2. Self-tests (`--label`, `--recents`, `--focused`, `--clearlogs`)
   run before daemonizing and exit without starting the service.
3. `log_init` / `redirect_stdio` — stdout/stderr go to `sysmon.log`.
4. `daemon()` — fork + `setsid` + `chdir("/")`.
5. Take `java.lock` (POSIX record lock, single instance).
6. `capture_previous()` — remembers the last `focused_app` /
   `refresh_rate` from the files so a restart keeps the on-screen app.
7. `load_codes()` — runs the embedded resolver APK, caches codes
   under `debug/codes.<sdk>.<incremental>`.
8. Bind AMS + `IPackageManager` via `libbinder_ndk`; `linkToDeath` on
   AMS (exit if `system_server` dies, so the module restarts us).
9. `observer::start()` — `IProcessObserver` channel + worker.
10. Recents poller (2 s), `fps::start()` (FPS supervisor thread),
    `seed()` (/proc scan → initial foreground guess),
    `focused::start()` (1 s RootTaskInfo correction poll).
11. `ABinderProcess_joinThreadPool()` — blocks forever.

---

## 5. Observation sources

| Source | Binder service / mechanism | What it gives |
|---|---|---|
| Process lifecycle | `IProcessObserver.registerProcessObserver` | `onForegroundActivitiesChanged`, `onProcessStarted`, `onProcessDied` |
| Screen on/off | `IPowerManager.isInteractive()` (poll + `IDisplayManagerCallback`) | `screen_awake` |
| Battery saver | `IPowerManager.isPowerSaveMode()` (poll) | `battery_saver` |
| Zen mode | `INotificationManager.getZenMode()` (poll) | `zen_mode` (0=off, 1=priority, 2=none, 3=alarms) |
| Battery level | `IBatteryPropertiesRegistrar.getProperty()` (poll) | `battery_level` (0–100), `is_charging` |
| Refresh rate | `IDisplayManager.getDisplayInfo()` parcel scan (5 s cycle + display events) | `refresh_rate` (Hz) |
| Recents | `IActivityTaskManager.getTasks()` / `IActivityManager.getRecentTasks()` (2 s poll) | `background_apps` package order |
| Focused task | `IActivityTaskManager.getFocusedRootTaskInfo()` (1 s poll) | Corrects focus on missed AMS callbacks |
| /proc | Direct scan | Process table, `oom_score_adj`, initial foreground guess |

### 5.1 `app_status`

```
focused_app <pkg> <pid> <uid>
screen_awake <0|1>
battery_saver <0|1>
zen_mode <0|1|2|3>
battery_level <0..100|-1>
is_charging <0|1>
app_name <label>
refresh_rate <Hz>
```

Published atomically (tmp + rename) to `<cfg>/app_status`, only when
content changed. `focused_app none 0 0` when nothing is in the
foreground. The label lookup calls `system_server` (~50 ms) and is
snapshot-validated: if focus changed meanwhile, the publish is
skipped so name and pid always belong to the same app.

### 5.2 `background_apps`

```
<pkg> <pid> <uid>
...
```

One line per recently used user app, focused first. A live process is
preferred; if it died but the task is still in recents, the last known
pid/uid is kept. Entries are removed only when the task leaves recents
**or** its last process dies. On Android 16 / several ROMs the task
parsers return only the focused task — a focused-only reply is then
ignored instead of wiping the list.

---

## 6. Transaction codes (`src/codes.rs`)

Binder transaction numbers change between Android versions and OEM
ROMs, so the daemon resolves them at runtime:

1. Writes the embedded `binder_resolver.apk` to
   `/data/local/tmp/zenith_resolver.apk`.
2. Runs it via `/system/bin/app_process` with `zx.binder.MainKt`
   (fallback `com.rem01gaming.binderresolver.MainKt`), feeding the
   query list on stdin.
3. Parses `Class::TRANSACTION_name <code>` lines into `Codes`.
4. Caches the raw output in
   `<cfg>/debug/codes.<sdk>.<incremental>` with a marker hash of the
   query list; a valid cache skips the resolver run.
5. Removes the temp APK.

`apply_boot_cp_env()` seeds `BOOTCLASSPATH` / `DEX2OATBOOTCLASSPATH`
for child `app_process` spawns when the daemon itself did not inherit
them — without them ART aborts in `JNI_CreateJavaVM`
(`BOOTCLASSPATH and DEX2OATBOOTCLASSPATH must not be empty`).

---

## 7. FPS client (`src/fps.rs`)

### 7.1 Supervisor

The supervisor thread keeps one FPS source alive while the **gate** is
open: `API/gameinfo` names a game, or
`debug/fps_counter_alwayson == "true"`. Both files are watched with
`inotify` and mirrored into a condvar-guarded `GATE`, so every check
is a mutex read and sleeping code wakes immediately when the answer
changes. While the gate is closed the source is stopped and both
`current_fps` and `current_frametime` hold the single line
`NO_ACTIVE_GAME` (same pattern for both files).

Source selection (`debug/explicit_fps_mthd`):

| Flag | Behaviour |
|---|---|
| missing / empty | Automatic (below) |
| `use_windmanager` | Windowmanager only, even when it keeps failing |
| `use_uprobe` / `use_ebpf` | Uprobe only (64-bit) |
| `use_sf` | SurfaceFlinger only |

Automatic order is **uprobe → surfaceflinger → windowmanager** so
frametime-capable sources win; windowmanager is the last resort
(rate only). The flag is re-read every 5 s, so pin changes take effect
without a daemon restart. `CLIENT_STARTUP_TIMEOUT` (15 s) and
`RETRY_DELAY` (5 s) bound startup and restart behaviour; the pinned
source is retried as-is.

### 7.2 Sources

| Source | Mechanism | Frametime |
|---|---|---|
| `uprobe` | eBPF uprobe (aya) on `libgui`'s `Surface::queueBuffer` family, attached to the focused app's processes; ring-buffer `ktime_ns` per frame | 100 µs intervals, per-surface stream key (`Surface*` from arm64 x0, busiest surface selected; first frame of any stream yields no interval) |
| `surfaceflinger` | `dumpsys SurfaceFlinger --timestats -dump` 1/s: `totalFrames` diff for fps + `presentToPresent` histogram diff for intervals | 1 ms global bins (`ms*1000+500` µs) when `--latency` is period-only on the ROM |
| `windowmanager` | Root `app_process` running `zx.binder.MainKt --fps` → `WindowManager.registerTaskFpsCallback` (~2 Hz) | **None** — publishes `none 0 0 -`; intervals are never fabricated from the fps number |

`current_fps` format is frozen: `"<fps> <epoch_ms>"`. The manager,
`archdaemon` and this daemon all parse it.

### 7.3 Frametime contract (`API/current_frametime`)

One line, rewritten atomically about once per second:

```
<epoch_ms> <gen> <source> <frames> <sum_us> <hist>
```

| Field | Meaning |
|---|---|
| `epoch_ms` | Wall-clock ms at publish time (stale > 3 s = dead source) |
| `gen` | Monotonic per daemon lifetime (seeded from `epoch_ms` at start); bumps on every source (re)start / switch / failure / gate reopen |
| `source` | `uprobe` \| `sf` \| `none` |
| `frames` / `sum_us` | Cumulative intervals / microseconds within this gen |
| `hist` | Sparse cumulative histogram `idx:count,…` ascending, `-` when empty; 100 µs buckets, idx 5000 = overflow (≥ 500 ms) |

Invariants: `sum(hist) == frames` in every line; counters never
decrease within a gen (a decrease rebases: next snapshot is the new
baseline, no delta). Publishing keeps the line fresh every ~1 s even
with zero new frames, and holds `NO_ACTIVE_GAME` when the gate is
closed.

---

## 8. Runtime paths

Config base: `/data/adb/.config/AZenith` (`<cfg>`).

| Path | Owner / purpose |
|---|---|
| `<cfg>/app_status` | Written here (focused app + system state) |
| `<cfg>/background_apps` | Written here (recent apps + pids) |
| `<cfg>/java.lock` | Instance lock (shared with `archdaemon`) |
| `<cfg>/sysmon.log` | Log + crash info (128 KB, keep 2) |
| `<cfg>/fpsclient.apk` | Staged FPS-client dex (from embedded resolver APK) |
| `<cfg>/fpsclient.log` | FPS-client stdout/stderr (append, survives retries) |
| `<cfg>/debug/codes.<sdk>.<inc>` | Transaction-code cache |
| `<cfg>/debug/fps_counter_alwayson` | `"true"` = FPS without a game |
| `<cfg>/debug/explicit_fps_mthd` | Pin FPS source |
| `<cfg>/API/current_fps` | `"<fps> <epoch_ms>"` (fps client) |
| `<cfg>/API/current_frametime` | Frametime histogram line (this daemon) |
| `<cfg>/API/gameinfo` | Active game (written by `archdaemon`, read as gate) |
| `<cfg>/API/current_modes` | Auto/manual mirror of `persist.sys.azenithconf.AIenabled` |

---

## 9. System properties

| Property | Used for |
|---|---|
| `persist.sys.azenithconf.AIenabled` | Read at startup to seed `API/current_modes` |
| `persist.sys.locale` | Optional label read fallback (`label.rs`) |

`getprop` is used only for the integer increment check in
`codes.rs` (`ro.build.version.sdk/incremental`); everything else is
read over binder, never by shelling out.

---

## 10. Logging

* File: `<cfg>/sysmon.log` (128 KB rotated, keep 2 generations).
* Tag: `daemon` (mirrored to logcat: `adb logcat -s daemon`).
* Levels: `debug` / `info` / `warn` / `error` via
  `crate::log::{debug,info,warn,error}!` macros; controlled by the
  `LIBBINDING_LOG` env var.
* Panic hook writes the panic location to the log before exit.

---

## 11. Self tests

| Flag | What it does |
|---|---|
| `--label <pkg>` | Prints the app label (APK resource parsing) |
| `--recents` | Prints the task-manager recents package list |
| `--focused` | Prints the focused root task package + live pid |
| `--clearlogs` | Removes `sysmon.log` generations |

---

## 12. Credits & license

* Developed by @Zexshia and @rianixia (Telegram).
* Credits: @Kombat, @Kaminarich, @Rem01Gaming.
* Adapted from AZenith (Java companion daemon).
* Licensed under the GNU General Public License v3.0 — see `LICENSE`.

---

## 13. Related documents in this repo

| Document | Purpose |
|---|---|
| `README.md` | Quick start + daemon summary |
| `../archdaemon/DAEMON.md` | Core daemon (consumes `app_status` / `current_fps` / `current_frametime`) |
| `../thermalcore/README.md` | Thermal telemetry collector |
| `../.github/workflows/build.yml` | CI build (Cargo NDK for all Rust binaries) |
| `../.github/scripts/verify.sh` | Version injection (Cargo semver for all binaries except `thermalcore`) |
| `../.github/scripts/compile_zip.sh` | Module packaging (copies Rust binaries into `mainfiles/libs/`) |
