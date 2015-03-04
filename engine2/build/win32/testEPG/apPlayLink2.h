#pragma once
#include <string>

class apPlayLink2
{
private:
	apPlayLink2();
public:
	apPlayLink2(const char * title, const char * id, const char * desc);

	apPlayLink2(const char * title, const char * ext_title, const char * id, const char * desc, 
			const char * mark, const char * director, const char * act, 
			const char * year, const char * area,
			const char * resolution, int duration_sec);

	~apPlayLink2(void);

	int get_id(){return atoi(mId.c_str());}

	const char * get_description(){return mDescription.c_str();}

	const char * get_title();

	int get_width(){return mWidth;}

	int get_height(){return mHeight;}

	int get_duration(){return mDurationSec;}

private:
	void setResolution();
private:
	std::string mTitle;
	std::string	mId;
	std::string mDescription;

	std::string mMark;
	std::string mDirector;
	std::string mAct;
	std::string mYear;
	std::string mArea;
	std::string mResolution;

	int			mWidth;
	int			mHeight;
	int			mDurationSec;
	
	std::string	mExtTitle;
	std::string mFinalTitle;
};

