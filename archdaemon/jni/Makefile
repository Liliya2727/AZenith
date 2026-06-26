CC = clang
TARGET = sys.azenith-service

CFLAGS = -Iinclude -DNDEBUG -Wall -Wextra -Werror \
         -pedantic-errors -Wpedantic \
         -O2 -std=c23 -fPIC -flto

LDFLAGS = -llog -flto

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

all: $(TARGET)

$(TARGET): $(SRCS)
	$(CC) $(CFLAGS) $(SRCS) $(LDFLAGS) -o $(TARGET)

clean:
	rm -f $(TARGET)
