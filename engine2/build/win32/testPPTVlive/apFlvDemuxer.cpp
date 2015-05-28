#include "stdafx.h"
#include "apFlvDemuxer.h"
#include <windows.h>
#define LOG_TAG "apFlvDemuxer"
#include "log.h"

#define PB_BUF_SIZE 65536

int apFlvDemuxer::interrupt_l(void* ctx)
{
    return 0;
}

int apFlvDemuxer::ff_read_packet(void *opaque, uint8_t *buf, int buf_size)
{
	//LOGD("ff_read_packet");

	apFlvDemuxer *pIns = (apFlvDemuxer *)opaque;
	if (pIns)
		return pIns->ff_read_packet_impl(buf, buf_size);

	return 0;
}

int apFlvDemuxer::ff_read_packet_impl(uint8_t *buf, int buf_size)
{
	int to_read = buf_size;
	if (m_data_len - m_data_offset < to_read)
		to_read = m_data_len - m_data_offset;

	memcpy(buf, m_data + m_data_offset, to_read);
	m_data_offset += to_read;
	LOGD("apFlvDemuxer: read_packet %d", to_read);
	return to_read;
}

int64_t apFlvDemuxer::ff_seek_packet(void *opaque, int64_t offset, int whence)
{
	LOGI("FFStream: seek_packet offset %lld, whence %d", offset, whence);
	
	apFlvDemuxer *pIns = (apFlvDemuxer *)opaque;
	if (pIns)
		return pIns->ff_seek_packet_impl(offset, whence);

	return 0;
}

int64_t apFlvDemuxer::ff_seek_packet_impl(int64_t offset, int whence)
{
	if (AVSEEK_SIZE == whence) {
		int64_t size = m_data_len;
		LOGI("AVSEEK_SIZE: filesize %I64d", size);
		return (int64_t)size;
	}
	else if (AVSEEK_FORCE == whence) {
		LOGW("AVSEEK_FORCE");
	}

	if (offset > (int64_t)m_data_len)
		return -1;

	return offset;
}

apFlvDemuxer::apFlvDemuxer()
	:m_fmt_ctx(NULL), m_pb(NULL), m_pb_buf(NULL), m_data(NULL), m_data_len(0), m_data_offset(0),
	m_callback(NULL)
{
	av_register_all();
}

apFlvDemuxer::~apFlvDemuxer(void)
{
}

bool apFlvDemuxer::setSource(char *data, int data_len)
{
	m_data		= (uint8_t *)data;
	m_data_len	= data_len;

	m_fmt_ctx = avformat_alloc_context();
    AVIOInterruptCB cb = {interrupt_l, this};
    m_fmt_ctx->interrupt_callback = cb;

	m_pb_buf	= (unsigned char *)av_mallocz(PB_BUF_SIZE);
	m_pb		= avio_alloc_context(m_pb_buf, PB_BUF_SIZE, 0, this, ff_read_packet,
		NULL, ff_seek_packet);
	if (!m_pb) {
		LOGE("failed to create input pb");
		return false;
	}

	m_fmt_ctx->pb = (AVIOContext *)m_pb;

	m_in_fmt = av_find_input_format("flv");
	if (!m_in_fmt) {
		LOGE("Could not find MPEG-TS demuxer.");
		return false;
	}

	if (avformat_open_input(&m_fmt_ctx, "", m_in_fmt, NULL) != 0) {
        LOGE("failed to open flv");
        return false;
	}

	m_fmt_ctx->max_analyze_duration2 = AV_TIME_BASE * 10;

	// Retrieve stream information after disable variant streams, like m3u8
	if (avformat_find_stream_info(m_fmt_ctx, NULL) < 0) {
        LOGE("failed to avformat_find_stream_info");
        avformat_close_input(&m_fmt_ctx);
        return false;
	}

	av_dump_format(m_fmt_ctx, 0, "", 0);
	return true;
}

void apFlvDemuxer::demux()
{
	AVPacket pkt;
	av_init_packet(&pkt);
	pkt.data = NULL;
	pkt.size = 0;

	int ret;

	while(1) {
		ret = av_read_frame(m_fmt_ctx, &pkt);
		if (ret < 0)
			break;

		if (m_callback) {
			ret = m_callback(&pkt);
			if (ret < 0)
				break;
		}

		av_free_packet(&pkt);
		Sleep(10);
	}
}