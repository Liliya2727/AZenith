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
#include <fcntl.h>
#include <errno.h>

/**
 * @brief Checks if the Java companion daemon is alive by verifying the lock status of the specified file.
 * @note This function only inspects the lock state using fcntl F_GETLK and does not acquire the lock itself.
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
 * @brief Writes formatted content to the specified file with optional appending and flock protection.
 * @note Avoid using flock on /sdcard due to Android FUSE limitations.
 * @param filename Path to the target file.
 * @param append Set to true for append mode, false for overwrite (truncate) mode.
 * @param use_flock Set to true to acquire an exclusive lock during the write operation.
 * @param data Format string for content, followed by variable arguments.
 * @return 0 if the content was written successfully, -1 on any error or truncation.
 */
int write2file(const char* filename, const bool append, const bool use_flock, const char* data, ...) {
    if (!data)
        return -1;

    char content[MAX_DATA_LENGTH];
    va_list args;
    va_start(args, data);
    int len = vsnprintf(content, sizeof(content), data, args);
    va_end(args);

    if (len <= 0)
        return -1;

    if (len >= (int)sizeof(content))
        return -1;

    int flags = O_WRONLY | O_CREAT;
    flags |= append ? O_APPEND : O_TRUNC;
    int fd = open(filename, flags, 0644);
    if (fd == -1)
        return -1;

    if (use_flock && flock(fd, LOCK_EX) == -1) {
        close(fd);
        return -1;
    }

    ssize_t written = write(fd, content, len);

    if (use_flock)
        flock(fd, LOCK_UN);
    close(fd);

    return (written == len) ? 0 : -1;
}

/**
 * @brief Checks if the daemon is already running by attempting to acquire a non-blocking exclusive flock.
 * @return 0 if the lock is successfully acquired (daemon not running), -1 if it fails (already running).
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

/**
 * @brief Checks whether the specified file is empty or not.
 * @param filename Path to the target file.
 * @return 1 if the file is empty, 0 if it contains data, or -1 if the file cannot be opened.
 */
int is_file_empty(const char *filename) {
    FILE *file = fopen(filename, "rb");
    if (!file) {
        perror("fopen failed");
        return -1;
    }
    
    int ch = fgetc(file);
    if (ch == EOF) {
        if (feof(file)) {
            fclose(file);
            return 1;
        } else {
            perror("fgetc failed");
            fclose(file);
            return -1;
        }
    }
    
    fclose(file);
    return 0;
}
