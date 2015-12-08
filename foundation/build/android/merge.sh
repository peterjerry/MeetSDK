#!/bin/bash

OS=`uname`
case $OS in
	MINGW32_NT-6.1)
		HOST=windows
		NDK=D:/Software/android-ndk-r10d
		export TMPDIR=D:/Tmp
		;;
	Linux)
		HOST=linux-x86_64
		NDK=$NDK_HOME
		;;
	*)
		echo Unknown os: $OS
		exit
esac

cd ../../foundation_rext

#default use arm
ARCH=arm

case $1 in
	x86)
		CONFIG_SRC=../build/android/config_x86
		TARGET1=../output/android/x86
		ARCH=x86
		;;
	neon)
		CONFIG_SRC=../build/android/config_neon
		TARGET1=../output/android/neon
		;;
	arm64-v8a)
		CONFIG_SRC=../build/android/config_arm64-v8a
		TARGET1=../output/android/arm64-v8a
		ARCH=aarch64
		;;
	tegra2)
		CONFIG_SRC=../build/android/config_tegra2
		TARGET1=../output/android/v7_vfpv3d16
		;;
	*)
		echo Unknown target: $1
		exit
esac

ASM_OBJ="libavutil/$ARCH/*.o libavcodec/$ARCH/*.o"

HOME_FOLDER=`pwd`
FDK_AAC_HOME=$HOME_FOLDER/../thirdparty/fdk-aac
RTMPDUMP_HOME=$HOME_FOLDER/../thirdparty/rtmpdump
X264_HOME=$HOME_FOLDER/../thirdparty/x264

if [ $ARCH == 'arm' ]
then
	PLATFORM=$NDK/platforms/android-9/arch-arm
	PREBUILT=$NDK/toolchains/arm-linux-androideabi-4.8/prebuilt/$HOST
	CROSS_PREFIX=$PREBUILT/bin/arm-linux-androideabi-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstack-protector -fstrict-aliasing"
	FDK_AAC_LIB=$FDK_AAC_HOME/lib/android/armeabi-v7a
	RTMPDUMP_LIB=$RTMPDUMP_HOME/lib/android/armeabi
	X264_LIB=$X264_HOME/lib/android/armeabi
	OPTFLAGS="-O2"
	ASM_OBJ="$ASM_OBJ libswresample/$ARCH/*.o"
elif [ $ARCH == 'aarch64' ]
then
	PLATFORM=$NDK/platforms/android-21/arch-arm64
	PREBUILT=$NDK/toolchains/aarch64-linux-android-4.9/prebuilt/$HOST
	CROSS_PREFIX=$PREBUILT/bin/aarch64-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstrict-aliasing"
	RTMPDUMP_LIB=$RTMPDUMP_HOME/lib/android/arm64-v8a
	X264_LIB=$X264_HOME/lib/android/arm64-v8a
	OPTFLAGS="-O2 -fno-pic"
elif [ $ARCH == 'x86' ]
then
	PLATFORM=$NDK/platforms/android-9/arch-x86
	PREBUILT=$NDK/toolchains/x86-4.8/prebuilt/$HOST
	CROSS_PREFIX=$PREBUILT/bin/i686-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstrict-aliasing"
	OPTFLAGS="-O2 -fno-pic"
	FDK_AAC_LIB=$FDK_AAC_HOME/lib/android/x86
	RTMPDUMP_LIB=$RTMPDUMP_HOME/lib/android/x86
	X264_LIB=$X264_HOME/lib/android/x86
	ASM_OBJ="$ASM_OBJ libswresample/$ARCH/*.o libswscale/x86/*.o libavfilter/x86/*.o"
elif [ $ARCH == 'mips' ]
then
	PLATFORM=$NDK/platforms/android-9/arch-mips
	PREBUILT=$NDK/toolchains/mipsel-linux-android-4.8/prebuilt/$HOST
	CROSS_PREFIX=$PREBUILT/bin/mipsel-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fno-strict-aliasing -fmessage-length=0 -fno-inline-functions-called-once -frerun-cse-after-loop -frename-registers"
	OPTFLAGS="-O2"
fi

if [ ${3}x == 'enc'x ]; then
	EXTRA_LIB="$FDK_AAC_LIB/libfdk-aac.a"
	EXTRA_LIB="$EXTRA_LIB $RTMPDUMP_LIB/libssl.a $RTMPDUMP_LIB/libcrypto.a"
	EXTRA_LIB="$EXTRA_LIB $X264_LIB/libx264.a"
#	EXTRA_LIB="$EXTRA_LIB $RTMPDUMP_LIB/librtmp.a $RTMPDUMP_LIB/libssl.a $RTMPDUMP_LIB/libcrypto.a"
fi

MY_CC=${CROSS_PREFIX}gcc
MY_AR=${CROSS_PREFIX}ar
MY_STRIP=${CROSS_PREFIX}strip

rm -f $TARGET1/lib/libffmpeg*.*

# static lib
$MY_AR -r $TARGET1/lib/libffmpeg.a libavutil/*.o libavcodec/*.o \
	libavformat/*.o libswresample/*.o libswscale/*.o libavfilter/*.o compat/*.o $ASM_OBJ
	
# shared lib
$MY_CC -lm -lz -shared --sysroot=$PLATFORM -Wl,--no-undefined -Wl,-z,noexecstack \
	libavutil/*.o libavcodec/*.o \
	libavformat/*.o libswresample/*.o libswscale/*.o libavfilter/*.o compat/*.o $ASM_OBJ \
	$EXTRA_LIB -o $TARGET1/lib/libffmpeg.so

cp ./config.h $TARGET1/
cp $TARGET1/lib/libffmpeg.so $TARGET1/lib/libffmpeg_nostrip.so
$MY_STRIP --strip-unneeded $TARGET1/lib/libffmpeg.so

# -L$TARGET1 -llenthevcdec
