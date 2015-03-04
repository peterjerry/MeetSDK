#pragma once
#include <string>

class apNavigator
{
private:
	apNavigator();
public:
	apNavigator(const char * name, const char * id, int count);
	~apNavigator(void);

	const char * get_name(){return mName.c_str();}

	const char * get_id(){return mId.c_str();}

	int get_count(){return mCount;}
private:
	std::string mName;
	std::string mId;
	int			mCount;
};

