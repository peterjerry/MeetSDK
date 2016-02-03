#include "gles2.h"
#include "jni.h"
#include <dlfcn.h> // for dlopen ...
#include "jniUtils.h"
#include "platform/android_opengles_render.h"
#define LOG_TAG "gles2"
#include "pplog.h"

typedef android_gles_render* (*GET_RENDERER_FUN)();
GET_RENDERER_FUN getRendererFun = NULL;

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_gotye_meetsdk_player_YUVRender
 * Method:    nativeInit
 * Signature: (Lcom/gotye/meetsdk/player/MeetGLYUVView;)J
 */
JNIEXPORT jlong JNICALL Java_com_gotye_meetsdk_player_YUVRender_nativeInit
  (JNIEnv *env , jobject thiz, jobject obj)
{
	PPLOGI("nativeInit()");

#ifdef BUILD_ONE_LIB
	getRendererFun		= getRenderer;
#endif

	android_gles_render *pRender = getRendererFun();
	pRender->setRequestMethod(env, obj);
	return (jlong)pRender;
}

/*
 * Class:     com_gotye_meetsdk_player_YUVRender
 * Method:    nativeResize
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_com_gotye_meetsdk_player_YUVRender_nativeResize
  (JNIEnv * env, jobject thiz, jlong ptr, jint width, jint height)
{
	PPLOGI("nativeResize()");

	if (!ptr)
		return;

	android_gles_render* pRender = (android_gles_render*) ptr;
	if (!pRender->ogl_init(width, height))
		jniThrowException(env, "java/lang/RuntimeException", "failed to init ogl");
}

/*
 * Class:     com_gotye_meetsdk_player_YUVRender
 * Method:    nativeRender
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_gotye_meetsdk_player_YUVRender_nativeRender(
	JNIEnv * env, jobject thiz, jlong ptr) 
{
	if (!ptr)
		return;

	android_gles_render* pRender = (android_gles_render*) ptr;
	pRender->ogl_render();
}

#ifdef __cplusplus
}
#endif

bool setup_renderer(void *so_handle)
{
	getRendererFun = (GET_RENDERER_FUN)dlsym(so_handle, "getRenderer");
	if (getRendererFun == NULL) {
		PPLOGE("Init getRenderer() failed: %s", dlerror());
		return false;
	}

	return true;
}