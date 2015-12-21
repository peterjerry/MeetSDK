#!/bin/bash

USER_ROOT=`pwd`
FFMPEG_HOME=$USER_ROOT/../../foundation_rext
MS_INT_TYPES_HOME=$USER_ROOT/msinttypes
PREFIX=$USER_ROOT/../../output/win32

if [ -z $1 ]; then
PREFIX=$PREFIX/full
echo "full build(default)"
cp config/full/config.h $FFMPEG_HOME
cp config/full/config.mak $FFMPEG_HOME
else
echo "${1} build"
PREFIX="$PREFIX/${1}"
cp config/${1}/config.h $FFMPEG_HOME
cp config/${1}/config.mak $FFMPEG_HOME
fi

rm -rf $PREFIX

cd $FFMPEG_HOME

echo "begin to building..."

# delete old files
make clean
OBJ_FOLDERS="libavutil libavformat libavcodec libswscale libswresample libavfilter compat"
for OBJ in $OBJ_FOLDERS
do
	if [ "`echo $OBJ/*.o`" != "$OBJ/*.o" ]; then
		rm $OBJ/*.o
	fi
done

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