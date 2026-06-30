CC     = clang
TARGET = sys.azenith-service

SRCS = \
    main.c \
    src/cmd_utils.c \
    src/azenith_log.c \
    src/azenith_profiler.c \
    src/file_utils.c \
    src/process_utils.c \
    src/misc_utils.c \
    src/game_preload.c \
    src/main_loop.c \
    src/azenith_commandline.c \
    src/bypass_charge.c \
    src/app_status_monitor.c \
    src/refreshrates.c \
    src/renderer.c \
    src/app_monitor.c

PROD_CFLAGS  = -Iinclude -DNDEBUG -Wall -Wextra -Werror \
               -pedantic-errors -Wpedantic \
               -O2 -std=c23 -fPIC -flto

PROD_LDFLAGS = -llog -flto

ASAN_FLAGS = -fsanitize=address,undefined \
             -fno-omit-frame-pointer \
             -fno-optimize-sibling-calls

DBG_CFLAGS  = -Iinclude -Wall -Wextra -std=c23 \
              $(ASAN_FLAGS) -g

DBG_LDFLAGS = -llog $(ASAN_FLAGS)

.PHONY: all debug clean

all: $(TARGET)

$(TARGET): $(SRCS)
	$(CC) $(PROD_CFLAGS) $(SRCS) $(PROD_LDFLAGS) -o $@

debug: $(TARGET)_asan

$(TARGET)_asan: $(SRCS)
	$(CC) $(DBG_CFLAGS) $(SRCS) $(DBG_LDFLAGS) -o $@

clean:
	rm -f $(TARGET) $(TARGET)_asan