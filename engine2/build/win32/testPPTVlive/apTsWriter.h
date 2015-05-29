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
#include "libavfilter\avfilter.h"
}

class apTsWriter
{
public:
	apTsWriter(void);
	~apTsWriter(void);

	bool open(AVFormatContext *in_fmt_ctx, char *file_name);
	bool write_frame(AVPacket *pkt);
	void close();
private:
	AVFormatContext*	m_in_fmt_ctx;
	AVFormatContext*	m_out_fmt_ctx;
	AVOutputFormat*		m_out_fmt;
	AVStream*			m_video_stream;
	AVStream*			m_audio_stream;
	int					m_video_stream_idx;
	int					m_audio_stream_idx;

	AVBitStreamFilterContext*	m_pBsfc_h264;
};

