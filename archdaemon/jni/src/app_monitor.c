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
 * Description     : Reads the currently visible (foreground) app
 *                   from the cached app status.
 * Returns.        : Returns a malloc()'d string containing the package name,
 *                   or NULL if none is found. Caller must free().
 ************************************************************/
char* get_visible_package(void) {
    if (!get_screenstate())
        return NULL;

    if (strlen(cached_focused_app) > 0) {
        return strdup(cached_focused_app);
    }
    return NULL;
}
