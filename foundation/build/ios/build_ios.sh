#!/bin/bash

if [ ${1}x == 'lite'x ]
then
echo "lite build"
build_selection="lite"
else
echo "full build"
build_selection="full"
fi

export PATH=/usr/local/bin/:$PATH

rm -rf ../../output/ios/v8
mkdir -p ../../output/ios/v8

ARCHS="arm64 armv7s armv7 x86_64 i386"
XCODE_VERSION="xc6"
release_version="1.0.3"

for ARCH in $ARCHS
do
    echo "building $ARCH..."
    ./config_$XCODE_VERSION.sh $ARCH $build_selection
    ./build_$XCODE_VERSION.sh $ARCH $build_selection
done

./merge.sh

cd ../../output/ios/v8
rm -rf i386
rm -rf x86_64
rm -rf armv7
rm -rf armv7s
rm -rf arm64
zip -r libffmpeg_univernal_${build_selection}_${release_version}.zip ./
