#pragma once
#include "apJsonParser.h"
#include "apXmlParser.h"

class apEPG
{
public:
	apEPG(void);
	~apEPG(void);

	EPG_LIST * getCatalog(int index);

	EPG_LIST * getPlaylink(int index);

private:
	void reset();

	static size_t write_data(void *buffer, size_t size, size_t nmemb, void *userp);

	size_t write_data_impl(void *buffer, size_t size, size_t nmemb, void *userp);
private:
	char *mData;
	unsigned int mDataSize;
	apJsonParser mParser;
	apXmlParser mParserXml;
};

