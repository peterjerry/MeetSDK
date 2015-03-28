/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */

#define LOG_TAG "Neon-SurfaceWrapper"
#include "log.h"
#include "surface.h"

#include <dlfcn.h>
#include <android/native_window.h>
#include <jni.h>
#include <android/native_window_jni.h>

#define ANDROID_SYM_S_LOCK "_ZN7android7Surface4lockEPNS0_11SurfaceInfoEb"
#define ANDROID_SYM_S_LOCK2 "_ZN7android7Surface4lockEPNS0_11SurfaceInfoEPNS_6RegionE"
#define ANDROID_SYM_S_UNLOCK "_ZN7android7Surface13unlockAndPostEv"

typedef struct _SurfaceInfo {
    uint32_t    w;
    uint32_t    h;
    uint32_t    s;
    uint32_t    usage;
    uint32_t    format;
    uint32_t*   bits;
    uint32_t    reserved[2];
} SurfaceInfo;

typedef status_t (*Surface_lock)(void *, void *, int);
typedef status_t (*Surface_lock2)(void *, void *, void *);
typedef status_t (*Surface_unlockAndPost)(void *);
typedef ANativeWindow* (*ptr_ANativeWindow_fromSurface)(JNIEnv*, jobject);
typedef void (*ptr_ANativeWindow_release)(ANativeWindow*);
typedef int32_t (*ptr_ANativeWindow_lock)(ANativeWindow*, ANativeWindow_Buffer*, ARect*);

extern JavaVM* gs_jvm;
extern jobject gs_androidsurface;

static ANativeWindow* window = NULL;

#ifndef NDK_NATIVE_WINDOW_IMPL
static void* sSurface = NULL;
static void* p_library = NULL;
static Surface_lock s_lock = NULL;
static Surface_lock2 s_lock2 = NULL;
static ptr_ANativeWindow_fromSurface s_winFromSurface = NULL;
static ptr_ANativeWindow_release s_winRelease = NULL;
static ptr_ANativeWindow_lock s_winLock = NULL;
static Surface_unlockAndPost s_unlockAndPost = NULL;

static inline status_t LoadSurface(const char *psz_lib)
{
    p_library = dlopen(psz_lib, RTLD_NOW);
    if (p_library)
    {
        LOGI("Load lib %s success", psz_lib);
        s_lock = (Surface_lock)(dlsym(p_library, ANDROID_SYM_S_LOCK));
        s_lock2 = (Surface_lock2)(dlsym(p_library, ANDROID_SYM_S_LOCK2));
        s_unlockAndPost =
            (Surface_unlockAndPost)(dlsym(p_library, ANDROID_SYM_S_UNLOCK));
        if ((s_lock ||s_lock2) && s_unlockAndPost)
        {
            return OK;
        }
        else
        {
            LOGE("lib %s does not provide required functions", psz_lib);
        }
        s_lock = NULL;
        s_lock2 = NULL;
        s_unlockAndPost = NULL;
        dlclose(p_library);
    }
    else
    {
        LOGE("Load lib %s failed", psz_lib);
    }
    return ERROR;
}

static inline status_t LoadANativeWindow(const char *psz_lib)
{
    p_library = dlopen(psz_lib, RTLD_NOW);
    if (p_library)
    {
        LOGI("Load lib %s success", psz_lib);
		s_winFromSurface = (ptr_ANativeWindow_fromSurface)(dlsym(p_library, "ANativeWindow_fromSurface"));
		s_winRelease = (ptr_ANativeWindow_release)(dlsym(p_library, "ANativeWindow_release"));
		s_winLock = (ptr_ANativeWindow_lock)(dlsym(p_library, "ANativeWindow_lock"));
		s_unlockAndPost = (Surface_unlockAndPost)(dlsym(p_library, "ANativeWindow_unlockAndPost"));

		if (s_winFromSurface && s_winRelease && s_winLock && s_unlockAndPost)
		{
        	if(gs_androidsurface && gs_jvm)
        	{
        	    JNIEnv* env = NULL;
                if (gs_jvm->GetEnv((void**) &env, JNI_VERSION_1_4) == JNI_OK)
                {
            	    window = s_winFromSurface(env, gs_androidsurface);
                    if(window)
                    {
                        sSurface = window;
                        return OK;
                    }
                    else
                    {
            		    LOGE("failed to get window");
                    }
                }
                else
                {
        		    LOGE("GetEnv failed");
                }
        	}
        	else
        	{
        		LOGE("java surface not ready");
        	}
		}
        else
        {
            LOGE("lib %s does not provide required functions", psz_lib);
        }

	    s_winFromSurface = NULL;
	    s_winRelease = NULL;
	    s_winLock = NULL;
	    s_unlockAndPost = NULL;
   		dlclose(p_library);
    }
    else
    {
        LOGE("Load lib %s failed", psz_lib);
    }
    return ERROR;
}

static status_t InitLibrary()
{
    if(LoadANativeWindow("libandroid.so") == OK)
        return OK;
    if(LoadSurface("libsurfaceflinger_client.so") == OK)
        return OK;
    if(LoadSurface("libgui.so") == OK)
        return OK;
    if(LoadSurface("libui.so") == OK)
        return OK;
    return ERROR;
}

static status_t Surface_check()
{
    SurfaceInfo info;

	if(s_winLock)
    {
        ANativeWindow_Buffer buf = { 0 };
        if(s_winLock(window, &buf, NULL) < 0)
        {
            LOGE("check s_winLock failed");
            return ERROR;
        }
		if(buf.width < 0)
		{
			LOGE("check surface width:%d is invalid", buf.width);
			return ERROR;
		}
		if(buf.height < 0)
		{
			LOGE("check surface height:%d is invalid", buf.height);
			return ERROR;
		}
		if(buf.stride < 0)
		{
			LOGE("check surface stride:%d is invalid", buf.stride);
			return ERROR;
		}
		
        info.w = buf.width;
        info.h = buf.height;
        info.bits = (uint32_t *)buf.bits;
        info.s = buf.stride;
        info.format = buf.format;
    }
    else if(s_lock)
    {
        if(s_lock(sSurface, &info, 1) != OK)
        {
            LOGE("check s_lock failed");
            return ERROR;
        }
    }
    else if(s_lock2)
    {
        if(s_lock2(sSurface, &info, NULL) != OK)
        {
            LOGE("check s_lock2 failed");
            return ERROR;
        }
    }
	else
	{
		LOGE("check no available lock api");
		return ERROR;
	}

    if(info.format != WINDOW_FORMAT_RGBA_8888
        && info.format != WINDOW_FORMAT_RGBX_8888)
    {
        LOGE("check surface format:%d is invalid", info.format);
        return ERROR;
    }
    if(info.s < info.w)
    {
        LOGE("check surface stride/width:%d/%d is invalid", info.s, info.w);
        return ERROR;
    }

    if(s_unlockAndPost(sSurface) != OK)
    {
        LOGE("check s_unlockAndPost failed");
        return ERROR;
    }

	return OK;
}
#endif

status_t Surface_open(void* surface)
{
	if(surface == NULL)
        return ERROR;

#ifdef NDK_NATIVE_WINDOW_IMPL
	JNIEnv* env = NULL;
    if (gs_jvm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		LOGE("GetEnv failed");
		return ERROR;
	}

    window = ANativeWindow_fromSurface(env, gs_androidsurface);
    if (!window)
        LOGE("failed to get window");

	return OK;
#else
	sSurface = surface;
    if (InitLibrary() != OK)
		return ERROR;
	return Surface_check();
#endif
}

status_t Surface_getPixels(uint32_t* width, uint32_t* height, uint32_t* stride, void** pixels)
{
#ifdef NDK_NATIVE_WINDOW_IMPL
	ANativeWindow_Buffer buffer = {0};
	int32_t ret = ANativeWindow_lock(window, &buffer, NULL);
	if (ret != 0) {
		LOGE("failed to lock native window: %d", ret);
		return ERROR;
	}

	if ((WINDOW_FORMAT_RGBX_8888 != buffer.format) && (WINDOW_FORMAT_RGBA_8888 != buffer.format)) {
		LOGE("native window format is not valid: %d", buffer.format);
		return ERROR;
	}

	if (NULL == buffer.bits) {
		LOGE("native window bits is null");
		return ERROR;
	}

	*width	= buffer.width;
	*height	= buffer.height;
	*stride	= buffer.stride;
	*pixels = buffer.bits;
	return OK;
#else
    SurfaceInfo info;

	if(s_winLock)
    {
        ANativeWindow_Buffer buf = {0};
        if(s_winLock(window, &buf, NULL) < 0)
        {
            LOGE("s_winLock failed");
            return ERROR;
        }
        info.w = buf.width;
        info.h = buf.height;
        info.bits = (uint32_t *)buf.bits;
        info.s = buf.stride;
        info.format = buf.format;
    }
    else if(s_lock)
    {
        if(s_lock(sSurface, &info, 1) != OK)
        {
            LOGE("s_lock failed");
            return ERROR;
        }
    }
    else if(s_lock2)
    {
        if(s_lock2(sSurface, &info, NULL) != OK)
        {
            LOGE("s_lock2 failed");
            return ERROR;
        }
    }
	else
	{
		LOGE("no available lock api");
		return ERROR;
	}

    if(width != NULL)
    {
        *width = info.w;
        LOGD("SurfaceInfo.width:%d", *width);
    }
    if(height != NULL)
    {
        *height = info.h;
        LOGD("SurfaceInfo.height:%d", *height);
    }
    if(stride != NULL)
    {
        *stride = info.s;
        LOGD("SurfaceInfo.stride:%d", *stride);
    }
    LOGD("SurfaceInfo.format:%d", info.format);
	*pixels = info.bits;
    LOGD("SurfaceInfo.pixels:%p", *pixels);

	return OK;
#endif
}

status_t Surface_updateSurface()
{
#ifdef NDK_NATIVE_WINDOW_IMPL
	if (ANativeWindow_unlockAndPost(window) != 0) {
		LOGE("ANativeWindow_unlockAndPost failed");
		return ERROR;
	}
	
	return OK;
#else
	// Surface inherit from ANativeWindow.
    if(s_unlockAndPost(sSurface) != OK)
    {
        LOGE("s_unlockAndPost failed");
        return ERROR;
    }
	return OK;
#endif
}

status_t Surface_close()
{
#ifdef NDK_NATIVE_WINDOW_IMPL
	ANativeWindow_release(window);
	return OK;
#else
	if (window && s_winRelease)
	{
        s_winRelease(window);
	}
	if(p_library != NULL)
	{
		dlclose(p_library);
	}
	LOGD("unregistered");
    return OK;
#endif
}
