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
#include <regex.h>
#include <stdlib.h>
#include <sys/system_properties.h>

/**
 * @brief Reads an Android system property as an integer.
 */
static int get_prop_int(const char* prop) {
    char buf[PROP_VALUE_MAX] = {0};

    if (__system_property_get(prop, buf) <= 0)
        return 0;

    return (int)strtol(buf, NULL, 10);
}

/**
 * @brief Applies downscale/fps via `cmd game set` (Android 13+).
 * @return true on success.
 */
static bool apply_game_set(const char* pkg, const char* downscale, const char* fps) {
    char cmd[256];
    size_t n = (size_t)snprintf(cmd, sizeof(cmd), "cmd game set --mode 2");

    if (!IS_DEFAULT(downscale) && n < sizeof(cmd))
        n += (size_t)snprintf(cmd + n, sizeof(cmd) - n, " --downscale %s", downscale);
    if (!IS_DEFAULT(fps) && n < sizeof(cmd))
        n += (size_t)snprintf(cmd + n, sizeof(cmd) - n, " --fps %s", fps);
    if (n < sizeof(cmd))
        snprintf(cmd + n, sizeof(cmd) - n, " %s", pkg);

    return systemv("%s", cmd) == 0;
}

/**
 * @brief Applies downscale/fps via device_config game_overlay (Android 12 path,
 *        also used as fallback when `cmd game set` fails on Android 13+).
 * @return true on success.
 */
static bool apply_legacy(const char* pkg, const char* downscale, const char* fps) {
    char config[128];
    size_t n = (size_t)snprintf(config, sizeof(config), "mode=2");

    if (!IS_DEFAULT(downscale) && n < sizeof(config))
        n += (size_t)snprintf(config + n, sizeof(config) - n, ",downscaleFactor=%s", downscale);
    if (!IS_DEFAULT(fps) && n < sizeof(config))
        n += (size_t)snprintf(config + n, sizeof(config) - n, ",fps=%s", fps);

    if (systemv("cmd device_config put game_overlay %s %s", pkg, config) != 0)
        return false;

    return systemv("cmd game mode 2 %s", pkg) == 0;
}

/**
 * @brief Applies per-app downscale/fps via Android Game Mode API.
 * @return true if a new intervention was applied (needs restart), false if
 *         already active or nothing to apply.
 */
bool apply_resolution_target(DaemonContext* ctx, const char* pkg,
                             const char* downscale, const char* fps) {
    if (IS_DEFAULT(downscale) && IS_DEFAULT(fps))
        return false;

    if (ctx->resolution_applied) {
        log_debug("resolution", "already applied pkg=%s, skipping", pkg);
        return false;
    }

    int sdk = get_prop_int("ro.build.version.sdk");
    if (sdk < 31) {
        log_debug("resolution", "game mode API unsupported sdk=%d", sdk);
        return false;
    }

    if (sdk >= 33) {
        if (apply_game_set(pkg, downscale, fps)) {
            ctx->resolution_applied = true;
            ctx->used_legacy_fallback = false;
            log_info("resolution", "applied pkg=%s sdk=%d downscale=%s fps=%s",
                     pkg, sdk, IS_DEFAULT(downscale) ? "-" : downscale,
                     IS_DEFAULT(fps) ? "-" : fps);
            return true;
        }
        log_warn("resolution", "cmd game set failed pkg=%s, falling back to device_config", pkg);
    }

    if (!apply_legacy(pkg, downscale, fps)) {
        log_warn("resolution", "legacy apply failed pkg=%s sdk=%d", pkg, sdk);
        return false;
    }

    ctx->resolution_applied = true;
    ctx->used_legacy_fallback = true;
    log_info("resolution", "applied pkg=%s sdk=%d mode=legacy downscale=%s fps=%s",
             pkg, sdk, IS_DEFAULT(downscale) ? "-" : downscale,
             IS_DEFAULT(fps) ? "-" : fps);
    return true;
}

/**
 * @brief Resets the per-app game mode config applied by apply_resolution_target().
 * @param ctx Daemon context holding the applied state and which path was used.
 * @param pkg Package name the config was applied to.
 */
void restore_resolution_target(DaemonContext* ctx, const char* pkg) {
    if (!ctx->resolution_applied) return;

    if (ctx->used_legacy_fallback) {
        systemv("cmd device_config delete game_overlay %s", pkg);
        /* A13+ has `game reset`, A12 doesn't: fall back to mode 1 (standard) */
        if (systemv("cmd game reset %s", pkg) != 0)
            systemv("cmd game mode 1 %s", pkg);
    } else {
        systemv("cmd game reset --mode 2 %s", pkg);
    }

    log_info("resolution", "reset pkg=%s legacy=%d", pkg, ctx->used_legacy_fallback ? 1 : 0);
    ctx->resolution_applied = false;
}
