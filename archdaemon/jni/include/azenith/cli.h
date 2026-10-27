#ifndef AZENITH_CLI_H
#define AZENITH_CLI_H

#include "core.h"
#include <sys/types.h>

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

#endif
