#include "jniUtils.h"
#define LOG_TAG "JNI-UTILS"
#include "pplog.h"
#include "FFMediaPlayer.h"
#include "FFMediaExtractor.h"
#include "platform/autolock.h" // for pthread

#include <stdio.h>
#include <stdlib.h> // for strxxx

JavaVM *gs_jvm = NULL;
pthread_mutex_t sLock;

char* vstrcat_impl(const char* first, ...)
{
	size_t len = 0;
	char* buf = NULL;
	va_list args;
	char* p = NULL;


	if (first == NULL)
		return NULL;

	len = strlen(first);

	va_start(args, first);
	while((p = va_arg(args, char*)) != NULL)
		len += strlen(p);
	va_end(args);

	buf = (char *)malloc(len + 1);

	strcpy(buf, first);

	va_start(args, first);
	while((p = va_arg(args, char*)) != NULL)
		strcat(buf, p);
	va_end(args);

	return buf;
}

char* jstr2cstr(JNIEnv* env, const jstring jstr)
{
	char* cstr = NULL;
	if (env != NULL)
	{
		const char* tmp = env->GetStringUTFChars(jstr, NULL);
		const size_t len = strlen(tmp) + 1;
		cstr = (char*)malloc(len);
		memset(cstr, 0, len);
		snprintf(cstr, len, "%s", tmp);
		env->ReleaseStringUTFChars(jstr, tmp);
	}

	return cstr;
}

jstring cstr2jstr(JNIEnv* env, const char* cstr)
{
	jstring jstr = NULL;
	if (env != NULL && cstr != NULL)
	{
		jstr = env->NewStringUTF(cstr);
	}

	return jstr;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	PPLOGI("JNI_OnLoad");

	JNIEnv* env = NULL;
	jint result = -1;

	if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		AND_LOGE("ERROR: GetEnv failed");
		goto bail;
	}

	if (env == NULL) {
		goto bail;
	}

	if (register_android_media_MediaPlayer(env) < 0) {
		AND_LOGE("ERROR: MediaPlayer native registration failed");
		goto bail;
	}

	pthread_mutex_init(&sLock, NULL);

	if (register_android_media_MediaExtractor(env) < 0) {
		AND_LOGE("ERROR: MediaExtractor native registration failed");
		goto bail;
	}

	//save jvm for multiple thread invoking to java application.
	gs_jvm = vm;

    pplog_init();
	
	/* success -- return valid version number */
	result = JNI_VERSION_1_4;

bail:
	return result;
}

void JNI_OnUnload(JavaVM* vm, void* reserved)
{
	PPLOGI("JNI_OnUnload");

	unload_player();

	pthread_mutex_destroy(&sLock);

	pplog_close();
}

JNIEnv* getJNIEnvPP()
{
	if (gs_jvm == NULL) {
		PPLOGE("gs_jvm is null");
		return NULL;
	}

	JNIEnv* env = NULL;

	if (gs_jvm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		PPLOGE("failed to GetEnv()");
		return NULL;
	}

	return env;
}

JNIEnv* getAttachedJNIEnv()
{
	if (gs_jvm == NULL) {
		PPLOGE("gs_jvm is null");
		return NULL;
	}

	JNIEnv* env = NULL;
	int status;

	status = gs_jvm->GetEnv((void**) &env, JNI_VERSION_1_4);
	if (JNI_OK == status)
		return env;
	else if (status == JNI_EDETACHED) {
		PPLOGI("AttachCurrentThread: JNI_EDETACHED");
		status = gs_jvm->AttachCurrentThread(&env, NULL);
		if (status != JNI_OK) {
			PPLOGE("AttachCurrentThread failed %d", status);
			return NULL;
		}

		return env;
	}
	else {
		PPLOGE("GetEnv failed %d", status);
	}

	return NULL;
}

void detachJNIEnv()
{
	int status;

	if (gs_jvm == NULL) {
		PPLOGE("gs_jvm is null");
		return;
	}

	status = gs_jvm->DetachCurrentThread();
	if (status != JNI_OK) {
		PPLOGE("DetachCurrentThread failed %d", status);
		return;
	}
	
	PPLOGI("CurrentThread Detached");
}
