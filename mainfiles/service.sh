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

CONF="/data/adb/.config/AZenith"
CPU="/sys/devices/system/cpu/cpu0/cpufreq"
# Parse Governor to use
chmod 644 "$CPU/scaling_governor"
default_gov=$(cat "$CPU/scaling_governor")
echo "$default_gov" >$CONF/default_cpu_gov
# Cleanup old Logs
rm -f /data/adb/.config/AZenith/AZenith.log
rm -f /data/adb/.config/AZenith/AZenithVerbose.log
rm -f /data/adb/.config/AZenith/AZenithPR.log

# Wait for boot to Complete
while [ "$(getprop sys.boot_completed)" != "1" ]; do
	sleep 40
done

# Fallback to normal gov if default is performance
if [ "$default_gov" == "performance" ] && [ ! -f $CONF/custom_default_cpu_gov ]; then
	for gov in scx schedhorizon walt sched_pixel sugov_ext uag schedplus energy_step schedutil interactive conservative powersave; do
		grep -q "$gov" "$CPU/scaling_available_governors" && {
			echo "$gov" >$CONF/default_cpu_gov
			default_gov="$gov"
			break
		}
	done
fi
# Revert governor
custom_gov="$CONF/custom_default_cpu_gov"
[ -f "$custom_gov" ] && default_gov=$(cat "$custom_gov")
echo "$default_gov" | tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
[ ! -f $CONF/custom_powersave_cpu_gov ] && echo "$default_gov" >$CONF/custom_powersave_cpu_gov
[ ! -f $CONF/custom_game_cpu_gov ] && echo "$default_gov" >$CONF/custom_game_cpu_gov

# Run Daemon
AZenith
