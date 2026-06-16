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

/**
 * GLOBAL VARIABLES
 */
char* gamestart = NULL;
char* active_app_name = NULL;
pid_t game_pids[MAX_GAME_PIDS] = {0};
int game_pid_count = 0;
bool is_restarting_renderer = false;
GameOptions opts;
SystemStateCache current_system_cache;

/**
 * @struct DaemonContext
 * @brief Manages the internal state and lifecycle variables of the main daemon.
 */
typedef struct {
    bool is_initialize_complete;
    bool dnd_enabled;
    bool need_profile_checkup;
    bool bypass_applied;
    bool has_applied_renderer;
    int saved_refresh_rate;
    int saved_zen_mode;
    int pid_retries;
    time_t screen_off_timer;
    ProfileMode cur_mode;
    char saved_renderer[PROP_VALUE_MAX];
    char last_freqoffset[PROP_VALUE_MAX];
    char prev_ai_state[16];
    const char* java_lock_path;
} DaemonContext;

/**
 * PRIVATE FUNCTION PROTOTYPES
 */
static void init_daemon_context(DaemonContext* ctx);
static void verify_system_integrity(void);
static void wait_for_java_companion(DaemonContext* ctx);
static int setup_inotify_watchers(void);
static bool process_inotify_events(int inotify_fd, DaemonContext* ctx);
static void handle_background_apps_event(void);
static void handle_dynamic_bypass(DaemonContext* ctx);
static void apply_performance_profile(DaemonContext* ctx);
static void apply_eco_profile(DaemonContext* ctx);
static void apply_balanced_profile(DaemonContext* ctx);

/**
 * @brief Initializes the daemon context with default values.
 * * @param ctx Pointer to the DaemonContext structure.
 */
static void init_daemon_context(DaemonContext* ctx) {
    memset(ctx, 0, sizeof(DaemonContext));
    ctx->is_initialize_complete = false;
    ctx->dnd_enabled = false;
    ctx->need_profile_checkup = false;
    ctx->bypass_applied = false;
    ctx->has_applied_renderer = false;
    ctx->saved_refresh_rate = -1;
    ctx->saved_zen_mode = -1;
    ctx->pid_retries = 0;
    ctx->screen_off_timer = 0;
    ctx->cur_mode = PERFCOMMON;
    strcpy(ctx->last_freqoffset, "Initial");
    strcpy(ctx->prev_ai_state, "0");
    ctx->java_lock_path = "/data/adb/.config/AZenith/java.lock";
}

/**
 * @brief Validates crucial system files and module integrity before startup.
 */
static void verify_system_integrity(void) {
    if (check_running_state() != 0) {
        fprintf(stderr, "\033[31mERROR:\033[0m Daemon is already running!\n");
        exit(EXIT_FAILURE);
    }

    systemv("touch %s", PROFILE_MODE_APP);
    systemv("touch %s", GAME_INFO_APP);

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
}

/**
 * @brief Waits for the Java companion daemon to acquire its lock file.
 */
static void wait_for_java_companion(DaemonContext* ctx) {
    log_zenith(LOG_INFO, "Waiting for Java companion daemon to initialize...");
    int java_check_retries = 0;
    const int MAX_JAVA_RETRIES = 120;

    while (!is_java_lock_held(ctx->java_lock_path)) {
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
}

/**
 * @brief Sets up inotify watchers for relevant module directories.
 * * @return File descriptor for inotify, or -1 on failure.
 */
static int setup_inotify_watchers(void) {
    int fd = inotify_init1(IN_NONBLOCK);
    if (fd < 0) {
        log_zenith(LOG_ERROR, "Failed to initialize inotify");
        return -1;
    }

    log_zenith(LOG_INFO, "Initializing inotify watchers...");
    struct WatchTarget {
        const char* path;
        uint32_t mask;
    } targets[] = {
        {"/data/adb/.config/AZenith/", IN_MODIFY | IN_CREATE | IN_MOVED_TO},
        {"/data/adb/.config/AZenith/API/", IN_MODIFY | IN_CREATE | IN_MOVED_TO},
        {"/data/adb/modules/AZenith/", IN_MODIFY | IN_CREATE | IN_MOVED_TO | IN_DELETE}
    };

    for (size_t i = 0; i < sizeof(targets) / sizeof(targets[0]); i++) {
        int wd = inotify_add_watch(fd, targets[i].path, targets[i].mask);
        if (wd >= 0) {
            log_zenith(LOG_INFO, "Added watch for directory: %s", targets[i].path);
        } else {
            log_zenith(LOG_WARN, "Failed to add watch for directory: %s", targets[i].path);
        }
    }
    return fd;
}

/**
 * @brief Processes PID adjustments when background_apps event is triggered.
 */
static void handle_background_apps_event(void) {
    if (!gamestart) return;

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
            if (strcmp(current_system_cache.focused_app, gamestart) == 0 || is_restarting_renderer) {
                log_zenith(LOG_INFO, "Game %s PIDs dropped (Restarting). Waiting to respawn...", active_app_name ? active_app_name : gamestart);
            } else {
                log_zenith(LOG_INFO, "Game %s completely closed. Exiting performance mode...", active_app_name ? active_app_name : gamestart);
                free(gamestart);
                gamestart = NULL;
                if (active_app_name) { 
                    free(active_app_name); 
                    active_app_name = NULL; 
                }
                // Checkup needs to be triggered in outer scope, handled via global or context
            }
        }
    }
}

/**
 * @brief Reads events from inotify descriptor and routes actions.
 * * @return true if an exit command was received, false otherwise.
 */
static bool process_inotify_events(int inotify_fd, DaemonContext* ctx) {
    if (inotify_fd < 0) return false;

    struct pollfd pfd = { inotify_fd, POLLIN, 0 };
    int ret = poll(&pfd, 1, LOOP_INTERVAL_MS);
    
    if (ret > 0 && (pfd.revents & POLLIN)) {
        char buf[4096] __attribute__((aligned(__alignof__(struct inotify_event))));
        ssize_t len;
        
        while ((len = read(inotify_fd, buf, sizeof(buf))) > 0) {
            for (char *ptr = buf; ptr < buf + len; ) {
                struct inotify_event *event = (struct inotify_event *)ptr;
                if (event->len > 0) {
                    if (strcmp(event->name, "app_status") == 0) {
                        read_app_status(&current_system_cache);
                    } else if (strcmp(event->name, "background_apps") == 0) {
                        handle_background_apps_event();
                        if (gamestart == NULL) ctx->need_profile_checkup = true;
                    } else if (strcmp(event->name, "current_profile") == 0) {
                        FILE* fp_prof = fopen(PROFILE_MODE, "r");
                        if (fp_prof) {
                            char prof_val[8] = {0};
                            if (fgets(prof_val, sizeof(prof_val), fp_prof)) {
                                trim_newline(prof_val);
                                int ext_profile = atoi(prof_val);
                                ctx->cur_mode = (ProfileMode)ext_profile;
                            }
                            fclose(fp_prof);
                        }
                    } else if (strcmp(event->name, "update") == 0) {
                        log_zenith(LOG_INFO, "Module update detected, exiting.");
                        notify("Module Update", "Please reboot your device to complete module update.", false, 0);
                        systemv("setprop persist.sys.azenith.service \"\"");
                        systemv("setprop persist.sys.azenith.state stopped");
                        return true;
                    } else if (strcmp(event->name, "remove") == 0) {
                        log_zenith(LOG_INFO, "Module is removed, exiting.");
                        notify("Module Removed", "Please reboot your device to complete module uninstallation.", false, 0);
                        return true;
                    } else if (strcmp(event->name, "reboot") == 0) {
                        log_zenith(LOG_INFO, "Configuration updated, notify user to reboot");
                        notify("Daemon Info", "Configuration updated. Please reboot your device to take full effect.", false, 0);
                    }
                }
                ptr += sizeof(struct inotify_event) + event->len;
            }
        }
    } else {          
        usleep(LOOP_INTERVAL_MS * 1000);
    }
    return false;
}

/**
 * @brief Evaluates and applies dynamic battery bypass threshold logic.
 */
static void handle_dynamic_bypass(DaemonContext* ctx) {
    char bypass_path_prop[PROP_VALUE_MAX] = {0};
    __system_property_get("persist.sys.azenithconf.bypasspath", bypass_path_prop);
    
    if (strcmp(bypass_path_prop, "UNSUPPORTED") != 0 && strlen(bypass_path_prop) > 0) {
        if (ctx->cur_mode == PERFORMANCE_PROFILE) {
            char bypass_toggle[PROP_VALUE_MAX] = {0};
            char threshold_prop[PROP_VALUE_MAX] = {0};
            
            __system_property_get("persist.sys.azenithconf.bypasschg", bypass_toggle);
            __system_property_get("persist.sys.azenithconf.bypasschgthreshold", threshold_prop);
            
            int threshold = (strlen(threshold_prop) > 0) ? atoi(threshold_prop) : 0;
            int current_battery = get_battery_level();

            if (strcmp(bypass_toggle, "1") == 0 && is_charging()) {
                if (current_battery >= threshold) {
                    if (read_current_ma() > 10) {
                        enable_bypass();
                        if (!ctx->bypass_applied) {
                            log_zenith(LOG_INFO, "Bypass Enabled: Battery (%d%%) >= Threshold (%d%%)", current_battery, threshold);
                            ctx->bypass_applied = true;
                        }
                    }
                } else if (ctx->bypass_applied) {
                    log_zenith(LOG_INFO, "Bypass Disabled: Battery (%d%%) dropped below threshold (%d%%)", current_battery, threshold);
                    disable_bypass();
                    ctx->bypass_applied = false;
                }
            } else if (ctx->bypass_applied) {
                disable_bypass();
                ctx->bypass_applied = false;
            }
        } else if (ctx->bypass_applied) {
            disable_bypass();
            ctx->bypass_applied = false;
        }
    }
}

/**
 * @brief Applies system tuning parameters specifically for Performance Mode.
 */
static void apply_performance_profile(DaemonContext* ctx) {
    toast("Applying Performance Profile");

    ctx->cur_mode = PERFORMANCE_PROFILE;
    ctx->need_profile_checkup = false;
    
    notify("Performance Profile", "Running at %s", false, 0, active_app_name ? active_app_name : gamestart);
    log_zenith(LOG_INFO, "Applying performance profile for %s", active_app_name ? active_app_name : gamestart);

    if (IS_TRUE(opts.perf_lite_mode)) {
        systemv("setprop persist.sys.azenithconf.litemode 1");
    } else if (IS_FALSE(opts.perf_lite_mode)) {
        systemv("setprop persist.sys.azenithconf.litemode 0");
    } else {
        char lite_prop[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.azenithconf.cpulimit", lite_prop);
        systemv("setprop persist.sys.azenithconf.litemode %s", (strcmp(lite_prop, "1") == 0) ? "1" : "0");
    }
             
    if (ctx->saved_zen_mode < 0) {
        ctx->saved_zen_mode = current_system_cache.zen_mode;
    }

    if (IS_TRUE(opts.dnd_on_gaming)) {
        if (ctx->saved_zen_mode == 0) {
            systemv("sys.azenith-utilityconf enableDND");
        }
        ctx->dnd_enabled = true;
    } else if (!IS_FALSE(opts.dnd_on_gaming)) {
        char dnd_state[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.azenithconf.dnd", dnd_state);
        if (strcmp(dnd_state, "1") == 0) {
            if (ctx->saved_zen_mode == 0) {
                systemv("sys.azenith-utilityconf enableDND");
            }
            ctx->dnd_enabled = true;
        }
    }
    
    EXECUTE("Performance Profile", run_profiler(PERFORMANCE_PROFILE));

    if (!IS_DEFAULT(opts.refresh_rate)) {
        int rr = atoi(opts.refresh_rate);
        if (rr >= 60 && rr <= 144) {
            if (ctx->saved_refresh_rate < 0) {
                ctx->saved_refresh_rate = get_current_refresh_rate();
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
}

/**
 * @brief Reverts system to Endurace state (Eco Mode).
 */
static void apply_eco_profile(DaemonContext* ctx) {
    if (ctx->cur_mode == ECO_MODE) return;
    
    toast("Applying Eco Mode");

    ctx->cur_mode = ECO_MODE;                
    ctx->need_profile_checkup = false;

    
    notify("ECO Mode", "System is now at Endurance state", false, 0);
    log_zenith(LOG_INFO, "Applying ECO Mode");

    if (ctx->saved_refresh_rate > 0) {
        apply_dynamic_refresh_rate(ctx->saved_refresh_rate);
        ctx->saved_refresh_rate = -1;
    }   

    if (ctx->dnd_enabled) {
        if (ctx->saved_zen_mode == 0) {
            systemv("sys.azenith-utilityconf disableDND");
        }
        ctx->dnd_enabled = false;
    }
    ctx->saved_zen_mode = -1;

    if (strlen(ctx->saved_renderer) > 0) {
        char current_now[PROP_VALUE_MAX] = {0};
        __system_property_get("debug.hwui.renderer", current_now);
        if (strcmp(current_now, ctx->saved_renderer) != 0) {
            log_zenith(LOG_INFO, "Restoring original system renderer: %s", ctx->saved_renderer);
            systemv("sys.azenith-utilityconf setrender %s", ctx->saved_renderer);
        }
        memset(ctx->saved_renderer, 0, sizeof(ctx->saved_renderer));
    }
    
    EXECUTE("ECO Mode", run_profiler(ECO_MODE));
}

/**
 * @brief Reverts system to Optimal state (Balanced Mode).
 */
static void apply_balanced_profile(DaemonContext* ctx) {
    if (ctx->cur_mode == BALANCED_PROFILE) return;
    
    toast("Applying Balanced profile");

    ctx->cur_mode = BALANCED_PROFILE;               
    ctx->need_profile_checkup = false;
    
    
    notify("Balanced Profile", "System is now at Optimal state", false, 0);
    log_zenith(LOG_INFO, "Applying Balanced profile");

    if (ctx->saved_refresh_rate > 0) {
        apply_dynamic_refresh_rate(ctx->saved_refresh_rate);
        ctx->saved_refresh_rate = -1;
    }

    if (ctx->dnd_enabled) {
        if (ctx->saved_zen_mode == 0) {
            systemv("sys.azenith-utilityconf disableDND");
        }
        ctx->dnd_enabled = false;
    }
    ctx->saved_zen_mode = -1; 
    
    if (strlen(ctx->saved_renderer) > 0) {
        char current_now[PROP_VALUE_MAX] = {0};
        __system_property_get("debug.hwui.renderer", current_now);
        if (strcmp(current_now, ctx->saved_renderer) != 0) {
            log_zenith(LOG_INFO, "Restoring original system renderer: %s", ctx->saved_renderer);
            systemv("sys.azenith-utilityconf setrender %s", ctx->saved_renderer);
        }
        memset(ctx->saved_renderer, 0, sizeof(ctx->saved_renderer));
    }
    
    EXECUTE("Balanced Profile", run_profiler(BALANCED_PROFILE));

    if (!ctx->is_initialize_complete) {
        notify("Daemon Info", "AZenith is running successfully", false, 60000);
        ctx->is_initialize_complete = true;
    }

}

/**
 * @brief Main entry point for the daemon logic.
 */
int main_daemon(void) {
    verify_system_integrity();

    if (daemon(0, 0)) {
        log_zenith(LOG_FATAL, "Unable to daemonize service");
        systemv("setprop persist.sys.azenith.service \"\"");
        systemv("setprop persist.sys.azenith.state stopped");
        return 1;
    }
                    
    signal(SIGINT,  sighandler);
    signal(SIGTERM, sighandler);

    DaemonContext ctx;
    init_daemon_context(&ctx);

    wait_for_java_companion(&ctx);

    log_zenith(LOG_INFO, "Daemon started as PID %d", getpid());
    setspid();

    systemv("setprop persist.sys.rianixia.learning_enabled true");
    systemv("setprop persist.sys.azenith.state running");
    notify("Initializing...", "Starting AZenith service...", false, 0);

    systemv("setprop persist.sys.rianixia.thermalcore-bigdata.path /data/adb/.config/AZenith/debug");
    runthermalcore();
    run_profiler(PERFCOMMON);

    FILE* fp_ai_init = fopen(DAEMON_MODES, "r");
    if (fp_ai_init) {
        if (fgets(ctx.prev_ai_state, sizeof(ctx.prev_ai_state), fp_ai_init)) trim_newline(ctx.prev_ai_state);
        fclose(fp_ai_init);
    }

    int inotify_fd = setup_inotify_watchers();
    
    log_zenith(LOG_INFO, "Reading initial applist status...");
    read_app_status(&current_system_cache);
    log_zenith(LOG_INFO, "Successfully read applist. Starting main monitoring loop...");

    /* Main Daemon Loop */
    while (1) {
        if (!is_java_lock_held(ctx.java_lock_path)) {
            log_zenith(LOG_FATAL, "Java daemon lock released, companion daemon exited or crashed, stopping daemon");
            notify("Daemon Error", "Java companion daemon crashed. Stopping AZenith.", false, 0);
            systemv("setprop persist.sys.azenith.service \"\"");
            systemv("setprop persist.sys.azenith.state stopped");
            break; 
        }

        bool should_exit = process_inotify_events(inotify_fd, &ctx);
        if (should_exit) break;
        
        int real_screen_state = get_screenstate(&current_system_cache); 
        char freqoffset[PROP_VALUE_MAX] = {0};
        
        __system_property_get("persist.sys.azenithconf.freqoffset", freqoffset);            
        if (strcmp(freqoffset, "Disabled") == 0) {
            if (strcmp(ctx.last_freqoffset, "Disabled") != 0) {
                systemv("sys.azenith-profilesettings applyfreqbalance");
            }
        } else if (real_screen_state && (ctx.cur_mode == BALANCED_PROFILE || ctx.cur_mode == ECO_MODE)) {
            systemv("sys.azenith-profilesettings applyfreqbalance");
        }
        strcpy(ctx.last_freqoffset, freqoffset);
        
        runtask();
        checkstate();
        is_kanged();
        check_module_version();
        
        // Fast paths and evaluations
        bool pending_game_pid = (gamestart != NULL && game_pid_count == 0);
        struct pollfd pfd_check = { inotify_fd, POLLIN, 0 };
        bool has_event = (poll(&pfd_check, 1, 0) > 0);

        if (ctx.is_initialize_complete && ctx.cur_mode != PERFORMANCE_PROFILE && !has_event && !pending_game_pid) {
            continue;
        }

        handle_dynamic_bypass(&ctx);

        char ai_state[16] = "0";
        FILE* fp_ai = fopen(DAEMON_MODES, "r");
        if (fp_ai) {
            if (fgets(ai_state, sizeof(ai_state), fp_ai)) trim_newline(ai_state);
            fclose(fp_ai);
        }

        if (ctx.is_initialize_complete) {
            if (strcmp(ctx.prev_ai_state, ai_state) != 0) {
                log_zenith(LOG_INFO, "Dynamic profile toggled, Reapplying Balanced Profiles");
                ctx.cur_mode = PERFCOMMON;
                apply_balanced_profile(&ctx);
                strcpy(ctx.prev_ai_state, ai_state);
                if (strcmp(ai_state, "1") == 0) {
                    // Clear gamestart if user still ingame by any chance
                    if (gamestart) {
                        free(gamestart);
                        gamestart = NULL;
                    }
                    if (active_app_name) {
                        free(active_app_name);
                        active_app_name = NULL;
                    }
                    game_pid_count = 0;
                    ctx.need_profile_checkup = true;
                }
            }
            if (strcmp(ai_state, "0") == 0) continue;
        }

        // Focused Game Resolution
        char* current_focused_game = get_gamestart(&opts, &current_system_cache);
        if (current_focused_game) {
            if (!gamestart || strcmp(gamestart, current_focused_game) != 0) {
                if (gamestart) free(gamestart);
                if (active_app_name) free(active_app_name);
                
                gamestart = current_focused_game;
                active_app_name = strdup(current_system_cache.app_name); 
                
                log_zenith(LOG_INFO, "New game detected: %s", active_app_name ? active_app_name : gamestart);
                game_pid_count = 0;
                ctx.pid_retries = 0;
                ctx.has_applied_renderer = false;
                ctx.need_profile_checkup = true;
            } else {
                free(current_focused_game);
            }
        }

        // Final State Machine Routing
        int effective_screen_state = real_screen_state;

        if (ctx.cur_mode == PERFORMANCE_PROFILE) {
            if (real_screen_state == 0) {
                if (ctx.screen_off_timer == 0) {
                    ctx.screen_off_timer = time(NULL);
                    log_zenith(LOG_INFO, "Screen OFF detected: Grace period started (10s)...");
                }
                if (difftime(time(NULL), ctx.screen_off_timer) < 10.0) {
                    effective_screen_state = 1; 
                } else if (ctx.screen_off_timer != -1) {
                    log_zenith(LOG_INFO, "Screen OFF Grace period ended. Dropping Performance Profile.");
                    ctx.screen_off_timer = -1;
                }
            } else {
                if (ctx.screen_off_timer != 0 && ctx.screen_off_timer != -1) {
                    log_zenith(LOG_INFO, "Screen ON detected within grace period. Keeping Performance Profile.");
                }
                ctx.screen_off_timer = 0; 
            }
        } else {
            ctx.screen_off_timer = 0;
        }

        if (ctx.is_initialize_complete && gamestart && effective_screen_state) {
            if (!ctx.need_profile_checkup && ctx.cur_mode == PERFORMANCE_PROFILE) continue;
            
            bool is_renderer_changing = false;
            
            if (!ctx.has_applied_renderer) {
                is_restarting_renderer = true;
                
                if (!IS_DEFAULT(opts.renderer)) {
                    is_renderer_changing = apply_smart_renderer(opts.renderer, gamestart, ctx.saved_renderer);
                } else {
                    char global_renderer[PROP_VALUE_MAX] = {0};
                    __system_property_get("debug.hwui.renderer", global_renderer);
                    if (strcmp(global_renderer, "default") != 0) {
                        is_renderer_changing = apply_smart_renderer(global_renderer, gamestart, ctx.saved_renderer);
                    }
                }

                if (is_renderer_changing) {
                    log_zenith(LOG_INFO, "Changing renderer. Waiting for app to respawn...");
                    sleep(5);
                    game_pid_count = 0;
                    ctx.pid_retries = 0;
                }
                
                is_restarting_renderer = false;
                ctx.has_applied_renderer = true;
            }

            if (game_pid_count == 0) [[clang::unlikely]] {
                if (strcmp(current_system_cache.focused_app, gamestart) == 0) {
                    game_pid_count = get_pids_of(gamestart, game_pids, MAX_GAME_PIDS);
                }

                if (game_pid_count == 0) {
                    if (ctx.pid_retries < 5) {
                        ctx.pid_retries++;
                        log_zenith(LOG_WARN, "Waiting for %s to spawn (Retry %d/5)...", active_app_name ? active_app_name : gamestart, ctx.pid_retries);
                        continue; 
                    } else {
                        log_zenith(LOG_ERROR, "Unable to fetch any PIDs for %s after 5 retries. Dropping.", active_app_name ? active_app_name : gamestart);
                        free(gamestart);
                        gamestart = NULL;
                        if (active_app_name) { free(active_app_name); active_app_name = NULL; }
                        ctx.pid_retries = 0;
                        ctx.need_profile_checkup = true;
                        continue;
                    }
                }
                                            
                ctx.pid_retries = 0;
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
            
            apply_performance_profile(&ctx);

        } else if (ctx.is_initialize_complete && get_low_power_state(&current_system_cache)) {
            apply_eco_profile(&ctx);
        } else {
            apply_balanced_profile(&ctx);
        }
    }

    if (inotify_fd >= 0) close(inotify_fd);
    return 0;
}
