#ifndef AZENITH_PROFILE_H
#define AZENITH_PROFILE_H

#include "core.h"

extern bool (*get_screenstate)(SystemStateCache*);
extern bool (*get_low_power_state)(SystemStateCache*);
const char* profile_mode_name(int mode);
bool get_screenstate_normal(SystemStateCache* cache);
bool get_low_power_state_normal(SystemStateCache* cache);
void run_profiler(const int profile);
void apply_performance_profile(DaemonContext* ctx);
void apply_eco_profile(DaemonContext* ctx);
void apply_balanced_profile(DaemonContext* ctx);

#endif
