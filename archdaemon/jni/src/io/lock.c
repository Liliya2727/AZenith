/*
 * Copyright (C) 2024-2025 Rem01Gaming
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
#include <errno.h>
#include <fcntl.h>

/**
 * @brief Checks if the Java companion daemon is alive by verifying the lock status of the specified
 * file.
 * @note This function only inspects the lock state using fcntl F_GETLK and does not acquire the
 * lock itself.
 * @param lock_path Path to the Java lock file.
 * @return true if the file is currently locked (Java companion is alive), false otherwise.
 */
bool is_java_lock_held(const char* lock_path) {
    int fd = open(lock_path, O_RDONLY);
    if (fd < 0) {
        return false;
    }

    struct flock fl;
    fl.l_type = F_WRLCK;
    fl.l_whence = SEEK_SET;
    fl.l_start = 0;
    fl.l_len = 0;

    if (fcntl(fd, F_GETLK, &fl) == -1) {
        close(fd);
        return false;
    }

    close(fd);

    return (fl.l_type != F_UNLCK);
}

/**
 * @brief Checks if the daemon is already running by attempting to acquire a non-blocking exclusive
 * flock.
 * @return 0 if the lock is successfully acquired (daemon not running), -1 if it fails (already
 * running).
 */
int check_running_state(void) {
    int fd = open(LOCK_FILE, O_WRONLY | O_CREAT, 0644);
    if (fd == -1) {
        perror("open");
        return -1;
    }

    if (flock(fd, LOCK_EX | LOCK_NB) == -1) {
        close(fd);
        return -1;
    }

    return 0;
}
