#ifndef AZENITH_STARTUP_H
#define AZENITH_STARTUP_H

#include "core.h"

void load_initial_config_files(DaemonContext* ctx);
void init_daemon_context(DaemonContext* ctx);
void verify_system_integrity(void);
void wait_for_java_companion(DaemonContext* ctx);
void* java_lock_watcher_thread(void* arg);

#endif
