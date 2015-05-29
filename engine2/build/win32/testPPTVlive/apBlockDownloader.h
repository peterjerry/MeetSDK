#pragma once
class apBlockDownloader
{
public:
	apBlockDownloader(const char * url) {
		m_url		= url;
		m_save_file = true;
		mFileHandle	= NULL;
		mData		= NULL;
		mDataSize	= 0;
	}
	~apBlockDownloader(void);

	bool saveAs(const char * filename);

	bool saveInMemory();

	char *getData(int *len);
private:
	static size_t write_data(char *buffer,size_t size, size_t nitems,void *outstream);
private:
	const char *m_url;
public:
	FILE *mFileHandle;
	char *mData;
	int mDataSize;
	int mDataOffset;
	bool m_save_file;
};

