#ifndef AZENITH_H
#define AZENITH_H

#include <ctype.h>
#include <dirent.h>
#include <ftw.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/file.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/system_properties.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>


#define TASK_INTERVAL_SEC (12 * 60 * 60)
#define LOOP_INTERVAL_MS 700
#define LOOP_INTERVAL_SEC 2
#define MAX_DATA_LENGTH 1024
#define MAX_COMMAND_LENGTH 600
#define MAX_OUTPUT_LENGTH 256
#define MAX_PATH_LENGTH 256
#define MAX_LINE 512
#define MAX_PACKAGE 128

#define NOTIFY_TITLE "AZenith"
#define LOG_TAG "AZenith"

#define LOCK_FILE "/data/adb/.config/AZenith/API/.lock"
#define LOG_FILE "/data/adb/.config/AZenith/debug/AZenith.log"
#define LOG_VFILE "/data/adb/.config/AZenith/debug/AZenithVerbose.log"
#define LOG_FILE_PRELOAD "/data/adb/.config/AZenith/preload/AZenithPR.log"
#define PROFILE_MODE "/data/adb/.config/AZenith/API/current_profile"
#define PROFILE_MODE_APP "/data/data/zx.azenith/API/current_profile"
#define GAME_INFO "/data/adb/.config/AZenith/API/gameinfo"
#define GAMELIST "/data/adb/.config/AZenith/gamelist/azenithApplist.json"
#define MODULE_PROP "/data/adb/modules/AZenith/module.prop"
#define MODULE_UPDATE "/data/adb/modules/AZenith/update"
#define MODULE_VERSION ".placeholder"
#define IS_TRUE(v)    ((v) && strcmp((v), "true") == 0)
#define IS_FALSE(v)   ((v) && strcmp((v), "false") == 0)
#define IS_DEFAULT(v) (!(v) || strcmp((v), "default") == 0)
#define MY_PATH                                                                                                                    \
    "PATH=/system/bin:/system/xbin:/data/adb/ap/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:/sbin:/sbin/su:/su/bin:/su/" \
    "xbin:/data/data/com.termux/files/usr/bin"

#define IS_MLBB(gamestart)                                                                               \
    (strcmp(gamestart, "com.mobile.legends") == 0 || strcmp(gamestart, "com.mobilelegends.hwag") == 0 || \
     strcmp(gamestart, "com.mobiin.gp") == 0 || strcmp(gamestart, "com.mobilechess.gp") == 0)

#define IS_AWAKE(state) (strcmp(state, "Awake") == 0 || strcmp(state, "true") == 0)
#define IS_LOW_POWER(state) (strcmp(state, "true") == 0 || strcmp(state, "1") == 0)

// Basic C knowledge: enum starts with 0
typedef struct {
    char perf_lite_mode[16];
    char dnd_on_gaming[16];
    char app_priority[16];
    char game_preload[16];
    char refresh_rate[16];
    char renderer[16];
} GameOptions;

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

typedef enum : char {
    MLBB_NOT_RUNNING,
    MLBB_RUN_BG,
    MLBB_RUNNING
} MLBBState;

extern char* gamestart;
extern char* custom_log_tag;
extern pid_t game_pid;

/*
 * If you're here for function comments, you
 * are in the wrong place.
 */

// CLI
void print_help();
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
char* timern(void);
void setspid(void);
bool return_true(void);
bool return_false(void);
void runthermalcore(void);
void check_module_version(void);
void runtask(void);
int get_current_refresh_rate(void);

// Shell and Command execution
char* execute_command(const char* format, ...);
char* execute_direct(const char* path, const char* arg0, ...);
int systemv(const char* format, ...);

// Utilities
int check_running_state(void);
int write2file(const char* filename, const bool append, const bool use_flock, const char* data, ...);
int is_file_empty(const char *filename);

// system
void log_preload(LogLevel level, const char* message, ...);
void log_zenith(LogLevel level, const char* message, ...);
void external_log(LogLevel level, const char* tag, const char* message);
void external_vlog(LogLevel level, const char* tag, const char* message);

// Utilities
void set_priority(const pid_t pid);
pid_t pidof(const char* name);
int uidof(pid_t pid);

// Dumpsys
char* get_visible_package(void);

// Handler
extern pid_t mlbb_pid;
MLBBState handle_mlbb(const char* gamestart);

// Profiler
extern bool (*get_screenstate)(void);
extern bool (*get_low_power_state)(void);
char* get_gamestart(GameOptions* options);
bool get_screenstate_normal(void);
bool get_low_power_state_normal(void);
void run_profiler(const int profile);
char* skip_space(char* p);
void extract_string_value(char* dest, const char* start, size_t max_len);

#endif
