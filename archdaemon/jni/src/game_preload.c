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
#include <dirent.h>
#include <string.h>
#include <sys/system_properties.h>

/**
 * @brief Preloads all native libraries (.so) or split APKs inside the target application into
 * memory.
 * @param package Target application package name.
 */
void GamePreload(const char* package) {
    sleep(5);

    if (!package || package[0] == '\0') {
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

    bool lib_exists = false;
    DIR* dir = opendir(lib_path);
    if (dir) {
        struct dirent* entry;
        while ((entry = readdir(dir)) != NULL) {
            char* ext = strrchr(entry->d_name, '.');
            if (ext && strcmp(ext, ".so") == 0) {
                lib_exists = true;
                break;
            }
        }
        closedir(dir);
    }

    char budget[32] = {0};
    if (__system_property_get("persist.sys.azenithconf.preloadbudget", budget) <= 0) {
        strcpy(budget, "500M");
    }

    const char* target_path = lib_exists ? lib_path : apk_path;
    const char* target_type = lib_exists ? "libs" : "split apks";

    char preload_cmd[512];
    snprintf(preload_cmd, sizeof(preload_cmd), "sys.azenith-preloadbin -v -t -m %s \"%s\"", budget,
             target_path);

    FILE* fp = popen(preload_cmd, "r");
    if (!fp) {
        log_zenith(LOG_WARN, "Failed to run preloadbin for %s", package);
        return;
    }

    log_zenith(LOG_INFO, "Preloading game %s %s", target_type, package);
    log_preload(LOG_INFO, "Preloading %s %s with budget %s", target_type, target_path, budget);

    char line[1024];
    int total_pages = 0;
    char total_size[32] = {0};

    while (fgets(line, sizeof(line), fp)) {
        line[strcspn(line, "\n")] = 0;

        char* p_pages = strstr(line, "Touched Pages:");
        if (p_pages) {
            int pages = 0;
            char size[32] = {0};

            if (sscanf(p_pages, "Touched Pages: %d (%31[^)])", &pages, size) == 2) {
                total_pages += pages;
                strncpy(total_size, size, sizeof(total_size) - 1);
                total_size[sizeof(total_size) - 1] = '\0';

                log_zenith(LOG_DEBUG, "Preloading complete: %d memory pages touched", pages);
                log_zenith(LOG_DEBUG, "Total memory used for preloaded libraries: %s", size);
            } else {
                log_zenith(LOG_WARN, "Failed to parse Touched Pages");
            }
            continue;
        }

        char* ext = strrchr(line, '.');
        if (ext) {
            if (strcmp(ext, ".so") == 0 || strcmp(ext, ".apk") == 0 || strcmp(ext, ".dm") == 0 ||
                strcmp(ext, ".odex") == 0 || strcmp(ext, ".vdex") == 0 ||
                strcmp(ext, ".art") == 0) {
                log_preload(LOG_DEBUG, "Touched: %s", line);
            }
        }
    }

    log_preload(LOG_INFO, "Game %s preloaded success: total %d pages touched (~%s)", package,
                total_pages, total_size);

    pclose(fp);
}
