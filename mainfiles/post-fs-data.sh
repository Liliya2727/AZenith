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
PERSISTENT_DIR="/data/adb/.config/AZenith"
LOCK_FILE="/dev/.azenithSingleInstance"

# Single Instance Lock
# Ksu in Metamodule mode, post-fs-data runs twice
if [ -f "$LOCK_FILE" ]; then
    exit 0
fi
touch "$LOCK_FILE"

# Anti bootloop
BOOTCOUNT=0
[ -f "$MODDIR/count.sh" ] && . "$MODDIR/count.sh"

BOOTCOUNT=$(( BOOTCOUNT + 1))

if [ $BOOTCOUNT -gt 1 ]; then
    touch "$MODDIR/disable"
    rm "$MODDIR/count.sh"
    
    string="description=anti-bootloop triggered. module disabled. enable to activate."
    sed -i "s/^description=.*/$string/g" "$MODDIR/module.prop"
    exit 1
else
    echo "BOOTCOUNT=1" > "$MODDIR/count.sh"

    if [ -f "$MODDIR/module.prop.orig" ]; then
        cp -f "$MODDIR/module.prop.orig" "$MODDIR/module.prop"
    fi
    
fi

exit 0
