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

readonly MODDIR="${0%/*}"
readonly MODULE_CONFIG="/data/adb/.config/AZenith"
readonly BIN_MON="$MODDIR/system/bin/sys.azenith-appmonitoring"
readonly BIN_SVC="$MODDIR/system/bin/sys.azenith-service"
readonly APK_COMP="$MODDIR/AZenith.apk"

# Wait boot to complete
until [ "$(getprop sys.boot_completed)" = "1" ]; do 
    sleep 1
done

# Reset anti bootloop
echo "BOOTCOUNT=0" > "$MODDIR/count.sh"

# Clear Old Logs
"$BIN_SVC" --clearlogs

# Remove reboot flag
if [ -f "$MODDIR/reboot" ]; then
    rm -f "$MODDIR/reboot"
fi

# Create Cleanup Files
if [ ! -f /data/adb/service.d/.azenith_cleanup.sh ]; then
  mkdir -p /data/adb/service.d
  cat "$MODDIR/cleanup.sh" > /data/adb/service.d/.azenith_cleanup.sh
  chmod +x /data/adb/service.d/.azenith_cleanup.sh
fi

# Refresh AZenith daemon state
STATE=$(getprop persist.sys.azenith.state)
{ [ -z "$STATE" ] || { [ "$STATE" = "running" ] && [ -z "$(/system/bin/toybox pidof sys.azenith-service)" ]; }; } && {
    setprop persist.sys.azenith.state stopped
    setprop persist.sys.azenith.service ""
}

# Expose public read access on CPU/GPU telemetry sysfs nodes and API mirror so the app reads via File IO without shell polling
mkdir -p /data/data/zx.azenith/API "$MODULE_CONFIG/API" 2>/dev/null
chmod 0777 /data/data/zx.azenith/API 2>/dev/null
chmod 0644 /data/data/zx.azenith/API/* "$MODULE_CONFIG/API/"* 2>/dev/null

for cpu_dir in /sys/devices/system/cpu/cpu[0-9]*/cpufreq; do
    [ -d "$cpu_dir" ] || continue
    chmod 0444 "$cpu_dir/scaling_cur_freq" "$cpu_dir/cpuinfo_cur_freq" \
        "$cpu_dir/cpuinfo_max_freq" "$cpu_dir/cpuinfo_min_freq" \
        "$cpu_dir/scaling_max_freq" "$cpu_dir/scaling_min_freq" 2>/dev/null
done
chmod 0444 /proc/stat 2>/dev/null

for gpu_node in \
    /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage \
    /sys/class/kgsl/kgsl-3d0/gpubusy \
    /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq \
    /sys/class/kgsl/kgsl-3d0/devfreq/max_freq \
    /sys/kernel/gpu/gpu_busy \
    /sys/kernel/gpu/gpu_clock \
    /sys/kernel/gpu/gpu_max_clock \
    /sys/module/ged/parameters/gpu_loading \
    /sys/kernel/ged/hal/gpu_utilization \
    /proc/mali/utilization \
    /proc/gpufreq/gpufreq_var_dump \
    /proc/gpufreqv2/gpufreq_status \
    /sys/class/misc/mali0/device/utilization \
    /sys/devices/platform/mali/utilization; do
    [ -e "$gpu_node" ] && chmod 0444 "$gpu_node" 2>/dev/null
done

for df_dir in /sys/class/devfreq/*; do
    [ -d "$df_dir" ] || continue
    chmod 0444 "$df_dir/cur_freq" "$df_dir/max_freq" "$df_dir/min_freq" "$df_dir/load" "$df_dir/utilization" 2>/dev/null
done

for tz_file in /sys/class/thermal/thermal_zone*/temp; do
    [ -e "$tz_file" ] && chmod 0444 "$tz_file" 2>/dev/null
done

pm grant zx.azenith android.permission.WRITE_SECURE_SETTINGS > /dev/null 2>&1
pm grant zx.azenith android.permission.PACKAGE_USAGE_STATS > /dev/null 2>&1
pm grant zx.azenith android.permission.DUMP > /dev/null 2>&1
appops set zx.azenith SYSTEM_ALERT_WINDOW allow > /dev/null 2>&1
appops set zx.azenith WRITE_SETTINGS allow > /dev/null 2>&1
appops set zx.azenith GET_USAGE_STATS allow > /dev/null 2>&1

# Exec Binder Companion Daemon
"$BIN_MON"

# Run AZenith service
sleep 1 && exec "$BIN_SVC" --run
