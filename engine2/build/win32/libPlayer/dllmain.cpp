// dllmain.cpp : 定义 DLL 应用程序的入口点。
#include "stdafx.h"
#include "apFileLog.h"

BOOL APIENTRY DllMain( HMODULE hModule,
                       DWORD  ul_reason_for_call,
                       LPVOID lpReserved
					 )
{
#ifdef SAVE_LOG_FILE
	apLog::init("c:\\log\\libplayer.log");
#endif

	switch (ul_reason_for_call)
	{
	case DLL_PROCESS_ATTACH:
	case DLL_THREAD_ATTACH:
	case DLL_THREAD_DETACH:
	case DLL_PROCESS_DETACH:
		break;
	}
	return TRUE;
}

