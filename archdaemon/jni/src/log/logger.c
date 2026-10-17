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
#include <android/log.h>
#include <sys/system_properties.h>

/*
 * Logging contract (keep it boring, greppable and cheap):
 *
 *   ERROR  something failed and needs attention
 *   WARN   odd, but the daemon keeps running
 *   INFO   significant events only (start, stop, profile change, config reload)
 *   DEBUG  internal detail, off unless persist.sys.azenith.debugmode=true
 *
 * One line per event, always shaped as:
 *   YYYY-MM-DD HH:MM:SS.mmm LEVEL [component] message key=value ...
 *
 * Never log per-loop-tick conditions at INFO. Log *transitions* and include
 * context (path, value, errno) on failures so a report from a stranger's
 * device is enough to diagnose.
 */

char* custom_log_tag = NULL;
const char* level_str[] = {"DEBUG", "INFO", "WARN", "ERROR", "FATAL"};

static const char* safe_component(const char* component) {
    return (component && component[0]) ? component : "daemon";
}

static int android_prio(LogLevel level) {
    switch (level) {
    case LOG_INFO:
        return ANDROID_LOG_INFO;
    case LOG_WARN:
        return ANDROID_LOG_WARN;
    case LOG_ERROR:
        return ANDROID_LOG_ERROR;
    case LOG_FATAL:
        return ANDROID_LOG_FATAL;
    default:
        return ANDROID_LOG_DEBUG;
    }
}

static bool debug_enabled(void) {
    char val[PROP_VALUE_MAX] = {0};
    return __system_property_get("persist.sys.azenith.debugmode", val) > 0 && strcmp(val, "true") == 0;
}

/*
 * Rotate <path> -> <path>.1 -> <path>.2 ... once it crosses LOG_MAX_BYTES.
 * On failure we simply keep appending; logging must never take the daemon
 * down. rename() is atomic, so a concurrent writer can only lose one
 * generation at worst.
 */
static void rotate_log_if_needed(const char* path) {
    struct stat st;
    if (stat(path, &st) != 0 || st.st_size < LOG_MAX_BYTES)
        return;

    char src[PATH_MAX];
    char dst[PATH_MAX];

    for (int i = LOG_KEEP_FILES; i >= 1; i--) {
        if (i == 1)
            snprintf(src, sizeof(src), "%s", path);
        else
            snprintf(src, sizeof(src), "%s.%d", path, i - 1);
        snprintf(dst, sizeof(dst), "%s.%d", path, i);

        remove(dst);
        rename(src, dst);
    }
}

static void write_line(const char* path, LogLevel level, const char* component, const char* body) {
    rotate_log_if_needed(path);
    write2file(path, true, true, "%s %s [%s] %s\n", timern(), level_str[level], safe_component(component), body);
}

/**
 * @brief Core file+logcat writer. Every level-specific helper funnels through
 *        here so the on-disk format stays identical everywhere.
 * @param level Log level enum (LOG_INFO, LOG_WARN, etc.).
 * @param component Short subsystem tag, e.g. "thermal", "profile", "bypass".
 * @param message Format string for the log message (prefer key=value pairs).
 */
void log_write(LogLevel level, const char* component, const char* message, ...) {
    char body[1024];
    va_list args;
    va_start(args, message);
    vsnprintf(body, sizeof(body), message, args);
    va_end(args);

    write_line(LOG_FILE, level, component, body);
    __android_log_print(android_prio(level), LOG_TAG, "[%s] %s", safe_component(component), body);
}

/** @brief Detail that is only useful while debugging: written to the verbose log. */
void log_debug(const char* component, const char* message, ...) {
    if (!debug_enabled())
        return;

    char body[1024];
    va_list args;
    va_start(args, message);
    vsnprintf(body, sizeof(body), message, args);
    va_end(args);

    write_line(LOG_VFILE, LOG_DEBUG, component, body);
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "[%s] %s", safe_component(component), body);
}

/** @brief A significant, low-frequency event. */
void log_info(const char* component, const char* message, ...) {
    char body[1024];
    va_list args;
    va_start(args, message);
    vsnprintf(body, sizeof(body), message, args);
    va_end(args);

    write_line(LOG_FILE, LOG_INFO, component, body);
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "[%s] %s", safe_component(component), body);
}

/** @brief Recoverable oddity; the daemon keeps running. */
void log_warn(const char* component, const char* message, ...) {
    char body[1024];
    va_list args;
    va_start(args, message);
    vsnprintf(body, sizeof(body), message, args);
    va_end(args);

    write_line(LOG_FILE, LOG_WARN, component, body);
    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "[%s] %s", safe_component(component), body);
}

/** @brief An operation failed and needs attention. Include path/value/errno. */
void log_error(const char* component, const char* message, ...) {
    char body[1024];
    va_list args;
    va_start(args, message);
    vsnprintf(body, sizeof(body), message, args);
    va_end(args);

    write_line(LOG_FILE, LOG_ERROR, component, body);
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "[%s] %s", safe_component(component), body);
}

/** @brief Unrecoverable failure; the caller is about to stop the daemon. */
void log_fatal(const char* component, const char* message, ...) {
    char body[1024];
    va_list args;
    va_start(args, message);
    vsnprintf(body, sizeof(body), message, args);
    va_end(args);

    write_line(LOG_FILE, LOG_FATAL, component, body);
    __android_log_print(ANDROID_LOG_FATAL, LOG_TAG, "[%s] %s", safe_component(component), body);
}

/**
 * @brief Preload-pipeline logging. Noisy by nature, so it is only emitted when
 *        debug mode is on and goes to a dedicated rotating file.
 * @param component Short subsystem tag, e.g. "preload".
 * @param message Format string for the preload log message.
 */
void log_preload(const char* component, const char* message, ...) {
    if (!debug_enabled())
        return;

    char body[1024];
    va_list args;
    va_start(args, message);
    vsnprintf(body, sizeof(body), message, args);
    va_end(args);

    write_line(LOG_FILE_PRELOAD, LOG_DEBUG, component, body);
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "[%s] %s", safe_component(component), body);
}

/**
 * @brief External logging interface for the companion binaries. Routes DEBUG to
 *        the verbose file and everything else to the main log.
 * @param level Log level enum (0-4).
 * @param tag Custom component tag identifying the external component.
 * @param message Raw log message string.
 */
void external_log(LogLevel level, const char* tag, const char* message, ...) {
    char body[1024];
    va_list args;
    va_start(args, message);
    vsnprintf(body, sizeof(body), message, args);
    va_end(args);

    if (level == LOG_DEBUG)
        write_line(LOG_VFILE, level, tag, body);
    else
        write_line(LOG_FILE, level, tag, body);

    __android_log_print(android_prio(level), LOG_TAG_PROFILE, "[%s] %s", safe_component(tag), body);
}

/**
 * @brief External verbose logging interface for the companion binaries.
 *        Written to the verbose log so the main log stays clean.
 * @param level Log level enum (0-4).
 * @param tag Custom component tag identifying the external component.
 * @param message Raw log message string.
 */
void external_vlog(LogLevel level, const char* tag, const char* message, ...) {
    char body[1024];
    va_list args;
    va_start(args, message);
    vsnprintf(body, sizeof(body), message, args);
    va_end(args);

    write_line(LOG_VFILE, level, tag, body);
    __android_log_print(android_prio(level), LOG_TAG_PROFILE, "[%s] %s", safe_component(tag), body);
}
