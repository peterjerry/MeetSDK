#include "stdafx.h"
#include "apXmlParser.h"
#include "markup.h"
#define LOG_TAG "apXmlParser"
#include "log.h"

static std::string Utf82Ansi(const char* srcCode);

apXmlParser::apXmlParser(void)
{
}


apXmlParser::~apXmlParser(void)
{
}

EPG_LIST * apXmlParser::parseSearch(char *context, unsigned int size)
{
	mClips.clear();

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
		
		return NULL;
	}

	dom.FindElem("vlist");
	dom.IntoElem();

	while (dom.FindElem("v")) {
		dom.IntoElem();

		MAP_ITEM new_item;

		std::string vid = dom.GetAttrib(_T("vid"));
		new_item.insert(MAP_ITEM::value_type("vid", vid));
		mClips.push_back(new_item);
	}

	return &mClips;
}

EPG_LIST * apXmlParser::parsePlaylink(char *context, unsigned int size)
{
	mClips.clear();

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

	

	ret = dom.FindElem(_T("v"));
	dom.IntoElem();

	ret = dom.FindElem(_T("title"));
	std::string title = dom.GetData();

	ret = dom.FindElem(_T("video_list2"));
	dom.IntoElem();
	
	while (dom.FindElem(_T("playlink2"))) {
		MAP_ITEM new_item;

		std::string id = dom.GetAttrib(_T("id"));
		std::string title = dom.GetAttrib(_T("title"));
		std::string duration = dom.GetAttrib(_T("duration")); // min
		std::string duration_sec = dom.GetAttrib(_T("durationSecond")); // min
		new_item.insert(MAP_ITEM::value_type("id", id));
		new_item.insert(MAP_ITEM::value_type("title", title));
		new_item.insert(MAP_ITEM::value_type("duration", duration));
		new_item.insert(MAP_ITEM::value_type("duration_sec", duration_sec));

		dom.IntoElem();
		if (dom.FindElem("source")) {
			std::string bitrate = dom.GetAttrib(_T("bitrate"));
			std::string resolution = dom.GetAttrib(_T("resolution"));
			
			new_item.insert(MAP_ITEM::value_type("bitrate", bitrate));
			new_item.insert(MAP_ITEM::value_type("resolution", resolution));
		}
		dom.OutOfElem();

		mClips.push_back(new_item);
	}

	return &mClips;
}

static std::string Utf82Ansi(const char* srcCode)  
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
}  