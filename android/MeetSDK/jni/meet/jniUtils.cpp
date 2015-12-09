#include "jniUtils.h"
#define LOG_TAG "JNI-UTILS"
#include "pplog.h"
#ifdef BUILD_FFPLAY
#include "FFMediaPlayer.h"
#endif
#ifdef BUILD_FFEXTRACTOR
#include "FFMediaExtractor.h"
#endif
#include "libplayer.h"
#include "platform/platforminfo.h"
#include "platform/autolock.h" // for pthread

#include <stdio.h>
#include <stdlib.h> // for strxxx

#define LOG_FATAL_IF(cond, ...) if (cond) { PPLOGE(__VA_ARGS__); return -1;}

JavaVM *gs_jvm = NULL;
PlatformInfo* gPlatformInfo = NULL;
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

#ifdef BUILD_FFPLAY
	if (register_android_media_MediaPlayer(env) < 0) {
		AND_LOGE("ERROR: MediaPlayer native registration failed");
		goto bail;
	}
#endif

#ifdef BUILD_FFEXTRACTOR
	if (register_android_media_MediaExtractor(env) < 0) {
		AND_LOGE("ERROR: MediaExtractor native registration failed");
		goto bail;
	}
#endif

	pthread_mutex_init(&sLock, NULL);

	//save jvm for multiple thread invoking to java application.
	gs_jvm = vm;

	gPlatformInfo = new PlatformInfo();
    gPlatformInfo->jvm = (void*)gs_jvm;
	gPlatformInfo->pplog_func = (void*)__pp_log_vprint;

    pplog_init();
	
	/* success -- return valid version number */
	result = JNI_VERSION_1_4;

bail:
	return result;
}

void JNI_OnUnload(JavaVM* vm, void* reserved)
{
	PPLOGI("JNI_OnUnload");
#if defined(BUILD_FFPLAY) || defined(BUILD_FFEXTRACTOR)
	unloadPlayerLib();
#endif

	pthread_mutex_destroy(&sLock);

	if (gPlatformInfo) {
		delete gPlatformInfo;
		gPlatformInfo = NULL;
	}

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

int jniRegisterNativeMethodsPP(JNIEnv* env, const char* className, const JNINativeMethod* gMethods, int numMethods)
{
	jclass clazz;

	PPLOGD("Registering %s natives", className);
	clazz = env->FindClass(className);
	if (clazz == NULL) {
		PPLOGE("Native registration unable to find class '%s'", className);
		return -1;
	}

	int result = 0;
	if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
		PPLOGE("RegisterNatives failed for '%s'", className);
		result = -1;
	}

	env->DeleteLocalRef(clazz);
	return result;
}

/*
 * Throw an exception with the specified class and an optional message.
 *
 * If an exception is currently pending, we log a warning message and
 * clear it.
 *
 * Returns 0 if the specified exception was successfully thrown.  (Some
 * sort of exception will always be pending when this returns.)
 */
int jniThrowException(JNIEnv* env, const char* className, const char* msg)
{
    jclass exceptionClass;

    if (env->ExceptionCheck()) {
        /* TODO: consider creating the new exception with this as "cause" */
        char buf[256];

        jthrowable exception = env->ExceptionOccurred();
        env->ExceptionClear();
    }

    exceptionClass = env->FindClass(className);
    if (exceptionClass == NULL) {
        PPLOGE("Unable to find exception class %s\n", className);
        /* ClassNotFoundException now pending */
        return -1;
    }

    int result = 0;
    if (env->ThrowNew(exceptionClass, msg) != JNI_OK) {
        PPLOGE("Failed throwing '%s' '%s'\n", className, msg);
        /* an exception, most likely OOM, will now be pending */
        result = -1;
    }

    env->DeleteLocalRef(exceptionClass);
    return result;
}

// Returns the Unix file descriptor for a ParcelFileDescriptor object
int getParcelFileDescriptorFDPP(JNIEnv* env, jobject object)
{
	jclass clazz = env->FindClass("java/io/FileDescriptor");
	LOG_FATAL_IF(clazz == NULL, "Unable to find class java.io.FileDescriptor");
	jfieldID descriptor = env->GetFieldID(clazz, "descriptor", "I");
	LOG_FATAL_IF(descriptor == NULL, "Unable to find descriptor field in java.io.FileDescriptor");

	return env->GetIntField(object, descriptor);
}

bool IsUTF8(const void* pBuffer, long size)  
{
	bool IsUTF8 = true;  
    unsigned char* start = (unsigned char*)pBuffer;  
    unsigned char* end = (unsigned char*)pBuffer + size;  
    while (start < end) {  
        if (*start < 0x80) // (10000000): 值小于0×80的为ASCII字符  
        {  
            start++;  
        }  
        else if (*start < (0xC0)) // (11000000): 值介于0×80与0xC0之间的为无效UTF-8字符  
        {  
            IsUTF8 = false;  
            break;  
        }  
        else if (*start < (0xE0)) // (11100000): 此范围内为2字节UTF-8字符  
        {  
            if (start >= end - 1)   
                break;  
            if ((start[1] & (0xC0)) != 0x80)  
            {  
                IsUTF8 = false;  
                break;  
            }  
            start += 2;  
        }   
        else if (*start < (0xF0)) // (11110000): 此范围内为3字节UTF-8字符  
        {  
            if (start >= end - 2)   
                break;  
            if ((start[1] & (0xC0)) != 0x80 || (start[2] & (0xC0)) != 0x80)  
            {  
                IsUTF8 = false;  
                break;  
            }  
            start += 3;  
        }   
        else  
        {  
            IsUTF8 = false;  
            break;  
        }  
    }  

    return IsUTF8;  
}

void correctUtfBytes(char* bytes)
{
	char three = 0;
	while (*bytes != '\0') {
		unsigned char utf8 = *(bytes++);
		three = 0;
		// Switch on the high four bits.
		switch (utf8 >> 4) {
		case 0x00:
		case 0x01:
		case 0x02:
		case 0x03:
		case 0x04:
		case 0x05:
		case 0x06:
		case 0x07:
			// Bit pattern 0xxx. No need for any extra bytes.
			break;
		case 0x08:
		case 0x09:
		case 0x0a:
		case 0x0b:
		case 0x0f:
			/*
			* Bit pattern 10xx or 1111, which are illegal start bytes.
			* Note: 1111 is valid for normal UTF-8, but not the
			* modified UTF-8 used here.
			*/
			*(bytes-1) = '?';
			break;
		case 0x0e:
			// Bit pattern 1110, so there are two additional bytes.
			utf8 = *(bytes++);
			if ((utf8 & 0xc0) != 0x80) {
				--bytes;
				*(bytes-1) = '?';
				break;
			}
			three = 1;
			// Fall through to take care of the final byte.
		case 0x0c:
		case 0x0d:
			// Bit pattern 110x, so there is one additional byte.
			utf8 = *(bytes++);
			if ((utf8 & 0xc0) != 0x80) {
				--bytes;
				if(three)
					--bytes;
				*(bytes-1)='?';
			}
			break;
		}
	}
}
