#ifndef AZENITH_UTIL_H
#define AZENITH_UTIL_H

#include "core.h"

[[gnu::always_inline]] char* trim_newline(char* string);
void escape_shell_string(char* dest, const char* src, size_t max_size);
char* timern(void);
[[noreturn]] void sighandler(const int signal);
void checkstate(void);
bool return_true(void);
bool return_false(void);
void extract_string_value(char* dest, const char* start, size_t max_len);
void restart_target_app(const char* pkg);

#endif
