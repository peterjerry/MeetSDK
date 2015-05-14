#include "stdafx.h"
#include "apBlockDownloader.h"
#define LOG_TAG "apBlockDownloader"
#include "log.h"

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

size_t apBlockDownloader::write_data(char *buffer,size_t size, size_t nitems,void *outstream)
{
	int written = fwrite(buffer, size, nitems, (FILE*)outstream);
	return written;
}

apBlockDownloader::~apBlockDownloader(void)
{
}

bool apBlockDownloader::saveAs(const char * filename)
{
	bool ret = true;

	curl_global_init(CURL_GLOBAL_ALL);
	CURL *curl = curl_easy_init();

	CURLcode res;

	curl_easy_reset(curl);

	FILE* pFile = NULL;
	if (fopen_s(&pFile, filename, "wb") != 0) {
		LOGE("failed to open file %s", filename);
		return false;
	}

	curl_easy_setopt(curl, CURLOPT_URL, m_url);
	curl_easy_setopt(curl,  CURLOPT_TIMEOUT, 5); // 5 sec
	curl_easy_setopt(curl,  CURLOPT_CONNECTTIMEOUT, 3); // 3 sec
	curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void*)pFile);
	curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data);
	curl_easy_setopt(curl, CURLOPT_RANGE, "1400-");

	res = curl_easy_perform(curl);
	if (CURLE_OK != res) {
		LOGE("curl error %s", curl_easy_strerror(res));
		ret = false;
	}

	if (pFile) {
		fclose(pFile);
		pFile = NULL;
	}

	return ret;
}