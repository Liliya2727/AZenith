#ifndef AZENITH_SHELL_H
#define AZENITH_SHELL_H

#include "core.h"
#include "glob.h"
#include "sys/stat.h"

char* execute_command(const char* format, ...);
char* execute_direct(const char* path, const char* arg0, ...);
int systemv(const char* format, ...);
void chmodg(const char* pattern, mode_t mode);

#endif
