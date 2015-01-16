#!/bin/bash

BUILD_OSLES=yes
BUILD_NATIVEWINDOOW=yes
BUILD_RENDER_RGB565=no

if [ "$NDK"x = "x" ]; then
	NDK=D:/Software/android-ndk-r9d
fi

case `uname -sm` in
	Linux\ x86_64*)
		HOST_TAG="linux-x86_64"
		;;
	Linux\ x86*)
		HOST_TAG="linux-x86"
		;;
	CYGWIN*)
		HOST_TAG="windows"
		;;
	MINGW*)
		HOST_TAG="windows"
		;;
	*)
		echo "Unkown Platform: `uname -sm`"
		exit -1
		;;
esac

CC=$NDK/toolchains/arm-linux-androideabi-4.8/prebuilt/$HOST_TAG/bin/arm-linux-androideabi-g++
AR=$NDK/toolchains/arm-linux-androideabi-4.8/prebuilt/$HOST_TAG/bin/arm-linux-androideabi-ar
STRIP=$NDK/toolchains/arm-linux-androideabi-4.8/prebuilt/$HOST_TAG/bin/arm-linux-androideabi-strip
INCLUDES="-I../../platform -I../../platform/yuv2rgb -I../../player -I../../foundation/foundation_rext \
	-Id:/Software/android-ndk-r9d/sources/cpufeatures"
LIBS="-lavformat -lavcodec -lswscale -lavutil -lswresample -lavfilter -llog -lz"
PLATFORMPATH=../../platform
PLAYERPATH=../../player
ASM_OBJ="$PLATFORMPATH/i420_rgb.S $PLATFORMPATH/nv12_rgb.S $PLATFORMPATH/nv21_rgb.S"
CPU_FEATURE_SRC="d:/Software/android-ndk-r9d/sources/android/cpufeatures/cpu-features.c";
case $1 in
	x86)
	    CFLAGS="-DNDEBUG --sysroot=$NDK/platforms/android-9/arch-x86 -shared -fPIC -fpermissive  -mtune=atom  -D__STDC_CONSTANT_MACROS -DOS_ANDROID -Wno-deprecated-declarations -Wno-psabi"
		LIBSPATH="-L../../foundation/output/android/x86"
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8 -fno-exceptions -fno-rtti"
		CC=$NDK/toolchains/x86-4.8/prebuilt/$HOST_TAG/bin/i686-linux-android-g++
		AR=$NDK/toolchains/x86-4.8/prebuilt/$HOST_TAG/bin/i686-linux-android-ar
		STRIP=$NDK/toolchains/x86-4.8/prebuilt/$HOST_TAG/bin/i686-linux-android-strip
		OUTPUT=../../output/android/x86/libplayer_neon.so
		ASM_OBJ=""
		;;
	neon)
		CFLAGS="-DNDEBUG --sysroot=$NDK/platforms/android-9/arch-arm -shared -fPIC -mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad -march=armv7-a -fpermissive -D__STDC_CONSTANT_MACROS -DOS_ANDROID  -Wno-deprecated-declarations -Wno-psabi"
		LIBSPATH="-L../../foundation/output/android/v7_neon"
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8 -fno-exceptions -fno-rtti"
		OUTPUT=../../output/android/libplayer_neon.so
		;;
	neon_lgpl)
		CFLAGS="-DNDEBUG --sysroot=$NDK/platforms/android-9/arch-arm -shared -fPIC -mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad -march=armv7-a -fpermissive -D__STDC_CONSTANT_MACROS -DOS_ANDROID -Wno-deprecated-declarations -Wno-psabi"
		LIBSPATH="-L../../foundation/output/android/v7_neon_lgpl"
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8 -fno-exceptions -fno-rtti"
		OUTPUT=../../output/android/libplayer_neon_lgpl.so
		;;
	v6_vfp)
		CFLAGS="--sysroot=$NDK/platforms/android-9/arch-arm -shared -fPIC -mfloat-abi=softfp -mfpu=vfp -march=armv6 -fpermissive -D__STDC_CONSTANT_MACROS -DNDEBUG -DOS_ANDROID -Wno-deprecated-declarations -Wno-psabi"
		LIBSPATH="-L../../foundation/output/android/v6_vfp"
		EXTRA_LDFLAGS="-fno-exceptions -fno-rtti"
		OUTPUT=../../output/android/libplayer_v6_vfp.so
		;;
	*)
		echo Unknown target: $1
		exit
esac

CFLAGS="$CFLAGS -Wall -DUSE_NDK_SURFACE_REF -DUSE_AV_FILTER"

if [ ${BUILD_OSLES} == "yes" ]; then 
echo "build-in osles"
CFLAGS="$CFLAGS -DOSLES_IMPL"
OSLES_SRC="${PLATFORMPATH}/fifobuffer.cpp ${PLATFORMPATH}/oslesrender.cpp"
LIBS="$LIBS -lOpenSLES"
fi

if [ ${BUILD_NATIVEWINDOOW} == "yes" ]; then 
echo "build-in ndk native window"
CFLAGS="$CFLAGS -DNDK_NATIVE_WINDOW_IMPL"
LIBS="$LIBS -landroid"
fi

if [ ${BUILD_RENDER_RGB565} == "yes" ]; then 
echo "build-in rgb565 render"
CFLAGS="$CFLAGS -DRENDER_RGB565"
SWS_SRC="${PLATFORMPATH}/yuv2rgb/yuv2rgb16tab.c"
if [ $1 == "neon" ]; then 
SWS_SRC="$SWS_SRC ${PLATFORMPATH}/yuv2rgb/yuv420rgb565.s"
else
SWS_SRC="$SWS_SRC ${PLATFORMPATH}/yuv2rgb/yuv420rgb565c.c"
fi

fi

$CC \
	-o $OUTPUT \
	$CFLAGS \
	$INCLUDES \
	$EXTRA_LDFLAGS \
	$PLATFORMPATH/packetqueue.cpp \
	$PLATFORMPATH/list.cpp \
	$PLATFORMPATH/loop.cpp \
	$PLATFORMPATH/utils.cpp \
	$PLATFORMPATH/audiotrack_android.c \
	$PLATFORMPATH/surface_android.cpp \
	$PLATFORMPATH/log_android.c \
	$ASM_OBJ \
	$PLAYERPATH/ffstream.cpp \
	$PLAYERPATH/audioplayer.cpp \
	$PLAYERPATH/audiorender.cpp \
	$PLAYERPATH/ffplayer.cpp \
	$PLAYERPATH/ffrender.cpp \
	$PLAYERPATH/filesource.cpp \
	$OSLES_SRC \
	$SWS_SRC \
	$CPU_FEATURE_SRC \
	$LIBSPATH \
	$LIBS

if [ ${PIPESTATUS[0]} -ne 0 ]; then 
	echo "failed to build"; 
	exit 1
fi

cp $OUTPUT ${OUTPUT/libplayer_neon/debug/libplayer_neon}
$STRIP --strip-unneeded $OUTPUT
	
echo
echo
echo "libplayer build done"