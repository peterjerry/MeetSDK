/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */

#define LOG_TAG "Neon-SurfaceWrapper"
#include "log.h"
#include "surface.h"
#include "sdl.h"
#include <string.h>
#include <stdio.h>

SDL_Surface*	g_surface	= NULL;
char*			g_pData		= NULL;
int				g_window_w	= 0;
int				g_window_h	= 0;

status_t Surface_open(void* surface)
{
	if (surface == NULL)
        return ERROR;

	return OK;
}

#if defined(__CYGWIN__) || defined(_MSC_VER)
status_t Surface_open2(void* surf)
{
	g_surface	= (SDL_Surface *)surf;
	g_window_w	= g_surface->w;
	g_window_h	= g_surface->h;
	g_pData = new char[g_window_w * g_window_h * 4];

	return OK;
}
#endif

status_t Surface_getPixels(uint32_t* width, uint32_t* height, uint32_t* stride, void** pixels)
{
	*width	= g_window_w;
	*height	= g_window_h;
	*stride = g_window_w;
	*pixels = g_pData;

	return OK;
}

status_t Surface_updateSurface()
{
	SDL_Surface* surf = NULL;
	surf = SDL_CreateRGBSurfaceFrom(g_pData, g_window_w, g_window_h, 32,
		g_window_w * 4, 0, 0, 0, 0);
	SDL_BlitSurface(surf, NULL, g_surface, NULL);
	SDL_Flip(g_surface);
	SDL_FreeSurface(surf);
	surf = NULL;

	return OK;
}

status_t Surface_close()
{
	if(g_pData) {
		delete g_pData;
		g_pData = NULL;
	}

    return OK;
}
