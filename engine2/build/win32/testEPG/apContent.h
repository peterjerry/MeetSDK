#pragma once
#include <string>

class apContent
{
private:
	apContent();
public:
	apContent(const char * id, const char * title, const char * param, const char * pos);
	~apContent(void);

	const char * get_title(){return mTitle.c_str();}

	const char * get_param(){return mParam.c_str();}

private:
	std::string mId;
	std::string mTitle;
	std::string mParam;
	std::string mPosition;
};

