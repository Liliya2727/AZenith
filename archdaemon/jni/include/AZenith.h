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
#include <pthread.h> // FIX: Ditambahkan untuk pthread_mutex_t

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

#define IS_TRUE(v)    ((v) && strcmp((v), "true") == 0)
#define IS_FALSE(v)   ((v) && strcmp((v), "false") == 0)
#define IS_DEFAULT(v) (!(v) || strcmp((v), "default") == 0)

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
} GameConfig;

// FIX: Gunakan extern agar variabel ini menjadi Global Shared di semua file .c
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
int get_battery_level();
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
bool apply_smart_renderer(const char* target_type, const char* pkg, char* saved_ref);

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
void external_log(LogLevel level, const char* tag, const char* message);
void external_vlog(LogLevel level, const char* tag, const char* message);

// Utilities
void set_priority(const pid_t pid);
int uidof(pid_t pid);

// App Monitor
char* get_visible_package(SystemStateCache* cache);
int get_pids_of(const char* name, pid_t* pids, int max_pids);

// Profiler
extern bool (*get_screenstate)(SystemStateCache*);
extern bool (*get_low_power_state)(SystemStateCache*);
char* get_gamestart(GameConfig* options, SystemStateCache* cache); // FIX: GameOptions -> GameConfig
bool get_screenstate_normal(SystemStateCache* cache);
bool get_low_power_state_normal(SystemStateCache* cache);
void run_profiler(const int profile);
char* skip_space(char* p);
void read_app_status(SystemStateCache* cache);
void extract_string_value(char* dest, const char* start, size_t max_len);

#endif
