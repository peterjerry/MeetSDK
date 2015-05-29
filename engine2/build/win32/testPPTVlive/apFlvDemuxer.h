#pragma once

#define inline _inline
#ifndef INT64_C
//#define INT64_C(c) (c ## LL)
//#define UINT64_C(c) (c ## ULL)
#define UINT64_C(val) val##ui64
#define INT64_C(val)  val##i64
#endif

extern "C"
{
#include "libavcodec\avcodec.h"
#include "libavformat\avformat.h"
#include "libswscale\swscale.h"
}

typedef int (*ON_FRAME_CALLBACK)(AVPacket *pkt);

class apFlvDemuxer
{
public:
	apFlvDemuxer();
	~apFlvDemuxer(void);

	bool setSource(char *data, int data_len);
	
	void setOnFrame(ON_FRAME_CALLBACK cb) {
		m_callback = cb;
	}

	void demux();

	AVFormatContext * getFmtCtx() {
		return m_fmt_ctx;
	}

protected:
	static int interrupt_l(void* ctx);
	static int ff_read_packet(void *opaque, uint8_t *buf, int buf_size);
	static int64_t ff_seek_packet(void *opaque, int64_t offset, int whence);
	int ff_read_packet_impl(uint8_t *buf, int buf_size);
	int64_t ff_seek_packet_impl(int64_t offset, int whence);
private:
	AVFormatContext*	m_fmt_ctx;
	AVInputFormat*		m_in_fmt;
	void*				m_pb;
	uint8_t*			m_pb_buf;
	uint8_t*			m_data;
	int					m_data_len;
	int					m_data_offset;
	ON_FRAME_CALLBACK	m_callback;
};

