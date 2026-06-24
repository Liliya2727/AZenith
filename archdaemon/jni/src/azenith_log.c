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

char* custom_log_tag = NULL;
const char* level_str[] = {"D", "I", "W", "E", "F"};

/**
 * @brief Prints and logs a formatted message with a timestamp to a log file and Android logcat.
 * @param level Log level enum (LOG_INFO, LOG_WARN, etc.).
 * @param message Format string for the log message.
 */
void log_zenith(LogLevel level, const char* message, ...) {
    char* timestamp = timern();
    char logMesg[MAX_OUTPUT_LENGTH];
    va_list args;
    va_start(args, message);
    vsnprintf(logMesg, sizeof(logMesg), message, args);
    va_end(args);

    write2file(LOG_FILE, true, true, "%s %s %s: %s\n", timestamp, level_str[level], LOG_TAG, logMesg);

    int android_log_level;
    switch (level) {
        case LOG_INFO:
            android_log_level = ANDROID_LOG_INFO;
            break;
        case LOG_WARN:
            android_log_level = ANDROID_LOG_WARN;
            break;
        case LOG_ERROR:
            android_log_level = ANDROID_LOG_ERROR;
            break;
        default:
            android_log_level = ANDROID_LOG_DEBUG;
            break;
    }

    __android_log_print(android_log_level, LOG_TAG, "%s", logMesg);
}

/**
 * @brief Logs preloading process information if the system debug mode property is enabled.
 * @param level Log level enum.
 * @param message Format string for the preload log message.
 */
void log_preload(LogLevel level, const char* message, ...) {
    char val[PROP_VALUE_MAX] = {0};
    if (__system_property_get("persist.sys.azenith.debugmode", val) > 0) {
        if (strcmp(val, "true") == 0) {
            char* timestamp = timern();
            char logMesg[MAX_OUTPUT_LENGTH];
            va_list args;
            va_start(args, message);
            vsnprintf(logMesg, sizeof(logMesg), message, args);
            va_end(args);

            write2file(LOG_FILE_PRELOAD, true, true, "%s %s %s: %s\n", timestamp, level_str[level], LOG_TAG, logMesg);

            int android_log_level;
            switch (level) {
                case LOG_INFO:
                    android_log_level = ANDROID_LOG_INFO;
                    break;
                case LOG_WARN:
                    android_log_level = ANDROID_LOG_WARN;
                    break;
                case LOG_ERROR:
                    android_log_level = ANDROID_LOG_ERROR;
                    break;
                default:
                    android_log_level = ANDROID_LOG_DEBUG;
                    break;
            }

            __android_log_print(android_log_level, LOG_TAG, "%s", logMesg);
        }
    }
}

/**
 * @brief Logs detailed debug/verbose info to the main log file when debug mode is enabled.
 * @param level Log level enum.
 * @param message Format string for the verbose log message.
 */
void log_verbose(LogLevel level, const char* message, ...) {
    char val[PROP_VALUE_MAX] = {0};
    if (__system_property_get("persist.sys.azenith.debugmode", val) > 0) {
        if (strcmp(val, "true") == 0) {
            char* timestamp = timern();
            char logMesg[MAX_OUTPUT_LENGTH];
            va_list args;
            va_start(args, message);
            vsnprintf(logMesg, sizeof(logMesg), message, args);
            va_end(args);

            write2file(LOG_FILE, true, true, "%s %s %s: %s\n", timestamp, level_str[level], LOG_TAG, logMesg);

            int android_log_level;
            switch (level) {
                case LOG_INFO:
                    android_log_level = ANDROID_LOG_INFO;
                    break;
                case LOG_WARN:
                    android_log_level = ANDROID_LOG_WARN;
                    break;
                case LOG_ERROR:
                    android_log_level = ANDROID_LOG_ERROR;
                    break;
                default:
                    android_log_level = ANDROID_LOG_DEBUG;
                    break;
            }

            __android_log_print(android_log_level, LOG_TAG, "%s", logMesg);
        }
    }
}

/**
 * @brief External logging interface for other applications to write to the main log file.
 * @param level Log level enum (mapped via index 0-4).
 * @param tag Custom log tag identifying the external app.
 * @param message Raw log message string.
 */
void external_log(LogLevel level, const char* tag, const char* message) {
    char* timestamp = timern();
    write2file(LOG_FILE, true, true, "%s %s %s: %s\n", timestamp, level_str[level], tag, message);
}

/**
 * @brief External logging interface for other applications to write to the verbose log file.
 * @param level Log level enum (mapped via index 0-4).
 * @param tag Custom log tag identifying the external app.
 * @param message Raw log message string.
 */
void external_vlog(LogLevel level, const char* tag, const char* message) {
    char* timestamp = timern();
    write2file(LOG_VFILE, true, true, "%s %s %s: %s\n", timestamp, level_str[level], tag, message);
}
