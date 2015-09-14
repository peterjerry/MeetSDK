@echo off

call ant release
copy /Y bin\MeetPlayer-release.apk \\172.16.204.106\web\test\test\
@pause

ant installd