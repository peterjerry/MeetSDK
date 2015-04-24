#!/bin/bash

cd ../../foundation_rext

XCODE_NAME=Xcode
SDK_VERSION=8.1
TARGET_VERSION=v8

case $1 in
	i386)
		ARCH=i386
		PLATFORM=/Applications/$XCODE_NAME.app/Contents/Developer/Platforms/iPhoneSimulator.platform
		SYSROOT=${PLATFORM}/Developer/SDKs/iPhoneSimulator${SDK_VERSION}.sdk
		EXTRA_C_FLAGS="-DOS_IOS -arch i386 -miphoneos-version-min=7.0"
		EXTRA_LD_FLAGS="-L${PLATFORM}/Developer/SDKs/iPhoneSimulator${SDK_VERSION}.sdk/usr/lib/system -arch i386 -miphoneos-version-min=7.0"
		CC=`xcrun -find -sdk iphonesimulator clang`
		TARGET=../output/ios/$TARGET_VERSION/i386
		EXTRA_PARAMETERS="--disable-asm"
		;;
	x86_64)
		ARCH=x86_64
		PLATFORM=/Applications/$XCODE_NAME.app/Contents/Developer/Platforms/iPhoneSimulator.platform
		SYSROOT=${PLATFORM}/Developer/SDKs/iPhoneSimulator${SDK_VERSION}.sdk
		EXTRA_C_FLAGS="-DOS_IOS -arch x86_64 -miphoneos-version-min=7.0"
		EXTRA_LD_FLAGS="-L${PLATFORM}/Developer/SDKs/iPhoneSimulator${SDK_VERSION}.sdk/usr/lib/system -arch x86_64 -miphoneos-version-min=7.0"
		CC=`xcrun -find -sdk iphonesimulator clang`
		TARGET=../output/ios/$TARGET_VERSION/x86_64
		EXTRA_PARAMETERS="--disable-asm"
		;;

	armv7)
		ARCH=armv7
		PLATFORM=/Applications/$XCODE_NAME.app/Contents/Developer/Platforms/iPhoneOS.platform
		SYSROOT=${PLATFORM}/Developer/SDKs/iPhoneOS${SDK_VERSION}.sdk
		EXTRA_C_FLAGS="-DOS_IOS -arch armv7"
		EXTRA_LD_FLAGS="-L${PLATFORM}/Developer/SDKs/iPhoneOS${SDK_VERSION}.sdk/usr/lib/system -arch armv7"
		CC=`xcrun -find -sdk iphoneos clang`
		TARGET=../output/ios/$TARGET_VERSION/armv7
		EXTRA_PARAMETERS="--cpu=cortex-a8"
		;;
	armv7s)
		ARCH=armv7s
		PLATFORM=/Applications/$XCODE_NAME.app/Contents/Developer/Platforms/iPhoneOS.platform
		SYSROOT=${PLATFORM}/Developer/SDKs/iPhoneOS${SDK_VERSION}.sdk
		EXTRA_C_FLAGS="-DOS_IOS -arch armv7s -mfpu=neon"
		EXTRA_LD_FLAGS="-L${PLATFORM}/Developer/SDKs/iPhoneOS${SDK_VERSION}.sdk/usr/lib/system -arch armv7s"
		CC=`xcrun -find -sdk iphoneos clang`
		EXTRA_PARAMETERS="--cpu=cortex-a9"
		TARGET=../output/ios/$TARGET_VERSION/armv7s
		;;
	arm64)
		ARCH=arm64
		PLATFORM=/Applications/$XCODE_NAME.app/Contents/Developer/Platforms/iPhoneOS.platform
		SYSROOT=${PLATFORM}/Developer/SDKs/iPhoneOS${SDK_VERSION}.sdk
		EXTRA_C_FLAGS="-DOS_IOS -arch arm64"
		EXTRA_LD_FLAGS="-L${PLATFORM}/Developer/SDKs/iPhoneOS${SDK_VERSION}.sdk/usr/lib/system -arch arm64"
		CC=`xcrun -find -sdk iphoneos clang`
		TARGET=../output/ios/$TARGET_VERSION/arm64
		;;
	*)
		echo Unknown target: $1
		exit
esac

#lite build
#if [[ $2 = 'lite' ]]
if [ ${2}x == 'lite'x ]
then
echo "lite build"
EXTRA_PARAMETERS="$EXTRA_PARAMETERS \
	--disable-decoders \
	--enable-decoder=h263,h264,hevc,flv,mpeg1video,mpeg2video,mpeg4,ac3,aac,mp1,mp2,mp3,rv30,rv40,cook,wmv1,wmv2,wmv3,wmv3image,ape,flac,wmav1,wmav2,wmapro,pcm_s16be,pcm_s16be_planar,pcm_s16le,pcm_s16le_planar \
	--disable-demuxers \
	--enable-demuxer=rm,mpegvideo,avi,h263,h264,hevc,matroska,aac,flv,mpegts,mp4,m4v,mov,ape,hls,flac,rawvideo,realtext,rtsp,vc1,mp3,wav,asf \
	--disable-parsers \
	--enable-parser=h263,h264,hevc,vp8,mpegaudio,mpegvideo,aac_latm,mpeg4video,aac,ac3,flac,png,bmp,rv30,rv40,cavsvideo,vc1,vorbis,mjpeg,vp8,vp9,cook "
else
echo "full build"
fi

#ios disble ac3
EXTRA_PARAMETERS="$EXTRA_PARAMETERS --disable-decoder=ac3,eac3 --disable-parser=ac3 --disable-demuxer=ac3,eac3 "

./configure \
--prefix=$TARGET \
--arch=$ARCH \
--target-os=darwin \
--enable-cross-compile \
--cc="$CC" \
--sysroot=$SYSROOT \
--extra-ldflags="$EXTRA_LD_FLAGS" \
--extra-cflags="$EXTRA_C_FLAGS" \
--enable-pic \
--disable-doc \
--disable-symver \
--disable-ffmpeg \
--disable-ffplay \
--disable-ffprobe \
--disable-ffserver \
--disable-avdevice \
--disable-postproc \
--disable-avfilter \
--disable-swscale-alpha \
--disable-encoders \
--disable-muxers \
--disable-demuxer=mlp \
--disable-parser=mlp \
--disable-decoder=mlp \
--disable-devices \
--disable-filters \
--disable-vfp \
--disable-armv6 \
--disable-armv6t2 \
--disable-fast-unaligned \
--disable-debug ${EXTRA_PARAMETERS}

#--disable-armv6 \
#--disable-armv6t2
#--disable-armvfp
#--as="perl gas-preprocessor/gas-preprocessor.pl ${PLATFORM}/Developer/usr/bin/gcc" \
#--enable-zlib \
#--enable-optimizations \
#--extra-cflags="-arch armv7 -mfpu=neon" \

#--disable-debug \
#--enable-debug=1
#--disable-static \
#--enable-shared \
#--cpu=cortex-a9
#-arch armv7s

