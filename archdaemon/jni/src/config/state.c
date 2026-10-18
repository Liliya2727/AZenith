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

#include <AZenith.h>

/**
 * @brief Persists critical runtime state (renderer, refresh rate, zen mode, bypass charging) to disk
 *        so it survives an abrupt daemon restart.
 * @param ctx Pointer to DaemonContext structure.
 */
void save_daemon_state(DaemonContext* ctx) {
    FILE* fp = fopen(DAEMON_STATE_FILE, "w");
    if (!fp) {
        log_error("state", "state file open failed path=%s errno=%d", DAEMON_STATE_FILE, errno);
        return;
    }

    fprintf(fp, "saved_renderer=%s\n", strlen(ctx->saved_renderer) > 0 ? ctx->saved_renderer : "");
    fprintf(fp, "saved_sys_renderer=%s\n", strlen(ctx->saved_sys_renderer) > 0 ? ctx->saved_sys_renderer : "");
    fprintf(fp, "saved_refresh_rate=%d\n", ctx->saved_refresh_rate);
    fprintf(fp, "saved_zen_mode=%d\n", ctx->saved_zen_mode);
    fprintf(fp, "dnd_enabled=%d\n", ctx->dnd_enabled ? 1 : 0);
    fprintf(fp, "cur_mode=%d\n", ctx->cur_mode);
    fprintf(fp, "bypass_applied=%d\n", ctx->bypass_applied ? 1 : 0);
    fprintf(fp, "bypass_charging=%s\n", strlen(opts.bypass_charging) > 0 ? opts.bypass_charging : "default");

    fclose(fp);
    log_debug("state", "state saved renderer=%s sys_renderer=%s refresh=%d zen=%d mode=%s bypass=%d bypass_charging=%s",
              ctx->saved_renderer, ctx->saved_sys_renderer, ctx->saved_refresh_rate,
              ctx->saved_zen_mode, profile_mode_name(ctx->cur_mode),
              ctx->bypass_applied ? 1 : 0, opts.bypass_charging);
}

/**
 * @brief Restores previously persisted runtime state on daemon startup, if present.
 *        Deletes the state file after successful restore to avoid stale reads.
 *        Bypass flag/override are restored too so an orphaned bypass node can be
 *        released and the per-app bypass_charging override survives the restart.
 * @param ctx Pointer to DaemonContext structure.
 */
void restore_daemon_state(DaemonContext* ctx) {
    FILE* fp = fopen(DAEMON_STATE_FILE, "r");
    if (!fp) {
        return; // No pending state, normal cold start
    }

    char line[PROP_VALUE_MAX + 32];
    while (fgets(line, sizeof(line), fp)) {
        trim_newline(line);
        char* eq = strchr(line, '=');
        if (!eq) continue;
        *eq = '\0';
        const char* key = line;
        const char* val = eq + 1;

        if (strcmp(key, "saved_renderer") == 0) {
            strncpy(ctx->saved_renderer, val, sizeof(ctx->saved_renderer) - 1);
        } else if (strcmp(key, "saved_sys_renderer") == 0) {
            strncpy(ctx->saved_sys_renderer, val, sizeof(ctx->saved_sys_renderer) - 1);
        } else if (strcmp(key, "saved_refresh_rate") == 0) {
            ctx->saved_refresh_rate = atoi(val);
        } else if (strcmp(key, "saved_zen_mode") == 0) {
            ctx->saved_zen_mode = atoi(val);
        } else if (strcmp(key, "dnd_enabled") == 0) {
            ctx->dnd_enabled = atoi(val) == 1;
        } else if (strcmp(key, "cur_mode") == 0) {
            ctx->cur_mode = (ProfileMode)atoi(val);
        } else if (strcmp(key, "bypass_applied") == 0) {
            ctx->bypass_applied = atoi(val) == 1;
        } else if (strcmp(key, "bypass_charging") == 0) {
            if (strlen(val) > 0) {
                strncpy(opts.bypass_charging, val, sizeof(opts.bypass_charging) - 1);
                opts.bypass_charging[sizeof(opts.bypass_charging) - 1] = '\0';
            } else {
                strcpy(opts.bypass_charging, "default");
            }
        }
    }

    fclose(fp);
    systemv("rm -rf /data/adb/.config/AZenith/daemon_state");
    log_info("state", "state restored renderer=%s refresh=%d mode=%s bypass=%d bypass_charging=%s",
             ctx->saved_renderer, ctx->saved_refresh_rate, profile_mode_name(ctx->cur_mode),
             ctx->bypass_applied ? 1 : 0, opts.bypass_charging);
}
