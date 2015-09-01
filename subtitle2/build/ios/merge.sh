#!/bin/sh

release_version=1.2.3

rm -rf output
mkdir output
mkdir -p output/include
mkdir -p output/lib

cp ../../src/subtitle/subtitle.h output/include/

OUTPUT_DIST=output/lib
FILE=libsubtitle.a
ARCHS="arm64 armv7s armv7 x86_64 i386"
for ARCH in $ARCHS
do
    mkdir -p output/$ARCH
done

xcrun -sdk iphoneos lipo -output output/i386/$FILE -thin i386 subtitle2/subtitle/build/Release-iphonesimulator/$FILE
xcrun -sdk iphoneos lipo -output output/x86_64/$FILE -thin x86_64 subtitle2/subtitle/build/Release-iphonesimulator/$FILE
xcrun -sdk iphoneos lipo -output output/armv7/$FILE -thin armv7 subtitle2/subtitle/build/Release-iphoneos/$FILE
xcrun -sdk iphoneos lipo -output output/arm64/$FILE -thin arm64 subtitle2/subtitle/build/Release-iphoneos/$FILE

xcrun -sdk iphoneos lipo -output $OUTPUT_DIST/$FILE -create \
    -arch i386 output/i386/$FILE \
    -arch x86_64 output/x86_64/$FILE \
    -arch armv7 output/armv7/$FILE \
    -arch arm64 output/arm64/$FILE
xcrun -sdk iphoneos lipo -info $OUTPUT_DIST/$FILE

for ARCH in $ARCHS
do
    rm -r output/$ARCH
done

cd output
zip -r subtitle2_univernal_${release_version}.zip ./
