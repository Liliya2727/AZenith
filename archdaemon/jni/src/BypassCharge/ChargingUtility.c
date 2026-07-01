/*
 * Copyright (C) 2026-2027 Zexshia
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
#include <dirent.h>
#include <string.h>
#include <sys/system_properties.h>

/**
 * @brief Writes a value to a sysfs/proc node and optionally sets it to read-only (chmod 0444) to
 * lock the value.
 * @param path Path to the sysfs or proc target file.
 * @param value String value to write.
 * @param lock Set to 1 to lock file as read-only, 0 to leave it writeable.
 * @return Status result of systemv execution.
 */
int echo_to_file(const char* path, const char* value, int lock) {
    if (access(path, F_OK) != 0)
        return -1;
    chmod(path, 0644);
    int res = systemv("echo '%s' > %s", value, path);
    if (lock)
        chmod(path, 0444);
    return res;
}

/**
 * @brief Checks the battery status via sysfs to determine if a power source is connected.
 * @return 1 if status is Charging or Full, 0 otherwise.
 */
int is_charging() {
    char status[32] = {0};
    int fd = open("/sys/class/power_supply/battery/status", O_RDONLY);
    if (fd < 0)
        return 0;
    read(fd, status, sizeof(status) - 1);
    close(fd);
    return (strstr(status, "Charging") || strstr(status, "Full")) ? 1 : 0;
}

/**
 * @brief Reads battery current from common sysfs paths and normalizes the value to milliAmperes.
 * @return Battery current in mA (always a positive value), or 9999 if paths are inaccessible.
 */
int read_current_ma() {
    const char* current_paths[] = {"/sys/class/power_supply/battery/current_now",
                                   "/sys/class/power_supply/battery/BatteryAverageCurrent"};
    for (int i = 0; i < 2; i++) {
        int fd = open(current_paths[i], O_RDONLY);
        if (fd >= 0) {
            char buf[32] = {0};
            read(fd, buf, sizeof(buf) - 1);
            close(fd);
            long val = atol(buf);
            if (val < 0)
                val = -val;
            return (val > 1000) ? (int)(val / 1000) : (int)val;
        }
    }
    return 9999;
}

/**
 * @brief Retrieves the saved bypass path from system properties and restores the node to its normal
 * charging state.
 */
void disable_bypass() {
    char path_key[PROP_VALUE_MAX];
    __system_property_get("persist.sys.azenithconf.bypasspath", path_key);

    if (strlen(path_key) == 0 || strcmp(path_key, "UNSUPPORTED") == 0)
        return;

    int total_nodes = bypass_list_size;

    for (int i = 0; i < total_nodes; i++) {
        if (strcmp(bypass_list[i].name, path_key) == 0) {
            echo_to_file(bypass_list[i].path, bypass_list[i].off_val, 0);
            log_zenith(LOG_INFO, "Bypass Charging Disabled");
            return;
        }
    }
}

/**
 * @brief Sets the active bypass node to its 'on' value and locks it to bypass the battery charging
 * circuit.
 * @return 0 on success, -1 if unsupported, -2 if node not found in internal structure.
 */
int enable_bypass() {
    char path_key[PROP_VALUE_MAX];
    __system_property_get("persist.sys.azenithconf.bypasspath", path_key);

    if (strlen(path_key) == 0 || strcmp(path_key, "UNSUPPORTED") == 0)
        return -1;

    int total_nodes = bypass_list_size;

    for (int i = 0; i < total_nodes; i++) {
        if (strcmp(bypass_list[i].name, path_key) == 0) {
            echo_to_file(bypass_list[i].path, bypass_list[i].on_val, 1);
            return 0;
        }
    }
    return -2;
}
