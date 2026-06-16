/*
 * Copyright (C) 2024-2025 Rem01Gaming
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
#include <time.h>

// Function pointers initialized to default handlers
bool (*get_screenstate)(SystemStateCache*) = get_screenstate_normal;
bool (*get_low_power_state)(SystemStateCache*) = get_low_power_state_normal;

/**
 * @brief Retrieves the current screen wakefulness state from cache.
 * @param cache Pointer to the system state cache.
 * @return true if screen was awake, false otherwise.
 */
bool get_screenstate_normal(SystemStateCache* cache) {
    if (!cache) return true; // Default to awake if cache is missing
    return cache->screen_awake != 0;
}

/**
 * @brief Checks if the device's Battery Saver mode is enabled from cache.
 * @param cache Pointer to the system state cache.
 * @return true if Battery Saver is enabled, false otherwise.
 */
bool get_low_power_state_normal(SystemStateCache* cache) {
    if (!cache) return false;
    return cache->battery_saver != 0;
}

/**
 * @brief Switch to specified performance profile.
 * @param profile 0 for perfcommon, 1 for performance, 2 for balanced, 3 for powersave.
 */
void run_profiler(const int profile) {
    is_kanged();

    time_t t = time(NULL);
    struct tm tm = *localtime(&t);
    char time_str[64];

    snprintf(time_str, sizeof(time_str), "%02d:%02d:%02d", tm.tm_hour, tm.tm_min, tm.tm_sec);

    if (profile == 1) {
        // Assuming game_pids and gamestart are still managed globally or passed correctly
        pid_t main_pid = (game_pid_count > 0) ? game_pids[0] : 0;
        write2file(GAME_INFO, false, false, "%s %d %d\nTime: %s\n", gamestart, main_pid, uidof(main_pid), time_str);
        write2file(GAME_INFO_APP, false, false, "%s %d %d\nTime: %s\n", gamestart, main_pid, uidof(main_pid), time_str);
    } else {
        write2file(GAME_INFO, false, false, "NULL 0 0\nTime: %s\n", time_str);
        write2file(GAME_INFO_APP, false, false, "NULL 0 0\nTime: %s\n", time_str);
    }

    write2file(PROFILE_MODE, false, false, "%d\n", profile);
    write2file(PROFILE_MODE_APP, false, false, "%d\n", profile);
    
    // Suggestion for future: Replace systemv with native property setting for performance
    (void)systemv("sys.azenith-profilesettings %d", profile);
}

/**
 * @brief Searches for the currently visible app that matches a game in the gamelist.
 * @param options Pointer to extract game specific options.
 * @param cache Pointer to system state cache to determine visibility.
 * @return Malloc-ed string containing package name, or NULL. Caller must free.
 */
char* get_gamestart(GameOptions* options, SystemStateCache* cache) {
    char* pkg = get_visible_package(cache);
    if (!pkg) return NULL;

    FILE* fp = fopen(GAMELIST, "r");
    if (!fp) {
        free(pkg);
        return NULL;
    }

    fseek(fp, 0, SEEK_END);
    long size = ftell(fp);
    fseek(fp, 0, SEEK_SET);

    if (size <= 0) {
        fclose(fp);
        free(pkg);
        return NULL;
    }

    char* buf = malloc(size + 1);
    if (!buf) {
        fclose(fp);
        free(pkg);
        return NULL;
    }

    if (fread(buf, 1, size, fp) != (size_t)size) {
        fclose(fp);
        free(buf);
        free(pkg);
        return NULL;
    }
    fclose(fp);
    buf[size] = '\0';

    char key[256];
    snprintf(key, sizeof(key), "\"%s\"", pkg);
    char* entry = strstr(buf, key);
    
    if (!entry) {
        free(buf);
        free(pkg);
        return NULL;
    }

    // Populate GameOptions if matched
    if (options) {
        char* p;

        p = strstr(entry, "\"perf_lite_mode\":");
        extract_string_value(options->perf_lite_mode, p, sizeof(options->perf_lite_mode));

        p = strstr(entry, "\"dnd_on_gaming\":");
        extract_string_value(options->dnd_on_gaming, p, sizeof(options->dnd_on_gaming));

        p = strstr(entry, "\"app_priority\":");
        extract_string_value(options->app_priority, p, sizeof(options->app_priority));

        p = strstr(entry, "\"game_preload\":");
        extract_string_value(options->game_preload, p, sizeof(options->game_preload));

        p = strstr(entry, "\"refresh_rate\":");
        extract_string_value(options->refresh_rate, p, sizeof(options->refresh_rate));

        p = strstr(entry, "\"renderer\":");
        extract_string_value(options->renderer, p, sizeof(options->renderer));
    }

    free(buf);
    
    // Since 'pkg' is already a dynamically allocated string from get_visible_package,
    // we can return it directly instead of duplicating it again.
    return pkg; 
}
