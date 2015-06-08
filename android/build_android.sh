# !bin/sh

echo begin to build engine2
cd ../engine2/build/android
ndk-build -j4

ABIS="armeabi x86"
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
cd ../../../android/MeetSDK
cd jni
chmod +x gen_version.sh
./gen_version.sh
cd ..

#android update project -p . -n MeetSDK
#ant clean
#ant debug

cd ../MeetPlayer
android update project -p . -n MeetPlayer
ant clean
ant debug
