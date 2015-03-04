#include "stdafx.h"
#include "apPlayLink2.h"

apPlayLink2::apPlayLink2(const char * title, const char * id, const char * desc)
{
	mTitle			= title;
	mExtTitle		= "";
	mId				= id;
	mDescription	= desc;

	mMark			= "";
	mDirector		= "";
	mAct			= "";
	mYear			= "";
	mArea			= "";

	mResolution		= "";
	mDurationSec	= 0;

	setResolution();
}

apPlayLink2::apPlayLink2(const char * title, const char * ext_title, const char * id, const char * desc, 
			const char * mark, const char * director, const char * act, 
			const char * year, const char * area,
			const char * resolution, int duration_sec)
{
	mTitle			= title;
	mExtTitle		= ext_title;
	mId				= id;
	mDescription	= desc;

	mMark			= mark;
	mDirector		= director;
	mAct			= act;
	mYear			= year;
	mArea			= area;

	mResolution		= resolution;
	mDurationSec	= duration_sec;

	setResolution();
}


apPlayLink2::~apPlayLink2(void)
{
}

const char * apPlayLink2::get_title()
{
	mFinalTitle = mTitle;

	if (!mExtTitle.empty()) {
		mFinalTitle += "(";
		mFinalTitle += mExtTitle;
		mFinalTitle += ")";
	}
		
	return mFinalTitle.c_str();
}

void apPlayLink2::setResolution()
{
	if (mResolution.empty())
		return;
		
    size_t pos;
    pos = mResolution.find('|');
	if (pos == std::string::npos) {
    	mWidth = mHeight = 0;
    }
    else {
		mWidth	= atoi(mResolution.substr(0, pos).c_str());
		mHeight = atoi(mResolution.substr(pos + 1, mResolution.length() - pos - 1).c_str());
    }
}