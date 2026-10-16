#ifndef AZENITH_BYPASS_H
#define AZENITH_BYPASS_H

#include "core.h"

int echo_to_file(const char* path, const char* value, int lock);
int is_charging(void);
int read_current_ma(void);
void disable_bypass(void);
int enable_bypass(void);
int check_bypass_compatibility(void);
void print_bypass_path_list(void);

#endif
