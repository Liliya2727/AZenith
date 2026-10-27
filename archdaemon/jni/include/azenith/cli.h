#ifndef AZENITH_CLI_H
#define AZENITH_CLI_H

#include "core.h"
#include <sys/types.h>
#include <ctype.h>
#include <stdlib.h>
#define RR_MODES_MAX 32
#define RR_MODES_PATH "/data/adb/.config/AZenith/rr_modes.json"

void print_help(void);
void clearlogs(void);
void printversion(void);
void openAppMainActivity(void);
int require_daemon_running(void);
int handle_profile(int argc, char** argv);
int handle_log(int argc, char** argv);
int handle_verboselog(int argc, char** argv);
int restart_service(void);
void shownotifications(void);
void hidenotifications(void);
void expose_telemetry_access(void);
void grant_app_permissions(void);
int dump_refresh_rate_modes(void);

#endif
