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

/**
 * @brief Iterates through the bypass_list to find a working node by analyzing current drops under
 * load.
 * @return 0 if compatible node found, 1 if no node matches, -1 if charger is disconnected.
 */
int check_bypass_compatibility() {
    printf("\n\033[36m[Bypass Charge Compatibility Check]\033[0m Initializing...\n");

    if (!is_charging()) {
        printf("\033[33m[!] WARNING:\033[0m Charger not detected. Plug in first!\n");
        log_zenith(LOG_WARN, "Connect charger to check compatibility");
        return -1;
    }

    int total_nodes = bypass_list_size;

    int skipped_count = 0;
    int tested_count = 0;

    for (int i = 0; i < total_nodes; i++) {
        if (access(bypass_list[i].path, F_OK) != 0) {
            printf("\033[90m[-] Node %-25s: Not Found, skipping...\033[0m\n", bypass_list[i].name);
            skipped_count++;
            continue;
        }

        tested_count++;
        printf("\n\033[1;32m[+]\033[0m Testing Node (%d/%d): \033[1;37m%s\033[0m\n", i + 1, total_nodes, bypass_list[i].name);
        echo_to_file(bypass_list[i].path, bypass_list[i].on_val, 0);

        int last_ma = 9999;
        for (int sec = 1; sec <= 10; sec++) {
            sleep(1);
            last_ma = read_current_ma();
            printf("    Checking current (%ds/10s): \033[33m%d mA\033[0m\n", sec, last_ma);
        }

        echo_to_file(bypass_list[i].path, bypass_list[i].off_val, 0);

        if (last_ma < 50) {
            printf("\n\033[1;32m[SUCCESS]\033[0m Found working node: \033[1m%s\033[0m\n", bypass_list[i].name);
            printf("\033[32m[INFO]\033[0m Process finished. %d nodes skipped.\n", skipped_count);

            log_zenith(LOG_INFO, "Compatible path found: %s. Skipped: %d", bypass_list[i].name, skipped_count);
            return 0;
        } else {
            printf("\033[31m[FAILED]\033[0m Current drop test failed for %s (%d mA)\n", bypass_list[i].name, last_ma);
            printf("------------------------------------------\n");
            usleep(300000);
        }
    }

    printf("\n\033[1;31m[-]\033[0m Final Result: No compatible bypass node found.\n");
    printf("\033[33m[INFO]\033[0m Summary: %d Scanned, %d Tested, %d Skipped.\033[0m\n", total_nodes, tested_count, skipped_count);

    __system_property_set("persist.sys.azenithconf.bypasspath", "UNSUPPORTED");
    __system_property_set("persist.sys.azenithconf.bypasschg", "0");
    __system_property_set("persist.sys.azenithconf.bypasschgthreshold", "20");
    systemv("echo UNSUPPORTED > %s/bypasspath", BYPASSCHG_CONFIG);
    systemv("echo 0 > %s/bypasschg", BYPASSCHG_CONFIG);
    systemv("echo 20 > %s/bypasschgthreshold", BYPASSCHG_CONFIG);
    return 1;
}

/**
 * @brief Displays all hardcoded bypass nodes and checks if they exist on the current device.
 */
void print_bypass_path_list() {
    printf("\n\033[36m[AZenith Available Bypass Path List]\033[0m\n");
    printf("---------------------------------------------------------------------------------------"
           "---\n");
    printf(" %-30s | %-10s | %s\n", "NODE NAME", "STATUS", "SYSFS/PROC PATH");
    printf("---------------------------------------------------------------------------------------"
           "---\n");

    int total_nodes = bypass_list_size;

    int found_count = 0;

    for (int i = 0; i < total_nodes; i++) {
        if (access(bypass_list[i].path, F_OK) == 0) {
            /* Berwarna Hijau jika path ada di device */
            printf(" \033[1;32m%-30s\033[0m | \033[32m[FOUND]\033[0m    | %s\n", bypass_list[i].name, bypass_list[i].path);
            found_count++;
        } else {
            /* Berwarna Abu-abu gelap jika path tidak ada */
            printf(" \033[90m%-30s | [NOT FOUND] | %s\033[0m\n", bypass_list[i].name, bypass_list[i].path);
        }
    }

    printf("---------------------------------------------------------------------------------------"
           "---\n");
    printf("\033[36m[SUMMARY]\033[0m Total Nodes: %d | Available on this device: "
           "\033[1;32m%d\033[0m\n\n",
           total_nodes, found_count);
}
