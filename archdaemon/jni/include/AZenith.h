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

#ifndef AZENITH_H
#define AZENITH_H

#include <ctype.h>
#include <dirent.h>
#include <ftw.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/file.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/system_properties.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>
#include <errno.h>
#include <stdarg.h>
#include <stdbool.h>
#include <sys/inotify.h>
#include <pthread.h>
#include <poll.h>

#define TASK_INTERVAL_SEC (12 * 60 * 60)
#define LOOP_INTERVAL_MS 1000
#define LOOP_INTERVAL_SEC 1
#define MAX_DATA_LENGTH 1024
#define MAX_COMMAND_LENGTH 600
#define MAX_OUTPUT_LENGTH 256
#define MAX_PATH_LENGTH 256
#define MAX_LINE 512
#define MAX_PACKAGE 128

#define MAX_GAME_PIDS 8

#define NOTIFY_TITLE "AZenith"
#define LOG_TAG "AZenith"
#define LOG_TAG_PROFILE "AZenith_Profiles"

#define LOCK_FILE "/data/adb/.config/AZenith/API/.lock"
#define LOG_FILE "/data/adb/.config/AZenith/debug/AZenith.log"
#define LOG_VFILE "/data/adb/.config/AZenith/debug/AZenithVerbose.log"
#define LOG_FILE_PRELOAD "/data/adb/.config/AZenith/preload/AZenithPR.log"
#define PROFILE_MODE "/data/adb/.config/AZenith/API/current_profile"
#define PROFILE_MODE_APP "/data/data/zx.azenith/API/current_profile"
#define GAME_INFO "/data/adb/.config/AZenith/API/gameinfo"
#define GAME_INFO_APP "/data/data/zx.azenith/API/gameinfo"
#define GAMELIST "/data/adb/.config/AZenith/gamelist/azenithApplist.json"
#define DAEMON_MODES "/data/adb/.config/AZenith/API/current_modes"
#define MODULE_PROP "/data/adb/modules/AZenith/module.prop"
#define MODULE_UPDATE "/data/adb/modules/AZenith/update"
#define MODULE_REMOVE "/data/adb/modules/AZenith/remove"
#define BYPASSCHG_CONFIG "/data/adb/.config/AZenith/bypasschgconfig"
#define MODULE_VERSION ".placeholder"
#define APP_MONITOR_FILE "/data/adb/.config/AZenith/app_status"
#define DAEMON_STATE_FILE "/data/adb/.config/AZenith/daemon_state"

#define IS_TRUE(v)    ((v) && strcmp((v), "true") == 0)
#define IS_FALSE(v)   ((v) && strcmp((v), "false") == 0)
#define IS_DEFAULT(v) (!(v) || strcmp((v), "default") == 0)

#define AZENITH_PROPERTIES      "persist.sys.azenith"
#define AZENITH_PROPERTIES_LEN  (sizeof(AZENITH_PROPERTIES) - 1)
#define MAX_PENDING_DELETE      128
#define MAX_PROP_NAME_BUF       192

#define MY_PATH                                                                                                                    \
    "PATH=/system/bin:/system/xbin:/data/adb/ap/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:/sbin:/sbin/su:/su/bin:/su/" \
    "xbin:/data/data/com.termux/files/usr/bin"

#define IS_AWAKE(state) (strcmp(state, "Awake") == 0 || strcmp(state, "true") == 0)
#define IS_LOW_POWER(state) (strcmp(state, "true") == 0 || strcmp(state, "1") == 0)

#define EXECUTE(mode_name, func_call) do { \
    struct timespec start, end; \
    clock_gettime(CLOCK_MONOTONIC, &start); \
    func_call; \
    clock_gettime(CLOCK_MONOTONIC, &end); \
    double elapsed = (end.tv_sec - start.tv_sec) + (end.tv_nsec - start.tv_nsec) / 1e9; \
    log_zenith(LOG_INFO, "%s executed for %.4f seconds", mode_name, elapsed); \
} while(0)

typedef struct {
    char package[128];
    char perf_lite_mode[16];
    char dnd_on_gaming[16];
    char app_priority[16];
    char game_preload[16];
    char refresh_rate[16];
    char renderer[64];
    char resolution_downscale[8];
    char resolution_fps[8];  
} GameConfig;

extern GameConfig* g_game_cache;
extern int g_game_cache_count;
extern pthread_mutex_t cache_mutex;

/**
 * @struct SystemStateCache
 * @brief Represents the synchronized state received from the Java Companion Daemon.
 */
typedef struct {
    char focused_app[128];
    char app_name[256];
    int focused_pid;
    int zen_mode;
    int screen_awake;
    int battery_saver;
    int battery_level;
    int is_charging;
} SystemStateCache;

typedef enum : char {
    LOG_DEBUG,
    LOG_INFO,
    LOG_WARN,
    LOG_ERROR,
    LOG_FATAL
} LogLevel;

typedef enum : char {
    PERFCOMMON,
    PERFORMANCE_PROFILE,
    BALANCED_PROFILE,
    ECO_MODE
} ProfileMode;

typedef struct {
    const char* name;
    const char* path;
    const char* on_val;
    const char* off_val;
} BypassNode;

typedef struct {
    char package[256];
} PreloadArgs;

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
    bool resolution_applied;
    bool used_legacy_fallback;
    bool fg_away_active;
    time_t fg_away_timer;
} DaemonContext;

extern const char* VALID_AZENITH_PROPS[];
extern const size_t VALID_AZENITH_PROPS_COUNT;
void validateprop(void);

extern BypassNode bypass_list[];
extern const int bypass_list_size;

extern char* gamestart;
extern char* active_app_name;
extern pid_t game_pids[MAX_GAME_PIDS];
extern int game_pid_count;
extern bool is_restarting_renderer;
extern GameConfig opts;
extern SystemStateCache current_system_cache;
extern int java_lock_pipe[2];
extern bool java_daemon_died;
extern GameConfig* g_game_cache;
extern int g_game_cache_count;
extern pthread_mutex_t cache_mutex;

void load_initial_config_files(DaemonContext* ctx);
void init_daemon_context(DaemonContext* ctx);

extern BypassNode bypass_list[]; 
extern char* gamestart;
extern char* custom_log_tag;

extern pid_t game_pids[MAX_GAME_PIDS];
extern int game_pid_count;

// Main Loop
int main_daemon(void);
void free_gamelist_cache(void);

// Bypass Charging
int echo_to_file(const char* path, const char* value, int lock);
int is_charging();
int read_current_ma();
void disable_bypass();
int enable_bypass();
int check_bypass_compatibility();
void print_bypass_path_list();

// CLI
void print_help();
void clearlogs();
void printversion();
void openAppMainActivity();
int require_daemon_running(void);
int handle_profile(int argc, char** argv);
int handle_log(int argc, char** argv);
int handle_verboselog(int argc, char** argv);
int restart_service(void);

// Misc Utilities
extern void GamePreload(const char* package);
void sighandler(const int signal);
char* trim_newline(char* string);
void notify(const char* title, const char* fmt, bool chrono, int timeout_ms, ...);
void toast(const char* message);
void is_kanged(void);
void checkstate(void);
void escape_shell_string(char *dest, const char *src, size_t max_size);
char* timern(void);
void setspid(void);
bool return_true(void);
bool return_false(void);
void runthermalcore(void);
void check_module_version(void);
void apply_dynamic_refresh_rate(int target_rr);
int get_max_refresh_rate(void);
bool apply_smart_renderer(const char* target_type, char* saved_ref);
bool apply_resolution_target(DaemonContext* ctx, const char* pkg,
                               const char* downscale, const char* fps);
void restore_resolution_target(DaemonContext* ctx, const char* pkg);

// Shell and Command execution
char* execute_command(const char* format, ...);
char* execute_direct(const char* path, const char* arg0, ...);
int systemv(const char* format, ...);

// Utilities
int check_running_state(void);
int write2file(const char* filename, const bool append, const bool use_flock, const char* data, ...);
int is_file_empty(const char *filename);
bool is_java_lock_held(const char* lock_path);

// system
void log_preload(LogLevel level, const char* message, ...);
void log_verbose(LogLevel level, const char* message, ...);
void log_zenith(LogLevel level, const char* message, ...);
void external_log(LogLevel level, const char* tag, const char* message, ...);
void external_vlog(LogLevel level, const char* tag, const char* message, ...);

// Utilities
void set_priority(const pid_t pid);
int uidof(pid_t pid);
void free_gamelist_cache(void);
void reload_gamelist_cache(DaemonContext* ctx);
void restore_daemon_state(DaemonContext* ctx);
void save_daemon_state(DaemonContext* ctx);
void restart_target_app(const char* pkg);

// App Monitor
char* get_visible_package(SystemStateCache* cache);
int get_pids_of(const char* name, pid_t* pids, int max_pids);
int get_current_refresh_rate(void);

// Profiler
extern bool (*get_screenstate)(SystemStateCache*);
extern bool (*get_low_power_state)(SystemStateCache*);
char* get_gamestart(GameConfig* options, SystemStateCache* cache);
bool get_screenstate_normal(SystemStateCache* cache);
bool get_low_power_state_normal(SystemStateCache* cache);
void run_profiler(const int profile);
void read_app_status(SystemStateCache* cache);
void extract_string_value(char* dest, const char* start, size_t max_len);
void handle_dynamic_bypass(DaemonContext* ctx);
void apply_performance_profile(DaemonContext* ctx);
void apply_eco_profile(DaemonContext* ctx);
void apply_balanced_profile(DaemonContext* ctx);

// inotifyhandler
void verify_system_integrity(void);
void wait_for_java_companion(DaemonContext* ctx);
void* java_lock_watcher_thread(void* arg);
int setup_inotify_watchers(void);
bool process_inotify_events(int inotify_fd, DaemonContext* ctx, int timeout_ms);

#endif
