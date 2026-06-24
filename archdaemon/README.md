# ArchDaemon

Core Program of AZenith

This C AZenith Service is Based on Encore Tweaks Daemon 4.0, ArchDaemon is the central engine that controls all AZenith operations to ensure it runs flawlessly. Its handling everything in AZenith including: Switching profiles, Handling bypass charging logic, Writing game statuses to the manager, Displaying notifications and toast messages, Writing and managing logs, Monitoring crucial processes.

This Daemon is reading app status from App Companion API File
- Foreground Apps
- App Name
- Recent Apps
- User current DND status
- Battery Saver status
- Screen state
- Battery level
- Charging status
- Device Refresh rates

Ultimately, ArchDaemon serves as the bridge that allows the apps and the underlying binaries to work together seamlessly.