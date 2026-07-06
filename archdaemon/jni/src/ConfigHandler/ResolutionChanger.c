/*
 * Copyright (C) 2025-2026 Zexshia
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
#include <regex.h>

static bool capture_cmd_output(const char* cmd, char* out, size_t outsz) {
    FILE* fp = popen(cmd, "r");
    if (!fp) {
        log_zenith(LOG_ERROR, "ResolutionChanger: Failed to popen '%s'", cmd);
        return false;
    }
    size_t total = 0;
    out[0] = '\0';
    char line[256];
    while (fgets(line, sizeof(line), fp) != NULL) {
        size_t len = strlen(line);
        if (total + len >= outsz)
            break;
        memcpy(out + total, line, len);
        total += len;
        out[total] = '\0';
    }
    pclose(fp);
    return true;
}

/* Mirrors `grep -oE 'PATTERN' | tail -n1` on a captured buffer */
static bool regex_last_match(const char* buf, const char* pattern, char* out, size_t outsz) {
    regex_t re;
    regmatch_t match;
    bool found = false;

    if (regcomp(&re, pattern, REG_EXTENDED) != 0)
        return false;

    const char* cursor = buf;
    while (regexec(&re, cursor, 1, &match, 0) == 0) {
        size_t len = match.rm_eo - match.rm_so;
        if (len >= outsz)
            len = outsz - 1;
        strncpy(out, cursor + match.rm_so, len);
        out[len] = '\0';
        found = true;
        cursor += match.rm_eo;
    }

    regfree(&re);
    return found;
}

/**
 * @brief Applies a downscaled resolution/density, saving originals into ctx
 *        the first time it's called (mirrors saved_renderer pattern).
 * @param ctx Daemon context, used to store fallback values in-memory.
 * @param target_width Desired width in px (e.g. 720, 1080).
 */
void apply_resolution_target(DaemonContext* ctx, int target_width) {
    char size_buf[256] = {0};
    char density_buf[256] = {0};
    char active_size[32] = {0};
    char active_density[16] = {0};

    capture_cmd_output("cmd window size", size_buf, sizeof(size_buf));
    capture_cmd_output("cmd window density", density_buf, sizeof(density_buf));

    if (!regex_last_match(size_buf, "[0-9]+x[0-9]+", active_size, sizeof(active_size)) ||
        !regex_last_match(density_buf, "[0-9]+", active_density, sizeof(active_density))) {
        log_zenith(LOG_ERROR, "ResolutionChanger: Failed to parse current window size/density");
        return;
    }

    int current_width = 0, current_height = 0;
    sscanf(active_size, "%dx%d", &current_width, &current_height);
    int current_density = atoi(active_density);

    if (target_width == current_width) {
        log_zenith(LOG_INFO, "ResolutionChanger: Already at width %d, skipping.", target_width);
        return;
    }

    /* Save fallback only once, same behavior as saved_renderer */
    if (!ctx->resolution_applied) {
        ctx->saved_width = current_width;
        ctx->saved_height = current_height;
        ctx->saved_density = current_density;
        ctx->resolution_applied = true;
        log_zenith(LOG_INFO, "ResolutionChanger: Saved fallback %dx%d @%d",
                   current_width, current_height, current_density);
    }

    int new_height = (current_height * target_width) / current_width;
    int new_density = (current_density * target_width) / current_width;

    char cmd[128];
    snprintf(cmd, sizeof(cmd), "cmd window size %dx%d", target_width, new_height);
    systemv(cmd);
    snprintf(cmd, sizeof(cmd), "cmd window density %d", new_density);
    systemv(cmd);

    log_zenith(LOG_INFO, "ResolutionChanger: Applied %dx%d @%d (was %dx%d @%d)",
               target_width, new_height, new_density,
               current_width, current_height, current_density);
}

/**
 * @brief Restores window size/density from ctx's saved fallback values.
 * @param ctx Daemon context holding the saved fallback.
 */
void restore_window_resolution(DaemonContext* ctx) {
    if (!ctx->resolution_applied) {
        log_zenith(LOG_INFO, "ResolutionChanger: No resolution override active, nothing to restore.");
        return;
    }

    char cmd[128];
    snprintf(cmd, sizeof(cmd), "cmd window size %dx%d", ctx->saved_width, ctx->saved_height);
    systemv(cmd);
    snprintf(cmd, sizeof(cmd), "cmd window density %d", ctx->saved_density);
    systemv(cmd);

    log_zenith(LOG_INFO, "ResolutionChanger: Restored %dx%d @%d",
               ctx->saved_width, ctx->saved_height, ctx->saved_density);

    ctx->resolution_applied = false;
    ctx->saved_width = 0;
    ctx->saved_height = 0;
    ctx->saved_density = 0;
}
