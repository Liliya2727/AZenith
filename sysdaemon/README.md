# sysdaemon — AZenith app monitor

A pure-binder native daemon. It observes Android system state through binder transactions only — no shell forking, no `cmd`/`dumpsys` parsing in the hot path, no profile logic. It only observes and publishes:

*   `app_status` — focused app (package/pid/uid) + screen/battery/zen/refresh-rate state
*   `background_apps` — recently used user apps + their live pids
*   `java.lock` — instance lock (shared with the C daemon)
*   `sysmon.log` — log, crash information, size-bounded and rotated

It runs as root (forks into the background), talks to `system_server` via `libbinder_ndk`, and resolves binder transaction codes at runtime via the embedded `binder_resolver.apk` — so it adapts to ROM differences instead of hard-coding transaction numbers.

---

## What this is 

**It is:** a root daemon that registers an `IProcessObserver` with ActivityManager, polls `IPowerManager`/`INotificationManager`/`IBatteryPropertiesRegistrar` over binder (500 ms cycle, pure transacts), reads the task manager's recents list via `getTasks`/`getRecentTasks`, and atomically publishes `app_status` / `background_apps` whenever something actually changes. An idle daemon does not touch the disk.

**It isn't:** a profile engine, a game booster, or a magic performance tool. It does not apply CPU governors, does not kill background apps, does not change refresh rates — it only watches and publishes. The C daemon (`archdaemon`) and the manager app consume these files to make decisions.

---

## Why it exists

The Java companion daemon (`AppMonitor.kt`) ran inside the manager app's process, forked `cmd`/`dumpsys` for refresh-rate and recents lookups, and mixed profile logic with observation. `sysdaemon` splits concerns:

1.  **Observation only** — no profile logic, no game detection, no boosts.
2.  **No shell forks** — refresh rate comes from `IDisplayManager.getDisplayInfo()` parcel scanning, not `dumpsys`; recents come from binder `getTasks`/`getRecentTasks`.
3.  **Transaction-code resilience** — binder codes change between Android versions (and between OEM ROMs). The embedded resolver APK looks them up at runtime and caches them per `(SDK, incremental build)`, so the same binary works on Android 11 through 17 without recompilation.

---

## How it works

### Startup sequence

```
main()
 ├─ refuse non-root
 ├─ self-tests (--label / --recents / --focused)
 ├─ redirect stdio to sysmon.log
 ├─ daemon()  (fork + setsid + chdir("/"))
 ├─ take java.lock (POSIX record lock, single instance)
 ├─ capture_previous()   (read last focused_app / refresh_rate from files)
 ├─ load_codes()         (binder_resolver.apk → transaction codes, cached)
 ├─ bind AMS + IPackageManager
 ├─ linkToDeath on AMS   (exit if system_server restarts)
 ├─ observer::start()    (IProcessObserver channel + worker)
 ├─ recents poller       (getTasks / getRecentTasks every 2 s)
 ├─ fps::start()         (frame-rate client for the focused task)
 ├─ seed()               (/proc scan → initial foreground guess)
 ├─ focused::start()     (RootTaskInfo poll to correct missed AMS callbacks)
 └─ ABinderProcess_joinThreadPool()  (blocks forever)
```

### The three layers

| Layer | Files | What it does |
|-------|-------|--------------|
| **Sense** | `observer.rs`, `procs.rs`, `status.rs`, `recent.rs`, `focused.rs` | Reads AMS process events, /proc, binder status values, task-manager recents, RootTaskInfo |
| **State** | `state.rs` | Holds focused app, recent list, process table, system stats; snapshots atomically for publishing |
| **Publish** | `state.rs` (`publish()`) | Writes `app_status` / `background_apps` only when content changed (atomic tmp + rename) |

### Observation sources

| Source | Binder service / mechanism | What it gives |
|--------|---------------------------|---------------|
| Process lifecycle | `IProcessObserver` (`registerProcessObserver`) | `onForegroundActivitiesChanged`, `onProcessStarted`, `onProcessDied` |
| Screen on/off | `IPowerManager.isInteractive()` (poll + `IDisplayManagerCallback`) | `screen_awake` |
| Battery saver | `IPowerManager.isPowerSaveMode()` (poll) | `battery_saver` |
| Zen mode | `INotificationManager.getZenMode()` (poll) | `zen_mode` (0=off,1=priority,2=none,3=alarms) |
| Battery level | `IBatteryPropertiesRegistrar.getProperty()` (poll) | `battery_level`, `is_charging` |
| Refresh rate | `IDisplayManager.getDisplayInfo()` (parcel scan, 5 s cycle + display events) | `refresh_rate` (Hz) |
| Recents | `IActivityTaskManager.getTasks()` / `IActivityManager.getRecentTasks()` (2 s poll) | `background_apps` package list |
| Focused task | `IActivityTaskManager.getFocusedRootTaskInfo()` (1 s poll) | Corrects focus when AMS callbacks miss a switch |
| /proc | Direct `/proc` scan | Process table, `oom_score_adj`, initial foreground guess |

### app_status format

```
focused_app {pkg} {pid} {uid}
 screen_awake {0|1}
 battery_saver {0|1}
 zen_mode {0|1|2|3}
 battery_level {0..100|-1}
 is_charging {0|1}
 app_name {label}
 refresh_rate {Hz}
```

Published atomically to `<cfg>/app_status`. `focused_app none 0 0` when nothing is in the foreground. The label lookup calls `system_server` (~50 ms) and is snapshot-validated: if the focused app changed while the label was being read, the publish is skipped and the new app's event triggers a fresh publish — so the name and pid always belong to the same app.

### background_apps format

```
{pkg} {pid} {uid}\n
...
```

One line per recently used user app, focused app first. A live process is preferred; if the process died but the task is still in the recents, the last known pid/uid is kept so the task does not disappear. An app is removed only when its task leaves the recents **or** its last process is destroyed — Android killing a cached process while the task remains in recents does **not** drop it.

### background_apps vs process death

Android kills cached processes all the time while their tasks stay in the recents. The daemon therefore distinguishes:

*   **Process died, task still in recents** — entry stays in `background_apps` (with last known pid if no live process remains).
*   **Process died, task gone from recents** — entry is removed immediately.
*   **Task swiped from recents** — entry is removed on the next recents poll.

On Android 16 (and several ROMs), `getRecentTasks`/`getTasks` return only the focused task. Blindly replacing the recent list on those replies would wipe every background app on each focus change. The daemon only replaces the list when the reply contains more than the focused app; focused-only or empty replies are treated as "no useful recents data" and the list is left untouched.

---

## FPS client (`fps.rs`)

A separate frame-rate source for the focused task, publishing `<fps> <epoch_ms>` to `<cfg>/API/current_fps`. Three sources, tried in order:

| Source | Mechanism | Notes |
|--------|-----------|-------|
| `windowmanager` | Root `app_process` hosting `WindowManager.registerTaskFpsCallback` | Needs `ACCESS_FPS_COUNTER` (granted to uid 0); framework pushes ~2 Hz |
| `uprobe` | eBPF uprobe on `libgui`'s `Surface::queueBuffer` family (aya) | 64-bit only; per-process, independent of SurfaceFlinger |
| `surfaceflinger` | `dumpsys SurfaceFlinger --timestats -dump`, differentiated `totalFrames` | 1 s poll; works where TaskFpsCallback is unavailable |

The source only runs while a game is active (`<cfg>/API/gameinfo`) or `always-on` is set (`<cfg>/debug/fps_counter_alwayson == "true"`). Both are watched with `inotify`. A method can be pinned via `<cfg>/debug/explicit_fps_mthd` (`use_windmanager`, `use_uprobe`, `use_sf`, or empty for automatic). While the gate is closed, `current_fps` holds `NO_ACTIVE_GAME` so the manager can tell "no game" from "daemon dead".

---

## System properties & config files

| Path | Purpose |
|------|---------|
| `<cfg>/app_status` | Focused app + system state (written by this daemon) |
| `<cfg>/background_apps` | Recently used apps + pids (written by this daemon) |
| `<cfg>/java.lock` | Instance lock (shared with C daemon) |
| `<cfg>/sysmon.log` | Daemon log (stdout/stderr redirected here) |
| `<cfg>/debug/codes.<sdk>.<inc>` | Cached binder transaction codes |
| `<cfg>/API/current_fps` | FPS for focused task (written by fps client) |
| `<cfg>/API/gameinfo` | Active game package (written by C daemon, read by fps gate) |
| `<cfg>/API/current_modes` | Auto/Manual mode mirror (written by this daemon from `persist.sys.azenithconf.AIenabled`) |
| `<cfg>/debug/fps_counter_alwayson` | `"true"` keeps FPS running without a game |
| `<cfg>/debug/explicit_fps_mthd` | Pin FPS source (`use_windmanager` / `use_uprobe` / `use_sf`) |

`persist.sys.azenithconf.AIenabled` is read at startup to seed `API/current_modes`; after that the manager app owns both files.

---

## Building

```bash
# Android (arm64-v8a) — requires Android NDK
cargo build --release --target aarch64-linux-android
# or use the repo's compile wrapper if present

# Host (linting only)
cargo check
```

Release profile uses `opt-level = 'z'`, LTO, single codegen unit, `panic = 'abort'`, and stripping for a small, fast binary.

---

## Deploying on device

```bash
# The binary must run as root
adb push target/aarch64-linux-android/release/sysdaemon /data/local/tmp/
adb shell chmod 755 /data/local/tmp/sysdaemon

# Run (daemon()s into the background)
adb shell su -c '/data/local/tmp/sysdaemon'

# Verify
adb shell cat /data/adb/.config/AZenith/app_status
adb shell cat /data/adb/.config/AZenith/background_apps
adb shell cat /data/adb/.config/AZenith/sysmon.log | tail
```

Watch logs:
```bash
adb logcat -s AZenith
```

Typical `app_status`:
```
focused_app com.miHoYo.GenshinImpact 1234 10123
 screen_awake 1
 battery_saver 0
 zen_mode 0
 battery_level 84
 is_charging 0
 app_name Genshin Impact
 refresh_rate 120
```

Typical `background_apps`:
```
com.miHoYo.GenshinImpact 1234 10123
com.instagram.android 5678 10045
com.whatsapp 9012 10056
```

---

## Self tests

The binary accepts flags for quick verification without running as a daemon:

| Flag | What it does |
|------|--------------|
| `--label <pkg>` | Prints the localized app label (via APK resource parsing) |
| `--recents` | Prints the task-manager recents package list |
| `--focused` | Prints the focused root task package + live pid |
| `--clearlogs` | Rotates/clears `sysmon.log` |

---

## Contributing

This is a personal project that happens to be open. If you want to hack on it:

1.  **Test on real hardware** — binder transaction codes and parcel layouts vary by ROM; the resolver handles code differences, but parcel scanning (`getDisplayInfo`, task parcels) is version-sensitive.
2.  **Keep dependency tree lean** — `aya` is 64-bit only and scoped accordingly; avoid adding heavy async runtimes.
3.  **Preserve the release profile** — LTO + single codegen unit + strip is non-negotiable for binary size and cold-start latency.
4.  **Never add shell forks to observation paths** — if a value can't be read over binder, read it from `/proc` or sysfs, not `cmd`/`dumpsys`.
5.  **Run `cargo check`** — both the default target and `aarch64-linux-android` must pass.

Open an issue or PR if you find a bug or have a focused improvement.

---

## License

Licensed under the **GNU General Public License v3.0 only** (GPL-3.0-only) — see [LICENSE](LICENSE) for the full text.

This is **copyleft**: if you redistribute the daemon (in source or binary form), you must also make the Corresponding Source available under the same GPL v3 terms. You may not re-license it as proprietary or under a weaker permissive license without replacing all copyrighted contributions.

## Access

`sysdaemon` runs as **root** and talks to `system_server` over libbinder. Treat it as privileged system software:

*   **Source access.** Because this is GPL v3, recipients of the binary are entitled to request the Corresponding Source. If you build and distribute modified binaries, you must offer the source under GPL v3 as well.

## Contributing

This project is licensed under **GPL v3 only**. All contributions are accepted under the same license — by submitting code, you agree to license your contribution under GPL v3 and affirm that you have the right to do so.

1.  **Test on real hardware** — binder transaction codes and parcel layouts vary by ROM; the resolver handles code differences, but parcel scanning (`getDisplayInfo`, task parcels) is version-sensitive.
2.  **Keep dependency tree lean** — `aya` is 64-bit only and scoped accordingly; avoid adding heavy async runtimes.
3.  **Preserve the release profile** — LTO + single codegen unit + strip is non-negotiable for binary size and cold-start latency.
4.  **Never add shell forks to observation paths** — if a value can't be read over binder, read it from `/proc` or sysfs, not `cmd`/`dumpsys`.
5.  **Run `cargo check`** — both the default target and `aarch64-linux-android` must pass.
6.  **Add the GPL copyright header** to every new source file (see the header in `src/main.rs`).

Open an issue or PR if you find a bug or have a focused improvement.
