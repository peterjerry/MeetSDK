# !bin/sh

ABIS="armeabi x86 arm64"
ROOT_PATH=`pwd`

echo begin to build foundation
cd ${ROOT_PATH}/../foundation
if [ ! -d output/android ]; then
	mkdir -p output/android
fi

cd build/android
chmod +x ./*.sh
for ABI in $ABIS; do
	echo process ${ABI} foundation lib
	./config.sh neon
	./build.sh neon
	./merge.sh neon
done

echo begin to build subtile2
SUBTITLE_ABIS="armeabi x86 arm64-v8a"
cd ${ROOT_PATH}/../subtile2
if [ ! -d output/android ]; then
	for ABI in $SUBTITLE_ABIS; do
		mkdir -p output/android/${ABI}
	done
fi

cd build/android
ndk-build -j4 -B

for ABI in $SUBTITLE_ABIS; do
	echo copy ${ABI} subtitle lib
	cp obj/local/${ABI}/*.a ../../output/android/lib/${ABI}/
done

echo begin to build engine2
cd ${ROOT_PATH}/../engine2/build/android
ndk-build -j4 -B

for ABI in $ABIS; do
	echo process ${ABI} lib
	if [ ! -d ../../output/android/${ABI}/ ]; then
		mkdir -p ../../output/android/${ABI}/
		mkdir -p ../../output/android/${ABI}/debug/
	fi
	cp -f libs/${ABI}/libplayer_neon.so ../../output/android/${ABI}/
	cp -f obj/local/${ABI}/libplayer_neon.so ../../output/android/${ABI}/debug/
done

echo begin to build meetsdk
cd ${ROOT_PATH}/MeetSDK
cd jni
chmod +x gen_version.sh
./gen_version.sh
cd ..

android update project -p . -n MeetSDK
ant clean
ant debug

#cd ../MeetPlayer
#android update project -p . -n MeetPlayer
#ant clean
#ant debug
