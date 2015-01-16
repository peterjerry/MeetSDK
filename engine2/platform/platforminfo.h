#ifndef PLATFORMINFO_H_
#define PLATFORMINFO_H_

#include <jni.h>

#define STRLEN 256

typedef struct PlatformInfo {

    char release_version[STRLEN];
    char model_name[STRLEN];
    char board_name[STRLEN];
    char chip_name[STRLEN];
    char manufacture_name[STRLEN];
    char app_path[STRLEN];
	char lib_path[STRLEN];
    char ppbox_lib_name[STRLEN];
    int sdk_version;

    void* ppbox;
    void* jvm;
    void* pplog_func;
    jobject javaSurface; // never use now
} PlatformInfo;


#endif
