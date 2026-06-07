/*
 * Copyright (C) 2024-2025 Rem01Gaming x Zexshia
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
#include <sys/system_properties.h>
#include <time.h>

/***********************************************************************************
 * Function Name      : apply_smart_renderer
 * Inputs             : target_type, pkg, saved_ref
 * Returns            : bool - true if renderer switched
 * Description        : Checks current renderer and switches if necessary
 ***********************************************************************************/
bool apply_smart_renderer(const char* target_type, const char* pkg, char* saved_ref) {
    if (target_type == NULL || strcmp(target_type, "default") == 0 || strlen(target_type) == 0) return false;

    char current_renderer[PROP_VALUE_MAX] = {0};
    __system_property_get("debug.hwui.renderer", current_renderer);

    if (strlen(saved_ref) == 0) {
        strncpy(saved_ref, current_renderer, PROP_VALUE_MAX - 1);
    }

    const char* real_target = strcmp(target_type, "vulkan") == 0 ? "vulkan" : "skiagl";

    if (strcmp(current_renderer, real_target) != 0) {
        log_zenith(LOG_INFO, "Switching to %s...", real_target);

        systemv("sys.azenith-utilityconf setrenderer %s", real_target);
        
        sleep(2);

        systemv("am force-stop %s && am start -n $(cmd package resolve-activity --brief %s | tail -n 1)", pkg, pkg);

        return true;
    }
    return false;
}
