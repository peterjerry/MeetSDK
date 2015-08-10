#!/bin/bash

USER_ROOT=`pwd`
FFMPEG_HOME=$USER_ROOT/../../foundation_rext
MS_INT_TYPES_HOME=$USER_ROOT/msinttypes
PREFIX=$USER_ROOT/../../output/win32

if [ ${1}x == 'lite'x ]
then
echo "lite build"
PREFIX=$PREFIX/lite
else
echo "full build"
PREFIX=$PREFIX/full
fi

rm -rf $PREFIX

cp config/config.h $FFMPEG_HOME
cp config/config.mak $FFMPEG_HOME

cd $FFMPEG_HOME

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