#pragma once
#include <string>

class apCatalog
{
private:
	apCatalog();
public:
	apCatalog(const char *title, const char *target, const char *link);
	~apCatalog(void);

	int get_vid();

	const char * get_title(){return mTitle.c_str();}

	const char * get_target(){return mTarget.c_str();}
private:
	std::string mTitle;
	std::string mTarget;
	std::string mLink;
};

