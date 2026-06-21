LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := sys.azenith-preloadbin
LOCAL_SRC_FILES := \
    main.c \

LOCAL_CFLAGS := -DNDEBUG \
                -O2 -std=c23 -fPIC -flto

LOCAL_LDFLAGS := -flto

include $(BUILD_EXECUTABLE)
