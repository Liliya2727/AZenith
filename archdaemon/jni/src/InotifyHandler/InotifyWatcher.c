/*
 * Copyright (C) 2024-2025 Zexshia
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

#include "AZenith.h"

/**
 * @brief Processes PID adjustments when background_apps event is triggered.
 */
static void handle_background_apps_event(DaemonContext* ctx) {
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
            log_zenith(LOG_INFO, "InotifyHandler: Tracking %d PID(s) for %s", new_count, active_app_name ? active_app_name : gamestart);
        } else {
            log_zenith(LOG_INFO, "InotifyHandler: Game %s PIDs updated. Found %d active processes.", active_app_name ? active_app_name : gamestart,
                       new_count);
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
                log_zenith(LOG_INFO, "InotifyHandler: Game %s PIDs dropped (Restarting). Waiting to respawn...",
                           active_app_name ? active_app_name : gamestart);
            } else {
                log_zenith(LOG_INFO, "InotifyHandler: Game %s completely closed. Exiting performance mode...",
                           active_app_name ? active_app_name : gamestart);
                restore_resolution_target(ctx, gamestart);
                free(gamestart);
                gamestart = NULL;
                if (active_app_name) {
                    free(active_app_name);
                    active_app_name = NULL;
                }
                ctx->fg_away_active = false;
                ctx->fg_away_timer = 0;
            }
        }
    }
}

/**
 * @brief Sets up inotify watchers for relevant module directories.
 * @return File descriptor for inotify, or -1 on failure.
 */
int setup_inotify_watchers(void) {
    int fd = inotify_init1(IN_NONBLOCK);
    if (fd < 0)
        return -1;

    struct WatchTarget {
        const char* path;
        uint32_t mask;
    } targets[] = {{"/data/adb/.config/AZenith/", IN_MODIFY | IN_CREATE | IN_MOVED_TO},
                   {"/data/adb/.config/AZenith/API/", IN_MODIFY | IN_CREATE | IN_MOVED_TO},
                   {"/data/adb/.config/AZenith/gamelist/", IN_MODIFY | IN_CLOSE_WRITE | IN_MOVED_TO | IN_CREATE},
                   {"/data/adb/.config/AZenith/bypasschgconfig/", IN_MODIFY | IN_CREATE | IN_MOVED_TO},
                   {"/data/adb/modules/AZenith/", IN_MODIFY | IN_CREATE | IN_MOVED_TO | IN_DELETE}};

    for (size_t i = 0; i < sizeof(targets) / sizeof(targets[0]); i++) {
        inotify_add_watch(fd, targets[i].path, targets[i].mask);
    }
    return fd;
}

/**
 * @brief Reads events from inotify descriptor and routes actions.
 * @param inotify_fd Watcher file descriptor.
 * @param ctx Pointer to DaemonContext structure.
 * @param timeout_ms Poll timeout in milliseconds.
 * @return true if an exit command was received, false otherwise.
 */
bool process_inotify_events(int inotify_fd, DaemonContext* ctx, int timeout_ms) {
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
                            handle_background_apps_event(ctx);
                            if (gamestart == NULL)
                                ctx->need_profile_checkup = true;
                        } else if (strcmp(event->name, "current_profile") == 0) {
                            FILE* fp_prof = fopen(PROFILE_MODE, "r");
                            if (fp_prof) {
                                char prof_val[8] = {0};
                                if (fgets(prof_val, sizeof(prof_val), fp_prof)) {
                                    trim_newline(prof_val);
                                    ctx->cur_mode = (ProfileMode)atoi(prof_val);
                                }
                                fclose(fp_prof);
                            }
                        } else if (strcmp(event->name, "current_modes") == 0) {
                            FILE* fp_ai = fopen(DAEMON_MODES, "r");
                            if (fp_ai) {
                                char ai_state[16] = "0";
                                if (fgets(ai_state, sizeof(ai_state), fp_ai)) {
                                    trim_newline(ai_state);
                                    if (ctx->is_initialize_complete && strcmp(ctx->prev_ai_state, ai_state) != 0) {
                                        log_zenith(LOG_INFO, "InotifyHandler: Dynamic profile toggled, Reapplying Balanced Profiles");
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
                            log_zenith(LOG_INFO, "InotifyHandler: Module update detected, exiting.");
                            notify("Module Update", "Please reboot your device to complete module update.", false, 0);
                            __system_property_set("persist.sys.azenith.service", "");
                            __system_property_set("persist.sys.azenith.state", "stopped");
                            return true;
                        } else if (strcmp(event->name, "remove") == 0) {
                            log_zenith(LOG_INFO, "InotifyHandler: Module is removed, exiting.");
                            notify("Module Removed", "Please reboot your device to complete module uninstallation.", false, 0);
                            return true;
                        } else if (strcmp(event->name, "module.prop") == 0) {
                            log_zenith(LOG_INFO, "InotifyHandler: module.prop modified...");
                            is_kanged();
                            check_module_version();
                        } else if (strcmp(event->name, "reboot") == 0) {
                            notify("Daemon Info", "Configuration updated. Please reboot your device to take full effect.", false, 0);
                        } else if (strcmp(event->name, "freqoffset") == 0) {
                            char path[PATH_MAX];
                            snprintf(path, sizeof(path), "/data/adb/.config/AZenith/freqoffset");
                            FILE* fp = fopen(path, "r");
                            if (fp) {
                                if (fgets(ctx->config_freqoffset, sizeof(ctx->config_freqoffset), fp)) {
                                    trim_newline(ctx->config_freqoffset);
                                    log_zenith(LOG_INFO, "InotifyHandler: freqoffset updated to [%s]", ctx->config_freqoffset);
                                }
                                fclose(fp);
                            }
                        } else if (strcmp(event->name, "bypasspath") == 0) {
                            char path[PATH_MAX];
                            snprintf(path, sizeof(path), "/data/adb/.config/AZenith/bypasschgconfig/bypasspath");
                            FILE* fp = fopen(path, "r");
                            if (fp) {
                                if (fgets(ctx->config_bypasspath, sizeof(ctx->config_bypasspath), fp))
                                    trim_newline(ctx->config_bypasspath);
                                fclose(fp);
                            }
                        } else if (strcmp(event->name, "bypasschg") == 0) {
                            char path[PATH_MAX], val[16] = {0};
                            snprintf(path, sizeof(path), "/data/adb/.config/AZenith/bypasschgconfig/bypasschg");
                            FILE* fp = fopen(path, "r");
                            if (fp) {
                                if (fgets(val, sizeof(val), fp))
                                    ctx->config_bypasschg = atoi(val);
                                fclose(fp);
                            }
                        } else if (strcmp(event->name, "bypasschgthreshold") == 0) {
                            char path[PATH_MAX], val[16] = {0};
                            snprintf(path, sizeof(path), "/data/adb/.config/AZenith/bypasschgconfig/bypasschgthreshold");
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
void handle_dynamic_bypass(DaemonContext* ctx) {
    if (strcmp(ctx->config_bypasspath, "UNSUPPORTED") != 0 && strlen(ctx->config_bypasspath) > 0) {
        if (ctx->cur_mode == PERFORMANCE_PROFILE) {
            int threshold = ctx->config_bypasschgthreshold;
            int current_battery = current_system_cache.battery_level;
            int is_device_charging = current_system_cache.is_charging;

            bool bypass_charging_allowed = true;
            if (strcmp(opts.bypass_charging, "false") == 0) {
                bypass_charging_allowed = false;
            } else if (strcmp(opts.bypass_charging, "true") == 0) {
                bypass_charging_allowed = true;
            } else {
                bypass_charging_allowed = (ctx->config_bypasschg == 1);
            }

            if (current_battery >= 0 && bypass_charging_allowed && is_device_charging) {
                if (current_battery >= threshold) {
                    if (read_current_ma() > 50) {
                        enable_bypass();
                        if (!ctx->bypass_applied) {
                            log_zenith(LOG_INFO, "InotifyHandler: Bypass Enabled: Battery (%d%%) >= Threshold (%d%%)", current_battery, threshold);
                            ctx->bypass_applied = true;
                        }
                    }
                } else if (ctx->bypass_applied) {
                    log_zenith(LOG_INFO, "InotifyHandler: Bypass Disabled: Battery (%d%%) dropped below threshold (%d%%)", current_battery, threshold);
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
