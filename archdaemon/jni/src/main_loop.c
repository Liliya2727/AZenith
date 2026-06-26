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
#include <poll.h>
#include <pthread.h>
#include <sys/inotify.h>

/**
 * @brief GLOBAL VARIABLES
 */
char* gamestart = NULL;
char* active_app_name = NULL;
pid_t game_pids[MAX_GAME_PIDS] = {0};
int game_pid_count = 0;
bool is_restarting_renderer = false;
GameConfig opts;
SystemStateCache current_system_cache;
int java_lock_pipe[2];
bool java_daemon_died = false;
GameConfig* g_game_cache = NULL;
int g_game_cache_count = 0;
pthread_mutex_t cache_mutex = PTHREAD_MUTEX_INITIALIZER;

/**
 * @struct PreloadArgs
 * @brief Arguments passed to the GamePreload thread.
 */
typedef struct {
    char package[256];
} PreloadArgs;

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
    bool grace_period_active;
    int prev_screen_state;
    int saved_refresh_rate;
    int saved_zen_mode;
    int pid_retries;
    time_t screen_off_timer;
    ProfileMode cur_mode;
    char saved_renderer[PROP_VALUE_MAX];
    char last_freqoffset[PROP_VALUE_MAX];
    char prev_ai_state[16];
    const char* java_lock_path;
    char config_freqoffset[PROP_VALUE_MAX];
    char config_bypasspath[PROP_VALUE_MAX];
    int config_bypasschg;
    int config_bypasschgthreshold;
} DaemonContext;

/**
 * @brief PRIVATE FUNCTION PROTOTYPES
 */
static void* async_preload_worker(void* arg);
static void* java_lock_watcher_thread(void* arg);
static void init_daemon_context(DaemonContext* ctx);
static void verify_system_integrity(void);
static void wait_for_java_companion(DaemonContext* ctx);
static void load_initial_config_files(DaemonContext* ctx);
static int setup_inotify_watchers(void);
static bool process_inotify_events(int inotify_fd, DaemonContext* ctx, int timeout_ms);
static void handle_background_apps_event(void);
static void handle_dynamic_bypass(DaemonContext* ctx);
static void apply_performance_profile(DaemonContext* ctx);
static void apply_eco_profile(DaemonContext* ctx);
static void apply_balanced_profile(DaemonContext* ctx);
static void reload_gamelist_cache(DaemonContext* ctx);

/**
 * @brief Thread worker function to run GamePreload asynchronously.
 * @param arg Pointer to PreloadArgs structure.
 * @return NULL
 */
static void* async_preload_worker(void* arg) {
    PreloadArgs* args = (PreloadArgs*)arg;
    GamePreload(args->package);
    free(args);
    return NULL;
}

/**
 * @brief Initializes the daemon context with default values.
 * @param ctx Pointer to the DaemonContext structure.
 */
static void init_daemon_context(DaemonContext* ctx) {
    memset(ctx, 0, sizeof(DaemonContext));
    ctx->is_initialize_complete = false;
    ctx->dnd_enabled = false;
    ctx->need_profile_checkup = false;
    ctx->bypass_applied = false;
    ctx->has_applied_renderer = false;
    ctx->grace_period_active = false;
    ctx->prev_screen_state = -1;
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
 * @brief Safely frees the gamelist cache memory under mutex lock.
 */
void free_gamelist_cache(void) {
    pthread_mutex_lock(&cache_mutex);
    if (g_game_cache != NULL) {
        free(g_game_cache);
        g_game_cache = NULL;
    }
    g_game_cache_count = 0;
    pthread_mutex_unlock(&cache_mutex);
}

/**
 * @brief Loads the initial config file values from disk into context.
 * @param ctx Pointer to the DaemonContext structure.
 */
static void load_initial_config_files(DaemonContext* ctx) {
    FILE* fp;
    char val[16];

    if ((fp = fopen("/data/adb/.config/AZenith/freqoffset", "r"))) {
        if (fgets(ctx->config_freqoffset, sizeof(ctx->config_freqoffset), fp)) {
            trim_newline(ctx->config_freqoffset);
        }
        fclose(fp);
    } else {
        strcpy(ctx->config_freqoffset, "Disabled");
    }

    if ((fp = fopen("/data/adb/.config/AZenith/bypasschgconfig/bypasspath", "r"))) {
        if (fgets(ctx->config_bypasspath, sizeof(ctx->config_bypasspath), fp)) {
            trim_newline(ctx->config_bypasspath);
        }
        fclose(fp);
    }

    if ((fp = fopen("/data/adb/.config/AZenith/bypasschgconfig/bypasschg", "r"))) {
        if (fgets(val, sizeof(val), fp))
            ctx->config_bypasschg = atoi(val);
        fclose(fp);
    }

    if ((fp = fopen("/data/adb/.config/AZenith/bypasschgconfig/bypasschgthreshold", "r"))) {
        if (fgets(val, sizeof(val), fp))
            ctx->config_bypasschgthreshold = atoi(val);
        fclose(fp);
    }

    log_zenith(LOG_INFO, "Initial configs loaded. Freqoffset: [%s], Bypass: [%s]",
               ctx->config_freqoffset, ctx->config_bypasspath);
}

/**
 * @brief Reads GAMELIST (JSON) from disk and caches it in memory.
 * @param ctx Pointer to the DaemonContext structure.
 */
void reload_gamelist_cache(DaemonContext* ctx) {
    free_gamelist_cache();

    FILE* fp = fopen(GAMELIST, "r");
    if (!fp) {
        log_zenith(LOG_ERROR, "Failed to open GAMELIST for caching.");
        return;
    }

    fseek(fp, 0, SEEK_END);
    long size = ftell(fp);
    fseek(fp, 0, SEEK_SET);

    if (size <= 0) {
        fclose(fp);
        log_zenith(LOG_WARN, "GAMELIST is empty or invalid.");
        return;
    }

    char* buf = malloc(size + 1);
    if (!buf) {
        fclose(fp);
        log_zenith(LOG_FATAL, "OOM: Failed to allocate GAMELIST read buffer");
        return;
    }

    if (fread(buf, 1, size, fp) != (size_t)size) {
        fclose(fp);
        free(buf);
        log_zenith(LOG_ERROR, "Failed to read data from GAMELIST file");
        return;
    }
    fclose(fp);
    buf[size] = '\0';

    pthread_mutex_lock(&cache_mutex);

    int capacity = 16;
    g_game_cache = malloc(capacity * sizeof(GameConfig));
    if (!g_game_cache) {
        free(buf);
        pthread_mutex_unlock(&cache_mutex);
        log_zenith(LOG_FATAL, "OOM: Failed to allocate game cache array");
        return;
    }

    char* ptr = buf;
    while ((ptr = strstr(ptr, "\": {")) != NULL) {
        char* start_quote = ptr - 1;
        while (start_quote > buf && *start_quote != '"') {
            start_quote--;
        }

        if (*start_quote == '"') {
            if (g_game_cache_count >= capacity) {
                capacity *= 2;
                GameConfig* temp = realloc(g_game_cache, capacity * sizeof(GameConfig));
                if (!temp) {
                    log_zenith(LOG_FATAL, "OOM: Realloc failed while parsing gamelist");
                    free(g_game_cache);
                    g_game_cache = NULL;
                    g_game_cache_count = 0;
                    break;
                }
                g_game_cache = temp;
            }

            size_t pkg_len = ptr - (start_quote + 1);
            if (pkg_len >= sizeof(g_game_cache[g_game_cache_count].package)) {
                pkg_len = sizeof(g_game_cache[g_game_cache_count].package) - 1;
            }
            strncpy(g_game_cache[g_game_cache_count].package, start_quote + 1, pkg_len);
            g_game_cache[g_game_cache_count].package[pkg_len] = '\0';

            char* next_block = strstr(ptr + 4, "\": {");
            char* p;

            p = strstr(ptr, "\"perf_lite_mode\":");
            if (p && (!next_block || p < next_block)) {
                extract_string_value(g_game_cache[g_game_cache_count].perf_lite_mode, p,
                                     sizeof(g_game_cache[g_game_cache_count].perf_lite_mode));
            } else
                strcpy(g_game_cache[g_game_cache_count].perf_lite_mode, "default");

            p = strstr(ptr, "\"dnd_on_gaming\":");
            if (p && (!next_block || p < next_block)) {
                extract_string_value(g_game_cache[g_game_cache_count].dnd_on_gaming, p,
                                     sizeof(g_game_cache[g_game_cache_count].dnd_on_gaming));
            } else
                strcpy(g_game_cache[g_game_cache_count].dnd_on_gaming, "default");

            p = strstr(ptr, "\"app_priority\":");
            if (p && (!next_block || p < next_block)) {
                extract_string_value(g_game_cache[g_game_cache_count].app_priority, p,
                                     sizeof(g_game_cache[g_game_cache_count].app_priority));
            } else
                strcpy(g_game_cache[g_game_cache_count].app_priority, "default");

            p = strstr(ptr, "\"game_preload\":");
            if (p && (!next_block || p < next_block)) {
                extract_string_value(g_game_cache[g_game_cache_count].game_preload, p,
                                     sizeof(g_game_cache[g_game_cache_count].game_preload));
            } else
                strcpy(g_game_cache[g_game_cache_count].game_preload, "default");

            p = strstr(ptr, "\"refresh_rate\":");
            if (p && (!next_block || p < next_block)) {
                extract_string_value(g_game_cache[g_game_cache_count].refresh_rate, p,
                                     sizeof(g_game_cache[g_game_cache_count].refresh_rate));
            } else
                strcpy(g_game_cache[g_game_cache_count].refresh_rate, "default");

            p = strstr(ptr, "\"renderer\":");
            if (p && (!next_block || p < next_block)) {
                extract_string_value(g_game_cache[g_game_cache_count].renderer, p,
                                     sizeof(g_game_cache[g_game_cache_count].renderer));
            } else
                strcpy(g_game_cache[g_game_cache_count].renderer, "default");

            g_game_cache_count++;
        }
        ptr += 4;
    }

    free(buf);
    pthread_mutex_unlock(&cache_mutex);

    if (!ctx->is_initialize_complete) {
        log_zenith(LOG_INFO,
                   "Gamelist in-memory cache loaded successfully. Total: %d games registered.",
                   g_game_cache_count);
    }
}

/**
 * @brief Background thread to monitor Java daemon liveness via fcntl blocking.
 * @param arg Pointer to lock path string.
 * @return NULL
 */
static void* java_lock_watcher_thread(void* arg) {
    const char* lock_path = (const char*)arg;
    int fd = open(lock_path, O_RDWR | O_CREAT, 0600);

    if (fd >= 0) {
        struct flock fl;
        memset(&fl, 0, sizeof(fl));
        fl.l_type = F_WRLCK;
        fl.l_whence = SEEK_SET;

        if (fcntl(fd, F_SETLKW, &fl) != -1) {
            fl.l_type = F_UNLCK;
            fcntl(fd, F_SETLK, &fl);
        }
        close(fd);
    }

    char signal_byte = '1';
    write(java_lock_pipe[1], &signal_byte, 1);
    return NULL;
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
        fprintf(stderr, "\033[31mFATAL ERROR:\033[0m /system/bin/dumpsys was tampered by kill "
                        "logger module.\n");
        log_zenith(LOG_FATAL, "/system/bin/dumpsys was tampered by kill logger module");
        notify("Daemon Error", "Please remove your stupid kill logger module.", false, 0);
        exit(EXIT_FAILURE);
    }

    if (access(GAMELIST, F_OK) != 0) {
        fprintf(stderr, "\033[31mFATAL ERROR:\033[0m Unable to access Gamelist, either has been "
                        "removed or moved.\n");
        log_zenith(LOG_FATAL, "Critical file not found (%s)", GAMELIST);
        exit(EXIT_FAILURE);
    }

    is_kanged();
    check_module_version();
}

/**
 * @brief Waits for the Java companion daemon to acquire its lock file.
 * @param ctx Pointer to the DaemonContext structure.
 */
static void wait_for_java_companion(DaemonContext* ctx) {
    log_zenith(LOG_INFO, "Waiting for Java companion daemon to initialize...");
    int java_check_retries = 0;
    const int MAX_JAVA_RETRIES = 120;

    while (!is_java_lock_held(ctx->java_lock_path)) {
        if (++java_check_retries > MAX_JAVA_RETRIES) {
            log_zenith(LOG_FATAL, "Java companion daemon absent after %d checks, exiting",
                       MAX_JAVA_RETRIES);
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
 * @return File descriptor for inotify, or -1 on failure.
 */
static int setup_inotify_watchers(void) {
    int fd = inotify_init1(IN_NONBLOCK);
    if (fd < 0)
        return -1;

    struct WatchTarget {
        const char* path;
        uint32_t mask;
    } targets[] = {
        {"/data/adb/.config/AZenith/", IN_MODIFY | IN_CREATE | IN_MOVED_TO},
        {"/data/adb/.config/AZenith/API/", IN_MODIFY | IN_CREATE | IN_MOVED_TO},
        {"/data/adb/.config/AZenith/gamelist/",
         IN_MODIFY | IN_CLOSE_WRITE | IN_MOVED_TO | IN_CREATE},
        {"/data/adb/.config/AZenith/bypasschgconfig/", IN_MODIFY | IN_CREATE | IN_MOVED_TO},
        {"/data/adb/modules/AZenith/", IN_MODIFY | IN_CREATE | IN_MOVED_TO | IN_DELETE}};

    for (size_t i = 0; i < sizeof(targets) / sizeof(targets[0]); i++) {
        inotify_add_watch(fd, targets[i].path, targets[i].mask);
    }
    return fd;
}

/**
 * @brief Processes PID adjustments when background_apps event is triggered.
 */
static void handle_background_apps_event(void) {
    if (!gamestart)
        return;

    pid_t new_pids[MAX_GAME_PIDS];
    int max_track_pids = 2;
    int new_count = get_pids_of(gamestart, new_pids, max_track_pids);

    if (new_count == 0 && game_pid_count > 0) {
        struct stat st;
        if (stat("/data/adb/.config/AZenith/background_apps", &st) == 0 && st.st_size == 0) {
            new_count = game_pid_count;
            for (int i = 0; i < game_pid_count; i++) {
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
                    found = true;
                    break;
                }
            }
            if (!found) {
                pids_changed = true;
                break;
            }
        }
    }

    if (pids_changed) {
        if (new_count > 0) {
            log_zenith(LOG_INFO, "Tracking %d PID(s) for %s", new_count,
                       active_app_name ? active_app_name : gamestart);
        } else {
            log_zenith(LOG_INFO, "Game %s PIDs updated. Found %d active processes.",
                       active_app_name ? active_app_name : gamestart, new_count);
        }

        game_pid_count = new_count;

        for (int i = 0; i < new_count; i++) {
            game_pids[i] = new_pids[i];

            if (IS_TRUE(opts.app_priority)) {
                set_priority(game_pids[i]);
            } else if (!IS_FALSE(opts.app_priority)) {
                char val[PROP_VALUE_MAX] = {0};
                if (__system_property_get("persist.sys.azenithconf.iosched", val) > 0 &&
                    val[0] == '1') {
                    set_priority(game_pids[i]);
                }
            }
        }

        if (new_count == 0) {
            if (strcmp(current_system_cache.focused_app, gamestart) == 0 ||
                is_restarting_renderer) {
                log_zenith(LOG_INFO, "Game %s PIDs dropped (Restarting). Waiting to respawn...",
                           active_app_name ? active_app_name : gamestart);
            } else {
                log_zenith(LOG_INFO, "Game %s completely closed. Exiting performance mode...",
                           active_app_name ? active_app_name : gamestart);
                free(gamestart);
                gamestart = NULL;
                if (active_app_name) {
                    free(active_app_name);
                    active_app_name = NULL;
                }
            }
        }
    }
}

/**
 * @brief Reads events from inotify descriptor and routes actions.
 * @param inotify_fd Watcher file descriptor.
 * @param ctx Pointer to DaemonContext structure.
 * @param timeout_ms Poll timeout in milliseconds.
 * @return true if an exit command was received, false otherwise.
 */
static bool process_inotify_events(int inotify_fd, DaemonContext* ctx, int timeout_ms) {
    if (inotify_fd < 0)
        return false;

    struct pollfd pfds[2];
    pfds[0].fd = inotify_fd;
    pfds[0].events = POLLIN;
    pfds[1].fd = java_lock_pipe[0];
    pfds[1].events = POLLIN;

    int ret = poll(pfds, 2, timeout_ms);

    if (ret > 0) {
        if (pfds[1].revents & POLLIN) {
            java_daemon_died = true;
            return true;
        }

        if (pfds[0].revents & POLLIN) {
            char buf[4096] __attribute__((aligned(__alignof__(struct inotify_event))));
            ssize_t len;

            while ((len = read(inotify_fd, buf, sizeof(buf))) > 0) {
                for (char* ptr = buf; ptr < buf + len;) {
                    struct inotify_event* event = (struct inotify_event*)ptr;
                    if (event->len > 0) {

                        if (event->mask & (IN_CLOSE_WRITE | IN_MOVED_TO | IN_CREATE)) {
                            if (strstr(event->name, "azenithApplist.json")) {
                                usleep(50000);
                                reload_gamelist_cache(ctx);
                                ctx->need_profile_checkup = true;
                            }
                        }

                        if (strcmp(event->name, "app_status") == 0) {
                            read_app_status(&current_system_cache);
                            ctx->need_profile_checkup = true;
                        } else if (strcmp(event->name, "background_apps") == 0) {
                            handle_background_apps_event();
                            if (gamestart == NULL)
                                ctx->need_profile_checkup = true;
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
                        } else if (strcmp(event->name, "current_modes") == 0) {
                            FILE* fp_ai = fopen(DAEMON_MODES, "r");
                            if (fp_ai) {
                                char ai_state[16] = "0";
                                if (fgets(ai_state, sizeof(ai_state), fp_ai)) {
                                    trim_newline(ai_state);

                                    if (ctx->is_initialize_complete &&
                                        strcmp(ctx->prev_ai_state, ai_state) != 0) {
                                        log_zenith(LOG_INFO, "Dynamic profile toggled, Reapplying "
                                                             "Balanced Profiles");
                                        ctx->cur_mode = PERFCOMMON;
                                        apply_balanced_profile(ctx);
                                        strcpy(ctx->prev_ai_state, ai_state);

                                        if (strcmp(ai_state, "1") == 0) {
                                            if (gamestart) {
                                                free(gamestart);
                                                gamestart = NULL;
                                            }
                                            if (active_app_name) {
                                                free(active_app_name);
                                                active_app_name = NULL;
                                            }
                                            game_pid_count = 0;
                                            ctx->need_profile_checkup = true;
                                        }
                                    }
                                }
                                fclose(fp_ai);
                            }
                        } else if (strcmp(event->name, "update") == 0) {
                            log_zenith(LOG_INFO, "Module update detected, exiting.");
                            notify("Module Update",
                                   "Please reboot your device to complete module update.", false,
                                   0);
                            systemv("setprop persist.sys.azenith.service \"\"");
                            systemv("setprop persist.sys.azenith.state stopped");
                            return true;
                        } else if (strcmp(event->name, "remove") == 0) {
                            log_zenith(LOG_INFO, "Module is removed, exiting.");
                            notify("Module Removed",
                                   "Please reboot your device to complete module uninstallation.",
                                   false, 0);
                            return true;
                        } else if (strcmp(event->name, "module.prop") == 0) {
                            log_zenith(LOG_INFO, "module.prop modified...");
                            is_kanged();
                            check_module_version();
                        } else if (strcmp(event->name, "reboot") == 0) {
                            log_zenith(LOG_INFO, "Configuration updated, notify user to reboot");
                            notify("Daemon Info",
                                   "Configuration updated. Please reboot your device to take full "
                                   "effect.",
                                   false, 0);
                        }

                        if (strcmp(event->name, "freqoffset") == 0) {
                            char path[PATH_MAX];
                            snprintf(path, sizeof(path), "/data/adb/.config/AZenith/freqoffset");
                            FILE* fp = fopen(path, "r");
                            if (fp) {
                                if (fgets(ctx->config_freqoffset, sizeof(ctx->config_freqoffset),
                                          fp)) {
                                    trim_newline(ctx->config_freqoffset);
                                    log_zenith(LOG_INFO, "Inotify: freqoffset updated to [%s]",
                                               ctx->config_freqoffset);
                                }
                                fclose(fp);
                            }
                        } else if (strcmp(event->name, "bypasspath") == 0) {
                            char path[PATH_MAX];
                            snprintf(path, sizeof(path),
                                     "/data/adb/.config/AZenith/bypasschgconfig/bypasspath");
                            FILE* fp = fopen(path, "r");
                            if (fp) {
                                if (fgets(ctx->config_bypasspath, sizeof(ctx->config_bypasspath),
                                          fp)) {
                                    trim_newline(ctx->config_bypasspath);
                                }
                                fclose(fp);
                            }
                        } else if (strcmp(event->name, "bypasschg") == 0) {
                            char path[PATH_MAX], val[16] = {0};
                            snprintf(path, sizeof(path),
                                     "/data/adb/.config/AZenith/bypasschgconfig/bypasschg");
                            FILE* fp = fopen(path, "r");
                            if (fp) {
                                if (fgets(val, sizeof(val), fp))
                                    ctx->config_bypasschg = atoi(val);
                                fclose(fp);
                            }
                        } else if (strcmp(event->name, "bypasschgthreshold") == 0) {
                            char path[PATH_MAX], val[16] = {0};
                            snprintf(
                                path, sizeof(path),
                                "/data/adb/.config/AZenith/bypasschgconfig/bypasschgthreshold");
                            FILE* fp = fopen(path, "r");
                            if (fp) {
                                if (fgets(val, sizeof(val), fp))
                                    ctx->config_bypasschgthreshold = atoi(val);
                                fclose(fp);
                            }
                        }
                    }
                    ptr += sizeof(struct inotify_event) + event->len;
                }
            }
        }
    }
    return false;
}

/**
 * @brief Evaluates and applies dynamic battery bypass threshold logic.
 * @param ctx Pointer to DaemonContext structure.
 */
static void handle_dynamic_bypass(DaemonContext* ctx) {
    if (strcmp(ctx->config_bypasspath, "UNSUPPORTED") != 0 && strlen(ctx->config_bypasspath) > 0) {
        if (ctx->cur_mode == PERFORMANCE_PROFILE) {
            int threshold = ctx->config_bypasschgthreshold;
            int current_battery = current_system_cache.battery_level;
            int is_device_charging = current_system_cache.is_charging;

            if (current_battery >= 0 && ctx->config_bypasschg == 1 && is_device_charging) {
                if (current_battery >= threshold) {
                    if (read_current_ma() > 50) {
                        enable_bypass();
                        if (!ctx->bypass_applied) {
                            log_zenith(LOG_INFO,
                                       "Bypass Enabled: Battery (%d%%) >= Threshold (%d%%)",
                                       current_battery, threshold);
                            ctx->bypass_applied = true;
                        }
                    }
                } else if (ctx->bypass_applied) {
                    log_zenith(LOG_INFO,
                               "Bypass Disabled: Battery (%d%%) dropped below threshold (%d%%)",
                               current_battery, threshold);
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
 * @param ctx Pointer to DaemonContext structure.
 */
static void apply_performance_profile(DaemonContext* ctx) {
    toast("Applying Performance Profile");

    ctx->cur_mode = PERFORMANCE_PROFILE;
    ctx->need_profile_checkup = false;

    notify("Performance Profile", "Running at %s", false, 0,
           active_app_name ? active_app_name : gamestart);
    log_zenith(LOG_INFO, "Applying performance profile for %s",
               active_app_name ? active_app_name : gamestart);

    if (IS_TRUE(opts.perf_lite_mode)) {
        systemv("setprop persist.sys.azenithconf.litemode 1");
    } else if (IS_FALSE(opts.perf_lite_mode)) {
        systemv("setprop persist.sys.azenithconf.litemode 0");
    } else {
        char lite_prop[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.azenithconf.cpulimit", lite_prop);
        systemv("setprop persist.sys.azenithconf.litemode %s",
                (strcmp(lite_prop, "1") == 0) ? "1" : "0");
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
        if (ctx->saved_refresh_rate < 0) {
            ctx->saved_refresh_rate = get_current_refresh_rate();
        }
        apply_dynamic_refresh_rate(rr);
    }

    bool is_preload_active = false;
    if (!IS_FALSE(opts.game_preload)) {
        char preload_active[PROP_VALUE_MAX] = {0};
        if (__system_property_get("persist.sys.azenithconf.APreload", preload_active) > 0) {
            is_preload_active = (strcmp(preload_active, "1") == 0);
        }
    }

    if (IS_TRUE(opts.game_preload) || is_preload_active) {
        notify("AZenith Preload", "Preloading initiated for: %s", true, 10000,
               active_app_name ? active_app_name : gamestart);

        PreloadArgs* p_args = malloc(sizeof(PreloadArgs));
        if (p_args) {
            strncpy(p_args->package, gamestart, sizeof(p_args->package) - 1);
            p_args->package[sizeof(p_args->package) - 1] = '\0';

            pthread_t preload_thread;
            pthread_attr_t attr;
            pthread_attr_init(&attr);
            pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);

            if (pthread_create(&preload_thread, &attr, async_preload_worker, p_args) != 0) {
                log_zenith(LOG_ERROR, "Failed to spawn async preload thread");
                free(p_args);
            }
            pthread_attr_destroy(&attr);
        } else {
            log_zenith(LOG_ERROR, "Failed to allocate memory for preload arguments");
        }
    }
}

/**
 * @brief Reverts system to Endurance state (Eco Mode).
 * @param ctx Pointer to DaemonContext structure.
 */
static void apply_eco_profile(DaemonContext* ctx) {
    if (ctx->cur_mode == ECO_MODE)
        return;

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

        if (strlen(current_now) == 0)
            strcpy(current_now, "default");

        if (strcmp(current_now, ctx->saved_renderer) != 0) {
            log_zenith(LOG_INFO, "Restoring original system renderer: %s", ctx->saved_renderer);
            if (strcmp(ctx->saved_renderer, "default") == 0) {
                systemv("sys.azenith-utilityconf setrender default");
            } else {
                systemv("sys.azenith-utilityconf setrender %s", ctx->saved_renderer);
            }
        }
        memset(ctx->saved_renderer, 0, sizeof(ctx->saved_renderer));
    }

    EXECUTE("ECO Mode", run_profiler(ECO_MODE));
}

/**
 * @brief Reverts system to Optimal state (Balanced Mode).
 * @param ctx Pointer to DaemonContext structure.
 */
static void apply_balanced_profile(DaemonContext* ctx) {
    if (ctx->is_initialize_complete && ctx->cur_mode == BALANCED_PROFILE)
        return;

    toast("Applying Balanced Profile");

    ctx->cur_mode = BALANCED_PROFILE;
    ctx->need_profile_checkup = false;

    notify("Balanced Profile", "System is now at Optimal state", false, 0);
    log_zenith(LOG_INFO, "Applying balanced profile");

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

        if (strlen(current_now) == 0)
            strcpy(current_now, "default");

        if (strcmp(current_now, ctx->saved_renderer) != 0) {
            log_zenith(LOG_INFO, "Restoring original system renderer: %s", ctx->saved_renderer);
            if (strcmp(ctx->saved_renderer, "default") == 0) {
                systemv("sys.azenith-utilityconf setrender default");
            } else {
                systemv("sys.azenith-utilityconf setrender %s", ctx->saved_renderer);
            }
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
 * @return 0 on clean exit, 1 on initialization failure.
 */
int main_daemon(void) {
    verify_system_integrity();

    if (daemon(0, 0)) {
        log_zenith(LOG_FATAL, "Unable to daemonize service");
        systemv("setprop persist.sys.azenith.service \"\"");
        systemv("setprop persist.sys.azenith.state stopped");
        return 1;
    }

    signal(SIGINT, sighandler);
    signal(SIGTERM, sighandler);

    DaemonContext ctx;
    init_daemon_context(&ctx);

    wait_for_java_companion(&ctx);

    if (pipe(java_lock_pipe) != 0) {
        log_zenith(LOG_ERROR, "Failed to create java lock pipe");
    } else {
        pthread_t lock_thread;
        pthread_attr_t attr;
        pthread_attr_init(&attr);
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
        pthread_create(&lock_thread, &attr, java_lock_watcher_thread, (void*)ctx.java_lock_path);
        pthread_attr_destroy(&attr);
    }

    log_zenith(LOG_INFO, "Daemon started as PID %d", getpid());
    setspid();

    systemv("setprop persist.sys.rianixia.learning_enabled true");
    systemv("setprop persist.sys.azenith.state running");
    notify("Initializing...", "Starting AZenith service...", false, 0);

    systemv(
        "setprop persist.sys.rianixia.thermalcore-bigdata.path /data/adb/.config/AZenith/debug");
    runthermalcore();
    run_profiler(PERFCOMMON);
    systemv("sys.azenith-utilityconf FSTrim");

    FILE* fp_ai_init = fopen(DAEMON_MODES, "r");
    if (fp_ai_init) {
        if (fgets(ctx.prev_ai_state, sizeof(ctx.prev_ai_state), fp_ai_init))
            trim_newline(ctx.prev_ai_state);
        fclose(fp_ai_init);
    }

    int inotify_fd = setup_inotify_watchers();
    load_initial_config_files(&ctx);

    log_zenith(LOG_INFO, "Reading initial applist status...");
    read_app_status(&current_system_cache);
    reload_gamelist_cache(&ctx);

    log_zenith(LOG_INFO, "Successfully read applist. Starting main monitoring loop...");
    checkstate();
    is_kanged();
    check_module_version();

    ctx.need_profile_checkup = true;
    bool need_loop = true;

    /* Main Daemon Loop */
    while (1) {
        int poll_timeout = -1;
        if (need_loop) {
            poll_timeout = 0;
        } else if (ctx.grace_period_active) {
            double elapsed = difftime(time(NULL), ctx.screen_off_timer);
            if (elapsed < 10.0) {
                poll_timeout = (int)((10.0 - elapsed) * 1000);
            } else {
                poll_timeout = 0;
            }
        }

        bool should_exit = process_inotify_events(inotify_fd, &ctx, poll_timeout);
        need_loop = false;

        if (java_daemon_died) {
            log_zenith(
                LOG_FATAL,
                "Java daemon lock released, companion daemon exited or crashed, stopping daemon");
            notify("Daemon Error", "Java companion daemon crashed. Stopping AZenith.", false, 0);
            systemv("setprop persist.sys.azenith.service \"\"");
            systemv("setprop persist.sys.azenith.state stopped");
            break;
        }

        if (should_exit)
            break;

        int real_screen_state = get_screenstate(&current_system_cache);

        if (strcmp(ctx.config_freqoffset, "Disabled") == 0) {
            if (strcmp(ctx.last_freqoffset, "Disabled") != 0) {
                systemv("sys.azenith-profilesettings applyfreqbalance");
            }
        } else if (real_screen_state &&
                   (ctx.cur_mode == BALANCED_PROFILE || ctx.cur_mode == ECO_MODE)) {
            systemv("sys.azenith-profilesettings applyfreqbalance");
        }
        strcpy(ctx.last_freqoffset, ctx.config_freqoffset);

        bool pending_game_pid = (gamestart != NULL && game_pid_count == 0);

        handle_dynamic_bypass(&ctx);

        if (ctx.is_initialize_complete && strcmp(ctx.prev_ai_state, "0") == 0) {
            continue;
        }

        if (ctx.need_profile_checkup) {
            char* current_focused_game = get_gamestart(&opts, &current_system_cache);
            if (current_focused_game) {
                if (gamestart && strcmp(gamestart, current_focused_game) == 0) {
                    free(current_focused_game);
                    ctx.need_profile_checkup = false;
                } else {
                    if (gamestart)
                        free(gamestart);
                    if (active_app_name)
                        free(active_app_name);
                    gamestart = current_focused_game;
                    active_app_name = strdup(current_system_cache.app_name);
                    log_zenith(LOG_INFO, "New game detected: %s",
                               active_app_name ? active_app_name : gamestart);
                    game_pid_count = 0;
                    ctx.pid_retries = 0;
                    ctx.has_applied_renderer = false;
                    ctx.need_profile_checkup = true;
                }
            } else {
                ctx.need_profile_checkup = false;
            }
        }

        int effective_screen_state = real_screen_state;

        if (real_screen_state != ctx.prev_screen_state) {
            if (real_screen_state == 0) {
                if (ctx.cur_mode == PERFORMANCE_PROFILE) {
                    ctx.screen_off_timer = time(NULL);
                    ctx.grace_period_active = true;
                    log_zenith(LOG_INFO, "Screen OFF Event: Grace period started (10s)...");
                }
            } else {
                if (ctx.grace_period_active) {
                    log_zenith(
                        LOG_INFO,
                        "Screen ON Event: Grace period aborted. Keeping Performance Profile.");
                    ctx.grace_period_active = false;
                }
                ctx.screen_off_timer = 0;
                ctx.need_profile_checkup = true;
            }
            ctx.prev_screen_state = real_screen_state;
        }

        if (ctx.grace_period_active) {
            if (difftime(time(NULL), ctx.screen_off_timer) < 10.0) {
                effective_screen_state = 1;
            } else {
                log_zenith(LOG_INFO, "Grace period expired. Dropping Performance Profile.");
                ctx.grace_period_active = false;
                ctx.screen_off_timer = 0;
                effective_screen_state = 0;
                ctx.need_profile_checkup = true;
            }
        }

        if (ctx.is_initialize_complete && ctx.cur_mode != PERFORMANCE_PROFILE &&
            !pending_game_pid && !ctx.need_profile_checkup) {
            continue;
        }

        if (ctx.is_initialize_complete && gamestart && effective_screen_state) {
            if (!ctx.need_profile_checkup && ctx.cur_mode == PERFORMANCE_PROFILE &&
                ctx.has_applied_renderer && game_pid_count > 0) {
                continue;
            }

            bool is_renderer_changing = false;

            if (!ctx.has_applied_renderer) {
                is_restarting_renderer = true;
                if (!IS_DEFAULT(opts.renderer)) {
                    is_renderer_changing =
                        apply_smart_renderer(opts.renderer, gamestart, ctx.saved_renderer);
                }

                if (is_renderer_changing) {
                    log_zenith(LOG_INFO, "Changing renderer. Waiting for app to respawn...");
                    usleep(500000);
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
                        log_zenith(LOG_WARN, "Waiting for %s to spawn (Retry %d/5)...",
                                   active_app_name ? active_app_name : gamestart, ctx.pid_retries);
                        need_loop = true;
                        continue;
                    } else {
                        log_zenith(LOG_ERROR,
                                   "Unable to fetch any PIDs for %s after 5 retries. Dropping.",
                                   active_app_name ? active_app_name : gamestart);
                        free(gamestart);
                        gamestart = NULL;
                        if (active_app_name) {
                            free(active_app_name);
                            active_app_name = NULL;
                        }
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
                        if (__system_property_get("persist.sys.azenithconf.iosched", val) > 0 &&
                            val[0] == '1') {
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

    if (inotify_fd >= 0)
        close(inotify_fd);
    return 0;
}
