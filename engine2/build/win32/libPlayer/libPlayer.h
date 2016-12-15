// ���� ifdef ���Ǵ���ʹ�� DLL �������򵥵�
// ��ı�׼�������� DLL �е������ļ��������������϶���� LIBPLAYER_EXPORTS
// ���ű���ġ���ʹ�ô� DLL ��
// �κ�������Ŀ�ϲ�Ӧ����˷��š�������Դ�ļ��а������ļ����κ�������Ŀ���Ὣ
// LIBPLAYER_API ������Ϊ�Ǵ� DLL ����ģ����� DLL ���ô˺궨���
// ������Ϊ�Ǳ������ġ�
#ifdef LIBPLAYER_EXPORTS
#define LIBPLAYER_API __declspec(dllexport)
#else
#define LIBPLAYER_API __declspec(dllimport)
#endif

// �����Ǵ� libPlayer.dll ������
class LIBPLAYER_API ClibPlayer {
public:
	ClibPlayer(void);
	// TODO: �ڴ�������ķ�����
};

extern LIBPLAYER_API int nlibPlayer;

LIBPLAYER_API IPlayer* able_getPlayer(void);

LIBPLAYER_API void able_releasePlayer(IPlayer *player);

LIBPLAYER_API bool able_surface_open(void* context);

LIBPLAYER_API void able_set_display_resolution(int w, int h);
