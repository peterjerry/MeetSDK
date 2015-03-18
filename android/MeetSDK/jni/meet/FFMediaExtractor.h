#ifndef _FF_MEDIA_EXTRACTOR_H
#define _FF_MEDIA_EXTRACTOR_H

#include "player/player.h"
#include <jni.h>

// This function only registers the native methods
int register_android_media_MediaExtractor(JNIEnv *env);

bool setup_extractor(void *so_handle);

class XOMediaPlayerListener: public MediaPlayerListener
{
	public:
		XOMediaPlayerListener(JNIEnv* env, jobject thiz, jobject weak_thiz);
		~XOMediaPlayerListener();
		void notify(int msg, int ext1, int ext2);
	private:
		XOMediaPlayerListener();
		jclass      mClass;     // Reference to MediaPlayer class
		jobject     mObject;    // Weak ref to MediaPlayer Java object to call on
};

#endif // _FF_MEDIA_EXTRACTOR_H