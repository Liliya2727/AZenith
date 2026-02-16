#!/system/bin/sh

#
# Copyright (C) 2026-2027 Zexshia
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
rm -rf "/data/data/zx.azenith"

# Add remove flag to module directories
touch "/data/adb/modules/AZenith/remove"

# Remove apk
pm uninstall zx.azenith 2>/dev/null

# Unlink AZenith binaries
manager_paths="/data/adb/ap/bin /data/adb/ksu/bin"
binaries="sys.azenith-service \
          sys.azenith-profilesettings sys.azenith-utilityconf \
          sys.azenith-preloadbin sys.azenith-rianixiathermalcore"
for dir in $manager_paths; do
	[ -d "$dir" ] || continue
	for remove in $binaries; do
		link="$dir/$remove"
		unlink "$link"
	done
done
