/*
 * Copyright (C) 2024-2025 Rem01Gaming
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <AZenith.h>

bool (*get_screenstate)(void) = get_screenstate_normal;
bool (*get_low_power_state)(void) = get_low_power_state_normal;

/***********************************************************************************
 * Function Name      : run_profiler
 * Inputs             : int - 0 for perfcommon
 *                            1 for performance
 *                            2 for normal
 *                            3 for powersave
 * Returns            : None
 * Description        : Switch to specified performance profile.
 ***********************************************************************************/
void run_profiler(const int profile) {
    is_kanged();

    if (profile == 1) {
        write2file(GAME_INFO, false, false, "%s %d %d\n", gamestart, game_pid, uidof(game_pid));
    } else {
        write2file(GAME_INFO, false, false, "NULL 0 0\n");
    }

    write2file(PROFILE_MODE, false, false, "%d\n", profile);
    (void)systemv("sys.azenith-profilesettings %d", profile);
}

/***********************************************************************************
 * Function Name      : get_gamestart
 * Inputs             : None
 * Returns            : char* (dynamically allocated string with the game package name)
 * Description        : Searches for the currently visible application that matches
 *                      any package name listed in gamelist.
 *                      This helps identify if a specific game is running in the foreground.
 *                      Uses dumpsys to retrieve visible apps and filters by packages
 *                      listed in Gamelist.
 * Note               : Caller is responsible for freeing the returned string.
 ***********************************************************************************/
char* get_gamestart(GameOptions* options) {
    char* pkg = get_visible_package();
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

    // Find package key in JSON
    char key[256];
    snprintf(key, sizeof(key), "\"%s\"", pkg);
    char* entry = strstr(buf, key);
    if (!entry) {
        free(buf);
        free(pkg);
        return NULL;
    }

    // Fill options
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
    char* ret_pkg = strdup(pkg);
    free(pkg);
    return ret_pkg;
}

/***********************************************************************************
 * Function Name      : get_screenstate_normal
 * Inputs             : None
 * Returns            : bool - true if screen was awake
 *                             false if screen was asleep
 * Description        : Retrieves the current screen wakefulness state from dumpsys command.
 * Note               : In repeated failures up to 6, this function will skip fetch routine
 *                      and just return true all time using function pointer.
 *                      Never call this function, call get_screenstate() instead.
 ***********************************************************************************/
bool get_screenstate_normal(void) {
    static char fetch_failed = 0;

    FILE* fp = popen("dumpsys power", "r");
    if (!fp) {
        log_zenith(LOG_ERROR, "Failed to run dumpsys power");
        goto fetch_fail;
    }

    char line[512];
    bool found = false;
    bool is_awake = true;
    while (fgets(line, sizeof(line), fp)) {
        char* p = strstr(line, "mWakefulness=");
        if (p) {
            p += strlen("mWakefulness=");
            char* newline = strchr(p, '\n');
            if (newline)
                *newline = 0;

            is_awake = (strcmp(p, "Awake") == 0 || strcmp(p, "true") == 0);
            found = true;
            break;
        }
    }

    pclose(fp);

    if (found) {
        fetch_failed = 0;
        return is_awake;
    }
fetch_fail:
    fetch_failed++;
    log_zenith(LOG_ERROR, "Unable to fetch current screenstate");

    if (fetch_failed == 6) {
        log_zenith(LOG_FATAL, "get_screenstate is out of order!");
        get_screenstate = return_true;
    }

    return true;
}

/***********************************************************************************
 * Function Name      : get_low_power_state_normal
 * Inputs             : None
 * Returns            : bool - true if Battery Saver is enabled
 *                             false otherwise
 * Description        : Checks if the device's Battery Saver mode is enabled by using
 *                      global db or dumpsys power.
 * Note               : In repeated failures up to 6, this function will skip fetch routine
 *                      and just return false all time using function pointer.
 *                      Never call this function, call get_low_power_state() instead.
 ***********************************************************************************/
bool get_low_power_state_normal(void) {
    static char fetch_failed = 0;

    FILE* fp = popen("/system/bin/settings get global low_power", "r");
    if (fp) {
        char line[128];
        if (fgets(line, sizeof(line), fp)) {
            char* p = line;
            while (*p == ' ' || *p == '\t')
                p++;
            for (int i = strlen(p) - 1; i >= 0 && (p[i] == '\n' || p[i] == '\r'); i--)
                p[i] = 0;

            pclose(fp);
            fetch_failed = 0;
            return IS_LOW_POWER(p);
        }
        pclose(fp);
    }
    fp = popen("dumpsys power", "r");
    if (fp) {
        char line[512];
        while (fgets(line, sizeof(line), fp)) {
            char* p = strstr(line, "mSettingBatterySaverEnabled=");
            if (p) {
                p += strlen("mSettingBatterySaverEnabled=");

                char* newline = strchr(p, '\n');
                if (newline)
                    *newline = 0;

                pclose(fp);
                fetch_failed = 0;
                return IS_LOW_POWER(p);
            }
        }
        pclose(fp);
    }

    fetch_failed++;
    log_zenith(LOG_ERROR, "Unable to fetch battery saver status");

    if (fetch_failed == 6) {
        log_zenith(LOG_FATAL, "get_low_power_state is out of order!");
        get_low_power_state = return_false;
    }

    return false;
}
