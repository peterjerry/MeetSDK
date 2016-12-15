@echo off

del /S build\outputs\apk\*.apk
call gradle assembleRelease
copy /Y /B build\Outputs\apk\MeetPlayer-release.apk \\192.168.80.250\sc\_michael\app\MeetPlayer-arm-release.apk
curl -u web:toTo2046 -T build\Outputs\apk\MeetPlayer-release.apk ftp://205.209.187.189//usr/share/nginx/html/app/MeetPlayer-arm-release.apk
@pause

rem call gradle assembleRelease_x86
rem copy /Y build\Outputs\apk\MeetPlayer-release_x86.apk \\192.168.1.169\share\www\test\MeetPlayer-x86-release.apk
rem curl -u web:daqiao1 -T build\Outputs\apk\MeetPlayer-release_x86.apk ftp://42.62.105.235//usr/local/live/gotye_srs_150512/trunk/objs/nginx-1.5.7/_release/html/test/app/MeetPlayer-arm-release.apk