#!/system/bin/sh

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
props=$(resetprop | grep "persist.sys.azenith" | awk -F'[][]' '{print $2}' | sed 's/:.*//')
for prop in $props; do
	resetprop --delete "$prop"
done

# Remove AI Thermal Properties
propsrn="\
persist.sys.rianixia.learning_enabled \
persist.sys.rianixia.thermalcore-bigdata.path "
for prop in $propsrn; do
	resetprop --delete "$prop"
done

# Remove module directories
rm -rf "/data/adb/.config/AZenith"
rm -rf "/data/AZenith"

# Remove toast apk
pm uninstall zx.azenith 2>/dev/null

# Remove azenith binaries
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
