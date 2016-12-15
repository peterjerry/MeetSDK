// libPlayer.cpp : ���� DLL Ӧ�ó���ĵ���������
//

#include "stdafx.h"
#include "libPlayer.h"
#include "surface.h"

// ���ǵ���������һ��ʾ��
LIBPLAYER_API int nlibPlayer=0;

// ���ǵ���������һ��ʾ����
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

// �����ѵ�����Ĺ��캯����
// �й��ඨ�����Ϣ������� libPlayer.h
ClibPlayer::ClibPlayer()
{
	return;
}
