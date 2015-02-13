#pragma once
#include "common.h"

class apXmlParser
{
public:
	apXmlParser(void);
	~apXmlParser(void);

	EPG_LIST * parsePlaylink(char *context, unsigned int size);

	EPG_LIST * parseSearch(char *context, unsigned int size);

private:
	EPG_LIST mClips;
};

