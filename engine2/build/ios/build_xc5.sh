#!/bin/bash
SDKVERSION="7.0"
#CC=/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin/llvm-g++
CC=`xcrun -find -sdk iphoneos clang`
LIBTOOL=`xcrun -find -sdk iphoneos libtool`
LIPO=/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin/lipo
CFLAGS="-DNDEBUG --sysroot=/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS${SDKVERSION}.sdk -arch armv7 -DOS_IOS -Wno-deprecated-declarations"
INCLUDES="-I../../platform -I../../player -I../../foundation/foundation"
#-DNDEBUG
OUTPUTPATH=../../output/ios/v7/armv7
PLATFORMPATH=../../platform
PLAYERPATH=../../player
FOUNDATIONPATH="../../foundation/output/ios/v7/armv7"

rm *.o

$CC -c \
	$CFLAGS \
	$INCLUDES \
	$PLATFORMPATH/packetqueue.cpp \
	$PLATFORMPATH/list.cpp \
	$PLATFORMPATH/loop.cpp \
	$PLATFORMPATH/utils.cpp \
	$PLATFORMPATH/audiotrack_ios.c \
	$PLATFORMPATH/surface_ios.m \
	$PLAYERPATH/ffstream.cpp \
	$PLAYERPATH/audioplayer.cpp \
	$PLAYERPATH/ffplayer.cpp

$CC -c \
	$CFLAGS "-fobjc-arc" \
	$INCLUDES \
	$PLATFORMPATH/log_ios.m \
	$PLATFORMPATH/glview_ios.m
		
$LIBTOOL \
	-o $OUTPUTPATH/libplayer.a \
	packetqueue.o \
	list.o \
	loop.o \
	utils.o \
	audiotrack_ios.o \
	surface_ios.o \
	glview_ios.o \
	log_ios.o \
	ffstream.o \
	audioplayer.o \
	ffplayer.o \
	-L$FOUNDATIONPATH \
	-lavformat \
	-lavcodec \
	-lswscale \
	-lavutil \
	-lswresample


echo "roger done"
