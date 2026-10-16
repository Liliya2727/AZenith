#ifndef AZENITH_IO_H
#define AZENITH_IO_H

#include "core.h"

int write2file(const char* filename, const bool append, const bool use_flock, const char* data, ...);
int is_file_empty(const char* filename);
bool is_java_lock_held(const char* lock_path);
int check_running_state(void);

#endif
