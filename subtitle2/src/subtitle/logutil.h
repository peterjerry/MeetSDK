#ifndef _LOG_UTIL_H_
#define _LOG_UTIL_H_

#if defined(__ANDROID__)
#include <jni.h>
#include <android/log.h>

#if LOG_NDEBUG
#define LOGV(...) ((void)0)
#else
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#endif

#if LOG_NDEBUG
#define LOGD(...) ((void)0)
#else
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#endif

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#elif defined(__APPLE__)

#define LOGV(...) ((void)0)
#define LOGD(...) ((void)0)
#define LOGI(...) ((void)0)
#define LOGW(...) ((void)0)
#define LOGE(...) ((void)0)

#else // MSC

int win32_log_print(int prio, const char *tag,  const char *fmt, ...);

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

#define LOGV(...) ((void)0)
#define LOGD(...) ((void)0)
#define LOGI(...) win32_log_print(WIN32_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) win32_log_print(WIN32_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) win32_log_print(WIN32_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#endif

#endif // _LOG_UTIL_H_