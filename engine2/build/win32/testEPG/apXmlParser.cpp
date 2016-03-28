#include "stdafx.h"
#include "apXmlParser.h"
#define LOG_TAG "apXmlParser"
#include "log.h"
#include <time.h>
#include "strptime.h"
#include "apKey.h"
#include <vector>

#define HOST_PORT_STR ":80"

//static std::string Utf82Ansi(const char* srcCode);

struct rid_t {
	int			ft;
	std::string	rid;
};

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

char * apXmlParser::parseCDN(char *context, unsigned int size, int ft, bool is_m3u8, bool novideo)
{
	FILE *pFile = NULL;
	fopen_s(&pFile, "tmp2.xml", "wb");
	fwrite(context, 1, size, pFile);
	fclose(pFile);

	CMarkup dom;
	bool ret;

	//ret = dom.SetDoc(context);
	ret = dom.Load(_T("tmp2.xml"));
	if (ret == false) {
		LOGE("failed to parse xml");
		
		return false;
	}

	dom.FindElem("");
	dom.IntoElem();

	dom.FindElem("channel");
	std::string main_rid = dom.GetAttrib("rid");
	dom.IntoElem();
	dom.FindElem("file");
	dom.IntoElem();
	std::vector<rid_t> ridList;

	while(dom.FindElem("item")) {
		std::string rid = dom.GetAttrib("rid");
		int ft = atoi(dom.GetAttrib("ft").c_str());
		struct rid_t new_item;
		new_item.ft = ft;
		new_item.rid = rid;
		ridList.push_back(new_item);
	}

	dom.OutOfElem();
	dom.OutOfElem();

	while (dom.FindElem("dt")) {
		std::string d_ft = dom.GetAttrib("ft");

		dom.IntoElem();
		dom.FindElem("sh");
		std::string d_sh = dom.GetData(); // main server
		ret = dom.FindElem("st");
		std::string d_st = dom.GetData(); // server time
		dom.FindElem("bh");
		std::string d_bh = dom.GetData(); // backup server
		dom.FindElem("key");
		std::string d_key = dom.GetData(); // key

		if (atoi(d_ft.c_str()) == ft) {
			//char * str_time = "Tue May 12 07:58:14 2015 UTC";
			tm utc_tm;
			strptime(d_st.c_str(), "%a %b %d %H:%M:%S %Y %Z", &utc_tm);
			//char * asctime(const struct tm * timeptr);
			//char* ctime(time_t* t);
			//time_t utc_time = mktime(&utc_tm);
			//tm *gmt_time = localtime(&utc_time);
			time_t t = _mkgmtime(&utc_tm);
			LOGI("ft %s sh %s, bh %s, st %s, key %s, time %I64d sec", 
				d_ft.c_str(), d_sh.c_str(), d_bh.c_str(), d_st.c_str(), d_key.c_str(), t);

			uint8_t *gen_key = apKey::genKey(t);
			LOGI("gen_key %s", gen_key);

			std::string final_url;

			if (d_sh.find("http://") == std::string::npos)
				final_url = "http://" + d_sh;
			else
				final_url = d_sh;
			if (d_sh.find(":") == std::string::npos)
				final_url += std::string(HOST_PORT_STR);
					
			final_url += "/";
					
			std::string url_rid;
			for (unsigned int j=0;j<ridList.size();j++) {
				if (ridList[j].ft == ft) {
					url_rid = ridList[j].rid;
					break;
				}
			}

			if (novideo/*force use m3u8*/ || is_m3u8) {
				std::string strsrc = ".mp4";
				std::string strdst = ".m3u8";
				std::string::size_type pos = 0;
				std::string::size_type srclen = strsrc.size();
				std::string::size_type dstlen = strdst.size();

				if( (pos = url_rid.find(strsrc, pos)) != std::string::npos ) {
					url_rid.replace( pos, srclen, strdst );
					pos += dstlen;
				}

				final_url += url_rid;
			}
			else
				final_url += url_rid;
					
			final_url += "?w=1&key=" + std::string((char *)gen_key);
			// fix vip video can ONLY get trailer duration problem 
			// key cbdcf8c028a5b26f1e12ba4f2fcf440c-2516-1459143765%26segment%3D5319010a_53190026_1459129365
			std::string::size_type pos = d_key.find("%26segment%3D");
			if (pos != -1) {
				d_key = d_key.substr(0, pos);
			}
			final_url += "&k=" + d_key;
			if (novideo)
				final_url += "&video=false";
			final_url += "&type=phone.android.vip&vvid=877a4382-f0e4-49ed-afea-8d59dbd11df1" \
				"&sv=4.1.3&platform=android3";
			final_url += "&ft=" + d_ft;
			final_url += "&accessType=wifi";
	
			char *str_url = new char[final_url.length() + 1];
			strcpy(str_url, final_url.c_str());

			LOGI("epg final cdn url: %s", str_url);
			
			delete gen_key;
			gen_key = NULL;
			return str_url;
		}

		dom.OutOfElem();
	}

	return NULL;
}

apCDNItem * apXmlParser::parseLiveCDN(char *context, unsigned int size)
{
	FILE *pFile = NULL;
	fopen_s(&pFile, "tmp3.xml", "wb");
	fwrite(context, 1, size, pFile);
	fclose(pFile);

	CMarkup dom;
	bool ret;
	ret = dom.SetDoc(context);
	if (ret == false) {
		LOGE("failed to parse xml");
		return false;
	}

	dom.FindElem("");
	dom.IntoElem();

	dom.FindElem("channel");
	dom.IntoElem();
	dom.FindElem("stream");
	dom.IntoElem();
	dom.FindElem("item");
	std::string rid = dom.GetAttrib("rid");
	std::string ft = dom.GetAttrib("ft");
	dom.OutOfElem();
	dom.OutOfElem();

	dom.FindElem("dt");
	dom.IntoElem();

	dom.FindElem("sh");
	std::string d_sh = dom.GetData(); // main server
	ret = dom.FindElem("st");
	std::string d_st = dom.GetData(); // server time
	dom.FindElem("key");
	std::string d_key = dom.GetData(); // key
	dom.FindElem("bh");
	std::string d_bh = dom.GetData(); // backup server

	return new apCDNItem(d_sh.c_str(), d_st.c_str(), d_bh.c_str(), atoi(ft.c_str()), rid.c_str(), d_key.c_str());
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

bool apXmlParser::add_v(CMarkup v)
{
	bool found = false;

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