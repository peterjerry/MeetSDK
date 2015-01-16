#include "filesource.h"
#define LOG_TAG "FILE_SOURCE"
#include "log.h"

FileSource::FileSource()
	:mURL(NULL), mFile(NULL), mFileSize(0)
{
}

FileSource::~FileSource()
{
	close();
}

bool FileSource::open(const char* url)
{
	mURL = url;
	mFile = fopen(mURL, "rb");
	if (mFile == NULL) {
		LOGE("failed to open url: %s", mURL);
		return false;
	}

	fseek(mFile, 0, SEEK_END);
	mFileSize = ftell(mFile);
	fseek(mFile, 0, SEEK_SET);

	return true;
}

int FileSource::read_data(char *data, unsigned int size)
{
	if (mFile == NULL)
		return -1;

	return fread(data, 1, size, mFile);
}

int64_t FileSource::read_seek(uint64_t offset, int whence)
{
	if (mFile == NULL)
		return -1;

	return fseek(mFile, offset, whence);
}

int64_t FileSource::get_size()
{
	return mFileSize;
}

void FileSource::close()
{
	if (mFile != NULL) {
		fclose(mFile);
		mFile = NULL;
	}
}
