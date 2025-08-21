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
persist.sys.azenith.justintime \
persist.sys.azenith.service \
persist.sys.azenith.disabletrace \
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
persist.sys.azenithconf.showtoast"
for prop in $props; do
  setprop "$prop" ""
  resetprop --delete "$prop"
done
# Uninstall module directories
rm -rf /data/local/tmp/module_icon.png
rm -rf /data/adb/.config/AZenith
rm -rf /data/AZenith
# Uninstaller Script
manager_paths="/data/adb/ap/bin /data/adb/ksu/bin"
binaries="sys.azenith-service sys.azenith-service_log \
          sys.azenith-profilesettings sys.azenith-utilityconf \
          sys.azenith-preloadbin sys.azenith-preloadbin2"
for dir in $manager_paths; do
  [ -d "$dir" ] || continue
  for remove in $binaries; do
    link="$dir/$remove"
    rm -f "$link"
  done
done
