/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <AZenith.h>
#include <string.h>

/**
 * @brief Reads the app status file and populates the SystemStateCache.
 * @param cache Pointer to the SystemStateCache structure.
 */
void read_app_status(SystemStateCache* cache) {
    if (!cache) return;

    FILE *fp = fopen("/data/adb/.config/AZenith/app_status", "r");
    if (!fp) return;
    
    char line[256];

    // Zero-out or set default values before reading new state
    memset(cache->focused_app, 0, sizeof(cache->focused_app));
    strncpy(cache->app_name, "Unknown", sizeof(cache->app_name) - 1);
    
    while (fgets(line, sizeof(line), fp)) {
        if (strncmp(line, "focused_app ", 12) == 0) {
            int uid;
            sscanf(line + 12, "%127s %d %d", cache->focused_app, &cache->focused_pid, &uid);
        } else if (strncmp(line, "screen_awake ", 13) == 0) {
            sscanf(line + 13, "%d", &cache->screen_awake);
        } else if (strncmp(line, "battery_saver ", 14) == 0) {
            sscanf(line + 14, "%d", &cache->battery_saver);
        } else if (strncmp(line, "zen_mode ", 9) == 0) {
            sscanf(line + 9, "%d", &cache->zen_mode);
        } else if (strncmp(line, "app_name ", 9) == 0) {
            strncpy(cache->app_name, line + 9, sizeof(cache->app_name) - 1);
            cache->app_name[strcspn(cache->app_name, "\n")] = 0; // Strip newline
        }
    }
    
    fclose(fp);
}
