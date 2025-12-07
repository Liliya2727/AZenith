/*
 * Copyright (C) 2024-2025 Rem01Gaming x Zexshia
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
#include <libgen.h>
char* gamestart = NULL;
pid_t game_pid = 0;

int main(int argc, char* argv[]) {
    // Handle case when not running on root
    if (getuid() != 0) {
        fprintf(stderr, "\033[31mERROR:\033[0m Please run this program as root\n");
        exit(EXIT_FAILURE);
    }

    // Expose logging interface for other modules
    char* base_name = basename(argv[0]);
    if (strcmp(base_name, "sys.azenith-service_log") == 0) {
        if (argc < 3) {
            fprintf(stderr, "Usage: sys.azenith-service_log <TAG> <LEVEL> <MESSAGE>\n");
            fprintf(stderr, "Levels: 0=DEBUG, 1=INFO, 2=WARN, 3=ERROR, 4=FATAL\n");
            return EXIT_FAILURE;
        }

        // Parse log level
        int level = atoi(argv[2]);
        if (level < LOG_DEBUG || level > LOG_FATAL) {
            fprintf(stderr, "Invalid log level. Use 0-4.\n");
            return EXIT_FAILURE;
        }

        // Combine message arguments
        size_t message_len = 0;
        for (int i = 3; i < argc; i++) {
            message_len += strlen(argv[i]) + 1;
        }

        char message[message_len];
        message[0] = '\0';

        for (int i = 3; i < argc; i++) {
            strcat(message, argv[i]);
            if (i < argc - 1)
                strcat(message, " ");
        }

        external_log(level, argv[1], message);
        return EXIT_SUCCESS;
    }

    ProfileMode cur_mode = PERFCOMMON;
    // Expose profiler interface
    if (strcmp(base_name, "sys.azenith-profiler") == 0) {
        if (argc < 2) {
            fprintf(stderr, "Usage: sys.azenith-profiler <1|2|3>\n");
            fprintf(stderr, "Usage: 1 = Performance, 2 = Balanced, 3 = Eco Mode\n");
            return EXIT_FAILURE;
        }

        const char* profile = argv[1];

        // Check properties to prevent execution in auto mode
        char ai_state[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.azenithconf.AIenabled", ai_state);

        if (strcmp(ai_state, "1") == 0) {
            log_zenith(LOG_WARN, "Can't apply profile in current mode");
            fprintf(stderr, "\033[31mERROR:\033[0m Cannot apply profile manually while auto mode is enabled.\n");
            return EXIT_FAILURE;
        }

        if (strcmp(profile, "1") == 0) {
            log_zenith(LOG_INFO, "Applying Performance Profile via execute");
            toast("Applying Performance Profile");
            run_profiler(PERFORMANCE_PROFILE);
            cur_mode = PERFORMANCE_PROFILE;
            fprintf(stderr, "Applying Performance Profile\n");
            return EXIT_SUCCESS;
        } else if (strcmp(profile, "2") == 0) {
            log_zenith(LOG_INFO, "Applying Balanced Profile via execute");
            toast("Applying Balanced Profile");
            run_profiler(BALANCED_PROFILE);
            cur_mode = BALANCED_PROFILE;
            fprintf(stderr, "Applying Balanced Profile\n");
            return EXIT_SUCCESS;
        } else if (strcmp(profile, "3") == 0) {
            log_zenith(LOG_INFO, "Applying Eco Mode via execute");
            toast("Applying Eco Mode");
            run_profiler(ECO_MODE);
            cur_mode = ECO_MODE;
            fprintf(stderr, "Applying Eco Mode\n");
            return EXIT_SUCCESS;
        } else {
            fprintf(stderr, "Invalid profile. Use: 1 | 2 | 3\n");
            return EXIT_FAILURE;
        }
    }

    // Make sure only one instance is running
    if (check_running_state() == 1) {
        fprintf(stderr, "\033[31mERROR:\033[0m Another instance of Daemon is already running!\n");
        exit(EXIT_FAILURE);
    }

    // Handle case when module modified by 3rd party
    is_kanged();

    // Daemonize service
    if (daemon(0, 0)) {
        log_zenith(LOG_FATAL, "Unable to daemonize service");
        systemv("setprop persist.sys.azenith.service \"\"");
        systemv("setprop persist.sys.azenith.state stopped");
        exit(EXIT_FAILURE);
    }

    // Register signal handlers
    signal(SIGINT, sighandler);
    signal(SIGTERM, sighandler);

    // Initialize variables
    bool need_profile_checkup = false;
    MLBBState mlbb_is_running = MLBB_NOT_RUNNING;
    static bool is_initialize_complete = false;

    // Remove old logs before start initializing script
    systemv("rm -f /data/adb/.config/AZenith/debug/AZenith.log");
    systemv("rm -f /data/adb/.config/AZenith/debug/AZenithVerbose.log");
    systemv("rm -f /data/adb/.config/AZenith/preload/AZenithPR.log");

    // Initiate Daemon
    log_zenith(LOG_INFO, "Daemon started as PID %d", getpid());
    // Set daemon PID to Prop
    setspid();
    systemv("setprop persist.sys.rianixia.learning_enabled true");
    systemv("setprop persist.sys.azenith.state running");
    notify("Initializing...");
    // Set Default ThermalPath
    systemv("setprop persist.sys.rianixia.thermalcore-bigdata.path /data/adb/.config/AZenith/debug");
    runthermalcore();
    run_profiler(PERFCOMMON);
    char prev_ai_state[PROP_VALUE_MAX] = "0";
    __system_property_get("persist.sys.azenithconf.AIenabled", prev_ai_state);

    while (1) {
        if (cur_mode == PERFORMANCE_PROFILE) {
            usleep(LOOP_INTERVAL_MS * 1000);
        } else {
            sleep(LOOP_INTERVAL_SEC);
        }

        // Handle case when module gets updated
        if (access(MODULE_UPDATE, F_OK) == 0) [[clang::unlikely]] {
            log_zenith(LOG_INFO, "Module update detected, exiting.");
            notify("Please reboot your device to complete module update.");
            systemv("setprop persist.sys.azenith.service \"\"");
            systemv("setprop persist.sys.azenith.state stopped");
            break;
        }

        // Check module state
        checkstate();

        char freqoffset[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.azenithconf.freqoffset", freqoffset);
        if (strstr(freqoffset, "Disabled") == NULL) {
            if (get_screenstate()) {
                if (cur_mode == PERFORMANCE_PROFILE) {
                    // No exec
                } else if (cur_mode == BALANCED_PROFILE) {
                    systemv("sys.azenith-profilesettings applyfreqbalance");
                } else if (cur_mode == ECO_MODE) {
                    systemv("sys.azenith-profilesettings applyfreqbalance");
                }
            } else {
                // Screen Off
            }
        }

        // Update state
        char ai_state[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.azenithconf.AIenabled", ai_state);
        if (is_initialize_complete) {
            if (strcmp(prev_ai_state, "1") == 0 && strcmp(ai_state, "0") == 0) {
                log_zenith(LOG_INFO, "Dynamic profile is disabled, Reapplying Balanced Profiles");
                toast("Applying Balanced Profile");
                cur_mode = BALANCED_PROFILE;
                run_profiler(BALANCED_PROFILE);
            }

            if (strcmp(prev_ai_state, "0") == 0 && strcmp(ai_state, "1") == 0) {
                log_zenith(LOG_INFO, "Dynamic profile is enabled, Reapplying Balanced Profiles");
                toast("Applying Balanced Profile");
                cur_mode = BALANCED_PROFILE;
                run_profiler(BALANCED_PROFILE);
            }
            strcpy(prev_ai_state, ai_state);
            // Skip applying if enabled
            if (strcmp(ai_state, "0") == 0) {
                continue;
            }
        }

        // Only fetch gamestart when user not in-game
        // prevent overhead from dumpsys commands.
        if (!gamestart) {
            gamestart = get_gamestart();
        } else if (game_pid != 0 && kill(game_pid, 0) == -1) [[clang::unlikely]] {
            log_zenith(LOG_INFO, "Game %s exited, resetting profile...", gamestart);
            game_pid = 0;
            free(gamestart);
            gamestart = get_gamestart();
            // Force profile recheck to make sure new game session get boosted
            need_profile_checkup = true;
        }

        if (gamestart)
            mlbb_is_running = handle_mlbb(gamestart);

        if (is_initialize_complete && gamestart && get_screenstate() && mlbb_is_running != MLBB_RUN_BG) {
            // Preload on loop

            // Bail out if we already on performance profile
            if (!need_profile_checkup && cur_mode == PERFORMANCE_PROFILE)
                continue;

            // Get PID and check if the game is "real" running program
            // Handle weird behavior of MLBB
            game_pid = (mlbb_is_running == MLBB_RUNNING) ? mlbb_pid : pidof(gamestart);
            if (game_pid == 0) [[clang::unlikely]] {
                log_zenith(LOG_ERROR, "Unable to fetch PID of %s", gamestart);
                free(gamestart);
                gamestart = NULL;
                continue;
            }

            cur_mode = PERFORMANCE_PROFILE;
            need_profile_checkup = false;
            log_zenith(LOG_INFO, "Applying performance profile for %s", gamestart);
            toast("Applying Performance Profile");
            set_priority(game_pid);
            run_profiler(PERFORMANCE_PROFILE);

            char preload_active[PROP_VALUE_MAX] = {0};
            __system_property_get("persist.sys.azenithconf.APreload", preload_active);
            if (strcmp(preload_active, "1") == 0) {
                GamePreload(gamestart);
            }
        } else if (is_initialize_complete && get_low_power_state()) {
            // Bail out if we already on powersave profile
            if (cur_mode == ECO_MODE)
                continue;

            cur_mode = ECO_MODE;
            need_profile_checkup = false;
            log_zenith(LOG_INFO, "Applying ECO Mode");
            toast("Applying Eco Mode");
            run_profiler(ECO_MODE);

        } else {
            // Bail out if we already on normal profile
            if (cur_mode == BALANCED_PROFILE)
                continue;

            cur_mode = BALANCED_PROFILE;
            need_profile_checkup = false;
            log_zenith(LOG_INFO, "Applying Balanced profile");
            toast("Applying Balanced profile");
            if (!is_initialize_complete) {
                notify("AZenith is running successfully");
                is_initialize_complete = true;
            }
            run_profiler(BALANCED_PROFILE);

        }
    }

    return 0;
}
