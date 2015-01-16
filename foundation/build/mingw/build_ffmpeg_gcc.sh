#!/bin/bash

cd ../../foundation_rext

PREFIX=../output/mingw
EXTRA_FF_BUILD_OPTION="--disable-decoders \
	--enable-decoder=h263,h264,hevc,flv,mpeg1video,mpeg2video,mpeg4,ac3,aac,mp1,mp2,mp3,cook \
	--disable-encoders \
	--disable-demuxers \
	--enable-demuxer=rm,mpegvideo,avi,h263,h264,hevc,mkv,aac,flv,mpegts,mp4,mov,ape \
	--disable-muxers \
	--disable-parsers \
	--enable-parser=h263,h264,hevc,vp8,mpegaudio,mpeg4video,aac,ac3 \
	--disable-devices"

echo "run configure"
./configure --prefix=$PREFIX \
    --enable-shared \
    --disable-static \
	--enable-optimizations \
	--extra-cflags="-DNDEBUG -march=i686 -msse4" \
	--optflags="-O2" \
	--disable-doc \
	--disable-programs \
	--disable-avdevice \
	--disable-swscale-alpha \
	--disable-symver $EXTRA_FF_BUILD_OPTION

echo "begin to building..."

make clean
make -j4 install

cp version.h $PREFIX/
cp config.h $PREFIX/

echo "build ffmepg gcc all done!" 
exit
