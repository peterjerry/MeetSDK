#ifndef _OMX_MEDIA_PLAYER_H
#define _OMX_MEDIA_PLAYER_H

#include <jni.h>

bool setup_omxplayer(void *so_handle);

bool init_omxplayer(JNIEnv *env);

#endif // _OMX_MEDIA_PLAYER_H