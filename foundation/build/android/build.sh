#!/bin/bash

cd ../../foundation_rext

case $1 in
	x86)
		CONFIG_SRC=../build/android/config_x86
		TARGET1=../output/android/x86
		;;
	neon)
		CONFIG_SRC=../build/android/config_neon
		TARGET1=../output/android/neon
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
