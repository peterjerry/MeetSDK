#pragma once
#include "common.h"

class apJsonParser;

class apJsonParser
{
public:
	apJsonParser(void);
	~apJsonParser(void);

	typedef std::map<std::string, std::string> MAP_ITEM;

	EPG_LIST * parseCatalog(char *context, unsigned int size);

	EPG_LIST * parseCollection(char *context, unsigned int size, int index);

	EPG_LIST * parsePlaylink(char *context, unsigned int size);
private:
	EPG_LIST mCatalog;
	EPG_LIST mCollection;
	EPG_LIST mClips;
};

