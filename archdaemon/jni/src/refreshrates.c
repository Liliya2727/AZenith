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

/**
 * @brief Retrieves the current screen refresh rate from the app monitor file.
 * @return The current refresh rate in Hz, or -1 if the file cannot be read.
 */
int get_current_refresh_rate(void) {
    FILE *fp = fopen(APP_MONITOR_FILE, "r");
    if (!fp) {
        return -1;
    }

    char line[256];
    int refresh_rate = -1;

    while (fgets(line, sizeof(line), fp)) {
        if (strncmp(line, "refresh_rate ", 13) == 0) {
            if (sscanf(line, "refresh_rate %d", &refresh_rate) == 1) {
                break;
            }
        }
    }

    fclose(fp);
    return refresh_rate;
}

/**
 * @brief Detects the maximum supported hardware refresh rate from the app monitor file.
 * @return The maximum refresh rate in Hz, defaulting to 60Hz if undetected or on error.
 */
int get_max_refresh_rate(void) {
    FILE *fp = fopen(APP_MONITOR_FILE, "r");
    if (!fp) {
        log_zenith(LOG_WARN, "App monitor file not found, defaulting max refresh rate to 60Hz");
        return 60;
    }

    char line[256];
    int max_rr = 60;

    while (fgets(line, sizeof(line), fp)) {
        if (strncmp(line, "max_refresh_rate ", 17) == 0) {
            if (sscanf(line, "max_refresh_rate %d", &max_rr) == 1) {
                break;
            }
        }
    }

    fclose(fp);
    return max_rr > 0 ? max_rr : 60;
}

/**
 * @brief Calculates and applies the refresh rate mode, capping it to the hardware maximum if needed.
 * @param target_rr The desired target refresh rate in Hz.
 */
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
