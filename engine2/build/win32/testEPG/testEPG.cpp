// testEPG.cpp : 定义控制台应用程序的入口点。
//

#include "stdafx.h"
#include "apFileLog.h"
#include "apEPG.h"
#include "apJsonParser.h"

int _tmain(int argc, _TCHAR* argv[])
{
	apLog::init(_T("c:\\log\\testEPG.log"));
	apEPG epg;

	EPG_MODULE_LIST *modulelist = epg.frontpage();
	if (!modulelist) {
		_tprintf(_T("failed to get frontpage\n")); // tchar.h compatible with ansi and unicode
		return 1;
	}

	int size = modulelist->size();
	for (int i=0;i<size;i++) {
		_tprintf(_T("#%d title: %s\n"), (*modulelist)[i].get_index(), (*modulelist)[i].get_title());
	}

	int index;
	printf("input index\n");
	scanf_s("%d", &index);

	printf("before epg.catalog\n");
	EPG_CATALOG_LIST *catloglist = epg.catalog(index);
	printf("after epg.catalog\n");
	if (!catloglist) {
		_tprintf(_T("failed to get catalog\n")); // tchar.h compatible with ansi and unicode
		return 1;
	}

	size = catloglist->size();
	for (int i=0;i<size;i++) {
		_tprintf(_T("#%d title: %s , vid: %d\n"), i, (*catloglist)[i].get_title(), (*catloglist)[i].get_vid());
	}

	printf("input playlink index\n");
	scanf_s("%d", &index);

	int vid = (*catloglist)[index].get_vid();
	EPG_PLAYLINK_LIST *playlinklist = epg.detail(vid);
	if (!catloglist) {
		_tprintf(_T("failed to get detail\n")); // tchar.h compatible with ansi and unicode
		return 1;
	}

	size = playlinklist->size();
	for (int i=0;i<size;i++) {
		_tprintf(_T("#%d vid: %d, title: %s, %d x %d, %d min\n"), 
			i, 
			(*playlinklist)[i].get_id(), 
			(*playlinklist)[i].get_title(), 
			(*playlinklist)[i].get_width(), (*playlinklist)[i].get_height(),
			(*playlinklist)[i].get_duration() / 60,
			(*playlinklist)[i].get_description());
	}

	//_tprintf(_T("playlink id: %s\n"), id.c_str());
						

	return 0;
}

