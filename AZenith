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

**AZenith** is an All-In-One (AIO) Universal Performance module for
Android — built to work across devices and ROMs, and designed to
respect your preferences: nothing is forced, every tweak is opt-in and
stays fully under your control.

It pairs two Rust daemons with a Material 3 Expressive manager app:
**archdaemon** (profiles, bypass charging, FPS Recorder) and
**sysdaemon** (pure-binder app monitor + FPS client). Together they
auto-switch performance profiles when a game runs, tune kernel
scheduling, record real frametime stats, and put an in-game Game Space
overlay on screen — without shell-forking in the hot path.

---

## Features

### Game Space

A landscape, gamepad-first shell built on Material 3 Expressive —
smooth, playful UI with spring animations, dynamic colors and
notification-style cards. Launched from the manager, then handed over
to the overlay service for the rest of the session. Designed for
gamepad users: D-Pad / joystick navigation, A select, B back, L1/R1
switch tab, no touch required.

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

Edge-bar + expandable Material 3 Expressive dock — every control
you need lives right inside the game: profile switch, battery, temps
and framerate without ever leaving the session.

- Handle bar (swipe-in to expand, vertical drag to reposition,
  configurable length/thickness/side) — shown only for games in your
  configured list, app-lunch list, or when force-sidepanel is on
- **In-game access control** — switch profile, check battery and
  temps, and watch framerate live, all from the dock
- **FPS Meter** with small `FPS` badge flush left of the number,
  frame time (`1000 / fps` ms) and estimated 1% Low
- **FPS Recorder** — one-tap Start / Stop (record pill with ripple,
  session timer); writes CSV + JSON summaries with real `ft_avg` /
  `ft_max` / `low1` / `low01` / `jank` from the frametime histogram
- **Customizable layout** — widgets (FPS, CPU, GPU, temp, ping,
  battery, profile) with HUD offsets, global + per-app layouts
- **Floating apps** — open other apps in floating windows on top of
  the game from inside the dock
- Quick Apps row, brightness/volume sliders, auto-mode toggle,
  profile switcher, toasts

### Per-App Tuning

Per-app tweaks override every global setting for the selected game —
set each parameter once per game and it applies on game focus,
falling back to the global values otherwise:

- Per-app Lite Mode (cap at ~90% CPU freq, mid-frequency ladder)
- Per-app rendering (renderer override: default / SkiaVulkan /
  SkiaGLES / software)
- Per-app refresh rate (target from the device's mode list)
- Per-app downscaler (resolution downscale 0–100% + target FPS slider)
- Per-app DND (do-not-disturb during gaming)
- App Priority (process priority boost)
- Game Preload (libs + page-cache preload, with budget)
- Resolution downscale badge always shows the live divider

_(Display config is not per-app — there is no per-app display filter.)_

### Profiles

AZenith switches profile automatically depending on what you need —
gaming in foreground → Performance, battery saver on → ECO,
everything else → Balanced (with a 10 s screen-off grace and 30 s
foreground-away grace before dropping back).

You can also take manual control: turn off **Auto Mode** in Settings
and pick **Performance**, **Balanced** or **ECO** yourself whenever
you want.

### Custom Governor

AZenith lets you set a custom CPU governor **per profile** — pick
your own governor for Performance, Balanced and ECO instead of the
stock defaults:

- Custom CPU governor per profile (`custom_*_cpu_gov`)
- Custom I/O scheduler per profile (`custom_*_IO`)
- Custom Mali GPU governor per profile (`custom_*_maligpu_gov`)
- Stock defaults stay untouched unless you override them; `balanced`
  can also restore the saved baseline governor
- `disabletweak` master switch

### Thermalcore

Intelligent thermal throttling — watches CPU/GPU temps and adapts
limits so you hold a stable FPS floor instead of thermal-crashing.

### Bypass Charging

Built-in bypass charging for supported devices:

- Diagnostic check that tests whether your device actually supports
  the feature before enabling it
- Adjustable charging threshold to match your battery health usage
- Charging-state dynamic bypass while gaming

### Preference Tweaks

AZenith ships extra add-on tweaks you can enable or disable whenever
you like — flip each one on/off from the tweaks screen and it applies
immediately:

- schedtunes, WALT tunes
- FPSGO & GED parameters (MediaTek), Mali scheduler
- SFL latency, display colour presets

---

## Installation

Magisk module — install through your root manager (KernelSU /
Magisk):

1. Flash the release zip.
2. Reboot.
3. Enjoy

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
