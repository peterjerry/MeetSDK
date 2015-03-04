#pragma once
#include <string>

class apModule
{
private:
	apModule();
public:
	apModule(int index, const char* title);

	apModule(int index, const char* title, const char* target, const char* link);

	~apModule(void);

	int get_index(){return mIndex;}
	
	const char * get_title(){return mTitle.c_str();}
	
	const char * get_link(){return mLink.c_str();}

private:
	int			mIndex;
	std::string mTitle;
	std::string mTarget;
	std::string mLink;
};

