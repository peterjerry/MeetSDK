#include "pplog.h"
#include "jniUtils.h"
#include <stdio.h>
#include <string.h>

#define LOG_BUF_SIZE	2048

extern JavaVM* gs_jvm;

jclass gs_clazz;
jmethodID gs_mid_log;
static int gs_inited = 0;

int java_log(int level, const char* tag, const char* msg);

int pplog_init()
{
	if (gs_inited)
		return 0;

	if (NULL == gs_jvm)
		return -1;

	JNIEnv* env = NULL;

	if (gs_jvm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK)
		return -1;

	jclass clazz = env->FindClass("com/gotye/meetsdk/util/LogUtils");
	if (NULL == clazz) {
		AND_LOGE("failed to find class com/gotye/meetsdk/util/LogUtils");
		return -1;
	}

	gs_mid_log = env->GetStaticMethodID(clazz, "nativeLog", "(ILjava/lang/String;Ljava/lang/String;)V");
	if (NULL == gs_mid_log) {
		AND_LOGE("failed to find nativeLog methodID");
		return -1;
	}

	gs_clazz = (jclass)env->NewGlobalRef(clazz);

	gs_inited = true;
	PPLOGI("pplog inited");
	return 0;
}

void pplog_close()
{
	JNIEnv* env = NULL;

	if (gs_jvm->GetEnv((void**)&env, JNI_VERSION_1_4) == JNI_OK) {
		env->DeleteGlobalRef(gs_clazz);
	}
}

int __pp_log_print(int prio, const char *tag, const char *fmt, ...)
{
    if (!gs_inited)
		return -1;

	va_list ap;
	char buf[LOG_BUF_SIZE] = {0};

	va_start(ap, fmt);
	vsnprintf(buf, LOG_BUF_SIZE, fmt, ap);
	va_end(ap);

	return java_log(prio, tag, buf);
}

int __pp_log_vprint(int prio, const char *tag, const char *fmt, va_list ap)
{
	if (!gs_inited)
		return -1;

    char buf[LOG_BUF_SIZE];
	vsnprintf(buf, LOG_BUF_SIZE, fmt, ap);
	return java_log(prio, tag, buf);
}

int java_log(int level, const char* tag, const char* msg)
{
	if (!gs_inited)
		return -1;

	JNIEnv* env = NULL;

	if (NULL != gs_jvm)
		gs_jvm->GetEnv((void**)&env, JNI_VERSION_1_4);

	if (!env)
		return -1;

	if (!IsUTF8(msg, strlen(msg))) {
		AND_LOGE("string is not utf-8(java_log): %s", msg);
		return -1;
	}

	jstring jtag = env->NewStringUTF(tag);
	jstring jmsg = env->NewStringUTF(msg);

	env->CallStaticVoidMethod(gs_clazz, gs_mid_log, level, jtag, jmsg);
	env->DeleteLocalRef(jtag);
	env->DeleteLocalRef(jmsg);
	return 0;
}

