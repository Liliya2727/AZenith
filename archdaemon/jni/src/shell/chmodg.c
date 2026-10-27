/*
 * Copyright (C) 2024-2025 Zexshia
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

/**
 * @brief Applies chmod() to every path matching a glob pattern.
 * @note Failures are ignored on purpose: unmatched patterns and missing
 *       sysfs nodes are expected across different SoC/kernel layouts.
 * @param pattern Glob pattern (e.g. "/sys/class/thermal/thermal_zone*\/temp").
 * @param mode    Permission bits to apply (e.g. 0444).
 */
void chmodg(const char* pattern, mode_t mode) {
    glob_t g = {0};
    if (glob(pattern, 0, NULL, &g) == 0) {
        for (size_t i = 0; i < g.gl_pathc; i++) {
            chmod(g.gl_pathv[i], mode);
        }
    }
    globfree(&g);
}