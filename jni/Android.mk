LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := sys.azenith-service
LOCAL_SRC_FILES := \
    main.c \
    src/cmd_utils.c \
    src/AZenith_log.c \
    src/AZenith_profiler.c \
    src/file_utils.c \
    src/process_utils.c \
    src/misc_utils.c \
    src/game_preload.c \
    src/dumpsys.c \
    src/CLI.c \
    src/mlbb_handler.c \
    src/bypass_charge.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)/include

LOCAL_CFLAGS := -DNDEBUG -Wall -Wextra -Werror \
                -pedantic-errors -Wpedantic \
                -O2 -std=c23 -fPIC -flto

LOCAL_LDFLAGS := -flto
LOCAL_LDLIBS  += -llog  

include $(BUILD_EXECUTABLE)
