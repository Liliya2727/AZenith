#ifndef AZENITH_LOG_H
#define AZENITH_LOG_H

#include "core.h"

void log_write(LogLevel level, const char* component, const char* message, ...);
void log_debug(const char* component, const char* message, ...);
void log_info(const char* component, const char* message, ...);
void log_warn(const char* component, const char* message, ...);
void log_error(const char* component, const char* message, ...);
void log_fatal(const char* component, const char* message, ...);
void log_preload(const char* component, const char* message, ...);
void external_log(LogLevel level, const char* tag, const char* message, ...);
void external_vlog(LogLevel level, const char* tag, const char* message, ...);

#endif
