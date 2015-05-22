@echo off

rem 使用ffmpeg混合多段音频，把mix.bat和ffmpeg.exe放在音频文件的同一文件夹下。

rem 用法：mix.bat input1 input2 input3 ... output
rem 例子：mix.bat 001_kisa_38 001_masato_55 001_ki38_ma55

rem 可以修改mix.bat改变扩展名和输出比特率

set execution=ffmpeg.exe
set extension=.mp3
set bitrate=48k

set "f=%*"
if not defined f (echo 没有参数! & goto :eof)
set /a x=0
for %%i in (%*) do (call set /a x+=1)
set /a y=1
set command=%execution%
:loop
if %y% EQU %x% goto :convert
echo 第%y%个音频:    %1%extension%
set "command=%command% -i %1%extension%"
set /a y+=1
shift
goto loop
:convert
set /a x-=1
set "command=%command% -b:a %bitrate% -filter_complex amix=inputs=%x%:duration=longest:dropout_transition=0 %1%extension%"
echo %command%
call %command%
