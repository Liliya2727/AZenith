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
GameOptions opts;

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

    if (!strcmp(argv[1], "--run") || !strcmp(argv[1], "-r")) {

        if (check_running_state() == 1) {
            fprintf(stderr,
                "\033[31mERROR:\033[0m Daemon is already running!\n"
            );
            return 1;
        }

        systemv("rm -f /data/adb/.config/AZenith/debug/AZenith.log");
        systemv("rm -f /data/adb/.config/AZenith/debug/AZenithVerbose.log");
        systemv("rm -f /data/adb/.config/AZenith/preload/AZenithPR.log");

               
        // Sanity check for dumpsys
        if (access("/system/bin/dumpsys", F_OK) != 0) {
            fprintf(stderr, "\033[31mFATAL ERROR:\033[0m /system/bin/dumpsys: inaccessible or not found\n");
            log_zenith(LOG_FATAL, "/system/bin/dumpsys: inaccessible or not found");
            notify("Something wrong happening in the daemon, please check module log.");
            exit(EXIT_FAILURE);
        }
    
        if (is_file_empty("/system/bin/dumpsys") == 1) {
            fprintf(stderr, "\033[31mFATAL ERROR:\033[0m /system/bin/dumpsys was tampered by kill logger module.\n");
            log_zenith(LOG_FATAL, "/system/bin/dumpsys was tampered by kill logger module");
            notify("Please remove your stupid kill logger module.");
            exit(EXIT_FAILURE);
        }
        
        // Handle missing Gamelist
        if (access(GAMELIST, F_OK) != 0) {
            fprintf(stderr, "\033[31mFATAL ERROR:\033[0m Unable to access Gamelist, either has been removed or moved.\n");
            log_zenith(LOG_FATAL, "Critical file not found (%s)", GAMELIST);
            exit(EXIT_FAILURE);
        }

        // Check Module Integrity
        is_kanged();
        check_module_version();

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
        static bool dnd_enabled = false;
        static int saved_refresh_rate = -1;
        
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
            runtask();
            // Check Module Integrity
            is_kanged();
            check_module_version();
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
                gamestart = get_gamestart(&opts);                
            } else if (game_pid != 0 && kill(game_pid, 0) == -1) [[clang::unlikely]] {
                log_zenith(LOG_INFO, "Game %s exited, resetting profile...", gamestart);
                game_pid = 0;
                free(gamestart);
                gamestart = get_gamestart(&opts);
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

                if (IS_TRUE(opts.perf_lite_mode)) {
                    systemv("setprop persist.sys.azenithconf.litemode 1");
                } else if (IS_FALSE(opts.perf_lite_mode)) {
                    systemv("setprop persist.sys.azenithconf.litemode 0");
                } else {
                    char lite_prop[PROP_VALUE_MAX] = {0};
                    __system_property_get("persist.sys.azenithconf.cpulimit", lite_prop);
                    if (strcmp(lite_prop, "1") == 0) {
                        systemv("setprop persist.sys.azenithconf.litemode 1");                 
                    } else {
                        systemv("setprop persist.sys.azenithconf.litemode 0");
                    }
                }
                
                if (strcmp(opts.renderer, "vulkan") == 0) {
                    systemv("sys.azenith-utilityconf setrender skiavk");
                } else if (strcmp(opts.renderer, "skiagl") == 0) {
                    systemv("sys.azenith-utilityconf setrender skiagl");
                } else {
                    // Do Nothing
                }               
                
                if (IS_TRUE(opts.app_priority)) {
                    set_priority(game_pid);
                } else if (IS_FALSE(opts.app_priority)) {
                    // do nothing
                } else {
                    char val[PROP_VALUE_MAX] = {0};
                    if (__system_property_get("persist.sys.azenithconf.iosched", val) > 0) {
                        if (val[0] == '1') {
                        set_priority(game_pid);
                        }
                    }
                }
                                                
                if (IS_TRUE(opts.dnd_on_gaming)) {                   
                    systemv("sys.azenith-utilityconf enableDND");
                    dnd_enabled = true;
                } else if (IS_FALSE(opts.dnd_on_gaming)) {
                    // do nothing
                } else {
                    char dnd_state[PROP_VALUE_MAX] = {0};
                    __system_property_get("persist.sys.azenithconf.dnd", dnd_state);
                    if (strcmp(dnd_state, "1") == 0) {
                        systemv("sys.azenith-utilityconf enableDND");
                        dnd_enabled = true;
                    }
                }
                                
                if (!IS_DEFAULT(opts.refresh_rate)) {
                    int rr = atoi(opts.refresh_rate);
                
                    if (rr >= 60 && rr <= 144) {
                
                        if (saved_refresh_rate < 0) {
                            saved_refresh_rate = get_current_refresh_rate();
                            log_zenith(LOG_INFO, "Saved refresh rate: %d", saved_refresh_rate);
                        }
                
                        systemv("sys.azenith-utilityconf setrefreshrates %d", rr);
                    }
                
                } else {
                    // do nothing
                }
                                                
                run_profiler(PERFORMANCE_PROFILE);
                      
                if (IS_TRUE(opts.game_preload)) {
                    GamePreload(gamestart);
                } else if (IS_FALSE(opts.game_preload)) {
                    // do nothing
                } else {
                    char preload_active[PROP_VALUE_MAX] = {0};
                    __system_property_get("persist.sys.azenithconf.APreload", preload_active);
                    if (strcmp(preload_active, "1") == 0) {
                        GamePreload(gamestart);
                    }
                }
            } else if (is_initialize_complete && get_low_power_state()) {
                // Bail out if we already on powersave profile
                if (cur_mode == ECO_MODE)
                    continue;
    
                cur_mode = ECO_MODE;
                need_profile_checkup = false;
                log_zenith(LOG_INFO, "Applying ECO Mode");
                toast("Applying Eco Mode");
                char renderer[PROP_VALUE_MAX] = {0};
                __system_property_get("persist.sys.azenithconf.renderer", renderer);                
                if (strcmp(renderer, "vulkan") == 0) {
                    systemv("sys.azenith-utilityconf setrender skiavk");
                    
                } else {
                    systemv("sys.azenith-utilityconf setrender skiagl");
                    
                }           
                if (saved_refresh_rate > 0) {
                    systemv("sys.azenith-utilityconf setrefreshrates %d", saved_refresh_rate);
                    saved_refresh_rate = -1;
                }   
                if (dnd_enabled) {
                    systemv("sys.azenith-utilityconf disableDND");
                    dnd_enabled = false;
                }
                run_profiler(ECO_MODE);    
            } else {
                // Bail out if we already on normal profile
                if (cur_mode == BALANCED_PROFILE)
                    continue;
    
                cur_mode = BALANCED_PROFILE;
                need_profile_checkup = false;
                log_zenith(LOG_INFO, "Applying Balanced profile");
                toast("Applying Balanced profile");  
                char renderer[PROP_VALUE_MAX] = {0};
                __system_property_get("persist.sys.azenithconf.renderer", renderer);                
                if (strcmp(renderer, "vulkan") == 0) {
                    systemv("sys.azenith-utilityconf setrender skiavk");
                    
                } else {
                    systemv("sys.azenith-utilityconf setrender skiagl");
                    
                }              
                if (saved_refresh_rate > 0) {
                    systemv("sys.azenith-utilityconf setrefreshrates %d", saved_refresh_rate);
                    saved_refresh_rate = -1;
                }
                if (dnd_enabled) {
                    systemv("sys.azenith-utilityconf disableDND");
                    dnd_enabled = false;
                }
                if (!is_initialize_complete) {
                    notify("AZenith is running successfully");
                    is_initialize_complete = true;
                }
                run_profiler(BALANCED_PROFILE);
    
            }
        }

        return 0;
    }    

    if (!require_daemon_running()) {
        return 1;
    }

    if (!strcmp(argv[1], "--profile") || !strcmp(argv[1], "-p")) {
        return handle_profile(argc, argv);
    }

    if (!strcmp(argv[1], "--log") || !strcmp(argv[1], "-l")) {
        return handle_log(argc, argv);
    }

    if (!strcmp(argv[1], "--verboselog") || !strcmp(argv[1], "-vl")) {
        return handle_verboselog(argc, argv);
    }
    
    if (!strcmp(argv[1], "--version") || !strcmp(argv[1], "-V")) {
        printversion();
        return 0;
    }

    fprintf(stderr,
        "\033[31mERROR:\033[0m Unknown command: %s\n", argv[1]
    );
    return 1; 
}
