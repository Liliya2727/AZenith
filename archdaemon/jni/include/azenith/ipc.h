#ifndef AZENITH_IPC_H
#define AZENITH_IPC_H

#include "core.h"

void handle_dynamic_bypass(DaemonContext* ctx);
int setup_inotify_watchers(void);
bool process_inotify_events(int inotify_fd, DaemonContext* ctx, int timeout_ms);

#endif
