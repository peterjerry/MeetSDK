#define LOG_TAG "SubTitleDecoder-JNI"

#include <stdio.h>
#include <string.h>
#include <strings.h>
#include <jni.h>
#include <android/log.h>

#include "subtitle.h"

#define MAX_SIZE 1024

#define TAG "subtitle-jni"

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

#if defined(__cplusplus)
#define	__BEGIN_DECLS		extern "C" {
#define	__END_DECLS		}
#endif

struct field_t {
	jfieldID context;
	jmethodID onPreparedID;
	jmethodID onSeekCompleteID;
};

struct segment_field_t {
    jmethodID setFromTimeID;
    jmethodID setToTimeID;
    jmethodID setDataID;
};

static field_t gFields;

static segment_field_t gSegmentFields;

static
void JNU_ThrowByName(JNIEnv* env, const char* name, const char *msg)
{
	jclass clazz = env->FindClass(name);
	if (NULL != clazz)
	{
		env->ThrowNew(clazz, msg);
	}

	env->DeleteLocalRef(clazz);
}

static
const char* jstr2cstr(JNIEnv* env, const jstring jstr)
{
	char* cstr = NULL;

	if (NULL != env && NULL != jstr)
	{
		const char* tmp = env->GetStringUTFChars(jstr, NULL);
		const size_t len = strlen(tmp) + 1;

		cstr = (char*)malloc(len);
		bzero(cstr, len);

		snprintf(cstr, len, "%s", tmp);
		env->ReleaseStringUTFChars(jstr, tmp);
	}

	return cstr;
}

static
const jstring cstr2jstr(JNIEnv* env, const char* cstr)
{
	if (NULL != env && NULL != cstr)
	{
		jstring jstr = env->NewStringUTF(cstr);
		return jstr;
	}

	return NULL;
}

static
ISubtitles* getSubTitleParser(JNIEnv* env, jobject thiz)
{
	ISubtitles* p = (ISubtitles *)env->GetLongField(thiz, gFields.context);
	return p;
}

static
ISubtitles* setSubTitleParser(JNIEnv* env, jobject thiz, ISubtitles* parser)
{
	ISubtitles* old = (ISubtitles *)env->GetLongField(thiz, gFields.context);
	env->SetLongField(thiz, gFields.context, (int64_t)parser);
	return old;
}

static
void native_onPrepared(JNIEnv* env, jobject thiz, bool success, const char* cmsg)
{
	LOGI("Call native_onPrepared");

	if (NULL == gFields.onPreparedID) {
		LOGE("gFields.onPreparedID is null");
		return;
	}

	const jstring jmsg = cstr2jstr(env, cmsg);
	env->CallVoidMethod(thiz, gFields.onPreparedID, success ? JNI_TRUE : JNI_FALSE, jmsg);
}

static
void native_onSeekComplete(JNIEnv* env, jobject thiz)
{
	if (NULL != gFields.onSeekCompleteID)
	{
		env->CallVoidMethod(thiz, gFields.onSeekCompleteID);
	}
}

static
jboolean setSegment(JNIEnv* env, STSSegment* src, jobject target)
{
	int64_t startTime = src->getStartTime();
	int64_t stopTime = src->getStopTime();
	char buf[MAX_SIZE];
	src->getSubtitleText(buf, MAX_SIZE);
	jstring text = cstr2jstr(env, buf);

	env->CallVoidMethod(target, gSegmentFields.setFromTimeID, startTime);
	env->CallVoidMethod(target, gSegmentFields.setToTimeID, stopTime);
    env->CallVoidMethod(target, gSegmentFields.setDataID, text);
	return true;
}

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_gotye_meetsdk_subtitle_SimpleSubTitleParser
 * Method:    native_init
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_gotye_meetsdk_subtitle_SimpleSubTitleParser_nativeInit(JNIEnv* env, jobject thiz)
{
	jclass clazz = env->FindClass("com/gotye/meetsdk/subtitle/SimpleSubTitleParser");
	if (clazz == NULL) {
		JNU_ThrowByName(env, "java/lang/IllegalStateException", 
			"Class com/gotye/meetsdk/subtitle/SimpleSubTitleParser not found.");
		return;
	}

	gFields.context = env->GetFieldID(clazz, "mNativeContext", "J");
	if (gFields.context == NULL) {
		JNU_ThrowByName(env, "java/lang/IllegalStateException", 
			"Field mNativeContext not found.");
		return;
	}

	gFields.onPreparedID = env->GetMethodID(clazz, "onPrepared", "(ZLjava/lang/String;)V");
	if (gFields.onPreparedID == NULL) {
		JNU_ThrowByName(env, "java/lang/IllegalStateException", "Method not found.");
		return;
	}

	gFields.onSeekCompleteID = env->GetMethodID(clazz, "onSeekComplete", "()V");
	if (gFields.onSeekCompleteID == NULL) {
		JNU_ThrowByName(env, "java/lang/IllegalStateException", "Method not found.");
		return;
	}

	clazz = env->FindClass("com/gotye/meetsdk/subtitle/SubTitleSegment");
	if (NULL == clazz) {
		JNU_ThrowByName(env, "java/lang/IllegalStateException", "Class not found.");
		return;
	}

	gSegmentFields.setFromTimeID = env->GetMethodID(clazz, "setFromTime", "(J)V");
	if (NULL == gSegmentFields.setFromTimeID) {
		JNU_ThrowByName(env, "java/lang/IllegalStateException", "Method not found.");
		return;
	}

	gSegmentFields.setToTimeID = env->GetMethodID(clazz, "setToTime", "(J)V");
	if (NULL == gSegmentFields.setToTimeID) {
		JNU_ThrowByName(env, "java/lang/IllegalStateException", "Method not found.");
		return;
	}

	gSegmentFields.setDataID = env->GetMethodID(clazz, "setData", "(Ljava/lang/String;)V");
	if (NULL == gSegmentFields.setDataID) {
		JNU_ThrowByName(env, "java/lang/IllegalStateException", "Method not found.");
		return;
	}
}

/*
 * Class:     com_gotye_meetsdk_subtitle_SimpleSubTitleParser
 * Method:    nativeSetup
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_gotye_meetsdk_subtitle_SimpleSubTitleParser_nativeSetup(JNIEnv* env, jobject thiz)
{
	ISubtitles* parser = NULL;
	if (!ISubtitles::create(&parser)) {
		JNU_ThrowByName(env, "java/lang/IllegalStateException", 
			"Native subtitle parser init failed.");
		return;
	}

	setSubTitleParser(env, thiz, parser);
}

/*
 * Class:     com_gotye_meetsdk_subtitle_SimpleSubTitleParser
 * Method:    nativeClose
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_gotye_meetsdk_subtitle_SimpleSubTitleParser_nativeClose(JNIEnv *env, jobject thiz)
{
	LOGI("native_close()");

	ISubtitles* parser = getSubTitleParser(env, thiz);
	if (NULL == parser) {
		JNU_ThrowByName(env, "java/lang/IllegalStateException", "Subtitle parser not found.");
		return;
	}

	delete parser;
	parser = NULL;
	setSubTitleParser(env, thiz, NULL);
}

/*
 * Class:     com_gotye_meetsdk_subtitle_SimpleSubTitleParser
 * Method:    nativeLoadSubtitle
 * Signature: (Ljava/lang/String;Z)V
 */
JNIEXPORT void JNICALL
Java_com_gotye_meetsdk_subtitle_SimpleSubTitleParser_nativeLoadSubtitle
(JNIEnv *env, jobject thiz, jstring jFilePath, jboolean isMediaFile)
{
	LOGI("native_loadSubtitle()");

	ISubtitles* parser = getSubTitleParser(env, thiz);
	if (NULL == parser) {
		JNU_ThrowByName(env, "java/lang/IllegalStateException", "Subtitle parser not found");
		return;
	}

	LOGI("Call native_loadSubtitle");
	const char* filePath = jstr2cstr(env, jFilePath);
	if (NULL == filePath) {
		LOGE("Subtitle file path is null.");
		native_onPrepared(env, thiz, false, "Subtitle file path is null.");
		return;
	}

	bool ret = parser->loadSubtitle(filePath, isMediaFile);
	free((char*)filePath);

	native_onPrepared(env, thiz, ret, ret ? "Load subtitle file succeed." : "Load subtitle file failed.");
}

/*
 * Class:     com_gotye_meetsdk_subtitle_SimpleSubTitleParser
 * Method:    nativeSeekTo
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_gotye_meetsdk_subtitle_SimpleSubTitleParser_nativeSeekTo(JNIEnv *env, jobject thiz, jlong msec)
{
	LOGI("native_seekTo");

	ISubtitles* parser = getSubTitleParser(env, thiz);
	if (NULL == parser)
	{
		JNU_ThrowByName(env, "java/lang/IllegalStateException", "Subtitle parser not found");
		return;
	}

	parser->seekTo(msec);

	native_onSeekComplete(env, thiz);
}

/*
 * Class:     com_gotye_meetsdk_subtitle_SimpleSubTitleParser
 * Method:    nativeNext
 * Signature: (Lcom/gotye/meetsdk/subtitle/SubTitleSegment;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_gotye_meetsdk_subtitle_SimpleSubTitleParser_nativeNext(JNIEnv *env, jobject thiz, jobject segment)
{
	LOGD("native_next");

	ISubtitles* parser = getSubTitleParser(env, thiz);
	if (NULL == parser) {
		JNU_ThrowByName(env, "java/lang/IllegalStateException", "Subtitle parser not found");
		return false;
	}

	STSSegment* src = NULL;
	if (parser->getNextSubtitleSegment(&src)) {
		return setSegment(env, src, segment);
	}
	return false;
}

#ifdef __cplusplus
}
#endif


