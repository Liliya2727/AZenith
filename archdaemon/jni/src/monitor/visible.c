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

/**
 * @brief Reads the currently visible (foreground) app from the cached state.
 * @param cache Pointer to the SystemStateCache containing current states.
 * @return Returns a malloc()'d string containing the package name, or NULL if none is found.
 *         Caller must free() the returned pointer.
 */
char* get_visible_package(SystemStateCache* cache) {
    if (!cache)
        return NULL;

    // Check if the screen is actually awake via the function pointer
    // (Assuming the function pointer signature is updated to take cache as argument)
    if (!get_screenstate(cache))
        return NULL;

    if (strlen(cache->focused_app) > 0) {
        return strdup(cache->focused_app);
    }

    return NULL;
}
