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
 * Function Name      : get_current_refresh_rate
 * Inputs             : None
 * Returns            : int - Current Refresh Rate in Hz
 * Description        : Retrieves current render frame rate
 ***********************************************************************************/
int get_current_refresh_rate(void) {
    FILE *fp = popen(
        "cmd display get-displays | "
        "grep -oE \"renderFrameRate [0-9.]+\" | "
        "awk '{print int($2+0.5)}'",
        "r"
    );

    if (!fp)
        return -1;

    char buf[32] = {0};
    if (!fgets(buf, sizeof(buf), fp)) {
        pclose(fp);
        return -1;
    }

    pclose(fp);
    return atoi(buf);
}

/***********************************************************************************
 * Function Name      : apply_dynamic_refresh_rate
 * Inputs             : target_rr (int)
 * Returns            : None
 * Description        : Calculates and applies refresh rate mode based on target
 ***********************************************************************************/
void apply_dynamic_refresh_rate(int target_rr) {
    int max_hw = get_max_refresh_rate();

    int steps[] = {165, 144, 120, 90, 60};
    int num_steps = sizeof(steps) / sizeof(steps[0]);

    int max_idx = -1;
    int target_idx = -1;

    for (int i = 0; i < num_steps; i++) {
        if (max_hw >= steps[i]) {
            max_idx = i;
            break;
        }
    }

    for (int i = 0; i < num_steps; i++) {
        if (target_rr >= steps[i]) {
            target_idx = i;
            break;
        }
    }

    int mode_to_apply = 0;
    if (max_idx != -1 && target_idx != -1) {
        mode_to_apply = target_idx - max_idx;
    }

    if (mode_to_apply < 0) mode_to_apply = 0;
    
    log_zenith(LOG_INFO, "Set refresh rates to %dHz",
                target_rr);

    systemv("sys.azenith-utilityconf setrefreshrates %d", mode_to_apply);
}

/***********************************************************************************
 * Function Name      : get_max_refresh_rate
 * Inputs             : None
 * Returns            : int - Max Refresh Rate in Hz
 * Description        : Detects max hardware refresh rate via dumpsys
 ***********************************************************************************/
int get_max_refresh_rate(void) {
    const char* cmd = "dumpsys display | grep -oE \"fps=[0-9.]+|refreshRate=[0-9.]+\" | grep -oE \"[0-9.]+\" | sort -rn | head -n1";

    FILE *fp = popen(cmd, "r");
    if (!fp) {
        log_zenith(LOG_WARN, "Failed to execute dumpsys for RR detection");
        return 60;
    }

    char buf[32] = {0};
    float max_rr_f = 60.0f;

    if (fgets(buf, sizeof(buf), fp)) {
        max_rr_f = atof(buf);
    }
    pclose(fp);

    int max_rr = (int)(max_rr_f + 0.5f);
    if (max_rr <= 0) max_rr = 60;

    return max_rr;
}
