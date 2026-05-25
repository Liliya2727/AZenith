/*
 * Copyright (C) 2026-2027 Zexshia
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
#include <dirent.h>
#include <sys/system_properties.h>

/***********************************************************************************
 * Function Name      : GamePreload
 * Inputs             : const char* package - target application package name
 * Returns            : void
 * Description        : Preloads all native libraries (.so) inside lib/arm64
 ***********************************************************************************/
void GamePreload(const char* package) {
    sleep(5);
    if (!package || strlen(package) == 0) {
        log_zenith(LOG_WARN, "Package is null or empty");
        return;
    }

    char apk_path[256] = {0};
    char cmd_apk[512];
    snprintf(cmd_apk, sizeof(cmd_apk), "cmd package path %s | head -n1 | cut -d: -f2", package);

    FILE* apk = popen(cmd_apk, "r");
    if (!apk || !fgets(apk_path, sizeof(apk_path), apk)) {
        log_zenith(LOG_WARN, "Failed to get APK path for %s", package);
        if (apk)
            pclose(apk);
        return;
    }
    pclose(apk);
    apk_path[strcspn(apk_path, "\n")] = 0;

    char* last_slash = strrchr(apk_path, '/');
    if (!last_slash) {
        log_zenith(LOG_WARN, "Failed to determine APK folder from path: %s", apk_path);
        return;
    }
    *last_slash = '\0';

    char lib_path[300];
    snprintf(lib_path, sizeof(lib_path), "%s/lib/arm64", apk_path);

    // check if .so exists
    bool lib_exists = false;
    DIR* dir = opendir(lib_path);
    if (dir) {
        struct dirent* entry;
        while ((entry = readdir(dir)) != NULL) {
            if (strstr(entry->d_name, ".so")) {
                lib_exists = true;
                break;
            }
        }
        closedir(dir);
    }

    char budget[32] = {0};
    __system_property_get("persist.sys.azenithconf.preloadbudget", budget);
    if (strlen(budget) == 0)
        strcpy(budget, "500M");

    // Common variables
    FILE* fp = NULL;
    char line[1024];
    int total_pages = 0;
    char total_size[32] = {0};

    if (lib_exists) {
        char preload_cmd[512];
        snprintf(preload_cmd, sizeof(preload_cmd), "sys.azenith-preloadbin -v -t -m %s \"%s\"", budget, lib_path);

        fp = popen(preload_cmd, "r");
        if (!fp) {
            log_zenith(LOG_WARN, "Failed to run preloadbin for %s", package);
            return;
        }

        log_zenith(LOG_INFO, "Preloading game libs %s", package);
        log_preload(LOG_INFO, "Preloading libs %s with budget %s", lib_path, budget);

    } else {
        // Fallback to split APKs
        char preload_cmd[512];
        snprintf(preload_cmd, sizeof(preload_cmd), "sys.azenith-preloadbin -v -t -m %s \"%s\"", budget, apk_path);

        fp = popen(preload_cmd, "r");
        if (!fp) {
            log_zenith(LOG_WARN, "Failed to run preloadbin for %s", package);
            return;
        }

        log_zenith(LOG_INFO, "Preloading game split apks %s", package);
        log_preload(LOG_INFO, "Preloading split apks %s with budget %s", apk_path, budget);
    }

    while (fgets(line, sizeof(line), fp)) {
        line[strcspn(line, "\n")] = 0;

        char* p_pages = strstr(line, "Touched Pages:");
        if (p_pages) {
            int pages = 0;
            char size[32] = {0};

            if (sscanf(p_pages, "Touched Pages: %d (%31[^)])", &pages, size) == 2) {
                total_pages += pages;
                strncpy(total_size, size, sizeof(total_size) - 1);
                log_zenith(LOG_DEBUG, "Preloading complete: %d memory pages touched", pages);
                log_zenith(LOG_DEBUG, "Total memory used for preloaded libraries: %s", size);
            } else {
                log_zenith(LOG_WARN, "Failed to parse Touched Pages");
            }
        }

        if (strstr(line, ".so") || strstr(line, ".apk") || strstr(line, ".dm") || strstr(line, ".odex") || strstr(line, ".vdex") ||
            strstr(line, ".art")) {
            log_preload(LOG_DEBUG, "Touched: %s", line);
        }
    }

    log_preload(LOG_INFO, "Game %s preloaded success: total %d pages touched (~%s)", package, total_pages, total_size);

    pclose(fp);
}
