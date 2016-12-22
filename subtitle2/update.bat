@echo off

cd build\android

rem call ndk-build clean
call ndk-build -j4
copy obj\local\armeabi-v7a\libsubtitle.a ..\..\output\android\lib\armeabi-v7a\
copy obj\local\x86\libsubtitle.a ..\..\output\android\lib\x86\
copy obj\local\arm64-v8a\libsubtitle.a ..\..\output\android\lib\arm64-v8a\

@pause
