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

#include "AZenith.h"

/**
 * @brief Thread worker function to run GamePreload asynchronously.
 * @param arg Pointer to PreloadArgs structure.
 * @return NULL
 */
void* async_preload_worker(void* arg) {
    PreloadArgs* args = (PreloadArgs*)arg;
    GamePreload(args->package);
    free(args);
    return NULL;
}

/**
 * @brief Applies system tuning parameters specifically for Performance Mode.
 * @param ctx Pointer to DaemonContext structure.
 */
void apply_performance_profile(DaemonContext* ctx) {
    toast("Applying Performance Profile");

    ctx->cur_mode = PERFORMANCE_PROFILE;
    ctx->need_profile_checkup = false;

    notify("Performance Profile", "Running at %s", false, 0, active_app_name ? active_app_name : gamestart);
    log_zenith(LOG_INFO, "Applying performance profile for %s", active_app_name ? active_app_name : gamestart);

    if (IS_TRUE(opts.perf_lite_mode)) {
        systemv("setprop persist.sys.azenithconf.litemode 1");
    } else if (IS_FALSE(opts.perf_lite_mode)) {
        systemv("setprop persist.sys.azenithconf.litemode 0");
    } else {
        char lite_prop[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.azenithconf.cpulimit", lite_prop);
        systemv("setprop persist.sys.azenithconf.litemode %s", (strcmp(lite_prop, "1") == 0) ? "1" : "0");
    }

    if (ctx->saved_zen_mode < 0) {
        ctx->saved_zen_mode = current_system_cache.zen_mode;
    }

    if (IS_TRUE(opts.dnd_on_gaming)) {
        if (ctx->saved_zen_mode == 0) {
            systemv("sys.azenith-utilityconf enableDND");
        }
        ctx->dnd_enabled = true;
    } else if (!IS_FALSE(opts.dnd_on_gaming)) {
        char dnd_state[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.azenithconf.dnd", dnd_state);
        if (strcmp(dnd_state, "1") == 0) {
            if (ctx->saved_zen_mode == 0) {
                systemv("sys.azenith-utilityconf enableDND");
            }
            ctx->dnd_enabled = true;
        }
    }

    EXECUTE("Performance Profile", run_profiler(PERFORMANCE_PROFILE));

    if (!IS_DEFAULT(opts.refresh_rate)) {
        int rr = atoi(opts.refresh_rate);
        if (ctx->saved_refresh_rate < 0) {
            ctx->saved_refresh_rate = get_current_refresh_rate();
        }
        apply_dynamic_refresh_rate(rr);
    }

    bool is_preload_active = false;
    if (!IS_FALSE(opts.game_preload)) {
        char preload_active[PROP_VALUE_MAX] = {0};
        if (__system_property_get("persist.sys.azenithconf.APreload", preload_active) > 0) {
            is_preload_active = (strcmp(preload_active, "1") == 0);
        }
    }

    if (IS_TRUE(opts.game_preload) || is_preload_active) {
        notify("AZenith Preload", "Preloading initiated for: %s", true, 10000, active_app_name ? active_app_name : gamestart);

        PreloadArgs* p_args = malloc(sizeof(PreloadArgs));
        if (p_args) {
            strncpy(p_args->package, gamestart, sizeof(p_args->package) - 1);
            p_args->package[sizeof(p_args->package) - 1] = '\0';

            pthread_t preload_thread;
            pthread_attr_t attr;
            pthread_attr_init(&attr);
            pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);

            if (pthread_create(&preload_thread, &attr, async_preload_worker, p_args) != 0) {
                log_zenith(LOG_ERROR, "Failed to spawn async preload thread");
                free(p_args);
            }
            pthread_attr_destroy(&attr);
        } else {
            log_zenith(LOG_ERROR, "Failed to allocate memory for preload arguments");
        }
    }
}

/**
 * @brief Reverts system to Endurance state (Eco Mode).
 * @param ctx Pointer to DaemonContext structure.
 */
void apply_eco_profile(DaemonContext* ctx) {
    if (ctx->cur_mode == ECO_MODE)
        return;

    toast("Applying Eco Mode");

    ctx->cur_mode = ECO_MODE;
    ctx->need_profile_checkup = false;

    notify("ECO Mode", "System is now at Endurance state", false, 0);
    log_zenith(LOG_INFO, "Applying ECO Mode");

    if (ctx->saved_refresh_rate > 0) {
        apply_dynamic_refresh_rate(ctx->saved_refresh_rate);
        ctx->saved_refresh_rate = -1;
    }

    if (ctx->dnd_enabled) {
        if (ctx->saved_zen_mode == 0) {
            systemv("sys.azenith-utilityconf disableDND");
        }
        ctx->dnd_enabled = false;
    }
    ctx->saved_zen_mode = -1;

    if (strlen(ctx->saved_renderer) > 0) {
        char current_now[PROP_VALUE_MAX] = {0};
        __system_property_get("debug.hwui.renderer", current_now);

        if (strlen(current_now) == 0)
            strcpy(current_now, "default");

        if (strcmp(current_now, ctx->saved_renderer) != 0) {
            log_zenith(LOG_INFO, "Restoring original system renderer: %s", ctx->saved_renderer);
            if (strcmp(ctx->saved_renderer, "default") == 0) {
                systemv("sys.azenith-utilityconf setrender default");
            } else {
                systemv("sys.azenith-utilityconf setrender %s", ctx->saved_renderer);
            }
        }
        memset(ctx->saved_renderer, 0, sizeof(ctx->saved_renderer));
    }

    EXECUTE("ECO Mode", run_profiler(ECO_MODE));
}

/**
 * @brief Reverts system to Optimal state (Balanced Mode).
 * @param ctx Pointer to DaemonContext structure.
 */
void apply_balanced_profile(DaemonContext* ctx) {
    if (ctx->is_initialize_complete && ctx->cur_mode == BALANCED_PROFILE)
        return;

    toast("Applying Balanced Profile");

    ctx->cur_mode = BALANCED_PROFILE;
    ctx->need_profile_checkup = false;

    notify("Balanced Profile", "System is now at Optimal state", false, 0);
    log_zenith(LOG_INFO, "Applying balanced profile");

    if (ctx->saved_refresh_rate > 0) {
        apply_dynamic_refresh_rate(ctx->saved_refresh_rate);
        ctx->saved_refresh_rate = -1;
    }

    if (ctx->dnd_enabled) {
        if (ctx->saved_zen_mode == 0) {
            systemv("sys.azenith-utilityconf disableDND");
        }
        ctx->dnd_enabled = false;
    }
    ctx->saved_zen_mode = -1;

    if (strlen(ctx->saved_renderer) > 0) {
        char current_now[PROP_VALUE_MAX] = {0};
        __system_property_get("debug.hwui.renderer", current_now);

        if (strlen(current_now) == 0)
            strcpy(current_now, "default");

        if (strcmp(current_now, ctx->saved_renderer) != 0) {
            log_zenith(LOG_INFO, "Restoring original system renderer: %s", ctx->saved_renderer);
            if (strcmp(ctx->saved_renderer, "default") == 0) {
                systemv("sys.azenith-utilityconf setrender default");
            } else {
                systemv("sys.azenith-utilityconf setrender %s", ctx->saved_renderer);
            }
        }
        memset(ctx->saved_renderer, 0, sizeof(ctx->saved_renderer));
    }

    EXECUTE("Balanced Profile", run_profiler(BALANCED_PROFILE));

    if (!ctx->is_initialize_complete) {
        notify("Daemon Info", "AZenith is running successfully", false, 60000);
        ctx->is_initialize_complete = true;
    }
}
