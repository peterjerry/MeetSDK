#ifndef AND_FIFOBUFFER_H
#define AND_FIFOBUFFER_H

typedef long long filesize_t;
#include <pthread.h>

class and_fifobuffer{
public:
	and_fifobuffer();
	~and_fifobuffer();
	
	int create(unsigned int size);

	void reset();
	void close();
	void end();
		
	int is_empty();
	int size();
	int used();
	filesize_t total_write();
	filesize_t total_read();
	int is_eof();
	char* header();
		
	int write(char* p, unsigned int howmuch);
	int read(char* p, unsigned int howmuch);
	int skip(unsigned int howmuch);
	
private:
	void reset_impl();
private:
    char*				buf_;
	char*				header_;
	char*				tail_;
	unsigned int		size_;
	unsigned int		used_;
	filesize_t			llPos_;					// total read start pos
	filesize_t			llTotal_;				// total write size
	int					eof_;
	pthread_mutex_t		mutex_;					// protect fifo
};
	
#endif // AND_FIFOBUFFER_H

