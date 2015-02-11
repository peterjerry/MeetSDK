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
	EPG_LIST *catalog = epg.getCatalog(-1);
	if (!catalog) {
		_tprintf(_T("failed to get catalog\n")); // tchar.h compatible with ansi and unicode
		return 1;
	}

	int link_id;
	std::string id = "";
	int done = 0;

	EPG_LIST::iterator it = catalog->begin();
	for (;it != catalog->end() && !done;it++) {
		MAP_ITEM::iterator it_map = (*it).begin();
		for (;it_map != (*it).end() && !done;it_map++) {
			_tprintf(_T("Item: %s, Value: %s\n"), (*it_map).first.c_str(), (*it_map).second.c_str());
			if (it_map == (*it).begin() && (*it_map).first.find("link") != std::string::npos) {
				link_id = atoi((*it_map).second.c_str());
				catalog = epg.getPlaylink(link_id);
				if (catalog) {
					MAP_ITEM item = catalog->at(0);
					MAP_ITEM::iterator it_clip = item.begin();
					for (;it_clip != item.end();it_clip++) {
						if((*it_clip).first.find("id") != std::string::npos) {
							id = (*it_clip).second.c_str();
							_tprintf(_T("playlink id: %s\n"), id.c_str());
						}
						else {
							_tprintf(_T("item: %s, value: %s\n"), (*it_clip).first.c_str(), (*it_clip).second.c_str());
						}
					}
					done = 1;
					break;
				}
				
			}
		}
		_tprintf(_T("\n"));
	}

	

	/*char buf[65536] = {0};
	FILE *pFile = NULL;
	if (fopen_s(&pFile, "frontpage\\module.json", "rb") != 0) {
		printf(_T("open file error\n"));
		return 1;
	}

	size_t readed = fread(buf, 1, 65536, pFile);
	fclose(pFile);

	apJsonParser json;
	json.parseContext(buf, readed);*/

	return 0;
}

