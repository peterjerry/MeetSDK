#!/bin/bash

FFMPEG_SRC_PATH=../../foundation_rext

for arg in $*
do
	if [ ${arg:0:5} == "path=" ]; then
		FFMPEG_SRC_PATH=${arg:5}
	fi
done

echo "set ffmpeg source path: $FFMPEG_SRC_PATH"

cd $FFMPEG_SRC_PATH

case $1 in
	x86)
		CONFIG_SRC=../build/android/config_x86
		TARGET1=../output/android/x86
		;;
	armeabi)
		CONFIG_SRC=../build/android/config_armeabi
		TARGET1=../output/android/armeabi
		;;
	armeabi-v7a)
		CONFIG_SRC=../build/android/config_armeabi-v7a
		TARGET1=../output/android/armeabi-v7a
		;;
	arm64-v8a)
		CONFIG_SRC=../build/android/config_arm64-v8a
		TARGET1=../output/android/arm64-v8a
		;;
	tegra2)
		CONFIG_SRC=../build/android/config_tegra2
		TARGET1=../output/android/v7_vfpv3d16
		;;
	*)
		echo Unknown target: $1
		exit
esac

case $2 in
	clean)
		make clean
		cp $CONFIG_SRC/config.h . -f
		cp $CONFIG_SRC/config.mak . -f
		;;
esac

if [ ! -d $TARGET1 ]; then 
	mkdir -p $TARGET1 
fi 

make clean
make -j4 install

