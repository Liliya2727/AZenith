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

# Val
list_logger="logd traced statsd tcpdump cnss_diag subsystem_ramdump charge_logger wlan_logging"
curprofile=$(<"$CONFIGPATH/API/current_profile")
POLICIES=$(ls /sys/devices/system/cpu/cpufreq | grep policy)
BYPASSPATHLIST="
    MTK_BYPASS_CHARGER:/sys/devices/platform/charger/bypass_charger
    MTK_CURRENT_CMD:/proc/mtk_battery_cmd/current_cmd
    TRAN_AICHG:/sys/devices/platform/charger/tran_aichg_disable_charger
    MTK_DISABLE_CHARGER:/sys/devices/platform/mt-battery/disable_charger
"        

# Properties
LIMITER=$(getprop persist.sys.azenithconf.freqoffset | sed -e 's/Disabled/100/' -e 's/%//g')
DND_STATE="$(getprop persist.sys.azenithconf.dnd)"
LOGD_STATE="$(getprop persist.sys.azenithconf.logd)"
DEBUGMODE="$(getprop persist.sys.azenith.debugmode)"
DTHERMAL_STATE="$(getprop persist.sys.azenithconf.DThermal)"
SFL_STATE="$(getprop persist.sys.azenithconf.SFL)"
MALISCHED_STATE="$(getprop persist.sys.azenithconf.malisched)"
FPSGED_STATE="$(getprop persist.sys.azenithconf.fpsged)"
SCHEDTUNES_STATE="$(getprop persist.sys.azenithconf.schedtunes)"
JUSTINTIME_STATE="$(getprop persist.sys.azenithconf.justintime)"
BYPASSCHG_STATE="$(getprop persist.sys.azenithconf.bypasschg)"
DISTRACE_STATE="$(getprop persist.sys.azenithconf.disabletrace)"
CLEARAPPS="$(getprop persist.sys.azenithconf.clearbg)"
LITEMODE="$(getprop persist.sys.azenithconf.cpulimit)"
VSYNCVALUE="$(getprop persist.sys.azenithconf.vsync)"
BYPASSPROPS="persist.sys.azenithconf.bypasspath"
BYPASSPATH="$(getprop persist.sys.azenithconf.bypasspath)"
WALT_STATE="$(getprop persist.sys.azenithconf.walttunes)"
MALI_COMP="$(getprop sys.azenith.maligovsupport)"

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
	log_tag="AZenith"
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

# Helper Functions
applyppmnfreqsets() {
	[ ! -f "$2" ] && return 1
	chmod 644 "$2" 2>/dev/null
	echo "$1" >"$2" 2>/dev/null
	chmod 444 "$2" 2>/dev/null
}

which_maxfreq() {
	tr ' ' '\n' <"$1" | sort -nr | head -n 1
}

which_minfreq() {
	tr ' ' '\n' <"$1" | grep -v '^[[:space:]]*$' | sort -n | head -n 1
}

which_midfreq() {
	total_opp=$(wc -w <"$1")
	mid_opp=$(((total_opp + 1) / 2))
	tr ' ' '\n' <"$1" | grep -v '^[[:space:]]*$' | sort -nr | head -n $mid_opp | tail -n 1
}

setfreqs() {
	local file="$1" target="$2" chosen=""
	if [ -f "$file" ]; then
		chosen=$(tr -s ' ' '\n' <"$file" |
			awk -v t="$target" '
                {diff = (t - $1 >= 0 ? t - $1 : $1 - t)}
                NR==1 || diff < mindiff {mindiff = diff; val=$1}
                END {print val}')
	else
		chosen="$target"
	fi
	echo "$chosen"
}

devfreq_max_perf() {
	[ ! -f "$1/available_frequencies" ] && return 1
	max_freq=$(which_maxfreq "$1/available_frequencies")
	zeshia "$max_freq" "$1/max_freq"
	zeshia "$max_freq" "$1/min_freq"
}

devfreq_mid_perf() {
	[ ! -f "$1/available_frequencies" ] && return 1
	max_freq=$(which_maxfreq "$1/available_frequencies")
	mid_freq=$(which_midfreq "$1/available_frequencies")
	zeshia "$max_freq" "$1/max_freq"
	zeshia "$mid_freq" "$1/min_freq"
}

devfreq_unlock() {
	[ ! -f "$1/available_frequencies" ] && return 1
	max_freq=$(which_maxfreq "$1/available_frequencies")
	min_freq=$(which_minfreq "$1/available_frequencies")
	zeshia "$max_freq" "$1/max_freq" false
	zeshia "$min_freq" "$1/min_freq" false
}

devfreq_min_perf() {
	[ ! -f "$1/available_frequencies" ] && return 1
	freq=$(which_minfreq "$1/available_frequencies")
	zeshia "$freq" "$1/min_freq"
	zeshia "$freq" "$1/max_freq"
}

qcom_cpudcvs_max_perf() {
	[ ! -f "$1/available_frequencies" ] && return 1
	freq=$(which_maxfreq "$1/available_frequencies")
	zeshia "$freq" "$1/hw_max_freq"
	zeshia "$freq" "$1/hw_min_freq"
}

qcom_cpudcvs_mid_perf() {
	[ ! -f "$1/available_frequencies" ] && return 1
	max_freq=$(which_maxfreq "$1/available_frequencies")
	mid_freq=$(which_midfreq "$1/available_frequencies")
	zeshia "$max_freq" "$1/hw_max_freq"
	zeshia "$mid_freq" "$1/hw_min_freq"
}

qcom_cpudcvs_unlock() {
	[ ! -f "$1/available_frequencies" ] && return 1
	max_freq=$(which_maxfreq "$1/available_frequencies")
	min_freq=$(which_minfreq "$1/available_frequencies")
	zeshia "$max_freq" "$1/hw_max_freq" false
	zeshia "$min_freq" "$1/hw_min_freq" false
}

qcom_cpudcvs_min_perf() {
	[ ! -f "$1/available_frequencies" ] && return 1
	freq=$(which_minfreq "$1/available_frequencies")
	zeshia "$freq" "$1/hw_min_freq"
	zeshia "$freq" "$1/hw_max_freq"
}

setgov() {
	chmod 644 /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
	echo "$1" | tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor >/dev/null
	chmod 444 /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
	chmod 444 /sys/devices/system/cpu/cpufreq/policy*/scaling_governor
}

setsIO() {
	for block in sda sdb sdc mmcblk0 mmcblk1; do
		if [ -e "/sys/block/$block/queue/scheduler" ]; then
			chmod 644 "/sys/block/$block/queue/scheduler"
			echo "$1" | tee "/sys/block/$block/queue/scheduler" >/dev/null
			chmod 444 "/sys/block/$block/queue/scheduler"
		fi
	done
}

setfreqppm() {
	if [ -d /proc/ppm ]; then
		LIMITER=$(getprop persist.sys.azenithconf.freqoffset | sed -e 's/Disabled/100/' -e 's/%//g')
		curprofile=$(<"/data/adb/.config/AZenith/API/current_profile")
		cluster=0
		for path in /sys/devices/system/cpu/cpufreq/policy*; do
			cpu_maxfreq=$(<"$path/cpuinfo_max_freq")
			cpu_minfreq=$(<"$path/cpuinfo_min_freq")
			new_max_target=$((cpu_maxfreq * LIMITER / 100))
			new_maxfreq=$(setfreqs "$path/scaling_available_frequencies" "$new_max_target")
			[ "$curprofile" = "3" ] && {
				target_min_target=$((cpu_maxfreq * 40 / 100))
				new_minfreq=$(setfreqs "$path/scaling_available_frequencies" "$target_min_target")
				zeshia "$cluster $new_maxfreq" "/proc/ppm/policy/hard_userlimit_max_cpu_freq"
				zeshia "$cluster $new_minfreq" "/proc/ppm/policy/hard_userlimit_min_cpu_freq"
				policy_name=$(basename "$path")
			    dlog "Set $policy_name maxfreq=$new_maxfreq minfreq=$new_minfreq"
				((cluster++))
				continue
			}
			zeshia "$cluster $new_maxfreq" "/proc/ppm/policy/hard_userlimit_max_cpu_freq"
			zeshia "$cluster $cpu_minfreq" "/proc/ppm/policy/hard_userlimit_min_cpu_freq"
			policy_name=$(basename "$path")
		    dlog "Set $policy_name maxfreq=$new_maxfreq minfreq=$cpu_minfreq"
			((cluster++))
		done
	fi
}

clear_background_apps() {
    visible_pkgs=$(dumpsys window displays | grep "Task{" | grep "visible=true" | sed -n 's/.*A=[0-9]*:\([^ ]*\).*/\1/p' | sort -u)
    invisible_pkgs=$(dumpsys window displays | grep "Task{" | grep "visible=false" | sed -n 's/.*A=[0-9]*:\([^ ]*\).*/\1/p' | sort -u)
    exclude="(com.android.systemui|com.android.settings|android|system)"

    for pkg in $invisible_pkgs; do
        if echo "$visible_pkgs" | grep -qFx "$pkg"; then
            continue
        fi
        if ! echo "$pkg" | grep -Eq "$exclude"; then
            am force-stop "$pkg" 2>/dev/null
            AZLog "Stopped app: $pkg"
        fi
    done

    dlog "Cleared background apps"
}

setfreq() {
	LIMITER=$(getprop persist.sys.azenithconf.freqoffset | sed -e 's/Disabled/100/' -e 's/%//g')
	curprofile=$(<"/data/adb/.config/AZenith/API/current_profile")
	for path in /sys/devices/system/cpu/*/cpufreq; do
		cpu_maxfreq=$(<"$path/cpuinfo_max_freq")
		cpu_minfreq=$(<"$path/cpuinfo_min_freq")
		new_max_target=$((cpu_maxfreq * LIMITER / 100))
		new_maxfreq=$(setfreqs "$path/scaling_available_frequencies" "$new_max_target")
		[ "$curprofile" = "3" ] && {
			target_min_target=$((cpu_maxfreq * 40 / 100))
			new_minfreq=$(setfreqs "$path/scaling_available_frequencies" "$target_min_target")
			zeshia "$new_maxfreq" "$path/scaling_max_freq"
			zeshia "$new_minfreq" "$path/scaling_min_freq"
			policy_name=$(basename "$path")
			dlog "Set $policy_name maxfreq=$new_maxfreq minfreq=$new_minfreq"
			continue
		}
		zeshia "$new_maxfreq" "$path/scaling_max_freq"
		zeshia "$cpu_minfreq" "$path/scaling_min_freq"
		policy_name=$(basename "$path")
		dlog "Set $policy_name maxfreq=$new_maxfreq minfreq=$cpu_minfreq"
		chmod -f 444 /sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq
	done
}

setgamefreqppm() {
	if [ -d /proc/ppm ]; then
		cluster=-1
		for path in /sys/devices/system/cpu/cpufreq/policy*; do
			((cluster++))
			cpu_maxfreq=$(<"$path/cpuinfo_max_freq")
			cpu_minfreq=$(<"$path/cpuinfo_max_freq")
			new_midtarget=$((cpu_maxfreq * 100 / 100))
			new_midfreq=$(setfreqs "$path/scaling_available_frequencies" "$new_midtarget")
			[ "$LITEMODE" -eq 1 ] && {
				cpu_minfreq=$(<"$path/cpuinfo_min_freq")								
				zeshia "$cluster $new_midfreq" "/proc/ppm/policy/hard_userlimit_max_cpu_freq"
				zeshia "$cluster $cpu_minfreq" "/proc/ppm/policy/hard_userlimit_min_cpu_freq"
				policy_name=$(basename "$path")
			    dlog "Set $policy_name maxfreq=$new_midfreq minfreq=$cpu_minfreq"
				continue
			}
			zeshia "$cluster $cpu_maxfreq" "/proc/ppm/policy/hard_userlimit_max_cpu_freq"
			zeshia "$cluster $new_midfreq" "/proc/ppm/policy/hard_userlimit_min_cpu_freq"
			policy_name=$(basename "$path")
	        dlog "Set $policy_name maxfreq=$cpu_maxfreq minfreq=$new_midfreq"
		done
	fi
}

setgamefreq() {
	for path in /sys/devices/system/cpu/*/cpufreq; do
		cpu_maxfreq=$(<"$path/cpuinfo_max_freq")
		cpu_minfreq=$(<"$path/cpuinfo_max_freq")
		new_midtarget=$((cpu_maxfreq * 100 / 100))
		new_midfreq=$(setfreqs "$path/scaling_available_frequencies" "$new_midtarget")
		[ "$LITEMODE" -eq 1 ] && {
			cpu_minfreq=$(<"$path/cpuinfo_min_freq")								
			zeshia "$cluster $new_midfreq" "/proc/ppm/policy/hard_userlimit_max_cpu_freq"
			zeshia "$cluster $cpu_minfreq" "/proc/ppm/policy/hard_userlimit_min_cpu_freq"
		    policy_name=$(basename "$path")
			dlog "Set $policy_name maxfreq=$new_midfreq minfreq=$cpu_minfreq"
			continue
		}
		zeshia "$cpu_maxfreq" "$path/scaling_max_freq"
		zeshia "$new_midfreq" "$path/scaling_min_freq"
		policy_name=$(basename "$path")
	    dlog "Set $policy_name maxfreq=$cpu_maxfreq minfreq=$new_midfreq"
		chmod -f 444 /sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq
	done
}

## For Daemon Calls
Dsetfreqppm() {
	if [ -d /proc/ppm ]; then
		LIMITER=$(getprop persist.sys.azenithconf.freqoffset | sed -e 's/Disabled/100/' -e 's/%//g')
		curprofile=$(<"/data/adb/.config/AZenith/API/current_profile")
		cluster=0
		for path in /sys/devices/system/cpu/cpufreq/policy*; do
			cpu_maxfreq=$(<"$path/cpuinfo_max_freq")
			cpu_minfreq=$(<"$path/cpuinfo_min_freq")
			new_max_target=$((cpu_maxfreq * LIMITER / 100))
			new_maxfreq=$(setfreqs "$path/scaling_available_frequencies" "$new_max_target")
			[ "$curprofile" = "3" ] && {
				target_min_target=$((cpu_maxfreq * 40 / 100))
				new_minfreq=$(setfreqs "$path/scaling_available_frequencies" "$target_min_target")
				applyppmnfreqsets "$cluster $new_maxfreq" "/proc/ppm/policy/hard_userlimit_max_cpu_freq"
				applyppmnfreqsets "$cluster $new_minfreq" "/proc/ppm/policy/hard_userlimit_min_cpu_freq"
				((cluster++))
				continue
			}
			applyppmnfreqsets "$cluster $new_maxfreq" "/proc/ppm/policy/hard_userlimit_max_cpu_freq"
			applyppmnfreqsets "$cluster $cpu_minfreq" "/proc/ppm/policy/hard_userlimit_min_cpu_freq"
			((cluster++))
		done
	fi
}

Dsetfreq() {
	LIMITER=$(getprop persist.sys.azenithconf.freqoffset | sed -e 's/Disabled/100/' -e 's/%//g')
	curprofile=$(<"/data/adb/.config/AZenith/API/current_profile")
	for path in /sys/devices/system/cpu/*/cpufreq; do
		cpu_maxfreq=$(<"$path/cpuinfo_max_freq")
		cpu_minfreq=$(<"$path/cpuinfo_min_freq")
		new_max_target=$((cpu_maxfreq * LIMITER / 100))
		new_maxfreq=$(setfreqs "$path/scaling_available_frequencies" "$new_max_target")
		[ "$curprofile" = "3" ] && {
			target_min_target=$((cpu_maxfreq * 40 / 100))
			new_minfreq=$(setfreqs "$path/scaling_available_frequencies" "$target_min_target")
			applyppmnfreqsets "$new_maxfreq" "$path/scaling_max_freq"
			applyppmnfreqsets "$new_minfreq" "$path/scaling_min_freq"
			continue
		}
		applyppmnfreqsets "$new_maxfreq" "$path/scaling_max_freq"
		applyppmnfreqsets "$cpu_minfreq" "$path/scaling_min_freq"
		chmod -f 444 /sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq
	done
}

Dsetgamefreqppm() {
	if [ -d /proc/ppm ]; then
		cluster=-1
		for path in /sys/devices/system/cpu/cpufreq/policy*; do
			((cluster++))
			cpu_maxfreq=$(<"$path/cpuinfo_max_freq")
			cpu_minfreq=$(<"$path/cpuinfo_max_freq")
			new_midtarget=$((cpu_maxfreq * 100 / 100))
			new_midfreq=$(setfreqs "$path/scaling_available_frequencies" "$new_midtarget")
			[ "$LITEMODE" -eq 1 ] && {
				cpu_minfreq=$(<"$path/cpuinfo_min_freq")								
				zeshia "$cluster $new_midfreq" "/proc/ppm/policy/hard_userlimit_max_cpu_freq"
				zeshia "$cluster $cpu_minfreq" "/proc/ppm/policy/hard_userlimit_min_cpu_freq"
				policy_name=$(basename "$path")
			    dlog "Set $policy_name maxfreq=$new_midfreq minfreq=$cpu_minfreq"
				continue
			}
			applyppmnfreqsets "$cluster $cpu_maxfreq" "/proc/ppm/policy/hard_userlimit_max_cpu_freq"
			applyppmnfreqsets "$cluster $new_midfreq" "/proc/ppm/policy/hard_userlimit_min_cpu_freq"
		done
	fi
}

Dsetgamefreq() {
	for path in /sys/devices/system/cpu/*/cpufreq; do
		cpu_maxfreq=$(<"$path/cpuinfo_max_freq")
		cpu_minfreq=$(<"$path/cpuinfo_max_freq")
		new_midtarget=$((cpu_maxfreq * 100 / 100))
		new_midfreq=$(setfreqs "$path/scaling_available_frequencies" "$new_midtarget")
		[ "$LITEMODE" -eq 1 ] && {
			cpu_minfreq=$(<"$path/cpuinfo_min_freq")								
			zeshia "$cluster $new_midfreq" "/proc/ppm/policy/hard_userlimit_max_cpu_freq"
			zeshia "$cluster $cpu_minfreq" "/proc/ppm/policy/hard_userlimit_min_cpu_freq"
		    policy_name=$(basename "$path")
			dlog "Set $policy_name maxfreq=$new_midfreq minfreq=$cpu_minfreq"
			continue
		}
		applyppmnfreqsets "$cpu_maxfreq" "$path/scaling_max_freq"
		applyppmnfreqsets "$new_midfreq" "$path/scaling_min_freq"
		chmod -f 444 /sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq
	done
}

applyfreqbalance() {
	[ -d /proc/ppm ] && Dsetfreqppm || Dsetfreq
}

applyfreqgame() {
	[ -d /proc/ppm ] && Dsetgamefreqppm || Dsetgamefreq
}

# detect highest frequency policy (biggest core cluster)
get_biggest_cluster() {
    local max_freq=0
    local target=""
    for p in $POLICIES; do
        cur_freq=$(cat /sys/devices/system/cpu/cpufreq/$p/cpuinfo_max_freq 2>/dev/null || echo 0)
        if [ "$cur_freq" -gt "$max_freq" ]; then
            max_freq=$cur_freq
            target=$p
        fi
    done
    echo "$target"
}

setsGPUMali() {
    MALI=/sys/devices/platform/soc/*.mali
    MALI_GOV=$MALI/devfreq/*.mali/governor
	chmod 644 $MALI_GOV
	echo "$1" | tee $MALI_GOV
	chmod 444 $MALI_GOV
}

###############################################
# # # # # # #  MEDIATEK BALANCE # # # # # # #
###############################################
mediatek_balance() {
	# PPM Settings
	if [ -d /proc/ppm ]; then
		if [ -f /proc/ppm/policy_status ]; then
			for idx in $(grep -E 'FORCE_LIMIT|PWR_THRO|THERMAL|USER_LIMIT' /proc/ppm/policy_status | awk -F'[][]' '{print $2}'); do
				zeshia "$idx 1" "/proc/ppm/policy_status"
			done

			for dx in $(grep -E 'SYS_BOOST' /proc/ppm/policy_status | awk -F'[][]' '{print $2}'); do
				zeshia "$dx 0" "/proc/ppm/policy_status"
			done
		fi
	fi

	# CPU POWER MODE
	zeshia "0" "/proc/cpufreq/cpufreq_cci_mode"
	zeshia "1" "/proc/cpufreq/cpufreq_power_mode"

	# GPU Frequency
	if [ -d /proc/gpufreq ]; then
		zeshia "0" /proc/gpufreq/gpufreq_opp_freq
	elif [ -d /proc/gpufreqv2 ]; then
		zeshia "-1" /proc/gpufreqv2/fix_target_opp_index
	fi

	# EAS/HMP Switch
	zeshia "1" /sys/devices/system/cpu/eas/enable

	# GPU Power limiter
	[ -f "/proc/gpufreq/gpufreq_power_limited" ] && {
		for setting in ignore_batt_oc ignore_batt_percent ignore_low_batt ignore_thermal_protect ignore_pbm_limited; do
			zeshia "$setting 0" /proc/gpufreq/gpufreq_power_limited
		done
	}

	# Batoc Throttling and Power Limiter>
	zeshia "0" /proc/perfmgr/syslimiter/syslimiter_force_disable
	zeshia "stop 0" /proc/mtk_batoc_throttling/battery_oc_protect_stop
	# Enable Power Budget management for new 5.x mtk kernels
	zeshia "stop 0" /proc/pbm/pbm_stop

	# Enable battery current limiter
	zeshia "stop 0" /proc/mtk_batoc_throttling/battery_oc_protect_stop

	# Eara Thermal
	zeshia "1" /sys/kernel/eara_thermal/enable

	# Restore UFS governor
	zeshia "-1" "/sys/devices/platform/10012000.dvfsrc/helio-dvfsrc/dvfsrc_req_ddr_opp"
	zeshia "-1" "/sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp"
	zeshia "userspace" "/sys/class/devfreq/mtk-dvfsrc-devfreq/governor"
	zeshia "userspace" "/sys/devices/platform/soc/1c00f000.dvfsrc/mtk-dvfsrc-devfreq/devfreq/mtk-dvfsrc-devfreq/governor"
	
	mali_sysfs=$(find /sys/devices/platform/ -iname "*.mali" -print -quit 2>/dev/null)
	zeshia coarse_demand "$mali_sysfs/power_policy"
}

###############################################
# # # # # # # SNAPDRAGON BALANCE # # # # # # #
###############################################
snapdragon_balance() {
	# Qualcomm CPU Bus and DRAM frequencies
	for path in /sys/class/devfreq/*cpu-ddr-latfloor*; do
		zeshia "compute" $path/governor
	done &

	for path in /sys/class/devfreq/*cpu*-lat; do
		zeshia "mem_latency" $path/governor
	done &

	for path in /sys/class/devfreq/*cpu-cpu-ddr-bw; do
		zeshia "bw_hwmon" $path/governor
	done &

	for path in /sys/class/devfreq/*cpu-cpu-llcc-bw; do
		zeshia "bw_hwmon" $path/governor
	done &

	if [ -d /sys/devices/system/cpu/bus_dcvs/LLCC ]; then
		max_freq=$(cat /sys/devices/system/cpu/bus_dcvs/LLCC/available_frequencies | tr ' ' '\n' | sort -nr | head -n 1)
		min_freq=$(cat /sys/devices/system/cpu/bus_dcvs/LLCC/available_frequencies | tr ' ' '\n' | sort -n | head -n 1)
		for path in /sys/devices/system/cpu/bus_dcvs/LLCC/*/max_freq; do
			zeshia $max_freq $path
		done &
		for path in /sys/devices/system/cpu/bus_dcvs/LLCC/*/min_freq; do
			zeshia $min_freq $path
		done &
	fi

	if [ -d /sys/devices/system/cpu/bus_dcvs/L3 ]; then
		max_freq=$(cat /sys/devices/system/cpu/bus_dcvs/L3/available_frequencies | tr ' ' '\n' | sort -nr | head -n 1)
		min_freq=$(cat /sys/devices/system/cpu/bus_dcvs/L3/available_frequencies | tr ' ' '\n' | sort -n | head -n 1)
		for path in /sys/devices/system/cpu/bus_dcvs/L3/*/max_freq; do
			zeshia $max_freq $path
		done &
		for path in /sys/devices/system/cpu/bus_dcvs/L3/*/min_freq; do
			zeshia $min_freq $path
		done &
	fi

	if [ -d /sys/devices/system/cpu/bus_dcvs/DDR ]; then
		max_freq=$(cat /sys/devices/system/cpu/bus_dcvs/DDR/available_frequencies | tr ' ' '\n' | sort -nr | head -n 1)
		min_freq=$(cat /sys/devices/system/cpu/bus_dcvs/DDR/available_frequencies | tr ' ' '\n' | sort -n | head -n 1)
		for path in /sys/devices/system/cpu/bus_dcvs/DDR/*/max_freq; do
			zeshia $max_freq $path
		done &
		for path in /sys/devices/system/cpu/bus_dcvs/DDR/*/min_freq; do
			zeshia $min_freq $path
		done &
	fi

	if [ -d /sys/devices/system/cpu/bus_dcvs/DDRQOS ]; then
		max_freq=$(cat /sys/devices/system/cpu/bus_dcvs/DDRQOS/available_frequencies | tr ' ' '\n' | sort -nr | head -n 1)
		min_freq=$(cat /sys/devices/system/cpu/bus_dcvs/DDRQOS/available_frequencies | tr ' ' '\n' | sort -n | head -n 1)
		for path in /sys/devices/system/cpu/bus_dcvs/DDRQOS/*/max_freq; do
			zeshia $max_freq $path
		done &
		for path in /sys/devices/system/cpu/bus_dcvs/DDRQOS/*/min_freq; do
			zeshia $min_freq $path
		done &
	fi

	# GPU Frequency
	gpu_path="/sys/class/kgsl/kgsl-3d0/devfreq"

	if [ -d $gpu_path ]; then
		max_freq=$(cat $gpu_path/available_frequencies | tr ' ' '\n' | sort -nr | head -n 1)
		min_freq=$(cat $gpu_path/available_frequencies | tr ' ' '\n' | sort -n | head -n 2)
		zeshia $min_freq $gpu_path/min_freq
		zeshia $max_freq $gpu_path/max_freq
	fi

	# GPU Bus
	for path in /sys/class/devfreq/*gpubw*; do
		zeshia "bw_vbif" $path/governor
	done &

	# Adreno Boost
	zeshia 1 /sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost
}

###############################################
# # # # # # # EXYNOS BALANCE # # # # # # #
###############################################
exynos_balance() {
	# GPU Frequency
	gpu_path="/sys/kernel/gpu"
	[ -d "$gpu_path" ] && {
		max_freq=$(which_maxfreq "$gpu_path/gpu_available_frequencies")
		min_freq=$(which_minfreq "$gpu_path/gpu_available_frequencies")
		zeshia "$max_freq" "$gpu_path/gpu_max_clock"
		zeshia "$min_freq" "$gpu_path/gpu_min_clock"
	}

	mali_sysfs=$(find /sys/devices/platform/ -iname "*.mali" -print -quit 2>/dev/null)
	zeshia coarse_demand "$mali_sysfs/power_policy"

	# DRAM frequency
	[ $DEVICE_MITIGATION -eq 0 ] && {
		for path in /sys/class/devfreq/*devfreq_mif*; do
			devfreq_unlock "$path"
		done &
	}
}

###############################################
# # # # # # # UNISOC BALANCE # # # # # # #
###############################################
unisoc_balance() {
	# GPU Frequency
	gpu_path=$(find /sys/class/devfreq/ -type d -iname "*.gpu" -print -quit 2>/dev/null)
	[ -n "$gpu_path" ] && devfreq_unlock "$gpu_path"
}

###############################################
# # # # # # # TENSOR BALANCE # # # # # # #
###############################################
tensor_balance() {
	# GPU Frequency
	gpu_path=$(find /sys/devices/platform/ -type d -iname "*.mali" -print -quit 2>/dev/null)
	[ -n "$gpu_path" ] && {
		max_freq=$(which_maxfreq "$gpu_path/available_frequencies")
		min_freq=$(which_minfreq "$gpu_path/available_frequencies")
		zeshia "$max_freq" "$gpu_path/scaling_max_freq"
		zeshia "$min_freq" "$gpu_path/scaling_min_freq"
	}

	# DRAM frequency
	[ $DEVICE_MITIGATION -eq 0 ] && {
		for path in /sys/class/devfreq/*devfreq_mif*; do
			devfreq_unlock "$path"
		done &
	}
}

###############################################
# # # # # # # MEDIATEK PERFORMANCE # # # # # # #
###############################################
mediatek_performance() {
	# PPM Settings
	if [ -d /proc/ppm ]; then
		if [ -f /proc/ppm/policy_status ]; then
			for idx in $(grep -E 'FORCE_LIMIT|PWR_THRO|THERMAL|USER_LIMIT' /proc/ppm/policy_status | awk -F'[][]' '{print $2}'); do
				zeshia "$idx 0" "/proc/ppm/policy_status"
			done

			for dx in $(grep -E 'SYS_BOOST' /proc/ppm/policy_status | awk -F'[][]' '{print $2}'); do
				zeshia "$dx 1" "/proc/ppm/policy_status"
			done
		fi
	fi

	# CPU Power Mode
	zeshia "1" "/proc/cpufreq/cpufreq_cci_mode"
	zeshia "3" "/proc/cpufreq/cpufreq_power_mode"

	# Max GPU Frequency
	if [ -d /proc/gpufreq ]; then
		gpu_freq="$(cat /proc/gpufreq/gpufreq_opp_dump | grep -o 'freq = [0-9]*' | sed 's/freq = //' | sort -nr | head -n 1)"
		zeshia "$gpu_freq" /proc/gpufreq/gpufreq_opp_freq
	elif [ -d /proc/gpufreqv2 ]; then
		zeshia 0 /proc/gpufreqv2/fix_target_opp_index
	fi

	# EAS/HMP Switch
	zeshia "0" /sys/devices/system/cpu/eas/enable

	# Disable GPU Power limiter
	[ -f "/proc/gpufreq/gpufreq_power_limited" ] && {
		for setting in ignore_batt_oc ignore_batt_percent ignore_low_batt ignore_thermal_protect ignore_pbm_limited; do
			zeshia "$setting 1" /proc/gpufreq/gpufreq_power_limited
		done
	}

	# Batoc battery and Power Limiter
	zeshia "0" /proc/perfmgr/syslimiter/syslimiter_force_disable
	zeshia "stop 1" /proc/mtk_batoc_throttling/battery_oc_protect_stop

	# Disable battery current limiter
	zeshia "stop 1" /proc/mtk_batoc_throttling/battery_oc_protect_stop

	# Eara Thermal
	zeshia "0" /sys/kernel/eara_thermal/enable

	# UFS Governor's
	zeshia "0" "/sys/devices/platform/10012000.dvfsrc/helio-dvfsrc/dvfsrc_req_ddr_opp"
	zeshia "0" "/sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp"
	zeshia "performance" "/sys/class/devfreq/mtk-dvfsrc-devfreq/governor"
	zeshia "performance" "/sys/devices/platform/soc/1c00f000.dvfsrc/mtk-dvfsrc-devfreq/devfreq/mtk-dvfsrc-devfreq/governor"
	
	mali_sysfs=$(find /sys/devices/platform/ -iname "*.mali" -print -quit 2>/dev/null)
	zeshia always_on "$mali_sysfs/power_policy"

}

###############################################
# # # # # # # SNAPDRAGON PERFORMANCE # # # # # # #
###############################################
snapdragon_performance() {
	# Qualcomm CPU Bus and DRAM frequencies
	for path in /sys/class/devfreq/*cpu-ddr-latfloor*; do
		zeshia "performance" $path/governor
	done &

	for path in /sys/class/devfreq/*cpu*-lat; do
		zeshia "performance" $path/governor
	done &

	for path in /sys/class/devfreq/*cpu-cpu-ddr-bw; do
		zeshia "performance" $path/governor
	done &

	for path in /sys/class/devfreq/*cpu-cpu-llcc-bw; do
		zeshia "performance" $path/governor
	done &

	if [ -d /sys/devices/system/cpu/bus_dcvs/LLCC ]; then
		freq=$(cat /sys/devices/system/cpu/bus_dcvs/LLCC/available_frequencies | tr ' ' '\n' | sort -nr | head -n 1)
		for path in /sys/devices/system/cpu/bus_dcvs/LLCC/*/max_freq; do
			zeshia $freq $path
		done &
		for path in /sys/devices/system/cpu/bus_dcvs/LLCC/*/min_freq; do
			zeshia $freq $path
		done &
	fi

	if [ -d /sys/devices/system/cpu/bus_dcvs/L3 ]; then
		freq=$(cat /sys/devices/system/cpu/bus_dcvs/L3/available_frequencies | tr ' ' '\n' | sort -nr | head -n 1)
		for path in /sys/devices/system/cpu/bus_dcvs/L3/*/max_freq; do
			zeshia $freq $path
		done &
		for path in /sys/devices/system/cpu/bus_dcvs/L3/*/min_freq; do
			zeshia $freq $path
		done &
	fi

	if [ -d /sys/devices/system/cpu/bus_dcvs/DDR ]; then
		freq=$(cat /sys/devices/system/cpu/bus_dcvs/DDR/available_frequencies | tr ' ' '\n' | sort -nr | head -n 1)
		for path in /sys/devices/system/cpu/bus_dcvs/DDR/*/max_freq; do
			zeshia $freq $path
		done &
		for path in /sys/devices/system/cpu/bus_dcvs/DDR/*/min_freq; do
			zeshia $freq $path
		done &
	fi

	if [ -d /sys/devices/system/cpu/bus_dcvs/DDRQOS ]; then
		freq=$(cat /sys/devices/system/cpu/bus_dcvs/DDRQOS/available_frequencies | tr ' ' '\n' | sort -nr | head -n 1)
		for path in /sys/devices/system/cpu/bus_dcvs/DDRQOS/*/max_freq; do
			zeshia $freq $path
		done &
		for path in /sys/devices/system/cpu/bus_dcvs/DDRQOS/*/min_freq; do
			zeshia $freq $path
		done &
	fi

	# GPU Frequency
	gpu_path="/sys/class/kgsl/kgsl-3d0/devfreq"

	if [ -d $gpu_path ]; then
		freq=$(cat $gpu_path/available_frequencies | tr ' ' '\n' | sort -nr | head -n 1)
		zeshia $freq $gpu_path/min_freq
		zeshia $freq $gpu_path/max_freq
	fi

	# GPU Bus
	for path in /sys/class/devfreq/*gpubw*; do
		zeshia "performance" $path/governor
	done &

	# Adreno Boost
	zeshia 3 /sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost
}

###############################################
# # # # # # # EXYNOS PERFORMANCE # # # # # # #
###############################################
exynos_performance() {
	# GPU Frequency
	gpu_path="/sys/kernel/gpu"
	[ -d "$gpu_path" ] && {
		max_freq=$(which_maxfreq "$gpu_path/gpu_available_frequencies")
		zeshia "$max_freq" "$gpu_path/gpu_max_clock"

		if [ $LITE_MODE -eq 1 ]; then
			mid_freq=$(which_midfreq "$gpu_path/gpu_available_frequencies")
			zeshia "$mid_freq" "$gpu_path/gpu_min_clock"
		else
			zeshia "$max_freq" "$gpu_path/gpu_min_clock"
		fi
	}

	mali_sysfs=$(find /sys/devices/platform/ -iname "*.mali" -print -quit 2>/dev/null)
	zeshia always_on "$mali_sysfs/power_policy"

	# DRAM and Buses Frequency
	[ $DEVICE_MITIGATION -eq 0 ] && {
		for path in /sys/class/devfreq/*devfreq_mif*; do
			[ $LITE_MODE -eq 1 ] &&
				devfreq_mid_perf "$path" ||
				devfreq_max_perf "$path"
		done &
	}
}

###############################################
# # # # # # # UNISOC PERFORMANCE # # # # # # #
###############################################
unisoc_performance() {
	# GPU Frequency
	gpu_path=$(find /sys/class/devfreq/ -type d -iname "*.gpu" -print -quit 2>/dev/null)
	[ -n "$gpu_path" ] && {
		if [ $LITE_MODE -eq 0 ]; then
			devfreq_max_perf "$gpu_path"
		else
			devfreq_mid_perf "$gpu_path"
		fi
	}
}

###############################################
# # # # # # # TENSOR PERFORMANCE # # # # # # #
###############################################
tensor_performance() {
	# GPU Frequency
	gpu_path=$(find /sys/devices/platform/ -type d -iname "*.mali" -print -quit 2>/dev/null)
	[ -n "$gpu_path" ] && {
		max_freq=$(which_maxfreq "$gpu_path/available_frequencies")
		zeshia "$max_freq" "$gpu_path/scaling_max_freq"

		if [ $LITE_MODE -eq 1 ]; then
			mid_freq=$(which_midfreq "$gpu_path/available_frequencies")
			zeshia "$mid_freq" "$gpu_path/scaling_min_freq"
		else
			zeshia "$max_freq" "$gpu_path/scaling_min_freq"
		fi
	}

	# DRAM frequency
	[ $DEVICE_MITIGATION -eq 0 ] && {
		for path in /sys/class/devfreq/*devfreq_mif*; do
			[ $LITE_MODE -eq 1 ] &&
				devfreq_mid_perf "$path" ||
				devfreq_max_perf "$path"
		done &
	}
}

###############################################
# # # # # # # MEDIATEK POWERSAVE # # # # # # #
###############################################
mediatek_powersave() {
	# PPM Settings
	if [ -d /proc/ppm ]; then
		if [ -f /proc/ppm/policy_status ]; then
			for idx in $(grep -E 'FORCE_LIMIT|PWR_THRO|THERMAL|USER_LIMIT' /proc/ppm/policy_status | awk -F'[][]' '{print $2}'); do
				zeshia "$idx 1" "/proc/ppm/policy_status"
			done

			for dx in $(grep -E 'SYS_BOOST' /proc/ppm/policy_status | awk -F'[][]' '{print $2}'); do
				zeshia "$dx 0" "/proc/ppm/policy_status"
			done
		fi
	fi

	# UFS governor
	zeshia "0" "/sys/devices/platform/10012000.dvfsrc/helio-dvfsrc/dvfsrc_req_ddr_opp"
	zeshia "0" "/sys/kernel/helio-dvfsrc/dvfsrc_force_vcore_dvfs_opp"
	zeshia "powersave" "/sys/class/devfreq/mtk-dvfsrc-devfreq/governor"
	zeshia "powersave" "/sys/devices/platform/soc/1c00f000.dvfsrc/mtk-dvfsrc-devfreq/devfreq/mtk-dvfsrc-devfreq/governor"

	# GPU Power limiter - Performance mode (not for Powersave)
	[ -f "/proc/gpufreq/gpufreq_power_limited" ] && {
		for setting in ignore_batt_oc ignore_batt_percent ignore_low_batt ignore_thermal_protect ignore_pbm_limited; do
			zeshia "$setting 1" /proc/gpufreq/gpufreq_power_limited
		done

	}

	# Batoc Throttling and Power Limiter>
	zeshia "0" /proc/perfmgr/syslimiter/syslimiter_force_disable
	zeshia "stop 0" /proc/mtk_batoc_throttling/battery_oc_protect_stop
	# Enable Power Budget management for new 5.x mtk kernels
	zeshia "stop 0" /proc/pbm/pbm_stop

	# Enable battery current limiter
	zeshia "stop 0" /proc/mtk_batoc_throttling/battery_oc_protect_stop

	# Eara Thermal
	zeshia "1" /sys/kernel/eara_thermal/enable
	
	mali_sysfs=$(find /sys/devices/platform/ -iname "*.mali" -print -quit 2>/dev/null)
	zeshia coarse_demand "$mali_sysfs/power_policy"

}

###############################################
# # # # # # # SNAPDRAGON POWERSAVE # # # # # # #
###############################################
snapdragon_powersave() {
	# Qualcomm CPU Bus and DRAM frequencies
	for path in /sys/class/devfreq/*cpu-ddr-latfloor*; do
		zeshia "powersave" $path/governor
	done &

	for path in /sys/class/devfreq/*cpu*-lat; do
		zeshia "powersave" $path/governor
	done &

	for path in /sys/class/devfreq/*cpu-cpu-ddr-bw; do
		zeshia "powersave" $path/governor
	done &

	for path in /sys/class/devfreq/*cpu-cpu-llcc-bw; do
		zeshia "powersave" $path/governor
	done &

	if [ -d /sys/devices/system/cpu/bus_dcvs/LLCC ]; then
		freq=$(cat /sys/devices/system/cpu/bus_dcvs/LLCC/available_frequencies | tr ' ' '\n' | sort -n | head -n 1)
		for path in /sys/devices/system/cpu/bus_dcvs/LLCC/*/max_freq; do
			zeshia $freq $path
		done &
		for path in /sys/devices/system/cpu/bus_dcvs/LLCC/*/min_freq; do
			zeshia $freq $path
		done &
	fi

	if [ -d /sys/devices/system/cpu/bus_dcvs/L3 ]; then
		freq=$(cat /sys/devices/system/cpu/bus_dcvs/L3/available_frequencies | tr ' ' '\n' | sort -n | head -n 1)
		for path in /sys/devices/system/cpu/bus_dcvs/L3/*/max_freq; do
			zeshia $freq $path
		done &
		for path in /sys/devices/system/cpu/bus_dcvs/L3/*/min_freq; do
			zeshia $freq $path
		done &
	fi

	if [ -d /sys/devices/system/cpu/bus_dcvs/DDR ]; then
		freq=$(cat /sys/devices/system/cpu/bus_dcvs/DDR/available_frequencies | tr ' ' '\n' | sort -n | head -n 1)
		for path in /sys/devices/system/cpu/bus_dcvs/DDR/*/max_freq; do
			zeshia $freq $path
		done &
		for path in /sys/devices/system/cpu/bus_dcvs/DDR/*/min_freq; do
			zeshia $freq $path
		done &
	fi

	if [ -d /sys/devices/system/cpu/bus_dcvs/DDRQOS ]; then
		freq=$(cat /sys/devices/system/cpu/bus_dcvs/DDRQOS/available_frequencies | tr ' ' '\n' | sort -n | head -n 1)
		for path in /sys/devices/system/cpu/bus_dcvs/DDRQOS/*/max_freq; do
			zeshia $freq $path
		done &
		for path in /sys/devices/system/cpu/bus_dcvs/DDRQOS/*/min_freq; do
			zeshia $freq $path
		done &
	fi

	# GPU Frequency
	gpu_path="/sys/class/kgsl/kgsl-3d0/devfreq"

	if [ -d $gpu_path ]; then
		freq=$(cat $gpu_path/available_frequencies | tr ' ' '\n' | sort -n | head -n 2)
		zeshia $freq $gpu_path/min_freq
		zeshia $freq $gpu_path/max_freq
	fi

	# GPU Bus
	for path in /sys/class/devfreq/*gpubw*; do
		zeshia "powersave" $path/governor
	done &

	# Adreno Boost
	zeshia 0 /sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost
}

###############################################
# # # # # # # EXYNOS POWERSAVE # # # # # # #
###############################################
exynos_powersave() {
	# GPU Frequency
	gpu_path="/sys/kernel/gpu"
	[ -d "$gpu_path" ] && {
		freq=$(which_minfreq "$gpu_path/gpu_available_frequencies")
		zeshia "$freq" "$gpu_path/gpu_min_clock"
		zeshia "$freq" "$gpu_path/gpu_max_clock"
	}
}

###############################################
# # # # # # # UNISOC POWERSAVE # # # # # # #
###############################################
unisoc_powersave() {
	# GPU Frequency
	gpu_path=$(find /sys/class/devfreq/ -type d -iname "*.gpu" -print -quit 2>/dev/null)
	[ -n "$gpu_path" ] && devfreq_min_perf "$gpu_path"
}

###############################################
# # # # # # # TENSOR POWERSAVE # # # # # # #
###############################################
tensor_powersave() {
	# GPU Frequency
	gpu_path=$(find /sys/devices/platform/ -type d -iname "*.mali" -print -quit 2>/dev/null)
	[ -n "$gpu_path" ] && {
		freq=$(which_minfreq "$gpu_path/available_frequencies")
		zeshia "$freq" "$gpu_path/scaling_min_freq"
		zeshia "$freq" "$gpu_path/scaling_max_freq"
	}
}

###############################################

###############################################

###############################################
###############################################
# # # # # # # PROFILESCRIPT # # # # # # #
###############################################

###############################################
# # # # # # # PERFORMANCE PROFILE! # # # # # # #
###############################################

performance_profile() {
	# Load Default Governor
	load_default_governor() {
		if [ -n "$(getprop persist.sys.azenith.custom_default_cpu_gov)" ]; then
			getprop persist.sys.azenith.custom_default_cpu_gov
		elif [ -n "$(getprop persist.sys.azenith.default_cpu_gov)" ]; then
			getprop persist.sys.azenith.default_cpu_gov
		else
			echo "performance"
		fi
	}
	# Apply Game Governor
	default_cpu_gov=$(load_default_governor)		
	if [ "$LITEMODE" -eq 0 ]; then
        setgov "performance"
        dlog "Applying global governor: performance"    
    else
        BIG_POLICY=$(get_biggest_cluster)
        chmod 644 /sys/devices/system/cpu/cpufreq/$BIG_POLICY/scaling_governor
    	echo "performance" | tee /sys/devices/system/cpu/cpufreq/$BIG_POLICY/scaling_governor >/dev/null
    	chmod 444 /sys/devices/system/cpu/cpufreq/$BIG_POLICY/scaling_governor
        dlog "Applying performance only to biggest cluster: $BIG_POLICY"
    fi

	# Load Default I/O Scheduler
	load_default_iosched() {
		if [ -n "$(getprop persist.sys.azenith.custom_default_balanced_IO)" ]; then
			getprop persist.sys.azenith.custom_default_balanced_IO
		elif [ -n "$(getprop persist.sys.azenith.default_balanced_IO)" ]; then
			getprop persist.sys.azenith.default_balanced_IO
		else
			echo "none"
		fi
	}
	# Apply Game I/O Scheduler
	default_iosched=$(load_default_iosched)
	if [ -n "$(getprop persist.sys.azenith.custom_performance_IO)" ]; then
		game_iosched=$(getprop persist.sys.azenith.custom_performance_IO)
		setsIO "$game_iosched" && dlog "Applying I/O scheduler to : $game_iosched"
	else
		setsIO "$default_iosched" && dlog "Applying I/O scheduler to : $default_iosched"
	fi
	
	if [ "$MALI_COMP" -eq 1 ]; then
    	# Load Default GPU Mali Gov
    	load_default_gpumaligov() {
    		if [ -n "$(getprop persist.sys.azenith.custom_default_gpumali_gov)" ]; then
    			getprop persist.sys.azenith.custom_default_gpumali_gov
    		elif [ -n "$(getprop persist.sys.azenith.default_gpumali_gov)" ]; then
    			getprop persist.sys.azenith.default_gpumali_gov
    		else
    			echo "dummy"
    		fi
    	}
    	# Apply Game GPU Mali Gov
    	default_maligov=$(load_default_gpumaligov)
    	if [ -n "$(getprop persist.sys.azenith.custom_performance_gpumali_gov)" ]; then
    		game_maligov=$(getprop persist.sys.azenith.custom_performance_gpumali_gov)
    		setsGPUMali "$game_maligov" && dlog "Applying GPU Mali Governor to : $game_maligov"
    	else
    		setsGPUMali "$default_maligov" && dlog "Applying GPU Mali Governor to : $default_maligov"
    	fi
    fi

	# Set DND Mode
	if [ "$DND_STATE" -eq 1 ]; then
		cmd notification set_dnd priority && dlog "DND enabled" || dlog "Failed to enable DND"
	fi

    # Bypass Charge
	if [ "$BYPASSCHG_STATE" -eq 1 ]; then
		sys.azenith-utilityconf enableBypass
	fi
	
	# Fix Target OPP Index
	if [ -d /proc/ppm ]; then
	    setgamefreqppm 
	else
	    setgamefreq
	fi
	if [ "$LITEMODE" -eq 0 ]; then	
	    dlog "Set CPU freq to max available Frequencies"
	else
	    dlog "Set CPU freq to normal Frequencies"
	fi
	
    # Power level settings
	for pl in /sys/devices/system/cpu/perf; do
		zeshia 1 "$pl/gpu_pmu_enable"
		zeshia 1 "$pl/fuel_gauge_enable"
		zeshia 1 "$pl/enable"
		zeshia 1 "$pl/charger_enable"
	done
	
	# VM Cache Pressure
	zeshia "40" "/proc/sys/vm/vfs_cache_pressure"
	zeshia "3" "/proc/sys/vm/drop_caches"

	# Workqueue settings
	zeshia "N" /sys/module/workqueue/parameters/power_efficient
	zeshia "N" /sys/module/workqueue/parameters/disable_numa
	zeshia "0" /sys/kernel/eara_thermal/enable
	zeshia "0" /sys/devices/system/cpu/eas/enable
	zeshia "1" /sys/devices/system/cpu/cpu2/online
	zeshia "1" /sys/devices/system/cpu/cpu3/online

	# Schedtune Settings
	for path in /dev/stune/*; do
		base=$(basename "$path")
		if [[ "$base" == "top-app" || "$base" == "foreground" ]]; then
			zeshia 30 "$path/schedtune.boost"
			zeshia 1 "$path/schedtune.sched_boost_enabled"
		else
			zeshia 30 "$path/schedtune.boost"
			zeshia 1 "$path/schedtune.sched_boost_enabled"
		fi
		zeshia 0 "$path/schedtune.prefer_idle"
		zeshia 0 "$path/schedtune.colocate"
	done

	# Power level settings
	for pl in /sys/devices/system/cpu/perf; do
		zeshia 1 "$pl/gpu_pmu_enable"
		zeshia 1 "$pl/fuel_gauge_enable"
		zeshia 1 "$pl/enable"
		zeshia 1 "$pl/charger_enable"
	done

	# CPU max tune percent
	zeshia 1 /proc/sys/kernel/perf_cpu_time_max_percent

	# Sched Energy Aware
	zeshia 1 /proc/sys/kernel/sched_energy_aware
	# CPU Core control Boost
	for cpucore in /sys/devices/system/cpu/cpu*; do
		zeshia 0 "$cpucore/core_ctl/enable"
		zeshia 0 "$cpucore/core_ctl/core_ctl_boost"
	done

	# Disable battery saver module
	[ -f /sys/module/battery_saver/parameters/enabled ] && {
		if grep -qo '[0-9]\+' /sys/module/battery_saver/parameters/enabled; then
			zeshia 0 /sys/module/battery_saver/parameters/enabled
		else
			zeshia N /sys/module/battery_saver/parameters/enabled
		fi
	}

	# Disable split lock mitigation
	zeshia 0 /proc/sys/kernel/split_lock_mitigate

	# Schedfeatures settings
	if [ -f "/sys/kernel/debug/sched_features" ]; then
		zeshia NEXT_BUDDY /sys/kernel/debug/sched_features
		zeshia NO_TTWU_QUEUE /sys/kernel/debug/sched_features
	fi
		
	if [ "$CLEARAPPS" -eq 1 ]; then
		clear_background_apps $
	fi

	if [ "$LITEMODE" -eq 0 ]; then
		case "$(getprop persist.sys.azenithdebug.soctype)" in
		1) mediatek_performance ;;
		2) snapdragon_performance ;;
		3) exynos_performance ;;
		4) unisoc_performance ;;
		5) tensor_performance ;;
		esac
	fi

	AZLog "Performance Profile Applied Successfully!"

}

###############################################
# # # # # # #  BALANCED PROFILES! # # # # # # #
###############################################

balanced_profile() {
	# Load Default Governor
	load_default_governor() {
		if [ -n "$(getprop persist.sys.azenith.custom_default_cpu_gov)" ]; then
			getprop persist.sys.azenith.custom_default_cpu_gov
		elif [ -n "$(getprop persist.sys.azenith.default_cpu_gov)" ]; then
			getprop persist.sys.azenith.default_cpu_gov
		else
			echo "schedutil"
		fi
	}
	default_cpu_gov=$(load_default_governor)
	setgov "$default_cpu_gov"
	dlog "Applying governor to : $default_cpu_gov"

	# Load Default I/O Scheduler
	load_default_iosched() {
		if [ -n "$(getprop persist.sys.azenith.custom_default_balanced_IO)" ]; then
			getprop persist.sys.azenith.custom_default_balanced_IO
		elif [ -n "$(getprop persist.sys.azenith.default_balanced_IO)" ]; then
			getprop persist.sys.azenith.default_balanced_IO
		else
			echo "none"
		fi
	}
	default_iosched=$(load_default_iosched)
	setsIO "$default_iosched"
	dlog "Applying I/O scheduler to : $default_iosched"
	
	if [ "$MALI_COMP" -eq 1 ]; then
    	# Load Default GPU Mali Gov
    	load_default_gpumaligov() {
    		if [ -n "$(getprop persist.sys.azenith.custom_default_gpumali_gov)" ]; then
    			getprop persist.sys.azenith.custom_default_gpumali_gov
    		elif [ -n "$(getprop persist.sys.azenith.default_gpumali_gov)" ]; then
    			getprop persist.sys.azenith.default_gpumali_gov
    		else
    			echo "dummy"
    		fi
    	}
    	# Apply GPU Mali Gov
    	default_maligov=$(load_default_gpumaligov)
        setsGPUMali "$default_maligov" && dlog "Applying GPU Mali Governor to : $default_maligov"
    fi
		
	# Disable DND
	if [ "$DND_STATE" -eq 1 ]; then
		cmd notification set_dnd off && dlog "DND disabled" || dlog "Failed to disable DND"
	fi

    # Bypass Charge
	if [ "$BYPASSCHG_STATE" -eq 1 ]; then
		sys.azenith-utilityconf disableBypass
	fi

	# Limit cpu freq
	if [ -d /proc/ppm ]; then
	    setfreqppm
	else
	    setfreq
	fi
	if [ "$(getprop persist.sys.azenithconf.freqoffset)" = "Disabled" ]; then
        dlog "Set CPU freq to normal Frequencies"
    else
	    dlog "Set CPU freq to normal selected Frequencies"
	fi
	
	# Power level settings
	for pl in /sys/devices/system/cpu/perf; do
		zeshia 0 "$pl/gpu_pmu_enable"
		zeshia 0 "$pl/fuel_gauge_enable"
		zeshia 0 "$pl/enable"
		zeshia 1 "$pl/charger_enable"
	done

	# vm cache pressure
	zeshia "120" "/proc/sys/vm/vfs_cache_pressure"

	# Workqueue settings
	zeshia "Y" /sys/module/workqueue/parameters/power_efficient
	zeshia "Y" /sys/module/workqueue/parameters/disable_numa
	zeshia "1" /sys/kernel/eara_thermal/enable
	zeshia "1" /sys/devices/system/cpu/eas/enable

	for path in /dev/stune/*; do
		base=$(basename "$path")
		if [[ "$base" == "top-app" || "$base" == "foreground" ]]; then
			zeshia 0 "$path/schedtune.boost"
			zeshia 0 "$path/schedtune.sched_boost_enabled"
		else
			zeshia 0 "$path/schedtune.boost"
			zeshia 0 "$path/schedtune.sched_boost_enabled"
		fi
		zeshia 0 "$path/schedtune.prefer_idle"
		zeshia 0 "$path/schedtune.colocate"
	done

	# CPU Max Time Percent
	zeshia 100 /proc/sys/kernel/perf_cpu_time_max_percent

	zeshia 2 /proc/sys/kernel/perf_cpu_time_max_percent
	# Sched Energy Aware
	zeshia 1 /proc/sys/kernel/sched_energy_aware

	for cpucore in /sys/devices/system/cpu/cpu*; do
		zeshia 0 "$cpucore/core_ctl/enable"
		zeshia 0 "$cpucore/core_ctl/core_ctl_boost"
	done

	#  Disable battery saver module
	[ -f /sys/module/battery_saver/parameters/enabled ] && {
		if grep -qo '[0-9]\+' /sys/module/battery_saver/parameters/enabled; then
			zeshia 0 /sys/module/battery_saver/parameters/enabled
		else
			zeshia N /sys/module/battery_saver/parameters/enabled
		fi
	}

	#  Enable split lock mitigation
	zeshia 1 /proc/sys/kernel/split_lock_mitigate

    # Schedfeature
	if [ -f "/sys/kernel/debug/sched_features" ]; then
		zeshia NEXT_BUDDY /sys/kernel/debug/sched_features
		zeshia TTWU_QUEUE /sys/kernel/debug/sched_features
	fi

	if [ "$LITEMODE" -eq 0 ]; then
		case "$(getprop persist.sys.azenithdebug.soctype)" in
		1) mediatek_balance ;;
		2) snapdragon_balance ;;
		3) exynos_balance ;;
		4) unisoc_balance ;;
		5) tensor_balance ;;
		esac
	fi

	AZLog "Balanced Profile applied successfully!"

}

###############################################
# # # # # # # POWERSAVE PROFILE # # # # # # #
###############################################

eco_mode() {
	# Load Powersave Governor
	load_powersave_governor() {
		if [ -n "$(getprop persist.sys.azenith.custom_powersave_cpu_gov)" ]; then
			getprop persist.sys.azenith.custom_powersave_cpu_gov
		else
			echo "powersave"
		fi
	}
	powersave_cpu_gov=$(load_powersave_governor)
	setgov "$powersave_cpu_gov"
	dlog "Applying governor to : $powersave_cpu_gov"

	# Load Powersave I/O Scheduler
	load_powersave_iosched() {
		if [ -n "$(getprop persist.sys.azenith.custom_powersave_IO)" ]; then
			getprop persist.sys.azenith.custom_powersave_IO
		else
			echo "none"
		fi
	}
	powersave_iosched=$(load_powersave_iosched)
	setsIO "$powersave_iosched"
	dlog "Applying I/O scheduler to : $powersave_iosched"
	if [ "$MALI_COMP" -eq 1 ]; then
    	# Load Default GPU Mali Gov
    	load_powersave_gpumaligov() {
    		if [ -n "$(getprop persist.sys.azenith.custom_powersave_gpumali_gov)" ]; then
    			getprop persist.sys.azenith.custom_powersave_gpumali_gov
    		else
    			echo "dummy"
    		fi
    	}
    	# Apply GPU Mali Gov
    	powersave_maligov=$(load_powersave_gpumaligov)
        setsGPUMali "$powersave_maligov" && dlog "Applying GPU Mali Governor to : $powersave_maligov"
    fi
	
	# Limit cpu freq
	if [ -d /proc/ppm ]; then
	    setfreqppm
	else
	    setfreq
	fi
	dlog "Set CPU freq to low Frequencies"

    # Disable DND
	if [ "$DND_STATE" -eq 1 ]; then
		cmd notification set_dnd off && dlog "DND disabled" || dlog "Failed to disable DND"
	fi
	
	# Bypass Charge
	if [ "$BYPASSCHG_STATE" -eq 1 ]; then
		sys.azenith-utilityconf disableBypass
	fi
	
	# Power level settings
	for pl in /sys/devices/system/cpu/perf; do
		zeshia 0 "$pl/gpu_pmu_enable"
		zeshia 0 "$pl/fuel_gauge_enable"
		zeshia 0 "$pl/enable"
		zeshia 1 "$pl/charger_enable"
	done
	
	# VM Cache Pressure
	zeshia "120" "/proc/sys/vm/vfs_cache_pressure"
	
	# Workqueue settings
	zeshia "Y" /sys/module/workqueue/parameters/power_efficient
	zeshia "Y" /sys/module/workqueue/parameters/disable_numa
	zeshia "1" /sys/kernel/eara_thermal/enable
	zeshia "1" /sys/devices/system/cpu/eas/enable

	for path in /dev/stune/*; do
		base=$(basename "$path")
		if [[ "$base" == "top-app" || "$base" == "foreground" ]]; then
			zeshia 0 "$path/schedtune.boost"
			zeshia 0 "$path/schedtune.sched_boost_enabled"
		else
			zeshia 0 "$path/schedtune.boost"
			zeshia 0 "$path/schedtune.sched_boost_enabled"
		fi
		zeshia 0 "$path/schedtune.prefer_idle"
		zeshia 0 "$path/schedtune.colocate"
	done
	
	# CPU Max Time Percent
	zeshia 50 /proc/sys/kernel/perf_cpu_time_max_percent

	zeshia 0 /proc/sys/kernel/perf_cpu_time_max_percent
	# Sched Energy Aware
	zeshia 0 /proc/sys/kernel/sched_energy_aware

	for cpucore in /sys/devices/system/cpu/cpu*; do
		zeshia 0 "$cpucore/core_ctl/enable"
		zeshia 0 "$cpucore/core_ctl/core_ctl_boost"
	done

	#  Enable battery saver module
	[ -f /sys/module/battery_saver/parameters/enabled ] && {
		if grep -qo '[0-9]\+' /sys/module/battery_saver/parameters/enabled; then
			zeshia 1 /sys/module/battery_saver/parameters/enabled
		else
			zeshia Y /sys/module/battery_saver/parameters/enabled
		fi
	}
	
	#  Enable split lock mitigation
	zeshia 1 /proc/sys/kernel/split_lock_mitigate

	# Schedfeature settings
	if [ -f "/sys/kernel/debug/sched_features" ]; then
		zeshia NO_NEXT_BUDDY /sys/kernel/debug/sched_features
		zeshia NO_TTWU_QUEUE /sys/kernel/debug/sched_features
	fi

	case "$(getprop persist.sys.azenithdebug.soctype)" in
	1) mediatek_powersave ;;
	2) snapdragon_powersave ;;
	3) exynos_powersave ;;
	4) unisoc_powersave ;;
	5) tensor_powersave ;;
	esac

	AZLog "ECO Mode applied successfully!"

}

###############################################
# # # # # # # INITIALIZE # # # # # # #
###############################################

initialize() {
	# Disable all kernel panic mechanisms
	for param in hung_task_timeout_secs panic_on_oom panic_on_oops panic softlockup_panic; do
		zeshia "0" "/proc/sys/kernel/$param"
	done

	# Tweaking scheduler to reduce latency
	zeshia 750000 /proc/sys/kernel/sched_migration_cost_ns
	zeshia 1000000 /proc/sys/kernel/sched_min_granularity_ns
	zeshia 600000 /proc/sys/kernel/sched_wakeup_granularity_ns
	# Disable read-ahead for swap devices
	zeshia 0 /proc/sys/vm/page-cluster
	# Update /proc/stat less often to reduce jitter
	zeshia 20 /proc/sys/vm/stat_interval
	# Disable compaction_proactiveness
	zeshia 0 /proc/sys/vm/compaction_proactiveness
	zeshia 255 /proc/sys/kernel/sched_lib_mask_force
	
	MALI=/sys/devices/platform/soc/*.mali
    MALI_GOV=$MALI/devfreq/*.mali/governor
    
    if ls $MALI_GOV >/dev/null 2>&1; then
        setprop sys.azenith.maligovsupport "1"
    
        chmod 644 $MALI_GOV
        defaultgpumali_gov=$(cat $MALI_GOV)
        setprop persist.sys.azenith.default_gpumali_gov "$defaultgpumali_gov"
    
        dlog "Default GPU Mali governor detected: $defaultgpumali_gov"
    
        custom_gov=$(getprop persist.sys.azenith.custom_default_gpumali_gov)
        [ -n "$custom_gov" ] && defaultgpumali_gov="$custom_gov"
    
        dlog "Using GPU Mali governor: $defaultgpumali_gov"
    
        chmod 644 $MALI_GOV
        echo "$defaultgpumali_gov" | tee $MALI_GOV >/dev/null
        chmod 444 $MALI_GOV
    
        [ -z "$(getprop persist.sys.azenith.custom_powersave_gpumali_gov)" ] \
            && setprop persist.sys.azenith.custom_powersave_gpumali_gov "$defaultgpumali_gov"
        [ -z "$(getprop persist.sys.azenith.custom_performance_gpumali_gov)" ] \
            && setprop persist.sys.azenith.custom_performance_gpumali_gov "$defaultgpumali_gov"
    
        dlog "Parsing GPU Mali Governor complete"
    else
        setprop sys.azenith.maligovsupport "0"
    fi

	CPU="/sys/devices/system/cpu/cpu0/cpufreq"
	chmod 644 "$CPU/scaling_governor"
	default_gov=$(cat "$CPU/scaling_governor")
	setprop persist.sys.azenith.default_cpu_gov "$default_gov"
	dlog "Default CPU governor detected: $default_gov"

	# Fallback if default is performance
	if [ "$default_gov" == "performance" ] && [ -z "$(getprop persist.sys.azenith.custom_default_cpu_gov)" ]; then
		dlog "Default governor is 'performance'"
		for gov in scx schedhorizon walt sched_pixel sugov_ext uag schedplus energy_step ondemand schedutil interactive conservative powersave; do
			if grep -q "$gov" "$CPU/scaling_available_governors"; then
				setprop persist.sys.azenith.default_cpu_gov "$gov"
				default_gov="$gov"
				dlog "Fallback governor to: $gov"
				break
			fi
		done
	fi

	# Revert to custom default if exists
	[ -n "$(getprop persist.sys.azenith.custom_default_cpu_gov)" ] && default_gov=$(getprop persist.sys.azenith.custom_default_cpu_gov)
	dlog "Using CPU governor: $default_gov"

	chmod 644 /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
	echo "$default_gov" | tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor >/dev/null
	chmod 444 /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
	chmod 444 /sys/devices/system/cpu/cpufreq/policy*/scaling_governor
	[ -z "$(getprop persist.sys.azenith.custom_powersave_cpu_gov)" ] && setprop persist.sys.azenith.custom_powersave_cpu_gov "$default_gov"
	dlog "Parsing CPU Governor complete"

	# Detect valid block device
    for dev in /sys/block/mmcblk0 /sys/block/mmcblk1 /sys/block/sda /sys/block/sdb /sys/block/sdc; do
        if [ -f "$dev/queue/scheduler" ]; then
            IO="$dev/queue"
            dlog "Detected valid block device: $(basename "$dev")"
            break
        fi
    done
    
    # Validate detection
    [ -z "$IO" ] && {
        dlog "No valid block device with scheduler found"
        exit 1
    }
    chmod 644 "$IO/scheduler"    
    # Detect default IO scheduler (marked with [ ])
    default_io=$(grep -o '\[.*\]' "$IO/scheduler" | tr -d '[]')
    setprop persist.sys.azenith.default_balanced_IO "$default_io"
    dlog "Default IO Scheduler detected: $default_io"
    
    # Use custom property if defined
    if [ -n "$(getprop persist.sys.azenith.custom_default_balanced_IO)" ]; then
        default_io=$(getprop persist.sys.azenith.custom_default_balanced_IO)
    fi    
    
    for block in sda sdb sdc mmcblk0 mmcblk1; do
		if [ -e "/sys/block/$block/queue/scheduler" ]; then
			chmod 644 "/sys/block/$block/queue/scheduler"
			echo "$default_io" | tee "/sys/block/$block/queue/scheduler" >/dev/null
			chmod 444 "/sys/block/$block/queue/scheduler"
		fi
	done

    # Set default for other profiles if not set
    [ -z "$(getprop persist.sys.azenith.custom_powersave_IO)" ] && setprop persist.sys.azenith.custom_powersave_IO "$default_io"
    [ -z "$(getprop persist.sys.azenith.custom_performance_IO)" ] && setprop persist.sys.azenith.custom_performance_IO "$default_io"    
    dlog "Parsing IO Scheduler complete"

	RESO_PROP="persist.sys.azenithconf.resosettings"
	RESO=$(wm size | grep -oE "[0-9]+x[0-9]+" | head -n 1)

	if [ -z "$(getprop $RESO_PROP)" ]; then
		if [ -n "$RESO" ]; then
			setprop "$RESO_PROP" "$RESO"
			dlog "Detected resolution: $RESO"
			dlog "Property $RESO_PROP set successfully"
		else
			dlog "Failed to detect physical resolution"
		fi
	fi

	if [ "$(getprop persist.sys.azenithconf.schemeconfig)" != "1000 1000 1000 1000" ]; then
		# Restore saved display boost
		val=$(getprop persist.sys.azenithconf.schemeconfig)
		r=$(echo $val | awk '{print $1}')
		g=$(echo $val | awk '{print $2}')
		b=$(echo $val | awk '{print $3}')
		s=$(echo $val | awk '{print $4}')
		rf=$(awk "BEGIN {print $r/1000}")
		gf=$(awk "BEGIN {print $g/1000}")
		bf=$(awk "BEGIN {print $b/1000}")
		sf=$(awk "BEGIN {print $s/1000}")
		service call SurfaceFlinger 1015 i32 1 f $rf f 0 f 0 f 0 f 0 f $gf f 0 f 0 f 0 f 0 f $bf f 0 f 0 f 0 f 0 f 1
		service call SurfaceFlinger 1022 f $sf
	fi

    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    # APPLY PREFERENCED SETTINGS IN INITIALIZING
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    # JIT COMPILE
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
	if [ "$JUSTINTIME_STATE" -eq 1 ]; then
        dlog "Applying JIT Compiler"
    
        cmd package list packages -3 | cut -f 2 -d ":" | while IFS= read -r pkg; do
            (
                cmd package compile -m speed-profile "$pkg"
                AZLog "$pkg | Success"
            ) &
        done
    
        wait
    fi
		
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    # SCHED TUNES
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
	if [ "$SCHEDTUNES_STATE" -eq 1 ]; then
        dlog "Applying Schedtunes for Schedutil and Schedhorizon"
    
        settunes() {
            local policy_path="$1"
    
            [ ! -d "$policy_path" ] && return
    
            local freqs
            freqs="$(cat "$policy_path/scaling_available_frequencies" 2>/dev/null)"
            [ -z "$freqs" ] && return
    
            local selected_freqs
            selected_freqs="$(echo "$freqs" | tr ' ' '\n' | sort -rn | head -n 6 | tr '\n' ' ' | sed 's/ $//')"
    
            local num
            num="$(echo "$selected_freqs" | wc -w)"
    
            local up_delay=""
            for i in $(seq 1 "$num"); do
                up_delay="$up_delay $((50 * i))"
            done
            up_delay="${up_delay# }"
    
            local up_rate=6500
            local down_rate=12000
            local rate_limit=7000
    
            local schedhorizon="$policy_path/schedhorizon"
            local schedutil="$policy_path/schedutil"
    
            if [ -d "$schedhorizon" ]; then
                [ -f "$schedhorizon/up_delay" ] && zeshia "$up_delay" "$schedhorizon/up_delay"
                [ -f "$schedhorizon/efficient_freq" ] && zeshia "$selected_freqs" "$schedhorizon/efficient_freq"
    
                if [ -f "$schedhorizon/up_rate_limit_us" ]; then
                    zeshia "$up_rate" "$schedhorizon/up_rate_limit_us"
                elif [ -f "$schedhorizon/rate_limit_us" ]; then
                    zeshia "$rate_limit" "$schedhorizon/rate_limit_us"
                fi
    
                if [ -f "$schedhorizon/down_rate_limit_us" ]; then
                    zeshia "$down_rate" "$schedhorizon/down_rate_limit_us"
                fi
            fi
    
            if [ -d "$schedutil" ]; then
                if [ -f "$schedutil/up_rate_limit_us" ]; then
                    zeshia "$up_rate" "$schedutil/up_rate_limit_us"
                elif [ -f "$schedutil/rate_limit_us" ]; then
                    zeshia "$rate_limit" "$schedutil/rate_limit_us"
                fi
    
                [ -f "$schedutil/down_rate_limit_us" ] && zeshia "$down_rate" "$schedutil/down_rate_limit_us"
            fi
        }
    
        for policy in /sys/devices/system/cpu/cpufreq/policy*; do
            settunes "$policy"
        done
    fi

    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    # WALT TUNING
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    if [ "$WALT_STATE" -eq 1 ]; then
        dlog "Applying WALT governor tuning"

        WALT_UP_RATE=8000
        WALT_DOWN_RATE=12000
        WALT_HISPEED_LOAD=92
        WALT_TOP_FREQ_COUNT=6
        WALT_TARGET_START=95
        WALT_TARGET_STEP=8
        WALT_FALLBACK_HISPEED_FREQ=0
        WALT_FALLBACK_RTG_BOOST_FREQ=0
        setwalt() {
            local policy_path="$1"
            local walt_path="$policy_path/walt"

            # Check if path is available 
            if [ ! -d "$walt_path" ]; then
                AZLog "Skipped: $policy_path (WALT not available)"
                return
            fi

            # Read available frequencies
            local available_freqs
            available_freqs=$(cat "$policy_path/scaling_available_frequencies" 2>/dev/null)

            if [ -z "$available_freqs" ]; then
                AZLog "Skipped: No available frequencies for $policy_path"
                return
            fi

            # Select top N frequencies
            local selected_freqs
            selected_freqs=$(echo "$available_freqs" | tr ' ' '\n' | sort -rn | head -n "$WALT_TOP_FREQ_COUNT" | tr '\n' ' ' | sed 's/ $//')

            [ -z "$selected_freqs" ] && return

            # Highest & second highest
            local highest second
            highest=$(echo "$selected_freqs" | awk '{print $1}')
            second=$(echo "$selected_freqs" | awk '{print $2}')
            [ -z "$second" ] && second="$highest"

            # Generate target_loads
            local num_freqs
            num_freqs=$(echo "$selected_freqs" | wc -w)

            local tloads=""
            local cur="$WALT_TARGET_START"
            local i=0

            while [ $i -lt $num_freqs ]; do
                tloads="$tloads $cur"
                cur=$((cur - WALT_TARGET_STEP))
                [ $cur -lt 10 ] && cur=10
                i=$((i + 1))
            done

            tloads=$(echo "$tloads" | sed 's/^ //')

            # Final tuned values
            local hispeed_freq_val="$second"
            local rtg_boost_freq_val="$highest"

            [ -z "$hispeed_freq_val" ] && hispeed_freq_val="$WALT_FALLBACK_HISPEED_FREQ"
            [ -z "$rtg_boost_freq_val" ] && rtg_boost_freq_val="$WALT_FALLBACK_RTG_BOOST_FREQ"

            # Apply safely
            [ -f "$walt_path/hispeed_load" ] && zeshia "$WALT_HISPEED_LOAD" "$walt_path/hispeed_load"
            [ -f "$walt_path/hispeed_freq" ] && zeshia "$hispeed_freq_val" "$walt_path/hispeed_freq"
            [ -f "$walt_path/rtg_boost_freq" ] && zeshia "$rtg_boost_freq_val" "$walt_path/rtg_boost_freq"
            [ -f "$walt_path/target_loads" ] && zeshia "$tloads" "$walt_path/target_loads"
            [ -f "$walt_path/efficient_freq" ] && zeshia "$selected_freqs" "$walt_path/efficient_freq"
            [ -f "$walt_path/up_rate_limit_us" ] && zeshia "$WALT_UP_RATE" "$walt_path/up_rate_limit_us"
            [ -f "$walt_path/down_rate_limit_us" ] && zeshia "$WALT_DOWN_RATE" "$walt_path/down_rate_limit_us"

            dlog "WALT Tuning Applied on $(basename "$policy_path")"
        }
        for policy in /sys/devices/system/cpu/cpufreq/policy*; do
            setwalt "$policy"
        done
    fi

    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    # FPS GO AND GED PARAMETER
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
	if [ "$FPSGED_STATE" -eq 1 ]; then
    dlog "Applying FPSGO Parameters"
		# GED parameters
		ged_params="ged_smart_boost 1 boost_upper_bound 100 enable_gpu_boost 1 enable_cpu_boost 1 ged_boost_enable 1 boost_gpu_enable 1 gpu_dvfs_enable 1 gx_frc_mode 1 gx_dfps 1 gx_force_cpu_boost 1 gx_boost_on 1 gx_game_mode 1 gx_3D_benchmark_on 1 gpu_loading 0 cpu_boost_policy 1 boost_extra 1 is_GED_KPI_enabled 0"

		zeshia "$ged_params" | while read -r param value; do
			zeshia "$value" "/sys/module/ged/parameters/$param"
		done

		# FPSGO Configuration Tweaks
		zeshia "0" /sys/kernel/fpsgo/fbt/boost_ta
		zeshia "1" /sys/kernel/fpsgo/fbt/enable_switch_down_throttle
		zeshia "1" /sys/kernel/fpsgo/fstb/adopt_low_fps
		zeshia "1" /sys/kernel/fpsgo/fstb/fstb_self_ctrl_fps_enable
		zeshia "0" /sys/kernel/fpsgo/fstb/boost_ta
		zeshia "1" /sys/kernel/fpsgo/fstb/enable_switch_sync_flag
		zeshia "0" /sys/kernel/fpsgo/fbt/boost_VIP
		zeshia "1" /sys/kernel/fpsgo/fstb/gpu_slowdown_check
		zeshia "1" /sys/kernel/fpsgo/fbt/thrm_limit_cpu
		zeshia "0" /sys/kernel/fpsgo/fbt/thrm_temp_th
		zeshia "0" /sys/kernel/fpsgo/fbt/llf_task_policy
		zeshia "100" /sys/module/mtk_fpsgo/parameters/uboost_enhance_f
		zeshia "0" /sys/module/mtk_fpsgo/parameters/isolation_limit_cap
		zeshia "1" /sys/pnpmgr/fpsgo_boost/boost_enable
		zeshia "1" /sys/pnpmgr/fpsgo_boost/boost_mode
		zeshia "1" /sys/pnpmgr/install
		zeshia "100" /sys/kernel/ged/hal/gpu_boost_level

	fi

    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    # GPU MALI SCHEDULING
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
	if [ "$MALISCHED_STATE" -eq 1 ]; then
    dlog "Applying GPU Mali Sched"
		# GPU Mali Scheduling
		mali_dir=$(ls -d /sys/devices/platform/soc/*mali*/scheduling 2>/dev/null | head -n 1)
		mali1_dir=$(ls -d /sys/devices/platform/soc/*mali* 2>/dev/null | head -n 1)
		if [ -n "$mali_dir" ]; then
			zeshia "full" "$mali_dir/serialize_jobs"
		fi
		if [ -n "$mali1_dir" ]; then
			zeshia "1" "$mali1_dir/js_ctx_scheduling_mode"
		fi
	fi

    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    # SURFACEFLINGER LATENCY
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
	if [ "$SFL_STATE" -eq 1 ]; then
    dlog "Applying SurfaceFlinger Latency"
		get_stable_refresh_rate() {
			i=0
			while [ $i -lt 5 ]; do
				period=$(dumpsys SurfaceFlinger --latency 2>/dev/null | head -n1 | awk 'NR==1 {print $1}')
				case $period in
				'' | *[!0-9]*) ;;
				*)
					if [ "$period" -gt 0 ]; then
						rate=$(((1000000000 + (period / 2)) / period))
						if [ "$rate" -ge 30 ] && [ "$rate" -le 240 ]; then
							samples="$samples $rate"
						fi
					fi
					;;
				esac
				i=$((i + 1))
				sleep 0.05
			done

			if [ -z "$samples" ]; then
				echo 60
				return
			fi

			sorted=$(echo "$samples" | tr ' ' '\n' | sort -n)
			count=$(echo "$sorted" | wc -l)
			mid=$((count / 2))

			if [ $((count % 2)) -eq 1 ]; then
				median=$(echo "$sorted" | sed -n "$((mid + 1))p")
			else
				val1=$(echo "$sorted" | sed -n "$mid p")
				val2=$(echo "$sorted" | sed -n "$((mid + 1))p")
				median=$(((val1 + val2) / 2))
			fi

			echo "$median"
		}
		refresh_rate=$(get_stable_refresh_rate)

		frame_duration_ns=$(awk -v r="$refresh_rate" 'BEGIN { printf "%.0f", 1000000000 / r }')

		calculate_dynamic_margin() {
			base_margin=0.07
			cpu_load=$(top -n 1 -b 2>/dev/null | grep "Cpu(s)" | awk '{print $2 + $4}')
			margin=$base_margin
			awk -v load="$cpu_load" -v base="$base_margin" 'BEGIN {
			if (load > 70) {
				print base + 0.01
			} else {
				print base
			}
		}'
		}

		margin_ratio=$(calculate_dynamic_margin)
		min_margin=$(awk -v fd="$frame_duration_ns" -v m="$margin_ratio" 'BEGIN { printf "%.0f", fd * m }')

		if [ "$refresh_rate" -ge 120 ]; then
			app_phase_ratio=0.68
			sf_phase_ratio=0.85
			app_duration_ratio=0.58
			sf_duration_ratio=0.32
		elif [ "$refresh_rate" -ge 90 ]; then
			app_phase_ratio=0.66
			sf_phase_ratio=0.82
			app_duration_ratio=0.60
			sf_duration_ratio=0.30
		elif [ "$refresh_rate" -ge 75 ]; then
			app_phase_ratio=0.64
			sf_phase_ratio=0.80
			app_duration_ratio=0.62
			sf_duration_ratio=0.28
		else
			app_phase_ratio=0.62
			sf_phase_ratio=0.75
			app_duration_ratio=0.65
			sf_duration_ratio=0.25
		fi

		app_phase_offset_ns=$(awk -v fd="$frame_duration_ns" -v r="$app_phase_ratio" 'BEGIN { printf "%.0f", -fd * r }')
		sf_phase_offset_ns=$(awk -v fd="$frame_duration_ns" -v r="$sf_phase_ratio" 'BEGIN { printf "%.0f", -fd * r }')

		app_duration=$(awk -v fd="$frame_duration_ns" -v r="$app_duration_ratio" 'BEGIN { printf "%.0f", fd * r }')
		sf_duration=$(awk -v fd="$frame_duration_ns" -v r="$sf_duration_ratio" 'BEGIN { printf "%.0f", fd * r }')

		app_end_time=$(awk -v offset="$app_phase_offset_ns" -v dur="$app_duration" 'BEGIN { print offset + dur }')
		dead_time=$(awk -v app_end="$app_end_time" -v sf_offset="$sf_phase_offset_ns" 'BEGIN { print -(app_end + sf_offset) }')

		adjust_needed=$(awk -v dt="$dead_time" -v mm="$min_margin" 'BEGIN { print (dt < mm) ? 1 : 0 }')
		if [ "$adjust_needed" -eq 1 ]; then
			adjustment=$(awk -v mm="$min_margin" -v dt="$dead_time" 'BEGIN { print mm - dt }')
			new_app_duration=$(awk -v app_dur="$app_duration" -v adj="$adjustment" 'BEGIN { res = app_dur - adj; print (res > 0) ? res : 0 }')
			echo "Optimization: Adjusted app duration by -${adjustment}ns for dynamic margin"
			app_duration=$new_app_duration
		fi

		min_phase_duration=$(awk -v fd="$frame_duration_ns" 'BEGIN { printf "%.0f", fd * 0.12 }')

		app_too_short=$(awk -v dur="$app_duration" -v min="$min_phase_duration" 'BEGIN { print (dur < min) ? 1 : 0 }')
		if [ "$app_too_short" -eq 1 ]; then
			app_duration=$min_phase_duration
		fi

		sf_too_short=$(awk -v dur="$sf_duration" -v min="$min_phase_duration" 'BEGIN { print (dur < min) ? 1 : 0 }')
		if [ "$sf_too_short" -eq 1 ]; then
			sf_duration=$min_phase_duration
		fi

		resetprop -n debug.sf.early.app.duration "$app_duration"
		resetprop -n debug.sf.earlyGl.app.duration "$app_duration"
		resetprop -n debug.sf.late.app.duration "$app_duration"

		resetprop -n debug.sf.early.sf.duration "$sf_duration"
		resetprop -n debug.sf.earlyGl.sf.duration "$sf_duration"
		resetprop -n debug.sf.late.sf.duration "$sf_duration"

		resetprop -n debug.sf.early_app_phase_offset_ns "$app_phase_offset_ns"
		resetprop -n debug.sf.high_fps_early_app_phase_offset_ns "$app_phase_offset_ns"
		resetprop -n debug.sf.high_fps_late_app_phase_offset_ns "$app_phase_offset_ns"
		resetprop -n debug.sf.early_phase_offset_ns "$sf_phase_offset_ns"
		resetprop -n debug.sf.high_fps_early_phase_offset_ns "$sf_phase_offset_ns"
		resetprop -n debug.sf.high_fps_late_sf_phase_offset_ns "$sf_phase_offset_ns"
		if [ "$refresh_rate" -ge 120 ]; then
			threshold_ratio=0.28
		elif [ "$refresh_rate" -ge 90 ]; then
			threshold_ratio=0.32
		elif [ "$refresh_rate" -ge 75 ]; then
			threshold_ratio=0.35
		else
			threshold_ratio=0.38
		fi

		phase_offset_threshold_ns=$(awk -v fd="$frame_duration_ns" -v tr="$threshold_ratio" 'BEGIN { printf "%.0f", fd * tr }')

		max_threshold=$(awk -v fd="$frame_duration_ns" 'BEGIN { printf "%.0f", fd * 0.45 }')
		min_threshold=$(awk -v fd="$frame_duration_ns" 'BEGIN { printf "%.0f", fd * 0.22 }')

		phase_offset_threshold_ns=$(awk -v val="$phase_offset_threshold_ns" -v max="$max_threshold" -v min="$min_threshold" '
		BEGIN {
			if (val > max) {
				print max
			} else if (val < min) {
				print min
			} else {
				print val
			}
		}')

		resetprop -n debug.sf.phase_offset_threshold_for_next_vsync_ns "$phase_offset_threshold_ns"

		resetprop -n debug.sf.enable_advanced_sf_phase_offset 1
		resetprop -n debug.sf.predict_hwc_composition_strategy 1
		resetprop -n debug.sf.use_phase_offsets_as_durations 1
		resetprop -n debug.sf.disable_hwc_vds 1
		resetprop -n debug.sf.show_refresh_rate_overlay_spinner 0
		resetprop -n debug.sf.show_refresh_rate_overlay_render_rate 0
		resetprop -n debug.sf.show_refresh_rate_overlay_in_middle 0
		resetprop -n debug.sf.kernel_idle_timer_update_overlay 0
		resetprop -n debug.sf.dump.enable 0
		resetprop -n debug.sf.dump.external 0
		resetprop -n debug.sf.dump.primary 0
		resetprop -n debug.sf.treat_170m_as_sRGB 0
		resetprop -n debug.sf.luma_sampling 0
		resetprop -n debug.sf.showupdates 0
		resetprop -n debug.sf.disable_client_composition_cache 0
		resetprop -n debug.sf.treble_testing_override false
		resetprop -n debug.sf.enable_layer_caching false
		resetprop -n debug.sf.enable_cached_set_render_scheduling true
		resetprop -n debug.sf.layer_history_trace false
		resetprop -n debug.sf.edge_extension_shader false
		resetprop -n debug.sf.enable_egl_image_tracker false
		resetprop -n debug.sf.use_phase_offsets_as_durations false
		resetprop -n debug.sf.layer_caching_highlight false
		resetprop -n debug.sf.enable_hwc_vds false
		resetprop -n debug.sf.vsp_trace false
		resetprop -n debug.sf.enable_transaction_tracing false
		resetprop -n debug.hwui.filter_test_overhead false
		resetprop -n debug.hwui.show_layers_updates false
		resetprop -n debug.hwui.capture_skp_enabled false
		resetprop -n debug.hwui.trace_gpu_resources false
		resetprop -n debug.hwui.skia_tracing_enabled false
		resetprop -n debug.hwui.nv_profiling false
		resetprop -n debug.hwui.skia_use_perfetto_track_events false
		resetprop -n debug.hwui.show_dirty_regions false
		resetprop -n debug.hwui.profile false
		resetprop -n debug.hwui.overdraw false
		resetprop -n debug.hwui.show_non_rect_clip hide
		resetprop -n debug.hwui.webview_overlays_enabled false
		resetprop -n debug.hwui.skip_empty_damage true
		resetprop -n debug.hwui.use_gpu_pixel_buffers true
		resetprop -n debug.hwui.use_buffer_age true
		resetprop -n debug.hwui.use_partial_updates true
		resetprop -n debug.hwui.skip_eglmanager_telemetry true
		resetprop -n debug.hwui.level 0
    fi
    
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    # DISABLE THERMAL
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    if [ "$DTHERMAL_STATE" -eq 1 ]; then
    
        list_thermal_services() {
            find /system/etc/init /vendor/etc/init /odm/etc/init -type f 2>/dev/null \
            | xargs grep -h "^service" \
            | awk '{print $2}' \
            | grep -i thermal
        }
        
        kill_thermald() {
            pkill -f thermald
        }
    
        stop_thermal_services() {
            for svc in $(list_thermal_services); do
                stop "$svc" 2>/dev/null
            done
        }
    
        reset_thermal_props() {
            getprop | grep -iE 'init.svc.thermal*|thermal-cutoff|ro.vendor.*thermal|debug.thermal.*|debug_pid.*thermal|boottime.*thermal|thermal.*running' \
            | awk -F'[][]' '{print $2}' | sed 's/:.*//' \
            | while read -r prop; do
                resetprop -n "$prop" suspended
            done
        }
    
        kill_thermal_processes() {
            ps -A | grep -iE 'thermal-engine|thermald|mtk_thermal' \
            | awk '{print $2}' \
            | while read -r pid; do
                kill -9 "$pid" 2>/dev/null
            done
        }                
    
        disable_thermal_zones() {
            for f in /sys/class/thermal/thermal_zone*/mode; do
                [ -f "$f" ] && zeshia "disabled" "$f"
            done
            for f in /sys/class/thermal/thermal_zone*/policy; do
                [ -f "$f" ] && zeshia "userspace" "$f"
            done
        }
    
        disable_gpu_thermal() {
            local gpu_limit="/proc/gpufreq/gpufreq_power_limited"
            [ -f "$gpu_limit" ] || return
            for k in ignore_batt_oc ignore_batt_percent ignore_low_batt ignore_thermal_protect ignore_pbm_limited; do
                zeshia "$k 1" "$gpu_limit"
            done
        }
    
        disable_ppm_limits() {
            local ppm="/proc/ppm/policy_status"
            [ -f "$ppm" ] || return
            grep -E 'FORCE_LIMIT|PWR_THRO|THERMAL' "$ppm" \
            | awk -F'[][]' '{print $2}' \
            | while read -r idx; do
                zeshia "$idx 0" "$ppm"
            done
        }
    
        restrict_thermal_monitoring() {
            chmod 000 /sys/devices/virtual/thermal/thermal_zone*/temp 2>/dev/null
            chmod 000 /sys/devices/virtual/thermal/thermal_zone*/trip_point_* 2>/dev/null
        }
    
        disable_battery_oc() {
            local batoc="/proc/mtk_batoc_throttling/battery_oc_protect_stop"
            [ -f "$batoc" ] && zeshia "stop 1" "$batoc"
        }
    
        kill_thermald
        stop_thermal_services
        reset_thermal_props
        kill_thermal_processes
        disable_thermal_zones
        disable_gpu_thermal
        disable_ppm_limits
        restrict_thermal_monitoring
        cmd thermalservice override-status 0
        disable_battery_oc
    
        AZLog "Thermal is disabled"
    fi
	
	# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    # DISABLE TRACE
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
	if [ "$DISTRACE_STATE" -eq 1 ]; then
    dlog "Applying disable trace"
		for trace_file in \
			/sys/kernel/tracing/instances/mmstat/trace \
			/sys/kernel/tracing/trace \
			$(find /sys/kernel/tracing/per_cpu/ -name trace 2>/dev/null); do
			zeshia "" "$trace_file"
		done
		zeshia "0" /sys/kernel/tracing/options/overwrite
		zeshia "0" /sys/kernel/tracing/options/record-tgids
		for f in /sys/kernel/tracing/*; do
			[ -w "$f" ] && echo "0" >"$f" 2>/dev/null
		done
		cmd accessibility stop-trace 2>/dev/null
		cmd input_method tracing stop 2>/dev/null
		cmd window tracing stop 2>/dev/null
		cmd window tracing size 0 2>/dev/null
		cmd migard dump-trace false 2>/dev/null
		cmd migard start-trace false 2>/dev/null
		cmd migard stop-trace true 2>/dev/null
		cmd migard trace-buffer-size 0 2>/dev/null
	fi
	
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    # KILL LOGD
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 		           
    if [ "$LOGD_STATE" = "1" ]; then
        for logger in $list_logger; do
            stop "$logger" 2>/dev/null
        done
        dlog "Applying Kill Logd"
    else
        for logger in $list_logger; do
            start "$logger" 2>/dev/null
        done
    fi
    
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    # INITIALIZE BYPASS CHARGING PATH 
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    if [ -z "$BYPASSPATH" ]; then    
    supported=0
        while IFS=":" read -r name path; do
            [ -z "$name" ] && continue
    
            name="${name//[[:space:]]/}"
            path="${path//[[:space:]]/}"
    
            if [ -e "$path" ]; then
                setprop "$BYPASSPROPS" "$name"
                dlog "Detected Bypass Charging path: $name"
                supported=1
                break
            fi
        done <<< "$BYPASSPATHLIST"
    
        if [ "$supported" -eq 0 ]; then
            dlog "Bypass Charging unsupported: no valid path found"
            setprop "$BYPASSPROPS" "UNSUPPORTED"
        fi
    else
        dlog "Bypass Charging path set: $BYPASSPATH"
    fi
       
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #  
    # APPLY DISABLE VSYNC IF AVAILABLE
	sys.azenith-utilityconf disablevsync $VSYNCVALUE
	# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
	
	# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
    # INITIALIZING COMPLETE
    # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
	sync
	AZLog "Initializing Complete"
	dlog "Initializing Complete"
}

###############################################
# # # # # # # MAIN FUNCTION! # # # # # # #
###############################################

case "$1" in
0) initialize ;;
1) performance_profile ;;
2) balanced_profile ;;
3) eco_mode ;;
esac
$@
wait
exit 0
