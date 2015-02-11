#include "stdafx.h"
#include "apXmlParser.h"
#include "markup.h"
#include "apFileLog.h"

static std::string Utf82Ansi(const char* srcCode);

apXmlParser::apXmlParser(void)
{
}


apXmlParser::~apXmlParser(void)
{
}

MAP_ITEM * apXmlParser::parsePlaylink(char *context, unsigned int size)
{
	_tprintf(_T("begin to parsePlaylink()\n"));

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
		apLog::print(0, apLog::error, "failed to parse xml");
		
		return NULL;
	}

	MAP_ITEM *new_item = new MAP_ITEM();

	ret = dom.FindElem(_T("v"));
	dom.IntoElem();

	ret = dom.FindElem(_T("title"));
	std::string title = dom.GetData();

	ret = dom.FindElem(_T("video_list2"));
	dom.IntoElem();

	ret = dom.FindElem(_T("playlink2"));
	std::string id = dom.GetAttrib(_T("id"));
	std::string duration = dom.GetAttrib(_T("duration")); // min
	new_item->insert(MAP_ITEM::value_type("id", id));
	new_item->insert(MAP_ITEM::value_type("duration", duration));

	return new_item;
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