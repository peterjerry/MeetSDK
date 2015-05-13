// testPPTVlive.cpp : 定义控制台应用程序的入口点。
//

#include "stdafx.h"

#define inline _inline
#ifndef INT64_C
//#define INT64_C(c) (c ## LL)
//#define UINT64_C(c) (c ## ULL)
#define UINT64_C(val) val##ui64
#define INT64_C(val)  val##i64
#endif

extern "C"
{
#include "libavcodec\avcodec.h"
#include "libavformat\avformat.h"
#include "libswscale\swscale.h"
}

#pragma comment(lib,"avcodec.lib")
#pragma comment(lib,"avformat.lib")
#pragma comment(lib,"avutil.lib")
#pragma comment(lib,"swscale.lib")

int _tmain(int argc, _TCHAR* argv[])
{
	return 0;
}

