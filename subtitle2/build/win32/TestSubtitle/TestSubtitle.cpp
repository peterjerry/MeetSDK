// TestSubtitle.cpp : 定义控制台应用程序的入口点。
//

#include "stdafx.h"

#include <atlbase.h>
#include <atlconv.h>
#include "subtitle.h"

#pragma comment(lib, "libass")
#pragma comment(lib, "pthreadVC2")

#define SUB_FILE_PATH "E:\\QQDownload\\Manhattan.S01E08.720p.HDTV.x264-KILLERS\\Manhattan.S01E08.720p.HDTV.x264-KILLERS.简体.srt"

int _tmain(int argc, _TCHAR* argv[])
{
    //_CrtSetDbgFlag( _CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF);

    ISubtitles* subtitle = NULL;
    if (!ISubtitles::create(&subtitle)) {
		printf("failed to create subtitle instance.\n");
        return 1;
    }
    
	if (!subtitle->loadSubtitle(SUB_FILE_PATH, false)) {
		printf("failed to load subtitle: %s", SUB_FILE_PATH);
		return 1;
	}

    if (!subtitle->seekTo(0)) {
		printf("failed to seekTo subtitle");
		return 1;
	}

    STSSegment* segment = NULL;
	char subtitleText[1024] = {0};

	int line = 0;
    while(line < 20 && subtitle->getNextSubtitleSegment(&segment)) {
        int64_t startTime = segment->getStartTime();
        int64_t stopTime = segment->getStopTime();
        printf("%01d:%02d:%02d.%02d  --> %01d:%02d:%02d.%02d  ",
            int(startTime/1000/3600), int(startTime/1000%3600/60), int(startTime/1000%60), int(startTime%1000)/10,
            int(stopTime/1000/3600), int(stopTime/1000%3600/60), int(stopTime/1000%60), int(stopTime%1000)/10);

        segment->getSubtitleText(subtitleText, 1024);
        printf("%s\n", CW2A(CA2W(subtitleText, CP_UTF8)));
		getchar();
		line++;
    }

    subtitle->close();

	return 0;
}

