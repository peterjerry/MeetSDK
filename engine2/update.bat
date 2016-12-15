@echo off

cd build\android

rem call ndk-build clean
call ndk-build -j4
copy libs\armeabi-v7a\libplayer.so ..\..\output\android\armeabi-v7a\
copy obj\local\armeabi-v7a\libplayer.so ..\..\output\android\armeabi-v7a\debug\
copy libs\x86\libplayer.so ..\..\output\android\x86\
copy obj\local\x86\libplayer.so ..\..\output\android\x86\debug\

rem copy obj\local\armeabi\libplayer.a ..\..\output\android\armeabi\
rem copy obj\local\armeabi\libplayer.a ..\..\output\android\armeabi\debug\
copy libs\x86\libplayer.so ..\..\output\android\x86\
copy obj\local\x86\libplayer.so ..\..\output\android\x86\debug\

rem copy ..\..\output\android\armeabi\libplayer.so ..\..\..\android\MeetSDK\libs\armeabi\
@pause
exit

cd ..\..\..\android\MeetPlayer
call ant debug install
copy /Y bin\MeetPlayer-debug.apk \\172.16.204.106\web\test\test\
@pause
exit

rem adb push ..\..\output\android\libplayer.so /storage/sdcard0/
adb push ..\..\output\android\libplayer_neon.so /mnt/sdcard/

rem create cmd list
rem echo su > temp.txt
rem echo cd /data/data/com.pplive.androidphone/lib >> temp.txt

adb shell < update_cmd.txt
rem del temp.txt

@pause
