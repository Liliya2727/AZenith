#ifndef AZENITH_MONITOR_H
#define AZENITH_MONITOR_H

#include "core.h"

char* get_visible_package(SystemStateCache* cache);
void read_app_status(SystemStateCache* cache);
void free_gamelist_cache(void);
void reload_gamelist_cache(DaemonContext* ctx);
char* get_gamestart(GameConfig* options, SystemStateCache* cache);

#endif
