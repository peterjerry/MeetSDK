#include "stdafx.h"
#include "apBlockDownloader.h"
#define LOG_TAG "apBlockDownloader"
#include "log.h"

#include <curl/curl.h> 

#define MAX_DATA_SIZE 1048576

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
	apBlockDownloader *ins = (apBlockDownloader *)outstream;

	int written;
	if (ins->m_save_file) {
		FILE *pFile = ins->mFileHandle;

		written = fwrite(buffer, size, nitems, pFile);
	}
	else {
		memcpy(ins->mData + ins->mDataOffset, buffer, size * nitems);
		written = size * nitems;
		ins->mDataOffset += written;
	}

	return written;
}

apBlockDownloader::~apBlockDownloader(void)
{
	if (mData) {
		delete mData;
		mData = NULL;
	}
}

bool apBlockDownloader::saveAs(const char * filename)
{
	bool ret = true;

	curl_global_init(CURL_GLOBAL_ALL);
	CURL *curl = curl_easy_init();

	CURLcode res;

	curl_easy_reset(curl);

	if (fopen_s(&mFileHandle, filename, "wb") != 0) {
		LOGE("failed to open file %s", filename);
		return false;
	}

	curl_easy_setopt(curl, CURLOPT_URL, m_url);
	curl_easy_setopt(curl,  CURLOPT_TIMEOUT, 5); // 5 sec
	curl_easy_setopt(curl,  CURLOPT_CONNECTTIMEOUT, 3); // 3 sec
	curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void*)this);
	curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data);
	curl_easy_setopt(curl, CURLOPT_RANGE, "1400-");

	res = curl_easy_perform(curl);
	if (CURLE_OK != res) {
		LOGE("curl error %s", curl_easy_strerror(res));
		ret = false;
	}

	if (mFileHandle) {
		fclose(mFileHandle);
		mFileHandle = NULL;
	}

	return ret;
}

bool apBlockDownloader::saveInMemory()
{
	m_save_file = false;

	bool ret = true;

	if (!mData) {
		mData = new char[MAX_DATA_SIZE];
		mDataSize = MAX_DATA_SIZE;
	}

	curl_global_init(CURL_GLOBAL_ALL);
	CURL *curl = curl_easy_init();

	CURLcode res;

	curl_easy_reset(curl);

	curl_easy_setopt(curl, CURLOPT_URL, m_url);
	curl_easy_setopt(curl,  CURLOPT_TIMEOUT, 5); // 5 sec
	curl_easy_setopt(curl,  CURLOPT_CONNECTTIMEOUT, 3); // 3 sec
	curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void*)this);
	curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data);
	curl_easy_setopt(curl, CURLOPT_RANGE, "1400-");

	mDataOffset = 0;

	res = curl_easy_perform(curl);
	if (CURLE_OK != res) {
		LOGE("curl error %s", curl_easy_strerror(res));
		ret = false;
	}

	return ret;
}

char * apBlockDownloader::getData(int *len) {
	if (len == NULL || m_save_file)
		return NULL;

	*len = mDataOffset;
	return mData;
}