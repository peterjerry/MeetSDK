#!/bin/bash

TARGET_VERSION=v8

HOME_FODLER=`pwd`
FFMPEG_HOME=$HOME_FODLER/../../foundation_rext
OUTPUT_I386=$HOME_FODLER/../../output/ios/$TARGET_VERSION/i386
OUTPUT_X86_64=$HOME_FODLER/../../output/ios/$TARGET_VERSION/x86_64
OUTPUT_ARMV7=$HOME_FODLER/../../output/ios/$TARGET_VERSION/armv7
OUTPUT_ARMV7S=$HOME_FODLER/../../output/ios/$TARGET_VERSION/armv7s
OUTPUT_ARM64=$HOME_FODLER/../../output/ios/$TARGET_VERSION/arm64

OUTPUT_DIST=$HOME_FODLER/../../output/ios/$TARGET_VERSION

if [ ! -d $OUTPUT_DIST/lib ]; then
mkdir -p $OUTPUT_DIST/lib
fi

cp $FFMPEG_HOME/config.h $OUTPUT_DIST/
cp -r $OUTPUT_ARMV7/include $OUTPUT_DIST/

cd $OUTPUT_ARMV7/lib
for file in *.a
do
cd - > /dev/null
xcrun -sdk iphoneos lipo -output $OUTPUT_DIST/lib/$file -create \
	-arch i386 $OUTPUT_I386/lib/$file \
	-arch x86_64 $OUTPUT_X86_64/lib/$file \
	-arch armv7 $OUTPUT_ARMV7/lib/$file \
	-arch armv7s $OUTPUT_ARMV7S/lib/$file \
	-arch arm64 $OUTPUT_ARM64/lib/$file

echo "fat $file created."

xcrun -sdk iphoneos lipo -info $OUTPUT_DIST/lib/$file
cd - > /dev/null
done


