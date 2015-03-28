LOCAL_PATH := $(call my-dir)

BUILD_OSLES				:= 1
BUILD_NATIVEWINDOOW		:= 1
#BUILD_RENDER_RGB565		:= 1

ifeq ($(TARGET_ARCH_ABI),armeabi)
FFMPEG_PATH		:= ../../../../foundation/output/android/neon
else
FFMPEG_PATH		:= ../../../../foundation/output/android/x86
endif

PLAYERPATH		:= ../../../player
EXTRACTORPATH	:= ../../../extractor
PLATFORMPATH	:= ../../../platform
SUBTITLEPATH	:= ../../../../subtitle2/output/android

include $(CLEAR_VARS)
LOCAL_MODULE 	:= ffmpeg
LOCAL_SRC_FILES := $(FFMPEG_PATH)/lib/libffmpeg.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= lenthevcdec
LOCAL_SRC_FILES := $(FFMPEG_PATH)/lib/liblenthevcdec.so
#include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE			:= player_neon
LOCAL_C_INCLUDES		:= $(LOCAL_PATH)/$(FFMPEG_PATH)/include $(LOCAL_PATH)/$(SUBTITLEPATH)/include \
	$(LOCAL_PATH)/$(PLATFORMPATH) $(LOCAL_PATH)/$(PLATFORMPATH)/yuv2rgb \
	$(LOCAL_PATH)/$(PLAYERPATH) $(LOCAL_PATH)/$(EXTRACTORPATH) 
	
LOCAL_CFLAGS    		:= -Wall -DNDK_BUILD=1 -DUSE_NDK_SURFACE_REF -DUSE_AV_FILTER #-DTEST_PERFORMANCE -DTEST_PERFORMANCE_BITRATE -DNO_AUDIO_PLAY 
MY_SRC_PLAYER_FILES 	:= ffstream.cpp audioplayer.cpp audiorender.cpp ffplayer.cpp ffrender.cpp filesource.cpp ffextractor.cpp
MY_SRC_PLATFORM_FILES	:= audiotrack_android.c \
	surface_android.cpp log_android.c packetqueue.cpp list.cpp loop.cpp utils.cpp
ifdef BUILD_RENDER_RGB565
MY_SRC_YUV2RGB_FILES	= yuv2rgb16tab.c
ifeq ($(TARGET_ARCH_ABI),armeabi)
MY_SRC_YUV2RGB_FILES	+= yuv420rgb565.s.arm yuv2rgb565.cpp
else
MY_SRC_YUV2RGB_FILES	+= yuv420rgb565c.c
endif
LOCAL_CFLAGS    		+=-DRENDER_RGB565 -DARCH_ARM=1 -DHAVE_NEON=1
endif
ifeq ($(TARGET_ARCH_ABI),armeabi)
MY_SRC_PLATFORM_FILES	+= i420_rgb.S.arm nv12_rgb.S.arm nv21_rgb.S.arm
endif
ifdef BUILD_OSLES
MY_SRC_PLATFORM_FILES	+= fifobuffer.cpp oslesrender.cpp
LOCAL_CFLAGS			+= -DOSLES_IMPL -D__STDC_CONSTANT_MACROS
endif
ifdef BUILD_NATIVEWINDOOW
LOCAL_CFLAGS			+= -DNDK_NATIVE_WINDOW_IMPL
endif
LOCAL_SRC_FILES 		:= $(addprefix $(PLAYERPATH)/, $(MY_SRC_PLAYER_FILES))
LOCAL_SRC_FILES 		+= $(addprefix $(PLATFORMPATH)/, $(MY_SRC_PLATFORM_FILES))
LOCAL_SRC_FILES 		+= $(addprefix $(PLATFORMPATH)/yuv2rgb/, $(MY_SRC_YUV2RGB_FILES))
LOCAL_STATIC_LIBRARIES 	:= ffmpeg cpufeatures
#LOCAL_SHARED_LIBRARIES 	:= lenthevcdec
LOCAL_LDLIBS 			:= -llog -lz -landroid -L$(FFMPEG_PATH)/lib
ifdef BUILD_OSLES
LOCAL_LDLIBS			+= -lOpenSLES
endif
include $(BUILD_SHARED_LIBRARY)

$(call import-module,cpufeatures)

