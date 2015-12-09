#ifndef _FF_MEDIA_PLAYER_H
#define _FF_MEDIA_PLAYER_H
#include "player/player.h"
#include <jni.h>

#ifdef USE_TS_CONVERT
typedef bool (*CONVERT_FUN) (uint8_t* , int , uint8_t* , int *, int, int);
extern CONVERT_FUN convertFun;
#endif

// This function only registers the native methods
int register_android_media_MediaPlayer(JNIEnv *env);

bool setup_player(void *so_handle);

class JNIMediaPlayerListener: public MediaPlayerListener
{
	public:
		JNIMediaPlayerListener(JNIEnv* env, jobject thiz, jobject weak_thiz);
		~JNIMediaPlayerListener();
		void notify(int msg, int ext1, int ext2);
	private:
		JNIMediaPlayerListener();
		jclass      mClass;     // Reference to MediaPlayer class
		jobject     mObject;    // Weak ref to MediaPlayer Java object to call on
};

#endif // _FF_MEDIA_PLAYER_H