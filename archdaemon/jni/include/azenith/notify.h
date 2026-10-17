#ifndef AZENITH_NOTIFY_H
#define AZENITH_NOTIFY_H

#include "core.h"

void notify(const char* title, const char* fmt, bool chrono, int timeout_ms, ...);
void toast(const char* message);

#endif
