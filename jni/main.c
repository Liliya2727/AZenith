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

void print_help() {
    printf("AZenith Daemon CLI by @Zexshia\n");
    printf("Usage:\n");
    printf("  sys.azenith-service --run\n");
    printf("      Start daemon\n\n");

    printf("  sys.azenith-service --profile <1|2|3>\n");
    printf("      Apply manual profile\n");
    printf("      1 = Performance\n");
    printf("      2 = Balanced\n");
    printf("      3 = Eco Mode\n\n");

    printf("  sys.azenith-service --log <TAG> <LEVEL> <MESSAGE>\n");
    printf("      Write log through AZenith logging service\n");
    printf("      LEVEL = 0..4\n\n");
}

int handle_profile(int argc, char** argv) {
    if (argc < 3) {
        fprintf(stderr, "Missing profile number.\n");
        return 1;
    }

    char ai_state[PROP_VALUE_MAX] = {0};
    __system_property_get("persist.sys.azenithconf.AIenabled", ai_state);

    if (!strcmp(ai_state, "1")) {
        fprintf(stderr, "ERROR: Auto mode enabled. Manual profile blocked.\n");
        return 1;
    }

    const char* profile = argv[2];

    if (!strcmp(profile, "1")) {
        run_profiler(PERFORMANCE_PROFILE);
        printf("Performance profile applied.\n");
    } else if (!strcmp(profile, "2")) {
        run_profiler(BALANCED_PROFILE);
        printf("Balanced profile applied.\n");
    } else if (!strcmp(profile, "3")) {
        run_profiler(ECO_MODE);
        printf("Eco Mode applied.\n");
    } else {
        fprintf(stderr, "Invalid profile number.\n");
        return 1;
    }

    return 0;
}

int handle_log(int argc, char** argv) {
    if (argc < 5) {
        fprintf(stderr, "Usage: --log <TAG> <LEVEL> <MESSAGE>\n");
        return 1;
    }

    int level = atoi(argv[3]);
    if (level < LOG_DEBUG || level > LOG_FATAL) {
        fprintf(stderr, "Invalid log level (0..4)\n");
        return 1;
    }

    char message[1024] = {0};
    for (int i = 4; i < argc; i++) {
        strcat(message, argv[i]);
        if (i != argc - 1)
            strcat(message, " ");
    }

    external_log(level, argv[2], message);
    return 0;
}

/* ============================================================
   MAIN
   ============================================================ */

int main(int argc, char* argv[]) {

    if (getuid() != 0) {
        fprintf(stderr, "\033[31mERROR:\033[0m Please run this program as root\n");
        return 1;
    }

    if (argc == 1) {
        print_help();
        return 0;
    }

    if (!strcmp(argv[1], "--help") || !strcmp(argv[1], "-h")) {
        print_help();
        return 0;
    }

    if (!strcmp(argv[1], "--profile") || !strcmp(argv[1], "-p")) {
        return handle_profile(argc, argv);
    }

    if (!strcmp(argv[1], "--log") || !strcmp(argv[1], "-l")) {
        return handle_log(argc, argv);
    }

    if (!strcmp(argv[1], "--run") || !strcmp(argv[1], "-r")) {

        if (check_running_state() == 1) {
            fprintf(stderr, "\033[31mERROR:\033[0m Another instance of Daemon is already running!\n");
            return 1;
        }

        is_kanged();

        if (daemon(0, 0)) {
            log_zenith(LOG_FATAL, "Unable to daemonize service");
            systemv("setprop persist.sys.azenith.service \"\"");
            systemv("setprop persist.sys.azenith.state stopped");
            return 1;
        }

        signal(SIGINT,  sighandler);
        signal(SIGTERM, sighandler);

        bool need_profile_checkup = false;
        MLBBState mlbb_is_running = MLBB_NOT_RUNNING;
        static bool is_initialize_complete = false;
        ProfileMode cur_mode = PERFCOMMON;

        systemv("rm -f /data/adb/.config/AZenith/debug/AZenith.log");
        systemv("rm -f /data/adb/.config/AZenith/debug/AZenithVerbose.log");
        systemv("rm -f /data/adb/.config/AZenith/preload/AZenithPR.log");
        
        log_zenith(LOG_INFO, "Daemon started as PID %d", getpid());
        setspid();

        systemv("setprop persist.sys.rianixia.learning_enabled true");
        systemv("setprop persist.sys.azenith.state running");
        notify("Initializing...");

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

    fprintf(stderr, "Unknown option: %s\nUse --help\n", argv[1]);
    return 1;
}