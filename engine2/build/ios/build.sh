#!/bin/bash

#CC=/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin/llvm-g++
CC=/Applications/Xcode4.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/clang
LIBTOOL=/Applications/Xcode4.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin/libtool
LIPO=/Applications/Xcode4.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin/lipo
CFLAGS="-DNDEBUG --sysroot=/Applications/Xcode4.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS6.1.sdk -arch armv7 -DOS_IOS -Wno-deprecated-declarations"
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
	$PLAYERPATH/ffplayer.cpp \
	$PLAYERPATH/ffrender.cpp

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
	ffrender.o \
	-L$FOUNDATIONPATH \
	-lavformat \
	-lavcodec \
	-lswscale \
	-lavutil \
	-lswresample


echo "roger done"
