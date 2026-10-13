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

cleanup_modules() {

    resetprop | awk -F'[][]' '/persist\.sys\.azenith/ {print $2}' | while read -r prop; do
        resetprop -p --delete "$prop"
    done

    for prop in persist.sys.rianixia.learning_enabled persist.sys.rianixia.thermalcore-bigdata.path; do
        resetprop -p --delete "$prop"
    done

    # Wait for PackageManager before uninstalling
    until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 2; done
    pm uninstall zx.azenith >/dev/null 2>&1

    rm -rf \
        "/data/adb/.config/AZenith" \
        "/data/AZenith" \
        "/data/data/zx.azenith"

    for dir in "/data/adb/ap/bin" "/data/adb/ksu/bin"; do
        rm -rf "$dir/zx"
        [ -d "$dir" ] && find "$dir" -name "sys.azenith-*" -exec rm -f {} +
    done
}

if [ -f /data/adb/modules/AZenith/disable ]; then
    cat /data/adb/modules/AZenith/module.prop.orig > /data/adb/modules/AZenith/module.prop
    rm /data/adb/service.d/.azenith_cleanup.sh
elif [ ! -d /data/adb/modules/AZenith ]; then
    cleanup_modules
    rm /data/adb/service.d/.azenith_cleanup.sh
fi
