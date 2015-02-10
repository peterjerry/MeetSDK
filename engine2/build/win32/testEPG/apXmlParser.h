#pragma once
#include "common.h"

class apXmlParser
{
public:
	apXmlParser(void);
	~apXmlParser(void);

	MAP_ITEM * parsePlaylink(char *context, unsigned int size);

private:
	std::list<MAP_ITEM> mClips;
};

