/*
 * Copyright (C) 2015 guoliang.ma@pptv.com
 *
 */

#define LOG_TAG "Neon-SurfaceWrapper"
#include "log.h"
#include "surface.h"

#include <dlfcn.h>
#include <android/native_window.h>
#include <jni.h>
#include <android/native_window_jni.h>

extern JavaVM* gs_jvm;

status_t Surface_open(void* surface, void** window)
{
	if (surface == NULL)
        return ERROR;

	JNIEnv* env = NULL;
    if (gs_jvm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		LOGE("GetEnv failed");
		return ERROR;
	}

	jobject nativeSurf = (jobject)surface;
	ANativeWindow *native_window = ANativeWindow_fromSurface(env, nativeSurf);
    if (!native_window) {
		LOGE("failed to get window");
		return ERROR;
	}

	*window = native_window;
	return OK;
}

status_t Surface_getPixels(void* window, uint32_t* width, uint32_t* height, uint32_t* stride, void** pixels)
{
	ANativeWindow *native_window = (ANativeWindow *)window;

	ANativeWindow_Buffer buffer = {0};
	int32_t ret = ANativeWindow_lock(native_window, &buffer, NULL);
	if (ret != 0) {
		LOGE("failed to lock native window: %d", ret);
		return ERROR;
	}

	if ((WINDOW_FORMAT_RGBX_8888 != buffer.format) && (WINDOW_FORMAT_RGBA_8888 != buffer.format)) {
		// 2015.3.31 guoliangma added to fix "already locked" problem when window_format is invalid
		ANativeWindow_unlockAndPost(native_window);

		LOGE("native window format is not valid: %d", buffer.format);
		return ERROR;
	}

	if (NULL == buffer.bits) {
		// 2015.3.31 guoliangma added
		ANativeWindow_unlockAndPost(native_window);

		LOGE("native window bits is null");
		return ERROR;
	}

	*width	= buffer.width;
	*height	= buffer.height;
	*stride	= buffer.stride;
	*pixels = buffer.bits;
	return OK;
}

status_t Surface_updateSurface(void* window)
{
	ANativeWindow *native_window = (ANativeWindow *)window;

	if (ANativeWindow_unlockAndPost(native_window) != 0) {
		LOGE("ANativeWindow_unlockAndPost failed");
		return ERROR;
	}
	
	return OK;
}

status_t Surface_close(void* window)
{
	ANativeWindow *native_window = (ANativeWindow *)window;
	if (native_window) {
		ANativeWindow_release(native_window);
		native_window = NULL;
	}

	return OK;
}
