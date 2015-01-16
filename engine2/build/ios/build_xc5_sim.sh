#!/bin/bash
SDKVERSION="7.0"
PLATFORM=/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform
CC=`xcrun -find -sdk iphonesimulator clang`
LIBTOOL=`xcrun -find -sdk iphonesimulator libtool`
LIPO=/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/usr/bin/lipo
CFLAGS="-DNDEBUG --sysroot=${PLATFORM}/Developer/SDKs/iPhoneSimulator${SDKVERSION}.sdk -arch i386 -miphoneos-version-min=7.0 -DOS_IOS -Wno-deprecated-declarations"
INCLUDES="-I../../platform -I../../player -I../../foundation/foundation"
#-DNDEBUG
OUTPUTPATH=../../output/ios/v7/i386
PLATFORMPATH=../../platform
PLAYERPATH=../../player
FOUNDATIONPATH="../../foundation/output/ios/v7/i386"

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
