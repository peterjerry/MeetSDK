@echo off

cd build\android

call ndk-build clean
call ndk-build -j4
copy libs\armeabi\libplayer_neon.so ..\..\output\android\armeabi\
copy obj\local\armeabi\libplayer_neon.so ..\..\output\android\armeabi\debug\

copy ..\..\output\android\armeabi\libplayer_neon.so ..\..\..\android\MeetSDK\libs\armeabi\

cd ..\..\..\android\MeetPlayer
call ant debug install
copy /Y bin\MeetPlayer-debug.apk \\172.16.204.106\web\test\test\
@pause
exit

rem adb push ..\..\output\android\libplayer_neon.so /storage/sdcard0/
adb push ..\..\output\android\libplayer_neon.so /mnt/sdcard/

rem create cmd list
rem echo su > temp.txt
rem echo cd /data/data/com.pplive.androidphone/lib >> temp.txt

adb shell < update_cmd.txt
rem del temp.txt

@pause
