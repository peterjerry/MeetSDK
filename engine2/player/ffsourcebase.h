#ifndef _FFSOURCE_BASE_H_
#define _FFSOURCE_BASE_H_

#include <stdint.h>

class FFSourceBase
{
public:
	FFSourceBase() {}
	virtual ~FFSourceBase() {}

public:
	virtual bool open(const char* url) = 0;

	virtual int read_data(char *data, unsigned int size) = 0;

	virtual int64_t read_seek(uint64_t offset, int whence) { return -1; }

	virtual int64_t get_size() = 0;

	virtual void close() = 0;
};


#endif // _FFSOURCE_BASE_H_
