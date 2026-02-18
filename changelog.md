## AZenith 4.3
### Changelog
- Migrate manager from WebUI to Manager APP
- Improved Bypass Charging logic, and added Compatibility Checks, Go to `Tweak Settings > Bypass Charging > Compatibility Check.`
- Added Per-Apps Tweak Settings
- Fix a bug where `JIT Compiler` causing screen freeze at Startup
- Refactoring Disable Thermal Script
- Added more nodes for Bypass Charge
- Improved Daemon flow and fixes some Bugs
  • Added 10s wait after screen off in `Performance Profile` 
  • Adjusted Polling Interval to 1s
  • Revert Daemon state use properties, and use .lock file instead


## AZenith 4.3
### Changelog - CI 1070-b60aaa9
- Fix data in Device Recognition Files

### Changelog - CI 1061-1cd43a4
- Implement native parsing for dumpsys and dropped grep and awk filter
- Improve App Process detection by switching from /proc/{}/cmdline to dumpsys activity activities for more accurate PID detections
- Reduced Daemon loops from 5s all condition to 2s and 700ms in-game
- Adjusted Performance Profiles in Lite mode to minimize lag in game scenario
- Fully rewrite the game preload logic... preloaded all apk files with budget 500MB
- Fixed an issue where the profiler settings did not trigger the Profile changes
- Update WebUI with a cleaner, and modern layout
- Added support for Monet(Material You) Dynamic Color via Webuix/KSU API
- Reworked Applist settings
- Added Save and Load Config/Presets, Supported in: 
   • KernelSU 2.1.1 
   • KernelSU Next CI/Upcoming Updates
   • KowSU
- Added Custom Banner (same compatibility as load config)
- Update Chipset Name recognition databases with 900+ Entries
- Added Device Name recognition databases with 3000+ Entries
- Saved applist now doesn't replace the old one after updating azenith
- Refactor uninstaller script


## AZenith 4.2
### Changelog
- Added ID, KR, JA, JV, and ZN translation in the WebUI
- Added gpu mali 'power_policy' for MediaTek
- Fix an issue where I/O Scheduler setting didn't show up in some device
- Added AI ThermalCore Service by: @rianixia
- Improve I/O Sched Parser in Initialization
- Improve Game Preload Logic, fixed ram overload if user were in game for too long
- Fixed an issue where performance profile always reapplies if Game Preload is active
- Some refining in the WebUI, make the cleaner looks
- Fully fixed random reboot in WebUI
- Improving log message for better readability.
- Fix Game Preload toggle won't checked


## AZenith 4.0
### Changelog
- Underscale settings now using option based on percentage
- Added light theme on webui(follow system)
- Readded Manual/Idle Mode with new logic
- Expose the Profiler Interface in the daemon | access by running "sys.azenith-profiler 1|2|3"
- Drop "dumpsys" detection to reduce overhead
- Fix CPUfreq limiter to correctly apply the selected frequency
- Reduce loop interval from 15sec to 5sec (45sec if Preload is active)
- Improve logging message for more detailed Information 
- Fixed random reboot on WebUI
- Reduced load time when opening WebUI
- Some redesign in the WebUI with new banner, avatar, layout etc...
- Minor optimization in daemon flow...


## AZenith 3.8
### Changelog
- Added I/O Scheduler settings
- Fix an issue where webui causing a random reboot
- Update sf settings based on carlotta render tweak
- Remove button to change profile in Idle mode
- Fix an issue where game preload keep reapplying performance profile while running
- Some optimization in background flow...


## AZenith 3.6
### Changelog
- Re Added Lite mode
- Add Resolution Changer / Underscale
- Color Scheme a.k.a Color boost now applied on boot
- Migrate restover azenith config into persistence properties
- Remove force target opp index
- Remove custom game governor(performance by default)
- Limit cpu frequency now applied in both balanced and powersaves profiles
- Fixed an anomaly where GamePreload left zombie processes
- Frequency now applied periodically to keep its value
- Other improvements in backend services


## AZenith 3.4
### Changelog 
- Major changes in backend flow
- Replace file-based saving value with persistence.prop.
- Add an option to enable or disable Toast notification(enabled by default).
- switch cat with getprop to increase efficiency.
- Resolved issue where cpufreq locked at max in Powersave mode.
- Implement android logcat (system logging) in daemon and sh script, check using "logcat | grep -i azenith"
- Add Disable tracing option.
- Add JIT(Just In Time) Option.
- Updated uninstaller.sh to correctly remove symlinks from /ksu/bin and /ap/bin.
- Daemon state is now stored in system prop, check using "getprop | grep azenith"
- Improving logging messages for better readability
- Restructuring codes...


## AZenith 3.3
### Changelog
- Fix CPU Governor Permission and Cpu Frequency anomalies
- Fix Webui Governor Settings (missing permissions)
- Remove Scheduler settings
- Remove all vm settings
- Remove tcp settings
- Remove Swappines settings
- Improving flow mechanism
- Added tweak script for exynos, tensor, and unisoc
- Reducing module size
- Improving soc recognition logic
- Improve ksu exec logic


## AZenith 3.1
### Changelog
- Add searchbar for gamelist edit in WebUi
- Update mediatek disable thermal
- Refactoring codes...


## AZenith 3.0
### Changelog
- Compile with ndk-build to support MultiArch
- Rebrand to universal performance module
- Fix missing function call in script
- Add module banner for KernelSU Next
- Use symlink for ksu/apatch
- Fix cpu recognition not detecting some specific chipset
- Add verify sha256
- Fix missing custom governor file in installation


## AZenith 2.8
### Changelog
- Update : Initial Support for Snapdragon
- Change : FSTrim is not set on boot now
- Update : Added an Option to Change Performance Governor
- Update : Added Lock Mode / Disable AI, Run Performance and Powersave Profile through the WebUI
- Change : Fix An issue where WebUI is lag on some devices
- Update : Disable Vsync now supports 60hz/90hz/120hz
- Change : Refactoring script and other improvement on flow and Logic


## AZenith 2.6
### Changelog
- Change : Change the logic of the slider in webUi and optimize it for low end devices
- Change : Compress webui and convert it gif to Webp
- Change : Restructure the webui script to make it lighter
- Change : increase the interval for Real time monitoring


## AZenith 2.5
### Changelog
- Update : Added color scheme settings based on Zirelia 1.0
- Change : Removed Lite Mode
- Change : Migrate to C Daemon based on Encore 4.5 (Thanks to Rem01Project for OpenSource EncoreDaemon)
- Fix : Fixed a condition where cpu frequency won't set to default after using ECO Mode
- Update : Added toggle to enable GPU Mali Scheduling
- Update : Added toggle to enable FPSGO and GED Parameters (it was applied by default before)
- Update : Added toggle to enable Scheduler Tunes (it was applied by default before)
- Change : Removed ML Prior and Replace it to App priority settings
- Update : Added Real time monitoring Ram Usage and Cpu Frequency on WebUI
- Change : Refine WebUI a little bit to improve UX
- Change : Change UP_RATE limit from 7000 => 7500
- Change : Change DOWN_RATE limit from 15000 => 14000
- Change : Increase Swappiness from 10 => 20
- Change : Increase nr_request from 64 => 128
- Change : Increase read_ahead_kb from 128 => 256
- Change : Decrease dirty_background_ratio from 35 => 10
- Change : Decrease dirty_ratio from 30 => 20
- Change : Decrease vfs_cache_pressure from 120 => 80
- Change : Decrease dirty_expire_centisecs from 400 => 300
- Change : Decrease dirty_writeback_centisecs from 6000 => 3000
- Change : Increase stat_interval from 10 => 20
- Change : Disable compaction_proactiveness from 1 => 0
- Change : Disable watermark_boost_factor from 1 => 0
- Change : Decrease watermark_scale_factor from 50 => 20
- Change : Decrease vfs_cache_pressure from 100 => 40
- Change : Decrease perf_cpu_time_max_percent from 3 => 1


## AZenith 2.4
### Changelog
- Changes : Redesign the WebUI a little bit to enhance stability and user experiences
- Updates : Add 2 new profiles, ECO Mode(Powersave) and Balanced Performance(Lite Mode)
- Updates : Remove toggle to force performance profile manually because it's buggy and unstable
- Fixes : Fix a problem where webui won't load after restarting Daemon
- Fixes : Restarting Daemon now doesn't take times and restart immediately (It took 45 Seconds in AZenith 2.0)
- Changes : Remove button to Kill the service since we don't really have to use it
- Changes : Move all startup process from service.sh to main AZenith Process for faster process and reduce child process
- Update : Add new logic to change from Balanced profile to ECO Mode and vice versa
- Change : New Logging method for better Readability, I also added Preload Log to check all game libraries that has been processed by VMT
- Update : New Logic to change profiler, using 2 different dumpsys in different flow but still in the same loop, this is prevent a bug where performance profile always run without opening any games.
- Changes : Daemon now run directly to improve responsiveness and faster process
- Changes : Move all AZenith file libraries from /AZenith/libs to /AZenith/system/bin
- Changes : Merge some small process to one single flow
- Updates : Add toggle to enable Lite Mode (balanced Performance)
- Changes : Limit Max Frequency now only work on ECO Mode and Lock High Frequency works only in High Performance Mode
- Updates : Add new path to bypass charging : /mt-battery/disable_charger


## AZenith 2.0
### Changelog
- Increase Loop delay to 35 Seconds when Using GamePreload to reduce usage
- Fix bootloop issue on some devices
- Fix Bypass Charge Checking when Installing Module
- Adjust Surfaceflinger Value
- Removed System.prop 
- Bypass charge now only supports Specific Devices!


## AZenith 1.9
### Changelog
- Introducing Game Preload, Preload game libs to memory to reduce load time and minimize lag
- Ram Freed, Kill background apps when entering performance profiles to reduce ram usage
- WebUi: added loading screen on WebUI, let everything to load before accessing WebUI
- WebUi: Now chipset automatically detect name, it'll use it's marketing name instead using the codename
- Removed the script to kill mlbb when it's in the background for too long
- Adjust the monitoring loops to 15 S
- Drop PID detection and using dumpsys activity recents instead
- Refining Module Logging, now it has 2 logging files, /data/AZenith/AZenith.log && /data/AZenith/AZenithMon.log thanks to @kanaochar


## AZenith 1.8 R2
### Changelog
- Fix Profiler, some settings wouldn't work without this one


## AZenith 1.8 R1
### Changelog
- Optimizing Script
- Added a script to clear background apps on Performance Profiles


## AZenith 1.7
### Changelog
- Optimize Performance Script and Tweak
- Added Feature to Underclock CPU Frequency
- Rebrand to Universal MediaTek Modules
- Optimize Monitoring Service
- Fix a bug where service won't start after being restarted
- Added KillLogger
- Fix a bug where ML High Prior always killed the Monitoring Service


## AZenith 1.2
### Changelog
- Fix a bug where service won't start again after being disabled
- Fix Volt Opt wont save the Value before Disabling it
- Adjust monitoring loop to 10 Seconds
- Change some shell Notification
- Reworked Something in WebUI
- Droped Kill Logger
- Some rework on Monitoring Service (Background Service)


## AZenith 1.0
### Changelog
- Initial Release on Github
- Sync with Encore 2.3 WebUi
- Added FSTrim (Adjustable in Webui)
- Fixed Bypass Charge won't active in Perf Mode
- Add Zenith Thermal (Adjustable in Webui)
- Add Disable Vsync (Can be Applied in Webui)
- Add Kill Logger
- Adjustable Performance mode (Automatically kill the Service)
- Fix a bug PID is running twice when restarting services
- Restarting Service now will took 15 Seconds until its start the service.
