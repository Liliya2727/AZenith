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
#include <sys/system_properties.h>

/**
 * @brief Handles manual performance profile selection via CLI argument.
 * @note Blocks execution if AI/Auto Mode is active in system properties.
 * @param argc Number of CLI arguments.
 * @param argv Array of CLI argument strings.
 * @return 0 on success, or 1 if an invalid profile is requested or Auto Mode is enabled.
 */
int handle_profile(int argc, char** argv) {
    if (argc < 3 || !argv[2] || !argv[2][0]) {
        fprintf(stderr, "ERROR: Missing profile number. Use --profile <1|2|3>\n");
        return 1;
    }

    char ai_state[PROP_VALUE_MAX] = {0};
    __system_property_get("persist.sys.azenithconf.AIenabled", ai_state);

    if (!strcmp(ai_state, "1")) {
        fprintf(stderr, "ERROR: Auto Mode is enabled.\n"
                        "       Manual profile selection is blocked.\n");
        return 1;
    }

    const char* profile = argv[2];

    if (!strcmp(profile, "0")) {
        log_warn("cli", "profile apply rejected number=0 reason=initialize-only");
        printf("WARN: Cannot Apply Profile 0 (Initialize)\n");
    } else if (!strcmp(profile, "1")) {
        log_info("cli", "profile apply requested profile=performance source=cli");
        char lite_prop[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.azenithconf.cpulimit", lite_prop);
        if (strcmp(lite_prop, "1") == 0) {
            __system_property_set("persist.sys.azenithconf.litemode", "1");
        } else {
            __system_property_set("persist.sys.azenithconf.litemode", "0");
        }
        run_profiler(PERFORMANCE_PROFILE);
        notify("Performance Profile", "System is now at Powerful state", false, 0);
        printf("Applying Performance Profile\n");
    } else if (!strcmp(profile, "2")) {
        log_info("cli", "profile apply requested profile=balanced source=cli");
        run_profiler(BALANCED_PROFILE);
        notify("Balanced Profile", "System is now at Optimal state", false, 0);
        printf("Applying Balanced Profile\n");
    } else if (!strcmp(profile, "3")) {
        log_info("cli", "profile apply requested profile=eco source=cli");
        run_profiler(ECO_MODE);
        notify("ECO Mode", "System is now at Endurance state", false, 0);
        printf("Applying Eco Mode\n");
    } else {
        fprintf(stderr, "Invalid profiles.\n");
        return 1;
    }

    return 0;
}

/**
 * @brief Validates input log levels and forwards the combined message string to the external
 * standard logger.
 * @param argc Number of CLI arguments.
 * @param argv Array of CLI argument strings.
 * @return 0 on success, or 1 on parameter validation errors.
 */
int handle_log(int argc, char** argv) {
    if (argc < 5) {
        fprintf(stderr, "Usage: --log <TAG> <LEVEL> <MESSAGE>\n"
                        "Levels: 0=DEBUG, 1=INFO, 2=WARN, 3=ERROR, 4=FATAL\n");
        return 1;
    }

    const char* tag = argv[2];
    const char* level_str = argv[3];

    int level = atoi(level_str);
    if (level < LOG_DEBUG || level > LOG_FATAL) {
        fprintf(stderr, "ERROR: Invalid log level '%s' (valid 0..4)\n", level_str);
        return 1;
    }

    char message[1024];
    message[0] = '\0';

    size_t remaining = sizeof(message);
    for (int i = 4; i < argc; i++) {
        size_t written = snprintf(message + strlen(message), remaining, "%s%s", argv[i], (i == argc - 1) ? "" : " ");
        if (written >= remaining) {
            fprintf(stderr, "ERROR: Log message too long.\n");
            return 1;
        }
        remaining -= written;
    }

    external_log(level, tag, "%s", message);
    return 0;
}

/**
 * @brief Validates input log levels and forwards the combined message string to the external
 * verbose logger.
 * @param argc Number of CLI arguments.
 * @param argv Array of CLI argument strings.
 * @return 0 on success, or 1 on parameter validation errors.
 */
int handle_verboselog(int argc, char** argv) {
    if (argc < 5) {
        fprintf(stderr, "Usage: --log <TAG> <LEVEL> <MESSAGE>\n"
                        "Levels: 0=DEBUG, 1=INFO, 2=WARN, 3=ERROR, 4=FATAL\n");
        return 1;
    }

    const char* tag = argv[2];
    const char* level_str = argv[3];

    int level = atoi(level_str);
    if (level < LOG_DEBUG || level > LOG_FATAL) {
        fprintf(stderr, "ERROR: Invalid log level '%s' (valid 0..4)\n", level_str);
        return 1;
    }

    char message[1024];
    message[0] = '\0';

    size_t remaining = sizeof(message);
    for (int i = 4; i < argc; i++) {
        size_t written = snprintf(message + strlen(message), remaining, "%s%s", argv[i], (i == argc - 1) ? "" : " ");
        if (written >= remaining) {
            fprintf(stderr, "ERROR: Log message too long.\n");
            return 1;
        }
        remaining -= written;
    }

    external_vlog(level, tag, "%s", message);
    return 0;
}

/**
 * @brief Prints the current AZenith module version string to stdout.
 */
void printversion(void) {
    printf("%s\n", MODULE_VERSION);
}

/**
 * @brief Directly launches the primary Android MainActivity of the AZenith application interface.
 */
void openAppMainActivity(void) {
    systemv("/system/bin/am start -a android.intent.action.MAIN zx.azenith/.MainActivity");
}

/**
 * @brief Restricts execution of specific CLI components if the backend daemon engine is not active.
 * @return 1 if the daemon state check passes, 0 otherwise.
 */
int require_daemon_running(void) {
    if (!check_running_state()) {
        fprintf(stderr, "\033[31mERROR:\033[0m AZenith daemon is not running.\n"
                        "Run: sys.azenith-service --run\n");
        return 0;
    }
    return 1;
}

/**
 * @brief Clears all historical and active log caches from storage nodes and triggers an internal
 * app broadcast reset.
 */
void clearlogs(void) {
    systemv("rm -f /data/adb/.config/AZenith/debug/AZenith*.*");
    systemv("rm -f /data/adb/.config/AZenith/sysmon.*");
    log_info("cli", "cleared all log files and rotated generations");
    systemv("su -c \"am broadcast -a zx.azenith.ACTION_MANAGE -n "
            "zx.azenith/.receiver.ZenithReceiver --ez clearall true >/dev/null "
            "2>&1\"");
}

/**
 * @brief Restarts the AZenith service daemon by spawning a detached child
 *        process. The caller returns immediately without blocking on the
 *        actual restart sequence.
 * @return 0 if the detach/fork succeeded, 1 if fork failed.
 */
int restart_service(void) {
    if (daemon(0, 0)) {
        log_fatal("restart", "daemonize failed errno=%d", errno);
        return 1;
    }
    system("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf restartservice");
    return 0;
}


/**
 * @brief Show notifications based on current profiles
 * app broadcast intent.
 */
void shownotifications(void) {
    FILE *fp = fopen("/data/adb/.config/AZenith/API/current_profile", "r");
    if (!fp) {
        return;
    }

    int profile = 0;
    if (fscanf(fp, "%d", &profile) != 1) {
        fclose(fp);
        return;
    }
    fclose(fp);

    switch (profile) {
        case 1:
            notify("Performance Profile", "System is now at Powerful state", false, 0);
            break;
        case 2:
            notify("Balanced Profile", "System is now at Optimal state", false, 0);
            break;
        case 3:
            notify("ECO Mode", "System is now at Endurance state", false, 0);
            break;
        default:
            break;
    }
}

/**
 * @brief Show notifications based on current profiles
 * app broadcast intent.
 */
void hidenotifications(void) {
    
    systemv("su -c \"am broadcast -a zx.azenith.ACTION_MANAGE -n "
            "zx.azenith/.receiver.ZenithReceiver --ez clearall true >/dev/null "
            "2>&1\"");
    
}

/**
 * @brief Expose public read access on CPU/GPU telemetry sysfs nodes and API
 *        mirror so the app reads via File IO without shell polling.
 */
void expose_telemetry_access(void) {
    static const char* cpu_nodes[] = {
        "scaling_cur_freq", "cpuinfo_cur_freq",
        "cpuinfo_max_freq", "cpuinfo_min_freq",
        "scaling_max_freq", "scaling_min_freq",
    };
    static const char* gpu_nodes[] = {
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
        "/sys/class/kgsl/kgsl-3d0/gpubusy",
        "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
        "/sys/class/kgsl/kgsl-3d0/devfreq/max_freq",
        "/sys/kernel/gpu/gpu_busy",
        "/sys/kernel/gpu/gpu_clock",
        "/sys/kernel/gpu/gpu_max_clock",
        "/sys/module/ged/parameters/gpu_loading",
        "/sys/kernel/ged/hal/gpu_utilization",
        "/sys/kernel/ged/hal/ged_kpi",
        "/proc/mali/utilization",
        "/proc/gpufreq/gpufreq_var_dump",
        "/proc/gpufreqv2/gpufreq_status",
        "/sys/class/misc/mali0/device/utilization",
        "/sys/devices/platform/mali/utilization",
    };
    static const char* devfreq_nodes[] = {
        "cur_freq", "max_freq", "min_freq", "load", "utilization",
    };
    char path[256];

    /* API mirror */
    systemv("mkdir -p /data/data/zx.azenith/API /data/adb/.config/AZenith/API");
    chmod("/data/data/zx.azenith/API", 0777);
    chmodg("/data/data/zx.azenith/API/*", 0644);
    chmodg("/data/adb/.config/AZenith/API/*", 0644);

    /* CPU freq + /proc/stat */
    for (size_t i = 0; i < sizeof(cpu_nodes) / sizeof(*cpu_nodes); i++) {
        snprintf(path, sizeof(path),
                 "/sys/devices/system/cpu/cpu[0-9]*/cpufreq/%s", cpu_nodes[i]);
        chmodg(path, 0444);
    }
    chmod("/proc/stat", 0444);

    /* GPU */
    chmod("/sys/kernel/ged", 0755);
    chmod("/sys/kernel/ged/hal", 0755);
    for (size_t i = 0; i < sizeof(gpu_nodes) / sizeof(*gpu_nodes); i++) {
        chmod(gpu_nodes[i], 0444);
    }

    /* devfreq */
    for (size_t i = 0; i < sizeof(devfreq_nodes) / sizeof(*devfreq_nodes); i++) {
        snprintf(path, sizeof(path), "/sys/class/devfreq/*/%s", devfreq_nodes[i]);
        chmodg(path, 0444);
    }

    /* thermal */
    chmodg("/sys/class/thermal/thermal_zone*/temp", 0444);

    log_info("cli", "telemetry access exposed");
}

/**
 * @brief Grant runtime permissions, appops, and doze whitelist to zx.azenith.
 */
void grant_app_permissions(void) {
    systemv("pm grant zx.azenith android.permission.READ_EXTERNAL_STORAGE >/dev/null 2>&1");
    systemv("pm grant zx.azenith android.permission.POST_NOTIFICATIONS >/dev/null 2>&1");
    systemv("pm grant zx.azenith android.permission.READ_MEDIA_IMAGES >/dev/null 2>&1");
    systemv("pm grant zx.azenith android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1");
    systemv("pm grant zx.azenith android.permission.PACKAGE_USAGE_STATS >/dev/null 2>&1");
    systemv("pm grant zx.azenith android.permission.BATTERY_STATS >/dev/null 2>&1");
    systemv("pm grant zx.azenith android.permission.DUMP >/dev/null 2>&1");

    systemv("appops set zx.azenith SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1");
    systemv("appops set zx.azenith WRITE_SETTINGS allow >/dev/null 2>&1");
    systemv("appops set zx.azenith GET_USAGE_STATS allow >/dev/null 2>&1");
    systemv("appops set zx.azenith RUN_IN_BACKGROUND allow >/dev/null 2>&1");
    systemv("appops set zx.azenith RUN_ANY_IN_BACKGROUND allow >/dev/null 2>&1");
    systemv("appops set zx.azenith ACCESS_RESTRICTED_SETTINGS allow >/dev/null 2>&1");

    systemv("pm grant zx.azenith android.permission.QUERY_ALL_PACKAGES >/dev/null 2>&1");
    systemv("pm grant zx.azenith com.android.permission.GET_INSTALLED_APPS >/dev/null 2>&1");
    systemv("appops set zx.azenith android:query_all_packages allow >/dev/null 2>&1");
    systemv("appops set zx.azenith query_all_packages allow >/dev/null 2>&1");
    systemv("appops set zx.azenith android:get_installed_apps allow >/dev/null 2>&1");
    systemv("appops set zx.azenith GET_INSTALLED_APPS allow >/dev/null 2>&1");
    systemv("appops set zx.azenith 10022 allow >/dev/null 2>&1");

    systemv("dumpsys deviceidle whitelist +zx.azenith >/dev/null 2>&1");

    log_info("cli", "app permissions granted");
}

/**
 * @brief Parses `dumpsys SurfaceFlinger` for display modes and writes the unique
 *        refresh rates (sorted ascending by Hz) to rr_modes.json.
 * @note Equivalent of the old shell pipeline. The file is left untouched if no
 *       modes are found.
 * @return 0 on success, 1 on failure or when no modes were found.
 */
typedef struct {
    int id;
    int hz;
} rr_mode_t;

static int rr_cmp_hz(const void* a, const void* b) {
    return ((const rr_mode_t*)a)->hz - ((const rr_mode_t*)b)->hz;
}

int dump_refresh_rate_modes(void) {
    FILE* p = popen("dumpsys SurfaceFlinger", "r");
    if (!p) {
        log_error("cli", "rr_modes popen failed errno=%d", errno);
        return 1;
    }

    rr_mode_t modes[RR_MODES_MAX];
    int count = 0;
    char line[8192];

    while (fgets(line, sizeof(line), p)) {
        char* cur = line;
        while ((cur = strstr(cur, "id=")) != NULL) {
        
            if (cur != line && isalnum((unsigned char)cur[-1])) {
                cur += 3;
                continue;
            }

            char* num = cur + 3;
            char* end = NULL;
            long id = strtol(num, &end, 10);
            if (end == num || *end != ',') {
                cur += 3;
                continue;
            }

            char* span_end = strchr(end, '}');
            size_t span_len = span_end ? (size_t)(span_end - end) : strlen(end);

            char* vs = NULL;
            for (char* s = end; s < end + span_len;) {
                char* f = strstr(s, "vsyncRate=");
                if (!f || f >= end + span_len) break;
                vs = f;
                s = f + 10;
            }
            if (!vs) {
                cur = end;
                continue;
            }

            char* hz_str = vs + 10;
            char* hz_end = NULL;
            long hz = strtol(hz_str, &hz_end, 10);
            if (hz_end != hz_str && hz > 0) {
                int dup = 0;
                for (int i = 0; i < count; i++) {
                    if (modes[i].hz == (int)hz) { dup = 1; break; }
                }
                if (!dup && count < RR_MODES_MAX) {
                    modes[count].id = (int)id;
                    modes[count].hz = (int)hz;
                    count++;
                }
            }
            cur = span_end ? span_end : end + span_len;
        }
    }
    pclose(p);

    if (count == 0) {
        log_warn("cli", "rr_modes no display modes found");
        return 1;
    }

    qsort(modes, count, sizeof(rr_mode_t), rr_cmp_hz);

    FILE* out = fopen(RR_MODES_PATH, "w");
    if (!out) {
        log_error("cli", "rr_modes open failed errno=%d", errno);
        return 1;
    }
    fputc('[', out);
    for (int i = 0; i < count; i++) {
        fprintf(out, "%s{\"id\":%d,\"hz\":%d}", i ? "," : "", modes[i].id, modes[i].hz);
    }
    fputs("]\n", out);
    fclose(out);

    log_info("cli", "rr_modes written count=%d", count);
    return 0;
}
