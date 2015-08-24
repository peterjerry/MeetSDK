#ifndef _FF_MEDIA_PLAYER_H
#define _FF_MEDIA_PLAYER_H
#include "player/player.h"
#include <jni.h>

typedef bool (*CONVERT_FUN) (uint8_t* , int , uint8_t* , int *, int, int);

// This function only registers the native methods
int register_android_media_MediaPlayer(JNIEnv *env);

void unload_player();

class FFMediaPlayerListener: public MediaPlayerListener
{
	public:
		FFMediaPlayerListener(JNIEnv* env, jobject thiz, jobject weak_thiz);
		~FFMediaPlayerListener();
		void notify(int msg, int ext1, int ext2);
	private:
		FFMediaPlayerListener();
		jclass      mClass;     // Reference to MediaPlayer class
		jobject     mObject;    // Weak ref to MediaPlayer Java object to call on
};

#endif // _FF_MEDIA_PLAYER_H