LOCAL_PATH := $(call my-dir)

JNI_BASE 		:= meet/jni
MEDIA_BASE 		:= meet/libmedia
PLAYER_BASE 	:= meet/libppplayer
OS_BASE 		:= meet/platform-pp/os
DEVICE_BASE 	:= meet/platform-pp/device
PLATFORM_BASE 	:= meet/platform-pp
PREBUILT_BASE 	:= meet/prebuilt

UTILS_BASE 		:= meet/libutils
CUTILS_BASE 	:= meet/libcutils
LOG_BASE 		:= meet/liblog

ENGINE_BASE 	:= ../../engine2

########################[libpplog]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES := meet
LOCAL_SRC_FILES := $(JNI_BASE)/pplog.cpp
LOCAL_MODULE := pplog
include $(BUILD_STATIC_LIBRARY)

########################[liblog]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES 	:= meet
MY_SRC_FILES		:= logd_write.c logprint.c event_tag_map.c \
	fake_log_device.c
LOCAL_SRC_FILES 	:= $(addprefix $(LOG_BASE)/, $(MY_SRC_FILES))
LOCAL_MODULE 		:= log
#include $(BUILD_STATIC_LIBRARY)

########################[liblog_a14]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES 	:= meet
MY_SRC_FILES 		:= logd_write.c logprint.c event_tag_map.c \
	fake_log_device.c
LOCAL_SRC_FILES 	:= $(addprefix $(LOG_BASE)/a14/, $(MY_SRC_FILES))
LOCAL_MODULE 		:= log_a14
#include $(BUILD_STATIC_LIBRARY)

########################[libcutils]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES 		:= meet
MY_SRC_COMMON_FILES		:= \
	array.c \
	hashmap.c \
	atomic.c.arm \
	native_handle.c \
	buffer.c \
	socket_inaddr_any_server.c \
	socket_local_client.c \
	socket_local_server.c \
	socket_loopback_client.c \
	socket_loopback_server.c \
	socket_network_client.c \
	config_utils.c \
	cpu_info.c \
	load_file.c \
	open_memstream.c \
	strdup16to8.c \
	strdup8to16.c \
	record_stream.c \
	process_name.c \
	properties.c \
	threads.c \
	sched_policy.c \
	iosched_policy.c
MY_SRC_COMMON_FILES 	+= \
	abort_socket.c \
	mspace.c \
	selector.c \
	tztime.c \
	zygote.c
	
ifeq ($(TARGET_ARCH_ABI),armeabi)
MY_SRC_TARGET_FILES += $(CUTILS_BASE)/arch-arm/memset32.S
endif

LOCAL_SRC_FILES 		:= $(addprefix $(CUTILS_BASE)/, $(MY_SRC_COMMON_FILES))
LOCAL_SRC_FILES 		+= $(MY_SRC_TARGET_FILES)
LOCAL_STATIC_LIBRARIES 	:= liblog
LOCAL_MODULE 			:= cutils
#include $(BUILD_STATIC_LIBRARY)

########################[libcutils_a14]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES 		:= meet
MY_SRC_COMMON_FILES		:= \
	array.c \
	hashmap.c \
	atomic.c.arm \
	native_handle.c \
	buffer.c \
	socket_inaddr_any_server.c \
	socket_local_client.c \
	socket_local_server.c \
	socket_loopback_client.c \
	socket_loopback_server.c \
	socket_network_client.c \
	config_utils.c \
	cpu_info.c \
	load_file.c \
	open_memstream.c \
	strdup16to8.c \
	strdup8to16.c \
	record_stream.c \
	process_name.c \
	properties.c \
	threads.c \
	sched_policy.c \
	iosched_policy.c \
	system_properties.c

MY_SRC_COMMON_FILES += \
	abort_socket.c \
	mspace.c \
	selector.c \
	tztime.c \
	zygote.c

ifeq ($(TARGET_ARCH_ABI),armeabi)
MY_SRC_TARGET_FILES += $(CUTILS_BASE)/a14/arch-arm/memset32.S
endif

LOCAL_SRC_FILES 		:= $(addprefix $(CUTILS_BASE)/a14/, $(MY_SRC_COMMON_FILES))
LOCAL_SRC_FILES 		+= $(MY_SRC_TARGET_FILES)
LOCAL_STATIC_LIBRARIES := liblog_a14
LOCAL_MODULE := cutils_a14
#include $(BUILD_STATIC_LIBRARY)

########################[libutils]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES 		:= meet
MY_SRC_FILES 			:= \
	BufferedTextOutput.cpp \
	Debug.cpp \
	RefBase.cpp \
	SharedBuffer.cpp \
	Static.cpp \
	String8.cpp \
	String16.cpp \
	SystemClock.cpp \
	TextOutput.cpp \
	Threads.cpp \
	Timers.cpp \
	VectorImpl.cpp
LOCAL_SRC_FILES 		:= $(addprefix $(UTILS_BASE)/, $(MY_SRC_FILES))
LOCAL_STATIC_LIBRARIES 	:= libcutils
LOCAL_MODULE 			:= utils
include $(BUILD_STATIC_LIBRARY)

########################[libutils_a14]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES 		:= meet
MY_SRC_FILES 			:= \
	BufferedTextOutput.cpp \
	Debug.cpp \
	RefBase.cpp \
	SharedBuffer.cpp \
	Static.cpp \
	String8.cpp \
	String16.cpp \
	SystemClock.cpp \
	TextOutput.cpp \
	Threads.cpp \
	Timers.cpp \
	VectorImpl.cpp \
	Unicode.cpp
LOCAL_SRC_FILES 		:= $(addprefix $(UTILS_BASE)/a14/, $(MY_SRC_FILES))
LOCAL_STATIC_LIBRARIES 	:= libcutils_a14
LOCAL_MODULE 			:= utils_a14
#include $(BUILD_STATIC_LIBRARY)

########################[libmedia_common]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES	:= \
	meet \
	meet/android/frameworks/base/include \
	meet/android/frameworks/base/include/media/stagefright/openmax \
	meet/android/system/core/include \
	meet/android/hardware/libhardware/include
MY_SRC_FILES 		:= \
	MediaSource.cpp MetaData.cpp MediaDefs.cpp \
	MediaBuffer.cpp Utils.cpp ESDS.cpp
LOCAL_SRC_FILES 	:= $(addprefix $(PLAYER_BASE)/, $(MY_SRC_FILES))
LOCAL_MODULE := media_common
#include $(BUILD_STATIC_LIBRARY)

########################[libdevice]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES := \
	meet \
	meet/platform-pp \
	meet/android/frameworks/base/include \
	meet/android/frameworks/base/include/media/stagefright/openmax \
	meet/android/system/core/include \
	meet/android/hardware/libhardware/include
MY_SRC_FILES 	:= \
	samsung_gti9000.cpp \
	samsung_gti9020.cpp \
	samsung_gti9220.cpp \
	samsung_gts5660.cpp \
	xiaomi_mioneplus.cpp \
	huawei_u8800.cpp \
	lge_optimus2x.cpp \
	motorola_mx525.cpp \
	htc_htcz710t.cpp \
	htc_htcz710e.cpp \
	htc_htcx515d.cpp \
	htc_htca510e.cpp
LOCAL_SRC_FILES := $(addprefix $(DEVICE_BASE)/, $(MY_SRC_FILES))	
LOCAL_MODULE 	:= device
#include $(BUILD_STATIC_LIBRARY)

########################[libppplayer_a14]########################
# Dependent interfaces
# - MediaSource/ReadOptions (frameworks\base\include\media\stagefright\MediaSource.h)
# - MetaData (frameworks\base\include\media\stagefright\MetaData.h)
# - MediaBuffer (frameworks\base\include\media\stagefright\MediaBuffer.h)
# - ANativeWindow  (frameworks\base\include\ui\egl\Android_natives.h)
# - RefBase/SP (frameworks\base\include\utils\RefBase.h)

include $(CLEAR_VARS)
LOCAL_C_INCLUDES := meet $(ENGINE_BASE)
LOCAL_SRC_FILES  := \
	$(PLATFORM_BASE)/a14/PPPlatForm.cpp \
	$(PLATFORM_BASE)/a14/samsung_gti9000.cpp \
	$(PLATFORM_BASE)/a14/samsung_gti9300.cpp

LOCAL_SRC_FILES += \
	$(PLAYER_BASE)/a14/PPPlayer.cpp \
	$(PLAYER_BASE)/a14/PPMediaBufferGroup.cpp \
	$(PLAYER_BASE)/a14/AwesomePlayer.cpp \
	$(PLAYER_BASE)/a14/MediaExtractor.cpp \
	$(PLAYER_BASE)/a14/AudioPlayer.cpp \
	$(PLAYER_BASE)/a14/ppDataSource.cpp \
	$(PLAYER_BASE)/a14/ppExtractor.cpp \
	$(PLAYER_BASE)/a14/TimedEventQueue.cpp \
	$(PLAYER_BASE)/a14/DataSource.cpp \
	$(PLAYER_BASE)/a14/MPEG4Extractor.cpp \
	$(PLAYER_BASE)/a14/SampleIterator.cpp \
	$(PLAYER_BASE)/a14/SampleTable.cpp \
	$(PLAYER_BASE)/a14/MediaBufferGroup.cpp \

LOCAL_SRC_FILES += \
	$(PLAYER_BASE)/a14/MediaSource.cpp \
	$(PLAYER_BASE)/a14/MediaBuffer.cpp \
	$(PLAYER_BASE)/a14/MetaData.cpp \
	$(PLAYER_BASE)/a14/MediaDefs.cpp \
	$(PLAYER_BASE)/a14/ESDS.cpp
	
ifeq ($(TARGET_ARCH_ABI),armeabi)
MY_PLATFORM_LIBPATH := $(PREBUILT_BASE)/a14
else
MY_PLATFORM_LIBPATH := $(PREBUILT_BASE)/x86_14
endif
	
LOCAL_LDLIBS := -L$(MY_PLATFORM_LIBPATH) -lbinder -lmedia -lstagefright -lstagefright_foundation
LOCAL_STATIC_LIBRARIES := utils_a14 cutils_a14 log_a14
LOCAL_MODULE := ppplayer_a14
#include $(BUILD_SHARED_LIBRARY)

########################[libffplayer_neon]########################
include $(CLEAR_VARS)
#ifeq ($(NDK_DEBUG),1)
MY_SO_PREFIX := debug/
#else
#MY_SO_PREFIX := 
#endif
ifeq ($(TARGET_ARCH_ABI),armeabi)
LOCAL_SRC_FILES := $(ENGINE_BASE)/output/android/$(MY_SO_PREFIX)libplayer_neon.so
else
LOCAL_SRC_FILES := $(ENGINE_BASE)/output/android/x86/$(MY_SO_PREFIX)libplayer_neon.so
endif
LOCAL_MODULE := player_neon
include $(PREBUILT_SHARED_LIBRARY)

########################[libppmediaextractor-jni]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES := \
	meet \
	meet/android/frameworks/base/include \
	meet/android/frameworks/base/include/media/stagefright/openmax \
	meet/android/system/core/include \
	meet/android/hardware/libhardware/include \
	$(ENGINE_BASE)
MY_SRC_BASE_FILES 	:= \
	PPMediaExtractor.cpp \
	PPBox_Util.cpp \
	JNIHelp.c
MY_SRC_PLAYER_FILES	:= \
	DataSource.cpp \
	FileSource.cpp \
	MediaExtractor.cpp \
	PPMediaBufferGroup.cpp \
	ppDataSource.cpp \
	ppExtractor.cpp \
	a16/ABuffer.cpp \
	a16/AString.cpp \
	a16/AMessage.cpp \
	a16/AAtomizer.cpp \
	a16/NuPPMediaExtractor.cpp
LOCAL_SRC_FILES := $(addprefix $(JNI_BASE)/, $(MY_SRC_BASE_FILES))
LOCAL_SRC_FILES += $(addprefix $(PLAYER_BASE)/, $(MY_SRC_PLAYER_FILES))
LOCAL_CFLAGS := -DANDROID_PLATFORM=16
LOCAL_STATIC_LIBRARIES := libmedia_common liblog libutils libcutils
LOCAL_MODULE := ppmediaextractor-jni
#include $(BUILD_SHARED_LIBRARY)

########################[libmeet]########################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES 		:= meet $(ENGINE_BASE)
MY_SRC_FILES 			:= android_media_MediaPlayer.cpp PPBox_Util.cpp JNIHelp.c 
LOCAL_SRC_FILES 		:= $(addprefix $(JNI_BASE)/, $(MY_SRC_FILES))
#LOCAL_SRC_FILES			+= $(addprefix $(CUTILS_BASE)/cpufeatures/, cpu-features.c)
LOCAL_STATIC_LIBRARIES 	:= pplog cpufeatures
LOCAL_LDLIBS 			:= -llog
LOCAL_MODULE 			:= meet
include $(BUILD_SHARED_LIBRARY)

#######################[libsubtitle-jni]#######################
ifeq ($(TARGET_ARCH_ABI),armeabi)
MY_PLATFORM_LIBPATH=$(ENGINE_BASE)/subtitle2/output/android/libs/armeabi
else
MY_PLATFORM_LIBPATH=$(ENGINE_BASE)/subtitle2/output/android/libs/x86
endif

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
LOCAL_SRC_FILES := $(MY_PLATFORM_LIBPATH)/libft2.a
LOCAL_MODULE := ft2
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(MY_PLATFORM_LIBPATH)/libexpat.a
LOCAL_MODULE := expat
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_C_INCLUDES 		:= meet $(ENGINE_BASE)
LOCAL_SRC_FILES 		:= $(JNI_BASE)/SimpleSubTitleParser.cpp
LOCAL_LDLIBS 			:= -llog
LOCAL_STATIC_LIBRARIES 	:= subtitle ass tinyxml2 fontconfig ft2 expat
LOCAL_MODULE 			:= subtitle-jni
include $(BUILD_SHARED_LIBRARY)

$(call import-module,cpufeatures)