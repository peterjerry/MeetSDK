#!/bin/bash

cd ../../ffmpeg-2.8.3

PREFIX=../output/mingw
EXTRA_FF_BUILD_OPTION="\
	--disable-decoders \
	--enable-decoder=h263,h264,hevc,vp3,vp5,vp6,vp6a,vp6f,vp7,vp8,vp9,flv,mpegvideo,mpeg1video,mpeg2video,mpeg4,ac3,eac3,aac,dca,mp1,mp2,mp3,rv30,rv40,cook,wmv1,wmv2,wmv3,wmv3image,vorbis,ape,flac,wmav1,wmav2,wmapro,mjpeg,msmpeg4v1,msmpeg4v2,msmpeg4v3,tscc,gsm,gsm_ms,amrnb,amrwb,pcm_s16be,pcm_s16be_planar,pcm_s16le,pcm_s16le_planar,ass,dvbsub,dvdsub,mov_text,sami,srt,ssa,subrip,text \
	--disable-demuxers \
	--enable-demuxer=rm,mpegvideo,mjpeg,avi,h263,h264,hevc,matroska,dts,dtshd,aac,flv,mpegts,mpegps,mp4,m4v,mov,ape,hls,flac,rawvideo,realtext,rtsp,vc1,mp3,wav,asf,ogg \
	--disable-parsers \
	--enable-parser=h263,h264,hevc,mpegaudio,mpegvideo,aac_latm,mpeg4video,dca,aac,ac3,eac3,flac,png,bmp,rv30,rv40,cavsvideo,vc1,vorbis,mjpeg,vp3,vp8,vp9,cook \
	--disable-devices"

echo "run configure"
./configure --prefix=$PREFIX \
    --enable-shared \
    --disable-static \
	--enable-optimizations \
	--extra-cflags="-DNDEBUG -march=i686 -msse4" \
	--optflags="-O2" \
	--disable-doc \
	--disable-avdevice \
	--disable-swscale-alpha \
	--disable-encoders \
	--disable-muxers \
	--disable-devices \
	--disable-symver $EXTRA_FF_BUILD_OPTION

echo "begin to building..."

make clean
make -j4 install

#cp version.h $PREFIX/
cp config.h $PREFIX/
cp ../build/mingw/dep/*.dll $PREFIX/bin/

echo "build ffmepg gcc all done!" 
exit
