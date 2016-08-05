@echo off

del /S build\Outputs\apk\*.apk

call gradle assembleRelease_x86
copy /Y build\Outputs\apk\MeetPlayer-release_x86.apk \\192.168.1.130\share\www\test\MeetPlayer-x86-release.apk
curl -u web:daqiao1 -T build\Outputs\apk\MeetPlayer-release_x86.apk ftp://42.62.105.235//usr/local/live/gotye_srs_150512/trunk/objs/nginx-1.5.7/_release/html/test/app/MeetPlayer-x86-release.apk
@pause