#ifndef _PP_LOG_H_
#define _PP_LOG_H_

#include <android/log.h>

#ifndef LOG_TAG
#define LOG_TAG "unknown tag"
#endif

#ifdef NDEBUG
#define LOG_NDEBUG 1
#else
#define LOG_NDEBUG 0
#endif

#if LOG_NDEBUG
#define PPLOGV(...) ((void)0)
#else
#define PPLOGV(...) __pp_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#endif

#if LOG_NDEBUG
#define PPLOGD(...) ((void)0)
#else
#define PPLOGD(...) __pp_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#endif

#define PPLOGI(...) __pp_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define PPLOGW(...) __pp_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

#define PPLOGE(...) __pp_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define AND_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

int pplog_init();

int __pp_log_print(int prio, const char *tag, const char *fmt, ...);

int __pp_log_vprint(int prio, const char *tag, const char *fmt, va_list ap);

void pplog_close();

#endif // _PP_LOG_H_
