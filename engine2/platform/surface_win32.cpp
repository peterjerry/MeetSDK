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

#ifdef USE_SDL2
SDL_Window*		g_window	= NULL;
SDL_Renderer*	g_renderer	= NULL;
SDL_Texture*	g_texture	= NULL;
#else
SDL_Surface*	g_surface	= NULL;
#endif
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
#ifdef USE_SDL2
status_t Surface_open3(void* window, void *renderer, void* texture)
{
	g_window	= (SDL_Window *)window;
	g_renderer	= (SDL_Renderer *)renderer;
	g_texture	= (SDL_Texture *)texture;

	SDL_GetWindowSize(g_window, &g_window_w, &g_window_h);
	g_pData = new char[g_window_w * g_window_h * 4];

	return OK;
}
#else
status_t Surface_open2(void* surf)
{
	g_surface	= (SDL_Surface *)surf;
	g_window_w	= g_surface->w;
	g_window_h	= g_surface->h;
	g_pData = new char[g_window_w * g_window_h * 4];

	return OK;
}
#endif
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
#ifdef USE_SDL2
	SDL_SetRenderDrawColor(g_renderer, 0x0, 0x0, 0x0, 0xFF);
	//SDL_SetRenderDrawColor(is->renderer, 0xA0, 0xA0, 0xA0, 0xFF);//gray
	SDL_RenderClear(g_renderer);
	SDL_UpdateTexture(g_texture, NULL, g_pData, g_window_w * 4);
    SDL_RenderCopy(g_renderer, g_texture, NULL, NULL);
	SDL_RenderPresent(g_renderer);
#else
	SDL_Surface* surf = NULL;
	surf = SDL_CreateRGBSurfaceFrom(g_pData, g_window_w, g_window_h, 32,
		g_window_w * 4, 0, 0, 0, 0);
	SDL_BlitSurface(surf, NULL, g_surface, NULL);
	SDL_Flip(g_surface);
	SDL_FreeSurface(surf);
	surf = NULL;
#endif

	return OK;
}

status_t Surface_close()
{
	if (g_pData) {
		delete g_pData;
		g_pData = NULL;
	}

    return OK;
}
