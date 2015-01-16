#!/bin/bash

XCODE_NAME=Xcode
SDK_VERSION=8.1
TARGET_VERSION=v8

cd ../../foundation_rext

case $1 in
	i386)
		CONFIG_SRC=../build/ios/$TARGET_VERSION/i386
		TARGET=../output/ios/$TARGET_VERSION/i386
		;;
	x86_64)
                CONFIG_SRC=../build/ios/$TARGET_VERSION/x86_64
                TARGET=../output/ios/$TARGET_VERSION/x86_64
                ;;
	armv7)
		CONFIG_SRC=../build/ios/$TARGET_VERSION/armv7
		TARGET=../output/ios/$TARGET_VERSION/armv7
		;;
	armv7s)
		CONFIG_SRC=../build/ios/$TARGET_VERSION/armv7s
		TARGET=../output/ios/$TARGET_VERSION/armv7s
		;;
	arm64)
                CONFIG_SRC=../build/ios/$TARGET_VERSION/arm64
                TARGET=../output/ios/$TARGET_VERSION/arm64
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

if [ ! -d $TARGET ]; then 
	mkdir -p $TARGET 
fi 

make clean
make -j4 install
