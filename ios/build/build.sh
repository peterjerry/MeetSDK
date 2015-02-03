#/bin/bash

function build_ppmediaplayer {
echo "build player for armv7 & arm64"
cd ../workspace/player
xcodebuild -project player.xcodeproj clean
xcodebuild -project player.xcodeproj build RUN_CLANG_STATIC_ANALYZER=NO ONLY_ACTIVE_ARCH=NO 

echo "build PPMediaPlayer"
cd ../PPMediaPlayer
xcodebuild -project PPMediaPlayer.xcodeproj clean
xcodebuild -project PPMediaPlayer.xcodeproj build RUN_CLANG_STATIC_ANALYZER=NO ONLY_ACTIVE_ARCH=NO 
mv ../../output/PPMediaPlayer.framework ../../output/armv7/
echo "build armv7 end"

echo "build player for i386"
cd ../player
xcodebuild -project player.xcodeproj clean
xcodebuild -project player.xcodeproj -sdk "iphonesimulator" VALID_ARCHS=i386\ x86_64

echo "build PPMediaPlayer"
cd ../PPMediaPlayer
xcodebuild -project PPMediaPlayer.xcodeproj clean
xcodebuild -project PPMediaPlayer.xcodeproj -sdk "iphonesimulator" VALID_ARCHS=i386\ x86_64
cp -rp ../../output/PPMediaPlayer.framework ../../output/i386/
echo "build i386 end"

echo "begin morge"
cd ../../output
rm PPMediaPlayer.framework/PPMediaPlayer 
lipo -create armv7/PPMediaPlayer.framework/PPMediaPlayer i386/PPMediaPlayer.framework/PPMediaPlayer  -output PPMediaPlayer.framework/PPMediaPlayer

rm -f *.zip
eval $(LANG=C svn info | awk -F ': ' '$1 == "Revision" {printf("version=%s",$2)}')
zip -rm PPMediaPlayer_$version.zip PPMediaPlayer.framework
}

rm -rf ../output/*
rm -rf ../lib/*
mkdir -p ../output/armv7 
mkdir -p ../output/i386
build_ppmediaplayer
rm -rf ../output/armv7 ../output/i386

