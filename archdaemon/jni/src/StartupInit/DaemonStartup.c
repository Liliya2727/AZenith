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
 * @brief Validates crucial system files and module integrity before startup.
 */
void verify_system_integrity(void) {
    if (check_running_state() != 0) {
        fprintf(stderr, "\033[31mERROR:\033[0m Daemon is already running!\n");
        exit(EXIT_FAILURE);
    }
    systemv("touch %s", PROFILE_MODE_APP);
    systemv("touch %s", GAME_INFO_APP);

    if (is_file_empty("/system/bin/dumpsys") == 1) {
        fprintf(stderr, "\033[31mFATAL ERROR:\033[0m /system/bin/dumpsys was tampered by kill logger module.\n");
        log_fatal("startup", "dumpsys tampered path=/system/bin/dumpsys");
        notify("Daemon Error", "Please remove your stupid kill logger module.", false, 0);
        exit(EXIT_FAILURE);
    }
    if (access(GAMELIST, F_OK) != 0) {
        fprintf(stderr, "\033[31mFATAL ERROR:\033[0m Unable to access Gamelist, either has been removed or moved.\n");
        log_fatal("startup", "critical file missing path=%s", GAMELIST);
        exit(EXIT_FAILURE);
    }
    is_kanged();
    check_module_version();
}

/**
 * @brief Waits for the Java companion daemon to acquire its lock file.
 * @param ctx Pointer to the DaemonContext structure.
 */
void wait_for_java_companion(DaemonContext* ctx) {
    log_info("startup", "waiting for java companion");
    int java_check_retries = 0;
    const int MAX_JAVA_RETRIES = 120;

    while (!is_java_lock_held(ctx->java_lock_path)) {
        if (++java_check_retries > MAX_JAVA_RETRIES) {
            log_fatal("startup", "java companion absent retries=%d", MAX_JAVA_RETRIES);
            notify("Daemon Error", "Java companion daemon crashed or failed to start.", false, 0);
            __system_property_set("persist.sys.azenith.service", "");
            __system_property_set("persist.sys.azenith.state", "stopped");
            exit(EXIT_FAILURE);
        }
        if (java_check_retries <= 1) {
            log_warn("startup", "java companion lock not held retry=%d", java_check_retries);
        }
        sleep(1);
    }
    log_info("startup", "java companion detected");
}

/**
 * @brief Background thread to monitor Java daemon liveness via fcntl blocking.
 * @param arg Pointer to lock path string.
 * @return NULL
 */
void* java_lock_watcher_thread(void* arg) {
    const char* lock_path = (const char*)arg;
    int fd = open(lock_path, O_RDWR | O_CREAT, 0600);

    if (fd >= 0) {
        struct flock fl;
        memset(&fl, 0, sizeof(fl));
        fl.l_type = F_WRLCK;
        fl.l_whence = SEEK_SET;
        if (fcntl(fd, F_SETLKW, &fl) != -1) {
            fl.l_type = F_UNLCK;
            fcntl(fd, F_SETLK, &fl);
        }
        close(fd);
    }

    char signal_byte = '1';
    write(java_lock_pipe[1], &signal_byte, 1);
    return NULL;
}
