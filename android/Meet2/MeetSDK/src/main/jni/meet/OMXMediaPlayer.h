#ifndef _OMX_MEDIA_PLAYER_H
#define _OMX_MEDIA_PLAYER_H
#include "player/player.h"
#include <jni.h>

bool setup_omxplayer(void *so_handle);

bool init_omxplayer(JNIEnv *env);

int register_android_media_omxplayer(JNIEnv *env);

class OMXMediaPlayerListener: public MediaPlayerListener
{
	public:
		OMXMediaPlayerListener(JNIEnv* env, jobject thiz, jobject weak_thiz);
		~OMXMediaPlayerListener();
		void notify(int msg, int ext1, int ext2);
	private:
		OMXMediaPlayerListener();
		jclass      mClass;     // Reference to MediaPlayer class
		jobject     mObject;    // Weak ref to MediaPlayer Java object to call on
};

#endif // _OMX_MEDIA_PLAYER_H