#include "common.h"
#include <stdio.h>

#ifdef __ANDROID__

#ifndef BUILD_ONE_LIB
JavaVM* gs_jvm = NULL;
PlatformInfo* platformInfo = NULL;
#endif
LogFunc pplog = NULL;

#endif