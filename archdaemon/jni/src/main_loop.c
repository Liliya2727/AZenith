/*
 * Copyright (C) 2026-2027 Zexshia
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
#include <libgen.h>
#include <sys/inotify.h>
#include <poll.h>

char* gamestart = NULL;
char* active_app_name = NULL;
pid_t game_pids[MAX_GAME_PIDS] = {0};
int game_pid_count = 0;
bool is_restarting_renderer = false;
GameOptions opts;

int main_daemon(void) {
    if (check_running_state() != 0) {
        fprintf(stderr, "\033[31mERROR:\033[0m Daemon is already running!\n");
        return 1;
    }

    systemv("rm -f /data/adb/.config/AZenith/debug/AZenith.log");
    systemv("rm -f /data/adb/.config/AZenith/debug/AZenithVerbose.log");
    systemv("rm -f /data/adb/.config/AZenith/preload/AZenithPR.log");        
    systemv("su -c \"am broadcast -a zx.azenith.ACTION_MANAGE -n zx.azenith/.receiver.ZenithReceiver --ez clearall true >/dev/null 2>&1\"");
    systemv("touch %s", PROFILE_MODE_APP);

    if (is_file_empty("/system/bin/dumpsys") == 1) {
        fprintf(stderr, "\033[31mFATAL ERROR:\033[0m /system/bin/dumpsys was tampered by kill logger module.\n");
        log_zenith(LOG_FATAL, "/system/bin/dumpsys was tampered by kill logger module");
        notify("Daemon Error", "Please remove your stupid kill logger module.", false, 0);
        exit(EXIT_FAILURE);
    }
    
    if (access(GAMELIST, F_OK) != 0) {
        fprintf(stderr, "\033[31mFATAL ERROR:\033[0m Unable to access Gamelist, either has been removed or moved.\n");
        log_zenith(LOG_FATAL, "Critical file not found (%s)", GAMELIST);
        exit(EXIT_FAILURE);
    }

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

    log_zenith(LOG_INFO, "Waiting for Java companion daemon to initialize...");
    int java_check_retries = 0;
    const int MAX_JAVA_RETRIES = 120;
    const char* java_lock_path = "/data/adb/.config/AZenith/java.lock";

    while (!is_java_lock_held(java_lock_path)) {
        if (++java_check_retries > MAX_JAVA_RETRIES) {
            log_zenith(LOG_FATAL, "Java companion daemon absent after %d checks, exiting", MAX_JAVA_RETRIES);
            notify("Daemon Error", "Java companion daemon crashed or failed to start.", false, 0);
            systemv("setprop persist.sys.azenith.service \"\"");
            systemv("setprop persist.sys.azenith.state stopped");
            exit(EXIT_FAILURE);
        }
        if (java_check_retries <= 1) {
            log_zenith(LOG_WARN, "Java companion daemon lock not held, waiting...");
        }
        sleep(1);
    }
    log_zenith(LOG_INFO, "Java companion daemon detected. Proceeding.");

    bool need_profile_checkup = false;
    bool bypass_applied = false; 
    static bool is_initialize_complete = false;
    static bool dnd_enabled = false;
    static int saved_refresh_rate = -1;
    static int saved_zen_mode = -1; 
    static time_t screen_off_timer = 0;
    static int pid_retries = 0;
    static bool has_applied_renderer = false; 
    
    ProfileMode cur_mode = PERFCOMMON;
    
    static char saved_renderer[PROP_VALUE_MAX] = {0};
    static char last_freqoffset[PROP_VALUE_MAX] = "Initial"; 
    
    log_zenith(LOG_INFO, "Daemon started as PID %d", getpid());
    setspid();

    systemv("setprop persist.sys.rianixia.learning_enabled true");
    systemv("setprop persist.sys.azenith.state running");
    notify("Initializing...", "Starting AZenith service...", false, 0);

    systemv("setprop persist.sys.rianixia.thermalcore-bigdata.path /data/adb/.config/AZenith/debug");
    runthermalcore();
    run_profiler(PERFCOMMON);

    char prev_ai_state[16] = "0";
    FILE* fp_ai_init = fopen(DAEMON_MODES, "r");
    if (fp_ai_init) {
        if (fgets(prev_ai_state, sizeof(prev_ai_state), fp_ai_init)) trim_newline(prev_ai_state);
        fclose(fp_ai_init);
    }

    int inotify_fd = inotify_init1(IN_NONBLOCK);
    if (inotify_fd >= 0) {
        inotify_add_watch(inotify_fd, "/data/adb/.config/AZenith/", IN_MODIFY | IN_CREATE | IN_MOVED_TO);
        inotify_add_watch(inotify_fd, "/data/adb/.config/AZenith/API/", IN_MODIFY | IN_CREATE | IN_MOVED_TO);
        inotify_add_watch(inotify_fd, "/data/adb/modules/AZenith/", IN_MODIFY | IN_CREATE | IN_MOVED_TO | IN_DELETE);
    }
    
    read_app_status();

    while (1) {
        if (!is_java_lock_held(java_lock_path)) {
            log_zenith(LOG_FATAL, "Java daemon lock released, companion daemon exited or crashed, stopping daemon");
            notify("Daemon Error", "Java companion daemon crashed. Stopping AZenith.", false, 0);
            systemv("setprop persist.sys.azenith.service \"\"");
            systemv("setprop persist.sys.azenith.state stopped");
            break; 
        }

        bool should_exit = false;
        bool has_event = false;

        if (inotify_fd >= 0) {
            struct pollfd pfd = { inotify_fd, POLLIN, 0 };
            int ret = poll(&pfd, 1, LOOP_INTERVAL_MS);
            
            if (ret > 0 && (pfd.revents & POLLIN)) {
                has_event = true;
                char buf[4096] __attribute__((aligned(__alignof__(struct inotify_event))));
                ssize_t len;
                while ((len = read(inotify_fd, buf, sizeof(buf))) > 0) {
                    for (char *ptr = buf; ptr < buf + len; ) {
                        struct inotify_event *event = (struct inotify_event *)ptr;
                        if (event->len > 0) {
                            if (strcmp(event->name, "app_status") == 0) {
                                read_app_status();
                            } else if (strcmp(event->name, "background_apps") == 0) {
                                if (gamestart) {
                                    pid_t new_pids[MAX_GAME_PIDS];
                                    
                                    int max_track_pids = 2; 
                                    int new_count = get_pids_of(gamestart, new_pids, max_track_pids);
                                    
                                    if (new_count == 0 && game_pid_count > 0) {
                                        struct stat st;
                                        if (stat("/data/adb/.config/AZenith/background_apps", &st) == 0 && st.st_size == 0) {
                                            new_count = game_pid_count;
                                            for(int i = 0; i < game_pid_count; i++) {
                                                new_pids[i] = game_pids[i];
                                            }
                                        }
                                    }
                                    
                                    bool pids_changed = false;
                                    if (new_count != game_pid_count) {
                                        pids_changed = true;
                                    } else {
                                        for (int i = 0; i < new_count; i++) {
                                            bool found = false;
                                            for (int j = 0; j < game_pid_count; j++) {
                                                if (new_pids[i] == game_pids[j]) {
                                                    found = true; break;
                                                }
                                            }
                                            if (!found) { pids_changed = true; break; }
                                        }
                                    }

                                    if (pids_changed) {
                                        if (new_count > 0) {
                                        
// ...


                                            log_zenith(LOG_INFO, "Tracking %d PID(s) for %s", new_count, active_app_name ? active_app_name : gamestart);
                                        } else {
                                            log_zenith(LOG_INFO, "Game %s PIDs updated. Found %d active processes.", active_app_name ? active_app_name : gamestart, new_count);
                                        }
                                        game_pid_count = new_count;
                                        
                                        for (int i = 0; i < new_count; i++) {
                                            game_pids[i] = new_pids[i];

                                            if (IS_TRUE(opts.app_priority)) {
                                                set_priority(game_pids[i]);
                                            } else if (!IS_FALSE(opts.app_priority)) {
                                                char val[PROP_VALUE_MAX] = {0};
                                                if (__system_property_get("persist.sys.azenithconf.iosched", val) > 0 && val[0] == '1') {
                                                    set_priority(game_pids[i]);
                                                }
                                            }
                                        }

                                        if (new_count == 0) {
                                            if (strcmp(cached_focused_app, gamestart) == 0 || is_restarting_renderer) {
                                                log_zenith(LOG_INFO, "Game %s PIDs dropped (Restarting). Waiting to respawn...", active_app_name ? active_app_name : gamestart);
                                            } else {
                                                log_zenith(LOG_INFO, "Game %s completely closed. Exiting performance mode...", active_app_name ? active_app_name : gamestart);
                                                free(gamestart);
                                                gamestart = NULL;
                                                if (active_app_name) { free(active_app_name); active_app_name = NULL; } // Bersihkan memory nama
                                                need_profile_checkup = true;
                                            }
                                        }
                                    }
                                }
                            } else if (strcmp(event->name, "update") == 0) {
                                log_zenith(LOG_INFO, "Module update detected, exiting.");
                                notify("Module Update", "Please reboot your device to complete module update.", false, 0);
                                systemv("setprop persist.sys.azenith.service \"\"");
                                systemv("setprop persist.sys.azenith.state stopped");
                                should_exit = true;
                            } else if (strcmp(event->name, "remove") == 0) {
                                log_zenith(LOG_INFO, "Module is removed, exiting.");
                                notify("Module Removed", "Please reboot your device to complete module uninstallation.", false, 0);
                                should_exit = true;
                            } else if (strcmp(event->name, "reboot") == 0) {
                                // Deteksi flag reboot yang dibuat (misal dari hasil restore tweak di App)
                                log_zenith(LOG_INFO, "Configuration updated, notify user to reboot");
                                notify("Daemon Info", "Configuration updated. Please reboot your device to take full effect.", false, 0);
                            }
                        }
                        ptr += sizeof(struct inotify_event) + event->len;
                    }
                }
            }
        } else {          
            usleep(LOOP_INTERVAL_MS * 1000);
        }
        
        if (should_exit) break;
        
        int real_screen_state = get_screenstate(); 
        char freqoffset[PROP_VALUE_MAX] = {0};
        
        __system_property_get("persist.sys.azenithconf.freqoffset", freqoffset);            
        if (strcmp(freqoffset, "Disabled") == 0) {
            if (strcmp(last_freqoffset, "Disabled") != 0) {
                systemv("sys.azenith-profilesettings applyfreqbalance");
            }
        } else if (real_screen_state && (cur_mode == BALANCED_PROFILE || cur_mode == ECO_MODE)) {
            systemv("sys.azenith-profilesettings applyfreqbalance");
        }
        strcpy(last_freqoffset, freqoffset);
        
        runtask();
        checkstate();
        is_kanged();
        check_module_version();
        
        bool pending_game_pid = (gamestart != NULL && game_pid_count == 0);
        
        if (is_initialize_complete && cur_mode != PERFORMANCE_PROFILE && !has_event && !pending_game_pid) {
            continue;
        }

        char bypass_path_prop[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.azenithconf.bypasspath", bypass_path_prop);
        
        if (strcmp(bypass_path_prop, "UNSUPPORTED") != 0 && strlen(bypass_path_prop) > 0) {
            if (cur_mode == PERFORMANCE_PROFILE) {
                char bypass_toggle[PROP_VALUE_MAX] = {0};
                char threshold_prop[PROP_VALUE_MAX] = {0};
                
                __system_property_get("persist.sys.azenithconf.bypasschg", bypass_toggle);
                __system_property_get("persist.sys.azenithconf.bypasschgthreshold", threshold_prop);
                
                int threshold = (strlen(threshold_prop) > 0) ? atoi(threshold_prop) : 0;
                int current_battery = get_battery_level();

                if (strcmp(bypass_toggle, "1") == 0 && is_charging()) {
                    if (current_battery >= threshold) {
                        if (read_current_ma() > 10) {
                            enable_bypass_logic();
                            if (!bypass_applied) {
                                log_zenith(LOG_INFO, "Bypass Enabled: Battery (%d%%) >= Threshold (%d%%)", current_battery, threshold);
                                bypass_applied = true;
                            }
                        }
                    } else if (bypass_applied) {
                        log_zenith(LOG_INFO, "Bypass Disabled: Battery (%d%%) dropped below threshold (%d%%)", current_battery, threshold);
                        disable_bypass();
                        bypass_applied = false;
                    }
                } else if (bypass_applied) {
                    disable_bypass();
                    bypass_applied = false;
                }
            } else if (bypass_applied) {
                disable_bypass();
                bypass_applied = false;
            }
        }

        char ai_state[16] = "0";
        FILE* fp_ai = fopen(DAEMON_MODES, "r");
        if (fp_ai) {
            if (fgets(ai_state, sizeof(ai_state), fp_ai)) trim_newline(ai_state);
            fclose(fp_ai);
        }

        if (is_initialize_complete) {
            if (strcmp(prev_ai_state, ai_state) != 0) {
                log_zenith(LOG_INFO, "Dynamic profile toggled, Reapplying Balanced Profiles");
                toast("Applying Balanced Profile");
                cur_mode = BALANCED_PROFILE;
                run_profiler(BALANCED_PROFILE);
                notify("Balanced Profile", "System is now at Optimal state", false, 0);
                strcpy(prev_ai_state, ai_state);
            }
            if (strcmp(ai_state, "0") == 0) continue;
        }

        char* current_focused_game = get_gamestart(&opts);
        if (current_focused_game) {
            if (!gamestart || strcmp(gamestart, current_focused_game) != 0) {
                if (gamestart) free(gamestart);
                if (active_app_name) free(active_app_name);
                
                gamestart = current_focused_game;
                active_app_name = strdup(cached_app_name); 
                
                log_zenith(LOG_INFO, "New game detected: %s", active_app_name ? active_app_name : gamestart);
                game_pid_count = 0;
                pid_retries = 0;
                has_applied_renderer = false;
                need_profile_checkup = true;
            } else {
                free(current_focused_game);
            }
        }


  
        int effective_screen_state = real_screen_state;

        if (cur_mode == PERFORMANCE_PROFILE) {
            if (real_screen_state == 0) {
                if (screen_off_timer == 0) {
                    screen_off_timer = time(NULL);
                    log_zenith(LOG_INFO, "Screen OFF detected: Grace period started (10s)...");
                }
                if (difftime(time(NULL), screen_off_timer) < 10.0) {
                    effective_screen_state = 1; 
                } else if (screen_off_timer != -1) {
                    log_zenith(LOG_INFO, "Screen OFF Grace period ended. Dropping Performance Profile.");
                    screen_off_timer = -1;
                }
            } else {
                if (screen_off_timer != 0 && screen_off_timer != -1) {
                    log_zenith(LOG_INFO, "Screen ON detected within grace period. Keeping Performance Profile.");
                }
                screen_off_timer = 0; 
            }
        } else {
            screen_off_timer = 0;
        }

        if (is_initialize_complete && gamestart && effective_screen_state) {
            if (!need_profile_checkup && cur_mode == PERFORMANCE_PROFILE) continue;
            
            bool is_renderer_changing = false;
            
            if (!has_applied_renderer) {
                is_restarting_renderer = true;
                
                if (!IS_DEFAULT(opts.renderer)) {
                    is_renderer_changing = apply_smart_renderer(opts.renderer, gamestart, saved_renderer);
                } else {
                    char global_renderer[PROP_VALUE_MAX] = {0};
                    __system_property_get("debug.hwui.renderer", global_renderer);
                    if (strcmp(global_renderer, "default") != 0) {
                        is_renderer_changing = apply_smart_renderer(global_renderer, gamestart, saved_renderer);
                    }
                }

                if (is_renderer_changing) {
                    log_zenith(LOG_INFO, "Changing renderer. Waiting for app to respawn...");
                    sleep(5);
                    game_pid_count = 0;
                    pid_retries = 0;
                }
                
                is_restarting_renderer = false;
                has_applied_renderer = true;
            }


            if (game_pid_count == 0) [[clang::unlikely]] {
                if (strcmp(cached_focused_app, gamestart) == 0) {
                    game_pid_count = get_pids_of(gamestart, game_pids, MAX_GAME_PIDS);
                }

                if (game_pid_count == 0) {
                    if (pid_retries < 5) {
                        pid_retries++;
                        log_zenith(LOG_WARN, "Waiting for %s to spawn (Retry %d/5)...", active_app_name ? active_app_name : gamestart, pid_retries);
                        continue; 
                    } else {
                        log_zenith(LOG_ERROR, "Unable to fetch any PIDs for %s after 5 retries. Dropping.", active_app_name ? active_app_name : gamestart);
                        free(gamestart);
                        gamestart = NULL;
                        if (active_app_name) { free(active_app_name); active_app_name = NULL; }
                        pid_retries = 0;
                        need_profile_checkup = true;
                        continue;
                    }
                }
                                            

                pid_retries = 0;

                for (int i = 0; i < game_pid_count; i++) {
                    if (IS_TRUE(opts.app_priority)) {
                        set_priority(game_pids[i]);
                    } else if (!IS_FALSE(opts.app_priority)) {
                        char val[PROP_VALUE_MAX] = {0};
                        if (__system_property_get("persist.sys.azenithconf.iosched", val) > 0 && val[0] == '1') {
                            set_priority(game_pids[i]);
                        }
                    }
                }
            }

            cur_mode = PERFORMANCE_PROFILE;
            need_profile_checkup = false;
            
            run_profiler(PERFORMANCE_PROFILE);
            notify("Performance Profile", "Running at %s", false, 0, active_app_name ? active_app_name : gamestart);
            log_zenith(LOG_INFO, "Applying performance profile for %s", active_app_name ? active_app_name : gamestart);
            
            toast("Applying Performance Profile");

            if (IS_TRUE(opts.perf_lite_mode)) {
                systemv("setprop persist.sys.azenithconf.litemode 1");
            } else if (IS_FALSE(opts.perf_lite_mode)) {
                systemv("setprop persist.sys.azenithconf.litemode 0");
            } else {
                char lite_prop[PROP_VALUE_MAX] = {0};
                __system_property_get("persist.sys.azenithconf.cpulimit", lite_prop);
                systemv("setprop persist.sys.azenithconf.litemode %s", (strcmp(lite_prop, "1") == 0) ? "1" : "0");
            }
                     
            if (saved_zen_mode < 0) {
                saved_zen_mode = cached_zen_mode;
            }

            if (IS_TRUE(opts.dnd_on_gaming)) {
                if (saved_zen_mode == 0) {
                    systemv("sys.azenith-utilityconf enableDND");
                }
                dnd_enabled = true;
            } else if (!IS_FALSE(opts.dnd_on_gaming)) {
                char dnd_state[PROP_VALUE_MAX] = {0};
                __system_property_get("persist.sys.azenithconf.dnd", dnd_state);
                if (strcmp(dnd_state, "1") == 0) {
                    if (saved_zen_mode == 0) {
                        systemv("sys.azenith-utilityconf enableDND");
                    }
                    dnd_enabled = true;
                }
            }

                            
            if (!IS_DEFAULT(opts.refresh_rate)) {
                int rr = atoi(opts.refresh_rate);
                if (rr >= 60 && rr <= 144) {
                    if (saved_refresh_rate < 0) {
                        saved_refresh_rate = get_current_refresh_rate();
                        
                    }
                    apply_dynamic_refresh_rate(rr);
                }
            }
                  
            if (IS_TRUE(opts.game_preload)) {
                notify("AZenith Preload", "Preloading Complete at : %s", true, 10000, active_app_name ? active_app_name : gamestart);

                GamePreload(gamestart);
            } else if (!IS_FALSE(opts.game_preload)) {
                char preload_active[PROP_VALUE_MAX] = {0};
                __system_property_get("persist.sys.azenithconf.APreload", preload_active);
                if (strcmp(preload_active, "1") == 0) {
                    notify("AZenith Preload", "Preloading Complete at : %s", true, 10000, active_app_name ? active_app_name : gamestart);

                    GamePreload(gamestart);
                }
            }

        } else if (is_initialize_complete && get_low_power_state()) {
            if (cur_mode == ECO_MODE) continue;

            cur_mode = ECO_MODE;                
            need_profile_checkup = false;
            
            run_profiler(ECO_MODE);
            notify("ECO Mode", "System is now at Endurance state", false, 0);
            
            log_zenith(LOG_INFO, "Applying ECO Mode");
            toast("Applying Eco Mode");

            if (saved_refresh_rate > 0) {
                apply_dynamic_refresh_rate(saved_refresh_rate);
                saved_refresh_rate = -1;
            }   

            if (dnd_enabled) {
                // Hanya matikan DND jika state asalnya memang mati (0)
                if (saved_zen_mode == 0) {
                    systemv("sys.azenith-utilityconf disableDND");
                }
                dnd_enabled = false;
            }
            // Reset saved_zen_mode untuk game berikutnya
            saved_zen_mode = -1; 


            if (strlen(saved_renderer) > 0) {
                char current_now[PROP_VALUE_MAX] = {0};
                __system_property_get("debug.hwui.renderer", current_now);
                if (strcmp(current_now, saved_renderer) != 0) {
                    log_zenith(LOG_INFO, "Restoring original system renderer: %s", saved_renderer);
                    systemv("sys.azenith-utilityconf setrender %s", saved_renderer);
                }
                memset(saved_renderer, 0, sizeof(saved_renderer));
            }

        } else {
            if (cur_mode == BALANCED_PROFILE) continue;

            cur_mode = BALANCED_PROFILE;               
            need_profile_checkup = false;
            
            run_profiler(BALANCED_PROFILE);
            notify("Balanced Profile", "System is now at Optimal state", false, 0);
            
            log_zenith(LOG_INFO, "Applying Balanced profile");
            toast("Applying Balanced profile");

            if (saved_refresh_rate > 0) {
                apply_dynamic_refresh_rate(saved_refresh_rate);
                saved_refresh_rate = -1;
            }

            if (dnd_enabled) {
                // Hanya matikan DND jika state asalnya memang mati (0)
                if (saved_zen_mode == 0) {
                    systemv("sys.azenith-utilityconf disableDND");
                }
                dnd_enabled = false;
            }
            // Reset saved_zen_mode untuk game berikutnya
            saved_zen_mode = -1; 


            if (!is_initialize_complete) {
                notify("Daemon Info", "AZenith is running successfully", false, 60000);
                is_initialize_complete = true;
            }

            if (strlen(saved_renderer) > 0) {
                char current_now[PROP_VALUE_MAX] = {0};
                __system_property_get("debug.hwui.renderer", current_now);
                if (strcmp(current_now, saved_renderer) != 0) {
                    log_zenith(LOG_INFO, "Restoring original system renderer: %s", saved_renderer);
                    systemv("sys.azenith-utilityconf setrender %s", saved_renderer);
                }
                memset(saved_renderer, 0, sizeof(saved_renderer));
            }

        }
    }
    return 0;
}
