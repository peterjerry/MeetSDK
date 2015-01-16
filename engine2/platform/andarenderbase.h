#ifndef and_a_renderbase_H
#define and_a_renderbase_H

#include <stddef.h>

class FFAudioDecoder;

class and_a_renderbase {
public:
	and_a_renderbase(void)
		:m_dec(NULL), m_benchmark(0)
	{
	}
	
	virtual ~and_a_renderbase(void){};

	virtual int open(int sample_rate, int channel, int bitsPerSample) = 0;
	
	virtual int play() = 0;
	
	virtual int pause() = 0;

	virtual int resume() = 0;

	virtual void close() = 0;

	virtual int setVol(int vol) = 0; // 0-100

	virtual int getVol() = 0;
	
	FFAudioDecoder * get_dec(){return m_dec;}

	void set_dec(FFAudioDecoder *dec){m_dec = dec;}

	void benchmark(){m_benchmark = 1;} 

protected:
	FFAudioDecoder* m_dec;
	int				m_benchmark;
};

#endif //and_a_renderbase_H




