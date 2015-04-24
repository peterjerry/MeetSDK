#!/bin/bash

OS=`uname`
case $OS in
	MINGW32_NT-6.1)
		HOST=windows
		NDK=D:/Software/android-ndk-r9d
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

PREFIX=`pwd`/../../output/android/$1
cd ../../foundation_rext

case $1 in
	x86)
		ARCH=x86
		CPU=i686
		EXTRA_CFLAGS="-march=i686 -msse4"
		#  -DANDROID -DPIC -fpic  -std=c99
		#EXTRA_LDFLAGS="-Lthirdparty/lenthevcdec/lib/x86"
		EXTRA_PARAMETERS="--disable-debug --disable-fast-unaligned"
		;;
	neon)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad"
		# -mthumb to fix hevc image problem
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8 -Lthirdparty/lenthevcdec/lib/armeabi-v7a"
		EXTRA_PARAMETERS="--disable-debug --disable-fast-unaligned --disable-armv6 --disable-armv6t2"
		;;
	neon_lgpl)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad"
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
		EXTRA_PARAMETERS="--disable-debug --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=hls --enable-protocol=file --disable-parser=dca --disable-demuxer=dts --disable-decoder=dca --disable-parser=ac3 --disable-demuxer=ac3 --disable-decoder=ac3 --disable-demuxer=eac3 --disable-decoder=eac3 --enable-muxer=hls"
		;;
	neon_debug)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad"
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
		EXTRA_PARAMETERS="--enable-debug=1 --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=hls --enable-protocol=file"
		;;
	neon_cut)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad"
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
		EXTRA_PARAMETERS="--disable-debug --disable-fast-unaligned --disable-protocols --enable-protocol=http --disable-demuxers --enable-demuxer=mov --enable-demuxer=hls --enable-demuxer=mpegts --disable-bsfs --enable-bsf=aac_adtstoasc --enable-bsf=h264_mp4toannexb --disable-parsers  --enable-parser=h264 --enable-parser=aac --disable-decoders --enable-decoder=h264 --enable-decoder=aac"
		;;
	tegra2)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=vfpv3-d16"
		EXTRA_PARAMETERS="--disable-debug --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=hls --enable-protocol=file"
		;;
	v6_vfp)
		ARCH=arm
		CPU=armv6
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=vfp"
		EXTRA_PARAMETERS="--disable-debug --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=hls --enable-protocol=file"
		;;
	v6_vfp_cut)
		ARCH=arm
		CPU=armv6
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=vfp"
		EXTRA_PARAMETERS="--disable-debug --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=file --disable-demuxers --enable-demuxer=mov --enable-demuxer=hls --enable-demuxer=mpegts --disable-bsfs --enable-bsf=aac_adtstoasc --enable-bsf=h264_mp4toannexb --disable-parsers  --enable-parser=h264 --enable-parser=aac --disable-decoders --enable-decoder=h264 --enable-decoder=aac"
		;;
	*)
		echo Unknown target: $1
		exit
esac

if [ $ARCH == 'arm' ] 
then
	CROSS_PREFIX=$NDK/toolchains/arm-linux-androideabi-4.8/prebuilt/$HOST/bin/arm-linux-androideabi-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstack-protector -fstrict-aliasing"
	OPTFLAGS="-O2"
elif [ $ARCH == 'x86' ] 
then
	CROSS_PREFIX=$NDK/toolchains/x86-4.8/prebuilt/$HOST/bin/i686-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstrict-aliasing"
	OPTFLAGS="-O2 -fno-pic"
	MXLIB_PATH="../libs/x86"
elif [ $ARCH == 'mips' ] 
then
	CROSS_PREFIX=$NDK/toolchains/mipsel-linux-android-4.8/prebuilt/$HOST/bin/mipsel-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fno-strict-aliasing -fmessage-length=0 -fno-inline-functions-called-once -frerun-cse-after-loop -frename-registers"
	OPTFLAGS="-O2"
	MXLIB_PATH="../libs/mips"
fi

#remove ac3 eac3
#EXTRA_PARAMETERS="$EXTRA_PARAMETERS --disable-decoder=ac3,eac3 --disable-parser=ac3 --disable-demuxer=ac3,eac3 "

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

# hevc,liblenthevchm91,liblenthevchm10,liblenthevc
else
echo "full build"
fi

#liblenthevcdec
#EXTRA_CFLAGS="$EXTRA_CFLAGS -Ithirdparty/lenthevcdec/ "
#EXTRA_PARAMETERS="$EXTRA_PARAMETERS --enable-liblenthevcdec "

./configure \
--prefix=$PREFIX \
--arch=$ARCH \
--cpu=$CPU \
--target-os=linux \
--enable-optimizations \
--enable-cross-compile \
--cross-prefix=$CROSS_PREFIX \
--sysroot=$NDK/platforms/android-9/arch-$ARCH \
--extra-cflags="-DNDEBUG -mandroid -ftree-vectorize -ffunction-sections -funwind-tables -fomit-frame-pointer -funswitch-loops -finline-limit=300 -finline-functions -fpredictive-commoning -fgcse-after-reload -fipa-cp-clone $EXTRA_CFLAGS" \
--extra-ldflags="$EXTRA_LDFLAGS" \
--optflags="$OPTFLAGS" \
--enable-zlib \
--enable-pic \
--disable-doc \
--disable-symver \
--disable-ffmpeg \
--disable-ffplay \
--disable-ffprobe \
--disable-ffserver \
--disable-avdevice \
--disable-postproc \
--disable-swscale-alpha \
--disable-encoders \
--disable-muxers \
--disable-devices \
--disable-filters \
--enable-filter=rotate,transpose,hflip,vflip,yadif \
--disable-vfp \
$EXTRA_PARAMETERS




