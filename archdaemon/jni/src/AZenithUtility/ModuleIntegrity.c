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

/**
 * @brief Checks if the module properties have been renamed or modified by a 3rd party.
 */
void is_kanged(void) {
    if (systemv("grep -q '^name=AZenith火$' %s", MODULE_PROP) != 0) [[clang::unlikely]] {
        goto doorprize;
    }

    if (systemv("grep -q '^author=ArchHaven Developers$' %s", MODULE_PROP) != 0) [[clang::unlikely]] {
        goto doorprize;
    }

    return;

doorprize:
    log_zenith(LOG_FATAL, "Module modified by 3rd party, exiting.");
    notify("Daemon Error", "Trying to rename me?", true, 0);
    __system_property_set("persist.sys.azenith.service", "");
    __system_property_set("persist.sys.azenith.state", "stopped");
    exit(EXIT_FAILURE);
}

/**
 * @brief Compares the version inside module.prop with the daemon version.
 */
void check_module_version(void) {
    char DAEMON_VERSION[MAX_LINE] = {0};

    snprintf(DAEMON_VERSION, sizeof(DAEMON_VERSION), "%s", MODULE_VERSION);

    int ret = systemv("grep -q '^version=%s$' %s", DAEMON_VERSION, MODULE_PROP);

    if (ret != 0) [[clang::unlikely]] {
        log_zenith(LOG_FATAL, "AZenith version mismatch with daemon version! please reinstall the module!");
        notify("Daemon Error", "AZenith version mismatch, please reinstall!", true, 0);
        __system_property_set("persist.sys.azenith.service", "");
        __system_property_set("persist.sys.azenith.state", "stopped");
        exit(EXIT_FAILURE);
    }
}
