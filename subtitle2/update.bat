@echo off

cd build\android

rem call ndk-build clean
call ndk-build -j4
copy obj\local\armeabi\libsubtitle.a ..\..\output\android\libs\armeabi\

@pause
