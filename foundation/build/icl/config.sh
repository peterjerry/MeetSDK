#!/bin/bash

USER_ROOT=`pwd`
FFMPEG_HOME=$USER_ROOT/../../foundation_rext
MS_INT_TYPES_HOME=$USER_ROOT/msinttypes
PREFIX=$USER_ROOT/../../output/icl

rm -rf $PREFIX

cd $FFMPEG_HOME

if [ ${1}x == 'lite'x ]
then
echo "lite build"
EXTRA_PARAMETERS="$EXTRA_PARAMETERS \
	--disable-decoders \
	--enable-decoder=h263,h264,hevc,flv,mpeg1video,mpeg2video,mpeg4,ac3,aac,mp1,mp2,mp3,rv30,rv40,cook,wmv1,wmv2,wmv3,wmv3image \
	--disable-demuxers \
	--enable-demuxer=rm,mpegvideo,avi,h263,h264,hevc,mkv,aac,flv,mpegts,mp4,mov,ape,hls,rtsp \
	--disable-parsers \
	--enable-parser=h263,h264,hevc,vp8,mpegaudio,mpeg4video,aac,ac3"
else
echo "full build"
fi

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
	--disable-programs \
	--disable-avdevice \
	--disable-swscale-alpha \
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