#ifndef _FILE_SOURCE_H_
#define _FILE_SOURCE_H_

#include "ffsourcebase.h"
#include <stdio.h>

class FileSource: public FFSourceBase
{
public:
	FileSource();
	~FileSource();

public:
	virtual bool open(const char* url);

	virtual int read_data(char *data, unsigned int size);

	virtual int64_t read_seek(uint64_t offset, int whence);

	virtual int64_t get_size();

	virtual void close();
private:
	const char*	mURL;
	FILE*		mFile;
	int64_t		mFileSize;
};


#endif // _FILE_SOURCE_H_
