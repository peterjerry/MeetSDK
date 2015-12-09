@echo off

call ant release
copy /Y bin\MeetPlayer-release.apk \\192.168.1.112\share\www\test\
@pause

ant installd