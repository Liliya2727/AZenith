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
 * Function Name      : apply_dynamic_refresh_rate
 * Inputs             : target_rr (int)
 * Returns            : None
 * Description        : Calculates and applies refresh rate mode based on target
 ***********************************************************************************/
void apply_dynamic_refresh_rate(int target_rr) {
    int max_hw = get_max_refresh_rate();
    int final_rr = target_rr;
    if (final_rr > max_hw) {
        log_zenith(LOG_WARN, "Requested %dHz exceeds hardware max %dHz. Capping to max.", target_rr, max_hw);
        final_rr = max_hw;
    }
    log_zenith(LOG_INFO, "Set refresh rates to %dHz", final_rr);
    systemv("sys.azenith-utilityconf setrefreshrates %d", final_rr);
}


/***********************************************************************************
 * Function Name      : get_max_refresh_rate
 * Inputs             : None
 * Returns            : int - Max Refresh Rate dalam Hz berdasarkan berkas pemetaan
 * Description        : Mendeteksi refresh rate maksimum dari cache file util_mapping.dat
 ***********************************************************************************/
int get_max_refresh_rate(void) {
    const char* mapping_path = "/data/adb/.config/AZenith/util_mapping.dat";
    FILE *fp = fopen(mapping_path, "r");

    if (!fp) {
        log_zenith(LOG_WARN, "Mapping file not found in %s", mapping_path);
        return 60;
    }

    char line[64];
    int max_rr = 60;
    while (fgets(line, sizeof(line), fp)) {
        char *delimiter = strchr(line, '=');
        if (delimiter != NULL) {
            *delimiter = '\0';
            
            int current_fps = atoi(line);
            if (current_fps > max_rr) {
                max_rr = current_fps;
            }
        }
    }

    fclose(fp);
    return max_rr;
}

