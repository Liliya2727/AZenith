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

# shellcheck disable=SC2013

# Path
MODDIR=${0%/*}
CONFIGPATH="/data/adb/.config/AZenith"

# Properties
DEBUGMODE="$(getprop persist.sys.azenith.debugmode)"
FSTRIM_STATE="$(getprop persist.sys.azenithconf.fstrim)"

# Logging Functions
AZLog() {
    if [ "$DEBUGMODE" = "true" ]; then
        local message log_tag log_level        
        message="$1"
        log_tag="AZLog"
        log_level="0"
        sys.azenith-service --verboselog $log_tag $log_level $message
    fi
}
dlog() {
	local message log_tag log_level
	message="$1"
	log_tag="AZenith_Utility"
	log_level="1"
    sys.azenith-service --log $log_tag $log_level $message
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

    AZLog "Set /$pathname to $value"

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
        sys.azenith-rianixiathermalcore &
        sleep 1
        local pid
        pid="$(pgrep -f sys.azenith-rianixiathermalcore)"
        if [ -n "$pid" ]; then
            dlog "Starting Thermalcore Service with pid $pid"
        else
            dlog "Thermalcore service started but PID not found"
        fi
    else
        pkill -9 -f sys.azenith-rianixiathermalcore >/dev/null 2>&1
        dlog "Stopped Thermalcore service"
    fi
}
    
FSTrim() {
    if [ "$FSTRIM_STATE" -eq 1 ]; then
    	for mount in /system /vendor /data /cache /metadata /odm /system_ext /product; do
    		if mountpoint -q "$mount"; then
    			fstrim -v "$mount"
    			AZLog "Trimmed: $mount"
    		else
    			AZLog "Skipped (not mounted): $mount"
    		fi
    	done
    	dlog "Trimmed unused blocks"
    fi
}

enableDND() {	
	cmd notification set_dnd priority && dlog "DND enabled" || dlog "Failed to enable DND"
}

disableDND() {	
	cmd notification set_dnd off && dlog "DND disabled" || dlog "Failed to disable DND"
}

setrefreshrates() {
    service call SurfaceFlinger 1035 i32 $1
}

restartservice() {
    pkill -9 -f sys.azenith.rianixiathermalcore
    pkill -9 -f sys.azenith-service
    setprop persist.sys.azenith.state stopped
    /data/adb/modules/AZenith/system/bin/sys.azenith-service -r > /dev/null 2>&1 & disown
}

setrender() {
    case "$1" in
        vulkan)
            setprop debug.hwui.renderer skiavk
            resetprop -p persist.sys.azenithconf.renderer skiavk
            ;;
        skiagl)
            setprop debug.hwui.renderer skiagl
            resetprop -p persist.sys.azenithconf.renderer skiagl
            ;;
        *)
            resetprop -p persist.sys.azenithconf.renderer Default
            ;;
    esac
    dlog "Set current renderer to: $1"
}

saveLog() {
    log_file="/sdcard/AZenithLog_$(date +"%Y-%m-%d_%H-%M").txt"
    echo "$log_file"
    module_ver=$(awk -F'=' '/version=/ {print $2}' /data/adb/modules/AZenith/module.prop 2>/dev/null)
    android_sdk=$(getprop ro.build.version.sdk)
    kernel_info=$(uname -r -m)
    fingerprint=$(getprop ro.build.fingerprint)

    {
        echo "##########################################"
        echo
        echo "             AZenith Process Log"
        echo
        echo "    Module: $module_ver"
        echo "    Android: $android_sdk"
        echo "    Kernel: $kernel_info"
        echo "    Fingerprint: $fingerprint"
        echo "##########################################"
        echo
        cat /data/adb/.config/AZenith/debug/AZenith.log 2>/dev/null
    } >"$log_file"
}

$@

exit 0
