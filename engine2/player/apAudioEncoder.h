#ifndef _AUDIO_ENCODER_H
#define _AUDIO_ENCODER_H

struct AVFormatContext;
struct AVOutputFormat;
struct AVStream;
struct AVFrame;

class apProxyUDP;

#include <stdint.h>
#include <stdarg.h> // for varlist

class apAudioEncoder
{
public:
	apAudioEncoder(void);
	~apAudioEncoder(void);

	bool init(const char *ip_addr, int port, int channels, int sample_rate, int sample_fmt, int bitrate);

	AVStream * add_audiostream(int channels, int sample_rate, int sample_fmt, int bitrate);

	bool write_audio_frame(uint8_t* pBuffer, int datalen);

	void close();
private:
	static int ff_write_packet(void *opaque, uint8_t *buf, int buf_size);

	static void ff_log(void* user, int level, const char* fmt, va_list vl);

	static int encode_interrupt_cb(void *opaque);
private:
	AVFormatContext*	m_ctx;
	AVOutputFormat*		m_ofmt;
	AVStream*			m_audio_st;
	AVFrame*			m_pAudioFrame;
	int					m_audio_frame_size;
	int					m_audio_buf_size;
	uint8_t*			m_audio_buf;
	int					m_audio_buf_offset;
	uint8_t*			m_pb_buf;
	int64_t				m_encodered_frames;
	int64_t				m_dump_bytes;
	bool				m_exit;

	apProxyUDP*			m_dump;

};

#endif // _AUDIO_ENCODER_H
