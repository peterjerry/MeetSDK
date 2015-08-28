# !bin/sh

COMMON_ABIS="armeabi x86 arm64-v8a"
FOUNDATION_ABIS="neon x86 arm64-v8a"
SUBTITLE_ABIS="armeabi x86 arm64-v8a"
ROOT_PATH=`pwd`

#$build_foundation

#$build_subtitle2

#$build_engine2

#$build_meetsdk

#build_foundation() {
echo begin to build foundation
cd ${ROOT_PATH}/../foundation
chmod +x foundation_rext/configure
chmod +x foundation_rext/*.sh

if [ ! -d output/android ]; then
	mkdir -p output/android
fi

cd build/android
chmod +x ./*.sh
for ABI in $FOUNDATION_ABIS; do
	echo build ${ABI} foundation lib
	./config.sh ${ABI}
	./build.sh ${ABI}
	./merge.sh ${ABI}
done
#}

#build_subtitle2() {
echo begin to build subtile2
cd ${ROOT_PATH}/../subtitle2
if [ ! -d output/android ]; then
	mkdir -p output/android/include
	for ABI in $COMMON_ABIS; do
		mkdir -p output/android/lib/${ABI}
	done
fi

cd build/android
ndk-build -j4 -B

cp ../../src/subtitle/subtitle.h ../../output/android/include/
for ABI in $COMMON_ABIS; do
	echo copy ${ABI} subtitle lib
	cp obj/local/${ABI}/*.a ../../output/android/lib/${ABI}/
done
#}

#build_engine2() {
echo begin to build engine2
cd ${ROOT_PATH}/../engine2/build/android
ndk-build -j4 -B

for ABI in $COMMON_ABIS; do
	echo process ${ABI} lib
	if [ ! -d ../../output/android/${ABI}/ ]; then
		mkdir -p ../../output/android/${ABI}/
		mkdir -p ../../output/android/${ABI}/debug/
	fi
	cp -f libs/${ABI}/libplayer_neon.so ../../output/android/${ABI}/
	cp -f obj/local/${ABI}/libplayer_neon.so ../../output/android/${ABI}/debug/
done
#}

#build_meetsdk() {
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
#}
