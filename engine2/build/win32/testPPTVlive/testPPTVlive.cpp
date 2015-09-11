// testPPTVlive.cpp : 定义控制台应用程序的入口点。
//

#include "stdafx.h"
#include "apEPG.h"
#include "apCDNItem.h"
#include "apKey.h"
#include "strptime.h"
#include "apBlockDownloader.h"
#include "apFlvDemuxer.h"
#include "apTsWriter.h"
#include "apFormatConverter.h"
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
#include "libavutil\time.h"
#include "libavcodec\avcodec.h"
#include "libavformat\avformat.h"
#include "libswscale\swscale.h"
}

#pragma comment(lib,"avcodec.lib")
#pragma comment(lib,"avformat.lib")
#pragma comment(lib,"avutil.lib")
#pragma comment(lib,"swscale.lib")

#define LIVE_PLAYLINK 300355
#define LIVE_URL_FMT "http://%s/live/%s/" \
							"%I64d.block?ft=1&platform=android3" \
							"&type=phone.android.vip&sdk=1" \
							"&channel=162&vvid=41&k=%s"

#define OUT_TS_MAX_SIZE 1048576
apTsWriter dumper;


int onFrame(AVPacket *pkt)
{
	if (!dumper.write_frame(pkt))
		return -1;

	return 0;
}

int _tmain(int argc, _TCHAR* argv[])
{
	apLog::init("c:\\log\\testPPTVlive.log");

	apEPG epg;
	apCDNItem *item = epg.get_live_cdn_url(LIVE_PLAYLINK);
	if (!item) {
		LOGE("failed to get live cdn url");
		return 1;
	}

	tm utc_tm;
	strptime(item->get_st(), "%a %b %d %H:%M:%S %Y %Z", &utc_tm);
	time_t t = _mkgmtime(&utc_tm);
	LOGI("sh %s, bh %s, st %s, key %s, time %I64d sec", 
		item->get_sh(), item->get_bh(), item->get_st(), item->get_key(), t);
	uint8_t * key = apKey::genKey(t);

	int64_t segment_time = t - 45; // unit second
	segment_time -= (segment_time % 5);

	char url[1024]= {0};
	char save_filename[64] = {0};

	bool dumper_opened = false;

	uint8_t *out_ts = new uint8_t[OUT_TS_MAX_SIZE];
	memset(out_ts, 0, OUT_TS_MAX_SIZE);

	for (int i=0;i<10;i++) {  
		sprintf(url, LIVE_URL_FMT, item->get_sh(), item->get_rid(), segment_time, item->get_key());

		LOGI("ready to download segment: %s", url);

		apBlockDownloader downloader(url);
		char save_filename[64] = {0};
		sprintf(save_filename, "d:\\dump\\%I64d.flv", segment_time);
		
		/*if (!downloader.saveAs(save_filename)) {
			LOGE("failed to download segment %s", url);
			break;
		}

		LOGI("segment %I64d.block downloaded as %s", segment_time, save_filename);
		printf("segment %I64d.block downloaded as %s\n", segment_time, save_filename);*/
		
		
		if (!downloader.saveInMemory()) {
			LOGE("failed to download segment %s", url);
			break;
		}

		int size;
		uint8_t * flv_data = (uint8_t *)downloader.getData(&size);
		if (flv_data == NULL) {
			LOGE("failed to get flv data");
			break;
		}

		LOGI("segment %I64d.block downloaded %p, size %d", segment_time, flv_data, size);
		printf("segment %I64d.block downloaded %p, size %d\n", segment_time, flv_data, size);

		apFormatConverter converter;
		int out_ts_len = OUT_TS_MAX_SIZE;
		if (!converter.convert(flv_data, size, out_ts, &out_ts_len, 0, 0)) {
			LOGE("failed to convert");
			printf("failed to convert");
			break;
		}

		sprintf(save_filename, "d:\\dump\\%I64d.ts", segment_time);
		FILE *pFile = fopen(save_filename, "wb");
		if (pFile) {
			fwrite(out_ts, 1, out_ts_len, pFile);
			fclose(pFile);
			printf("save ts file %s: size %d\n", save_filename, out_ts_len);
			pFile = NULL;
		}

		/*apFlvDemuxer demux;

		demux.setOnFrame(onFrame);
		if (!demux.setSource(data, size)) {
			LOGE("failed to open flv");
			printf("failed to open flv\n");
			break;
		}

		if (!dumper_opened) {
			if (!dumper.open(demux.getFmtCtx(), "udp://127.0.0.1:9981/1.ts")) {
				LOGE("failed to open output ts file");
				break;
			}

			dumper_opened = true;
		}

		demux.demux();
		*/

		segment_time += 5;
		Sleep(500);
	}

	//dumper.close();

	return 0;
}

