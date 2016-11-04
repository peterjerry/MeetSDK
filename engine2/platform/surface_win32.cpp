/*
 * Copyright (C) 2015 Michael.Ma  guoliangma@pptv.com
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

#define DEFAULT_MAX_DISPLAY_WIDTH	1280
#define DEFAULT_MAX_DISPLAY_HEIGHT	720
int g_max_display_w = DEFAULT_MAX_DISPLAY_WIDTH;
int g_max_display_h = DEFAULT_MAX_DISPLAY_HEIGHT;
int g_display_w = 640;
int g_display_h = 480;

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
#endif

#ifdef ENABLE_SUBTITLE
TTF_Font*		g_font		= NULL;
char*			g_text		= NULL;
SDL_mutex*		g_mutex		= NULL;
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

	g_mutex = SDL_CreateMutex();
#endif

	return OK;
}
#else
status_t Surface_open2(void* surf)
{
	g_surface	= (SDL_Surface *)surf;
	g_texture_w	= g_surface->w;
	g_texture_h	= g_surface->h;
	g_pData = new char[g_texture_w * g_texture_h * 4];
	return OK;
}
#endif
#endif

#ifdef ENABLE_SUBTITLE
status_t Surface_setText(char *text)
{
	SDL_LockMutex(g_mutex);
	if (g_text) {
		free(g_text);
		g_text = NULL;
	}

	if (text)
		g_text = _strdup(text);
	SDL_UnlockMutex(g_mutex);
	return OK;
}
#endif

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

#ifdef ENABLE_SUBTITLE
	SDL_LockMutex(g_mutex);
	if (g_text) {
		int offset = 10;
		for (unsigned int i = 0;i<strlen(g_text);i++) {
			if (g_text[i] == '\n')
				offset += TTF_FontHeight(g_font);
		}

		int start = 0;
		int end;
		for (unsigned int i = 0;i<strlen(g_text);i++) {
			if (i == strlen(g_text) - 1 || g_text[i] == '\n') {
				end = i;

				char tmp[2048] = {0};
				if (i == strlen(g_text) - 1)
					strcpy(tmp, g_text + start);
				else
					strncpy(tmp, g_text + start, end - start - 1);
				SDL_Surface *text_surf = TTF_RenderUTF8_Blended(g_font, tmp, RGB_White);  
				if (text_surf) {  
					SDL_Texture *t = SDL_CreateTextureFromSurface(g_renderer, text_surf);

					SDL_Rect dstRect;

					Uint32 format;
					int access;
					int sub_w, sub_h;

					SDL_QueryTexture(t, &format, &access, &sub_w, &sub_h);
					dstRect.x = (g_texture_w - sub_w) / 2;  
					dstRect.y = g_texture_h - sub_h - offset;  
					dstRect.w = sub_w;  
					dstRect.h = sub_h;
					if (dstRect.x < 0)
						dstRect.x = 0;
					SDL_RenderCopy(g_renderer, t, NULL, &dstRect);
					SDL_DestroyTexture(t);
					SDL_FreeSurface(text_surf);

					offset -= sub_h;
					if (offset < 0)
						offset = 0;
				}  

				start = i + 1;
			}
		}
	}
	SDL_UnlockMutex(g_mutex);
#endif

	SDL_RenderPresent(g_renderer);
#else
	SDL_Surface* surf = NULL;
	surf = SDL_CreateRGBSurfaceFrom(g_pData, g_texture_w, g_texture_h, 32,
		g_texture_w * 4, 0, 0, 0, 0);
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

#if defined(USE_SDL2) && defined(ENABLE_SUBTITLE)
	TTF_Quit();

	SDL_DestroyMutex(g_mutex);
#endif

    return OK;
}
