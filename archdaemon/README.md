# ArchDaemon — AZenith Core Daemon

ArchDaemon is the core daemon of AZenith — the single system-level
performance service that profile-switches, handles bypass charging,
writes game status to the manager, displays notifications and toasts,
manages logs, and monitors foreground processes.

It reads app status from the App Companion API files:

- Foreground apps, app name, recent apps
- DND status, battery saver, screen state
- Battery level, charging status
- Device refresh rates

ArchDaemon is the bridge between the apps and the underlying binaries.

Developed by [@Zexshia](https://t.me/Zexshia) and [@rianixia](https://t.me/rianixia) on Telegram.

---

## Quick Start

### 1. Build

```sh
cargo ndk -t arm64-v8a -t armeabi-v7a build --release
```

Working directory: `archdaemon/` (crate `azenith-service`, edition 2024).
For debug builds, drop `--release`.

### 2. Deploy

Copy `target/aarch64-linux-android/release/azenith-service` to
`/data/adb/modules/AZenith/system/bin/sys.azenith-service` and
restart the service.

---

## Credits

- [@Kombat](https://t.me/Kombat)
- [@Kaminarich](https://t.me/Kaminarich)
- [@Rem01Gaming](https://t.me/Rem01Gaming)

---

## Full Docs

See [DAEMON.md](DAEMON.md) for the complete technical reference
(architecture, startup sequence, properties, runtime paths, FPS recorder, and more).
