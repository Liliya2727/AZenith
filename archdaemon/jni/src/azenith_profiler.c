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
    if (!cache)
        return true; // Default to awake if cache is missing
    return cache->screen_awake != 0;
}

/**
 * @brief Checks if the device's Battery Saver mode is enabled from cache.
 * @param cache Pointer to the system state cache.
 * @return true if Battery Saver is enabled, false otherwise.
 */
bool get_low_power_state_normal(SystemStateCache* cache) {
    if (!cache)
        return false;
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
        write2file(GAME_INFO, false, false, "%s %d %d\nTime: %s\n", gamestart, main_pid,
                   uidof(main_pid), time_str);
        write2file(GAME_INFO_APP, false, false, "%s %d %d\nTime: %s\n", gamestart, main_pid,
                   uidof(main_pid), time_str);
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
char* get_gamestart(GameConfig* options, SystemStateCache* cache) {
    char* pkg = get_visible_package(cache);
    if (!pkg)
        return NULL;

    pthread_mutex_lock(&cache_mutex);

    if (g_game_cache == NULL || g_game_cache_count == 0) {
        pthread_mutex_unlock(&cache_mutex);
        free(pkg);
        return NULL;
    }

    bool match_found = false;

    for (int i = 0; i < g_game_cache_count; i++) {
        if (strcmp(g_game_cache[i].package, pkg) == 0) {
            match_found = true;

            if (options) {
                strcpy(options->perf_lite_mode, g_game_cache[i].perf_lite_mode);
                strcpy(options->dnd_on_gaming, g_game_cache[i].dnd_on_gaming);
                strcpy(options->app_priority, g_game_cache[i].app_priority);
                strcpy(options->game_preload, g_game_cache[i].game_preload);
                strcpy(options->refresh_rate, g_game_cache[i].refresh_rate);
                strcpy(options->renderer, g_game_cache[i].renderer);
            }
            break;
        }
    }

    pthread_mutex_unlock(&cache_mutex);

    if (!match_found) {
        free(pkg);
        return NULL;
    }

    return pkg;
}
