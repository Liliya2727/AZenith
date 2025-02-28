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
resetprop -n debug.hwui.render_thread true
resetprop -n debug.hwui.renderer skiagl
resetprop -n -v debug.thermal.throttle.support no
resetprop -n vendor.debug.renderengine.backend skiaglthreaded
resetprop -n debug.renderengine.backend skiaglthreaded

# Params Executions 
resetprop -n ro.surface_flinger.max_frame_buffer_acquired_buffers 5
resetprop -n debug.sf.early_phase_offset_ns 500000
resetprop -n debug.sf.early_app_phase_offset_ns 500000
resetprop -n debug.sf.early_gl_phase_offset_ns 1000000
resetprop -n debug.sf.early_gl_app_phase_offset_ns 1000000
resetprop -n debug.sf.high_fps_early_phase_offset_ns 1000000
resetprop -n debug.sf.high_fps_early_gl_phase_offset_ns 1000000
resetprop -n debug.sf.high_fps_late_app_phase_offset_ns 1000000
resetprop -n debug.sf.phase_offset_threshold_for_next_vsync_ns 1000000
resetprop -n debug.sf.showupdates 0 
resetprop -n debug.sf.showcpu 0 
resetprop -n debug.sf.showbackground 0 
resetprop -n debug.sf.showfps 0

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

# Run Antares Service
Antares >/dev/null 2>&1
