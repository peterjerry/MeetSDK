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

PREFIX=`pwd`/../../output/android/$1
cd ../../foundation_rext

case $1 in
	x86)
		ARCH=x86
		CPU=i686
		EXTRA_CFLAGS="-march=i686 -msse4"
		#  -DANDROID -DPIC -fpic  -std=c99
		#EXTRA_LDFLAGS="-Lthirdparty/lenthevcdec/lib/x86"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-debug --disable-fast-unaligned"
		;;
	neon)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad"
		# -mthumb to fix hevc image problem
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8 -Lthirdparty/lenthevcdec/lib/armeabi-v7a"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-debug --disable-fast-unaligned --disable-armv6 --disable-armv6t2"
		;;
	arm64)
		ARCH=arm64
		EXTRA_PARAMETERS="--disable-debug --disable-fast-unaligned --disable-armv6 --disable-armv6t2"
		;;
	neon_lgpl)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad"
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-debug --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=hls --enable-protocol=file --disable-parser=dca --disable-demuxer=dts --disable-decoder=dca --disable-parser=ac3 --disable-demuxer=ac3 --disable-decoder=ac3 --disable-demuxer=eac3 --disable-decoder=eac3 --enable-muxer=hls"
		;;
	neon_debug)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad"
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
		EXTRA_PARAMETERS="--cpu=$CPU --enable-debug=1 --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=hls --enable-protocol=file"
		;;
	neon_cut)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad"
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-debug --disable-fast-unaligned --disable-protocols --enable-protocol=http --disable-demuxers --enable-demuxer=mov --enable-demuxer=hls --enable-demuxer=mpegts --disable-bsfs --enable-bsf=aac_adtstoasc --enable-bsf=h264_mp4toannexb --disable-parsers  --enable-parser=h264 --enable-parser=aac --disable-decoders --enable-decoder=h264 --enable-decoder=aac"
		;;
	tegra2)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=vfpv3-d16"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-debug --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=hls --enable-protocol=file"
		;;
	v6_vfp)
		ARCH=arm
		CPU=armv6
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=vfp"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-debug --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=hls --enable-protocol=file"
		;;
	v6_vfp_cut)
		ARCH=arm
		CPU=armv6
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=vfp"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-debug --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=file --disable-demuxers --enable-demuxer=mov --enable-demuxer=hls --enable-demuxer=mpegts --disable-bsfs --enable-bsf=aac_adtstoasc --enable-bsf=h264_mp4toannexb --disable-parsers  --enable-parser=h264 --enable-parser=aac --disable-decoders --enable-decoder=h264 --enable-decoder=aac"
		;;
	*)
		echo Unknown target: $1
		exit
esac

if [ $ARCH == 'arm64' ] 
then
	SYSROOT=$NDK/platforms/android-21/arch-$ARCH
else
	SYSROOT=$NDK/platforms/android-9/arch-$ARCH
fi

USER_ROOT=`pwd`
FDK_AAC_HOME=$USER_ROOT/thirdparty/fdk-aac
RTMPDUMP_HOME=$USER_ROOT/thirdparty/rtmpdump
X264_HOME=$USER_ROOT/thirdparty/x264

if [ $ARCH == 'arm' ] 
then
	CROSS_PREFIX=$NDK/toolchains/arm-linux-androideabi-4.8/prebuilt/$HOST/bin/arm-linux-androideabi-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstack-protector -fstrict-aliasing"
	OPTFLAGS="-O2"
	FDK_AAC_LIB=$FDK_AAC_HOME/lib/android/armeabi-v7a
	RTMPDUMP_LIB=$USER_ROOT/thirdparty/rtmpdump/lib/android/armeabi
	X264_LIB=$X264_HOME/lib/android/armeabi
elif [ $ARCH == 'arm64' ] 
then
	CROSS_PREFIX=$NDK/toolchains/aarch64-linux-android-4.9/prebuilt/$HOST/bin/aarch64-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstack-protector -fstrict-aliasing"
	OPTFLAGS="-O2"
	RTMPDUMP_LIB=$USER_ROOT/thirdparty/rtmpdump/lib/android/arm64-v8a
	X264_LIB=$X264_HOME/lib/android/arm64-v8a
elif [ $ARCH == 'x86' ] 
then
	CROSS_PREFIX=$NDK/toolchains/x86-4.8/prebuilt/$HOST/bin/i686-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstrict-aliasing"
	OPTFLAGS="-O2 -fno-pic"
	FDK_AAC_LIB=$FDK_AAC_HOME/lib/x86
	RTMPDUMP_LIB=$USER_ROOT/thirdparty/rtmpdump/lib/android/x86
	X264_LIB=$X264_HOME/lib/android/x86
elif [ $ARCH == 'mips' ] 
then
	CROSS_PREFIX=$NDK/toolchains/mipsel-linux-android-4.8/prebuilt/$HOST/bin/mipsel-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fno-strict-aliasing -fmessage-length=0 -fno-inline-functions-called-once -frerun-cse-after-loop -frename-registers"
	OPTFLAGS="-O2"
fi

if [ ${3}x == 'enc'x ]; then

echo "build-in fdk-aac" 
echo "USER_ROOT: $USER_ROOT"
echo "fdk-aac include: $FDK_AAC_HOME/include"
echo "fdk-aac lib: $FDK_AAC_LIB"

echo "rtmpdump include: $RTMPDUMP_HOME/include"
echo "rtmpdump lib: $RTMPDUMP_LIB"

echo "x264 include: $X264_HOME/include"
echo "x264 lib: $X264_LIB"

EXTRA_CFLAGS="$EXTRA_CFLAGS -I$FDK_AAC_HOME/include -I$RTMPDUMP_HOME/include -I$X264_HOME/include"
EXTRA_LDFLAGS="$EXTRA_LDFLAGS -L$FDK_AAC_LIB -L$X264_LIB"
EXTRA_LDFLAGS="$EXTRA_LDFLAGS -L$RTMPDUMP_LIB -lssl -lcrypto -lz"
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
	--enable-decoder=h263,h264,hevc,vp8,vp9,flv,mpeg1video,mpeg2video,mpeg4,ac3,aac,mp1,mp2,mp3,rv30,rv40,cook,wmv1,wmv2,wmv3,wmv3image,vorbis,ape,flac,wmav1,wmav2,wmapro,mjpeg,msmpeg4v1,msmpeg4v2,msmpeg4v3,amrnb,amrwb,pcm_s16be,pcm_s16be_planar,pcm_s16le,pcm_s16le_planar,ass,dvbsub,dvdsub,mov_text,sami,srt,ssa,subrip,text \
	--disable-demuxers \
	--enable-demuxer=rm,mpegvideo,mjpeg,avi,h263,h264,hevc,matroska,aac,flv,mpegts,mp4,m4v,mov,ape,hls,flac,rawvideo,realtext,rtsp,vc1,mp3,wav,asf \
	--disable-parsers \
	--enable-parser=h263,h264,hevc,mpegaudio,mpegvideo,aac_latm,mpeg4video,aac,ac3,flac,png,bmp,rv30,rv40,cavsvideo,vc1,vorbis,mjpeg,vp8,vp9,cook "

# hevc,liblenthevchm91,liblenthevchm10,liblenthevc
else
echo "full build"
fi

if [ ${3}x == 'enc'x ]; then
EXTRA_PARAMETERS="$EXTRA_PARAMETERS \
	--enable-nonfree \
	--enable-encoder=libfdk_aac \
	--enable-libfdk-aac \
	--enable-muxer=mpegts,flv,hls \
	--enable-openssl \
	--enable-gpl \
	--enable-libx264 \
	--enable-encoder=libx264"
#	--enable-librtmp \	
fi

#liblenthevcdec
#EXTRA_CFLAGS="$EXTRA_CFLAGS -Ithirdparty/lenthevcdec/ "
#EXTRA_PARAMETERS="$EXTRA_PARAMETERS --enable-liblenthevcdec "

./configure \
--prefix=$PREFIX \
--arch=$ARCH \
--target-os=linux \
--enable-optimizations \
--enable-cross-compile \
--cross-prefix=$CROSS_PREFIX \
--sysroot=$SYSROOT \
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
--enable-filter=rotate,transpose,hflip,vflip,yadif,showspectrum,showwaves,aresample,scale \
--disable-vfp \
$EXTRA_PARAMETERS

#--cpu=$CPU \




