LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := sys.azenith-service
LOCAL_SRC_FILES := \
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

LOCAL_C_INCLUDES := $(LOCAL_PATH)/include

LOCAL_CFLAGS := -DNDEBUG -Wall -Wextra -Werror \
                -pedantic-errors -Wpedantic \
                -O2 -std=c23 -fPIC -flto

LOCAL_LDFLAGS := -flto
LOCAL_LDLIBS  += -llog  

include $(BUILD_EXECUTABLE)
