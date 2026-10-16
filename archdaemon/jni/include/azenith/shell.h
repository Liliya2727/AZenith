#ifndef AZENITH_SHELL_H
#define AZENITH_SHELL_H

#include "core.h"

char* execute_command(const char* format, ...);
char* execute_direct(const char* path, const char* arg0, ...);
int systemv(const char* format, ...);

#endif
