#!/bin/bash

LIB_PATH=../lib
FFMPEGLIB_PATH=../../engine2/foundation/output/ios/v7
LIBSUBTITLE=../../engine2/subtitle/output/ios/libsubtitle.a

if [ ! -f "$LIB_PATH/libsubtitle.a" ]; then
    ln -s $LIBSUBTITLE $LIB_PATH
fi
