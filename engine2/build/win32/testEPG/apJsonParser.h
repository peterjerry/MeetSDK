#pragma once
#include "common.h"

class apJsonParser;

class apJsonParser
{
public:
	apJsonParser(void);
	~apJsonParser(void);

	typedef std::map<std::string, std::string> MAP_ITEM;

	EPG_MODULE_LIST * parseFrontpage(char *context, unsigned int size);

	EPG_MODULE_LIST * get_module(){return &mModuleList;}

	EPG_CATALOG_LIST * parseCatalog(char *context, unsigned int size, int index);

	EPG_CATALOG_LIST * get_catalog(){return &mCatalogList;}

	EPG_PLAYLINK_LIST * parseDetail(char *context, unsigned int size);
private:
	EPG_MODULE_LIST		mModuleList;
	EPG_CATALOG_LIST	mCatalogList;
};

