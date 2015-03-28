LOCAL_PATH := $(call my-dir)

JNI_BASE 		:= meet

ENGINE_BASE 	:= ../../engine2
SUBTITLE_BASE	:= ../../subtitle2

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
LOCAL_SRC_FILES := ../$(ENGINE_BASE)/output/android/$(TARGET_ARCH_ABI)/$(MY_SO_PREFIX)libplayer_neon.so
LOCAL_MODULE := player_neon
include $(PREBUILT_SHARED_LIBRARY)

########################[libmeet]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES 		:= meet $(ENGINE_BASE) $(SUBTITLE_BASE)/output/android/include
MY_SRC_FILES 			:= cpuext.cpp jniUtils.cpp FFMediaExtractor.cpp FFMediaPlayer.cpp
LOCAL_SRC_FILES 		:= $(addprefix $(JNI_BASE)/, $(MY_SRC_FILES))
LOCAL_STATIC_LIBRARIES 	:= pplog cpufeatures
LOCAL_LDLIBS 			:= -llog
LOCAL_MODULE 			:= meet
include $(BUILD_SHARED_LIBRARY)

#######################[libsubtitle-jni]#######################
MY_PLATFORM_LIBPATH	= ../$(SUBTITLE_BASE)/output/android/libs/$(TARGET_ARCH_ABI)

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