#!/bin/bash

NDK=E:/Software/android-ndk-r10d

ADDR=$NDK/toolchains/arm-linux-androideabi-4.6/prebuilt/windows/bin/arm-linux-androideabi-addr2line

echo "addr $1"
$ADDR -C -f -e ../../output/android/armeabi-v7a/debug/libplayer.so $1
#$ADDR -C -f -e ../../output/android/armeabi-v7a/debug/libmeet.so $1
#$ADDR -C -f -e ../../foundation/output/android/v7_neon/libffmpeg_nostrip.so $1