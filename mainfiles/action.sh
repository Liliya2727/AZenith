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
readonly BIN_SVC="$MODDIR/system/bin/sys.azenith-service"
readonly APK_COMP="$MODDIR/AZenith.apk"
readonly TMP_DIR="/data/local/tmp"
readonly TMP_APK="$TMP_DIR/AZenith_install.apk"

# Check if app is installed
_app_installed() {
	pm path zx.azenith >/dev/null 2>&1
}

# Check if service binary exists
_service_exists() {
	[ -f "$BIN_SVC" ]
}

# Prepare app files safely
_prepare_apk() {
	if [ ! -f "$APK_COMP" ]; then
		echo "[!] APK not found at $APK_COMP" >&2
		return 1
	fi
	cp "$APK_COMP" "$TMP_APK" || return 1
	chmod 644 "$TMP_APK" || return 1
	return 0
}

# Cleanup temporary APK
_cleanup_apk() {
	[ -f "$TMP_APK" ] && rm -f "$TMP_APK"
}

# Install APK with timeout protection
install_manager() {
	local tmp_log="$TMP_DIR/action_install_log.txt"
	local MAX_TIMEOUT=100

	(
		local install_output
		install_output=$(pm install -r -d --user 0 "$TMP_APK" 2>&1)
		if ! echo "$install_output" | grep -iq "Success"; then
			install_output=$(cmd package install -r -d --user 0 "$TMP_APK" 2>&1)
		fi
		echo "$install_output" >"$tmp_log"
	) &
	local pid=$!

	local i=0
	while kill -0 $pid 2>/dev/null; do
		clear
		case $((i % 4)) in
		0) echo "[-] Installing AZenith Manager..." ;;
		1) echo "[/] Installing AZenith Manager..." ;;
		2) echo "[|] Installing AZenith Manager..." ;;
		3) echo "[\] Installing AZenith Manager..." ;;
		esac
		sleep 0.1
		i=$((i + 1))
		
		# Pengecekan batas waktu
		if [ $i -ge $MAX_TIMEOUT ]; then
			echo "[!] Installation took too long. Stopping..."
			kill -9 $pid 2>/dev/null
			echo "Timeout: Installation exceeded 10 seconds" > "$tmp_log"
			break
		fi
	done

	wait $pid 2>/dev/null
	clear

	local result
	[ -f "$tmp_log" ] && result=$(cat "$tmp_log")
	rm -f "$tmp_log"

	if echo "$result" | grep -iq "Success"; then
		printf "[✓] AZenith Manager installed successfully\n"

		pm enable --user 0 zx.azenith/.Launcher >/dev/null 2>&1
		pm grant zx.azenith android.permission.READ_EXTERNAL_STORAGE >/dev/null 2>&1
		pm grant zx.azenith android.permission.POST_NOTIFICATIONS >/dev/null 2>&1
		pm grant zx.azenith android.permission.READ_MEDIA_IMAGES >/dev/null 2>&1

		echo "- Launching manager..."
		sleep 1
		return 0
	else
		echo "[!] Failed to install AZenith Manager"
		if echo "$result" | grep -iq "Timeout"; then
			echo "  Error: Installation failed due to Timeout"
            echo "  Possible reason: Your Rom blocking background install."
		else
			echo "  Log: $result"
		fi
		echo "  Please install AZenith.apk manually."
		sleep 3
		return 1
	fi
}

clear

if ! _app_installed; then
	echo "[*] App not detected. Preparing installation..."
	sleep 1

	if _prepare_apk; then
		if install_manager; then
			_cleanup_apk
			if _service_exists; then
				exec "$BIN_SVC" --appactivity >/dev/null 2>&1
			else
				echo "[!] Service binary not found at $BIN_SVC" >&2
				exit 1
			fi
		else
			_cleanup_apk
			exit 1
		fi
	else
		exit 1
	fi
else
	if _service_exists; then
		echo "[*] Launching AZenith Manager..."
		exec "$BIN_SVC" --appactivity >/dev/null 2>&1
	else
		echo "[!] Service binary not found at $BIN_SVC" >&2
		exit 1
	fi
fi
