#ifndef _PROXY_UDP_
#define _PROXY_UDP_

#include "fifobuffer.h"
#include "ActiveSocket.h"
#include <stdint.h>
#include <pthread.h>

#define WAIT_TIME		1 //msec
#define FIFO_SIZE		1024*1024*2 
#define READ_SIZE_UDP	1316
#define MAX_SENDBUF		65536

class apProxyUDP
{
public:
	apProxyUDP(void);
	~apProxyUDP(void);
	bool init(const char *ip_addr, int port);
	void close();
	int in(char* data, size_t len);
private:
	static void* dump_thread(void* ptr);

	void dump_thread_impl();

	int wait(int64_t usec);
private:
	and_fifobuffer	m_fifo;
	CActiveSocket*	m_udp;
	bool			m_bInit;
	bool			m_bQuit;
	pthread_t		m_thr;
	pthread_mutex_t	m_mutex;
	pthread_cond_t	m_cond;
};

#endif // _PROXY_UDP_