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
#include <sys/inotify.h>
#include <poll.h>
#include <string.h>

char cached_focused_app[128] = {0};
char cached_app_name[256] = "Unknown";
int cached_focused_pid = 0;
int cached_zen_mode = 0; 
int cached_screen_awake = 1;
int cached_battery_saver = 0;

void read_app_status() {
    FILE *fp = fopen("/data/adb/.config/AZenith/app_status", "r");
    if (!fp) return;
    
    char line[256];

    while (fgets(line, sizeof(line), fp)) {
        if (strncmp(line, "focused_app ", 12) == 0) {
            int uid;
            sscanf(line + 12, "%127s %d %d", cached_focused_app, &cached_focused_pid, &uid);
        } else if (strncmp(line, "screen_awake ", 13) == 0) {
            sscanf(line + 13, "%d", &cached_screen_awake);
        } else if (strncmp(line, "battery_saver ", 14) == 0) {
            sscanf(line + 14, "%d", &cached_battery_saver);
        } else if (strncmp(line, "zen_mode ", 9) == 0) {
            sscanf(line + 9, "%d", &cached_zen_mode);
        } else if (strncmp(line, "app_name ", 9) == 0) {
            strncpy(cached_app_name, line + 9, sizeof(cached_app_name) - 1);
            cached_app_name[strcspn(cached_app_name, "\n")] = 0;
        }
    }
    fclose(fp);
}
