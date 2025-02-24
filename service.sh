#!/system/bin/sh
while [ -z "$(getprop sys.boot_completed)" ]; do
    sleep 15
done

# apply
apply() {
    echo "$1" > "$2"
}

sleep 1
# Renderer Configurations
resetprop -n debug.hwui.renderer skiagl
resetprop -n vendor.debug.renderengine.backend skiaglthreaded
resetprop -n debug.renderengine.backend skiaglthreaded

# Ged Optimisation
echo "1" > /sys/module/ged/parameters/gx_game_mode
echo "1" > /sys/module/ged/parameters/ged_smart_boost
echo "1" > /sys/module/ged/parameters/cpu_boost_policy
echo "1" > /sys/module/ged/parameters/boost_extra
echo "886000" > /sys/module/ged/parameters/gpu_cust_upbound_freq
echo "1" > /sys/module/ged/parameters/enable_gpu_boost
echo "1" > /sys/module/ged/parameters/ged_boost_enable
echo "1" > /sys/module/ged/parameters/gx_boost_on
echo "1" > /sys/module/ged/parameters/boost_gpu_enable
echo "1" > /sys/module/ged/parameters/ged_monitor_3D_fence_systrace
echo "90" > /sys/module/ged/parameters/g_fb_dvfs_threshold
echo "1" > /sys/module/ged/parameters/g_gpu_timer_based_emu
echo "1" > /sys/module/ged/parameters/gpu_cust_boost_freq
echo "1" > /sys/kernel/ged/hal/gpu_boost_level

# Volt Optimizer
echo "-14" > /proc/eem/EEM_DET_B/eem_offset
echo "-14" > /proc/eem/EEM_DET_BL/eem_offset
echo "-14" > /proc/eem/EEM_DET_L/eem_offset
echo "-9" > /proc/eem/EEM_DET_CCI/eem_offset
echo "-2" > /proc/eemg/EEMG_DET_GPU/eemg_offset
echo "-2" > /proc/eemg/EEMG_DET_GPU_HI/eemg_offset

# Schedul Optimisation
echo "500000" > /proc/sys/kernel/sched_migration_cost_ns
echo "30" > /proc/sys/kernel/perf_cpu_time_max_percent
echo "3" > /proc/sys/vm/drop_caches
echo "256" > /proc/sys/kernel/sched_nr_migrate
echo "10000000" > /proc/sys/kernel/sched_latency_ns
echo "1024" > /proc/sys/kernel/sched_util_clamp_max
echo "1024" > /proc/sys/kernel/sched_util_clamp_min
echo "1" > /proc/sys/kernel/sched_child_runs_first
echo "1" > /proc/sys/kernel/sched_tunable_scaling
echo "0" > /proc/sys/kernel/sched_energy_aware

# mmcblk0
echo "0" > /sys/block/mmcblk0/queue/add_random
echo "0" > /sys/block/mmcblk0/queue/iostats
echo "0" > /sys/block/mmcblk0/queue/nomerges
echo "0" > /sys/block/mmcblk0/queue/rotational
echo "2" > /sys/block/mmcblk0/queue/rq_affinity
echo "256" > /sys/block/mmcblk0/queue/nr_requests
echo "2048" > /sys/block/mmcblk0/queue/read_ahead_kb

# Sda
echo "0" > /sys/block/sda/queue/add_random
echo "0" > /sys/block/sda/queue/iostats
echo "0" > /sys/block/sda/queue/nomerges
echo "0" > /sys/block/sda/queue/rotational
echo "2" > /sys/block/sda/queue/rq_affinity
echo "256" > /sys/block/sda/queue/nr_requests
echo "2048" > /sys/block/sda/queue/read_ahead_kb

# Sdb
echo "0" > /sys/block/sdb/queue/add_random
echo "0" > /sys/block/sdb/queue/iostats
echo "0" > /sys/block/sdb/queue/nomerges
echo "0" > /sys/block/sdb/queue/rotational
echo "2" > /sys/block/sdb/queue/rq_affinity
echo "256" > /sys/block/sdb/queue/nr_requests
echo "2048" > /sys/block/sdb/queue/read_ahead_kb

# Sdc
echo "0" > /sys/block/sdc/queue/add_random
echo "0" > /sys/block/sdc/queue/iostats
echo "0" > /sys/block/sdc/queue/nomerges
echo "0" > /sys/block/sdc/queue/rotational
echo "2" > /sys/block/sdc/queue/rq_affinity
echo "256" > /sys/block/sdc/queue/nr_requests
echo "2048" > /sys/block/sdc/queue/read_ahead_kb

# SET FREQ VOLTAGE
echo "6000000" > /sys/class/power_supply/battery/voltage_now
echo "1" > /sys/devices/system/cpu/perf/enable

# TURN OFF ALL POLICIES EXCEPT PPM_POLICY_HARD_USER_LIMIT
apply "0 0" /proc/ppm/policy_status  # PPM_POLICY_PTPOD
apply "1 0" /proc/ppm/policy_status  # PPM_POLICY_UT
apply "2 0" /proc/ppm/policy_status  # PPM_POLICY_FORCE_LIMIT
apply "3 0" /proc/ppm/policy_status  # PPM_POLICY_PWR_THRO
apply "4 0" /proc/ppm/policy_status  # PPM_POLICY_THERMAL
apply "5 0" /proc/ppm/policy_status  # PPM_POLICY_DLPT
apply "6 1" /proc/ppm/policy_status  # PPM_POLICY_HARD_USER_LIMIT
apply "7 0" /proc/ppm/policy_status  # PPM_POLICY_USER_LIMIT
apply "8 0" /proc/ppm/policy_status  # PPM_POLICY_LCM_OFF
apply "9 0" /proc/ppm/policy_status  # PPM_POLICY_SYS_BOOST

# Scheduler IO
echo "mq-deadline" > /sys/block/mmcblk0/queue/scheduler
echo "deadline" > /sys/block/sda/queue/scheduler
echo "deadline" > /sys/block/sdb/queue/scheduler
echo "deadline" > /sys/block/sdc/queue/scheduler
echo "deadline" > /sys/block/dm-0/queue/scheduler

# Schedhorizon Tunes
echo "50 100 200 400 600" > /sys/devices/system/cpu/cpufreq/policy0/schedhorizon/up_delay
echo "500000 850000 1250000 1450000 1800000" > /sys/devices/system/cpu/cpufreq/policy0/schedhorizon/efficient_freq
echo "50 100 200 400 600 800" > /sys/devices/system/cpu/cpufreq/policy4/schedhorizon/up_delay
echo "437000 902000 1162000 1451000 1740000 1985000" > /sys/devices/system/cpu/cpufreq/policy4/schedhorizon/efficient_freq
echo "30 50 150 300 500 700" > /sys/devices/system/cpu/cpufreq/policy7/schedhorizon/up_delay
echo "659000 1108000 1370000 1632000 1820000 2284000" > /sys/devices/system/cpu/cpufreq/policy7/schedhorizon/efficient_freq

# Run Antares Service
Antares >/dev/null 2>&1
