#ifndef _JNI_UTILS_H_
#define _JNI_UTILS_H_

#include <jni.h>

#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

// util method
#define vstrcat(first, ...) (vstrcat_impl(first, __VA_ARGS__, (char*)NULL))

char* vstrcat_impl(const char* first, ...);

char* jstr2cstr(JNIEnv* env, const jstring jstr);

jstring cstr2jstr(JNIEnv* env, const char* cstr);

JNIEnv* getJNIEnvPP();

JNIEnv* getAttachedJNIEnv();

void detachJNIEnv();

#endif // _JNI_UTILS_H_