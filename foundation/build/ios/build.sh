#!/bin/bash

XCODE_NAME=Xcode
SDK_VERSION=6.1
TARGET_VERSION=v6

cd ../../foundation_rext

case $1 in
	i386)
		CONFIG_SRC=../build/ios/TARGET_VERSION/i386
		TARGET=../output/ios/$TARGET_VERSION/i386
		;;
	armv7)
		CONFIG_SRC=../build/ios/TARGET_VERSION/armv7
		TARGET=../output/ios/$TARGET_VERSION/armv7
		;;
	armv7s)
		CONFIG_SRC=../build/ios/TARGET_VERSION/armv7s
		TARGET=../output/ios/$TARGET_VERSION/armv7s
		;;
	*)
		echo Unknown target: $1
		exit
esac

case $2 in
	clean)
		make clean
		cp $CONFIG_SRC/config.h .
		cp $CONFIG_SRC/config.mak .
		cp $CONFIG_SRC/config.fate .
		;;
esac

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

cd ..
