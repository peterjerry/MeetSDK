#include "stdafx.h"
#include "apEPG.h"
#include "apJsonParser.h"
#define LOG_TAG "apEPG"
#include "log.h"

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

#define CDN_URL_FMT "http://play.api.pptv.com/boxplay.api?" \
	"ft=1&platform=android3&type=phone.android.vip" \
	"&sdk=1&channel=162&vvid=41&auth=55b7c50dc1adfc3bcabe2d9b2015e35c&id=%d"

apEPG::apEPG(void)
	:mData(NULL), mCurl(NULL)
{
	mData = new char[MAX_DATA_SIZE];
	reset();

	curl_global_init(CURL_GLOBAL_ALL);
	mCurl = curl_easy_init();
}


apEPG::~apEPG(void)
{
	if (mData) {
		delete mData;
		mData = NULL;
	}

	if (mCurl) {
		curl_easy_cleanup(mCurl);
		mCurl = NULL;
	}

	curl_global_cleanup();
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

EPG_MODULE_LIST * apEPG::frontpage()
{
	reset();
   
	CURLcode res;

	LOGI("curl_perform(getCatalog): %s", FRONTPAGE_URL);

	curl_easy_reset(mCurl);

	curl_easy_setopt(mCurl, CURLOPT_URL, FRONTPAGE_URL);
	curl_easy_setopt(mCurl,  CURLOPT_TIMEOUT, 5); // 5 sec
	curl_easy_setopt(mCurl,  CURLOPT_CONNECTTIMEOUT, 3); // 3 sec
	curl_easy_setopt(mCurl, CURLOPT_WRITEFUNCTION, apEPG::write_data);
	curl_easy_setopt(mCurl, CURLOPT_WRITEDATA, this);

	res = curl_easy_perform(mCurl);
	if (CURLE_OK != res) {
		apLog::print(0, apLog::info, "curl error %s\n", curl_easy_strerror(res));
		return NULL;
	}
	
	return mParser.parseFrontpage(mData, mDataSize);
}

EPG_CATALOG_LIST * apEPG::catalog(int catalog_index)
{
	reset();

	CURLcode res;

	LOGI("curl_perform(getCatalog): %s", FRONTPAGE_URL);

	curl_easy_reset(mCurl);

	curl_easy_setopt(mCurl, CURLOPT_URL, FRONTPAGE_URL);
	curl_easy_setopt(mCurl,  CURLOPT_TIMEOUT, 5); // 5 sec
	curl_easy_setopt(mCurl,  CURLOPT_CONNECTTIMEOUT, 3); // 3 sec
	curl_easy_setopt(mCurl, CURLOPT_WRITEFUNCTION, apEPG::write_data);
	curl_easy_setopt(mCurl, CURLOPT_WRITEDATA, this);

	res = curl_easy_perform(mCurl);
	if (CURLE_OK != res) {
		apLog::print(0, apLog::info, "curl error %s\n", curl_easy_strerror(res));
		return NULL;
	}
	
	return mParser.parseCatalog(mData, mDataSize, catalog_index);
}

bool apEPG::search(const char* key, EPG_NAVIGATOR_LIST **pNav, EPG_PLAYLINK_LIST **pPlaylink)
{
	reset();

	CURLcode res;
	
	int out_len = 0;
	char *encoded_key = urlencode(key, strlen(key), &out_len);

	TCHAR url[1024] = {0};
	_stprintf_s(url, SREATCH_URL_FMT, encoded_key);
	LOGI("curl_perform(search): %s", url);

	curl_easy_reset(mCurl);

	curl_easy_setopt(mCurl, CURLOPT_URL, url);
	curl_easy_setopt(mCurl,  CURLOPT_TIMEOUT, 5); // 5 sec
	curl_easy_setopt(mCurl,  CURLOPT_CONNECTTIMEOUT, 3); // 3 sec
	curl_easy_setopt(mCurl, CURLOPT_WRITEFUNCTION, apEPG::write_data);
	curl_easy_setopt(mCurl, CURLOPT_WRITEDATA, this);

	res = curl_easy_perform(mCurl);
	if (CURLE_OK != res) {
		apLog::print(0, apLog::error, "curl error %s\n", curl_easy_strerror(res));
		return false;
	}
	
	if (!mParserXml.parseSearch(mData, mDataSize)) {
		apLog::print(0, apLog::error, "failed to parse search xml");
		return false;
	}

	*pNav		= mParserXml.get_nav();
	*pPlaylink	= mParserXml.get_playlink();
	return true;
}

EPG_PLAYLINK_LIST * apEPG::detail(int vid)
{
	LOGI("getPlaylink() vid %d", vid);

	if (vid == 0) {
		LOGE("invalid vid %d", vid);
		return NULL;
	}

	reset();

	TCHAR url[1024] = {0};
	_stprintf_s(url, DETAIL_URL_FMT, vid, vid);
	LOGI("curl_perform(getPlaylink): %s", url);

	CURLcode res;

	curl_easy_reset(mCurl);

	curl_easy_setopt(mCurl, CURLOPT_URL, url);
	curl_easy_setopt(mCurl, CURLOPT_WRITEFUNCTION, apEPG::write_data);
	curl_easy_setopt(mCurl, CURLOPT_WRITEDATA, this);

	res = curl_easy_perform(mCurl);
	if (CURLE_OK != res) {
		apLog::print(0, apLog::info, "curl error %s\n", curl_easy_strerror(res));
		return NULL;
	}
	
	apLog::print(0, apLog::info, "post ok. %d", mDataSize);
	mData[mDataSize] = '\0';
	return mParserXml.parseDetail(mData, mDataSize);
}

char * apEPG::get_cdn_url(int vid, int ft, bool is_m3u8, bool novideo)
{
	LOGI("get_cdn_url() vid %d", vid);

	if (vid == 0) {
		LOGE("invalid vid %d", vid);
		return NULL;
	}

	reset();

	TCHAR url[1024] = {0};
	_stprintf_s(url, CDN_URL_FMT, vid);
	LOGI("curl_perform(live_cdn): %s", url);

	CURLcode res;

	curl_easy_reset(mCurl);

	curl_easy_setopt(mCurl, CURLOPT_URL, url);
	curl_easy_setopt(mCurl, CURLOPT_WRITEFUNCTION, apEPG::write_data);
	curl_easy_setopt(mCurl, CURLOPT_WRITEDATA, this);

	res = curl_easy_perform(mCurl);
	if (CURLE_OK != res) {
		apLog::print(0, apLog::info, "curl error %s\n", curl_easy_strerror(res));
		return false;
	}
	
	apLog::print(0, apLog::info, "post ok. %d", mDataSize);
	mData[mDataSize] = '\0';
	return mParserXml.parseCDN(mData, mDataSize, ft, is_m3u8, novideo);
}

apCDNItem * apEPG::get_live_cdn_url(int vid)
{
	LOGI("get_cdn_url() vid %d", vid);

	if (vid == 0) {
		LOGE("invalid vid %d", vid);
		return NULL;
	}

	reset();

	TCHAR url[1024] = {0};
	_stprintf_s(url, CDN_URL_FMT, vid);
	LOGI("curl_perform(live_cdn): %s", url);

	CURLcode res;

	curl_easy_reset(mCurl);

	curl_easy_setopt(mCurl, CURLOPT_URL, url);
	curl_easy_setopt(mCurl, CURLOPT_WRITEFUNCTION, apEPG::write_data);
	curl_easy_setopt(mCurl, CURLOPT_WRITEDATA, this);

	res = curl_easy_perform(mCurl);
	if (CURLE_OK != res) {
		apLog::print(0, apLog::info, "curl error %s\n", curl_easy_strerror(res));
		return false;
	}
	
	apLog::print(0, apLog::info, "post ok. %d", mDataSize);
	mData[mDataSize] = '\0';
	return mParserXml.parseLiveCDN(mData, mDataSize);
}
