#include "stdafx.h"
#include "apXmlParser.h"
#define LOG_TAG "apXmlParser"
#include "log.h"

//static std::string Utf82Ansi(const char* srcCode);

apXmlParser::apXmlParser(void)
{
}


apXmlParser::~apXmlParser(void)
{
}

bool apXmlParser::parseSearch(char *context, unsigned int size)
{
	mNavigatorList.clear();
	mPlaylinkList.clear();

	FILE *pFile = NULL;
	fopen_s(&pFile, "tmp.xml", "wb");
	fwrite(context, 1, size, pFile);
	fclose(pFile);

	CMarkup dom;
	bool ret;

	//ret = dom.SetDoc(context);
	ret = dom.Load(_T("tmp.xml"));
	if (ret == false) {
		LOGE("failed to parse xml");
		
		return false;
	}

	dom.FindElem("vlist");
	dom.IntoElem();

	while (dom.FindElem("v")) {
		dom.IntoElem();
		std::string vid = dom.GetAttrib(_T("vid"));
		apPlayLink2 l("", vid.c_str(), "");
		mPlaylinkList.push_back(l);
	}

	return true;
}

EPG_PLAYLINK_LIST * apXmlParser::parseDetail(char *context, unsigned int size)
{
	mPlaylinkList.clear();

	FILE *pFile = NULL;
	fopen_s(&pFile, "tmp.xml", "wb");
	fwrite(context, 1, size, pFile);
	fclose(pFile);

	//std::string curLocale = setlocale(LC_ALL, NULL);
	//setlocale(LC_ALL, "chs");

	//setlocale(LC_ALL, curLocale.c_str());
	
	CMarkup dom;
	bool ret;

	//ret = dom.SetDoc(context);
	ret = dom.Load(_T("tmp.xml"));
	if (ret == false) {
		LOGE("failed to parse xml");
		return NULL;
	}

	dom.FindElem(_T("v"));
	dom.IntoElem();

	add_v(dom);

	return &mPlaylinkList;
}

boolean apXmlParser::add_v(CMarkup v)
{
	boolean found = false;

	v.FindElem("title");
	std::string link_title		= v.GetData();
	v.FindElem("vid");
	std::string link_id			= v.GetData();
	v.FindElem("director");
	std::string link_director	= v.GetData();
	v.FindElem("act");
	std::string link_act		= v.GetData();
	v.FindElem("year");
	std::string link_year		= v.GetData();
	v.FindElem("area");
	std::string link_area		= v.GetData();

	v.FindElem("resolution");
	std::string link_resolution = v.GetData();

	std::string str_du;
	int duration_sec;
	found = v.FindElem("durationSecond");
	if (found) {
		str_du = v.GetData();
		duration_sec = atoi(str_du.c_str());
	}
	else {
		v.FindElem("duration");
		str_du = v.GetData();
		duration_sec = atoi(str_du.c_str()) * 60;
	}

	std::string link_description = "N/A";
	found = v.FindElem("content");
	if (found)
		link_description = v.GetData();

	found = v.FindElem("video_list2");
	if (found)
		v.IntoElem();

	int count = 0;
	while(v.FindElem("playlink2")) {
		count++;
	}

	v.ResetMainPos();
	while(v.FindElem("playlink2")) {
		std::string id = v.GetAttrib("id");
		if (!id.empty())
			link_id = id; // overwrite

		std::string ext_title = "";
		if (count > 1)
			ext_title = v.GetAttrib("title");

		v.IntoElem();
		v.FindElem("source");
		std::string src_mark = v.GetAttrib("mark");
		std::string src_res = v.GetAttrib("resolution");
		if(!src_res.empty())
			link_resolution = src_res; // overwrite
		v.OutOfElem();

		apPlayLink2 l(link_title.c_str(), ext_title.c_str(), link_id.c_str(), link_description.c_str(), 
			src_mark.c_str(), link_director.c_str(), link_act.c_str(),
			link_year.c_str(), link_area.c_str(),
			link_resolution.c_str(), duration_sec);
		mPlaylinkList.push_back(l);
	}

	return true;
}

/*static std::string Utf82Ansi(const char* srcCode)  
{     
    int srcCodeLen=0;  
    srcCodeLen=MultiByteToWideChar(CP_UTF8,NULL,srcCode,strlen(srcCode),NULL,0);  
    wchar_t* result_t=new wchar_t[srcCodeLen+1];  
    MultiByteToWideChar(CP_UTF8,NULL,srcCode,strlen(srcCode),result_t,srcCodeLen);  
    result_t[srcCodeLen]='/0';  
    srcCodeLen=WideCharToMultiByte(CP_ACP,NULL,result_t,wcslen(result_t),NULL,0,NULL,NULL);  
    char* result=new char[srcCodeLen+1];  
    WideCharToMultiByte(CP_ACP,NULL,result_t,wcslen(result_t),result,srcCodeLen,NULL,NULL);  
    result[srcCodeLen]='/0';  
    std::string srcAnsiCode="";  
    srcAnsiCode=(std::string)result;  
    delete result_t;  
    delete result;  
    return srcAnsiCode;  
}*/