#!/system/bin/sh

# Wait until the system has fully booted
while [ -z "$(getprop sys.boot_completed)" ]; do
    sleep 15
done

# Function to apply system properties
apply_prop() {
    resetprop -n "$1" "$2"
}

sleep 1

# Renderer Configurations
apply_prop debug.hwui.render_thread true
apply_prop debug.hwui.renderer skiagl
apply_prop debug.thermal.throttle.support no
apply_prop vendor.debug.renderengine.backend skiaglthreaded
apply_prop debug.renderengine.backend skiaglthreaded
apply_prop debug.sf.use_phase_offsets_as_durations 1
apply_prop debug.sf.late.sf.duration 2000000
apply_prop debug.sf.late.app.duration 27000000
apply_prop debug.sf.frame_rate_multiple_threshold 120
apply_prop ro.surface_flinger.max_frame_buffer_acquired_buffers 3

# SurfaceFlinger Optimizations
apply_prop ro.surface_flinger.max_frame_buffer_acquired_buffers 5
apply_prop debug.sf.early_phase_offset_ns 500000
apply_prop debug.sf.early_app_phase_offset_ns 500000
apply_prop debug.sf.early_gl_phase_offset_ns 1000000
apply_prop debug.sf.early_gl_app_phase_offset_ns 1000000
apply_prop debug.sf.high_fps_early_phase_offset_ns 1000000
apply_prop debug.sf.high_fps_early_gl_phase_offset_ns 1000000
apply_prop debug.sf.high_fps_late_app_phase_offset_ns 1000000
apply_prop debug.sf.phase_offset_threshold_for_next_vsync_ns 1000000

# Blur Effects & Graphics Optimizations
for prop in debug.egl.disable_blur debug.hwui.disable_blur debug.renderengine.no_blur \
            debug.sf.disable_blur_effects persist.sys.graphics.disable_blur \
            persist.vendor.game.optimization.disable_blur ro.graphics.disable_game_blur \
            persist.game.mode.blur_off persist.vendor.graphics.disable_blur; do
    apply_prop "$prop" 1
done

# Sharpness & High-Quality Rendering
apply_prop persist.sys.graphics.sharpness_boost 1
apply_prop debug.renderengine.force_high_quality 1
apply_prop ro.graphics.force_high_sharpness 1
apply_prop persist.vendor.graphics.sharpness 1
apply_prop ro.sf.force_rasterization 1
apply_prop persist.sys.sf.high_quality 1

# Run Antares Service
Antares >/dev/null 2>&1 &