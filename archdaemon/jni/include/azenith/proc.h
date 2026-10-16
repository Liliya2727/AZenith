#ifndef AZENITH_PROC_H
#define AZENITH_PROC_H

#include "core.h"

int get_pids_of(const char* name, pid_t* pids, int max_pids);
int uidof(pid_t pid);
void setspid(void);
void set_priority(const pid_t pid);

#endif
