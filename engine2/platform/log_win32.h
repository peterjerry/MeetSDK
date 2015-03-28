/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */

#ifndef FF_WIN32_LOG_H
#define FF_WIN32_LOG_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdio.h>
#include <stdarg.h>

typedef enum WIN32_LOGPriority {
	WIN32_LOG_UNKNOWN = 0,
    WIN32_LOG_DEFAULT,    /* only for SetMinPriority() */
    WIN32_LOG_VERBOSE,
    WIN32_LOG_DEBUG,
    WIN32_LOG_INFO,
    WIN32_LOG_WARN,
    WIN32_LOG_ERROR,
    WIN32_LOG_FATAL,
    WIN32_LOG_SILENT,     /* only for SetMinPriority(); must be last */
} WIN32_LOGPriority;

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
#define LOGV(...) __pp_log_print(WIN32_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#endif

#if LOG_NDEBUG
#define LOGD(...) ((void)0)
#else
#define LOGD(...) __pp_log_print(WIN32_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#endif

#define LOGI(...) __pp_log_print(WIN32_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define LOGW(...) __pp_log_print(WIN32_LOG_WARN, LOG_TAG, __VA_ARGS__)

#define LOGE(...) __pp_log_print(WIN32_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifdef __cplusplus
}
#endif

#endif // FF_WIN32_LOG_H
