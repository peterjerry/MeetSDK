// testPPTVlive.cpp : 定义控制台应用程序的入口点。
//

#include "stdafx.h"
#include "apEPG.h"
#include "apCDNItem.h"
#include "apKey.h"
#include "strptime.h"
#include "apBlockDownloader.h"
#define LOG_TAG "testPPTVLive"
#include "log.h"
#include "apFileLog.h"

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

#define LIVE_URL_FMT "http://%s/live/074094e6c24c4ebbb4bf6a82f4ceabda/" \
							"%I64d.block?ft=1&platform=android3" \
							"&type=phone.android.vip&sdk=1" \
							"&channel=162&vvid=41&k=%s"

int _tmain(int argc, _TCHAR* argv[])
{
	apLog::init("c:\\log\\testPPTVlive.log");

	apEPG epg;
	apCDNItem *item = epg.get_live_cdn_url(300151);
	if (!item) {
		LOGE("failed to get live cdn url");
		return 1;
	}

	tm utc_tm;
	strptime(item->get_st(), "%a %b %d %H:%M:%S %Y %Z", &utc_tm);
	time_t t = _mkgmtime(&utc_tm);
	LOGI("sh %s, bh %s, st %s, time %I64d sec", 
		item->get_sh(), item->get_bh(), item->get_st(), t);
	uint8_t * key = apKey::getKey(t);

	int64_t segment_time = t - 45;
	segment_time -= (segment_time % 5);

	char url[1024]= {0};
	char save_filename[64] = {0};

	for (int i=0;i<10;i++) {  
		sprintf(url, LIVE_URL_FMT, item->get_sh(), segment_time, (char *)key);

		LOGI("ready to download segment: %s", url);

		apBlockDownloader downloader(url);
		char save_filename[64] = {0};
		sprintf(save_filename, "d:\\dump\\%I64d.flv", segment_time);
		if (!downloader.saveAs(save_filename)) {
			LOGE("failed to download segment %s", url);
			break;
		}

		LOGI("segment %I64d.block downloaded as %s", segment_time, save_filename);
		printf("segment %I64d.block downloaded as %s\n", segment_time, save_filename);

		segment_time += 5;
	}

	return 0;
}

