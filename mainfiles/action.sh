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

install_pkg() {

    local apk_path="/data/adb/modules/AZenith/AZenith.apk"

    if [ -f "$apk_path" ]; then
        echo "- App Manager is not fully installed. Installing now..."
        local install_res=$(cmd package install -r -d --user 0 "$apk_path" 2>&1)
        
        if echo "$install_res" | grep -iq "Success"; then
            echo "- App Manager installed successfully."
            sleep 1
        else
            echo "[!] Failed to install APK. Log: $install_res" >&2
            return 1
        fi
    else
        echo "[!] APK file not found at $apk_path" >&2
        return 1
    fi
}

# Cek kelayakan environment sebelum membuka aplikasi
if ! command -v /data/adb/modules/AZenith/system/bin/sys.azenith-service >/dev/null 2>&1 || ! pm path zx.azenith >/dev/null 2>&1; then
    install_pkg
fi

exec /data/adb/modules/AZenith/system/bin/sys.azenith-service --appactivity > /dev/null 2>&1
