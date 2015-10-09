/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */
	 
#ifndef FF_IOS_LOG_H
#define FF_IOS_LOG_H

#import <stdarg.h>
#import <stdio.h>

typedef enum ios_LogPriority {
    IOS_LOG_UNKNOWN = 0,
    IOS_LOG_VERBOSE,
    IOS_LOG_DEBUG,
    IOS_LOG_INFO,
    IOS_LOG_WARN,
    IOS_LOG_ERROR,
} ios_LogPriority;

#ifdef NDEBUG
#define LOG_NDEBUG 1
#else
#define LOG_NDEBUG 0
#endif

#if LOG_NDEBUG
#define LOGV(...) ((void)0)
#else
#define LOGV(...) __ios_log(IOS_LOG_VERBOSE, LOG_TAG,__VA_ARGS__)
#endif

#if LOG_NDEBUG
#define LOGD(...) ((void)0)
#else
#define LOGD(...) __ios_log(IOS_LOG_DEBUG, LOG_TAG,__VA_ARGS__)
#endif

#define LOGI(...) __ios_log(IOS_LOG_INFO, LOG_TAG,__VA_ARGS__)

#define LOGW(...) __ios_log(IOS_LOG_WARN, LOG_TAG,__VA_ARGS__)

#define LOGE(...) __ios_log(IOS_LOG_ERROR, LOG_TAG,__VA_ARGS__)

#if __cplusplus
extern "C" {
#endif

void __ios_log(int prio, const char* tag, const char *fmt, ...);

#if __cplusplus
}
#endif

#endif
