#!/bin/bash

cd ../../foundation_rext

XCODE_NAME=Xcode4
SDK_VERSION=6.1
TARGET_VERSION=v6

case $1 in
	i386)
		ARCH=i386
		CPU=i386
		PLATFORM=/Applications/$XCODE_NAME.app/Contents/Developer/Platforms/iPhoneSimulator.platform
		SYSROOT=${PLATFORM}/Developer/SDKs/iPhoneSimulator${SDK_VERSION}.sdk
		EXTRA_C_FLAGS="-DOS_IOS -arch i386"
		EXTRA_LD_FLAGS="-L${PLATFORM}/Developer/SDKs/iPhoneSimulator${SDK_VERSION}.sdk/usr/lib/system "
		CC="${PLATFORM}/Developer/usr/bin/gcc -arch i386"
		TARGET=../output/ios/$TARGET_VERSION/i386
		EXTRA_PARAMETERS="--disable-asm "
		;;
	armv7)
		ARCH=arm
		CPU=cortex-a8
		PLATFORM=/Applications/$XCODE_NAME.app/Contents/Developer/Platforms/iPhoneOS.platform
		SYSROOT=${PLATFORM}/Developer/SDKs/iPhoneOS${SDK_VERSION}.sdk
		EXTRA_C_FLAGS="-DOS_IOS -arch armv7"
		EXTRA_LD_FLAGS="-L${PLATFORM}/Developer/SDKs/iPhoneOS${SDK_VERSION}.sdk/usr/lib/system "
		CC="${PLATFORM}/Developer/usr/bin/gcc -arch armv7"
		TARGET=../output/ios/$TARGET_VERSION/armv7
		;;
	armv7s)
		ARCH=arm
		CPU=cortex-a9
		PLATFORM=/Applications/$XCODE_NAME.app/Contents/Developer/Platforms/iPhoneOS.platform
		SYSROOT=${PLATFORM}/Developer/SDKs/iPhoneOS${SDK_VERSION}.sdk
		EXTRA_C_FLAGS="-DOS_IOS -arch armv7s -mfpu=neon"
		EXTRA_LD_FLAGS="-L${PLATFORM}/Developer/SDKs/iPhoneOS${SDK_VERSION}.sdk/usr/lib/system "
		CC="${PLATFORM}/Developer/usr/bin/gcc -arch armv7s"
		TARGET=../output/ios/$TARGET_VERSION/armv7s
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
	--enable-decoder=h263,h264,hevc,flv,mpeg1video,mpeg2video,mpeg4,ac3,aac,mp1,mp2,mp3,rv30,rv40,cook \
	--disable-demuxers \
	--enable-demuxer=rm,mpegvideo,avi,h263,h264,hevc,mkv,aac,flv,mpegts,mp4,mov,ape,hls \
	--disable-parsers \
	--enable-parser=h263,h264,hevc,vp8,mpegaudio,mpeg4video,aac,ac3 "
else
echo "full build"
fi

#ios disble ac3
EXTRA_PARAMETERS="$EXTRA_PARAMETERS --disable-decoder=ac3,eac3 --disable-parser=ac3 --disable-demuxer=ac3,eac3 "

./configure \
--arch=$ARCH \
--cpu=$CPU \
--target-os=darwin \
--enable-cross-compile \
--cc="$CC" \
--sysroot=$SYSROOT \
--extra-ldflags=$EXTRA_LD_FLAGS \
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
--disable-parser=mlp \
--disable-demuxer=mlp \
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

exit

make clean
make -j4

if [ ! -d $TARGET ]; then 
	mkdir -p $TARGET 
fi 

cp libavutil/libavutil.a $TARGET/
cp libswresample/libswresample.a $TARGET/
cp libavcodec/libavcodec.a $TARGET/
cp libavformat/libavformat.a $TARGET/
cp libswscale/libswscale.a $TARGET/

