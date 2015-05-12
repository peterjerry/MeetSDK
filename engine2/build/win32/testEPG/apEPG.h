#pragma once
#include "apJsonParser.h"
#include "apXmlParser.h"

class apEPG
{
public:
	apEPG(void);
	~apEPG(void);

	EPG_MODULE_LIST * frontpage();

	EPG_MODULE_LIST * get_module(){return mParser.get_module();}

	EPG_CATALOG_LIST * catalog(int catalog_index);

	EPG_CATALOG_LIST * get_catalog(){return mParser.get_catalog();}

	EPG_PLAYLINK_LIST * detail(int vid);

	EPG_PLAYLINK_LIST * get_playlink(){return mParserXml.get_playlink();}

	bool live_cdn(int vid);

	bool search(const char* key, EPG_NAVIGATOR_LIST **pNav, EPG_PLAYLINK_LIST **pPlaylink);

private:
	void reset();

	static size_t write_data(void *buffer, size_t size, size_t nmemb, void *userp);

	size_t write_data_impl(void *buffer, size_t size, size_t nmemb, void *userp);
private:
	char *mData;
	unsigned int			mDataSize;
	apJsonParser			mParser;
	apXmlParser				mParserXml;
	void*					mCurl;
};

