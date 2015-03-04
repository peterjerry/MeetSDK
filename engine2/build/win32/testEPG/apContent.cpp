#include "stdafx.h"
#include "apContent.h"


apContent::apContent(const char * id, const char * title, const char * param, const char * pos)
{
	mId			= id;
	mTitle		= title;
	mParam		= param;
	mPosition	= pos;
}


apContent::~apContent(void)
{
}
