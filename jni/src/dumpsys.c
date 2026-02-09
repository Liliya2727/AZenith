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

/************************************************************
 * Function Name   : get_visible_package
 * Description     : Reads "dumpsys window displays" and extracts the
 *                   package name of the currently visible (foreground) app.
 * Returns.        : Returns a malloc()'d string containing the package name,
 *                   or NULL if none is found. Caller must free().
 ************************************************************/
char* get_visible_package(void) {
    if (!get_screenstate())
        return NULL;

    FILE* fp = popen("dumpsys window displays", "r");
    if (!fp) {
        log_zenith(LOG_INFO, "Failed to run dumpsys window displays");
        return NULL;
    }

    char line[MAX_LINE];
    char pkg[MAX_PACKAGE] = {0};
    bool in_section = false;
    bool task_visible = false;

    while (fgets(line, sizeof(line), fp)) {
        line[strcspn(line, "\n")] = 0;

        if (!in_section) {
            if (strstr(line, "Application tokens in top down Z order:"))
                in_section = true;
            continue;
        }

        if (strstr(line, "* Task{") && strstr(line, "type=standard")) {
            task_visible = strstr(line, "visible=true") != NULL;
            continue;
        }

        if (task_visible && strstr(line, "* ActivityRecord{")) {
            char* start = strstr(line, " u0 ");
            if (start) {
                start += 4;
                char* slash = strchr(start, '/');
                if (slash) {
                    size_t len = slash - start;
                    if (len >= MAX_PACKAGE) len = MAX_PACKAGE - 1;
                    memcpy(pkg, start, len);
                    pkg[len] = 0;
                    break;
                }
            }
            task_visible = false;
        }
    }

    pclose(fp);
    return pkg[0] ? strdup(pkg) : NULL;
}
