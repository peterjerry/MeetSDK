LOCAL_PATH := $(call my-dir)

BUILD_OSLES				:= 1
BUILD_NATIVEWINDOOW		:= 1
#BUILD_RENDER_RGB565	:= 1
#BUILD_PCM_DUMP			:= 1
BUILD_LIBRTMP			:= 1
#BUILD_TS_CONVERT		:= 1
#BUILD_ONE_LIB			:= 1
BUILD_FFPLAYER			:= 1
BUILD_FFEXTRACTOR		:= 1
BUILD_OMXPLAYER			:= 1

FDK_AAC_PATH	:= ../../../../foundation/thirdparty/fdk-aac/lib/android/$(TARGET_ARCH_ABI)
RTMPDUMP_PATH	:= ../../../../foundation/thirdparty/rtmpdump/lib/android/$(TARGET_ARCH_ABI)
X264_PATH		:= ../../../../foundation/thirdparty/x264/lib/android/$(TARGET_ARCH_ABI)
FFMPEG_PATH		:= ../../../../foundation/output/android/$(TARGET_ARCH_ABI)

PLAYERPATH		:= ../../../player
EXTRACTORPATH	:= ../../../extractor
PLATFORMPATH	:= ../../../platform
SUBTITLEPATH	:= ../../../../subtitle2/output/android

ifdef BUILD_PCM_DUMP
include $(CLEAR_VARS)
LOCAL_MODULE 	:= fdk-aac
LOCAL_SRC_FILES := $(FDK_AAC_PATH)/libfdk-aac.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= x264
LOCAL_SRC_FILES := $(X264_PATH)/libx264.a
include $(PREBUILT_STATIC_LIBRARY)
endif

ifdef BUILD_LIBRTMP
include $(CLEAR_VARS)
LOCAL_MODULE 	:= rtmp
LOCAL_SRC_FILES := $(RTMPDUMP_PATH)/librtmp.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= ssl
LOCAL_SRC_FILES := $(RTMPDUMP_PATH)/libssl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= crypto
LOCAL_SRC_FILES := $(RTMPDUMP_PATH)/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)
endif

include $(CLEAR_VARS)
LOCAL_MODULE 	:= ffmpeg
LOCAL_SRC_FILES := $(FFMPEG_PATH)/lib/libffmpeg.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= lenthevcdec
LOCAL_SRC_FILES := $(FFMPEG_PATH)/lib/liblenthevcdec.so
#include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE			:= player
LOCAL_C_INCLUDES		:= $(LOCAL_PATH)/$(FFMPEG_PATH)/include $(LOCAL_PATH)/$(SUBTITLEPATH)/include \
	$(LOCAL_PATH)/$(PLATFORMPATH) $(LOCAL_PATH)/$(PLATFORMPATH)/yuv2rgb $(LOCAL_PATH)/$(PLATFORMPATH)/clsocket \
	$(LOCAL_PATH)/$(PLAYERPATH) $(LOCAL_PATH)/$(EXTRACTORPATH) 
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_C_INCLUDES		+= $(LOCAL_PATH)/$(PLATFORMPATH)/libyuv/jni/include
endif	
	
LOCAL_CFLAGS    		:= -Wall -DNDK_BUILD=1 -DUSE_NDK_SURFACE_REF -DUSE_AV_FILTER -DUSE_SWSCALE
MY_SRC_PLAYER_FILES 	:= common.cpp ffconverter.cpp
ifdef BUILD_FFPLAYER
MY_SRC_PLAYER_FILES 	+= ffstream.cpp audioplayer.cpp audiorender.cpp ffplayer.cpp androidrender.cpp \
	filesource.cpp
endif
ifdef BUILD_FFEXTRACTOR
MY_SRC_PLAYER_FILES 	+= ffextractor.cpp
endif
ifdef BUILD_OMXPLAYER
MY_SRC_PLAYER_FILES 	+= omxplayer.cpp
endif
ifdef BUILD_TS_CONVERT
MY_SRC_PLAYER_FILES 	+= apFormatConverter.cpp
LOCAL_CFLAGS			+= -DBUILD_TS_CONVERT
endif
MY_SRC_PLATFORM_FILES	= log_android.c packetqueue.cpp list.cpp utils.cpp
ifdef BUILD_FFPLAYER
MY_SRC_PLATFORM_FILES	+= loop.cpp
endif
MY_SRC_SOCKET_FILES		:= SimpleSocket.cpp ActiveSocket.cpp
ifdef BUILD_PCM_DUMP
MY_SRC_PLAYER_FILES 	+= apAudioEncoder.cpp
MY_SRC_PLATFORM_FILES	+= apProxyUDP.cpp
endif
ifdef BUILD_RENDER_RGB565
$(info build render rgb565)
MY_SRC_YUV2RGB_FILES	= yuv2rgb16tab.c
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
MY_SRC_YUV2RGB_FILES	+= yuv420rgb565.s.arm yuv2rgb565.cpp
else
MY_SRC_YUV2RGB_FILES	+= yuv420rgb565c.c
endif
LOCAL_CFLAGS    		+=-DRENDER_RGB565 -DARCH_ARM=1 -DHAVE_NEON=1
endif
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
MY_SRC_PLATFORM_FILES	+= i420_rgb.S.arm nv12_rgb.S.arm nv21_rgb.S.arm
endif
ifdef BUILD_OSLES
MY_SRC_PLATFORM_FILES	+= fifobuffer.cpp oslesrender.cpp
LOCAL_CFLAGS			+= -DOSLES_IMPL -D__STDC_CONSTANT_MACROS
endif
ifdef BUILD_NATIVEWINDOOW
LOCAL_CFLAGS			+= -DNDK_NATIVE_WINDOW_IMPL
endif
ifdef BUILD_PCM_DUMP
LOCAL_CFLAGS			+= -DPCM_DUMP -DPROTOCOL_RTMP
endif
LOCAL_SRC_FILES 		:= $(addprefix $(PLAYERPATH)/, $(MY_SRC_PLAYER_FILES))
LOCAL_SRC_FILES 		+= $(addprefix $(PLATFORMPATH)/, $(MY_SRC_PLATFORM_FILES))
LOCAL_SRC_FILES 		+= $(addprefix $(PLATFORMPATH)/yuv2rgb/, $(MY_SRC_YUV2RGB_FILES))
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_SRC_FILES 		+= $(addprefix $(PLATFORMPATH)/libyuv/jni/source/, row_neon64.cc)
endif
ifdef BUILD_PCM_DUMP
LOCAL_SRC_FILES 		+= $(addprefix $(PLATFORMPATH)/clsocket/, $(MY_SRC_SOCKET_FILES))
endif
LOCAL_STATIC_LIBRARIES 	:= ffmpeg cpufeatures
#LOCAL_SHARED_LIBRARIES 	:= lenthevcdec
ifdef BUILD_ONE_LIB
LOCAL_CFLAGS			+= -DBUILD_ONE_LIB
else
LOCAL_LDLIBS 			:= -llog -lz -landroid -L$(FFMPEG_PATH)/lib
ifdef BUILD_OSLES
LOCAL_LDLIBS			+= -lOpenSLES
endif
ifdef BUILD_OMXPLAYER
LOCAL_LDLIBS			+= -lOpenMAXAL
endif
endif
ifdef BUILD_PCM_DUMP
LOCAL_STATIC_LIBRARIES 	+= fdk-aac x264
endif
ifdef BUILD_LIBRTMP
LOCAL_STATIC_LIBRARIES 	+= rtmp ssl crypto
endif
ifdef BUILD_ONE_LIB
include $(BUILD_STATIC_LIBRARY)
else
include $(BUILD_SHARED_LIBRARY)
endif

$(call import-module,cpufeatures)

