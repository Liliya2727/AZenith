#!/bin/sh

#
# Copyright (C) 2024-2025 Zexshia
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Remove Persistent Properties
props="persist.sys.azenith.state \
persist.sys.azenith.debugmode \
persist.sys.azenith.service \
persist.sys.azenithconf.justintime \
persist.sys.azenithconf.disabletrace \
persist.sys.azenithconf.logd \
persist.sys.azenithconf.DThermal \
persist.sys.azenithconf.SFL \
persist.sys.azenithconf.malisched \
persist.sys.azenithconf.fpsged \
persist.sys.azenithconf.schedtunes \
persist.sys.azenithconf.clearbg \
persist.sys.azenithconf.bypasschg \
persist.sys.azenithconf.APreload \
persist.sys.azenithconf.iosched \
persist.sys.azenithconf.cpulimit \
persist.sys.azenithconf.dnd \
persist.sys.azenithconf.AIenabled \
persist.sys.azenithdebug.soctype \
persist.sys.azenithconf.vsync \
persist.sys.azenithconf.freqoffset \
persist.sys.azenithconf.schemeconfig \
persist.sys.azenithdebug.freqlist \
persist.sys.azenithdebug.vsynclist \
persist.sys.azenith.custom_default_cpu_gov \
persist.sys.azenith.custom_game_cpu_gov \
persist.sys.azenith.custom_powersave_cpu_gov \
persist.sys.azenith.default_cpu_gov \
persist.sys.azenithconf.scale \
persist.sys.azenithconf.hgsize \
persist.sys.azenithconf.wdsize \
persist.sys.azenithconf.showtoast \
persist.sys.azenith.custom_default_balanced_IO \
persist.sys.azenith.custom_powersave_IO \
persist.sys.azenith.custom_performance_IO \
persist.sys.azenith.default_balanced_IO \
persist.sys.azenithconf.resosettings"
for prop in $props; do
	setprop "$prop" ""
	resetprop --delete "$prop"
done
# Uninstall module directories
rm -rf /data/adb/.config/AZenith
rm -rf /data/AZenith
# Uninstall toast apk
pm uninstall azenith.toast 2>/dev/null
# Uninstaller Script
manager_paths="/data/adb/ap/bin /data/adb/ksu/bin"
binaries="sys.azenith-service sys.azenith-service_log \
          sys.azenith-profilesettings sys.azenith-utilityconf \
          sys.azenith-preloadbin sys.azenith-rianixiathermalcorev4"
for dir in $manager_paths; do
	[ -d "$dir" ] || continue
	for remove in $binaries; do
		link="$dir/$remove"
		rm -f "$link"
	done
done
