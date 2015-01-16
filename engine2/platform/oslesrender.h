#ifndef AND_OSLES_H
#define AND_OSLES_H

class and_fifobuffer;

class and_osles {
public:
	and_osles(void);
	~and_osles(void);

	int open(int sample_rate, int channel, int bitsPerSample);
	
	int play();
	
	int pause();

	int resume();

	void close();

	int setVol(int vol); // 0-100

	int getVol();

	int write_data(const char *buf, int size);

	int free_size();

	int read_data(char *buf, int size);

	char *get_buf(){return m_pBuf;}

	void *get_handle(){return m_osles_handle;}

	int get_latency(); // unit: msec

private:
	
	int createEngine();
	
	int createBufferQueueAudioPlayer(int sample_rate, int channel, int bitsPerSample);

private:
	void*			m_osles_handle;
	and_fifobuffer*	m_fifo;
	char*			m_pBuf;
	int				m_latency;
	int				m_one_sec_byte;
};

#endif //AND_OSLES_H

