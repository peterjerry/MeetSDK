/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */

#ifndef FF_ANDROID_LOG_H
#define FF_ANDROID_LOG_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdarg.h>

#include <jni.h>
#include <android/log.h>

typedef int(*LogFunc)(int, const char *, const char *, va_list);

int __pp_log_print(int prio, const char *tag,  const char *fmt, ...);

//#ifndef LOG_NDEBUG
#ifdef NDEBUG
#define LOG_NDEBUG 1
#else
#define LOG_NDEBUG 0
#endif
//#endif

#if LOG_NDEBUG
#define LOGV(...) ((void)0)
#else
#define LOGV(...) __pp_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)
#endif

#if LOG_NDEBUG
#define LOGD(...) ((void)0)
#else
#define LOGD(...) __pp_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#endif

#define LOGI(...) __pp_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

#define LOGW(...) __pp_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)

#define LOGE(...) __pp_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

void ff_log_callback(void* avcl, int level, const char* fmt, va_list vl);

#ifdef __cplusplus
}
#endif

#endif // FF_ANDROID_LOG_H
