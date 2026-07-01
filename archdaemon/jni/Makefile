CC     = clang
TARGET = sys.azenith-service

SRCS = \
    main.c \
    src/androidffi/*.c \
    src/charging/*.c \
    src/cmdline/*.c \
    src/cmdutils/*.c \
    src/fileutils/*.c \
    src/gamelist/*.c \
    src/handler/*.c \
    src/init/*.c \
    src/inotifyhandler/*.c \
    src/misc/*.c \
    src/pidmgr/*.c \
    src/preloads/*.c \
    src/profiling/*.c \
    src/system/*.c

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