#include "fifobuffer.h"
#include "autolock.h"
#include <stdlib.h>
#include <string.h>

and_fifobuffer::and_fifobuffer()
:buf_(NULL), header_(NULL), tail_(NULL) {
}

and_fifobuffer::~and_fifobuffer()
{
	close();
}

char* and_fifobuffer::header()
{
	pthread_mutex_lock(&mutex_);
	char* p = header_;
	pthread_mutex_unlock(&mutex_);
	return p;
}

int and_fifobuffer::create(unsigned int size)
{
	buf_ = (char *)malloc(size);
	if(!buf_) 
		return -1;
	
	int ret;
	ret = pthread_mutex_init(&mutex_, 0);
	if (ret < 0) {
		free(buf_);
		buf_ = NULL;
		return -1;
	}
		
	pthread_mutex_lock(&mutex_);
	size_= size;
	reset_impl();
	pthread_mutex_unlock(&mutex_);

	return 0;
}

void and_fifobuffer::reset()
{
	AutoLock lock(&mutex_);
	reset_impl();
}

void and_fifobuffer::close()
{
	pthread_mutex_lock(&mutex_);

	if (buf_)
		free(buf_);

	reset_impl();
	pthread_mutex_unlock(&mutex_);
	
	pthread_mutex_destroy(&mutex_);
}

int and_fifobuffer::write(char* p, unsigned int howmuch)
{
	AutoLock lock(&mutex_);

	unsigned int nWrite = size_ - used_;
	if (nWrite > howmuch)
		nWrite = howmuch;

	if (0 == nWrite)
		return 0;
	
	// Copy the data
	if ( tail_ + nWrite >= buf_ + size_)
	{		
		unsigned int t = size_ - (unsigned int)(tail_ - buf_);
		memcpy(tail_, p, t);
		memcpy(buf_, p + t, nWrite - t);
		//move m_tail ptr pos
		tail_ = buf_ + nWrite - t;
	}
	else
	{
		memcpy(tail_, p, nWrite);
		tail_ += nWrite;

		if ( tail_ == buf_ + size_ )
			tail_ = buf_;
	}

	used_		+= nWrite;
	llTotal_	+= nWrite;
	return nWrite;
}

int and_fifobuffer::read(char* p, unsigned int howmuch)
{
	AutoLock lock(&mutex_);

	unsigned int nRead = used_;
	if (howmuch <= nRead)
		nRead = howmuch;

	if (0 == nRead)
		return 0;

	//read data exceed the end of the buffer
	//return to the header,continue reading
	
	if ( header_ + nRead > buf_ + size_ ) {
		//t = the length from header to the tail
		unsigned int t = (unsigned int)(buf_ + size_ - header_);
		memcpy(p, header_, t);
		memcpy(p+t, buf_, nRead-t);
		header_ = buf_ + nRead - t;
	}
	else {
		memcpy(p, header_, nRead);
		header_ += nRead;
		if ( header_ == buf_ + size_)
		{
			header_ = buf_;
		}
	}

	used_  	-= nRead;
	llPos_	+= nRead;
	return nRead;
}

int and_fifobuffer::skip(unsigned int howmuch)
{
	AutoLock lock(&mutex_);
	
	unsigned nRead = used_;
	if( howmuch<=nRead )
		nRead = howmuch;

	if( nRead==0 )
		return 0;

	//if the data which need to omit exceed the end of the buffer
	//set the header back to the front.
	if ( header_+nRead > buf_ + size_ ) {
		unsigned int t = (unsigned int)(buf_ + size_ - header_);
		header_ = buf_ + nRead - t;
	}
	else {
		header_ += nRead;
		if ( header_ == buf_ + size_)
		{
			header_ = buf_;
		}
	}

	used_  -= nRead;
	return nRead;
}

void and_fifobuffer::end()
{
	AutoLock lock(&mutex_);
	eof_ = 1;
}

int and_fifobuffer::is_eof()
{
	AutoLock lock(&mutex_);

	int ret = 0;
	if (eof_ && used() == 0)
		ret = 1;
	
	return ret;
}

int and_fifobuffer::is_empty()
{
	AutoLock lock(&mutex_);

	int ret = 0;
	if (buf_ == 0)
		ret = 1;

	return ret;
}

int and_fifobuffer::size()
{
	AutoLock lock(&mutex_);
	return (int)size_;
}

int and_fifobuffer::used()
{
	AutoLock lock(&mutex_);
	return (int)used_;
}

void and_fifobuffer::reset_impl()
{
	header_	= buf_;
	tail_	= buf_;
	used_	= 0;
	eof_	= 0;
}

