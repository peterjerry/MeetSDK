#ifndef JNI_LOG_H
#define JNI_LOG_H

#ifdef __ANDROID__
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
#endif

#endif // JNI_LOG_H