#pragma once
class apBlockDownloader
{
public:
	apBlockDownloader(const char * url) {
		m_url = url;
	}
	~apBlockDownloader(void);

	bool saveAs(const char * filename);
private:
	static size_t write_data(char *buffer,size_t size, size_t nitems,void *outstream);
private:
	const char *m_url;
};

