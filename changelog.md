## Changelog V2.8 (R2864)
- Update : Initial Support for Snapdragon
- Change : FSTrim is not set on boot now
- Update : Added an Option to Change Performance Governor
- Update : Added Lock Mode / Disable AI, Run Performance and Powersave Profile through the WebUI
- Change : Fix An issue where WebUI is lag on some devices
- Update : Disable Vsync now supports 60hz/90hz/120hz
- Change : Refactoring script and other improvement on flow and Logic


## Changelog V2.6 (R2648)
- Change : Change the logic of the slider in webUi and optimize it for low end devices
- Change : Compress webui and convert it gif to Webp
- Change : Restructure the webui script to make it lighter
- Change : increase the interval for Real time monitoring


## Changelog V2.5 (R2509)
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


## Changelog V2.4 (R2467)
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


## Changelog V2.0 (V2044)
- Increase Loop delay to 35 Seconds when Using GamePreload to reduce usage
- Fix bootloop issue on some devices
- Fix Bypass Charge Checking when Installing Module
- Adjust Surfaceflinger Value
- Removed System.prop 
- Bypass charge now only supports Specific Devices!


## Changelog V1.9 (V1927)
- Introducing Game Preload, Preload game libs to memory to reduce load time and minimize lag
- Ram Freed, Kill background apps when entering performance profiles to reduce ram usage
- WebUi: added loading screen on WebUI, let everything to load before accessing WebUI
- WebUi: Now chipset automatically detect name, it'll use it's marketing name instead using the codename
- Removed the script to kill mlbb when it's in the background for too long
- Adjust the monitoring loops to 15 S
- Drop PID detection and using dumpsys activity recents instead
- Refining Module Logging, now it has 2 logging files, /data/AZenith/AZenith.log && /data/AZenith/AZenithMon.log thanks to @kanaochar


## Changelog V1.8 (1825)
- Fix Profiler, some settings wouldn't work without this one


## Changelog V1.8 (1824)
- Optimizing Script
- Added a script to clear background apps on Performance Profiles


## Changelog V1.7 (1723)
- Optimize Performance Script and Tweak
- Added Feature to Underclock CPU Frequency
- Rebrand to Universal MediaTek Modules
- Optimize Monitoring Service
- Fix a bug where service won't start after being restarted
- Added KillLogger
- Fix a bug where ML High Prior always killed the Monitoring Service


## Changelog V1.2 Release (1216)
- Fix a bug where service won't start again after being disabled
- Fix Volt Opt wont save the Value before Disabling it
- Adjust monitoring loop to 10 Seconds
- Change some shell Notification
- Reworked Something in WebUI
- Droped Kill Logger
- Some rework on Monitoring Service (Background Service)


## Changelog V1.0 Release (1014)
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
