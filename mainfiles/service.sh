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
"$BIN_SVC" --setup-access

# Dump and save refresh rates id
"$BIN_SVC" --get_rr_modes

# Exec Binder Companion Daemon
"$BIN_MON"

# Run AZenith service
sleep 1 && exec "$BIN_SVC" --run
