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

WALT_STATE="$(getprop persist.sys.azenithconf.walttunes)"
LOGD_STATE="$(getprop persist.sys.azenithconf.logd)"
DTHERMAL_STATE="$(getprop persist.sys.azenithconf.DThermal)"
SFL_STATE="$(getprop persist.sys.azenithconf.SFL)"
MALISCHED_STATE="$(getprop persist.sys.azenithconf.malisched)"
FPSGED_STATE="$(getprop persist.sys.azenithconf.fpsged)"
SCHEDTUNES_STATE="$(getprop persist.sys.azenithconf.schedtunes)"
JUSTINTIME_STATE="$(getprop persist.sys.azenithconf.justintime)"
DISTRACE_STATE="$(getprop persist.sys.azenithconf.disabletrace)"
list_logger="logd traced statsd tcpdump cnss_diag subsystem_ramdump charge_logger wlan_logging"

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
    local pathname parent_dir

    parent_dir="${path%/*}"
    pathname="${parent_dir##*/}/${path##*/}"

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

applyprefencedsettings() {
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
}

applyprefencedsettings
wait
sync

exit 0