#include "approxyudp.h"
#include "utils.h"
#define LOG_TAG "proxyUDP"
#include "log.h"

apProxyUDP::apProxyUDP(void)
:m_bInit(false), m_bQuit(false)
{
	pthread_mutex_init(&m_mutex, NULL);
	pthread_cond_init(&m_cond, NULL);
}

apProxyUDP::~apProxyUDP(void)
{
	if (m_bInit)
		close();

	pthread_mutex_destroy(&m_mutex);
	pthread_cond_destroy(&m_cond);
}

bool apProxyUDP::init(const char *ip_addr, int port)
{
	bool ret = false;
	m_fifo.create(FIFO_SIZE);

	m_udp = new CActiveSocket(CSimpleSocket::SocketTypeUdp);
	m_udp->Initialize();

#ifdef _MSC_VER
	ret = m_udp->Open((const uint8 *)"127.0.0.1", port);
#else
	ret = m_udp->Open((const uint8 *)ip_addr, port);
#endif

	if (!ret){
		LOGE("create udp class failed");
		return false;
	}

	if (pthread_create(&m_thr, NULL, dump_thread, this) != 0) {
		LOGE("failed to start thread");
		return false;
	}

	m_bInit = true;
	return true;
}

void* apProxyUDP::dump_thread(void* ptr)
{
	LOGI("udp dump thread started");

	apProxyUDP* proxy = (apProxyUDP *) ptr;
    
    proxy->dump_thread_impl();

	LOGI("udp dump thread exited");
    return NULL;
}

void apProxyUDP::close()
{
	LOGI("before pthread_join %p", m_thr);

	pthread_mutex_lock(&m_mutex);
	pthread_cond_signal(&m_cond);
	m_bQuit = true;
	pthread_mutex_unlock(&m_mutex);

	if (pthread_join(m_thr, NULL) != 0)
		LOGE("failed to join udp dump thread");

	if (m_udp) {
		m_udp->Close();
		delete m_udp;
		m_udp = NULL;
	}
}

int apProxyUDP::in(char* data, size_t len)
{
	int size = m_fifo.write(data, (unsigned int)len);
	if (size < (int)len)
		LOGW("fifo overflow in %d(%d)", size, len);
	else
		LOGD("buffer in %d", len);

	pthread_mutex_lock(&m_mutex);
	pthread_cond_signal(&m_cond);
	pthread_mutex_unlock(&m_mutex);

	return size;
}

int apProxyUDP::wait(int64_t usec)
{
	struct timespec ts;
	ts.tv_sec = usec / 1000000ll; // unit: sec
	ts.tv_nsec = (usec % 1000000ll) * 1000ll;

#if defined(__CYGWIN__) || defined(_MSC_VER) || defined(__aarch64__)
	int64_t now_usec = getNowUs();
	int64_t now_sec = now_usec / 1000000;
	now_usec = now_usec - now_sec * 1000000;
	ts.tv_sec	+= now_sec;
	ts.tv_nsec	+= (long)now_usec * 1000;
			
	return pthread_cond_timedwait(&m_cond, &m_mutex, &ts);
#else
	return pthread_cond_timedwait_relative_np(&m_cond, &m_mutex, &ts);
#endif
}

void apProxyUDP::dump_thread_impl()
{
	char *pData = new char[READ_SIZE_UDP];
	int64_t totalsent = 0;
	int32 n;
	int32 ret;

	pthread_mutex_lock(&m_mutex);

	while (!m_bQuit) {
		while (m_fifo.used() < READ_SIZE_UDP) {
			wait(WAIT_TIME * 1000);

			if (m_bQuit)
				break;
		}

		n = m_fifo.read(pData, READ_SIZE_UDP);
		if (n > 0) {
			if (m_udp) {
				ret = m_udp->Send((uint8 *)pData, n);
				if (ret < 0) {
					LOGE("failed to send udp %d", ret);
					break;
				}

				if (ret == 0) {
					LOGE("udp sent size is 0");
					break;
				}

				totalsent += (unsigned int)ret;
				LOGI("sent udp data end. n1:%d \t n2:%d \t left %d", n, ret, m_fifo.used());
			}
		}
	}

	pthread_mutex_unlock(&m_mutex);

	delete pData;
	pData = NULL;

	LOGI("remain fifo buffer: %d", m_fifo.used());
	LOGI("total write into fifo %I64d, read from fifo %I64d, total sent to socket %I64d",
		m_fifo.total_read(), m_fifo.total_write(), totalsent);
}