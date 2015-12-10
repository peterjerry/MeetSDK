#ifndef _COMMON_H_
#define _COMMON_H_

#ifdef __ANDROID__

#include <jni.h>
#include "platforminfo.h"
#include "log.h"

// declared in meetsdk jni
extern int __pp_log_vprint(int prio, const char *tag, const char *fmt, va_list ap);
extern LogFunc pplog;

// if BUILD_ONELIB gs_jvm, platformInfo declared in meetsdk jni, else decleard in common.cpp
extern JavaVM* gs_jvm;
extern PlatformInfo* platformInfo;

#endif

#endif // _COMMON_H_