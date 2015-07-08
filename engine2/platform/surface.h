/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */

#ifndef SURFACE_WRAPPER_H
#define SURFACE_WRAPPER_H

#include <stdint.h>
#if defined(_MSC_VER) || defined(__CYGWIN__)
#include "pperrors.h"
#else
#include "errors.h"
#endif

#if __cplusplus
extern "C" {
#endif
    
#if defined(__ANDROID__)
status_t Surface_open(void* surface, void** window);
#elif defined(OS_IOS)
status_t Surface_open(void* surface, uint32_t frameWidth, uint32_t frameHeight, uint32_t frameFormat);
#else //if defined(__CYGWIN__) || defined(_MSC_VER)
#ifdef USE_SDL2
status_t Surface_open3(void* window, void *renderer, void* texture);
#else
status_t Surface_open2(void* surf);
#endif
#endif

#ifdef __ANDROID__
status_t Surface_getPixels(void* window, uint32_t* width, uint32_t* height, uint32_t* stride, void** pixels);
#else
status_t Surface_getPixels(uint32_t* width, uint32_t* height, uint32_t* stride, void** pixels);
#endif

#ifdef __ANDROID__
status_t Surface_updateSurface(void* window);
#else
status_t Surface_updateSurface();
#endif

status_t Surface_displayPicture(void* picture);

#ifdef __ANDROID__
status_t Surface_close(void* window);
#else
status_t Surface_close();
#endif

#if __cplusplus
}
#endif
#endif

