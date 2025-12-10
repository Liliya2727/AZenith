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

# shellcheck disable=SC2013

# Path
MODDIR=${0%/*}
CONFIGPATH="/data/adb/.config/AZenith"
LOGFILEV="$CONFIGPATH/debug/AZenithVerbose.log"
LOGFILE="$CONFIGPATH/debug/AZenith.log"

# Properties
DEBUGMODE="$(getprop persist.sys.azenith.debugmode)"
BYPASSPATH="$(getprop persist.sys.azenithconf.bypasspath)"

# Bypass Charging Path
MTK_BYPASS_CHARGER="/sys/devices/platform/charger/bypass_charger"
MTK_BYPASS_CHARGER_ON="1"
MTK_BYPASS_CHARGER_OFF="0"

MTK_CURRENT_CMD="/proc/mtk_battery_cmd/current_cmd"
MTK_CURRENT_CMD_ON="0 1"
MTK_CURRENT_CMD_OFF="0 0"

TRAN_AICHG="/sys/devices/platform/charger/tran_aichg_disable_charger"
TRAN_AICHG_ON="1"
TRAN_AICHG_OFF="0"

MTK_DISABLE_CHARGER="/sys/devices/platform/mt-battery/disable_charger"
MTK_DISABLE_CHARGER_ON="1"
MTK_DISABLE_CHARGER_OFF="0"

# Logging Functions
AZLog() {
    if [ "$DEBUGMODE" = "true" ]; then
        local timestamp message log_tag
        timestamp=$(date +"%Y-%m-%d %H:%M:%S.%3N")
        message="$1"
        log_tag="AZenith"
        echo "$timestamp I $log_tag: $message" >>"$LOGFILEV"
        log -t "$log_tag" "$message"
    fi
}
dlog() {
	local message log_tag
	message="$1"
	timestamp=$(date +"%Y-%m-%d %H:%M:%S.%3N")
	log_tag="AZenith_Utility"
	echo "$timestamp I $log_tag: $message" >>"$LOGFILE"
    log -t "$log_tag" "$message"
}

# Apply Functions
zeshia() {
    local value="$1"
    local path="$2"
    local lock="${3:-true}"
    local pathname

    pathname="$(echo "$path" | awk -F'/' '{print $(NF-1)"/"$NF}')"

    if [ ! -e "$path" ]; then
        AZLog "File /$pathname not found, skipping..."
        return
    fi

    chmod 644 "$path" 2>/dev/null

    if ! echo "$value" >"$path" 2>/dev/null; then
        AZLog "Cannot write to /$pathname (permission denied)"
        [ "$lock" = "true" ] && chmod 444 "$path" 2>/dev/null
        return
    fi

    local current
    current="$(cat "$path" 2>/dev/null)"

    if [ "$current" = "$value" ]; then
        AZLog "Set /$pathname to $value"
    else
        echo "$value" >"$path" 2>/dev/null
        current="$(cat "$path" 2>/dev/null)"

        if [ "$current" = "$value" ]; then
            AZLog "Set /$pathname to $value (after retry)"
        else
            AZLog "Failed to set /$pathname to $value"
        fi
    fi

    [ "$lock" = "true" ] && chmod 444 "$path" 2>/dev/null
}

# Main functions
setsgov() {
	chmod 644 /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
	echo "$1" | tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
	chmod 444 /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
	dlog "Set current CPU Governor to $1"
}

setsIO() {
	for block in sda sdb sdc mmcblk0 mmcblk1; do
		if [ -e "/sys/block/$block/queue/scheduler" ]; then
			chmod 644 "/sys/block/$block/queue/scheduler"
			echo "$1" | tee "/sys/block/$block/queue/scheduler" >/dev/null
			chmod 444 "/sys/block/$block/queue/scheduler"
		fi
	done
	dlog "Set current IO Scheduler to $1"
}

setthermalcore() {
    local state="$1"
    if [ "$state" -eq 1 ]; then
        sys.azenith-rianixiathermalcorev4 &
        sleep 1
        local pid
        pid="$(pgrep -f sys.azenith-rianixiathermalcorev4)"
        if [ -n "$pid" ]; then
            dlog "Starting Thermalcore Service with pid $pid"
        else
            dlog "Thermalcore service started but PID not found"
        fi
    else
        pkill -9 -f sys.azenith-rianixiathermalcorev4 >/dev/null 2>&1
        dlog "Stopped Thermalcore service"
    fi
}
    
FSTrim() {
	for mount in /system /vendor /data /cache /metadata /odm /system_ext /product; do
		if mountpoint -q "$mount"; then
			fstrim -v "$mount"
			AZLog "Trimmed: $mount"
		else
			AZLog "Skipped (not mounted): $mount"
		fi
	done
	dlog "Trimmed unused blocks"
}

disablevsync() {
    case "$1" in
        60hz)
            service call SurfaceFlinger 1035 i32 2
            dlog "Applied 60hz DisableVsync"
        ;;
        90hz)
            service call SurfaceFlinger 1035 i32 1
            dlog "Applied 90hz DisableVsync"
        ;;
        120hz)
            service call SurfaceFlinger 1035 i32 0
            dlog "Applied 120hz DisableVsync"
        ;;
        Disabled)
            echo "disabled"
        ;;
    esac
}

# Bypass Charge
enableBypass() {
    source $MODDIR/bypasschg
    key="$BYPASSPATH"
    val="${key}_ON"
    zeshia "$(eval echo \${$val})" "$(eval echo \${$key})"
}

disableBypass() {
    source $MODDIR/bypasschg
    key="$BYPASSPATH"
    val="${key}_OFF"
    zeshia "$(eval echo \${$val})" "$(eval echo \${$key})"
}

saveLog() {
    log_file="/sdcard/AZenithLog_$(date +"%Y-%m-%d_%H-%M").txt"
    echo "$log_file"
    module_ver=$(awk -F'=' '/version=/ {print $2}' /data/adb/modules/AZenith/module.prop 2>/dev/null)
    android_sdk=$(getprop ro.build.version.sdk)
    kernel_info=$(uname -r -m)
    fingerprint=$(getprop ro.build.fingerprint)
    device=$(getprop sys.azenith.device)
    chipset=$(getprop sys.azenith.soc)

    {
        echo "##########################################"
        echo "             AZenith Process Log"
        echo
        echo "    Module: $module_ver"
        echo "    Android: $android_sdk"
        echo "    Kernel: $kernel_info"
        echo "    Fingerprint: $fingerprint"
        echo "    Device: $device"
        echo "    Chipset: $chipset"
        echo "##########################################"
        echo
        cat /data/adb/.config/AZenith/debug/AZenith.log 2>/dev/null
    } >"$log_file"
}

$@

exit 0
