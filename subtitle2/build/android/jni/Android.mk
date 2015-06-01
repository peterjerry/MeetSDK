LOCAL_PATH:= $(call my-dir)

######################## expat ############################
include $(CLEAR_VARS)
LOCAL_MODULE:= expat
SRC_PATH = ../../../src/expat
#SRC_PATH = $(LOCAL_PATH)/../../../../src/expat
LOCAL_C_INCLUDES = ../../src/expat
#LOCAL_C_INCLUDES = $(LOCAL_PATH)/../../../src/expat
common_SRC_FILES := \
	lib/xmlparse.c \
	lib/xmlrole.c \
	lib/xmltok.c
LOCAL_SRC_FILES := $(addprefix $(SRC_PATH)/, $(common_SRC_FILES))
LOCAL_CFLAGS += \
    -Wall \
    -Wmissing-prototypes -Wstrict-prototypes \
    -Wno-unused-parameter -Wno-missing-field-initializers \
    -fexceptions \
    -DHAVE_EXPAT_CONFIG_H
include $(BUILD_STATIC_LIBRARY)

####################### freetype ##########################
include $(CLEAR_VARS)
LOCAL_MODULE:= ft2
ifeq ($(TARGET_ARCH_ABI),armeabi)
LOCAL_ARM_MODE := arm
endif
SRC_PATH = ../../../src/freetype
LOCAL_C_INCLUDES += \
	../../src/freetype/builds \
	../../src/freetype/include
common_SRC_FILES:= \
	src/base/ftbbox.c \
	src/base/ftbitmap.c \
	src/base/ftglyph.c \
	src/base/ftstroke.c \
	src/base/ftxf86.c \
	src/base/ftbase.c \
	src/base/ftsystem.c \
	src/base/ftinit.c \
	src/base/ftgasp.c \
	src/raster/raster.c \
	src/sfnt/sfnt.c \
	src/smooth/smooth.c \
	src/autofit/autofit.c \
	src/truetype/truetype.c \
	src/cff/cff.c \
	src/psnames/psnames.c \
	src/pshinter/pshinter.c
LOCAL_SRC_FILES := $(addprefix $(SRC_PATH)/, $(common_SRC_FILES))

LOCAL_EXPORT_C_INCLUDES += $(LOCAL_C_INCLUDES)

LOCAL_CFLAGS += -W -Wall
LOCAL_CFLAGS += "-DDARWIN_NO_CARBON"
LOCAL_CFLAGS += "-DFT2_BUILD_LIBRARY"
LOCAL_CFLAGS += -O2

include $(BUILD_STATIC_LIBRARY)

######################## fontconfig ######################
include $(CLEAR_VARS)
LOCAL_MODULE := fontconfig
LOCAL_CFLAGS := -DFONTCONFIG_PATH=\"/sdcard/.fcconfig\"
LOCAL_CFLAGS += -DFC_CACHEDIR=\"/sdcard/.fccache\"
LOCAL_CFLAGS += -DFC_DEFAULT_FONTS=\"/system/fonts\"

SRC_PATH = ../../../src/fontconfig
LOCAL_C_INCLUDES := \
	../../src/fontconfig \
	../../src/freetype/include \
	../../src/expat/lib

common_SRC_FILES := \
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
LOCAL_SRC_FILES := $(addprefix $(SRC_PATH)/, $(common_SRC_FILES))
LOCAL_STATIC_LIBRARIES := expat ft2
include $(BUILD_STATIC_LIBRARY)

######################## iconv ######################
include $(CLEAR_VARS) 
LOCAL_MODULE    := iconv 
LOCAL_CFLAGS    := \
	-Wno-multichar \
	-D_ANDROID \
	-DBUILDING_LIBICONV \
	-DIN_LIBRARY \
	-DLIBDIR="\"c\""
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/../../../src/iconv/ \
    $(LOCAL_PATH)/../../../src/iconv/include/ \
    $(LOCAL_PATH)/../../../src/iconv/lib/ \
	$(LOCAL_PATH)/../../../src/iconv/libcharset/include

LOCAL_SRC_FILES := \
     ../../../src/iconv/lib/iconv.c \
     ../../../src/iconv/lib/relocatable.c \
     ../../../src/iconv/libcharset/lib/localcharset.c
include $(BUILD_STATIC_LIBRARY) 

######################## enca ######################
include $(CLEAR_VARS) 
LOCAL_MODULE    := enca
SRC_PATH = ../../../src/enca
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/../../../src/enca/ \
    $(LOCAL_PATH)/../../../src/enca/lib/

common_SRC_FILES := \
	lib/common.c \
	lib/ctype.c \
	lib/enca.c \
	lib/encnames.c \
	lib/filters.c \
	lib/guess.c \
	lib/lang.c \
	lib/lang_be.c \
	lib/lang_bg.c \
	lib/lang_cs.c \
	lib/lang_et.c \
	lib/lang_hr.c \
	lib/lang_hu.c \
	lib/lang_lt.c \
	lib/lang_lv.c \
	lib/lang_pl.c \
	lib/lang_ru.c \
	lib/lang_sk.c \
	lib/lang_sl.c \
	lib/lang_uk.c \
	lib/lang_zh.c \
	lib/multibyte.c \
	lib/pair.c \
	lib/unicodemap.c \
	lib/utf8_double.c
	
LOCAL_SRC_FILES := $(addprefix $(SRC_PATH)/, $(common_SRC_FILES))
include $(BUILD_STATIC_LIBRARY) 

######################## ass ######################
include $(CLEAR_VARS)
LOCAL_MODULE := ass
LOCAL_C_INCLUDES := \
	../../src/fontconfig/ \
	../../src/freetype/include \
	../../src/iconv/include \
	../../src/enca/lib \
	../../src/libass \
	../../src/libass/libass
SRC_PATH = ../../../src/libass
common_SRC_FILES := \
	libass/ass.c \
	libass/ass_library.c \
	libass/libass_glue.c \
	libass/ass_strtod.c
LOCAL_SRC_FILES := $(addprefix $(SRC_PATH)/, $(common_SRC_FILES))
LOCAL_STATIC_LIBRARIES := fontconfig iconv enca
include $(BUILD_STATIC_LIBRARY)

######################## tinyxml2 ######################
include $(CLEAR_VARS)
LOCAL_MODULE:= tinyxml2
SRC_PATH = ../../../src/tinyxml2
common_SRC_FILES:= tinyxml2.cpp
LOCAL_SRC_FILES := $(addprefix $(SRC_PATH)/, $(common_SRC_FILES))
include $(BUILD_STATIC_LIBRARY)

######################## subtitle ######################
include $(CLEAR_VARS)
LOCAL_MODULE := subtitle
LOCAL_C_INCLUDES := \
	../../src/libass \
	../../src/tinyxml2
SRC_PATH = ../../../src/subtitle
common_SRC_FILES := \
	subtitle.cpp \
	simpletextsubtitle.cpp \
	stssegment.cpp
LOCAL_SRC_FILES 		:= $(addprefix $(SRC_PATH)/, $(common_SRC_FILES))
LOCAL_STATIC_LIBRARIES := tinyxml2 ass
include $(BUILD_STATIC_LIBRARY)

######################## fake ######################
include $(CLEAR_VARS)
LOCAL_MODULE := fake
LOCAL_C_INCLUDES = ../../../src/subtitle
SRC_PATH = ../../../src/subtitle
common_SRC_FILES := \
	fake.cpp
LOCAL_SRC_FILES 		:= $(addprefix $(SRC_PATH)/, $(common_SRC_FILES))
LOCAL_STATIC_LIBRARIES 	:= iconv enca subtitle
LOCAL_LDLIBS 			:= -llog
include $(BUILD_SHARED_LIBRARY)
