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
	armeabi-v7a)
		CONFIG_SRC=../build/android/config_armeabi-v7a
		TARGET1=../output/android/armeabi-v7a
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

HOME_FOLDER=`pwd`
FDK_AAC_HOME=$HOME_FOLDER/../thirdparty/fdk-aac
RTMPDUMP_HOME=$HOME_FOLDER/../thirdparty/rtmpdump
X264_HOME=$HOME_FOLDER/../thirdparty/x264

FDK_AAC_LIB=$FDK_AAC_HOME/lib/android/$1
RTMPDUMP_LIB=$RTMPDUMP_HOME/lib/android/$1
X264_LIB=$X264_HOME/lib/android/$1

if [ $ARCH == 'arm' ]; then
	PLATFORM=$NDK/platforms/android-9/arch-arm
	PREBUILT=$NDK/toolchains/arm-linux-androideabi-4.8/prebuilt/$HOST
	CROSS_PREFIX=$PREBUILT/bin/arm-linux-androideabi-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstack-protector -fstrict-aliasing"
	OPTFLAGS="-O2"
elif [ $ARCH == 'aarch64' ]
then
	PLATFORM=$NDK/platforms/android-21/arch-arm64
	PREBUILT=$NDK/toolchains/aarch64-linux-android-4.9/prebuilt/$HOST
	CROSS_PREFIX=$PREBUILT/bin/aarch64-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstrict-aliasing"
	OPTFLAGS="-O2 -fno-pic"
elif [ $ARCH == 'x86' ]
then
	PLATFORM=$NDK/platforms/android-9/arch-x86
	PREBUILT=$NDK/toolchains/x86-4.8/prebuilt/$HOST
	CROSS_PREFIX=$PREBUILT/bin/i686-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstrict-aliasing"
	OPTFLAGS="-O2 -fno-pic"
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
OBJ_FILES="libavutil/*.o libavformat/*.o libavcodec/*.o compat/*.o"
FF_COMPONENTS="libavutil/$ARCH libavcodec/$ARCH libswresample libswresample/$ARCH libswscale libswscale/$ARCH libavfilter libavfilter/$ARCH"
if [ $ARCH == 'arm' ]; then
	FF_COMPONENTS="$FF_COMPONENTS libavcodec/neon"
fi
for COMPONENT in $FF_COMPONENTS; do
if [ "`echo $COMPONENT/*.o`" != "$COMPONENT/*.o" ]; then
	OBJ_FILES="$OBJ_FILES $COMPONENT/*.o"
fi
done

$MY_AR -r $TARGET1/lib/libffmpeg.a $OBJ_FILES
	
# shared lib
$MY_CC -lm -lz -shared --sysroot=$PLATFORM -Wl,--no-undefined -Wl,-z,noexecstack \
	$OBJ_FILES \
	$EXTRA_LIB -o $TARGET1/lib/libffmpeg.so

cp ./config.h $TARGET1/
cp $TARGET1/lib/libffmpeg.so $TARGET1/lib/libffmpeg_nostrip.so
$MY_STRIP --strip-unneeded $TARGET1/lib/libffmpeg.so

# -L$TARGET1 -llenthevcdec
