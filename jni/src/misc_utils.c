/*
 * Copyright (C) 2024-2025 Rem01Gaming x Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 *                      present.
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
 * Inputs             : 
 * Returns            : None
 * Description        : Push a notification.
 ***********************************************************************************/
void notify(const char* title, const char* fmt, const char* chrono, int timeout_ms, ...) {
    char message[512];
    va_list args;
    va_start(args, timeout_ms);
    vsnprintf(message, sizeof(message), fmt, args);
    va_end(args);

    if (timeout_ms > 0) {
        systemv("su -c \"am start -n zx.azenith/.MainActivity -e notifytitle '%s' -e notifytext '%s' -e chrono %s -e timeout %d\"", 
                title, message, chrono, timeout_ms);
    } else {
        systemv("su -c \"am start -n zx.azenith/.MainActivity -e notifytitle '%s' -e notifytext '%s' -e chrono %s\"", 
                title, message, chrono);
    }
}



/***********************************************************************************
 * Function Name      : timern
 * Inputs             : None
 * Returns            : char * - pointer to a statically allocated string
 *                      with the formatted time.
 * Description        : Generates a timestamp with the format
 *                      [YYYY-MM-DD HH:MM:SS.milliseconds].
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

    // Append milliseconds
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

    // Exit gracefully
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
    if (__system_property_get("persist.sys.azenithconf.showtoast", val) > 0) {
        if (val[0] == '1') {            
            
            int exit = systemv("su -c \"/system/bin/am start --user 0 -n zx.azenith/.MainActivity -e toasttext '%s' >/dev/null 2>&1\"",
                               message);

            if (exit != 0) [[clang::unlikely]] {
                log_zenith(LOG_WARN, "Unable to show toast message: %s", message);
            }
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

char* skip_space(char* p) {
    while (*p && isspace(*p)) p++;
    return p;
}

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
