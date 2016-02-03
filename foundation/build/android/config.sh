#!/bin/bash

if [ $# == 0 ] ; then 
echo "USAGE: $0 <abi(armeabi-v7a x86 arm64-v8a)> [config(full lite micro tiny gotye)] [component(enc mux librtmp openssl)]" 
echo " e.g.: $0 armeabi-v7a"
echo " e.g.: $0 armeabi-v7a lite"
echo " e.g.: $0 armeabi-v7a lite enc" 
exit 1; 
fi

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

FFMPEG_SRC_PATH=../../foundation_rext

for arg in $*
do
	if [ ${arg:0:5} == "path=" ]; then
		FFMPEG_SRC_PATH=${arg:5}
	fi
done

echo "set ffmpeg source path: $FFMPEG_SRC_PATH"

PREFIX=`pwd`/../../output/android/$1
cd $FFMPEG_SRC_PATH

case $1 in
	x86)
		ARCH=x86
		CPU=i686
		EXTRA_CFLAGS="-march=i686 -msse4"
		#  -DANDROID -DPIC -fpic  -std=c99
		#EXTRA_LDFLAGS="-Lthirdparty/lenthevcdec/lib/x86"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-fast-unaligned"
		;;
	armeabi)
		ARCH=arm
		EXTRA_CFLAGS="-march=armv5te -mtune=arm9tdmi -msoft-float"
		EXTRA_PARAMETERS="--disable-fast-unaligned --disable-armv6 --disable-armv6t2 --disable-neon"
		;;
	armeabi-v7a)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad"
		# -mthumb to fix hevc image problem
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8 -Lthirdparty/lenthevcdec/lib/armeabi-v7a"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-fast-unaligned --disable-armv6 --disable-armv6t2"
		;;
	arm64-v8a)
		ARCH=arm64
		EXTRA_PARAMETERS="--disable-fast-unaligned --disable-armv6 --disable-armv6t2"
		;;
	neon_lgpl)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad"
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=hls --enable-protocol=file --disable-parser=dca --disable-demuxer=dts --disable-decoder=dca --disable-parser=ac3 --disable-demuxer=ac3 --disable-decoder=ac3 --disable-demuxer=eac3 --disable-decoder=eac3 --enable-muxer=hls"
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
		EXTRA_PARAMETERS="--cpu=$CPU --disable-fast-unaligned --disable-protocols --enable-protocol=http --disable-demuxers --enable-demuxer=mov --enable-demuxer=hls --enable-demuxer=mpegts --disable-bsfs --enable-bsf=aac_adtstoasc --enable-bsf=h264_mp4toannexb --disable-parsers  --enable-parser=h264 --enable-parser=aac --disable-decoders --enable-decoder=h264 --enable-decoder=aac"
		;;
	tegra2)
		ARCH=arm
		CPU=armv7-a
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=vfpv3-d16"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=hls --enable-protocol=file"
		;;
	v6_vfp)
		ARCH=arm
		CPU=armv6
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=vfp"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=hls --enable-protocol=file"
		;;
	v6_vfp_cut)
		ARCH=arm
		CPU=armv6
		EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=vfp"
		EXTRA_PARAMETERS="--cpu=$CPU --disable-fast-unaligned --disable-protocols --enable-protocol=http --enable-protocol=file --disable-demuxers --enable-demuxer=mov --enable-demuxer=hls --enable-demuxer=mpegts --disable-bsfs --enable-bsf=aac_adtstoasc --enable-bsf=h264_mp4toannexb --disable-parsers  --enable-parser=h264 --enable-parser=aac --disable-decoders --enable-decoder=h264 --enable-decoder=aac"
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

if [ $ARCH == 'arm' ] 
then
	CROSS_PREFIX=$NDK/toolchains/arm-linux-androideabi-4.8/prebuilt/$HOST/bin/arm-linux-androideabi-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstack-protector -fstrict-aliasing"
	OPTFLAGS="-O2"
elif [ $ARCH == 'arm64' ] 
then
	CROSS_PREFIX=$NDK/toolchains/aarch64-linux-android-4.9/prebuilt/$HOST/bin/aarch64-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstack-protector -fstrict-aliasing"
	OPTFLAGS="-O2"
elif [ $ARCH == 'x86' ] 
then
	CROSS_PREFIX=$NDK/toolchains/x86-4.8/prebuilt/$HOST/bin/i686-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fstrict-aliasing"
	OPTFLAGS="-O2 -fno-pic"
elif [ $ARCH == 'mips' ] 
then
	CROSS_PREFIX=$NDK/toolchains/mipsel-linux-android-4.8/prebuilt/$HOST/bin/mipsel-linux-android-
	EXTRA_CFLAGS="$EXTRA_CFLAGS -fno-strict-aliasing -fmessage-length=0 -fno-inline-functions-called-once -frerun-cse-after-loop -frename-registers"
	OPTFLAGS="-O2"
fi

if [ $# == 1 ]; then
	echo "full build"
fi

for arg in $*
do
	if [ ${arg}x == 'enc'x ]; then
		HOME_FOLDER=`pwd`
		FDK_AAC_HOME=$HOME_FOLDER/../thirdparty/fdk-aac
		X264_HOME=$HOME_FOLDER/../thirdparty/x264
		FDK_AAC_LIB=$FDK_AAC_HOME/lib/android/$1
		X264_LIB=$X264_HOME/lib/android/$1
		echo "build-in fdk-aac & libx264"
		echo "================="
		echo "HOME_FOLDER: $HOME_FOLDER"
		echo "fdk-aac include: $FDK_AAC_HOME/include"
		echo "fdk-aac lib: $FDK_AAC_LIB"
		echo "x264 include: $X264_HOME/include"
		echo "x264 lib: $X264_LIB"
		echo "================="

		EXTRA_CFLAGS="$EXTRA_CFLAGS -I$FDK_AAC_HOME/include -I$X264_HOME/include"
		EXTRA_LDFLAGS="$EXTRA_LDFLAGS -L$FDK_AAC_LIB -L$X264_LIB"
		
		EXTRA_PARAMETERS="$EXTRA_PARAMETERS \
			--enable-nonfree \
			--enable-encoder=libfdk_aac \
			--enable-libfdk-aac \
			--enable-openssl \
			--enable-gpl \
			--enable-libx264 \
			--enable-encoder=libx264"
	elif [ ${arg}x == 'mux'x ]; then
		EXTRA_PARAMETERS="$EXTRA_PARAMETERS --enable-muxer=mpegts,flv,mp4,hls"
	elif [ ${arg}x == 'openssl'x ]; then
		HOME_FOLDER=`pwd`
		OPENSSL_HOME=$HOME_FOLDER/../thirdparty/rtmpdump
		OPENSSL_LIB=$OPENSSL_HOME/lib/android/$1
		echo "build-in openssl"
		echo "================="
		echo "openssl include: $OPENSSL_HOME/include"
		echo "openssl lib: $OPENSSL_LIB"
		echo "================="
		EXTRA_CFLAGS="$EXTRA_CFLAGS -I$OPENSSL_HOME/include"
		EXTRA_LDFLAGS="$EXTRA_LDFLAGS -L$OPENSSL_LIB -lssl -lcrypto -lz"
	elif [ ${arg}x == 'librtmp'x ]; then
		HOME_FOLDER=`pwd`
		RTMPDUMP_HOME=$HOME_FOLDER/../thirdparty/rtmpdump
		RTMPDUMP_LIB=$RTMPDUMP_HOME/lib/android/$1
		echo "build-in librtmp"
		echo "================="
		echo "librtmp include: $RTMPDUMP_HOME/include"
		echo "librtmp lib: $RTMPDUMP_LIB"
		echo "================="
		EXTRA_PARAMETERS="$EXTRA_PARAMETERS --enable-librtmp"
	elif [ ${arg}x == 'lite'x ]; then
		echo "lite build"
		EXTRA_PARAMETERS="$EXTRA_PARAMETERS \
			--disable-decoders \
			--enable-decoder=h263,h264,hevc,vp3,vp5,vp6,vp6a,vp6f,vp7,vp8,vp9,flv,mpeg1video,mpeg2video,mpegvideo,mpeg4,dca,ac3,eac3,aac,mp1,mp2,mp3,rv30,rv40,cook,wmv1,wmv2,wmv3,wmv3image,vorbis,ape,flac,wmav1,wmav2,wmapro,mjpeg,msmpeg4v1,msmpeg4v2,msmpeg4v3,tscc,gsm,gsm_ms,amrnb,amrwb,pcm_s16be,pcm_s16be_planar,pcm_s16le,pcm_s16le_planar,ass,dvbsub,dvdsub,mov_text,sami,srt,ssa,subrip,text \
			--disable-demuxers \
			--enable-demuxer=rm,mpegvideo,mjpeg,avi,h263,h264,hevc,matroska,dts,dtshd,aac,flv,mpegts,mpegps,mp4,m4v,mov,ape,hls,flac,rawvideo,realtext,rtsp,vc1,mp3,wav,asf,ogg \
			--disable-parsers \
			--enable-parser=h263,h264,hevc,mpegaudio,mpegvideo,aac_latm,mpeg4video,dca,aac,ac3,eac3,flac,png,bmp,rv30,rv40,cavsvideo,vc1,vorbis,mjpeg,vp3,vp8,vp9,cook "

		# hevc,liblenthevchm91,liblenthevchm10,liblenthevc
	elif [ ${arg}x == 'micro'x ]; then
		echo "micro build"
		EXTRA_PARAMETERS="$EXTRA_PARAMETERS \
				--disable-decoders \
				--enable-decoder=h264,aac \
				--disable-demuxers \
				--enable-demuxer=h264,mp4,mov,mpegts,flv,hls \
				--disable-parsers \
				--enable-parser=h264,aac_latm \
			--enable-muxer=mpegts,flv,hls \
			--disable-protocols \
			--enable-protocol=file,http,rtmp,hls "
	elif [ ${arg}x == 'tiny'x ]; then
		echo "tiny build"
		EXTRA_PARAMETERS="$EXTRA_PARAMETERS \
			--disable-decoders \
			--enable-decoder=h264,aac \
			--disable-demuxers \
			--enable-demuxer=h264,mp4,mov,mpegts,flv,hls \
			--disable-parsers \
			--enable-parser=h264,aac_latm \
			--disable-protocols \
			--enable-protocol=file,http,rtmp,hls \
			--disable-bsfs \
			--disable-swscale \
			--disable-avfilter \
			--disable-postproc \
			--enable-small "
		#       --enable-bsf=ac_adtstoasc,h264_mp4toannexb
	elif [ ${arg}x == 'gotye'x ]; then
		echo "gotye build"
		EXTRA_PARAMETERS="$EXTRA_PARAMETERS \
			--disable-decoders \
			--enable-decoder=aac_latm \
			--disable-demuxers \
			--enable-demuxer=flv \
			--disable-parsers \
			--disable-protocols \
			--enable-protocol=rtmp \
			--disable-bsfs \
			--enable-bsf=h264_mp4toannexb \
			--disable-swscale \
			--disable-swresample \
			--disable-avfilter \
			--disable-postproc \
			--enable-small "
	fi
done

#remove ac3 eac3
#EXTRA_PARAMETERS="$EXTRA_PARAMETERS --disable-decoder=ac3,eac3 --disable-parser=ac3 --disable-demuxer=ac3,eac3 "

if [ ${2}x != 'tiny'x ] && [ ${2}x != 'gotye'x ]; then
EXTRA_PARAMETERS="$EXTRA_PARAMETERS \
	--enable-filter=rotate,transpose,hflip,vflip,yadif,showspectrum,showwaves,aresample,scale"
fi

#liblenthevcdec
#EXTRA_CFLAGS="$EXTRA_CFLAGS -Ithirdparty/lenthevcdec/ "
#EXTRA_PARAMETERS="$EXTRA_PARAMETERS --enable-liblenthevcdec "

# delete old files
if [ -f Makefile ]; then
	make clean
fi

OBJ_FOLDERS="libavutil libavformat libavcodec libswscale libswresample libavfilter compat"
for OBJ in $OBJ_FOLDERS
do
	if [ "`echo $OBJ/*.o`" != "$OBJ/*.o" ]; then
		rm $OBJ/*.o
	fi
done

./configure \
	--prefix=$PREFIX \
	--arch=$ARCH \
	--target-os=linux \
	--disable-debug \
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
	--disable-vfp \
	$EXTRA_PARAMETERS

#--cpu=$CPU \

if [[ $? -ne 0 ]]; then
	echo -e "\033[31m \n\nfailed to config ffmpeg \033[0m"
    exit 1
fi




