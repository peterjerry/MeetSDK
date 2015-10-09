#ifndef _FORMAT_CONVERTER_H_
#define _FORMAT_CONVERTER_H_

#ifdef _MSC_VER
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
#else
#include "ppffmpeg.h"
#endif

class apFormatConverter
{
public:
	apFormatConverter(void);
	~apFormatConverter(void);

	// @param process_timestamp : need process pts in media file
	// @param first segment should be 1 to note down the start_time pts
	bool convert(uint8_t* from, int from_size, uint8_t *to, int *to_size, int process_timestamp, int first_seg);

protected:
	static int interrupt_l(void* ctx);
	static int ff_read_packet(void *opaque, uint8_t *buf, int buf_size);
	static int64_t ff_seek_packet(void *opaque, int64_t offset, int whence);
	static int ff_write_packet(void *opaque, uint8_t *buf, int buf_size);
	int ff_read_packet_impl(uint8_t *buf, int buf_size);
	int64_t ff_seek_packet_impl(int64_t offset, int whence);
	int ff_write_packet_impl(uint8_t *buf, int buf_size);

private:
	static int64_t		m_start_pts;

	AVFormatContext*	m_ifmt_ctx;
	AVInputFormat*		m_in_fmt;
	AVIOContext *		m_in_pb;
	uint8_t*			m_in_pb_buf;
	uint8_t*			m_indata;
	int					m_indata_len;
	int					m_indata_offset;

	AVFormatContext*	m_ofmt_ctx;
	AVOutputFormat*		m_out_fmt;
	AVIOContext *		m_out_pb;
	uint8_t*			m_out_pb_buf;
	uint8_t*			m_outdata;
	int					m_outdata_len;
	int					m_outdata_offset;

	AVBitStreamFilterContext*	m_pBsfc_h264;
};

#endif // _FORMAT_CONVERTER_H_