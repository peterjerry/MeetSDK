@echo off

call ant clean
call ant debug
copy /Y bin\MeetPlayer-debug.apk \\172.16.204.106\web\test\test\
@pause

ant installd