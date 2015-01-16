#include <stdio.h>
#include <string.h>
#ifndef _MSC_VER
#include <unistd.h>
#endif

#include "ffplayer.h"
#include "surface.h"
#define LOG_TAG "testapp"
#include "log.h"
#include "apminidump.h"
//#include "picqueue.h"

#include "sdl.h"
#undef main

#ifdef _MSC_VER
#pragma comment(lib, "sdlmain")
#pragma comment(lib, "sdl")
#endif

#ifdef _MSC_VER
#define CLIP_NAME "e:\\Work\\HEVC\\Transformers3-720p.mp4"
#else
#define CLIP_NAME "e:/Work/HEVC/Transformers3-720p.mp4"
#endif

FFPlayer *player	= NULL;
int g_finished		= 0;
int g_quit			= 0;
int32_t g_width, g_height;
int32_t g_duration;

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

Uint32 timer_cb(Uint32 interval)
{
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
	if (MEDIA_BUFFERING_UPDATE == msg) {
		//LOGD("position %d%%", ext1);
	}
	else if (MEDIA_PLAYBACK_COMPLETE == msg) {
		LOGI("MEDIA_PLAYBACK_COMPLETE");
		g_finished = 1;
	}
	else if (MEDIA_ERROR == msg) {
		LOGE("MEDIA_ERROR %d", ext1);
		g_finished = 1;
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
	status_t status;
	player->getDuration(&g_duration);
	status = player->getVideoWidth(&g_width);
	if (status != OK) {
		LOGE("failed to get width");
		return;
	}

	player->getVideoHeight(&g_height);
	LOGI("duration %d sec, %dx%d", g_duration / 1000, g_width, g_height);

	SDL_Init(SDL_INIT_EVERYTHING);
	SDL_SetTimer((1000/10)*10, timer_cb);
	
	SDL_Surface* screen;
	screen = SDL_SetVideoMode(g_width, g_height, 32, 
		SDL_HWSURFACE | SDL_DOUBLEBUF/* | SDL_RESIZABLE*/);

	Surface_open2((void *)screen);

	player->start();
}

struct PicData {
	int index;
	float num;
};

int main(int argc, char **argv)
{
	/*pthread_cond_t cond;
	pthread_mutex_t mLock;
	pthread_mutex_init(&mLock, NULL);
	pthread_cond_init(&cond, NULL);

	for (int i=0;i<5;i++) {
		LOGI("befre");
		pthread_mutex_lock(&mLock);
		timespec ts;
		ts.tv_sec	= time(NULL) + 3;
		ts.tv_nsec	= 0;
		int32_t err = pthread_cond_timedwait(&cond, &mLock, &ts);
		pthread_mutex_unlock(&mLock);
		LOGI("end %d", err);
	}

	LOGI("%d", ETIMEDOUT);*/

	/*PicQueue q1(8);
	PicData d1[2] = {0};
	d1[0].index = 1;
	d1[0].num = 2.0f;
	d1[1].index = 2;
	d1[1].num = 3.0f;
	q1.QueuePic((void *)&d1[0], 0);
	q1.QueuePic((void *)&d1[1], 0);

	PicData *res;
	while(1) {
		res = (PicData *)q1.DequeuePic(0);
		if (res == NULL)
			break;

		LOGI("data index %d, num %.2f", res->index, res->num);
	}

	return 0;*/

	apMiniDump dump;
	dump.setdump("c:\\log\\libplayer.dmp");

	const char* uri = NULL;
	if (argc > 1) {
		uri = argv[1];
		printf("input clip: %s\n", uri);
	}
	else {
		uri = CLIP_NAME;
		printf("use default clip: %s\n", uri);
	}

	status_t status;
	my_listener listener;
	player = new FFPlayer;
	player->setListener(&listener);
	player->setDataSource(uri);
	player->prepare();

	start_player();

	//Our event structure
	SDL_Event e;
	bool paused = false;
	int32_t pos, duration;

	g_quit = 0;

	while (!g_quit){
		if (g_finished)
			break;

		while (SDL_PollEvent(&e)) { // would block!
			if (e.type == SDL_QUIT) {
				LOGI("click quit");
				SDL_SetTimer(0, NULL);
				g_quit = 1;
				break;
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
				default:
					break;
				}
			}
			else if (e.type == SDL_MOUSEBUTTONDOWN) {
				Uint16 x = e.motion.x;
				LOGI("mouse x: %d", x);
				status = player->getDuration(&duration);
				if (status == OK) {
					int32_t new_pos;
					new_pos = (int32_t)((int64_t)duration * (int64_t)x / (int64_t)g_width);
					player->seekTo(new_pos);
					LOGI("seek %d/%d %d", x, g_width, duration);
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