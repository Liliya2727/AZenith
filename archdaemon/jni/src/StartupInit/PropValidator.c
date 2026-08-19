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

const char* VALID_AZENITH_PROPS[] = {
    "persist.sys.azenith.custom_default_balanced_IO",
    "persist.sys.azenith.custom_default_cpu_gov",
    "persist.sys.azenith.custom_default_maligpu_gov",
    "persist.sys.azenith.custom_performance_IO",
    "persist.sys.azenith.custom_performance_cpu_gov",
    "persist.sys.azenith.custom_performance_maligpu_gov",
    "persist.sys.azenith.custom_powersave_IO",
    "persist.sys.azenith.custom_powersave_cpu_gov",
    "persist.sys.azenith.custom_powersave_maligpu_gov",
    "persist.sys.azenith.debugmode",
    "persist.sys.azenith.default_balanced_IO",
    "persist.sys.azenith.default_cpu_gov",
    "persist.sys.azenith.default_maligpu_gov",
    "persist.sys.azenith.disabletweak",
    "persist.sys.azenith.service",
    "persist.sys.azenith.soctype",
    "persist.sys.azenith.state",
    "persist.sys.azenith.profilenotifications",
    "persist.sys.azenith.dropforeground",
    "persist.sys.azenithconf.AIenabled",
    "persist.sys.azenithconf.APreload",
    "persist.sys.azenithconf.DThermal",
    "persist.sys.azenithconf.SFL",
    "persist.sys.azenithconf.bypasschg",
    "persist.sys.azenithconf.bypasschgthreshold",
    "persist.sys.azenithconf.renderer",
    "persist.sys.azenithconf.bypasspath",
    "persist.sys.azenithconf.clearbg",
    "persist.sys.azenithconf.cpulimit",
    "persist.sys.azenithconf.disabletrace",
    "persist.sys.azenithconf.dnd",
    "persist.sys.azenithconf.fpsged",
    "persist.sys.azenithconf.freqoffset",
    "persist.sys.azenithconf.fstrim",
    "persist.sys.azenithconf.iosched",
    "persist.sys.azenithconf.justintime",
    "persist.sys.azenithconf.litemode",
    "persist.sys.azenithconf.logd",
    "persist.sys.azenithconf.malisched",
    "persist.sys.azenithconf.preloadbudget",
    "persist.sys.azenithconf.renderer",
    "persist.sys.azenithconf.schedtunes",
    "persist.sys.azenithconf.schemeconfig",
    "persist.sys.azenithconf.showtoast",
    "persist.sys.azenithconf.thermalcore",
    "persist.sys.azenithconf.usefpsgo",
    "persist.sys.azenithconf.walttunes",
};
const size_t VALID_AZENITH_PROPS_COUNT = sizeof(VALID_AZENITH_PROPS) / sizeof(VALID_AZENITH_PROPS[0]);

/**
 * @brief Checks whether a given property name belongs to the known/whitelisted
 *        set of AZenith properties currently in use.
 *
 * @param name Null-terminated property name to check.
 * @return true if the property is recognized as valid, false otherwise.
 */
static bool is_known_azenith_prop(const char *name) {
    for (size_t i = 0; i < VALID_AZENITH_PROPS_COUNT; i++) {
        if (strcmp(name, VALID_AZENITH_PROPS[i]) == 0)
            return true;
    }
    return false;
}

typedef struct {
    char names[MAX_PENDING_DELETE][MAX_PROP_NAME_BUF];
    int  count;
} StalePropList;

/**
 * @brief Callback invoked by __system_property_read_callback for each property.
 *        Receives the original, untruncated name and value directly from the
 *        property area.
 *
 * @param cookie  Pointer to a StalePropList used to accumulate results.
 * @param name    Full-length original property name.
 * @param value   Property value (unused here).
 * @param serial  Property serial number (unused here).
 */
static void read_prop_cb(void *cookie, const char *name, const char *value, uint32_t serial) {
    (void)value;
    (void)serial;
    StalePropList *pending = (StalePropList *)cookie;

    if (strncmp(name, AZENITH_PROPERTIES, AZENITH_PROPERTIES_LEN) != 0)
        return;

    if (is_known_azenith_prop(name))
        return;

    if (pending->count < MAX_PENDING_DELETE) {
        strlcpy(pending->names[pending->count], name, MAX_PROP_NAME_BUF);
        pending->count++;
        log_zenith(LOG_WARN, "PropValidator: flagged stale prop -> %s", name);
    } else {
        log_zenith(LOG_WARN, "PropValidator: buffer full, skip %s", name);
    }
}

/**
 * @brief Foreach-level callback invoked per prop_info by __system_property_foreach.
 *        Forwards to __system_property_read_callback to obtain the full,
 *        untruncated property name.
 *
 * @param pi     Property handle provided by foreach.
 * @param cookie Pointer to a StalePropList, forwarded to the next callback.
 */
static void foreach_prop_cb(const prop_info *pi, void *cookie) {
    __system_property_read_callback(pi, read_prop_cb, cookie);
}

/**
 * @brief Validates crucial system files and module integrity before startup.
 */
void validateprop(void) {
    StalePropList pending = { .count = 0 };

    __system_property_foreach(foreach_prop_cb, &pending);

    if (pending.count == 0) {
        log_zenith(LOG_INFO, "PropValidator: no unused prop found, continue...");
        return;
    }

    log_zenith(LOG_INFO, "PropValidator: Found %d unused prop cleaning...", pending.count);

    for (int i = 0; i < pending.count; i++) {
        char cmd[MAX_PROP_NAME_BUF + 32];
        snprintf(cmd, sizeof(cmd), "resetprop -p --delete %s", pending.names[i]);
        log_zenith(LOG_WARN, "PropValidator: delete -> %s", pending.names[i]);
        systemv(cmd);
    }
}
