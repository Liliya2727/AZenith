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

#include "AZenith.h"

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

    systemv("setprop persist.sys.rianixia.thermalcore-bigdata.path /data/adb/.config/AZenith/debug");
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
            if (elapsed < 10.0)
                poll_timeout = (int)((10.0 - elapsed) * 1000);
            else
                poll_timeout = 0;
        }

        bool should_exit = process_inotify_events(inotify_fd, &ctx, poll_timeout);
        need_loop = false;

        if (java_daemon_died) {
            log_zenith(LOG_FATAL, "Java daemon lock released, companion daemon exited, stopping daemon");
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
        } else if (real_screen_state && (ctx.cur_mode == BALANCED_PROFILE || ctx.cur_mode == ECO_MODE)) {
            systemv("sys.azenith-profilesettings applyfreqbalance");
        }
        strcpy(ctx.last_freqoffset, ctx.config_freqoffset);

        handle_dynamic_bypass(&ctx);

        if (ctx.is_initialize_complete && strcmp(ctx.prev_ai_state, "0") == 0)
            continue;

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
                    log_zenith(LOG_INFO, "New game detected: %s", active_app_name ? active_app_name : gamestart);
                    game_pid_count = 0;
                    ctx.pid_retries = 0;
                    ctx.has_applied_renderer = false;
                    ctx.need_profile_checkup = true;
                }
            } else if (ctx.cur_mode != BALANCED_PROFILE && ctx.cur_mode != ECO_MODE) {
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
                    log_zenith(LOG_INFO, "Screen ON Event: Grace period aborted. Keeping Performance.");
                    ctx.grace_period_active = false;
                }
                ctx.screen_off_timer = 0;
                ctx.need_profile_checkup = true;
            }
            ctx.prev_screen_state = real_screen_state;
        }

        if (ctx.grace_period_active) {
            if (difftime(time(NULL), ctx.screen_off_timer) < 10.0)
                effective_screen_state = 1;
            else {
                log_zenith(LOG_INFO, "Grace period expired. Dropping Performance Profile.");
                ctx.grace_period_active = false;
                ctx.screen_off_timer = 0;
                effective_screen_state = 0;
                ctx.need_profile_checkup = true;
            }
        }

        if (ctx.is_initialize_complete && ctx.cur_mode != PERFORMANCE_PROFILE && gamestart == NULL && !ctx.need_profile_checkup)
            continue;

        if (ctx.is_initialize_complete && gamestart && effective_screen_state) {
            if (!ctx.need_profile_checkup && ctx.cur_mode == PERFORMANCE_PROFILE && ctx.has_applied_renderer && game_pid_count > 0)
                continue;

            bool is_renderer_changing = false;
            if (!ctx.has_applied_renderer) {
                is_restarting_renderer = true;
                if (!IS_DEFAULT(opts.renderer)) {
                    is_renderer_changing = apply_smart_renderer(opts.renderer, gamestart, ctx.saved_renderer);
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
                        log_zenith(LOG_WARN, "Waiting for %s to spawn (Retry %d/5)...", active_app_name ? active_app_name : gamestart,
                                   ctx.pid_retries);
                        need_loop = true;
                        continue;
                    } else {
                        log_zenith(LOG_ERROR, "Unable to fetch any PIDs for %s after 5 retries. Dropping.",
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
                    if (IS_TRUE(opts.app_priority))
                        set_priority(game_pids[i]);
                    else if (!IS_FALSE(opts.app_priority)) {
                        char val[PROP_VALUE_MAX] = {0};
                        if (__system_property_get("persist.sys.azenithconf.iosched", val) > 0 && val[0] == '1')
                            set_priority(game_pids[i]);
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
