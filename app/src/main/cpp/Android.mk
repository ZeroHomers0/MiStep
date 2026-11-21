LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := onesteplite_raw
LOCAL_SRC_FILES := native_raw_input.cpp
LOCAL_CPPFLAGS := -std=c++17 -fno-exceptions -fno-rtti -fvisibility=hidden -Wall -Wextra -Werror
LOCAL_LDFLAGS := -Wl,-z,max-page-size=16384 -Wl,-z,defs -Wl,-z,relro,-z,now
LOCAL_LDLIBS := -llog -ldl -lm
include $(BUILD_SHARED_LIBRARY)
