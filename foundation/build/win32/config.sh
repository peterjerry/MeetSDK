#!/bin/bash

USER_ROOT=`pwd`
FFMPEG_HOME=$USER_ROOT/../../foundation_rext
MS_INT_TYPES_HOME=$USER_ROOT/msinttypes
PREFIX=$USER_ROOT/../../output/win32

if [ ! -n "$1" ] ; then
	echo "Usage: ${0} full/lite"
	exit
fi

cd $FFMPEG_HOME

if [ ${1}x == 'lite'x ]
then
echo "lite build"
EXTRA_FF_BUILD_OPTION="\
	--disable-decoders \
	--enable-decoder=h263,h264,hevc,vp3,vp5,vp6,vp6a,vp6f,vp7,vp8,vp9,flv,mpegvideo,mpeg1video,mpeg2video,mpeg4,ac3,eac3,aac,dca,mp1,mp2,mp3,rv30,rv40,cook,wmv1,wmv2,wmv3,wmv3image,vorbis,ape,flac,wmav1,wmav2,wmapro,mjpeg,msmpeg4v1,msmpeg4v2,msmpeg4v3,tscc,amrnb,amrwb,pcm_s16be,pcm_s16be_planar,pcm_s16le,pcm_s16le_planar,ass,dvbsub,dvdsub,mov_text,sami,srt,ssa,subrip,text \
	--disable-demuxers \
	--enable-demuxer=rm,mpegvideo,mjpeg,avi,h263,h264,hevc,matroska,dts,dtshd,aac,flv,mpegts,mpegps,mp4,m4v,mov,ape,hls,flac,rawvideo,realtext,rtsp,vc1,mp3,wav,asf,ogg \
	--disable-parsers \
	--enable-parser=h263,h264,hevc,mpegaudio,mpegvideo,aac_latm,mpeg4video,dca,aac,ac3,eac3,flac,png,bmp,rv30,rv40,cavsvideo,vc1,vorbis,mjpeg,vp3,vp8,vp9,cook "
PREFIX=$PREFIX/lite
else
echo "full build"
PREFIX=$PREFIX/full
fi

rm -rf $PREFIX

#--enable-demuxer=rm,mpegvideo,avi,h263,h264,hevc,mkv,aac,flv,mpegts,mp4,mov,ape,hls,rtsp,rtp,sdp,asf \

echo "run configure"
./configure --prefix=$PREFIX \
	--toolchain=icl \
	--arch=i686 \
    --enable-shared \
    --disable-static \
	--enable-w32threads \
	--enable-optimizations \
	--extra-cflags="-DNDEBUG -march=i686 -Z7 -DX264_API_IMPORTS -I${MS_INT_TYPES_HOME}" \
	--extra-ldflags="-DEBUG" \
	--optflags="-O2" \
	--disable-doc \
	--disable-avdevice \
	--disable-dxva2 \
	--disable-swscale-alpha \
	--disable-encoders \
	--disable-muxers \
	--disable-devices \
	--disable-asm \
	--disable-symver $EXTRA_FF_BUILD_OPTION

echo "begin to building..."

make clean
make -j4 install

echo "copy pdb files..."

cp libavcodec/*.pdb $PREFIX/bin/
cp libavformat/*.pdb $PREFIX/bin/
cp libavutil/*.pdb $PREFIX/bin/
cp libavfilter/*.pdb $PREFIX/bin/
cp libswresample/*.pdb $PREFIX/bin/
cp libswscale/*.pdb $PREFIX/bin/

cp $MS_INT_TYPES_HOME/stdint.h $PREFIX/include/
cp $MS_INT_TYPES_HOME/inttypes.h $PREFIX/include/
cp config.h $PREFIX/

echo "build ffmepg icl all done!"