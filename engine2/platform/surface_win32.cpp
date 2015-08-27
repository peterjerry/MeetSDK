/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */

#define LOG_TAG "Neon-SurfaceWrapper"
#include "log.h"
#include "surface.h"
#include "sdl.h"
#ifdef ENABLE_SUBTITLE
#include "sdl_ttf.h"
const SDL_Color RGB_Black   = { 0, 0, 0 };  
const SDL_Color RGB_Red     = { 255, 0, 0 };  
const SDL_Color RGB_White   = { 255, 255, 255 };  
const SDL_Color RGB_Yellow  = { 255, 255, 0 }; 
#endif
#include <string.h>
#include <stdio.h>

#ifdef ENABLE_SUBTITLE
#pragma comment(lib, "sdl2_ttf")
#endif

#ifdef USE_SDL2
SDL_Window*		g_window	= NULL;
SDL_Renderer*	g_renderer	= NULL;
SDL_Texture*	g_texture	= NULL;
int				g_scr_w		= 0;
int				g_scr_h		= 0;
SDL_Rect		srcRect		= {0, 0, 0, 0};
SDL_Rect		dstRect		= {0, 0, 0, 0};
#else
SDL_Surface*	g_surface	= NULL;
TTF_Font*		g_font		= NULL;
char*			g_text		= NULL;
#endif
char*			g_pData		= NULL;
int				g_texture_w	= 0;
int				g_texture_h	= 0;

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

	Uint32 format;
	int access;

	SDL_GetWindowSize(g_window, &g_scr_w, &g_scr_h);
	SDL_QueryTexture(g_texture, &format, &access, &g_texture_w, &g_texture_h);
	srcRect.w	= g_texture_w;
	srcRect.h	= g_texture_h;
	g_pData		= new char[g_texture_w * g_texture_h * 4];

	return OK;
}
#else
status_t Surface_open2(void* surf)
{
	g_surface	= (SDL_Surface *)surf;
	g_texture_w	= g_surface->w;
	g_texture_h	= g_surface->h;
	g_pData = new char[g_texture_w * g_texture_h * 4];

#ifdef ENABLE_SUBTITLE
	/* 初始化字体库 */  
    if (TTF_Init() == -1 ) {
		LOGE("failed to init ttf");
		return ERROR;  
	}  
  
    /* 打开simfang.ttf 字库，设字体为20号 */  
    g_font  = TTF_OpenFont("C:\\windows\\fonts\\simhei.ttf", 24);  
    if (g_font == NULL) {  
		LOGE("failed to open font");
        return ERROR;  
    }    

	TTF_SetFontStyle(g_font, TTF_STYLE_BOLD); 
#endif

	return OK;
}
#endif
#endif

status_t Surface_setText(char *text)
{
	if (g_text)
		free(g_text);

	g_text = _strdup(text);
	return OK;
}

status_t Surface_getPixels(uint32_t* width, uint32_t* height, uint32_t* stride, void** pixels)
{
	*width	= g_texture_w;
	*height	= g_texture_h;
	*stride = g_texture_w;
	*pixels = g_pData;

	return OK;
}

status_t Surface_updateSurface()
{
#ifdef USE_SDL2
	SDL_SetRenderDrawColor(g_renderer, 0x0, 0x0, 0x0, 0xFF);
	//SDL_SetRenderDrawColor(is->renderer, 0xA0, 0xA0, 0xA0, 0xFF);//gray
	SDL_RenderClear(g_renderer);
	SDL_UpdateTexture(g_texture, NULL, g_pData, g_texture_w * 4);

#ifdef SDL_EMBEDDED_WINDOW
	SDL_RenderCopy(g_renderer, g_texture, NULL, NULL);
#else
	if (dstRect.w == 0 || dstRect.h == 0) {
		dstRect.w = g_scr_w;
		dstRect.h = g_scr_h;

		if (g_texture_w * g_scr_h > g_scr_w * g_texture_h) {
			LOGI("SDL surface too high");
			dstRect.h = dstRect.w * g_texture_h / g_texture_w;
			dstRect.y = (g_scr_h - dstRect.h) / 2;
		}
		else if (g_texture_w * g_scr_h < g_scr_w * g_texture_h) {
			LOGI("SDL surface too wide");
			dstRect.w = dstRect.h * g_scr_w / g_scr_h;
			dstRect.x = (g_scr_w - dstRect.w) / 2;
		}
	}
	
    SDL_RenderCopy(g_renderer, g_texture, &srcRect, &dstRect);
#endif
	SDL_RenderPresent(g_renderer);
#else
	SDL_Surface* surf = NULL;
	surf = SDL_CreateRGBSurfaceFrom(g_pData, g_texture_w, g_texture_h, 32,
		g_texture_w * 4, 0, 0, 0, 0);
	SDL_BlitSurface(surf, NULL, g_surface, NULL);

	if (g_text) {
		SDL_Surface *text_surf = TTF_RenderText_Blended(g_font, "aaaaaaabbbb", RGB_Red);  
		if (text_surf) {  
			SDL_Rect rect;
  
			rect.x = 200;
			rect.y = 200;
			rect.w = text_surf->w;
			rect.h = text_surf->h;
			SDL_BlitSurface(text_surf, NULL, g_surface, &rect);
			//SDL_FreeSurface(pText);    
		}  
	}

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
