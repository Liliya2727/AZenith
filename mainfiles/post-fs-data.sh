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

get_state() {
    local val
    val=$(getprop "$1")
    echo "${val:-0}"
}

WALT_STATE=$(get_state persist.sys.azenithconf.walttunes)
LOGD_STATE=$(get_state persist.sys.azenithconf.logd)
DTHERMAL_STATE=$(get_state persist.sys.azenithconf.DThermal)
SFL_STATE=$(get_state persist.sys.azenithconf.SFL)
MALISCHED_STATE=$(get_state persist.sys.azenithconf.malisched)
FPSGED_STATE=$(get_state persist.sys.azenithconf.fpsged)
SCHEDTUNES_STATE=$(get_state persist.sys.azenithconf.schedtunes)
JUSTINTIME_STATE=$(get_state persist.sys.azenithconf.justintime)
DISTRACE_STATE=$(get_state persist.sys.azenithconf.disabletrace)

readonly LIST_LOGGER="logd traced statsd tcpdump cnss_diag subsystem_ramdump charge_logger wlan_logging"

verbose_log() {
    [ "$DEBUGMODE" = "true" ] && sys.azenith-service --verboselog "AZenith" "0" "$1"
}

write_log() {
    sys.azenith-service --log "AZenith" "1" "$1"
}

write_val() {
    local value="$1" path="$2" lock="${3:-true}"
    
    [ -e "$path" ] || { verbose_log "File /${path#/} not found, skipping..."; return 1; }

    chmod 644 "$path" 2>/dev/null
    if echo "$value" >"$path" 2>/dev/null; then
        verbose_log "Set /${path#/} to $value"
        [ "$lock" = "true" ] && chmod 444 "$path" 2>/dev/null
    else
        verbose_log "Cannot write to /${path#/} (permission denied)"
        [ "$lock" = "true" ] && chmod 444 "$path" 2>/dev/null
        return 1
    fi
}

prefsettings() {

    if [ "$JUSTINTIME_STATE" -eq 1 ]; then
        write_log "Applying JIT Compiler"
        cmd package list packages -3 2>/dev/null | awk -F':' '{print $2}' | while read -r pkg; do
            ( cmd package compile -m speed-profile "$pkg" >/dev/null 2>&1 && verbose_log "$pkg | Success" ) &
        done
        wait
    fi

    
    # SCHED TUNES
    if [ "$SCHEDTUNES_STATE" -eq 1 ]; then
        write_log "Applying Schedtunes for Schedutil and Schedhorizon"
        
        settunes() {
            local p="$1" schedhorizon="$1/schedhorizon" schedutil="$1/schedutil"
            [ -d "$p" ] || return

            local freqs selected_freqs num up_delay="" up_rate=6500 down_rate=12000 rate_limit=7000
            freqs=$(cat "$p/scaling_available_frequencies" 2>/dev/null)
            [ -z "$freqs" ] && return

            selected_freqs=$(echo "$freqs" | awk '{for(i=1;i<=NF;i++) a[i]=$i} END {asort(a); for(i=NF;i>NF-6 && i>0;i--) printf "%s ", a[i]}')
            num=$(echo "$selected_freqs" | wc -w)

            for i in $(seq 1 "$num"); do up_delay="$up_delay $((50 * i))"; done
            up_delay="${up_delay# }"

            # Setup Horizon
            if [ -d "$schedhorizon" ]; then
                write_val "$up_delay" "$schedhorizon/up_delay"
                write_val "$selected_freqs" "$schedhorizon/efficient_freq"
                [ -f "$schedhorizon/up_rate_limit_us" ] && write_val "$up_rate" "$schedhorizon/up_rate_limit_us" || write_val "$rate_limit" "$schedhorizon/rate_limit_us"
                write_val "$down_rate" "$schedhorizon/down_rate_limit_us"
            fi

            # Setup Schedutil
            if [ -d "$schedutil" ]; then
                [ -f "$schedutil/up_rate_limit_us" ] && write_val "$up_rate" "$schedutil/up_rate_limit_us" || write_val "$rate_limit" "$schedutil/rate_limit_us"
                write_val "$down_rate" "$schedutil/down_rate_limit_us"
            fi
        }

        for policy in /sys/devices/system/cpu/cpufreq/policy*; do settunes "$policy"; done
    fi

    
    # WALT TUNING
    if [ "$WALT_STATE" -eq 1 ]; then
        write_log "Applying WALT governor tuning"
        # Parameter const (Inline untuk kerapihan)
        local WR_UP=8000 WR_DN=12000 WL_HI=92 WT_CNT=6 WT_ST=95 WT_SP=8 WF_HI=0 WF_RTG=0

        setwalt() {
            local walt="$1/walt" available_freqs selected_freqs highest second num_freqs tloads="" cur="$WT_ST" i=0
            [ -d "$walt" ] || { verbose_log "Skipped: $1 (WALT NA)"; return; }

            available_freqs=$(cat "$1/scaling_available_frequencies" 2>/dev/null)
            [ -z "$available_freqs" ] && return

            # Awk native sort (Jauh lebih cepat dari pipe tr | sort | head | tr)
            selected_freqs=$(echo "$available_freqs" | awk -v c="$WT_CNT" '{for(i=1;i<=NF;i++) a[i]=$i} END {asort(a); for(i=NF;i>NF-c && i>0;i--) printf "%s ", a[i]}')
            [ -z "$selected_freqs" ] && return

            highest=$(echo "$selected_freqs" | awk '{print $1}')
            second=$(echo "$selected_freqs" | awk '{print $2}')
            [ -z "$second" ] && second="$highest"

            num_freqs=$(echo "$selected_freqs" | wc -w)
            while [ $i -lt "$num_freqs" ]; do
                tloads="$tloads $cur"; cur=$((cur - WT_SP)); [ "$cur" -lt 10 ] && cur=10; i=$((i + 1))
            done

            write_val "$WL_HI" "$walt/hispeed_load"
            write_val "${second:-$WF_HI}" "$walt/hispeed_freq"
            write_val "${highest:-$WF_RTG}" "$walt/rtg_boost_freq"
            write_val "${tloads# }" "$walt/target_loads"
            write_val "$selected_freqs" "$walt/efficient_freq"
            write_val "$WR_UP" "$walt/up_rate_limit_us"
            write_val "$WR_DN" "$walt/down_rate_limit_us"
            write_log "WALT Tuning Applied on ${1##*/}"
        }

        for policy in /sys/devices/system/cpu/cpufreq/policy*; do setwalt "$policy"; done
    fi

    
    # FPS GO AND GED PARAMETER
    if [ "$FPSGED_STATE" -eq 1 ]; then
        write_log "Applying FPSGO Parameters"
        
        set -- \
            ged_smart_boost 1 boost_upper_bound 100 enable_gpu_boost 1 enable_cpu_boost 1 \
            ged_boost_enable 1 boost_gpu_enable 1 gpu_dvfs_enable 1 gx_frc_mode 1 gx_dfps 1 \
            gx_force_cpu_boost 1 gx_boost_on 1 gx_game_mode 1 gx_3D_benchmark_on 1 \
            gpu_loading 0 cpu_boost_policy 1 boost_extra 1 is_GED_KPI_enabled 0

        while [ $# -gt 0 ]; do
            write_val "$2" "/sys/module/ged/parameters/$1"
            shift 2 
        done

        local fpsgo_dir="/sys/kernel/fpsgo"
        write_val "0" "$fpsgo_dir/fbt/boost_ta"
        write_val "1" "$fpsgo_dir/fbt/enable_switch_down_throttle"
        write_val "1" "$fpsgo_dir/fstb/adopt_low_fps"
        write_val "1" "$fpsgo_dir/fstb/fstb_self_ctrl_fps_enable"
        write_val "0" "$fpsgo_dir/fstb/boost_ta"
        write_val "1" "$fpsgo_dir/fstb/enable_switch_sync_flag"
        write_val "0" "$fpsgo_dir/fbt/boost_VIP"
        write_val "1" "$fpsgo_dir/fstb/gpu_slowdown_check"
        write_val "1" "$fpsgo_dir/fbt/thrm_limit_cpu"
        write_val "0" "$fpsgo_dir/fbt/thrm_temp_th"
        write_val "0" "$fpsgo_dir/fbt/llf_task_policy"
        
        write_val "100" /sys/module/mtk_fpsgo/parameters/uboost_enhance_f
        write_val "0" /sys/module/mtk_fpsgo/parameters/isolation_limit_cap
        write_val "1" /sys/pnpmgr/fpsgo_boost/boost_enable
        write_val "1" /sys/pnpmgr/fpsgo_boost/boost_mode
        write_val "1" /sys/pnpmgr/install
        write_val "100" /sys/kernel/ged/hal/gpu_boost_level
    fi

    
    # GPU MALI SCHEDULING
    
    if [ "$MALISCHED_STATE" -eq 1 ]; then
        write_log "Applying GPU Mali Sched"
        local m_sched m_base
        m_sched=$(find /sys/devices/platform/soc -maxdepth 2 -type d -name "*mali*" -exec find {} -maxdepth 1 -type d -name "scheduling" \; -print -quit 2>/dev/null)
        m_base=$(find /sys/devices/platform/soc -maxdepth 1 -type d -name "*mali*" -print -quit 2>/dev/null)

        [ -n "$m_sched" ] && write_val "full" "$m_sched/serialize_jobs"
        [ -n "$m_base" ] && write_val "1" "$m_base/js_ctx_scheduling_mode"
    fi

    
    # SURFACEFLINGER LATENCY
    if [ "$SFL_STATE" -eq 1 ]; then
        write_log "Applying SurfaceFlinger Latency"

        local refresh_rate=120 app_duration=12000 sf_duration=10000 app_phase_offset_ns=-12000 sf_phase_offset_ns=-8000 phase_offset_threshold_ns=3000

        cat <<EOF | while read -r prop value; do resetprop -n "$prop" "$value"; done
debug.sf.early.app.duration $app_duration
debug.sf.earlyGl.app.duration $app_duration
debug.sf.late.app.duration $app_duration
debug.sf.early.sf.duration $sf_duration
debug.sf.earlyGl.sf.duration $sf_duration
debug.sf.late.sf.duration $sf_duration
debug.sf.early_app_phase_offset_ns $app_phase_offset_ns
debug.sf.high_fps_early_app_phase_offset_ns $app_phase_offset_ns
debug.sf.high_fps_late_app_phase_offset_ns $app_phase_offset_ns
debug.sf.early_phase_offset_ns $sf_phase_offset_ns
debug.sf.high_fps_early_phase_offset_ns $sf_phase_offset_ns
debug.sf.high_fps_late_sf_phase_offset_ns $sf_phase_offset_ns
debug.sf.phase_offset_threshold_for_next_vsync_ns $phase_offset_threshold_ns
debug.sf.enable_advanced_sf_phase_offset 1
debug.sf.predict_hwc_composition_strategy 1
debug.sf.use_phase_offsets_as_durations 1
debug.sf.disable_hwc_vds 1
debug.sf.show_refresh_rate_overlay_spinner 0
debug.sf.enable_layer_caching false
debug.sf.enable_cached_set_render_scheduling true
debug.hwui.skip_empty_damage true
debug.hwui.use_gpu_pixel_buffers true
debug.hwui.use_buffer_age true
debug.hwui.use_partial_updates true
EOF
    fi
    
    
    # DISABLE THERMAL
    if [ "$DTHERMAL_STATE" -eq 1 ]; then
        write_log "Disabling Thermal Engine"

        pkill -9 -f 'thermald|thermal-engine|mtk_thermal' 2>/dev/null

        find /system/etc/init /vendor/etc/init /odm/etc/init -type f 2>/dev/null -exec awk '/^service.*thermal/ {print $2}' {} + | while read -r svc; do
            stop "$svc" 2>/dev/null
        done
        getprop | awk -F'[][]' '/init\.svc\.thermal.*|thermal-cutoff|ro\.vendor\..*thermal|debug\.thermal.*|debug_pid.*thermal|boottime.*thermal|thermal.*running/ {print $2}' | while read -r prop; do
            resetprop -n "$prop" suspended
        done

        # 4. Modifikasi Zone (Wildcard Expansion)
        for f in /sys/class/thermal/thermal_zone*/mode; do write_val "disabled" "$f" false; done
        for f in /sys/class/thermal/thermal_zone*/policy; do write_val "userspace" "$f" false; done
        chmod 000 /sys/devices/virtual/thermal/thermal_zone*/temp 2>/dev/null
        chmod 000 /sys/devices/virtual/thermal/thermal_zone*/trip_point_* 2>/dev/null

        awk -F'[][]' '/FORCE_LIMIT|PWR_THRO|THERMAL/ {print $2}' /proc/ppm/policy_status 2>/dev/null | while read -r idx; do
            write_val "$idx 0" "/proc/ppm/policy_status"
        done
        
        local gpu_limit="/proc/gpufreq/gpufreq_power_limited"
        [ -f "$gpu_limit" ] && for k in ignore_batt_oc ignore_batt_percent ignore_low_batt ignore_thermal_protect ignore_pbm_limited; do write_val "$k 1" "$gpu_limit"; done

        cmd thermalservice override-status 0 2>/dev/null
        write_val "stop 1" "/proc/mtk_batoc_throttling/battery_oc_protect_stop"

        verbose_log "Thermal is disabled"
    fi

    
    # DISABLE TRACE
    if [ "$DISTRACE_STATE" -eq 1 ]; then
        write_log "Applying disable trace"
        
        find /sys/kernel/tracing -type f -name "trace" -exec sh -c '> "{}"' \; 2>/dev/null
        
        write_val "0" /sys/kernel/tracing/options/overwrite
        write_val "0" /sys/kernel/tracing/options/record-tgids

        for c in "accessibility stop-trace" "input_method tracing stop" "window tracing stop" "window tracing size 0" "migard dump-trace false" "migard start-trace false" "migard stop-trace true" "migard trace-buffer-size 0"; do
            cmd $c 2>/dev/null
        done
    fi

    
    # KILL LOGD
    if [ "$LOGD_STATE" = "1" ]; then
        write_log "Applying Kill Logd"
        for logger in $LIST_LOGGER; do stop "$logger" 2>/dev/null; done
    else
        for logger in $LIST_LOGGER; do start "$logger" 2>/dev/null; done
    fi
}

prefsettings
sync

exit 0
