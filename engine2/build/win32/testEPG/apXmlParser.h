#pragma once
#include "common.h"
#include "markup.h"

class apXmlParser
{
public:
	apXmlParser(void);
	~apXmlParser(void);

	EPG_PLAYLINK_LIST * parseDetail(char *context, unsigned int size);

	EPG_PLAYLINK_LIST * get_playlink(){return &mPlaylinkList;}

	bool parseSearch(char *context, unsigned int size);

	bool parseCDN(char *context, unsigned int size);

	EPG_NAVIGATOR_LIST * get_nav(){return &mNavigatorList;}
private:
	boolean add_v(CMarkup v);

private:
	EPG_NAVIGATOR_LIST	mNavigatorList;
	EPG_PLAYLINK_LIST	mPlaylinkList;
};

