<p align="center">
  <img src="https://github.com/user-attachments/assets/a3ac7f35-ff7e-4d73-8952-0fcd985a6f26" alt="AZenith Banner" width="100%">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Status-Stable-green?style=for-the-badge" alt="Status">
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge" alt="License">
  <img src="https://img.shields.io/badge/Android-11+-blue?style=for-the-badge&logo=android" alt="Android Version">
</p>

---

## Overview

**AZenith** is an All-In-One (AIO) system-optimization Magisk module for
Android. It pairs two Rust daemons with a Material 3 Expressive
manager app: **archdaemon** (profiles, bypass charging, FPS Recorder)
and **sysdaemon** (pure-binder app monitor + FPS client). Together they
auto-switch performance profiles when a game runs, tune kernel
scheduling, record real frametime stats, and put an in-game Game Space
overlay on screen — without shell-forking in the hot path.

---

## Features

### Game Space

A landscape, gamepad-first shell built on Material 3 Expressive —
smooth, playful UI with spring animations, dynamic colors and
notification-style cards. Launched from the manager, then handed over
to the overlay service (`ZXSidePanelService`) for the rest of the
session. Designed for gamepad users: D-Pad / joystick navigation,
A select, B back, L1/R1 switch tab, no touch required.

- **Perfect dashboard in one place** — game library tiles, app tiles,
  spotlight, BGM playlist with now-playing controls, wallpapers,
  status trophies, battery, service PID, games tracked, FPS, CPU/GPU
  load and temps all on one landscape screen
- **Ambient music** — customizable BGM playlist (shuffle / repeat /
  sequential) with volume control and long-press quick actions
- **Fun interface** — expressive shapes, dynamic Material You
  theming, spring micro-animations, boot hand-off sequence
  (linking, scan, tune, ready), quick toggles for profile,
  refresh rate and tweaks
- Settings: appearance (sidepanel side / sensitivity / length /
  thickness / opacity / blur, custom images), audio, general (boost
  on launch, renderer, refresh rate, frame-rate target, resolution
  downscale), tweaks (FPSGO, GED, WAL, SFL, thermal, scheduler),
  and About
- Floating app windows on top of the game (open from inside the shell)
- Crosshair reticle overlay

### Sidepanel + FPS Monitor

Edge-bar + expandable Material 3 Expressive dock (`ZXSidePanelService`):

- Handle bar (swipe-in to expand, vertical drag to reposition,
  configurable length/thickness/side) — shown only for games in your
  configured list, app-lunch list, or when force-sidepanel is on
- **FPS Meter** with small `FPS` badge flush left of the number,
  frame time (`1000 / fps` ms) and estimated 1% Low
- **FPS Recorder** — one-tap Start / Stop (record pill with ripple,
  session timer); writes CSV + JSON summaries with real `ft_avg` /
  `ft_max` / `low1` / `low01` / `jank` from the frametime histogram
- Widgets: FPS, CPU, GPU, temp, ping, battery, profile (customizable
  layout, HUD offsets, global + per-app layouts)
- Quick Apps row, brightness/volume sliders, auto-mode toggle,
  profile switcher, toasts

### Per-App Tuning

Per detected game (`ZXGameSpaceAppSettings`), switching on game focus:

- Refresh rate target (per app, from the device's mode list)
- Resolution downscale (per app, `resolution_downscale` 0–100%)
- Target FPS (`resolution_fps` slider)
- Renderer override (default / SkiaVulkan / SkiaGLES / software)
- Display filter / colour scheme (preset, per app)
- Lite Mode (cap at ~90% CPU freq, mid-frequency ladder)
- DND during gaming, App Priority (process priority boost)
- Game Preload (libs + page-cache preload, with budget)
- Resolution downscale badge always shows the live divider

### Profiles & Governor Tweaks

Auto profiles (Performance / Balanced / ECO) switch on game focus;
manual mode in Settings disables Auto Mode:

- Per-profile CPU governor (default / performance / powersave),
  I/O scheduler (per profile), Mali GPU governor (per profile)
- CPU frequency limiter (`freqoffset` % ceiling) and CPU limit
- Lite mode, Clear background apps, Fstrim, DND on gaming,
  App Priority, Game Preload + budget
- Custom governor, custom I/O, custom Mali overrides per profile
- `disabletweak` master switch

### Thermalcore

Intelligent thermal throttling management (separate Rust crate
`thermalcore`): watches thermal zones, adapts limits to keep a stable
FPS floor instead of a thermal crash, with `persist.sys.rynx.*` props
(threshold floor, audit logs, actuator mode, gradient alerts).

### Bypass Charging

78 sysfs nodes (common / MediaTek / Oplus / QCOM) for bypass charging,
with charging-state dynamic bypass, compatibility diagnosis
node-by-node, custom threshold (`bypasschgthreshold`) and custom node
path.

### FPS Sources & Frametime

`sysdaemon` supervises three sources in automatic fallback order
(frametime-capable first):

1. **uprobe** — eBPF on `libgui` `queueBuffer` family; 100 µs
   per-surface intervals (64-bit only, `aya`)
2. **surfaceflinger** — `timestats -dump` (`totalFrames` diff for fps
   + `presentToPresent` histogram, 1 ms bins, when `--latency` is
   period-only on the ROM)
3. **windowmanager** — `TaskFpsCallback` (~2 Hz, rate only)

Publishes cumulative frametime histograms to
`API/current_frametime` (`hist-sum == frames`, gen bumps on every
source restart) — consumed by archdaemon for the recorder's
`low1` / `low01` / `jank`. `current_fps` format stays
`"<fps> <epoch_ms>"` (frozen for the manager + archdaemon).

### Preference Tweaks

`binpreferenced` (toggles under `persist.sys.azenithconf.*`):

- JIT (speed-profile compile), schedtunes, WALT tunes
- FPSGO & GED parameters (MediaTek), Mali scheduler
- SFL latency, DThermal (thermal engine), trace/logd kill-switches
- Fstrim, custom thermal, display colour presets

### Cosmetic & UX

- Display colour schemes, custom themes, accent colors, Material You
- Sound effects (tap/scroll/notice/cancel volume), haptic feedback
- App icon (shortcut / launcher alias), banner / boot jingle
- BGM playlist, custom sidepanel images, blur/opacity controls

---

## Installation

Magisk module — install through your root manager (KernelSU /
Magisk):

1. Flash the release zip.
2. Reboot.
3. Open AZenith, grant overlay permission, tap **Enter Game Space**.

---

## For Developers (ROM Integration)

**ROM-Integrated AZenith**: system-level service for custom ROMs —
single `vendor.azenith-service` binary, AIDL interface, init rc,
sepolicy. See the
[RomIntegratedAZenith](https://github.com/Liliya2727/RomIntegratedAZenith)
repository.

---

## Contributing & Bug Reports

We welcome contributions!

- **Report Bugs:** open an issue with a log.
- **Feature Requests:** open an issue to discuss.
- **Pull Requests:** fork, create a branch, submit a PR with a clear
  description.

---

## Credits & Acknowledgements

| Role | Developer |
| :--- | :--- |
| **Author** | [@Zexshia](https://github.com/Liliya2727) |
| **Collaborators** | [@rianixia](https://github.com/ryanistr), [@kanaochar](https://github.com/kanaodnd) |

| Area | Credits |
| :--- | :--- |
| App Translation & Internationalization | @YIDYIF |
| Tweak Sources | @Rem01Gaming, @MiAzami, @KanagawaYamadaVTeacher, @ShiraXblood, @Laynsb, @Koneko_dev |
| Game Preload | @HoyoSlave, @KutuMoba, @Feravolt, @iamlooper |

---

## Stay Updated

<p align="left">
  <a href="https://t.me/ZeshArch">
    <img src="https://img.shields.io/badge/Telegram-Channel-blue?style=for-the-badge&logo=telegram" alt="Telegram Channel">
  </a>
  <a href="https://t.me/Zexshia">
    <img src="https://img.shields.io/badge/Support-Author-red?style=for-the-badge&logo=telegram" alt="Support">
  </a>
</p>

---

## License

This project is licensed under the **Apache License, Version 2.0** —
see [LICENSE](LICENSE) for the full text and [NOTICE.md](NOTICE.md)
for third-party notices.
