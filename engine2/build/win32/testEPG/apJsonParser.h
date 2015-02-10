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

	EPG_LIST * parsePlaylink(char *context, unsigned int size);
private:
	std::list<MAP_ITEM> mCatalog;
	std::list<MAP_ITEM> mClips;
};

