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
 * @brief GLOBAL VARIABLES
 */
char* gamestart = NULL;
char* active_app_name = NULL;
pid_t game_pids[MAX_GAME_PIDS] = {0};
int game_pid_count = 0;
bool is_restarting_renderer = false;
GameConfig opts;
SystemStateCache current_system_cache;
int java_lock_pipe[2];
bool java_daemon_died = false;
GameConfig* g_game_cache = NULL;
int g_game_cache_count = 0;
pthread_mutex_t cache_mutex = PTHREAD_MUTEX_INITIALIZER;

/**
 * @brief Initializes the daemon context with default values.
 * @param ctx Pointer to the DaemonContext structure.
 */
void init_daemon_context(DaemonContext* ctx) {
    memset(ctx, 0, sizeof(DaemonContext));
    ctx->is_initialize_complete = false;
    ctx->dnd_enabled = false;
    ctx->need_profile_checkup = false;
    ctx->bypass_applied = false;
    ctx->has_applied_renderer = false;
    ctx->grace_period_active = false;
    ctx->prev_screen_state = -1;
    ctx->saved_refresh_rate = -1;
    ctx->saved_zen_mode = -1;
    ctx->pid_retries = 0;
    ctx->screen_off_timer = 0;
    ctx->cur_mode = PERFCOMMON;
    strcpy(ctx->last_freqoffset, "Initial");
    strcpy(ctx->prev_ai_state, "0");
    ctx->java_lock_path = "/data/adb/.config/AZenith/java.lock";
    ctx->resolution_applied = false;
    ctx->used_legacy_fallback = false;
    ctx->fg_away_active = false;
    ctx->fg_away_timer = 0;
}
