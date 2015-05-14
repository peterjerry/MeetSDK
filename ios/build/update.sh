#!/bin/bash

FFMPEG_URL="http://jenkins/job/ffmpeg_ios_git/2/artifact/foundation/output/ios/v8/libffmpeg_univernal_1.0.2.5.zip"
FFMPEG_ZIP="ffmpeg.zip"
SUBTITLE_URL="http://jenkins/view/Meet/job/subtitle2_ios/12/artifact/subtitle2/build/ios/output/subtitle2_univernal_1.0.2.8.zip"
SUBTITLE_ZIP="subtitle.zip"

function update_founction
{
cd ../lib
echo "check ffmpeg lib"
lib_arr=("libavcodec.a" "libavutil.a" "libswresample.a" "libavformat.a" "libswscale.a")
for var in ${lib_arr[@]}; do
    if [ ! -f "$var" ]; then
        echo "can't find $var, update by jenkins"
        curl "$FFMPEG_URL" -o $FFMPEG_ZIP
        unzip -o $FFMPEG_ZIP -d ./tmp
        echo "copy lib"
        cp -rp ./tmp/lib/*.a .
        echo "copy include"
        rm -rf ../include/libavcodec ../include/libavformat ../include/libavutil ../include/libswresample ../include/libswscale
        cp -rp ./tmp/include/* ../include/
        rm -rf ./tmp
        rm $FFMPEG_ZIP
        break
    else 
        echo find $var
    fi
done
echo "check finish"

echo "check subtitle"
if [ ! -f "libsubtitle.a" ]; then
	echo "can't find libsubtitle.a, update by jenkins"
	curl "$SUBTITLE_URL" -o $SUBTITLE_ZIP
	unzip -o $SUBTITLE_ZIP -d ./tmp
	echo "copy lib"
	cp -rp ./tmp/lib/libsubtitle.a .
	echo "copy head file"
	rm -rf ../include/subtitle
	mkdir ../include/subtitle
	cp -f ./tmp/include/* ../include/subtitle/
	rm -rf ./tmp
	rm $SUBTITLE_ZIP
fi
echo "check finfish"
cd -
}

update_founction
