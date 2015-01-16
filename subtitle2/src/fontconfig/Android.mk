LOCAL_PATH := $(call my-dir)

FT2_LIB_PATH	:= $(LOCAL_PATH)/freetype/lib/$(TARGET_ARCH_ABI)
EXPAT_LIB_PATH 	:= $(LOCAL_PATH)/expat/lib/$(TARGET_ARCH_ABI)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= ft2
LOCAL_SRC_FILES := freetype/lib/$(TARGET_ARCH_ABI)/libft2.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 	:= expat
LOCAL_SRC_FILES := expat/lib/$(TARGET_ARCH_ABI)/libexpat.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := fontconfig
LOCAL_CFLAGS := -DFONTCONFIG_PATH=\"/sdcard/.fcconfig\"
LOCAL_CFLAGS += -DFC_CACHEDIR=\"/sdcard/.fccache\"
LOCAL_CFLAGS += -DFC_DEFAULT_FONTS=\"/system/fonts\"
LOCAL_STATIC_LIBRARIES := ft2 expat
LOCAL_LDLIBS := -L$(FT2_LIB_PATH) -L$(EXPAT_LIB_PATH)

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/fontconfig \
	$(LOCAL_PATH)/freetype/include \
	$(LOCAL_PATH)/expat/include

LOCAL_SRC_FILES := \
	src/fcatomic.c \
	src/fcblanks.c \
	src/fccache.c \
	src/fccfg.c \
	src/fccharset.c \
	src/fcdbg.c \
	src/fcdefault.c \
	src/fcdir.c \
	src/fcformat.c \
	src/fcfreetype.c \
	src/fcfs.c \
	src/fcinit.c \
	src/fclang.c \
	src/fclist.c \
	src/fcmatch.c \
	src/fcmatrix.c \
	src/fcname.c \
	src/fcpat.c \
	src/fcserialize.c \
	src/fcstr.c \
	src/fcxml.c \
	src/ftglue.c
include $(BUILD_STATIC_LIBRARY)
