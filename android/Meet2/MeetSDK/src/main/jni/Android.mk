LOCAL_PATH := $(call my-dir)

JNI_BASE 		:= meet

ENGINE_BASE 	:= ../../../../../engine2

SUBTITLE_BASE	:= ../../../../../subtitle2

BUILD_ONE_LIB		:= 1
BUILD_FFPLAYER		:= 1
BUILD_FFEXTRACTOR	:= 1
#BUILD_OMXPLAYER		:= 1
#BUILD_PCM_DUMP			:= 1
BUILD_LIBRTMP			:= 1
#BUILD_TS_CONVERT	:= 1
BUILD_GLES			:= 1

FOUNDATION_BASE	:= ../../../../../../foundation
FFMPEG_PATH		:= $(FOUNDATION_BASE)/output/android/$(TARGET_ARCH_ABI)
FDK_AAC_PATH	:= $(FOUNDATION_BASE)/thirdparty/fdk-aac/lib/android/$(TARGET_ARCH_ABI)
RTMPDUMP_PATH	:= $(FOUNDATION_BASE)/thirdparty/rtmpdump/lib/android/$(TARGET_ARCH_ABI)
X264_PATH		:= $(FOUNDATION_BASE)/thirdparty/x264/lib/android/$(TARGET_ARCH_ABI)

########################[libpplog]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES := meet
LOCAL_SRC_FILES := $(JNI_BASE)/pplog.cpp
LOCAL_MODULE := pplog
include $(BUILD_STATIC_LIBRARY)

########################[libffplayer]########################
include $(CLEAR_VARS)
#ifeq ($(NDK_DEBUG),1)
MY_SO_PREFIX := debug/
#else
#MY_SO_PREFIX := 
#endif

ifdef BUILD_ONE_LIB
LOCAL_SRC_FILES := ../$(ENGINE_BASE)/output/android/$(TARGET_ARCH_ABI)/libplayer.a
LOCAL_MODULE := player
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= ffmpeg
LOCAL_SRC_FILES := $(FFMPEG_PATH)/lib/libffmpeg.a
include $(PREBUILT_STATIC_LIBRARY)

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
LOCAL_MODULE 	:= ssl
LOCAL_SRC_FILES := $(RTMPDUMP_PATH)/libssl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= crypto
LOCAL_SRC_FILES := $(RTMPDUMP_PATH)/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= rtmp
LOCAL_SRC_FILES := $(RTMPDUMP_PATH)/librtmp.a
include $(PREBUILT_STATIC_LIBRARY)
endif

else
LOCAL_SRC_FILES := ../$(ENGINE_BASE)/output/android/$(TARGET_ARCH_ABI)/$(MY_SO_PREFIX)libplayer.so
LOCAL_MODULE := player
include $(PREBUILT_SHARED_LIBRARY)
endif

########################[libmeet]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES 		:= meet $(ENGINE_BASE) $(ENGINE_BASE)/player $(ENGINE_BASE)/platform $(SUBTITLE_BASE)/output/android/include
ifdef BUILD_ONE_LIB
LOCAL_CFLAGS    		+= -DBUILD_ONE_LIB
endif
MY_SRC_FILES 			:= cpuext.cpp jniUtils.cpp libplayer.cpp
ifdef BUILD_FFPLAYER
MY_SRC_FILES 			+= FFMediaPlayer.cpp
LOCAL_CFLAGS    		+= -DBUILD_FFPLAYER
endif
ifdef BUILD_FFEXTRACTOR
MY_SRC_FILES 			+= FFMediaExtractor.cpp
LOCAL_CFLAGS    		+= -DBUILD_FFEXTRACTOR
endif
ifdef BUILD_OMXPLAYER
MY_SRC_FILES 			+= OMXMediaPlayer.cpp
LOCAL_CFLAGS    		+= -DBUILD_OMXPLAYER
endif
ifdef BUILD_TS_CONVERT
LOCAL_CFLAGS    		+= -DBUILD_TS_CONVERT
MY_SRC_FILES			+= native_convert.cpp
endif
ifdef BUILD_GLES
MY_SRC_FILES			+= gles2.cpp
LOCAL_CFLAGS    		+= -DBUILD_GLES
endif
LOCAL_SRC_FILES 		:= $(addprefix $(JNI_BASE)/, $(MY_SRC_FILES))
LOCAL_STATIC_LIBRARIES 	:= pplog cpufeatures
LOCAL_LDLIBS 			:= -llog
ifdef BUILD_ONE_LIB
LOCAL_STATIC_LIBRARIES 	+= player ffmpeg
ifdef BUILD_PCM_DUMP
LOCAL_STATIC_LIBRARIES 	+= fdk-aac x264
endif
ifdef BUILD_LIBRTMP
LOCAL_STATIC_LIBRARIES 	+= librtmp ssl crypto
endif
ifdef BUILD_GLES
LOCAL_LDLIBS			+= -lGLESv2
endif
LOCAL_LDLIBS 			+= -lz -landroid -lOpenSLES -L../$(ENGINE_BASE)/output/android/$(TARGET_ARCH_ABI)/$(MY_SO_PREFIX) \
	-L$(FFMPEG_PATH)/lib
ifdef BUILD_PCM_DUMP
LOCAL_LDLIBS 			+= -L$(FDK_AAC_PATH) -L$(X264_PATH)
endif
ifdef BUILD_LIBRTMP
LOCAL_LDLIBS 			+= -L$(RTMPDUMP_PATH)
endif
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