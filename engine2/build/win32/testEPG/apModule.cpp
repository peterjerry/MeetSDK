#include "stdafx.h"
#include "apModule.h"


apModule::apModule(int index, const char* title)
{
	mIndex		= index;
	mTitle		= title;
	mTarget		= "";
	mLink		= "";
}

apModule::apModule(int index, const char* title, const char* target, const char* link)
{
	mIndex		= index;
	mTitle		= title;
	mTarget		= target;
	mLink		= link;
}

apModule::~apModule(void)
{
}
