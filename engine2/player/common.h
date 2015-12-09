#ifndef _COMMON_H_
#define _COMMON_H_

#ifdef __ANDROID__

#include <jni.h>
#include "platforminfo.h"
#include "log.h"
extern JavaVM* gs_jvm;
extern int __pp_log_vprint(int prio, const char *tag, const char *fmt, va_list ap);

extern PlatformInfo* platformInfo;
extern LogFunc pplog;

#endif

#endif // _COMMON_H_