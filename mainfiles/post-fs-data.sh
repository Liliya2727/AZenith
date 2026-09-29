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
LOCK_FILE="/dev/.azenithSingleInstance"

# Single Instance Lock
# Ksu in Metamodule mode, post-fs-data runs twice
if [ -f "$LOCK_FILE" ]; then
    exit 1
fi
touch "$LOCK_FILE"

# Anti bootloop
BOOTCOUNT=0
[ -f "$MODDIR/count.sh" ] && . "$MODDIR/count.sh"

BOOTCOUNT=$(( BOOTCOUNT + 1))

if [ ! -f "/data/adb/modules/AZenith/explicit_I_want_a_bootloop" ] && [ $BOOTCOUNT -gt 1 ]; then
    touch "$MODDIR/disable"
    rm "$MODDIR/count.sh"
    rm /data/adb/service.d/.azenith_cleanup.sh
    string="description=anti-bootloop triggered. module disabled. enable to activate."
    sed -i "s/^description=.*/$string/g" "$MODDIR/module.prop"
    exit 1
else
    echo "BOOTCOUNT=1" > "$MODDIR/count.sh"

    if [ -f "$MODDIR/module.prop.orig" ]; then
        cp -f "$MODDIR/module.prop.orig" "$MODDIR/module.prop"
    fi
    
fi

# KSU Run Stop in Emulated Soft Reboot, but our daemon is running as detached process, making ksu failed to kill the daemon
# So we have to kill it here...
# Clear old process
for p in $(/system/bin/toybox pidof sys.azenith-service); do
    kill -TERM "$p" 2>/dev/null
done
for p in $(/system/bin/toybox pidof sys.azenith-appmonitoring); do
    kill -TERM "$p" 2>/dev/null
done

# Make sure we removed the old .lock file
rm -f "$MODULE_CONFIG/java.lock"
rm -f "$MODULE_CONFIG/API/.lock"

exit 0
