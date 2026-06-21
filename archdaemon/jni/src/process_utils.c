/*
 * Copyright (C) 2024-2025 Rem01Gaming
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <AZenith.h>
#include <sys/system_properties.h>

/***********************************************************************************
 * Function Name      : get_pids_of
 * Inputs             : name (char *), pids (pid_t array), max_pids (int)
 * Returns            : int - jumlah PID yang berhasil ditemukan
 * Description        : Menarik semua PID dari background_apps untuk package tertentu.
 ***********************************************************************************/
int get_pids_of(const char* name, pid_t* pids, int max_pids) {
    if (!name || !name[0] || max_pids < 1) return 0;
    
    FILE* fp = fopen("/data/adb/.config/AZenith/background_apps", "r");
    if (!fp) return 0;

    char line[256];
    int count = 0;

    while (fgets(line, sizeof(line), fp) && count < max_pids) {
        char pkg[128];
        pid_t pid;
        int uid;
        
        if (sscanf(line, "%127s %d %d", pkg, &pid, &uid) == 3) {
            if (strcmp(pkg, name) == 0) {
                pids[count] = pid;
                count++;
            }
        }
    }

    fclose(fp);
    return count;
}

/***********************************************************************************
 * Function Name      : uidof
 * Inputs             : pid (pid_t) - PID of process
 * Returns            : uid (int) - UID of process
 * Description        : Fetch UID from background_apps cache.
 * Note               : Returns -1 on error.
 ***********************************************************************************/
int uidof(pid_t pid) {
    if (pid <= 0) return -1;

    FILE* fp = fopen("/data/adb/.config/AZenith/background_apps", "r");
    if (!fp) return -1;

    char line[256];
    while (fgets(line, sizeof(line), fp)) {
        char pkg[128];
        pid_t current_pid;
        int current_uid;
        
        if (sscanf(line, "%127s %d %d", pkg, &current_pid, &current_uid) == 3) {
            if (current_pid == pid) {
                fclose(fp);
                return current_uid;
            }
        }
    }

    fclose(fp);
    return -1;
}


/***********************************************************************************
 * Function Name      : set_priority
 * Inputs             : pid (pid_t) - PID to be boosted
 * Returns            : None
 * Description        : Sets the maximum CPU nice priority and I/O priority of a
 *                      given process.
 ***********************************************************************************/
void set_priority(const pid_t pid) {    
    if (setpriority(PRIO_PROCESS, pid, -20) == -1)
        log_zenith(LOG_ERROR, "Unable to set nice priority for %d", pid);

    if (syscall(SYS_ioprio_set, 1, pid, (1 << 13) | 0) == -1)
        log_zenith(LOG_ERROR, "Unable to set IO priority for %d", pid);
}
