/*
 * Copyright (C) 2015 Michael ma guoliangma@pptv.com
 *
 */
#include "FFMediaPlayer.h"
#include <stdio.h>
#include <stdlib.h> // for atoi
#include <fcntl.h> // for access R_OK
#include <jni.h>
#include <dlfcn.h> // for dlopen ...
#include <cpu-features.h>
#include <sys/system_properties.h> // for __system_property_get

#define LOG_TAG "JNI-MediaPlayer"
#include "pplog.h"
#include "version.h" // for auto version
#include "platform/autolock.h"
#include "platform/platforminfo.h"
#include "subtitle.h"
#include "cpuext.h" // for get_cpu_freq()
#include "FFMediaExtractor.h" // for extractor
#include "jniUtils.h"

#define USE_NDK_SURFACE_REF

#define COMPATIBILITY_HARDWARE_DECODE	1
#define COMPATIBILITY_SOFTWARE_DECODE	2

#define LEVEL_SYSTEM					1 // LEVEL_HAREWARE
#define LEVEL_SOFTWARE_SD				2 // LEVEL_SOFTWARE_LIUCHANG
#define LEVEL_SOFTWARE_HD1				3 // LEVEL_SOFTWARE_GAOQING
#define LEVEL_SOFTWARE_HD2				4 // LEVEL_SOFTWARE_CHAOQING
#define LEVEL_SOFTWARE_BD				5 // LEVEL_SOFTWARE_LANGUANG

#define LOG_FATAL_IF(cond, ...) if (cond) { PPLOGE(__VA_ARGS__); return -1;}

class Surface;

struct fields_t {
	jfieldID    context; // for save player handle
	// 2015.7.2 guoliang.ma add to support mutil-session player
	jfieldID	listener; // for save listener handle
	jfieldID    surface; // for save surface object
	/* actually in android.view.Surface XXX */
	jfieldID    surface_native;

	jmethodID   post_event;
	jfieldID    iSubtitle;
};

// member from jniUtils.cpp
extern JavaVM *gs_jvm;
extern pthread_mutex_t sLock;

static fields_t fields;
static bool sInited = false;
static PlatformInfo* gPlatformInfo = NULL;
static void* player_handle_software = NULL;

// new
typedef IPlayer* (*GET_PLAYER_FUN) (void*);
typedef void (*RELEASE_PLAYER_FUN) (IPlayer *);

GET_PLAYER_FUN getPlayerFun = NULL; // function to NEW player instance
RELEASE_PLAYER_FUN releasePlayerFun = NULL; // function to DELETE player instance
#ifdef USE_TS_CONVERT
CONVERT_FUN convertFun = NULL;
#endif

static int jniThrowException(JNIEnv* env, const char* className, const char* msg);

static void unloadPlayerLib(void** handler);

static bool loadPlayerLib();

static
const char* getCodecLibName(uint64_t cpuFeatures)
{
#if defined(__aarch64__)
	return "libplayer_neon.so";
#else
	const char* codecLibName = NULL;
	
	PPLOGI("android_getCpuFamily %d", android_getCpuFamily());
	
	char value[PROP_VALUE_MAX];
	__system_property_get("ro.product.cpu.abi", value);
	char* occ = strcasestr(value,"x86");
	if (occ) {
		// x86 arch
		PPLOGI("the device is x86 platform");
		codecLibName = "libplayer_neon.so";
		return codecLibName;
	}

	if ((cpuFeatures & ANDROID_CPU_ARM_FEATURE_NEON) != 0)
	{
		//neon
		PPLOGI("the device supports neon");
		codecLibName = "libplayer_neon.so";
	}
	else if((cpuFeatures & ANDROID_CPU_ARM_FEATURE_ARMv7) != 0)
	{
		//v7_vfpv3d16
		PPLOGI("the device supports v7_vfpv3d16");
		codecLibName = "libplayer_tegra2.so";
	}
	/*else if ((cpuFeatures & ANDROID_CPU_ARM_FEATURE_VFP) != 0)
	{
		//armv6_vfp
		PPLOGI("the device supports armv6_vfp");
		codecLibName = "libplayer_v6_vfp.so";
	}*/
	else if((cpuFeatures & ANDROID_CPU_ARM_FEATURE_LDREX_STREX) != 0)
	{
		//armv6
		PPLOGI("the device supports armv6");
		codecLibName = "libplayer_v6.so";
	}
	else
	{
		//armv5te or lower
		PPLOGI("the device supports armv5te");
		codecLibName = "libplayer_v5te.so";
	}

	return codecLibName;
#endif
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

// Returns the Unix file descriptor for a ParcelFileDescriptor object
int getParcelFileDescriptorFDPP(JNIEnv* env, jobject object)
{
	jclass clazz = env->FindClass("java/io/FileDescriptor");
	LOG_FATAL_IF(clazz == NULL, "Unable to find class java.io.FileDescriptor");
	jfieldID descriptor = env->GetFieldID(clazz, "descriptor", "I");
	LOG_FATAL_IF(descriptor == NULL, "Unable to find descriptor field in java.io.FileDescriptor");

	return env->GetIntField(object, descriptor);
}

JNIMediaPlayerListener::JNIMediaPlayerListener(JNIEnv* env, jobject thiz, jobject weak_thiz)
{
	PPLOGD("JNIMediaPlayerListener constructor");

	// Hold onto the MediaPlayer class for use in calling the static method
	// that posts events to the application thread.
	jclass clazz = env->GetObjectClass(thiz);
	if (clazz == NULL) {
		PPLOGE("Can't find android/pplive/media/player/FFMediaPlayer");
		jniThrowException(env, "java/lang/Exception", NULL);
		return;
	}
	mClass = (jclass)env->NewGlobalRef(clazz);

	// We use a weak reference so the MediaPlayer object can be garbage collected.
	// The reference is only used as a proxy for callbacks.
	mObject  = env->NewGlobalRef(weak_thiz);
}

JNIMediaPlayerListener::~JNIMediaPlayerListener()
{
	PPLOGD("JNIMediaPlayerListener destructor");
	// remove global references
	//JNIEnv *env = AndroidRuntime::getJNIEnv();
	JNIEnv *env = getJNIEnvPP();
	if (env) {
		env->DeleteGlobalRef(mObject);
		env->DeleteGlobalRef(mClass);
	}
	else {
		PPLOGE("~JNIMediaPlayerListener() env is null");
	}
}

void JNIMediaPlayerListener::notify(int msg, int ext1, int ext2)
{
	JNIEnv *env = getJNIEnvPP();
	if (env) {
		env->CallStaticVoidMethod(mClass, fields.post_event, mObject, msg, ext1, ext2, 0);
	}
	else {
		PPLOGE("notify() env is null");
	}
}

// ----------------------------------------------------------------------------

static Surface* get_surface(JNIEnv* env, jobject clazz)
{
	return (Surface*)env->GetLongField(clazz, fields.surface_native);
}

static IPlayer* getMediaPlayer(JNIEnv* env, jobject thiz)
{
	AutoLock l(&sLock);
	IPlayer* p = (IPlayer*)env->GetLongField(thiz, fields.context);
	return p;
}

static IPlayer* setMediaPlayer(JNIEnv* env, jobject thiz, IPlayer* player)
{
	AutoLock l(&sLock);
	IPlayer* old = (IPlayer*)env->GetLongField(thiz, fields.context);
	env->SetLongField(thiz, fields.context, (int64_t)player);
	return old;
}

// If exception is NULL and opStatus is not OK, this method sends an error
// event to the client application; otherwise, if exception is not NULL and
// opStatus is not OK, this method throws the given exception to the client
// application.
static void process_media_player_call(JNIEnv *env, jobject thiz, status_t opStatus, const char* exception, const char *message)
{
	if (exception == NULL) {  // Don't throw exception. Instead, send an event.
		if (opStatus != (status_t) OK) {
			IPlayer* mp = getMediaPlayer(env, thiz);
			if (mp) {
				PPLOGE("process_media_player_call: call player's notify status=%d", opStatus);
				mp->notify(MEDIA_ERROR, opStatus, 0);
			}
		}
	} else {  // Throw exception!
		if ( opStatus == (status_t) INVALID_OPERATION ) {
			jniThrowException(env, "java/lang/IllegalStateException", NULL);
		} else if ( opStatus != (status_t) OK ) {
			if (strlen(message) > 230) {
				// if the message is too long, don't bother displaying the status code
				jniThrowException( env, exception, message);
			} else {
				char msg[256];
				// append the status code to the message
				sprintf(msg, "%s: status=0x%X", message, opStatus);
				jniThrowException( env, exception, msg);
			}
		}
	}
}

static void
android_media_MediaPlayer_setDataSourceAndHeaders(
		JNIEnv *env, jobject thiz, jstring path, jobject headers)
{
	PPLOGI("setDataSourceAndHeaders()");
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		PPLOGE("create MediaPlayer failed");
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}

	if (path == NULL) {
		jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
		return;
	}

	const char *pathStr = env->GetStringUTFChars(path, NULL);
	if (pathStr == NULL) {  // Out of memory
		jniThrowException(env, "java/lang/RuntimeException", "GetStringUTFChars: Out of memory");
		return;
	}

	status_t opStatus = mp->setDataSource(pathStr);

	// Make sure that local ref is released before a potential exception
	env->ReleaseStringUTFChars(path, pathStr);

	process_media_player_call(
			env, thiz, opStatus, "java/io/IOException",
			"setDataSource failed." );
}

static
void android_media_MediaPlayer_setDataSource(JNIEnv *env, jobject thiz, jstring path)
{
	PPLOGI("setDataSource()");
	android_media_MediaPlayer_setDataSourceAndHeaders(env, thiz, path, 0);
}

static
void android_media_MediaPlayer_setDataSourceFD(JNIEnv *env, jobject thiz, jobject fileDescriptor, jlong offset, jlong length)
{
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}

	if (fileDescriptor == NULL) {
		jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
		return;
	}
	int fd = getParcelFileDescriptorFDPP(env, fileDescriptor);
	PPLOGI("setDataSourceFD: fd %d", fd);
	process_media_player_call( env, thiz, mp->setDataSource(fd, offset, length), "java/io/IOException", "setDataSourceFD failed." );
}

static void setVideoSurface(IPlayer* mp, JNIEnv *env, jobject thiz, jobject surface)
{
	PPLOGI("setVideoSurface()");
#ifdef USE_NDK_SURFACE_REF
	PPLOGI("setVideoSurface: use java side surface");
	if (NULL != surface) {
		gPlatformInfo->javaSurface = env->NewGlobalRef(surface);
		mp->setVideoSurface((void*)gPlatformInfo->javaSurface);
	}
#else
	PPLOGI("setVideoSurface: use member variable surface");
	jobject surf = env->GetObjectField(thiz, fields.surface);

	if (surf != NULL) {
		// sdk_version >= 18
		gPlatformInfo->javaSurface = env->NewGlobalRef(surf);

		void* native_surface = (void*)get_surface(env, surf);
		mp->setVideoSurface((void*)native_surface);
	}
	else {
		PPLOGE("setVideoSurface is NULL");
	}
#endif
}

static
void android_media_MediaPlayer_setVideoSurface(JNIEnv *env, jobject thiz, jobject surface)
{
	PPLOGI("android_media_MediaPlayer_setVideoSurface()");
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}
	setVideoSurface(mp, env, thiz, surface);
}

static
void android_media_MediaPlayer_prepare(JNIEnv *env, jobject thiz)
{
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}
	// 2014.12.9 guoliang.ma comment out to fix android 2.3.x failed to render video 
	//setVideoSurface(mp, env, thiz, NULL);
	process_media_player_call( env, thiz, mp->prepare(), "java/io/IOException", "Prepare failed." );
}

static
void android_media_MediaPlayer_prepareAsync(JNIEnv *env, jobject thiz)
{
	PPLOGI("android_media_MediaPlayer_prepareAsync()");
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}
	// 2014.12.9 guoliang.ma comment out to fix android 2.3.x failed to render video 
	//setVideoSurface(mp, env, thiz, NULL);
	process_media_player_call( env, thiz, mp->prepareAsync(), "java/io/IOException", "Prepare Async failed." );
}

static
void android_media_MediaPlayer_start(JNIEnv *env, jobject thiz)
{
	PPLOGI("android_media_MediaPlayer_start()");
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}
	process_media_player_call( env, thiz, mp->start(), NULL, NULL );
}

static
void android_media_MediaPlayer_stop(JNIEnv *env, jobject thiz)
{
	PPLOGI("android_media_MediaPlayer_stop");
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}

	process_media_player_call( env, thiz, mp->stop(), NULL, NULL );
	PPLOGI("stoped");
}

static
void android_media_MediaPlayer_pause(JNIEnv *env, jobject thiz)
{
	PPLOGI("android_media_MediaPlayer_pause");
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}
	process_media_player_call( env, thiz, mp->pause(), NULL, NULL );
	PPLOGI("paused");
}

static
jboolean android_media_MediaPlayer_isPlaying(JNIEnv *env, jobject thiz)
{
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return false;
	}
	const jboolean is_playing = mp->isPlaying();

	PPLOGD("isPlaying: %d", is_playing);
	return is_playing;
}

static
void android_media_MediaPlayer_seekTo(JNIEnv *env, jobject thiz, int msec)
{
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}

	int playTime=0;
	mp->getCurrentPosition(&playTime);
	int mediaDiff = msec - playTime; // msec
	mediaDiff = mediaDiff>0 ? mediaDiff : -mediaDiff;

	if(mediaDiff > 2000) // 2sec
	{
		PPLOGD("jni seekTo: %d(msec)", msec);
		process_media_player_call( env, thiz, mp->seekTo(msec), NULL, NULL);
		PPLOGD("jni seekTo: %d(msec) end", msec);
	}
	else
	{
		mp->notify(MEDIA_SEEK_COMPLETE, 0, 0);
	}
}

static
int android_media_MediaPlayer_getVideoWidth(JNIEnv *env, jobject thiz)
{
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		PPLOGE("player is null, getVideoWidth failed");
		return 0;
	}
	int w;
	if (0 != mp->getVideoWidth(&w)) {
		PPLOGE("getVideoWidth failed");
		w = 0;
	}
	PPLOGI("getVideoWidth: %d", w);
	return w;
}

static
int android_media_MediaPlayer_getVideoHeight(JNIEnv *env, jobject thiz)
{
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		PPLOGE("player is null, getVideoHeight failed");
		return 0;
	}
	int h;
	if (0 != mp->getVideoHeight(&h)) {
		PPLOGE("getVideoHeight failed");
		h = 0;
	}
	PPLOGV("getVideoHeight: %d", h);
	return h;
}


static
int android_media_MediaPlayer_getCurrentPosition(JNIEnv *env, jobject thiz)
{
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		PPLOGE("player is null, getCurrentPosition failed");
		return 0;
	}
	PPLOGD("getCurrentPosition start");
	int msec;
	process_media_player_call( env, thiz, mp->getCurrentPosition(&msec), NULL, NULL );
	PPLOGD("getCurrentPosition: %d (msec)", msec);
	return msec;
}

static
int android_media_MediaPlayer_getDuration(JNIEnv *env, jobject thiz)
{
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		PPLOGE("player is null, getDuration failed");
		return 0;
	}
	PPLOGD("get media duration start");
	int msec;
	process_media_player_call( env, thiz, mp->getDuration(&msec), NULL, NULL );
	PPLOGD("get media duration is : %d (msec)", msec);
	return msec;
}

static
int android_media_MediaPlayer_getBufferingTime(JNIEnv *env, jobject thiz)
{
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL)
	{
		PPLOGE("player is null, getBufferingTime failed");
		return 0;
	}

	int msec;
	process_media_player_call(env, thiz, mp->getBufferingTime(&msec), NULL, NULL);

	PPLOGD("get buffering time is: %d (msec)", msec);

	return msec;

}

static
void android_media_MediaPlayer_reset(JNIEnv *env, jobject thiz)
{
	PPLOGD("++++++++Start resetting");
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}
	process_media_player_call( env, thiz, mp->reset(), NULL, NULL );
	PPLOGD("++++++++End resetting");
}

static
void android_media_MediaPlayer_setAudioStreamType(JNIEnv *env, jobject thiz, int streamtype)
{
	PPLOGI("setAudioStreamType: %d", streamtype);
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		PPLOGE("player is null, setAudioStreamType failed");
		return;
	}
	process_media_player_call( env, thiz, mp->setAudioStreamType(streamtype) , NULL, NULL );
}

static
void android_media_MediaPlayer_selectAudioChannel(JNIEnv *env, jobject thiz, int index)
{
	PPLOGI("selectAudioChannel: %d", index);
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		PPLOGE("player is null, selectAudioChannel failed");
		return;
	}

	if (OK != mp->selectAudioChannel(index))
		PPLOGW("failed to set audio channel %d", index);
}

static
void android_media_MediaPlayer_selectSubtitleChannel(JNIEnv *env, jobject thiz, int index)
{
	PPLOGI("selectSubtitleChannel: %d", index);
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		PPLOGE("player is null, selectSubtitleChannel failed");
		return;
	}

	if (OK != mp->selectSubtitleChannel(index))
		PPLOGW("failed to set audio channel %d", index);
}

static
void android_media_MediaPlayer_setSubtitleParser(JNIEnv *env, jobject thiz, jobject paser)
{
	PPLOGI("setSubtitleParser");
	jclass clazzSubtitle = env->FindClass("android/pplive/media/subtitle/SimpleSubTitleParser");
	if (clazzSubtitle == NULL) {
		PPLOGE("player is null, setSubtitleParser failed");
		return;
	}
	//fields.iSubtitle
	jfieldID is = env->GetFieldID(clazzSubtitle, "mNativeContext", "J");
	PPLOGD("GetFieldID: mNativeContext");
	ISubtitles* p = NULL;
	if(paser)
		p = (ISubtitles*)env->GetLongField(paser, is);

	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}

	process_media_player_call(env, thiz, mp->setISubtitle(p), NULL, NULL);
}

static
void android_media_MediaPlayer_setLooping(JNIEnv *env, jobject thiz, jboolean looping)
{
	PPLOGI("setLooping: %d", looping);

	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}
	process_media_player_call( env, thiz, mp->setLooping(looping), NULL, NULL );
}

static
jboolean android_media_MediaPlayer_isLooping(JNIEnv *env, jobject thiz)
{
	PPLOGD("isLooping");
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return false;
	}
	return mp->isLooping();
}

static
void android_media_MediaPlayer_setVolume(JNIEnv *env, jobject thiz, float leftVolume, float rightVolume)
{
	PPLOGI("setVolume: left %f  right %f", leftVolume, rightVolume);
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}
	process_media_player_call( env, thiz, mp->setVolume(leftVolume, rightVolume), NULL, NULL );
}

// FIXME: deprecated
static
jobject android_media_MediaPlayer_getFrameAt(JNIEnv *env, jobject thiz, jint msec)
{
	return NULL;
}


// Sends the request and reply parcels to the media player via the
// binder interface.
static
jint android_media_MediaPlayer_invoke(JNIEnv *env, jobject thiz,
		jobject java_request, jobject java_reply)
{
	return 0;
}

// Sends the new filter to the client.
static
jint android_media_MediaPlayer_setMetadataFilter(JNIEnv *env, jobject thiz, jobject request)
{
	return 0;
}

static
jboolean android_media_MediaPlayer_getMetadata(JNIEnv *env, jobject thiz, jboolean update_only,
		jboolean apply_filter, jobject reply)
{
	return 0;
}

static
jint android_media_MediaPlayer_flags(JNIEnv *env, jobject thiz)
{
	PPLOGD("get flag");
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return 0;
	}
	int flag = mp->flags();
	PPLOGD("flag: %d", flag);
	return flag;
}

static
char* getCStringFromJavaStaticStringField(JNIEnv* env, const char* clazz_name, const char* field_name)
{
	jclass clazz = env->FindClass(clazz_name);
	if (clazz == NULL)
	{
		PPLOGE("Can't find class %s", clazz_name);
		return NULL;
	}

	jfieldID fieldID = env->GetStaticFieldID(clazz, field_name, "Ljava/lang/String;");
	if (fieldID == NULL)
	{
		PPLOGE("Can't find fieldID %s.%s", clazz_name, field_name);
		return NULL;
	}

	jstring jstr = (jstring)env->GetStaticObjectField(clazz, fieldID);
	if (jstr == NULL)
	{
		PPLOGE("Get static string field %s.%s failed.", clazz_name, field_name);
		return NULL;
	}

	char* cstr = jstr2cstr(env, jstr);

	return cstr;
}

// called when MeetSDK.initSDK() call initPlayer()
// This function gets some field IDs, which in turn causes class initialization.
// It is called from a static block in MediaPlayer, which won't run until the
// first time an instance of this class is used.
static
jboolean android_media_MediaPlayer_native_init(JNIEnv *env, jobject thiz)
{
	if (sInited)
		return true;

	PPLOGI("native_init");

	if (gPlatformInfo)
		delete gPlatformInfo;

	gPlatformInfo = new PlatformInfo();

    gPlatformInfo->jvm = (void*)gs_jvm;
	gPlatformInfo->pplog_func = (void*)__pp_log_vprint;

	jclass clazzSDK = env->FindClass("android/pplive/media/MeetSDK");
	if (clazzSDK == NULL)
		jniThrowException(env, "java/lang/RuntimeException", "Can't find android/pplive/media/MeetSDK");

	jmethodID midGetAppRootDir = env->GetStaticMethodID(clazzSDK, 
			"getAppRootDir", "()Ljava/lang/String;");
	jstring str_root_dir = (jstring)env->CallStaticObjectMethod(clazzSDK, midGetAppRootDir);
	char *app_root_dir = jstr2cstr(env, str_root_dir);

	if (app_root_dir != NULL)
		snprintf(gPlatformInfo->app_path, STRLEN, "%s", app_root_dir);

	char* lib_path = getCStringFromJavaStaticStringField(env, "android/pplive/media/player/FFMediaPlayer", "libPath");
	if (lib_path != NULL) {
		snprintf(gPlatformInfo->lib_path, STRLEN, "%s", lib_path);
		free(lib_path);
	}

#ifndef __aarch64__
	if (strlen(gPlatformInfo->model_name) == 0) {
		__system_property_get("ro.product.model", gPlatformInfo->model_name);
		__system_property_get("ro.board.platform", gPlatformInfo->board_name);
		__system_property_get("ro.build.mainchipname", gPlatformInfo->chip_name);
		__system_property_get("ro.product.manufacturer", gPlatformInfo->manufacture_name);
		__system_property_get("ro.build.version.release", gPlatformInfo->release_version);

		char sdk_version[STRLEN];
		__system_property_get("ro.build.version.sdk", sdk_version);
		gPlatformInfo->sdk_version = atoi(sdk_version);

		PPLOGI("MODEL_NAME: %s", gPlatformInfo->model_name);
		PPLOGI("BOARD_NAME: %s", gPlatformInfo->board_name);
		PPLOGI("CHIP_NAME: %s", gPlatformInfo->chip_name);
		PPLOGI("MANUFACTURE_NAME: %s", gPlatformInfo->manufacture_name);
		PPLOGI("RELEASE_VERSION: %s", gPlatformInfo->release_version);
		PPLOGI("SDK_VERSION: %d", gPlatformInfo->sdk_version);
		PPLOGI("APP_PATH: %s", gPlatformInfo->app_path);
		PPLOGI("LIB_PATH: %s", gPlatformInfo->lib_path);
		PPLOGI("PPBOX_LIB_NAME: %s", gPlatformInfo->ppbox_lib_name);
	}
#endif

	jclass clazz = env->FindClass("android/pplive/media/player/FFMediaPlayer");
	if (clazz == NULL)
		jniThrowException(env, "java/lang/RuntimeException", "Can't find android/pplive/media/player/FFMediaPlayer");

	fields.context = env->GetFieldID(clazz, "mNativeContext", "J");
	if (fields.context == NULL)
		jniThrowException(env, "java/lang/RuntimeException", "Can't find FFMediaPlayer.mNativeContext");

	fields.listener = env->GetFieldID(clazz, "mListenerContext", "J");
	if (fields.listener == NULL)
		jniThrowException(env, "java/lang/RuntimeException", "Can't find FFMediaPlayer.mListenerContext");

	fields.post_event = env->GetStaticMethodID(clazz, "postEventFromNative",
			"(Ljava/lang/Object;IIILjava/lang/Object;)V");
	if (fields.post_event == NULL)
		jniThrowException(env, "java/lang/RuntimeException", "Can't find FFMediaPlayer.postEventFromNative");

#ifdef BUILD_ONE_LIB
	getPlayerFun		= getPlayer;
	releasePlayerFun	= releasePlayer;
#ifdef USE_TS_CONVERT
	convertFun			= my_convert;
#endif
#else
	if (!loadPlayerLib()) {
		jniThrowException(env, "java/lang/RuntimeException", "Load Library Failed!!!");
		return false;
	}
#endif

	sInited = true;
	return true;
}

static
void* loadLibrary(const char* libPath)
{
	PPLOGD("Before Load lib %s", libPath);
	void* lib_handle = dlopen(libPath, RTLD_NOW);

	if (lib_handle == NULL) {
		PPLOGE("Load lib %s error: %s", libPath, dlerror());
		return NULL;
	}

	PPLOGI("Load lib %s success", libPath);
	return lib_handle;
}

static void unloadPlayerLib(void** handler)
{
	PPLOGD("Start unloading player lib");
	if (*handler != NULL) {
		dlclose(*handler);
		*handler = NULL;
	}
}

static bool loadPlayerLib()
{
	PPLOGI("loadPlayerLib");

	void** player_handle = NULL; // handle to shared library
	player_handle = &player_handle_software;

	if (NULL == gPlatformInfo) {
		PPLOGE("PlatformInfo is null");
		return false;
	}

	PPLOGI("Before Player Library Load.");
	char* libPath = NULL;

	// load player lib
	if (*player_handle == NULL) {
		PPLOGI("using ffplay");
		uint64_t cpuFeatures = android_getCpuFeatures();
		const char* libName = getCodecLibName(cpuFeatures); //libplayer_neon.so

		do {
			//0. try to load from specified lib path
			if (strcmp(gPlatformInfo->lib_path, "") != 0) {
				libPath = vstrcat(gPlatformInfo->lib_path, libName);
				PPLOGI("Load from specified lib path #0: %s", libPath);
				*player_handle = loadLibrary(libPath);
				if (!player_handle)
					PPLOGW("step0 loadLibrary failed: %s", libPath);
				free(libPath);
				if (*player_handle)
					break;
			}

			//1. try to load from app lib path
			libPath = vstrcat(gPlatformInfo->app_path, "lib/", libName);
			PPLOGI("Load from app lib path #1: %s", libPath);
			*player_handle = loadLibrary(libPath);
			if (!player_handle)
				PPLOGW("step1 loadLibrary failed: %s", libPath);
			free(libPath);
			if (*player_handle)
				break;

			// 2015.6.9 guoliangma added to fix arm64 load error
			//2. try to load system lib path
			PPLOGI("Load from system lib path #2: %s", libName);
			*player_handle = loadLibrary(libName);
			if (!player_handle)
				PPLOGW("step2 loadLibrary failed: %s", libPath);
		}while(0);

		if (NULL == *player_handle) {
			PPLOGE("oops: all loadlibrary try failed");
			return false;
		}
	}

	PPLOGD("Before init getPlayer()");

	getPlayerFun = (GET_PLAYER_FUN)dlsym(*player_handle, "getPlayer");
	if (getPlayerFun == NULL) {
		PPLOGE("Init getPlayer() failed: %s", dlerror());
		return false;
	}

	releasePlayerFun = (RELEASE_PLAYER_FUN)dlsym(*player_handle, "releasePlayer");
	if (releasePlayerFun == NULL) {
		PPLOGE("Init releasePlayer() failed: %s", dlerror());
		return false;
	}

#ifdef USE_TS_CONVERT
	convertFun = (CONVERT_FUN)dlsym(*player_handle, "my_convert");
	if (convertFun == NULL) {
		PPLOGE("Init convert() failed: %s", dlerror());
		return false;
	}
#endif

	if (!setup_extractor(*player_handle))
		return false;

	PPLOGD("After init getPlayer()");

	return true;
}

// callled when new FFMediaPlayer
static
void android_media_MediaPlayer_native_setup(JNIEnv *env, jobject thiz, jobject weak_this)
{
	PPLOGI("native_setup");

	IPlayer* mp = getPlayerFun((void*)gPlatformInfo);
	if (mp == NULL) {
		jniThrowException(env, "java/lang/RuntimeException", "Create IPlayer failed.");
		return;
	}

	// create new listener and give it to MediaPlayer
	JNIMediaPlayerListener * player_listener = new JNIMediaPlayerListener(env, thiz, weak_this);
	//IPlayer takes responsibility to release listener.
	mp->setListener(player_listener);

	// 2015.7.2 guoliang.ma add to support multi-session player
	env->SetLongField(thiz, fields.listener, (int64_t)player_listener);

	// Stow our new C++ MediaPlayer in an opaque field in the Java object.
	setMediaPlayer(env, thiz, mp);
	//check if it needs to release old mediaplayer.
}

static
void android_media_MediaPlayer_release(JNIEnv *env, jobject thiz)
{
	PPLOGI("release()");

	IPlayer* mp = setMediaPlayer(env, thiz, NULL);
	releasePlayerFun(mp);

    if (gPlatformInfo != NULL) {
		if (gPlatformInfo->javaSurface != NULL) {
			env->DeleteGlobalRef(gPlatformInfo->javaSurface);
			gPlatformInfo->javaSurface = NULL;
		}
    }

	JNIMediaPlayerListener* player_listener = (JNIMediaPlayerListener *)env->GetLongField(thiz, fields.listener);
	if (player_listener) {
		delete player_listener;
		player_listener = NULL;
	}
	env->SetLongField(thiz, fields.listener, 0);

	PPLOGI("release done!");
}

static jint
android_media_MediaPlayer_snoop(JNIEnv* env, jobject thiz, jobject data, jint kind) {
	jshort* ar = (jshort*)env->GetPrimitiveArrayCritical((jarray)data, 0);
	jsize len = env->GetArrayLength((jarray)data);
	int ret = 0;
	if (ar) {
		// roger
		// ret = MediaPlayer::snoop(ar, len, kind);
		// env->ReleasePrimitiveArrayCritical((jarray)data, ar, 0);
	}
	return ret;
}

static jint
android_media_MediaPlayer_native_suspend_resume(
		JNIEnv *env, jobject thiz, jboolean isSuspend) {

	PPLOGD("suspend_resume(%d)", isSuspend);
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return UNKNOWN_ERROR;
	}

	return isSuspend ? mp->suspend() : mp->resume();
}

static bool native_checkCompatibility_hardware(Surface* native_surface)
{
	return true;
}

static bool native_checkCompatibility_software()
{
	bool ret = false;
	uint64_t cpuFeatures = android_getCpuFeatures();
	if ((cpuFeatures & ANDROID_CPU_ARM_FEATURE_NEON) != 0 ||
			(cpuFeatures & ANDROID_CPU_ARM_FEATURE_ARMv7) != 0) // (cpuFeatures & ANDROID_CPU_ARM_FEATURE_VFP) != 0
	{
		ret = true;
	}
	return ret;
}



static
jboolean android_media_MediaPlayer_native_checkCompatibility(JNIEnv *env, jobject thiz, jint checkWhat, jobject jsurface)
{
	if(checkWhat == COMPATIBILITY_HARDWARE_DECODE) {
		Surface* native_surface = get_surface(env, jsurface);
		return native_checkCompatibility_hardware(native_surface);
	}
	else if(checkWhat == COMPATIBILITY_SOFTWARE_DECODE) {
		return native_checkCompatibility_software();
	}
	
	return false;
}

static int android_getCpuFreq()
{
	return get_cpu_freq();
}

static
jint android_media_MediaPlayer_native_checkSoftwareDecodeLevel()
{
	uint64_t cpuFeatures = android_getCpuFeatures();

    //non-NEON arch
    int level = LEVEL_SYSTEM;
	if ((cpuFeatures & ANDROID_CPU_ARM_FEATURE_NEON) != 0)
	{
	    //neon arch
        int cpuCount = android_getCpuCount();
        int cpuFreq = android_getCpuFreq();

		PPLOGI("cpuinfo count %d, freq %d(%.2f GHz)", cpuCount, cpuFreq, (double)cpuFreq / 1000000);

        if(cpuCount >= 4)// 4 cores
        {
            level = LEVEL_SOFTWARE_BD;
        }
        else if(cpuCount >= 2 && cpuFreq >= 1400000) // cpu:1.4G, 2 cores
        {
            level = LEVEL_SOFTWARE_HD2;
        }
        else if(cpuCount >= 1 && cpuFreq >= 1000000)
        {
            level = LEVEL_SOFTWARE_HD1;
        }
        else
        {
            level = LEVEL_SOFTWARE_SD;
        }
    }
	return level;
}

static
int accessCodec(const char* app_root, const char* lib_name)
{
	char* libPath = NULL;
	int canRead;

	libPath = vstrcat(app_root, "player/lib/", lib_name);
	canRead = access(libPath, R_OK);
	free(libPath);

	if (canRead != 0)
	{
		libPath = vstrcat(app_root, "lib/" , lib_name);
		canRead = access(libPath, R_OK);
		free(libPath);
	}

	return canRead;

}

static
jstring android_media_MediaPlayer_getBestCodec(JNIEnv *env, jobject thiz, jstring applicationPath)
{
	//char* appPath = (char*)env->GetStringUTFChars(applicationPath, NULL);
	const char* appPath = NULL;
	appPath = (char*)env->GetStringUTFChars(applicationPath, NULL);
	if (appPath == NULL)
	{
		appPath = "/"; //for not crash programe
	}

	uint64_t cpuFeatures = android_getCpuFeatures();
	const char* codecName = getCodecLibName(cpuFeatures);
	if (accessCodec(appPath, codecName) == 0)
	{
		codecName = "";
	}

	return env->NewStringUTF(codecName);
}

static jboolean
android_media_MediaPlayer_native_getMediaInfo(JNIEnv *env, jobject thiz, jstring js_media_file_path, jobject info)
{
	bool ret = false;

	IPlayer* mp = getPlayerFun((void*)gPlatformInfo);
	if (mp == NULL)
	{
		PPLOGE("Player init failed.");
		return false;
	}

	const char* url = jstr2cstr(env, js_media_file_path);
	MediaInfo native_info;
	ret = mp->getMediaInfo(url, &native_info);
	if (!ret) {
		PPLOGE("Get MediaInfo failed: %s", url);
	}
	else {
		PPLOGD("Get MediaInfo succeed.");
		jclass clazz = env->FindClass("android/pplive/media/player/MediaInfo");
		jfieldID f_path = env->GetFieldID(clazz, "mPath", "Ljava/lang/String;");
		jfieldID f_duration = env->GetFieldID(clazz, "mDurationMS", "J");
		jfieldID f_size = env->GetFieldID(clazz, "mSizeByte", "J");
		//jfieldID f_audio_channels = env->GetFieldID(clazz, "mAudioChannels", "I");
		//jfieldID f_video_channels = env->GetFieldID(clazz, "mVideoChannels", "I");

		env->SetObjectField(info, f_path, js_media_file_path);
		env->SetLongField(info, f_duration, native_info.duration_ms);
		env->SetLongField(info, f_size, native_info.size_byte);
		//env->SetIntField(info, f_audio_channels, native_info.audio_channels);
		//env->SetIntField(info, f_video_channels, native_info.video_channels);
	}

	releasePlayerFun(mp);

	return ret;
}

static void fill_media_info(JNIEnv *env, jobject thiz, jobject info, jstring file_path, MediaInfo *native_info)
{
	jclass clazz = env->FindClass("android/pplive/media/player/MediaInfo");
	jfieldID f_path = env->GetFieldID(clazz, "mPath", "Ljava/lang/String;");
	jfieldID f_duration = env->GetFieldID(clazz, "mDurationMS", "J");
	jfieldID f_size = env->GetFieldID(clazz, "mSizeByte", "J");
	jfieldID f_format = env->GetFieldID(clazz, "mFormatName", "Ljava/lang/String;");

	jfieldID f_videocodec_name = env->GetFieldID(clazz, "mVideoCodecName", "Ljava/lang/String;");
	jfieldID f_width = env->GetFieldID(clazz, "mWidth", "I");
	jfieldID f_height = env->GetFieldID(clazz, "mHeight", "I");
		
	jfieldID f_video_channels = env->GetFieldID(clazz, "mVideoChannels", "I");
	jfieldID f_audio_channels = env->GetFieldID(clazz, "mAudioChannels", "I");
	jfieldID f_subtitle_channels = env->GetFieldID(clazz, "mSubTitleChannels", "I");

	// 2015.1.9 guoliangma added
	jmethodID midSetAudioChannelInfo = env->GetMethodID(clazz, 
		"setAudioChannelsInfo", "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	jmethodID midSetSubtitleChannelInfo = env->GetMethodID(clazz, 
		"setSubtitleChannelsInfo", "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
			
	//jmethodID midSetChnnels = env->GetMethodID(clazz,"setChannels","(Ljava/lang/String;I)V");
	jmethodID construction_id = env->GetMethodID(clazz, "<init>", "()V");
	jobject obj = env->NewObject(clazz, construction_id);
		
	// 2015.1.9 guoliangma added
	env->SetIntField(info, f_audio_channels, native_info->audio_channels);
	for(int i=0;i<native_info->audio_channels;i++) {
		env->CallVoidMethod(info, midSetAudioChannelInfo, i, 
			native_info->audio_streamIndexs[i], 
			env->NewStringUTF(native_info->audiocodec_names[i]), 
			env->NewStringUTF(native_info->audio_languages[i]), 
			env->NewStringUTF(native_info->audio_titles[i]));
	}

	env->SetIntField(info, f_subtitle_channels, native_info->subtitle_channels);
	for(int i=0;i<native_info->subtitle_channels;i++) {
		env->CallVoidMethod(info, midSetSubtitleChannelInfo, i, 
			native_info->subtitle_streamIndexs[i], 
			env->NewStringUTF(native_info->subtitlecodec_names[i]), 
			env->NewStringUTF(native_info->subtitle_languages[i]), 
			env->NewStringUTF(native_info->subtitle_titles[i]));
	}

	if (file_path != NULL)
		env->SetObjectField(info, f_path, file_path);
	else
		env->SetObjectField(info, f_path, env->NewStringUTF("N/A"));
	env->SetLongField(info, f_duration, native_info->duration_ms);
	env->SetLongField(info, f_size, native_info->size_byte);
		
	env->SetObjectField(info, f_format, env->NewStringUTF(native_info->format_name));
	env->SetIntField(info, f_video_channels, native_info->video_channels);
	env->SetObjectField(info, f_videocodec_name, env->NewStringUTF(native_info->videocodec_name));
	env->SetIntField(info, f_width, native_info->width);
	env->SetIntField(info, f_height, native_info->height);
}

static jboolean
android_media_MediaPlayer_native_getCurrentMediaInfo(JNIEnv *env, jobject thiz, jobject info)
{
	PPLOGI("native_getCurrentMediaInfo()");
	bool ret = false;

	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return false;
	}

	MediaInfo native_info;
	if (mp->getCurrentMediaInfo(&native_info)) {
		fill_media_info(env, thiz, info, NULL, &native_info);
		ret = true;
	}

	return ret;
}

static jboolean
android_media_MediaPlayer_native_getMediaDetailInfo(JNIEnv *env, jobject thiz, jstring js_media_file_path, jobject info)
{
	PPLOGI("native_getMediaDetailInfo()");
	bool ret = false;
	
	//use ffmpeg to get media info
	IPlayer* mp = getPlayerFun((void*)gPlatformInfo);
	if (mp == NULL)
	{
		PPLOGE("Player init failed.");
		return false;
	}
	
	const char* url = jstr2cstr(env, js_media_file_path);
	MediaInfo native_info;
	if (mp->getMediaDetailInfo(url, &native_info))
	{
		fill_media_info(env, thiz, info, js_media_file_path, &native_info);
		ret = true;
		PPLOGD("Get MediaDetailInfo succeed.");
	}
	else
	{
		PPLOGE("Get MediaDetailInfo failed.");
	}

	releasePlayerFun(mp);

	return ret;
}

static jboolean
android_media_MediaPlayer_native_getThumbnail(JNIEnv *env, jobject thiz, jstring js_media_file_path, jobject info)
{
	bool ret = false;
	IPlayer* mp = getPlayerFun((void*)gPlatformInfo);
	if (mp == NULL) {
		PPLOGE("Player init failed.");
		return false;
	}

	const char* url = jstr2cstr(env, js_media_file_path);
	MediaInfo native_info;
	bool isSuccess = mp->getThumbnail(url, &native_info);
	if (!isSuccess || native_info.thumbnail == NULL) {
		if(native_info.thumbnail != NULL)
        {
            free(native_info.thumbnail);
            native_info.thumbnail = NULL;
        }
		PPLOGE("Get Thumbnail failed: %s", url);
	}
	else {
		jclass clazz = env->FindClass("android/pplive/media/player/MediaInfo");
		jfieldID f_path = env->GetFieldID(clazz, "mPath", "Ljava/lang/String;");
		jfieldID f_duration = env->GetFieldID(clazz, "mDurationMS", "J");
		jfieldID f_size = env->GetFieldID(clazz, "mSizeByte", "J");
		jfieldID f_width = env->GetFieldID(clazz, "mWidth", "I");
		jfieldID f_height = env->GetFieldID(clazz, "mHeight", "I");
		jfieldID f_format = env->GetFieldID(clazz, "mFormatName", "Ljava/lang/String;");
		// fix me!!!
		//jfieldID f_audio = env->GetFieldID(clazz, "mAudioName", "Ljava/lang/String;");
		jfieldID f_videocodec = env->GetFieldID(clazz, "mVideoCodecName", "Ljava/lang/String;");
		jfieldID f_thumbnailwidth = env->GetFieldID(clazz, "mThumbnailWidth", "I");
		jfieldID f_thumbnailheight = env->GetFieldID(clazz, "mThumbnailHeight", "I");
		jfieldID f_thumbnail = env->GetFieldID(clazz, "mThumbnail", "[I");
		jfieldID f_audio_channels = env->GetFieldID(clazz, "mAudioChannels", "I");
		jfieldID f_video_channels = env->GetFieldID(clazz, "mVideoChannels", "I");

		env->SetObjectField(info, f_path, js_media_file_path);
		env->SetLongField(info, f_duration, native_info.duration_ms);
		env->SetLongField(info, f_size, native_info.size_byte);
		env->SetIntField(info, f_width, native_info.width);
		env->SetIntField(info, f_height, native_info.height);
		env->SetObjectField(info, f_format, env->NewStringUTF(native_info.format_name));
		// fix me!!!
		//env->SetObjectField(info, f_audio, env->NewStringUTF(native_info.audio_name));
		env->SetObjectField(info, f_videocodec, env->NewStringUTF(native_info.videocodec_name));

		env->SetIntField(info, f_thumbnailwidth, native_info.thumbnail_width);
		env->SetIntField(info, f_thumbnailheight, native_info.thumbnail_height);
        int size = native_info.thumbnail_width*native_info.thumbnail_height;
        jintArray thumbnail = env->NewIntArray(size);
        env->SetIntArrayRegion(thumbnail,0, size, native_info.thumbnail);
		env->SetObjectField(info, f_thumbnail, thumbnail);
        PPLOGD("get thumbnail success");
        free(native_info.thumbnail);
        native_info.thumbnail = NULL;

		env->SetIntField(info, f_audio_channels, native_info.audio_channels);
		env->SetIntField(info, f_video_channels, native_info.video_channels);
        ret = true;
	}

	releasePlayerFun(mp);

	return ret;
}

static jboolean
android_media_MediaPlayer_native_getSnapShot(JNIEnv *env, jobject thiz, int width, int height, int fmt, int msec, jobject pic)
{
	bool ret = false;
		
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return false;
	}		

	SnapShot *native_pic = NULL;
	native_pic = mp->getSnapShot(width, height, fmt, msec);
	if (native_pic == NULL)
		return false;

	if (native_pic->picture_data == NULL)
		return false;
		
	jclass clazz = env->FindClass("android/pplive/media/player/SnapShot");
	jfieldID f_mSnapShotWidth = env->GetFieldID(clazz, "mSnapShotWidth", "I");
	jfieldID f_mSnapShotHeight = env->GetFieldID(clazz, "mSnapShotHeight", "I");
	jfieldID f_format = env->GetFieldID(clazz, "mSnapFormat", "I");
	jfieldID f_mPicture = env->GetFieldID(clazz, "mPicture", "[I");

	env->SetIntField(pic, f_mSnapShotWidth, native_pic->width);
	env->SetIntField(pic, f_mSnapShotHeight, native_pic->height);
	env->SetIntField(pic, f_format, native_pic->picture_fmt);
	
	int size = native_pic->stride * native_pic->height * 4 / 4; // int is 4 bytes
	jintArray picture = env->NewIntArray(size);
	env->SetIntArrayRegion(picture, 0, size, (jint *)native_pic->picture_data);
	env->SetObjectField(pic, f_mPicture, picture);
	PPLOGI("get snapshot success %d x %d, size %d", native_pic->width, native_pic->height, size);
	
	free(native_pic);
	native_pic = NULL;
	return true;
}

static
jobject android_media_MediaPlayer_native_getTrackInfo(JNIEnv *env, jobject thiz)
{
	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return NULL;
	}	
	
	return NULL;
}

static
jint android_media_MediaPlayer_native_getCpuArchNumber()
{
	return android_getCpuCount();
}

static
jstring android_media_MediaPlayer_native_getVersion(JNIEnv *env, jobject thiz)
{
	return cstr2jstr(env, MEET_NATIVE_VERISON);
}

static
jboolean android_media_MediaPlayer_native_supportSoftDecode()
{
#if defined(__aarch64__)
	return true;
#else
	char value[PROP_VALUE_MAX];
	__system_property_get("ro.product.cpu.abi", value);
	char* occ = strcasestr(value,"x86");
	if (occ) {
		// x86 arch
		PPLOGI("the device is x86 platform");
		return true;
	}

	uint64_t cpuFeatures = android_getCpuFeatures();
	if ((cpuFeatures & ANDROID_CPU_ARM_FEATURE_NEON) != 0)
		return true;

	return false;
#endif
}

static
void android_media_MediaPlayer_native_set_option(JNIEnv *env, jobject thiz, jstring option)
{
	PPLOGI("set_option");

	IPlayer* mp = getMediaPlayer(env, thiz);
	if (mp == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return;
	}

	if (option == NULL) {
		jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
		return;
	}

	const char *optStr = env->GetStringUTFChars(option, NULL);
	if (optStr == NULL) {  // Out of memory
		jniThrowException(env, "java/lang/RuntimeException", "GetStringUTFChars: Out of memory");
		return;
	}

	PPLOGI("set_option %s", optStr);
	mp->set_opt(optStr);

	env->ReleaseStringUTFChars(option, optStr);
}

// ----------------------------------------------------------------------------

static JNINativeMethod gMethods[] = {
	{"_setDataSource",       "(Ljava/lang/String;)V",		(void *)android_media_MediaPlayer_setDataSource},
	{"_setDataSource",       "(Ljava/lang/String;Ljava/util/Map;)V",	(void *)android_media_MediaPlayer_setDataSourceAndHeaders},
	{"_setDataSource",       "(Ljava/io/FileDescriptor;JJ)V",	(void *)android_media_MediaPlayer_setDataSourceFD},
	{"_setVideoSurface",    "(Landroid/view/Surface;)V",					(void *)android_media_MediaPlayer_setVideoSurface},
	{"_prepare",             "()V",					(void *)android_media_MediaPlayer_prepare},
	{"_prepareAsync",        "()V",					(void *)android_media_MediaPlayer_prepareAsync},
	
	{"_start",              "()V",					(void *)android_media_MediaPlayer_start},
	{"_pause",              "()V",					(void *)android_media_MediaPlayer_pause},
	{"_stop",               "()V",					(void *)android_media_MediaPlayer_stop},
	{"_seekTo",              "(I)V",				(void *)android_media_MediaPlayer_seekTo},
	{"_release",            "()V",					(void *)android_media_MediaPlayer_release},
	{"_reset",              "()V",					(void *)android_media_MediaPlayer_reset},
	// get info
	{"getVideoWidth",       "()I",					(void *)android_media_MediaPlayer_getVideoWidth},
	{"getVideoHeight",      "()I",					(void *)android_media_MediaPlayer_getVideoHeight},
	{"getCurrentPosition",  "()I",					(void *)android_media_MediaPlayer_getCurrentPosition},
	{"getDuration",         "()I",					(void *)android_media_MediaPlayer_getDuration},
	{"getBufferingTime", 	"()I",					(void *)android_media_MediaPlayer_getBufferingTime},
	// state
	{"isPlaying",           "()Z",					(void *)android_media_MediaPlayer_isPlaying},
	{"isLooping",           "()Z",					(void *)android_media_MediaPlayer_isLooping},
	{"flags",               "()I",					(void *)android_media_MediaPlayer_flags},
	// set
	{"_setAudioStreamType",  "(I)V",					(void *)android_media_MediaPlayer_setAudioStreamType},
	{"_selectAudioChannel",  "(I)V",					(void *)android_media_MediaPlayer_selectAudioChannel},
	{"_selectSubtitleChannel",  "(I)V",					(void *)android_media_MediaPlayer_selectSubtitleChannel},
	{"setLooping",          "(Z)V",					(void *)android_media_MediaPlayer_setLooping},
	{"setVolume",           "(FF)V",				(void *)android_media_MediaPlayer_setVolume},

	{"native_setSubtitleParser",  "(Landroid/pplive/media/subtitle/SimpleSubTitleParser;)V", (void *)android_media_MediaPlayer_setSubtitleParser},
	
	{"getFrameAt",          "(I)Landroid/graphics/Bitmap;",		(void *)android_media_MediaPlayer_getFrameAt},
	{"native_invoke",       "(Landroid/os/Parcel;Landroid/os/Parcel;)I",(void *)android_media_MediaPlayer_invoke},
	{"native_setMetadataFilter", "(Landroid/os/Parcel;)I",		(void *)android_media_MediaPlayer_setMetadataFilter},
	{"native_getMetadata", "(ZZLandroid/os/Parcel;)Z",		(void *)android_media_MediaPlayer_getMetadata},
	
	{"native_init",         "()Z",					(void *)android_media_MediaPlayer_native_init},
	{"native_setup",        "(Ljava/lang/Object;)V",		(void *)android_media_MediaPlayer_native_setup},
	{"snoop",               "([SI)I",				(void *)android_media_MediaPlayer_snoop},
	{"native_suspend_resume", "(Z)I",				(void *)android_media_MediaPlayer_native_suspend_resume},
	{"native_checkCompatibility","(ILandroid/view/Surface;)Z",	(void *)android_media_MediaPlayer_native_checkCompatibility},
	{"native_getBestCodec",        "(Ljava/lang/String;)Ljava/lang/String;",(void *)android_media_MediaPlayer_getBestCodec},
	{"native_getMediaInfo",	"(Ljava/lang/String;Landroid/pplive/media/player/MediaInfo;)Z",(void *)android_media_MediaPlayer_native_getMediaInfo},
	{"native_getMediaDetailInfo",	"(Ljava/lang/String;Landroid/pplive/media/player/MediaInfo;)Z",(void *)android_media_MediaPlayer_native_getMediaDetailInfo},
	{"native_getThumbnail",	"(Ljava/lang/String;Landroid/pplive/media/player/MediaInfo;)Z",(void *)android_media_MediaPlayer_native_getThumbnail},
	{"native_getSnapShot",	"(IIIILandroid/pplive/media/player/SnapShot;)Z",(void *)android_media_MediaPlayer_native_getSnapShot},
	{"native_getTrackInfo", 	"()[Landroid/media/MediaPlayer$TrackInfo;",	(void *)android_media_MediaPlayer_native_getTrackInfo},
	{"native_checkSoftwareDecodeLevel",	"()I",(void *)android_media_MediaPlayer_native_checkSoftwareDecodeLevel},
	{"native_getCpuArchNumber",	"()I",(void *)android_media_MediaPlayer_native_getCpuArchNumber},
	{"native_getVersion",	"()Ljava/lang/String;",(void *)android_media_MediaPlayer_native_getVersion},
	{"native_supportSoftDecode",	"()Z",(void *)android_media_MediaPlayer_native_supportSoftDecode},
	{"setOption",	"(Ljava/lang/String;)V",(void *)android_media_MediaPlayer_native_set_option},
	{"native_getCurrentMediaInfo",	"(Landroid/pplive/media/player/MediaInfo;)Z",(void *)android_media_MediaPlayer_native_getCurrentMediaInfo},
};


// This function only registers the native methods
int register_android_media_MediaPlayer(JNIEnv *env)
{
	return jniRegisterNativeMethodsPP(env,
			"android/pplive/media/player/FFMediaPlayer", gMethods, NELEM(gMethods));
}

void unload_player()
{
#ifndef BUILD_ONE_LIB
	unloadPlayerLib(&player_handle_software);
#endif

	if (gPlatformInfo != NULL) {
		delete gPlatformInfo;
		gPlatformInfo = NULL;
	}
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
static int jniThrowException(JNIEnv* env, const char* className, const char* msg)
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





