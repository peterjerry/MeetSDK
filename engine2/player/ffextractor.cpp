#include "ffextractor.h"
#include "ppffmpeg.h"
#include "autolock.h"
#include "utils.h"
#define LOG_TAG "FFExtractor"
#include "log.h"
#include "player.h"

#ifdef __ANDROID__
#include <jni.h>
extern JavaVM* gs_jvm;
#endif

#ifdef _MSC_VER
#ifndef INT64_MIN
#define INT64_MIN        (INT64_C(-9223372036854775807)-1)
#define INT64_MAX        (INT64_C(9223372036854775807))
#endif
#pragma comment(lib, "avutil")
#pragma comment(lib, "avcodec")
#pragma comment(lib, "avformat")

#pragma comment(lib, "pthreadVC2")
#endif

#define MAX_PKT_SIZE (65536 * 4)
#define DEFAULT_MAX_BUFFER_SIZE 1048576

#define MEDIA_OPEN_TIMEOUT_MSEC		(120 * 1000) // 2 min
#define MEDIA_READ_TIMEOUT_MSEC		(300 * 1000) // 5 min

#define VIDOE_POP_AHEAD_MSEC 200

// NAL unit types
enum NALUnitType {
    NAL_SLICE = 1,
    NAL_DPA,
    NAL_DPB,
    NAL_DPC,
    NAL_IDR_SLICE,
    NAL_SEI,
    NAL_SPS,
    NAL_PPS,
    NAL_AUD,
    NAL_END_SEQUENCE,
    NAL_END_STREAM,
    NAL_FILLER_DATA,
    NAL_SPS_EXT,
    NAL_AUXILIARY_SLICE = 19
};

#define BUFFER_FLAG_SYNC_FRAME		1
#define BUFFER_FLAG_CODEC_CONFIG	2
#define BUFFER_FLAG_END_OF_STREAM	4

extern "C" IExtractor* getExtractor()
{
    return new FFExtractor();
}

extern "C" void releaseExtractor(IExtractor* extractor)
{
	if (extractor) {
		delete extractor;
		extractor = NULL;
	}
}

FFExtractor::FFExtractor()
{
	mListener			= NULL;

	m_status			= FFEXTRACTOR_INITED;

	m_sorce_type		= TYPE_UNKNOWN;
	m_url				= NULL;
	m_fmt_ctx			= NULL;
	m_video_stream		= NULL;
	m_video_stream_idx	= -1;
	m_video_dec_ctx		= NULL;

	for (int i=0;i<4;i++) {
		m_video_dst_data[i] = NULL;
		m_video_dst_linesize[i] = 0;
	}

	m_video_dst_bufsize		= 0;
	m_framerate				= 0;
	m_video_clock_msec		= 0;
	m_pBsfc_h264			= NULL;
	m_video_keyframe_sync	= false;
	m_sps_data				= NULL;
	m_sps_size				= 0;
	m_pps_data				= NULL;
	m_pps_size				= 0;

	m_audio_stream			= NULL;
	m_audio_stream_idx		= -1;
	m_audio_dec_ctx			= NULL;
	m_audio_clock_msec		= 0;
	m_pBsfc_aac				= NULL;

	m_frame					= NULL;

	m_video_frame_count		= 0;
	m_audio_frame_count		= 0;

	m_sample_track_idx		= -1;
	m_sample_clock_msec		= 0;
	m_sample_pkt			= NULL;

	pthread_cond_init(&mCondition, NULL);
    pthread_mutex_init(&mLock, NULL);
	pthread_mutex_init(&mLockNotify, NULL);

	m_buffered_size			= 0;
	m_max_buffersize		= DEFAULT_MAX_BUFFER_SIZE;
	m_min_play_buf_count	= 0;
	m_cached_duration_msec	= 0;

	m_seek_flag				= 0;
	m_seek_time_msec		= 0;

	m_open_stream_start_msec= 0;
	//m_read_stream_start_msec= 0;

	m_buffering				= false;
	m_seeking				= false;
	m_eof					= false;

	av_register_all();
	avformat_network_init();
}

FFExtractor::~FFExtractor()
{
	LOGI("FFExtractor destrcutor()");

	close();

	pthread_cond_destroy(&mCondition);
    pthread_mutex_destroy(&mLock);
	pthread_mutex_destroy(&mLockNotify);

	avformat_network_deinit();
	LOGI("FFExtractor destrcutor done");
}

int FFExtractor::interrupt_l(void* ctx)
{
    //LOGD("Checking interrupt_l");
	//return 1: error
    
	FFExtractor* extractor = (FFExtractor*)ctx;
    if (extractor == NULL)
		return 1;

	if (extractor->m_open_stream_start_msec != 0) {
		if (getNowMs() - extractor->m_open_stream_start_msec > MEDIA_OPEN_TIMEOUT_MSEC) {
			LOGE("interrupt_l: open stream time out");
			extractor->notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_TIMED_OUT, 0);
			return 1;
		}
	}

	/*if (extractor->m_status == FFEXTRACTOR_STARTED) {
		if (getNowMs() - extractor->m_read_stream_start_msec > MEDIA_READ_TIMEOUT_MSEC) {
			LOGE("interrupt_l: read stream time out");
			extractor->notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_TIMED_OUT, 0);
			return 1;
		}
	}*/

    if (extractor->m_status == FFEXTRACTOR_STOPPED || extractor->m_status == FFEXTRACTOR_STOPPING) {
        //abort av_read_frame or avformat_open_input, avformat_find_stream_info
        LOGI("interrupt_l: FFSTREAM_STOPPED");
        return 1;
    }
	
	return 0;
}

status_t FFExtractor::setListener(MediaPlayerListener* listener)
{
    mListener = listener;
    return OK;
}

status_t FFExtractor::stop()
{
	m_status = FFEXTRACTOR_STOPPING;
	LOGI("stop preparing media");
	return OK;
}

void FFExtractor::close()
{
	if (FFEXTRACTOR_STOPPED == m_status)
		return;

	LOGI("close()");

	if (m_status == FFEXTRACTOR_STARTED || m_status == FFEXTRACTOR_PAUSED) {
		m_status = FFEXTRACTOR_STOPPING;
		pthread_cond_signal(&mCondition);

		LOGI("stop(): before pthread_join %p", mThread);
		if (pthread_join(mThread, NULL) != 0) {
			LOGE("pthread_join error");
		}

		m_video_q.flush();
		m_audio_q.flush();
		m_cached_duration_msec = 0;
		m_buffered_size = 0;
	}

	if (m_pBsfc_h264) {
		av_bitstream_filter_close(m_pBsfc_h264);
		m_pBsfc_h264 = NULL;
	}
	if (m_pBsfc_aac) {
		av_bitstream_filter_close(m_pBsfc_aac);
		m_pBsfc_aac = NULL;
	}
	if (m_video_dec_ctx) {
		avcodec_close(m_video_dec_ctx);
		m_video_dec_ctx = NULL;
	}
    if (m_audio_dec_ctx) {
		avcodec_close(m_audio_dec_ctx);
		m_audio_dec_ctx = NULL;
	}
	if (m_fmt_ctx) {
		m_fmt_ctx->interrupt_callback.callback = NULL;
		m_fmt_ctx->interrupt_callback.opaque = NULL;
		avformat_close_input(&m_fmt_ctx);
	}
	if (m_frame)
		av_frame_free(&m_frame);
	if (m_video_dst_data[0])
		av_free(m_video_dst_data[0]);

	if (m_url) {
		delete m_url;
		m_url = NULL;
	}

	m_status = FFEXTRACTOR_STOPPED;

	LOGI("close done!");
}

void FFExtractor::notifyListener_l(int msg, int ext1, int ext2)
{
	LOGD("notifyListener_l %p %d %d %d", mListener, msg, ext1, ext2);

	AutoLock lock(&mLockNotify);

    if (mListener != NULL)
        mListener->notify(msg, ext1, ext2);
    else
		LOGE("mListener is null");
}

status_t FFExtractor::setDataSource(const char *path)
{
	LOGI("setDataSource() %s", path);

	/* register all formats and codecs */
    av_register_all();

	if (!path || strcmp(path, "") == 0) {
		LOGE("url is empty");
		return ERROR;
	}

	if (m_url)
		delete m_url;

	int len = strlen(path) + 1;
	m_url = new char[len];
	strcpy(m_url, path);

	if (strncmp(m_url, "/", 1) == 0 || strncmp(m_url, "file://", 7) == 0)
		m_sorce_type = TYPE_LOCAL_FILE;
	else if(strstr(m_url, "type=pplive"))
		m_sorce_type = TYPE_LIVE;
	else
		m_sorce_type = TYPE_VOD;
	
	m_open_stream_start_msec = getNowMs();

	m_fmt_ctx = avformat_alloc_context();
	AVIOInterruptCB cb = {interrupt_l, this};
    m_fmt_ctx->interrupt_callback = cb;

	/* open input file, and allocate format context */
    if (avformat_open_input(&m_fmt_ctx, m_url, NULL, NULL) < 0) {
		LOGE("Could not open source file %s", m_url);
        return ERROR;
    }

	m_open_stream_start_msec = 0;

	/* retrieve stream information */
    if (avformat_find_stream_info(m_fmt_ctx, NULL) < 0) {
        LOGE("Could not find stream information");
        return ERROR;
    }

	// disable all stream at first
	for (unsigned int i=0;i<m_fmt_ctx->nb_streams;i++) {
		m_fmt_ctx->streams[i]->discard = AVDISCARD_ALL;
	}

	/* dump input information to stderr */
	av_dump_format(m_fmt_ctx, 0, m_url, 0);

#if defined(__CYGWIN__)
	if(strstr(m_url, ":/") != NULL) // cygwin local file is like "e:/folder/1.mov"
#elif defined(_MSC_VER)
	if(strstr(m_url, ":\\") != NULL) // win32 local file is like "e:\folder\1.mov"
#else
    if(strncmp(m_url, "/", 1) == 0 || strstr(m_url, "file://") != NULL)
#endif    
		m_min_play_buf_count = 1; // m_framerate * FF_PLAYER_MIN_BUFFER_SECONDS_LOCAL_FILE;
	else
		m_min_play_buf_count = 25 * 4;

	LOGI("setDataSource done");
	m_status = FFEXTRACTOR_PREPARED;
	return OK;
}

status_t FFExtractor::getTrackCount(int32_t *track_count)
{
	if (!m_fmt_ctx) {
		LOGE("media is not opened");
		return ERROR;
	}

	LOGI("nb_streams %d", m_fmt_ctx->nb_streams);
	*track_count = m_fmt_ctx->nb_streams;
	return OK;
}

status_t FFExtractor::getTrackFormat(int32_t index, MediaFormat *format)
{
	if (NULL == format)
		return INVALID_OPERATION;
	
	if (index >= (int32_t)m_fmt_ctx->nb_streams) {
		LOGE("invalid stream index: %d", index);
		return INVALID_OPERATION;
	}

	if (NULL == m_fmt_ctx->streams[index]->codec) {
		LOGE("stream #%d codec not found", index);
		return INVALID_OPERATION;
	}

	LOGI("before getTrackFormat()");

	AVCodecContext *c		= m_fmt_ctx->streams[index]->codec;
	AVMediaType type		= c->codec_type;
	AVCodecID codec_id		= c->codec_id;

	if (AVMEDIA_TYPE_VIDEO == type) {
		format->media_type	= PPMEDIA_TYPE_VIDEO;
		format->codec_id	= (int32_t)codec_id;
		format->width		= c->width;
		format->height		= c->height;
		format->ar			= av_q2d(c->sample_aspect_ratio);
		format->duration_us	= m_fmt_ctx->duration;

		// get pps and sps
		if (strstr(m_fmt_ctx->iformat->name, "matroska") != NULL ||
			strstr(m_fmt_ctx->iformat->name, "mp4") != NULL ||
			strstr(m_fmt_ctx->iformat->name, "flv") != NULL)
		{
			uint8_t unit_nb;
			const uint8_t *extradata = c->extradata+4;  //jump first 4 bytes
			static const uint8_t nalu_header[4] = {0, 0, 0, 1};
			/* retrieve sps and pps unit(s) */  
			unit_nb = *extradata++ & 0x1f; /* number of sps unit(s) */  
			// sps
			format->csd_0_size	= unit_nb;
			format->csd_0 = new uint8_t[unit_nb];
			memset(format->csd_0, 0, unit_nb);
			memcpy(format->csd_0, nalu_header, 4);
			memcpy(format->csd_0 + 4, extradata + 3, unit_nb - 4);
			// pps
			format->csd_1_size	= 9;
			format->csd_1 = new uint8_t[9];
			memset(format->csd_1, 0, 9);
			memcpy(format->csd_1, nalu_header, 4);
			memcpy(format->csd_1 + 4, extradata + unit_nb + 3, 5);
		}
		else if (strstr(m_fmt_ctx->iformat->name, "mpegts") != NULL ||
			strstr(m_fmt_ctx->iformat->name, "hls,applehttp") != NULL) 
		{	
			m_fmt_ctx->streams[index]->discard = AVDISCARD_NONE;

			AVPacket pkt;
			av_init_packet(&pkt);
			pkt.size = 0;
			pkt.data = NULL;

			int ret;
			while (m_sps_data == NULL || m_pps_data == NULL) {
				LOGI("read frame");
				ret = av_read_frame(m_fmt_ctx, &pkt);
				if (ret == AVERROR_EOF) {
					LOGE("find sps and pps: av_read_frame() eof");
					break;
				}
				else if (ret < 0) {
					char msg[128] = {0};
					av_make_error_string(msg, 128, ret);
					LOGE("find sps and pps: failed to read frame %d %s", ret, msg);
					break;
				}

				if (pkt.stream_index == index) {
					find_sps_pps(&pkt);
				}

				av_free_packet(&pkt);
			}

			if (!m_sps_data || !m_pps_data) {
				LOGE("failed to find sps and pps");
				return ERROR;
			}

			if (av_seek_frame(m_fmt_ctx, -1, 0, AVSEEK_FLAG_BACKWARD) < 0) {
				LOGE("failed to seekback to head");
				return ERROR;
			}

			m_fmt_ctx->streams[index]->discard = AVDISCARD_ALL;

			format->csd_0		= m_sps_data;
			format->csd_0_size	= m_sps_size;
			format->csd_1		= m_pps_data;
			format->csd_1_size	= m_pps_size;
		}
		else {
			LOGE("unsupported media format %s", m_fmt_ctx->iformat->name);
			return ERROR;
		}

		float frame_rate;
		AVRational fr;
		fr = av_guess_frame_rate(m_fmt_ctx, m_fmt_ctx->streams[index], NULL);
		//@return the guessed (valid) frame rate, 0/1 if no idea
        if (fr.num == 0 && fr.den == 1) {
			frame_rate = 25.0f;
		}
		else {
            frame_rate = (float)av_q2d(fr);
            if (frame_rate > 100.0f || frame_rate <= 0.0f)
				frame_rate = 25.0f;
		}

		format->frame_rate = frame_rate;
	}
	else if (AVMEDIA_TYPE_AUDIO == type) {
		format->media_type		= PPMEDIA_TYPE_AUDIO;
		format->codec_id		= (int32_t)codec_id;
		format->channels		= c->channels;
		format->channel_layout	= c->channel_layout;
		format->sample_rate		= c->sample_rate;
		format->sample_fmt		= (int)c->sample_fmt;
		format->duration_us		= m_fmt_ctx->duration;
		if (c->extradata && c->extradata_size > 0) {
			LOGI("audio extradata %p, size %d", c->extradata, c->extradata_size);
			format->csd_0_size = c->extradata_size;
			format->csd_0 = new uint8_t[c->extradata_size];
			memcpy(format->csd_0, c->extradata, c->extradata_size);
		}
		else {
			// get aac extradata

			// method 1
			int16_t extradata = get_aac_extradata(c);
			int len	= 2;

			format->csd_0_size		= len;
			format->csd_0			= new uint8_t[len];
			memcpy(format->csd_0, (uint8_t *)&extradata + 1, 1);
			memcpy(format->csd_0 + 1, (uint8_t *)&extradata, 1);

			/*
			// method 2
			m_fmt_ctx->streams[index]->discard = AVDISCARD_NONE;

			m_pBsfc_aac =  av_bitstream_filter_init("aac_adtstoasc");
			if (!m_pBsfc_aac) {
				LOGE("Could not aquire aac_adtstoasc filter");
				return ERROR;
			}

			AVPacket pkt;
			av_init_packet(&pkt);
			pkt.size = 0;
			pkt.data = NULL;

			int ret;
			bool found = false;
			while (!found) {
				ret = av_read_frame(m_fmt_ctx, &pkt);
				if (ret == AVERROR_EOF) {
					LOGE("find sps and pps: av_read_frame() eof");
					break;
				}
				else if (ret < 0) {
					char msg[128] = {0};
					av_make_error_string(msg, 128, ret);
					LOGE("find aac extradata: failed to read frame %d %s", ret, msg);
					break;
				}

				if (index == pkt.stream_index) {
					uint8_t *pOutBuf = NULL;
					int outBufSize;
					int isKeyFrame = pkt.flags & AV_PKT_FLAG_KEY;
					av_bitstream_filter_filter(m_pBsfc_aac, m_fmt_ctx->streams[index]->codec, NULL, &pOutBuf, &outBufSize, 
						pkt.data, pkt.size, isKeyFrame);

					if (c->extradata_size) {
						format->csd_0_size = c->extradata_size;
						format->csd_0 = new uint8_t[c->extradata_size];
						memcpy(format->csd_0, c->extradata, c->extradata_size);

						av_free(pOutBuf);
						LOGI("audio extradata found size %d, 0x%02x 0x%02x", c->extradata_size, format->csd_0[0], format->csd_0[1]);
						found = true;
					}
				}
			} // end of while

			if (format->csd_0_size == 0 || format->csd_0 == NULL) {
				LOGE("failed to find audio extradata");
				return ERROR;
			}

			if (av_seek_frame(m_fmt_ctx, -1, 0, AVSEEK_FLAG_BACKWARD) < 0) {
				LOGE("failed to seekback to head");
				return ERROR;
			}

			if (m_pBsfc_aac) {
				av_bitstream_filter_close(m_pBsfc_aac);
				m_pBsfc_aac = NULL;
			}

			m_fmt_ctx->streams[index]->discard = AVDISCARD_ALL;*/
		}
	}
	else if (AVMEDIA_TYPE_SUBTITLE == type) {
		// to do
		format->media_type	= PPMEDIA_TYPE_SUBTITLE;
	}

	return OK;
}

int16_t FFExtractor::get_aac_extradata(AVCodecContext *c)
{
	int16_t aacObjectType = 2; // 2: AAC LC (Low Complexity)
	int16_t sampleRateIdx;
	int16_t numChannels;

	/*Sampling Frequencies
	0: 96000 Hz
	1: 88200 Hz
	2: 64000 Hz
	3: 48000 Hz
	4: 44100 Hz
	5: 32000 Hz
	6: 24000 Hz
	7: 22050 Hz
	8: 16000 Hz
	9: 12000 Hz
	10: 11025 Hz
	11: 8000 Hz
	12: 7350 Hz
	13: Reserved
	14: Reserved
	15: frequency is written explictly
	*/
	switch(c->sample_rate / 2) {
	case 96000:
		sampleRateIdx = 0;
		break;
	case 88200:
		sampleRateIdx = 1;
		break;
	case 64000:
		sampleRateIdx = 2;
		break;
	case 48000:
		sampleRateIdx = 3;
		break;
	case 44100:
		sampleRateIdx = 4;
		break;
	case 32000:
		sampleRateIdx = 5;
		break;
	case 24000:
		sampleRateIdx = 6;
		break;
	case 22050:
		sampleRateIdx = 7;
		break;
	case 16000:
		sampleRateIdx = 8;
		break;
	case 12000:
		sampleRateIdx = 9;
		break;
	case 11025:
		sampleRateIdx = 10;
		break;
	case 8000:
		sampleRateIdx = 11;
		break;
	case 7350:
		sampleRateIdx = 12;
		break;
	default:
		LOGE("unsupported audio sample rate %lld", c->sample_rate);
		return ERROR;
	}

	/*Channel Configurations
	0: Defined in AOT Specifc Config
	1: 1 channel: front-center
	2: 2 channels: front-left, front-right
	3: 3 channels: front-center, front-left, front-right
	4: 4 channels: front-center, front-left, front-right, back-center
	5: 5 channels: front-center, front-left, front-right, back-left, back-right
	6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
	7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel
	8-15: Reserved*/
	switch(c->channel_layout) {
	case AV_CH_LAYOUT_MONO:
		numChannels = 1; 
		break;
	case AV_CH_LAYOUT_STEREO:
		numChannels = 2; 
		break;
	case AV_CH_LAYOUT_2POINT1:
	case AV_CH_LAYOUT_2_1:
	case AV_CH_LAYOUT_SURROUND:
		numChannels = 3; 
		break;
	case AV_CH_LAYOUT_4POINT0:
		numChannels = 4; 
		break;
	case AV_CH_LAYOUT_5POINT0_BACK:
		numChannels = 5; 
		break;
	case AV_CH_LAYOUT_5POINT1_BACK:
		numChannels = 6; 
		break;
	default:
		LOGE("unsupported audio channel layout %lld", c->channel_layout);
		return ERROR;
	}

	return (aacObjectType << 11) | (sampleRateIdx << 7) | (numChannels << 3);
}

status_t FFExtractor::selectTrack(int32_t index)
{
	if (index < 0 || index >= (int)m_fmt_ctx->nb_streams) {
		LOGE("invalid stream idx: %d to selectTrack", index);
		return ERROR;
	}

	if (open_codec_context_idx(index) < 0) {
		LOGE("failed to select track #%d", index);
		return ERROR;
	}

	return OK;
}

status_t FFExtractor::unselectTrack(int32_t index)
{
	if (index >= (int)m_fmt_ctx->nb_streams) {
		LOGE("invalid stream idx: %d to unselectTrack", index);
		return ERROR;
	}

	m_fmt_ctx->streams[index]->discard = AVDISCARD_ALL;
	return OK;
}

status_t FFExtractor::seekTo(int64_t timeUs, int mode)
{
	LOGI("extractor op seekTo() pos %lld, mode %d", timeUs, mode);

	AutoLock autoLock(&mLock);
	
	int incr;
	if (timeUs > m_sample_clock_msec * 1000)
		incr = 1;
	else
		incr = -1;

    m_seek_time_msec	= timeUs / 1000;
    m_seeking			= true;
    m_eof				= false;
	m_seek_flag			= incr < 0 ? AVSEEK_FLAG_BACKWARD : 0;

	LOGI("seekTimeMs %lld(%lld sec), incr %d", m_seek_time_msec, m_seek_time_msec / 1000, incr);

    pthread_cond_signal(&mCondition);
	return OK;
}

bool FFExtractor::seek_l()
{
	int stream_index = -1;
	int64_t seek_target = m_seek_time_msec * AV_TIME_BASE / 1000;

	if (m_video_stream_idx >= 0)
		stream_index = m_video_stream_idx;
	else if (m_audio_stream_idx >= 0)
		stream_index = m_audio_stream_idx;

	if (stream_index < 0) {
		LOGW("no stream to seek");
		return false;
	}

#ifdef _MSC_VER
	AVRational ra;
	ra.num = 1;
	ra.den = AV_TIME_BASE;
	seek_target= av_rescale_q(seek_target, ra, m_fmt_ctx->streams[stream_index]->time_base);
#else
	seek_target= av_rescale_q(seek_target, AV_TIME_BASE_Q, m_fmt_ctx->streams[stream_index]->time_base);
#endif

	pthread_mutex_lock(&mLock);
	m_seeking = true;
	pthread_cond_signal(&mCondition);
	pthread_mutex_unlock(&mLock);

    if (av_seek_frame(m_fmt_ctx, stream_index, seek_target, m_seek_flag) < 0) {
        LOGW("failed to seek to: %lld msec", m_seek_time_msec);
		return false;
    }
				
    LOGI("after seek to :%lld msec", m_seek_time_msec);

	flush_l();

	LOGI("put flush packet"); 

	if (m_video_stream) {
		AVPacket* flush_pkt = (AVPacket*)av_malloc(sizeof(AVPacket));
		av_init_packet(flush_pkt);

		flush_pkt->data = (uint8_t*)"FLUSH";
		flush_pkt->size = 5;

		m_video_q.put(flush_pkt);
	}

	if (m_audio_stream) {
		AVPacket* flush_pkt = (AVPacket*)av_malloc(sizeof(AVPacket));
		av_init_packet(flush_pkt);

		flush_pkt->data = (uint8_t*)"FLUSH";
		flush_pkt->size = 5;

		m_audio_q.put(flush_pkt);
	}

	return true;
}

void FFExtractor::flush_l()
{
	m_video_q.flush();
	m_audio_q.flush();
    m_buffered_size = 0;
	m_cached_duration_msec = m_seek_time_msec;
}

status_t FFExtractor::advance()
{
	LOGD("advance()");

	if (m_sample_pkt) {
		if (m_sample_pkt->data && strncmp((const char *)m_sample_pkt->data, "FLUSH", 5) != 0)
			av_free_packet(m_sample_pkt);
		av_free(m_sample_pkt);
		m_sample_pkt = NULL;
	}

	if (m_video_q.count() == 0 || m_audio_q.count() == 0) {
		if (m_eof) {
			LOGI("advance meet eof");
			return OK;
		}

		m_buffering = true;

		LOGI("notifyListener_l MEDIA_INFO_BUFFERING_START");
		notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_START);

		LOGI("start to buffering");
		while (m_buffering) {
			av_usleep(10000); // 10 msec
			if (FFEXTRACTOR_STOPPING == m_status) {
				LOGI("advance was interrputd by stop");
				return ERROR;
			}
		}

		LOGI("notifyListener_l MEDIA_INFO_BUFFERING_END");
		notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_END);
	}

	int64_t video_msec, audio_msec;
	AVPacket *tmpPkt = NULL;
	tmpPkt = m_video_q.peek();
	if (tmpPkt == NULL) {
		LOGE("failed to peek video pkt");
		return ERROR;
	}

	if (tmpPkt->data && strncmp((const char *)tmpPkt->data, "FLUSH", 5) == 0) {
		LOGI("found video flush pkt");
		m_sample_pkt = m_video_q.get();
		m_sample_track_idx	= m_sample_pkt->stream_index;
		return OK;
	}

	video_msec = get_packet_pos(tmpPkt);
	
	tmpPkt = m_audio_q.peek();
	if (tmpPkt == NULL) {
		LOGE("failed to peek video pkt");
		return ERROR;
	}

	if (tmpPkt->data && strncmp((const char *)tmpPkt->data, "FLUSH", 5) == 0) {
		LOGI("found audio flush pkt");
		m_sample_pkt = m_audio_q.get();
		m_sample_track_idx	= m_sample_pkt->stream_index;
		return OK;
	}

	audio_msec = get_packet_pos(tmpPkt);

	if (video_msec == AV_NOPTS_VALUE) {
		LOGW("video pts is AV_NOPTS_VALUE, use last corrent value");
		video_msec = m_video_clock_msec;
	}
	else {
		m_video_clock_msec = video_msec;
	}

	if (audio_msec == AV_NOPTS_VALUE) {
		LOGW("audio pts is AV_NOPTS_VALUE, use last corrent value");
		audio_msec = m_audio_clock_msec;
	}
	else {
		m_audio_clock_msec = audio_msec;
	}

#ifdef _MSC_VER
	LOGD("v_a msec %I64d %I64d", video_msec, audio_msec);
#else
	LOGD("v_a msec %lld %lld", video_msec, audio_msec);
#endif

	if (video_msec - audio_msec < VIDOE_POP_AHEAD_MSEC)
		m_sample_pkt = m_video_q.get();
	else
		m_sample_pkt = m_audio_q.get();

	if (m_sample_pkt == NULL) {
		LOGE("failed to get packet");
		return ERROR;
	}

	AutoLock autoLock(&mLock);
	pthread_cond_signal(&mCondition);

	m_sample_track_idx		= m_sample_pkt->stream_index;
	m_buffered_size			-= m_sample_pkt->size;
	m_cached_duration_msec	-= get_packet_duration(m_sample_pkt);
	
	if (m_video_stream_idx == m_sample_track_idx)
		m_sample_clock_msec = m_video_clock_msec;
	else if (m_audio_stream_idx == m_sample_track_idx)
		m_sample_clock_msec = m_audio_clock_msec;
	else {
		LOGE("invalid sample stream index %d", m_sample_track_idx);
		return ERROR;
	}

	return OK;
}

status_t FFExtractor::readSampleData(unsigned char *data, int32_t *sampleSize)
{
	LOGD("readSampleData()");
	
	if (start() < 0)
		return ERROR;

	if (!is_packet_valid()) {
		if (m_eof)
			return READ_EOF;
		
		return ERROR;
	}

	if (m_video_stream_idx == m_sample_pkt->stream_index) {
		if (m_sample_pkt->data && strncmp((const char *)m_sample_pkt->data, "FLUSH", 5) != 0 && m_pBsfc_h264) {
			// Apply MP4 to H264 Annex B filter on buffer
			//int origin_size = m_sample_pkt->size;
			int isKeyFrame = m_sample_pkt->flags & AV_PKT_FLAG_KEY;
			av_bitstream_filter_filter(m_pBsfc_h264, m_video_stream->codec, NULL, &m_sample_pkt->data, &m_sample_pkt->size, 
				m_sample_pkt->data, m_sample_pkt->size, isKeyFrame);
			//LOGD("readSampleData_flt(video) pkt size %d, outbuf size %d", origin_size, m_sample_pkt->size);
		}

		memcpy(data, m_sample_pkt->data, m_sample_pkt->size);
		*sampleSize = m_sample_pkt->size;
	}
	else if (m_audio_stream_idx == m_sample_pkt->stream_index) {
		if (m_sample_pkt->data && strncmp((const char *)m_sample_pkt->data, "FLUSH", 5) != 0 && m_pBsfc_aac) {
			// Apply aac adts to asc filter on buffer
			int isKeyFrame = m_sample_pkt->flags & AV_PKT_FLAG_KEY;
			//int origin_size = m_sample_pkt->size;
			av_bitstream_filter_filter(m_pBsfc_aac, m_audio_stream->codec, NULL, &m_sample_pkt->data, &m_sample_pkt->size, 
				m_sample_pkt->data, m_sample_pkt->size, isKeyFrame);
			//LOGD("readSampleData_flt(audio) pkt size %d, outbuf size %d", origin_size, m_sample_pkt->size);
		}
		
		memcpy(data, m_sample_pkt->data, m_sample_pkt->size);
		*sampleSize = m_sample_pkt->size;
	}
	else {
		LOGE("unknown stream index #%d", m_sample_pkt->stream_index);
		return ERROR;
	}

	return OK;
}

bool FFExtractor::is_packet_valid()
{
	return (NULL != m_sample_pkt);
}

status_t FFExtractor::getSampleTrackIndex(int32_t *trackIndex)
{
	if (start() < 0)
		return ERROR;

	*trackIndex = m_sample_track_idx;
	return OK;
}

status_t FFExtractor::getSampleTime(int64_t *sampleTimeUs)
{
	if (!is_packet_valid())
		return ERROR;

	*sampleTimeUs = m_sample_clock_msec * 1000;
	return OK;
}

status_t FFExtractor::getSampleFlags(uint32_t *sampleFlags)
{
	if (!is_packet_valid())
		return ERROR;

	//int sync = (m_sample_pkt->flags & AV_PKT_FLAG_KEY);
	//int codec_config = 2;
	//int eof = 0;//(m_eof ? 4 : 0);

	int ret = 0;
	if (m_sorce_type == TYPE_LOCAL_FILE)
		ret |= BUFFER_FLAG_SYNC_FRAME;

	*sampleFlags = ret;
	return OK;
}

status_t FFExtractor::getCachedDuration(int64_t *durationUs, bool *eos)
{
	if (NULL == durationUs || NULL == eos)
		return ERROR;

	*durationUs = m_cached_duration_msec * 1000;
	*eos = (m_eof && m_video_q.count() == 0 && m_audio_q.count() == 0);
	return OK;
}

int FFExtractor::open_codec_context(int *stream_idx, int media_type)
{
    int ret;
    AVStream *st;
    AVCodecContext *dec_ctx = NULL;
    AVCodec *dec = NULL;
    AVDictionary *opts = NULL;

	enum AVMediaType type = (AVMediaType)media_type;

    ret = av_find_best_stream(m_fmt_ctx, type, -1, -1, NULL, 0);
    if (ret < 0) {
		LOGE("Could not find best %s stream in input file '%s'",
			av_get_media_type_string(type), m_url);
        return ret;
    } else {
        *stream_idx = ret;
        st = m_fmt_ctx->streams[*stream_idx];

        /* find decoder for the stream */
        dec_ctx = st->codec;
        dec = avcodec_find_decoder(dec_ctx->codec_id);
        if (!dec) {
            LOGE("Failed to find %s codec", av_get_media_type_string(type));
            return AVERROR(EINVAL);
        }

        /* Init the decoders, with or without reference counting */
        av_dict_set(&opts, "refcounted_frames", "1", 0);
        if ((ret = avcodec_open2(dec_ctx, dec, &opts)) < 0) {
            LOGE("Failed to open %s codec", av_get_media_type_string(type));
            return ret;
        }
    }

    return 0;
}

int FFExtractor::open_codec_context_idx(int stream_idx)
{
    int ret;
    AVStream *st = NULL;
    AVCodecContext *dec_ctx = NULL;
    AVCodec *dec = NULL;
    AVDictionary *opts = NULL;

	if (stream_idx < 0 || stream_idx >= (int)m_fmt_ctx->nb_streams) {
		LOGE("stream id #%d is invalid, nb_streams %d", stream_idx, m_fmt_ctx->nb_streams);
		return -1;
	}
    
	st = m_fmt_ctx->streams[stream_idx];
	st->discard = AVDISCARD_NONE;

    /* find decoder for the stream */
    dec_ctx = st->codec;
	AVMediaType type = dec_ctx->codec_type;
    dec = avcodec_find_decoder(dec_ctx->codec_id);
    if (!dec) {
        LOGE("Failed to find %s codec", av_get_media_type_string(type));
        return AVERROR(EINVAL);
    }

    /* Init the decoders, with or without reference counting */
    av_dict_set(&opts, "refcounted_frames", "1", 0);
    if ((ret = avcodec_open2(dec_ctx, dec, &opts)) < 0) {
        LOGE("Failed to open %s codec", av_get_media_type_string(type));
        return ret;
    }

	if (AVMEDIA_TYPE_VIDEO == type) {
		m_video_stream		= st;
		m_video_stream_idx	= stream_idx;
		m_video_dec_ctx		= m_video_stream->codec;

		if (m_video_dec_ctx->extradata)
			LOGI("extradata %p, extradata_size %d", m_video_dec_ctx->extradata, m_video_dec_ctx->extradata_size);

		if (strstr(m_fmt_ctx->iformat->name, "matroska") != NULL ||
				strstr(m_fmt_ctx->iformat->name, "mp4") != NULL ||
				strstr(m_fmt_ctx->iformat->name, "flv") != NULL)
		{
			// Retrieve required h264_mp4toannexb filter
			m_pBsfc_h264 = av_bitstream_filter_init("h264_mp4toannexb");
			if (!m_pBsfc_h264) {
				LOGE("Could not aquire h264_mp4toannexb filter");
				return ERROR;
			}
		}

        // allocate image where the decoded image will be put
        ret = av_image_alloc(m_video_dst_data, m_video_dst_linesize,
                             m_video_dec_ctx->width, m_video_dec_ctx->height,
                             m_video_dec_ctx->pix_fmt, 1);
        if (ret < 0) {
            LOGE("Could not allocate raw video buffer");
			return ERROR;
        }
        m_video_dst_bufsize = ret;

		m_framerate = 25;//default
		AVRational fr;
		fr = av_guess_frame_rate(m_fmt_ctx, m_video_stream, NULL);
		if (fr.num > 0 && fr.den > 0) {
			m_framerate = fr.num / fr.den;
			if(m_framerate > 100 || m_framerate <= 0)
				m_framerate = 25;
		}

		LOGI("media framerate: %d", m_framerate);

		if (strncmp(m_url, "/", 1) != 0 && strstr(m_url, "file://") == NULL)  
			m_min_play_buf_count = m_framerate * 4;
	}
	else if (AVMEDIA_TYPE_AUDIO == type) {
		m_audio_stream		= st;
		m_audio_stream_idx	= stream_idx;
		m_audio_dec_ctx		= m_audio_stream->codec;

		if (!m_audio_dec_ctx->extradata || m_audio_dec_ctx->extradata_size == 0) {
			m_pBsfc_aac =  av_bitstream_filter_init("aac_adtstoasc");
			if (!m_pBsfc_aac) {
				LOGE("Could not aquire aac_adtstoasc filter");
				return ERROR;
			}
		}
	}
	else {
		LOGE("#%d stream is a %s stream(not video or audio)", stream_idx, av_get_media_type_string(type));
		return -1;
	}

    return 0;
}

void* FFExtractor::demux_thread(void* ptr)
{
	LOGI("demux thread started");
    FFExtractor* extractor = (FFExtractor*)ptr;
    extractor->thread_impl();
	LOGI("demux thread exited");
    return NULL;
}

int64_t FFExtractor::get_packet_duration(AVPacket *pPacket)
{
	int stream_idx = pPacket->stream_index;
	AVRational timebase = m_fmt_ctx->streams[stream_idx]->time_base;
	return (int64_t)((double)pPacket->duration * 1000 * av_q2d(timebase));
}

int64_t FFExtractor::get_packet_pos(AVPacket *pPacket)
{
	//update cached duration
    int64_t pts = AV_NOPTS_VALUE;
    if(pPacket->pts != AV_NOPTS_VALUE)
        pts = pPacket->pts;
    else if (pPacket->dts == AV_NOPTS_VALUE)
        pts = pPacket->dts;

	if (pts != AV_NOPTS_VALUE) {
		AVRational timebase = m_fmt_ctx->streams[pPacket->stream_index]->time_base;
		pts = (int64_t)(pts * 1000 * av_q2d(timebase));
	}

	return pts;
}

/**
 *  Add ADTS header at the beginning of each and every AAC packet.
 *  This is needed as MediaCodec encoder generates a packet of raw
 *  AAC data.
 *
 *  Note the packetLen must count in the ADTS header itself.
 **/
void FFExtractor::addADTStoPacket(uint8_t *packet, int packetLen)
{
    int profile = 2;  //AAC LC
                      //39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
    int freqIdx = 4;  //44.1KHz
    int chanCfg = 2;  //CPE

    // fill in ADTS data
    packet[0] = 0xFF;
    packet[1] = 0xF9;
    packet[2] = (((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
    packet[3] = (((chanCfg&3)<<6) + (packetLen>>11));
    packet[4] = ((packetLen & 0x7FF) >> 3);
    packet[5] = (((packetLen & 7)<<5) + 0x1F);
    packet[6] = 0xFC;
}

int FFExtractor::start()
{
	if (FFEXTRACTOR_STARTED == m_status || FFEXTRACTOR_PAUSED == m_status)
		return 0;

	if (!m_audio_stream && !m_video_stream) {
		LOGE("both audio and video stream was not set, aborting");
		return -1;
	}

	m_frame = av_frame_alloc();
	if (!m_frame) {
		LOGE("Could not allocate frame");
		return -1;
	}

	pthread_create(&mThread, NULL, demux_thread, this);

	m_buffering = true;
	m_status = FFEXTRACTOR_STARTED;
	advance();

	return 0;
}

void FFExtractor::find_sps_pps(AVPacket *pPacket)
{
	static const uint8_t nalu_header[4] = {0x00, 0x00, 0x00, 0x01};
				
	int32_t last_nalu_start = 0;
	for(int32_t offset=0; offset < pPacket->size; offset++ ) {
		if (memcmp(pPacket->data + offset, nalu_header, 4) == 0) {
			if (last_nalu_start != 0 || offset == pPacket->size - 1) {
				uint8_t* pNAL = NULL;
				int32_t sizeNAL = 0;

				// 00 00 00 00 xx data ...
				pNAL = pPacket->data + last_nalu_start;
				sizeNAL = offset - last_nalu_start;

				//int32_t nal_ref_idc   = pNAL[4] >> 5;
				int32_t nal_unit_type = pNAL[4] & 0x1F;
				LOGI("nalType: %d", nal_unit_type);
				if (nal_unit_type == NAL_SPS) {
					if (m_sps_data == NULL) {
						m_sps_data = new uint8_t[sizeNAL];
						memcpy(m_sps_data, pNAL, sizeNAL);
						m_sps_size = sizeNAL;
						LOGI("sps data 0x%02x 0x%02x 0x%02x 0x%02x ,size %d", pNAL[5], pNAL[6], pNAL[7], pNAL[8], sizeNAL);
					}
				}
				else if (nal_unit_type == NAL_PPS) {
					if (m_pps_data == NULL) {
						m_pps_data = new uint8_t[sizeNAL];
						memcpy(m_pps_data, pNAL, sizeNAL);
						m_pps_size = sizeNAL;
						LOGI("pps data 0x%02x 0x%02x 0x%02x 0x%02x ,size %d", pNAL[5], pNAL[6], pNAL[7], pNAL[8], sizeNAL);
					}
				}
			}

			last_nalu_start = offset;
		}
	}
}

void FFExtractor::thread_impl()
{
	int ret;

	LOGI("FFExtractor start to demux media");
	
	// fix MEDIA_INFO_BUFFERING_END sent before MEDIA_INFO_BUFFERING_START problem
	//notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_START);

	while (1) {
		if (FFEXTRACTOR_STOPPING == m_status || FFEXTRACTOR_STOPPING ==  m_status) {
            LOGI("FFExtractor is stopping");
            break;
        }

		if (m_seeking) {
			m_video_keyframe_sync = false;

			seek_l();

			notifyListener_l(MEDIA_SEEK_COMPLETE, 0);
			m_seeking = false;
		}

		if (m_buffering) {
			bool video_enough = ((m_video_stream != NULL) ? (m_video_q.count() > m_min_play_buf_count) : true);
			bool audio_enough = false;
			double min_duration = 0.0, a_duration = 0.0;

			if (m_audio_stream == NULL)
				audio_enough = true;
			else {
				min_duration = m_min_play_buf_count * 1000 / (double)m_framerate;
				a_duration = m_audio_q.duration() * 1000 * av_q2d(m_audio_stream->time_base);
				if (a_duration >= min_duration)
					audio_enough = true;
			}

			if ( video_enough && audio_enough) {
				// packet queue is enough for play
				m_buffering = false;
			}
		}

		if (m_buffered_size > m_max_buffersize) {
            LOGD("Buffering reaches max size %d %d, vQueueSize %d, aQueueSize %d", 
				m_buffered_size, m_max_buffersize, m_video_q.count(), m_audio_q.count());
				
			if (m_buffering) {
                m_max_buffersize *= 2;
                LOGI("Double max buffer size to: %d", m_max_buffersize);
            }
            else {
				// too much data to decode, just wait for decoder consuming some data
				while (m_buffered_size > m_max_buffersize) {
					struct timespec ts;
					ts.tv_sec = 0;
					ts.tv_nsec = 250000000ll; // 250 msec
					AutoLock autoLock(&mLock);
#if defined(__CYGWIN__) || defined(_MSC_VER)
					int64_t now_usec = getNowUs();
					int64_t now_sec = now_usec / 1000000;
					now_usec = now_usec - now_sec * 1000000;
					ts.tv_sec	+= now_sec;
					ts.tv_nsec	+= (long)now_usec * 1000;
					pthread_cond_timedwait(&mCondition, &mLock, &ts);
#else
					pthread_cond_timedwait_relative_np(&mCondition, &mLock, &ts);
#endif
					if (FFEXTRACTOR_STOPPING == m_status || m_seeking || m_buffering) {
						LOGI("buffer too much sleep was interrputed by stoping || seek || buffer");
						break;
					}
				}
            }

            continue;
        }

		// ready to read frame
		AVPacket* pPacket = (AVPacket*)av_malloc(sizeof(AVPacket));
		memset(pPacket, 0, sizeof(AVPacket));

		ret = av_read_frame(m_fmt_ctx, pPacket);
		if (ret == AVERROR_EOF) {
			av_free_packet(pPacket);
			av_free(pPacket);
			m_eof = true;
			LOGI("av_read_frame() eof");

			// 2014.8.25 guoliangma added, to fix cannot play clip which duration is less than 3sec
			if (m_buffering) {
				m_buffering = false;
				LOGI("MEDIA_INFO_BUFFERING_END because of stream end");
				notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_END);
			}

			// continue for seek back
			av_usleep(10 * 1000); // 10 msec
			continue;
		}
		else if (ret < 0) {
			char msg[128] = {0};
			av_make_error_string(msg, 128, ret);
			LOGE("failed to read frame %d %s", ret, msg);
			break;
		}

		if (pPacket->stream_index == m_video_stream_idx) {
			if (!m_video_keyframe_sync) {
				if (pPacket->flags & AV_PKT_FLAG_KEY) {
					LOGI("video sync done!");
					m_video_keyframe_sync = true;
				}
				else {
					LOGW("drop no sync video frame");
					av_free_packet(pPacket);
					pPacket = NULL;
					continue;
				}
			}

			m_video_q.put(pPacket);
		}
		else if(pPacket->stream_index == m_audio_stream_idx) {
			m_audio_q.put(pPacket);

			if (pPacket->stream_index == m_audio_stream_idx) {
				int64_t cached_pos_msec = get_packet_pos(pPacket);
				if (cached_pos_msec == AV_NOPTS_VALUE)
					m_cached_duration_msec += get_packet_duration(pPacket);
				else
					m_cached_duration_msec = cached_pos_msec;
			}
		}
		else {
			LOGE("invalid packet found: stream_idx %d", pPacket->stream_index);
			break;
		}

		m_buffered_size += pPacket->size;
	}

#ifdef __ANDROID__
	if (gs_jvm != NULL) {
		int status;
		status = gs_jvm->DetachCurrentThread();
		if (status != JNI_OK) {
			LOGE("DetachCurrentThread failed %d", status);
		}
	}
#endif

	LOGI("CurrentThread Detached");
}