#pragma once
#include "common.h"
#include "markup.h"

class apCDNItem;

class apXmlParser
{
public:
	apXmlParser(void);
	~apXmlParser(void);

	EPG_PLAYLINK_LIST * parseDetail(char *context, unsigned int size);

	EPG_PLAYLINK_LIST * get_playlink(){return &mPlaylinkList;}

	bool parseSearch(char *context, unsigned int size);

	char * parseCDN(char *context, unsigned int size, int ft, bool is_m3u8, bool novideo);

	apCDNItem * parseLiveCDN(char *context, unsigned int size);

	EPG_NAVIGATOR_LIST * get_nav(){return &mNavigatorList;}
private:
	bool add_v(CMarkup v);

private:
	EPG_NAVIGATOR_LIST	mNavigatorList;
	EPG_PLAYLINK_LIST	mPlaylinkList;
};

