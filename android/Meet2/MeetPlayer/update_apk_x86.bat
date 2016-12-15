@echo off

del /S build\outputs\apk\*.apk

call gradle assembleRelease_x86
copy /Y build\Outputs\apk\MeetPlayer-release_x86.apk \\192.168.80.250\sc\_michael\app\MeetPlayer-x86-release.apk
curl -u web:toTo2046 -T build\Outputs\apk\MeetPlayer-release_x86.apk ftp://205.209.187.189//usr/share/nginx/html/app//MeetPlayer-x86-release.apk
@pause