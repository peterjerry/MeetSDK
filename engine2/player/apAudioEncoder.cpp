#include "apAudioEncoder.h"
#include "apProxyUDP.h"
#include "ppffmpeg.h"
#define LOG_TAG "AudioEncoder"
#include "log.h"

#define PB_BUF_SIZE 65536

apAudioEncoder::apAudioEncoder(void)
	:m_ctx(NULL), m_ofmt(NULL), m_audio_st(NULL), m_pAudioFrame(NULL),
		m_audio_frame_size(0), m_audio_buf_size(0), 
		m_audio_buf(NULL), m_audio_buf_offset(0), 
		m_pb_buf(NULL), m_encodered_frames(0), m_dump_bytes(0), m_exit(false)
{
	av_register_all();
	avformat_network_init();

	av_log_set_callback(ff_log);
}


apAudioEncoder::~apAudioEncoder(void)
{
	close();

	avformat_network_deinit();
}

int apAudioEncoder::ff_write_packet(void *opaque, uint8_t *buf, int buf_size)
{
	apAudioEncoder *pIns = (apAudioEncoder*)opaque;
	if (pIns->m_dump) {
		int written = pIns->m_dump->in((char *)buf, buf_size);
		LOGD("ff_write_packet %d/%d", buf_size, written);
	}
	
	return buf_size;
}

void apAudioEncoder::ff_log(void* user, int level, const char* fmt, va_list vl)
{
	char szLog[2048] = {0};
	vsprintf(szLog, fmt, vl);

	if (strstr(szLog, "first_dts") != NULL)
		return;

	switch (level) {
		case AV_LOG_PANIC:
		case AV_LOG_FATAL:
		case AV_LOG_ERROR:
			LOGE("ffmpeg(%d) %s", level, szLog);
			break;
		case AV_LOG_WARNING:
            LOGW("ffmpeg(%d) %s", level, szLog);
			break;
		case AV_LOG_INFO:
            LOGI("ffmpeg(%d) %s", level, szLog);
			break;
		case AV_LOG_DEBUG:
            LOGD("ffmpeg(%d) %s", level, szLog);
			break;
		case AV_LOG_VERBOSE:
            LOGV("ffmpeg(%d) %s", level, szLog);
			break;
		default:
			LOGI("ffmpeg(%d) %s", level, szLog);
			break;
	}
}

int apAudioEncoder::encode_interrupt_cb(void *opaque)
{
	apAudioEncoder *pIns = (apAudioEncoder *)opaque;

	if (pIns->m_exit)
		return 1;

	return 0;
}

bool apAudioEncoder::init(const char *ip_addr, int port, int channels, int sample_rate, int sample_fmt, int bitrate)
{
	LOGI("audio encoder init: channels %d, sample_rate %d, sample_fmt %d, bitrate %d",
		channels, sample_rate, sample_fmt, bitrate);

	int ret;

	AVIOContext* my_pb = NULL;

	m_ofmt = av_guess_format("mpegts", NULL, NULL);  
	if (!m_ofmt) {
		LOGE("Could not find MPEG-TS muxer.");
		return false;
	}

	ret = avformat_alloc_output_context2(&m_ctx, NULL, m_ofmt->name, NULL);
	if (ret < 0) {
		LOGE("Could not allocated output context");
		return false;
	}

	m_ctx->oformat = m_ofmt;
	m_ctx->oformat->flags |= AVFMT_TS_NONSTRICT;
	m_ctx->interrupt_callback.callback = encode_interrupt_cb;
	m_ctx->interrupt_callback.opaque = this;

	m_audio_st = add_audiostream(channels, sample_rate, sample_fmt, bitrate);
	if (!m_audio_st) {
		LOGE("failed to create audio stream");
		return false;
	}

	m_pb_buf = (uint8_t *)av_mallocz(PB_BUF_SIZE);
	my_pb = avio_alloc_context(m_pb_buf, PB_BUF_SIZE, 1, this, NULL, ff_write_packet, NULL);
	if(!my_pb){
		LOGE("failed to create pb");
		return false;
	}

	m_ctx->pb = my_pb;

	av_dump_format(m_ctx, 0, NULL, 1);

	avformat_write_header(m_ctx, NULL);

	m_dump = new apProxyUDP();
	if (!m_dump->init(ip_addr, port)) {
		LOGE("failed to init udp dump");
		return false;
	}

	m_encodered_frames	= 0;
	m_dump_bytes		= 0;
	m_exit				= false;

	return true;
}

void apAudioEncoder::close()
{
	if (m_ctx == NULL)
		return;

	av_write_trailer(m_ctx);

	//close audio
	if (m_audio_st) {
		avcodec_close(m_audio_st->codec);
		av_free(m_audio_buf);
		av_free(m_pAudioFrame);
	}

	if (m_ctx) {
		/* free the streams */
		for(int i = 0; i < (int)m_ctx->nb_streams; i++) {
			av_freep(&m_ctx->streams[i]->codec);
			av_freep(&m_ctx->streams[i]);
		}
		m_ctx->pb->opaque = NULL;//hard code, need fix
		avio_close(m_ctx->pb);
		av_free(m_ctx);
		m_ctx = NULL;
	}

	if (m_dump) {
		delete m_dump;
		m_dump = NULL;
	}
}

bool apAudioEncoder::write_audio_frame(uint8_t* pBuffer, int datalen)
{
	int got_packet, ret;
	AVPacket pkt = {0};
	av_init_packet(&pkt);

	AVCodecContext* c  = m_audio_st->codec;
	pkt.pts = pkt.dts = 0;

	int left = datalen;
	int offset = 0;

	while (left >= m_audio_buf_size) {
		if (m_audio_buf_offset > 0)
			memcpy(m_audio_buf + m_audio_buf_offset, pBuffer + offset, m_audio_buf_size - m_audio_buf_offset);
		else
			memcpy(m_audio_buf, pBuffer + offset, m_audio_buf_size);
		
		ret = avcodec_encode_audio2(c, &pkt, m_pAudioFrame, &got_packet);
		if (ret < 0) {
			LOGE("Error encoding audio frame");
			return false;
		}

		if (got_packet) {
			pkt.stream_index = m_audio_st->index;
			ret = av_interleaved_write_frame(m_ctx, &pkt);
			if ( ret != 0)
			{
				LOGE("failed to write audio frame. err = %d", ret);
				av_free_packet(&pkt);
				return false;
			}	
		}

		left	-= m_audio_buf_size;
		offset	+= m_audio_buf_size;

		if (m_audio_buf_offset > 0)
			m_audio_buf_offset = 0;
	}

	if (left > 0) {
		memcpy(m_audio_buf, pBuffer + offset, left);
		m_audio_buf_offset = left;
	}

	av_free_packet(&pkt);
	return true;
}

AVStream * apAudioEncoder::add_audiostream(int channels, int sample_rate, int sample_fmt, int bitrate)
{
	AVStream*		st = NULL;
	AVCodecContext*	c = NULL;
	AVCodec*		codec = NULL;
	int				ret;

	codec = avcodec_find_encoder(CODEC_ID_AAC);
	if (!codec) {
		LOGE("Could not find codec.");
		return NULL;
	}

	st = avformat_new_stream(m_ctx, codec);
	if (!st) {
		LOGE("Could not allocate audio stream.");
		return NULL;
	}

	st->id			= 0;
	c				= st->codec;
	c->sample_rate	= sample_rate;
	c->sample_fmt	= (AVSampleFormat)sample_fmt;
	c->channels		= channels;
	c->bit_rate		= bitrate;

	ret = avcodec_open2(c, codec, NULL);
	if (ret < 0) {
		char buf[1024] = "";
		av_strerror(ret, buf, 1024);
		LOGE("FFMPEG cannot open audio codec err:%d(%s), %d, %s", ret, buf, c->codec_id, avcodec_get_name(c->codec_id));
		return NULL;
	}

	m_pAudioFrame = av_frame_alloc();

	m_pAudioFrame->nb_samples		= c->frame_size;
	m_pAudioFrame->format			= c->sample_fmt;
	m_pAudioFrame->channel_layout	= c->channel_layout;

	int len = av_samples_get_buffer_size(NULL, c->channels, 
		c->frame_size, c->sample_fmt, 0);
	if(len <16) {
		LOGE("failed to calc audio frame buffer len:%d", len);
		return NULL;
	}

	m_audio_frame_size = c->frame_size * c->channels * 2;
	m_audio_buf_size = len;
	m_audio_buf = (uint8_t*)av_mallocz(m_audio_buf_size);

	ret = avcodec_fill_audio_frame(m_pAudioFrame, c->channels,
		c->sample_fmt, m_audio_buf, len, 1);
	if(ret < 0) {
		LOGE("failed to assign sample buffer for audio.");
		return NULL;
	}

	return st;
}