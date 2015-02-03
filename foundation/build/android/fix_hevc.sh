#!/bin/sh

CC=/d/Software/android-ndk-r9d/toolchains/arm-linux-androideabi-4.8/prebuilt/windows/bin/arm-linux-androideabi-gcc
SOURCE_FOLDER=libavcodec/arm
SOURCE=$SOURCE_FOLDER/hevcdsp_epel_neon.S

cd ../../foundation

echo haha

cd $SOURCE_FOLDER
rm -r hevcdsp*.o hevcdsp*.d
cd ../..

#============ get the file name ===========  
Folder_A="libavcodec/arm"  
for file_a in ${Folder_A}/hevcdsp*.S ${Folder_A}/hevcdsp*.c; do  
    temp_file=`basename $file_a`
	echo "build $file_a"
	$CC $file_a -c -o $file_a.o --sysroot=D:/Software/android-ndk-r9d/platforms/android-9/arch-arm -D_ISOC99_SOURCE -D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE -Dstrtod=avpriv_strtod -DPIC -DNDEBUG -mandroid -ftree-vectorize -ffunction-sections -funwind-tables -fomit-frame-pointer -funswitch-loops -finline-limit=300 -finline-functions -fpredictive-commoning -fgcse-after-reload -fipa-cp-clone -mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mvectorize-with-neon-quad -fstack-protector -fstrict-aliasing -march=armv7-a -std=c99 -fomit-frame-pointer -fPIC -mthumb -Wdeclaration-after-statement -Wall -Wno-parentheses -Wno-switch -Wno-format-zero-length -Wdisabled-optimization -Wpointer-arith -Wredundant-decls -Wno-pointer-sign -Wwrite-strings -Wtype-limits -Wundef -Wmissing-prototypes -Wno-pointer-to-int-cast -Wstrict-prototypes -O2 -fno-math-errno -fno-signed-zeros -fno-tree-vectorize -Werror=implicit-function-declaration -Werror=missing-prototypes -Werror=return-type -Werror=vla -I.
done   
exit

$CC $SOURCE -c -o libavcodec/arm/hevcdsp_epel_neon.o -O3 -march=armv7-a -mfpu=neon -mfloat-abi=softfp -mtune=cortex-a5 -DCAN_USE_VFP_INSTRUCTIONS=1 -DCAN_USE_ARMV7_INSTRUCTIONS=1 -DCAN_USE_UNALIGNED_ACCESSES=1 -fPIC -DANDROID -mthumb-interwork -ffunction-sections -funwind-tables -fstack-protector -fno-short-enums -fomit-frame-pointer -fno-strict-aliasing -finline-limit=64 -DANDROID -Wa,--noexecstack -MMD -MP -I/home/guoliangma/out_x264_neon/include -I/home/guoliangma/out_fdk-aac_gcc/include -march=armv7-a -std=c99 -fomit-frame-pointer -fPIC -mthumb -pthread -g -Wdeclaration-after-statement -Wall -Wdisabled-optimization -Wpointer-arith -Wredundant-decls -Wwrite-strings -Wtype-limits -Wundef -Wmissing-prototypes -Wno-pointer-to-int-cast -Wstrict-prototypes -Wempty-body -Wno-parentheses -Wno-switch -Wno-format-zero-length -Wno-pointer-sign -O3 -fno-math-errno -fno-signed-zeros -fno-tree-vectorize -Werror=implicit-function-declaration -Werror=missing-prototypes -Werror=return-type -Werror=vla -Wno-maybe-uninitialized -I.

