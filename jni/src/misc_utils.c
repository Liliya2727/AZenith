/*
 * Copyright (C) 2024-2025 Rem01Gaming x Zexshia
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
#include <time.h>
static time_t last_task_run = 0;

/***********************************************************************************
 * Function Name      : trim_newline
 * Inputs             : str (char *) - string to trim newline from
 * Returns            : char * - string without newline
 * Description        : Trims a newline character at the end of a string if
 * present.
 ***********************************************************************************/
[[gnu::always_inline]] char* trim_newline(char* string) {
    if (string == NULL)
        return NULL;

    char* end;
    if ((end = strchr(string, '\n')) != NULL)
        *end = '\0';

    return string;
}

/***********************************************************************************
 * Function Name      : notify
 * Inputs             : const char* title, const char* fmt, bool chrono, int timeout
 * Returns            : None
 * Description        : Push a notification.
 ***********************************************************************************/
void notify(const char* title, const char* fmt, bool chrono, int timeout_ms, ...) {
    char message[512];
    va_list args;
    va_start(args, timeout_ms);
    vsnprintf(message, sizeof(message), fmt, args);
    va_end(args);

    const char* action = "zx.azenith.ACTION_MANAGE";
    const char* component = "zx.azenith/.receiver.ZenithReceiver";

    if (timeout_ms > 0) {
        systemv("su -c \"am broadcast -a %s -n %s "
                "--es notifytitle '%s' --es notifytext '%s' "
                "--ez chrono_bool %s --es timeout '%d' "
                ">/dev/null 2>&1\"",
                action, component, title, message,
                chrono ? "true" : "false", timeout_ms);
    } else {
        systemv("su -c \"am broadcast -a %s -n %s "
                "--es notifytitle '%s' --es notifytext '%s' "
                "--ez chrono_bool %s "
                ">/dev/null 2>&1\"",
                action, component, title, message,
                chrono ? "true" : "false");
    }
}

/***********************************************************************************
 * Function Name      : timern
 * Inputs             : None
 * Returns            : char * - pointer to a statically allocated string
 * with the formatted time.
 * Description        : Generates a timestamp with the format
 * [YYYY-MM-DD HH:MM:SS.milliseconds].
 ***********************************************************************************/
char* timern(void) {
    static char timestamp[64];
    struct timeval tv;
    time_t current_time;
    struct tm* local_time;

    gettimeofday(&tv, NULL);
    current_time = tv.tv_sec;
    local_time = localtime(&current_time);

    if (local_time == NULL) [[clang::unlikely]] {
        strcpy(timestamp, "[TimeError]");
        return timestamp;
    }

    size_t format_result = strftime(timestamp, sizeof(timestamp), "%Y-%m-%d %H:%M:%S", local_time);
    if (format_result == 0) [[clang::unlikely]] {
        strcpy(timestamp, "[TimeFormatError]");
        return timestamp;
    }

    snprintf(timestamp + strlen(timestamp), sizeof(timestamp) - strlen(timestamp), ".%03ld", tv.tv_usec / 1000);

    return timestamp;
}

/***********************************************************************************
 * Function Name      : sighandler
 * Inputs             : int signal - exit signal
 * Returns            : None
 * Description        : Handle exit signal.
 ***********************************************************************************/
[[noreturn]] void sighandler(const int signal) {
    switch (signal) {
    case SIGTERM:
        log_zenith(LOG_INFO, "Received SIGTERM, exiting.");
        break;
    case SIGINT:
        log_zenith(LOG_INFO, "Received SIGINT, exiting.");
        break;
    }

    _exit(EXIT_SUCCESS);
}

/***********************************************************************************
 * Function Name      : toast
 * Inputs             : message (const char *) - Message to display
 * Returns            : None
 * Description        : Display a toast notification using bellavita.toast app.
 ***********************************************************************************/
void toast(const char* message) {
    char val[PROP_VALUE_MAX] = {0};

    if (__system_property_get("persist.sys.azenithconf.showtoast", val) > 0 && val[0] == '1') {

        int exit = systemv("su -c \"am broadcast "
                           "-a zx.azenith.ACTION_MANAGE "
                           "-n zx.azenith/.receiver.ZenithReceiver "
                           "--es toasttext '%s' "
                           ">/dev/null 2>&1\"",
                           message);

        if (exit != 0) [[clang::unlikely]] {
            log_zenith(LOG_WARN, "Unable to send toast broadcast: %s", message);
        }
    }
}

/***********************************************************************************
 * Function Name      : is_kanged
 * Inputs             : None
 * Returns            : None
 * Description        : Checks if the module renamed/modified by 3rd party.
 ***********************************************************************************/
void is_kanged(void) {
    if (systemv("grep -q '^name=AZenith火$' %s", MODULE_PROP) != 0) [[clang::unlikely]] {
        goto doorprize;
    }

    if (systemv("grep -q '^author=@Zexshia X @kanaochar$' %s", MODULE_PROP) != 0) [[clang::unlikely]] {
        goto doorprize;
    }

    return;

doorprize:
    log_zenith(LOG_FATAL, "Module modified by 3rd party, exiting.");
    notify("Daemon Error", "Trying to rename me?", "false", 0);
    systemv("setprop persist.sys.azenith.service \"\"");
    systemv("setprop persist.sys.azenith.state stopped");
    exit(EXIT_FAILURE);
}

/***********************************************************************************
 * Function Name      : check_module_version
 * Inputs             : None
 * Returns            : None
 * Description        : Compares version inside module.prop with daemon version.
 ***********************************************************************************/
void check_module_version(void) {
    char DAEMON_VERSION[MAX_LINE] = {0};

    snprintf(DAEMON_VERSION, sizeof(DAEMON_VERSION), "%s", MODULE_VERSION);

    int ret = systemv(
        "grep -q '^version=%s$' %s",
        DAEMON_VERSION,
        MODULE_PROP
    );

    if (ret != 0) [[clang::unlikely]] {
        log_zenith(LOG_FATAL,
                   "AZenith version mismatch with daemon version! please reinstall the module!");
        notify("Daemon Error", "AZenith version mismatch, please reinstall!", "false", 0);
        systemv("setprop persist.sys.azenith.service \"\"");
        systemv("setprop persist.sys.azenith.state stopped");
        exit(EXIT_FAILURE);
    }
}

/***********************************************************************************
 * Function Name      : checkstate
 * Inputs             : None
 * Returns            : None
 * Description        : Exits if the module prop is "stopped" or not set
 ***********************************************************************************/
void checkstate(void) {
    char state[64] = {0};
    FILE* fp = popen("getprop persist.sys.azenith.state", "r");
    if (fp) {
        fgets(state, sizeof(state), fp);
        pclose(fp);
    }
    state[strcspn(state, "\n")] = 0;
    if (state[0] == '\0' || strcmp(state, "stopped") == 0) [[clang::unlikely]] {
        goto killsvc;
    }
    return;
killsvc:
    log_zenith(LOG_FATAL, "Service killed by checkstate().");
    systemv("setprop persist.sys.azenith.service \"\"");
    systemv("setprop persist.sys.azenith.state stopped");
    exit(EXIT_FAILURE);
}

/***********************************************************************************
 * Function Name      : return_true
 * Inputs             : None
 * Returns            : bool - only true
 * Description        : Will be used for error fallback.
 * Note               : Never call this function.
 ***********************************************************************************/
bool return_true(void) {
    return true;
}

/***********************************************************************************
 * Function Name      : return_false
 * Inputs             : None
 * Returns            : bool - only false
 * Description        : Will be used for error fallback.
 * Note               : Never call this function.
 ***********************************************************************************/
bool return_false(void) {
    return false;
}

/***********************************************************************************
 * Function Name      : setspid
 * Inputs             : None
 * Returns            : Service PID
 * Description        : Set Service PID Properties
 ***********************************************************************************/
void setspid(void) {
    char cmd[128];
    pid_t pid = getpid();

    snprintf(cmd, sizeof(cmd), "setprop persist.sys.azenith.service %d", pid);
    systemv(cmd);
}

/***********************************************************************************
 * Function Name      : runthermalcore
 * Inputs             : none
 * Returns            : None
 * Description        : run thermalcore service if enabled
 ***********************************************************************************/
void runthermalcore(void) {
    char thermalcore[PROP_VALUE_MAX] = {0};
    __system_property_get("persist.sys.azenithconf.thermalcore", thermalcore);
    if (strcmp(thermalcore, "1") == 0) {
        systemv("sys.azenith-rianixiathermalcore &");
        FILE* fp = popen("pidof sys.azenith-rianixiathermalcore", "r");
        if (fp == NULL) {
            perror("pidof failed");
            log_zenith(LOG_INFO, "Failed to run Thermalcore service");
            return;
        }
        char pid_str[32] = {0};
        if (fgets(pid_str, sizeof(pid_str), fp) != NULL) {
            int pid = atoi(pid_str);
            log_zenith(LOG_INFO, "Starting Thermalcore Service with pid %d", pid);
        } else {
            log_zenith(LOG_INFO, "Thermalcore Service started but PID not found");
        }

        pclose(fp);
    }
}

/***********************************************************************************
 * Function Name      : runtask
 * Inputs             : none
 * Returns            : None
 * Description        : run a command periodically for every 12hours
 ***********************************************************************************/
void runtask(void) {
    struct timespec now;

    clock_gettime(CLOCK_MONOTONIC, &now);

    if (last_task_run == 0) {
        last_task_run = now.tv_sec;
        log_zenith(LOG_INFO, "Running scheduled task for the next 12h");
        systemv("sys.azenith-utilityconf FSTrim");
        return;
    }

    if ((now.tv_sec - last_task_run) >= TASK_INTERVAL_SEC) {
        last_task_run = now.tv_sec;
        log_zenith(LOG_INFO, "Executing scheduled task, next task will be run in next 12h");
        notify("Daemon Info", "12 hours passed — AZenith doing its routine check. All good.", "false", 0);

        systemv("sys.azenith-utilityconf FSTrim");
    }
}

/***********************************************************************************
 * Function Name      : apply_smart_renderer
 * Inputs             : target_type, pkg, saved_ref
 * Returns            : bool - true if renderer switched
 * Description        : Checks current renderer and switches if necessary
 ***********************************************************************************/
bool apply_smart_renderer(const char* target_type, const char* pkg, char* saved_ref) {
    if (target_type == NULL || strcmp(target_type, "default") == 0 || strlen(target_type) == 0) return false;

    char current_renderer[PROP_VALUE_MAX] = {0};
    __system_property_get("debug.hwui.renderer", current_renderer);

    if (strlen(saved_ref) == 0) {
        strncpy(saved_ref, current_renderer, PROP_VALUE_MAX - 1);
    }

    const char* real_target = strcmp(target_type, "vulkan") == 0 ? "skiavk" : "skiagl";

    if (strcmp(current_renderer, real_target) != 0) {
        log_zenith(LOG_INFO, "Renderer mismatch for %s. Switching to %s...", pkg, real_target);

        systemv("sys.azenith-utilityconf setrender %s", real_target);

        systemv("am force-stop %s && am start -n $(cmd package resolve-activity --brief %s | tail -n 1)", pkg, pkg);

        return true;
    }
    return false;
}

/***********************************************************************************
 * Function Name      : get_max_refresh_rate
 * Inputs             : None
 * Returns            : int - Max Refresh Rate in Hz
 * Description        : Detects max hardware refresh rate via dumpsys
 ***********************************************************************************/
int get_max_refresh_rate(void) {
    const char* cmd = "dumpsys display | grep -oE \"fps=[0-9.]+|refreshRate=[0-9.]+\" | grep -oE \"[0-9.]+\" | sort -rn | head -n1";

    FILE *fp = popen(cmd, "r");
    if (!fp) {
        log_zenith(LOG_WARN, "Failed to execute dumpsys for RR detection");
        return 60;
    }

    char buf[32] = {0};
    float max_rr_f = 60.0f;

    if (fgets(buf, sizeof(buf), fp)) {
        max_rr_f = atof(buf);
    }
    pclose(fp);

    int max_rr = (int)(max_rr_f + 0.5f);
    if (max_rr <= 0) max_rr = 60;

    log_zenith(LOG_INFO, "Detected Hardware Max Refresh Rate: %dHz", max_rr);
    return max_rr;
}

/***********************************************************************************
 * Function Name      : apply_dynamic_refresh_rate
 * Inputs             : target_rr (int)
 * Returns            : None
 * Description        : Calculates and applies refresh rate mode based on target
 ***********************************************************************************/
void apply_dynamic_refresh_rate(int target_rr) {
    int max_hw = get_max_refresh_rate();

    int steps[] = {165, 144, 120, 90, 60};
    int num_steps = sizeof(steps) / sizeof(steps[0]);

    int max_idx = -1;
    int target_idx = -1;

    for (int i = 0; i < num_steps; i++) {
        if (max_hw >= steps[i]) {
            max_idx = i;
            break;
        }
    }

    for (int i = 0; i < num_steps; i++) {
        if (target_rr >= steps[i]) {
            target_idx = i;
            break;
        }
    }

    int mode_to_apply = 0;
    if (max_idx != -1 && target_idx != -1) {
        mode_to_apply = target_idx - max_idx;
    }

    if (mode_to_apply < 0) mode_to_apply = 0;

    log_zenith(LOG_INFO, "DynamicRR: MaxHW %dHz, Target %dHz -> Applying Mode %d",
               max_hw, target_rr, mode_to_apply);

    systemv("sys.azenith-utilityconf setrefreshrates %d", mode_to_apply);
}

/***********************************************************************************
 * Function Name      : skip_space
 * Inputs             : p (char *)
 * Returns            : char * - pointer to first non-space character
 * Description        : Skips whitespace characters in a string
 ***********************************************************************************/
char* skip_space(char* p) {
    while (*p && isspace(*p)) p++;
    return p;
}

/***********************************************************************************
 * Function Name      : extract_string_value
 * Inputs             : dest, key_pos, max_len
 * Returns            : None
 * Description        : Extracts value from key: "value" string format
 ***********************************************************************************/
void extract_string_value(char* dest, const char* key_pos, size_t max_len) {
    if (!key_pos) {
        strncpy(dest, "default", max_len-1);
        dest[max_len-1] = '\0';
        return;
    }

    const char* colon = strchr(key_pos, ':');
    if (!colon) {
        strncpy(dest, "default", max_len-1);
        dest[max_len-1] = '\0';
        return;
    }

    const char* start = colon + 1;
    while (*start == ' ' || *start == '\t') start++;

    if (*start == '\"') start++;

    const char* end = strchr(start, '\"');
    if (!end) {
        strncpy(dest, "default", max_len-1);
        dest[max_len-1] = '\0';
        return;
    }

    size_t len = end - start;
    if (len >= max_len) len = max_len - 1;
    strncpy(dest, start, len);
    dest[len] = '\0';
}

/***********************************************************************************
 * Function Name      : get_current_refresh_rate
 * Inputs             : None
 * Returns            : int - Current Refresh Rate in Hz
 * Description        : Retrieves current render frame rate
 ***********************************************************************************/
int get_current_refresh_rate(void) {
    FILE *fp = popen(
        "cmd display get-displays | "
        "grep -oE \"renderFrameRate [0-9.]+\" | "
        "awk '{print int($2+0.5)}'",
        "r"
    );

    if (!fp)
        return -1;

    char buf[32] = {0};
    if (!fgets(buf, sizeof(buf), fp)) {
        pclose(fp);
        return -1;
    }

    pclose(fp);
    return atoi(buf);
}
