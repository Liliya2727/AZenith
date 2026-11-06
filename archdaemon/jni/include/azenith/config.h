#ifndef AZENITH_CONFIG_H
#define AZENITH_CONFIG_H

#include "core.h"

int get_current_refresh_rate(void);
void apply_dynamic_refresh_rate(int target_rr);
int is_manual_mode(void);
void show_sidepanel(const char* app_label);
bool apply_smart_renderer(const char* target_type, char* saved_ref, char* saved_sys);
bool apply_resolution_target(DaemonContext* ctx, const char* pkg, const char* downscale, const char* fps);
void restore_resolution_target(DaemonContext* ctx, const char* pkg);
void save_daemon_state(DaemonContext* ctx);
void restore_daemon_state(DaemonContext* ctx);

#endif
