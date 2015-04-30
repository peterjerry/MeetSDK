@echo off
setlocal enabledelayedexpansion
set PROJECT_PROPERTITY_FILE=project.properties
if %1% == quick (
  echo quick build
  goto quick
) else (
  echo normal build
  goto normal
)

:quick
(for /f "tokens=*" %%i in (%PROJECT_PROPERTITY_FILE%) do (
set s=%%i
set s=!s:android.library=#android.library!
set s=!s:##=#!
echo !s!))>temp.txt
move /y temp.txt "%PROJECT_PROPERTITY_FILE%"
exit

:normal
(for /f "tokens=*" %%i in (%PROJECT_PROPERTITY_FILE%) do (
set s=%%i
set s=!s:#android.library=android.library!
echo !s!))>temp.txt
move /y temp.txt "%PROJECT_PROPERTITY_FILE%"
exit