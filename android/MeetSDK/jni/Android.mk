LOCAL_PATH := $(call my-dir)

JNI_BASE 		:= meet

ENGINE_BASE 	:= ../../engine2
SUBTITLE_BASE	:= ../../subtitle2

#BUILD_ONE_LIB	:= 1

ifeq ($(TARGET_ARCH_ABI),armeabi)
FFMPEG_PATH		:= ../../../foundation/output/android/neon
else
FFMPEG_PATH		:= ../../../foundation/output/android/x86
endif

ifeq ($(TARGET_ARCH_ABI),armeabi)
FDK_AAC_PATH	:= ../../../foundation/thirdparty/fdk-aac/lib/android/armeabi-v7a
else
FDK_AAC_PATH	:= ../../../foundation/thirdparty/fdk-aac/lib/android/x86
endif

RTMPDUMP_PATH	:= ../../../foundation/thirdparty/rtmpdump/lib/android/$(TARGET_ARCH_ABI)
X264_PATH		:= ../../../foundation/thirdparty/x264/lib/android/$(TARGET_ARCH_ABI)

########################[libpplog]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES := meet
LOCAL_SRC_FILES := $(JNI_BASE)/pplog.cpp
LOCAL_MODULE := pplog
include $(BUILD_STATIC_LIBRARY)

########################[libffplayer_neon]########################
include $(CLEAR_VARS)
#ifeq ($(NDK_DEBUG),1)
MY_SO_PREFIX := debug/
#else
#MY_SO_PREFIX := 
#endif

ifdef BUILD_ONE_LIB
LOCAL_SRC_FILES := ../$(ENGINE_BASE)/output/android/$(TARGET_ARCH_ABI)/$(MY_SO_PREFIX)libplayer_neon.a
LOCAL_MODULE := player_neon
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= ffmpeg
LOCAL_SRC_FILES := $(FFMPEG_PATH)/lib/libffmpeg.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= fdk-aac
LOCAL_SRC_FILES := $(FDK_AAC_PATH)/libfdk-aac.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= ssl
LOCAL_SRC_FILES := $(RTMPDUMP_PATH)/libssl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= crypto
LOCAL_SRC_FILES := $(RTMPDUMP_PATH)/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= x264
LOCAL_SRC_FILES := $(X264_PATH)/libx264.a
include $(PREBUILT_STATIC_LIBRARY)

else
LOCAL_SRC_FILES := ../$(ENGINE_BASE)/output/android/$(TARGET_ARCH_ABI)/$(MY_SO_PREFIX)libplayer_neon.so
LOCAL_MODULE := player_neon
include $(PREBUILT_SHARED_LIBRARY)
endif

########################[libmeet]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES 		:= meet $(ENGINE_BASE) $(SUBTITLE_BASE)/output/android/include
ifdef BUILD_ONE_LIB
LOCAL_CFLAGS    		+= -DBUILD_ONE_LIB
endif
#LOCAL_CFLAGS    		+= -DUSE_TS_CONVERT
LOCAL_CFLAGS    		+= -DBUILD_FFEXTRACTOR -DBUILD_FFPLAYER
MY_SRC_FILES 			:= cpuext.cpp jniUtils.cpp libplayer.cpp FFMediaPlayer.cpp FFMediaExtractor.cpp
#MY_SRC_FILES			+= native_convert.cpp
LOCAL_SRC_FILES 		:= $(addprefix $(JNI_BASE)/, $(MY_SRC_FILES))
LOCAL_STATIC_LIBRARIES 	:= pplog cpufeatures
LOCAL_LDLIBS 			:= -llog
ifdef BUILD_ONE_LIB
LOCAL_STATIC_LIBRARIES 	+= player_neon ffmpeg fdk-aac x264 ssl crypto
LOCAL_LDLIBS 			+= -lz -landroid -lOpenSLES -L../$(ENGINE_BASE)/output/android/$(TARGET_ARCH_ABI)/$(MY_SO_PREFIX) \
	-L$(FFMPEG_PATH)/lib -L$(FDK_AAC_PATH) -L$(RTMPDUMP_PATH) -L$(X264_PATH)
endif
LOCAL_MODULE 			:= meet
include $(BUILD_SHARED_LIBRARY)

#######################[libsubtitle-jni]#######################
MY_PLATFORM_LIBPATH	= ../$(SUBTITLE_BASE)/output/android/lib/$(TARGET_ARCH_ABI)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(MY_PLATFORM_LIBPATH)/libsubtitle.a
LOCAL_MODULE := subtitle
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(MY_PLATFORM_LIBPATH)/libass.a
LOCAL_MODULE := ass
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(MY_PLATFORM_LIBPATH)/libtinyxml2.a
LOCAL_MODULE := tinyxml2
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(MY_PLATFORM_LIBPATH)/libfontconfig.a
LOCAL_MODULE := fontconfig
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(MY_PLATFORM_LIBPATH)/libiconv.a
LOCAL_MODULE := iconv
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(MY_PLATFORM_LIBPATH)/libenca.a
LOCAL_MODULE := enca
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(MY_PLATFORM_LIBPATH)/libft2.a
LOCAL_MODULE := ft2
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(MY_PLATFORM_LIBPATH)/libexpat.a
LOCAL_MODULE := expat
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_C_INCLUDES 		:= $(SUBTITLE_BASE)/output/android/include
LOCAL_SRC_FILES 		:= $(JNI_BASE)/SimpleSubTitleParser.cpp
LOCAL_LDLIBS 			:= -llog
LOCAL_STATIC_LIBRARIES 	:= subtitle ass iconv enca tinyxml2 fontconfig ft2 expat  
LOCAL_MODULE 			:= subtitle-jni
include $(BUILD_SHARED_LIBRARY)

$(call import-module,cpufeatures)