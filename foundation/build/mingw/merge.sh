#!/bin/bash

cd ../../foundation_rext

TARGET1=../output/mingw
ASM_OBJ="libavutil/x86/*.o libavcodec/x86/*.o libswresample/x86/*.o libswscale/x86/*.o"

# static lib
ar -r $TARGET1/libffmpeg.a libavutil/*.o libavcodec/*.o \
	libavformat/*.o libswresample/*.o libswscale/*.o $ASM_OBJ

cd ..
