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

#include "AZenith.h"

/**
 * @brief Loads the initial config file values from disk into context.
 * @param ctx Pointer to the DaemonContext structure.
 */
void load_initial_config_files(DaemonContext* ctx) {
    FILE* fp;
    char val[16];

    if ((fp = fopen("/data/adb/.config/AZenith/freqoffset", "r"))) {
        if (fgets(ctx->config_freqoffset, sizeof(ctx->config_freqoffset), fp)) {
            trim_newline(ctx->config_freqoffset);
        }
        fclose(fp);
    } else {
        strcpy(ctx->config_freqoffset, "Disabled");
    }

    if ((fp = fopen("/data/adb/.config/AZenith/bypasschgconfig/bypasspath", "r"))) {
        if (fgets(ctx->config_bypasspath, sizeof(ctx->config_bypasspath), fp)) {
            trim_newline(ctx->config_bypasspath);
        }
        fclose(fp);
    }

    if ((fp = fopen("/data/adb/.config/AZenith/bypasschgconfig/bypasschg", "r"))) {
        if (fgets(val, sizeof(val), fp))
            ctx->config_bypasschg = atoi(val);
        fclose(fp);
    }

    if ((fp = fopen("/data/adb/.config/AZenith/bypasschgconfig/bypasschgthreshold", "r"))) {
        if (fgets(val, sizeof(val), fp))
            ctx->config_bypasschgthreshold = atoi(val);
        fclose(fp);
    }

    log_zenith(LOG_INFO, "ConfigLoader: Initial configs loaded. Freqoffset: [%s], Bypass: [%s]", ctx->config_freqoffset, ctx->config_bypasspath);
}
