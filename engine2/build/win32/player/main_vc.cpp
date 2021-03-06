#include <stdio.h>
#include <string.h>
#ifndef _MSC_VER
#include <unistd.h>
#endif

#include "ffplayer.h"
#include "surface.h"
#define LOG_TAG "testapp"
#include "log.h"
#include "apFileLog.h"
#include "apminidump.h"

#include "sdl.h"
#undef main

#ifdef _MSC_VER
#ifdef USE_SDL2
#pragma comment(lib, "sdl2")
#else
#pragma comment(lib, "sdlmain")
#pragma comment(lib, "sdl")
#endif
#endif

#define CLIP_NAME "http://172.16.204.106/test/h265/Transformers3-720p.mp4"

FFPlayer *player	= NULL;
int g_finished		= 0;
int g_quit			= 0;
int32_t g_width, g_height;
int32_t g_duration;

int32_t scr_width, scr_height;

#ifdef USE_SDL2
SDL_Window *sdlWindow	= NULL;
SDL_Renderer *sdlRender = NULL;
SDL_Texture *sdlTexture = NULL;
Uint32 myOnPreparedEventType = -1;
#endif

void start_player();

class my_listener:public MediaPlayerListener
{
public:
	my_listener();
	~my_listener();
	virtual void notify(int msg, int ext1, int ext2);
};

my_listener::my_listener()
{

}

my_listener::~my_listener()
{

}

#ifdef USE_SDL2
Uint32 timer_cb(Uint32 interval, void *param)
{
	(void)param;
#else
Uint32 timer_cb(Uint32 interval)
{
#endif

	if (g_quit)
		return 1000;

	status_t status;
	int32_t pos;
	status = player->getCurrentPosition(&pos);
	if(status == OK)
		LOGI("---%d/%d---", pos / 1000, g_duration / 1000);
	
	return (1000/10)*10;
}

void my_listener::notify(int msg, int ext1, int ext2)
{
	//printf("notify %d %d %d\n", msg, ext1, ext2);

	if (MEDIA_PREPARED == msg) {
		SDL_Event event;
		SDL_zero(event);
		event.type = myOnPreparedEventType;
		event.user.code = 0;
		event.user.data1 = NULL;
		event.user.data2 = 0;
		SDL_PushEvent(&event);
	}
	else if (MEDIA_BUFFERING_UPDATE == msg) {
		//LOGD("position %d%%", ext1);
	}
	else if (MEDIA_PLAYBACK_COMPLETE == msg) {
		LOGI("MEDIA_PLAYBACK_COMPLETE");
		g_finished = 1;
	}
	else if (MEDIA_ERROR == msg) {
		LOGE("MEDIA_ERROR %d", ext1);
		g_finished = 1;
		player->stop();
	}
	else if (MEDIA_INFO == msg) {
		//LOGI("MEDIA_INFO ext1: %d, ext2: %d", ext1, ext2);
	}
	else if (MEDIA_SEEK_COMPLETE == msg) {
		LOGI("MEDIA_SEEK_COMPLETE");
	}
}

void start_player()
{
	LOGI("start_player()");
	printf("start_player()\n");

	status_t status;

	player->getDuration(&g_duration);
	status = player->getVideoWidth(&g_width);
	if (status != OK) {
		LOGE("failed to get width");
		return;
	}

	player->getVideoHeight(&g_height);
	LOGI("duration %d sec, %dx%d", g_duration / 1000, g_width, g_height);

	scr_width	= GetSystemMetrics ( SM_CXSCREEN );
	scr_height	= GetSystemMetrics ( SM_CYSCREEN );

#ifdef USE_SDL2
#ifdef SDL_EMBEDDED_WINDOW
	sdlWindow = SDL_CreateWindow("xxx", 200, 200, g_width, g_height, 0);
#else
	sdlWindow = SDL_CreateWindow("xxx", 0, 0, scr_width, scr_height, SDL_WINDOW_FULLSCREEN);
#endif

	sdlRender = SDL_CreateRenderer(sdlWindow, -1, SDL_RENDERER_ACCELERATED);
	sdlTexture = SDL_CreateTexture(sdlRender, SDL_PIXELFORMAT_ARGB8888, 
		SDL_TEXTUREACCESS_STREAMING, g_width, g_height);

	if (!sdlTexture) {
		printf("Couldn't set create texture: %s\n", SDL_GetError());
		return;
	}

	SDL_SetRenderTarget(sdlRender, sdlTexture);

	Surface_open3((void *)sdlWindow, (void *)sdlRender, (void *)sdlTexture);

	SDL_AddTimer(1000, timer_cb, NULL);
#else
	SDL_Surface* screen;
	screen = SDL_SetVideoMode(scr_width, scr_height, 32, 
		SDL_HWSURFACE | SDL_DOUBLEBUF | SDL_FULLSCREEN);
	player->setVideoSurface((void *)screen);
	Surface_open2((void *)screen);

	SDL_SetTimer(1000, timer_cb);
#endif

	player->start();
}

int main(int argc, char **argv)
{
	apMiniDump dump;
	dump.setdump("c:\\log\\player_vc.dmp");

	apLog::init("c:\\log\\player_vc.log");

	const char* uri = CLIP_NAME;
	if (argc > 1) {
		uri = argv[1];
		LOGI("input clip: %s", uri);
		printf("input clip: %s\n", uri);
	}
	else {
		LOGI("use default clip: %s", uri);
		printf("use default clip: %s\n", uri);
	}

	SDL_Init(SDL_INIT_VIDEO | SDL_INIT_TIMER);
	myOnPreparedEventType = SDL_RegisterEvents(1);
	if (myOnPreparedEventType == ((Uint32)-1)) {
		printf("failed to register sdl event");
		return 1;
	}

	status_t status;

	LOGI("begin to init player");

	my_listener listener;
	player = new FFPlayer;
	player->setListener(&listener);
	player->setDataSource(uri);
	player->prepareAsync();

	//start_player();

	LOGI("start messsage loop");

	//Our event structure
	SDL_Event e;
	bool paused = false;
	int32_t pos, duration;

	g_quit		= 0;
	g_finished	= 0;

	while (!g_quit){
		if (g_finished)
			break;

		while (SDL_PollEvent(&e)) { // would block!
			if (e.type == SDL_QUIT) {
				LOGI("click quit");
#ifdef USE_SDL2
				SDL_RemoveTimer(0);
#else
				SDL_SetTimer(0, NULL);
#endif
				g_quit = 1;
				break;
			}
			else if (e.type == myOnPreparedEventType) {
				start_player();
			}
			else if (e.type == SDL_KEYDOWN) {
				switch(e.key.keysym.sym) {
				case SDLK_SPACE:
					if(paused)
						player->start();
					else
						player->pause();
					paused = !paused;
					break;
				case SDLK_LEFT:
					status = player->getCurrentPosition(&pos);
					if(status == OK) {
						player->seekTo(pos - 10000); // -10 sec
					}
					break;
				case SDLK_RIGHT:
					status = player->getCurrentPosition(&pos);
					if(status == OK) {
						player->seekTo(pos + 10000); // +10 sec
					}
					break;
				case SDLK_ESCAPE:
					LOGI("escape quit");
#ifdef USE_SDL2
					SDL_RemoveTimer(0);
#else
					SDL_SetTimer(0, NULL);
#endif
					g_quit = 1;
					break;
				default:
					break;
				}
			}
			else if (e.type == SDL_MOUSEBUTTONDOWN) {
				Uint16 x = e.motion.x;
				LOGI("mouse x: %d", x);
				status = player->getDuration(&duration);
				if (status == OK) {
#ifdef SDL_EMBEDDED_WINDOW
					int w = g_width;
#else
					int w = scr_width;
#endif
					int32_t new_pos = (int32_t)((int64_t)duration * (int64_t)x / (int64_t)w);
					player->seekTo(new_pos);
					LOGI("seek %d/%d %d", x, w, duration);
				}
			}
		}

		SDL_Delay(40);
	}

	LOGI("call player stop");
	player->stop();
	LOGI("player stoped");
	delete player;
	player = NULL;

	return 0;
}