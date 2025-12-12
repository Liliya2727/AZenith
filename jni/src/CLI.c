/*
 * Copyright (C) 2024-2025 Zexshia
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
 * Function Name      : print_help
 * Inputs             : None
 * Returns            : None
 * Description        : Prints all available AZenith Daemon CLI commands to stdout.
 *                      Displays usage instructions for running the daemon,
 *                      selecting profiles, and sending log messages.
 ***********************************************************************************/
void print_help() {
    printf(
        "AZenith Daemon CLI (by @Zexshia)\n"
        "Version: %s\n"
        "\n"
        "Usage: sys.azenith-service [options]\n"
        "\n"
        "Options:\n"
        "     -r, --run      Start AZenith daemon service\n"
        "\n"
        "     -p, --profile <1|2|3>\n" 
        "                    Apply AZenith profiles via CLI\n"
        "                    1 : Performance\n"
        "                    2 : Balanced\n"
        "                    3 : Eco Mode\n"
        "\n"
        "     -l, --log <TAG> <LEVEL> <MSG>\n"     
        "                    Write a log message via AZenith logging service\n"
        "                    LEVEL values:\n"
        "                    0 : DEBUG\n"
        "                    1 : INFO\n"
        "                    2 : WARN\n"
        "                    3 : ERROR\n"
        "                    4 : FATAL\n"
        "     -vl, --verboselog <TAG> <LEVEL> <MSG>\n"
        "                    Write a verbose log message via AZenith logging service\n"
        "\n"
        "     -h, --help     Display this help message and exit\n"
        "\n"
        "Examples:\n"
        "     sys.azenith-service --run\n"
        "     sys.azenith-service --profile 2\n"
        "     sys.azenith-service --help\n"
        ,MODULE_VERSION
    );
}

/***********************************************************************************
 * Function Name      : handle_profile
 * Inputs             : argc - number of CLI arguments
 *                      argv - array of CLI argument strings
 * Returns            : int - 0 on success, non-zero on failure
 * Description        : Handles manual profile selection. Validates that Auto Mode
 *                      is disabled, reads the requested profile (1/2/3), logs the
 *                      action, sends a toast message, and executes the profiler.
 *                      Profiles:
 *                          1 = Performance
 *                          2 = Balanced
 *                          3 = Eco Mode
 ***********************************************************************************/
int handle_profile(int argc, char** argv) {
    if (argc < 3 || !argv[2] || !argv[2][0]) {
        fprintf(stderr, "ERROR: Missing profile number. Use --profile <1|2|3>\n");
        return 1;
    }

    char ai_state[PROP_VALUE_MAX] = {0};
    __system_property_get("persist.sys.azenithconf.AIenabled", ai_state);

    if (!strcmp(ai_state, "1")) {
        fprintf(stderr,
            "ERROR: Auto Mode is enabled.\n"
            "       Manual profile selection is blocked.\n");
        return 1;
    }

    const char* profile = argv[2];
    
    if (!strcmp(profile, "0")) {
        log_zenith(LOG_WARN, "WARN: Cannot Apply Profile 0 (Initialize)");
        printf("WARN: Cannot Apply Profile 0 (Initialize)\n");                
    } else if (!strcmp(profile, "1")) {
        log_zenith(LOG_INFO, "Applying Performance Profile via execute");
        toast("Applying Performance Profile");
        run_profiler(PERFORMANCE_PROFILE);
        printf("Applying Performance Profile\n");        
    } else if (!strcmp(profile, "2")) {
        log_zenith(LOG_INFO, "Applying Balanced Profile via execute");
        toast("Applying Balanced Profile");
        run_profiler(BALANCED_PROFILE);
        printf("Applying Balanced Profile\n");
    } else if (!strcmp(profile, "3")) {
        log_zenith(LOG_INFO, "Applying Eco Mode via execute");
        toast("Applying Eco Mode");
        run_profiler(ECO_MODE);
        printf("Applying Eco Mode\n");
    } else {
        fprintf(stderr, "Invalid profiles.\n");
        return 1;
    }

    return 0;
}

/***********************************************************************************
 * Function Name      : handle_log
 * Inputs             : argc - number of CLI arguments
 *                      argv - array of CLI argument strings
 * Returns            : int - 0 on success, non-zero on failure
 * Description        : Handles the --log command. Validates log level (0..4),
 *                      concatenates the message arguments into a single string,
 *                      and forwards the formatted log entry to the external log
 *                      handler.
 *                      Log Levels:
 *                          0 = DEBUG
 *                          1 = INFO
 *                          2 = WARN
 *                          3 = ERROR
 *                          4 = FATAL
 ***********************************************************************************/
int handle_log(int argc, char** argv) {
    if (argc < 5) {
        fprintf(stderr,
            "Usage: --log <TAG> <LEVEL> <MESSAGE>\n"
            "Levels: 0=DEBUG, 1=INFO, 2=WARN, 3=ERROR, 4=FATAL\n");
        return 1;
    }

    const char *tag = argv[2];
    const char *level_str = argv[3];

    int level = atoi(level_str);
    if (level < LOG_DEBUG || level > LOG_FATAL) {
        fprintf(stderr, "ERROR: Invalid log level '%s' (valid 0..4)\n", level_str);
        return 1;
    }

    char message[1024];
    message[0] = '\0';

    size_t remaining = sizeof(message);
    for (int i = 4; i < argc; i++) {
        size_t written = snprintf(
            message + strlen(message),
            remaining,
            "%s%s",
            argv[i],
            (i == argc - 1) ? "" : " "
        );
        if (written >= remaining) {
            fprintf(stderr, "ERROR: Log message too long.\n");
            return 1;
        }
        remaining -= written;
    }

    /* Send the log */
    external_log(level, tag, message);
    return 0;
}

/***********************************************************************************
 * Function Name      : handle_verboselog
 * Inputs             : argc - number of CLI arguments
 *                      argv - array of CLI argument strings
 * Returns            : int - 0 on success, non-zero on failure
 * Description        : Handles the --log command. Validates log level (0..4),
 *                      concatenates the message arguments into a single string,
 *                      and forwards the formatted log entry to the external log
 *                      handler.
 *                      Log Levels:
 *                          0 = DEBUG
 *                          1 = INFO
 *                          2 = WARN
 *                          3 = ERROR
 *                          4 = FATAL
 ***********************************************************************************/
int handle_verboselog(int argc, char** argv) {
    if (argc < 5) {
        fprintf(stderr,
            "Usage: --log <TAG> <LEVEL> <MESSAGE>\n"
            "Levels: 0=DEBUG, 1=INFO, 2=WARN, 3=ERROR, 4=FATAL\n");
        return 1;
    }

    const char *tag = argv[2];
    const char *level_str = argv[3];

    int level = atoi(level_str);
    if (level < LOG_DEBUG || level > LOG_FATAL) {
        fprintf(stderr, "ERROR: Invalid log level '%s' (valid 0..4)\n", level_str);
        return 1;
    }

    char message[1024];
    message[0] = '\0';

    size_t remaining = sizeof(message);
    for (int i = 4; i < argc; i++) {
        size_t written = snprintf(
            message + strlen(message),
            remaining,
            "%s%s",
            argv[i],
            (i == argc - 1) ? "" : " "
        );
        if (written >= remaining) {
            fprintf(stderr, "ERROR: Log message too long.\n");
            return 1;
        }
        remaining -= written;
    }

    /* Send the log */
    external_vlog(level, tag, message);
    return 0;
}
