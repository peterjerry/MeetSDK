#include "stdafx.h"
#include "apEPG.h"
#include "apJsonParser.h"
#define LOG_TAG "apEPG"
#include "log.h"

#include "wininet.h"
#include "urlcodec.h"
#include "md5c.h"
#include "apFileLog.h"

#include <curl/curl.h> 

#ifdef _DEBUG
#pragma comment(lib, "libcurl_mdd.lib")
#else
#pragma comment(lib, "libcurl_md.lib")
#endif

#pragma comment( lib, "ws2_32.lib" )
#pragma comment( lib, "winmm.lib" )
#pragma comment(lib, "wldap32.lib")
#pragma  comment(lib, "wininet.lib")

#define MAX_DATA_SIZE (65536 * 10)

#define FRONTPAGE_URL "http://mtbu.api.pptv.com/v4/module?lang=zh_cn&platform=aphone" \
	"&appid=com.pplive.androidphone&appver=4.1.3&appplt=aph&userLevel=0"

#define DETAIL_URL_FMT "http://epg.api.pptv.com/detail.api?auth=d410fafad87e7bbf6c6dd62434345818" \
	"&canal=@SHIP.TO.%dPI@&userLevel=0&appid=com.pplive.androidphone&appver=4.1.3" \
	"&appplt=aph&vid=%d&series=1&virtual=1&ver=2&platform=android3"

#define SREATCH_URL_FMT "http://so.api.pptv.com/search_smart.api?auth=d410fafad87e7bbf6c6dd62434345818" \
	"&appver=4.1.3&canal=@SHIP.TO.31415926PI@&userLevel=0&hasVirtual=1&k=%s&conlen=0" \
	"&shownav=1&type=0&mode=all&contentype=0&c=2&s=1&ver=2&platform=android3" // k=xxx

apEPG::apEPG(void)
	:mData(NULL)
{
	mData = new char[MAX_DATA_SIZE];
	reset();
}


apEPG::~apEPG(void)
{
	if (mData) {
		delete mData;
		mData = NULL;
	}
}

void apEPG::reset()
{
	memset(mData, 0, MAX_DATA_SIZE);
	mDataSize = 0;
}

size_t apEPG::write_data(void *buffer, size_t size, size_t nmemb, void *userp)
{
	apEPG *instance = (apEPG *)userp;
	return instance->write_data_impl(buffer, size, nmemb, userp);
}

size_t apEPG::write_data_impl(void *buffer, size_t size, size_t nmemb, void *userp)
{
	size_t write_size = size * nmemb;
	if (write_size > MAX_DATA_SIZE) {
		apLog::print(0, apLog::info, "exceed max data size");
		return -1;
	}

	memcpy(mData + mDataSize, buffer, write_size);
	mDataSize += write_size;
	return nmemb;
}

EPG_LIST * apEPG::getCatalog(int index)
{
	reset();

	CURL *curl = NULL;   
	CURLcode res;
	curl = curl_easy_init();  
	if (!curl){
		return NULL;
	}

	LOGI("curl_perform(getCatalog): %s", FRONTPAGE_URL);

	curl_easy_reset(curl);

	curl_easy_setopt(curl, CURLOPT_URL, FRONTPAGE_URL);
	curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, apEPG::write_data);
	curl_easy_setopt(curl, CURLOPT_WRITEDATA, this);

	res = curl_easy_perform(curl);
	if (CURLE_OK != res) {
		apLog::print(0, apLog::info, "curl error %s\n", curl_easy_strerror(res));
		return NULL;
	}
	
	if (index == -1)
		return mParser.parseCatalog(mData, mDataSize);
	else
		return mParser.parseCollection(mData, mDataSize, index);
}

EPG_LIST * apEPG::search(const char* key)
{
	reset();

	CURL *curl = NULL;   
	CURLcode res;
	curl = curl_easy_init();  
	if (!curl){
		return NULL;
	}

	int out_len = 0;
	char *encoded_key = urlencode(key, strlen(key), &out_len);

	TCHAR url[1024] = {0};
	_stprintf_s(url, SREATCH_URL_FMT, encoded_key);
	LOGI("curl_perform(search): %s", url);

	curl_easy_reset(curl);

	curl_easy_setopt(curl, CURLOPT_URL, url);
	curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, apEPG::write_data);
	curl_easy_setopt(curl, CURLOPT_WRITEDATA, this);

	res = curl_easy_perform(curl);
	if (CURLE_OK != res) {
		apLog::print(0, apLog::info, "curl error %s\n", curl_easy_strerror(res));
		return NULL;
	}
	
	
	return mParserXml.parseSearch(mData, mDataSize);
}

EPG_LIST * apEPG::getPlaylink(int index)
{
	LOGI("getPlaylink() index %d", index);

	if (index == 0) {
		LOGE("invalid index %d", index);
		return NULL;
	}

	reset();

	TCHAR url[1024] = {0};
	_stprintf_s(url, DETAIL_URL_FMT, index, index);
	LOGI("curl_perform(getPlaylink): %s", url);

	CURL *curl = NULL;   
	CURLcode res;
	curl = curl_easy_init();  
	if(!curl){
		return NULL;
	}

	curl_easy_reset(curl);

	curl_easy_setopt(curl, CURLOPT_URL, url);
	curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, apEPG::write_data);
	curl_easy_setopt(curl, CURLOPT_WRITEDATA, this);

	res = curl_easy_perform(curl);
	if (CURLE_OK != res) {
		apLog::print(0, apLog::info, "curl error %s\n", curl_easy_strerror(res));
		return NULL;
	}
	
	apLog::print(0, apLog::info, "post ok. %d", mDataSize);
	mData[mDataSize] = '\0';
	return mParserXml.parsePlaylink(mData, mDataSize);
}
