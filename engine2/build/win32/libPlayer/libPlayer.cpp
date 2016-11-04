// libPlayer.cpp : 定义 DLL 应用程序的导出函数。
//

#include "stdafx.h"
#include "libPlayer.h"
#include "surface.h"

// 这是导出变量的一个示例
LIBPLAYER_API int nlibPlayer=0;

// 这是导出函数的一个示例。
LIBPLAYER_API IPlayer* able_getPlayer(void)
{
	return getPlayer(NULL);
}

LIBPLAYER_API void able_releasePlayer(IPlayer *player)
{
	releasePlayer(player);
}

LIBPLAYER_API bool able_surface_open(void* context)
{
	return (Surface_open2(context) == OK);
}

LIBPLAYER_API void able_set_display_resolution(int w, int h)
{
	g_max_display_w = w;
	g_max_display_h = h;
}

// 这是已导出类的构造函数。
// 有关类定义的信息，请参阅 libPlayer.h
ClibPlayer::ClibPlayer()
{
	return;
}
