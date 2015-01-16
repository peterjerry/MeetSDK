#!/bin/bash
SDKVERSION="6.1"
PLATFORM=/Applications/Xcode4.app/Contents/Developer/Platforms/iPhoneSimulator.platform
#CC=`xcrun -find -sdk iphonesimulator clang`
#CC=/Applications/Xcode4.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/usr/bin/gcc
CC=/Applications/Xcode4.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/clang
#LIBTOOL=`xcrun -find -sdk iphonesimulator libtool`
LIBTOOL=/Applications/Xcode4.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/libtool
CFLAGS="-DNDEBUG --sysroot=${PLATFORM}/Developer/SDKs/iPhoneSimulator${SDKVERSION}.sdk -arch i386 -miphoneos-version-min=6.1 -DOS_IOS -Wno-deprecated-declarations"
INCLUDES="-I../../platform -I../../player -I../../foundation/foundation"
#-DNDEBUG
OUTPUTPATH=../../output/ios/v7/
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
