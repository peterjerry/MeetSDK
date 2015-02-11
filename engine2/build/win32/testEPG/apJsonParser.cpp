#include "stdafx.h"
#include "apJsonParser.h"
#define LOG_TAG "apJsonParser"
#include "log.h"

#include <json/json.h>
using namespace Json;

#ifdef _DEBUG
#pragma comment(lib, "libjson_mdd.lib")
#else
#pragma comment(lib, "libjson_md.lib")
#endif

apJsonParser::apJsonParser(void)
{
}


apJsonParser::~apJsonParser(void)
{
}

EPG_LIST * apJsonParser::parseCatalog(char *context, unsigned int size)
{
	mCatalog.clear();

	Value  v;
	Reader r;
	if(!r.parse(context, v, false))
		return NULL;

	const int len = strlen("&vid=");

	int c = v["modules"].size();
	for (int i=0;i<c;i++) {
		std::string title = v["modules"][i]["data"]["title"].asString();
		if (!title.empty()) {
			char str_index[8] = {0};
			sprintf_s(str_index, "%d", i);

			MAP_ITEM new_item;
			new_item.insert(MAP_ITEM::value_type("index", str_index));
			new_item.insert(MAP_ITEM::value_type("title", title));
			mCatalog.push_back(new_item);
		}
	}

	return &mCatalog;
}

EPG_LIST * apJsonParser::parseCollection(char *context, unsigned int size, int index)
{
	LOGI("parseCollection() index %d", index);

	mCollection.clear();

	Value  v;
	Reader r;
	if (!r.parse(context, v, false))
		return NULL;

	const int len = strlen("&vid=");

	int c = v["modules"].size();
	if (index >= c)
		return NULL;

	int c2 = v["modules"][index]["data"]["dlist"].size();
	for (int j=0;j<c2;j++) {
		std::string title = v["modules"][index]["data"]["dlist"][j]["title"].asString();
		std::string str_link = v["modules"][index]["data"]["dlist"][j]["link"].asString();
		std::string link = v["modules"][index]["data"]["dlist"][j]["epg_id"].asString();
		if(link.empty()) {
			int p1, p2;
			p1 = str_link.find("&vid=");
			p2 = str_link.find("&sid=");
			if (std::string::npos == p1 || std::string::npos == p2)
				continue;

			link = str_link.substr(p1 + len, p2 - p1 - len);
			
		}
		MAP_ITEM new_item;
		new_item.insert(MAP_ITEM::value_type("title", title));
		new_item.insert(MAP_ITEM::value_type("link", link));
		mCollection.push_back(new_item);
	}

	return &mCollection;
}

EPG_LIST * apJsonParser::parsePlaylink(char *context, unsigned int size)
{
	mClips.clear();

	Value  v;
	Reader r;
	if(!r.parse(context, v, false))
		return NULL;

	const int len = strlen("&vid=");

	int c = v["modules"].size();
	for (int i=0;i<c;i++) {
		int c2 = v["modules"][i]["data"]["dlist"].size();
		for (int j=0;j<c2;j++) {
			std::string title = v["modules"][i]["data"]["dlist"][j]["title"].asString();
			//printf("title %s\n", title.c_str());
			std::string str_link = v["modules"][i]["data"]["dlist"][j]["link"].asString();
			int p1, p2;
			p1 = str_link.find("&vid=");
			p2 = str_link.find("&sid=");
			if (std::string::npos == p1 || std::string::npos == p2)
				continue;

			std::string link = str_link.substr(p1 + len, p2 - p1 - len);
			printf("link %s\n", link.c_str());
			MAP_ITEM new_item;
			new_item.insert(MAP_ITEM::value_type("title", title));
			new_item.insert(MAP_ITEM::value_type("link", link));
			mClips.push_back(new_item);
		}
	}

	return &mClips;
}