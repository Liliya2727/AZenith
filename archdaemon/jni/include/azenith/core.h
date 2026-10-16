#ifndef AZENITH_CORE_H
#define AZENITH_CORE_H

#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <ftw.h>
#include <limits.h>
#include <poll.h>
#include <pthread.h>
#include <stdarg.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/file.h>
#include <sys/inotify.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/system_properties.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

#define MAX_DATA_LENGTH 2048
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

#define LOG_MAX_BYTES (512 * 1024)
#define LOG_KEEP_FILES 2

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
#define APP_MONITOR_FILE "/data/adb/.config/AZenith/app_status"
#define DAEMON_STATE_FILE "/data/adb/.config/AZenith/daemon_state"

#define IS_TRUE(v) ((v) && strcmp((v), "true") == 0)
#define IS_FALSE(v) ((v) && strcmp((v), "false") == 0)
#define IS_DEFAULT(v) (!(v) || strcmp((v), "default") == 0)

#define AZENITH_PROPERTIES "persist.sys.azenith"
#define AZENITH_PROPERTIES_LEN (sizeof(AZENITH_PROPERTIES) - 1)
#define MAX_PENDING_DELETE 128
#define MAX_PROP_NAME_BUF 192

#define MY_PATH                                                                                                                    \
    "PATH=/system/bin:/system/xbin:/data/adb/ap/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:/sbin:/sbin/su:/su/bin:/su/" \
    "xbin:/data/data/com.termux/files/usr/bin"

#define EXECUTE(mode_name, func_call) do { \
    struct timespec start, end; \
    clock_gettime(CLOCK_MONOTONIC, &start); \
    func_call; \
    clock_gettime(CLOCK_MONOTONIC, &end); \
    double elapsed = (end.tv_sec - start.tv_sec) + (end.tv_nsec - start.tv_nsec) / 1e9; \
    log_info("profile", "profile applied name=%s elapsed=%.4fs", mode_name, elapsed); \
} while (0)

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
    char bypass_charging[16];
} GameConfig;

extern GameConfig* g_game_cache;
extern int g_game_cache_count;
extern pthread_mutex_t cache_mutex;

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
    char saved_sys_renderer[PROP_VALUE_MAX];
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
extern char* custom_log_tag;

void validateprop(void);

#endif
