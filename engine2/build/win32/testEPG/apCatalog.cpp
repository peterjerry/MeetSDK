#include "stdafx.h"
#include "apCatalog.h"


apCatalog::apCatalog(const char *title, const char *target, const char *link)
{
	mTitle	= title;
	mTarget	= target;
	mLink	= link;
}


apCatalog::~apCatalog(void)
{
}

int apCatalog::get_vid()
{
	if (mLink.empty())
		return NULL;
		
	size_t pos1, pos2;
	pos1 = mLink.find("&vid=");
	pos2 = mLink.find("&sid=");
	if (std::string::npos == pos1 || std::string::npos == pos2)
		return NULL;

	int count = pos2 - (pos1 + 5);
	std::string vid = mLink.substr(pos1 + 5, count);
	return atoi(vid.c_str());
}