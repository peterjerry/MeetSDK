#include "ffextractor.h"
#include "common.h"
#include "ppffmpeg.h"
#include "autolock.h"
#include "utils.h"
#include "player.h"
#include "subtitle.h"
#define LOG_TAG "FFExtractor"
#include "log.h"

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

static const uint8_t nalu_header[4] = {0x00, 0x00, 0x00, 0x01};

static bool getStreamLangTitle(char** langcode, char** langtitle, int index, AVStream* stream);

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

typedef struct
{
	int write_adts;
	int objecttype;
	int sample_rate_index;
	int channel_conf;
}ADTSContext;

union bigedian_size {
	int size;
	uint8_t uc[4];
};

#define ADTS_HEADER_SIZE 7

static void ff_log_callback(void* avcl, int level, const char* fmt, va_list vl);

static int aac_decode_extradata(ADTSContext *adts, unsigned char *pbuf, int bufsize);

static int aac_set_adts_head(ADTSContext *acfg, unsigned char *buf, int size);

static int get_size(uint8_t *s, int len);

extern "C" IExtractor* getExtractor(void* context)
{
#ifdef __ANDROID__
#ifdef BUILD_ONE_LIB
	pplog = __pp_log_vprint;
#else
    platformInfo = (PlatformInfo*)context;
    gs_jvm = (JavaVM*)(platformInfo->jvm);
	pplog = (LogFunc)(platformInfo->pplog_func); 
#endif

	if (0) {
		// dead code
		aac_decode_extradata(NULL, NULL, 0);
		aac_set_adts_head(NULL, NULL, 0);
	}
#endif

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

	m_framerate				= 0;
	m_video_clock_msec		= 0;
	m_pBsfc_h264			= NULL;
	m_nalu_convert			= false;
	m_video_keyframe_sync	= false;
	m_sps_data				= NULL;
	m_sps_size				= 0;
	m_pps_data				= NULL;
	m_pps_size				= 0;
	m_video_ahead_msec		= 0;

	m_audio_stream			= NULL;
	m_audio_stream_idx		= -1;
	m_audio_dec_ctx			= NULL;
	m_audio_clock_msec		= 0;
	m_pBsfc_aac				= NULL;

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

	m_NALULengthSizeMinusOne= 4; // default
	m_num_of_sps			= 0;
	m_num_of_pps			= 0;

	m_subtitle_stream		= NULL;
	m_subtitle_stream_idx	= -1;
	m_subtitle_dec_ctx		= NULL;
	mAVSubtitle				= NULL;
	mSubtitleTrackFirstIndex= -1;
	mSubtitleTrackIndex		= -1;
	mISubtitle				= NULL;

	pthread_mutex_init(&mSubtitleLock, NULL);

	/* register all formats and codecs */
	av_register_all();

	avformat_network_init();

	av_log_set_callback(ff_log_callback);
#ifndef NDEBUG
	av_log_set_level(AV_LOG_DEBUG);
#endif
}

FFExtractor::~FFExtractor()
{
	LOGI("FFExtractor destrcutor()");

	close();

	pthread_cond_destroy(&mCondition);
    pthread_mutex_destroy(&mLock);
	pthread_mutex_destroy(&mLockNotify);

	pthread_mutex_destroy(&mSubtitleLock);

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

status_t FFExtractor::setVideoAhead(int32_t msec)
{
	m_video_ahead_msec = msec;
	m_min_play_buf_count = m_video_ahead_msec * m_framerate / 1000;
	LOGI("setVideoAhead() video_ahead_msec %d, min_play_buf_count %d", 
		m_video_ahead_msec, m_min_play_buf_count);
	return OK;
}

status_t FFExtractor::setISubtitle(ISubtitles* subtitle)
{
	LOGI("setISubtitle %p", subtitle);

	mISubtitle = subtitle;
	return OK;
}

void FFExtractor::close()
{
	if (FFEXTRACTOR_STOPPED == m_status)
		return;

	LOGI("close()");

	if (m_status == FFEXTRACTOR_STARTED || m_status == FFEXTRACTOR_PAUSED) {
		m_status = FFEXTRACTOR_STOPPING;
		m_buffering = false;
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
	if (m_subtitle_dec_ctx) {
		avcodec_close(m_subtitle_dec_ctx);
		m_subtitle_dec_ctx = NULL;
	}
	if (m_fmt_ctx) {
		m_fmt_ctx->interrupt_callback.callback = NULL;
		m_fmt_ctx->interrupt_callback.opaque = NULL;
		avformat_close_input(&m_fmt_ctx);
	}

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

	if (!path || strcmp(path, "") == 0) {
		LOGE("url is empty");
		return ERROR;
	}

	if (m_url)
		delete m_url;

	int len = strlen(path) + 1;
	m_url = new char[len];
	strcpy(m_url, path);

#if defined(__CYGWIN__)
	if (strstr(m_url, ":/") != NULL) // cygwin local file is like "e:/folder/1.mov"
#elif defined(_MSC_VER)
	if (strstr(m_url, ":\\") != NULL) // win32 local file is like "e:\folder\1.mov"
#else
    if (strncmp(m_url, "/", 1) == 0 || strncmp(m_url, "file://", 7) == 0)
#endif
		m_sorce_type = TYPE_LOCAL_FILE;
	else if(strstr(m_url, "type=pplive") || strncmp(m_url, "rtmp://", 7) == 0)
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

	if (strncmp(m_url, "rtmp://", 7) == 0)
		m_fmt_ctx->flags |= AVFMT_FLAG_NOBUFFER;

	/* retrieve stream information */
    if (avformat_find_stream_info(m_fmt_ctx, NULL) < 0) {
        LOGE("Could not find stream information");
        return ERROR;
    }

	// only set mISubtitle will do subtitle parse and decode
	if (mISubtitle) {
		for (int32_t i = 0; i < (int32_t)m_fmt_ctx->nb_streams; i++) {
			if (m_fmt_ctx->streams[i]->codec->codec_type == AVMEDIA_TYPE_SUBTITLE) {
				AVStream *subtitle_stream = m_fmt_ctx->streams[i];
				AVCodecID codec_id = subtitle_stream->codec->codec_id;
				// only support 5 type subtitle
				if (codec_id == AV_CODEC_ID_ASS || codec_id == AV_CODEC_ID_SSA ||
						codec_id == AV_CODEC_ID_TEXT || codec_id == AV_CODEC_ID_SRT ||
						codec_id == AV_CODEC_ID_SUBRIP) {
					if (m_subtitle_stream_idx == -1) {
    					m_subtitle_stream_idx = i;
						LOGI("m_subtitle_stream_idx: %d", m_subtitle_stream_idx);
						m_subtitle_stream = subtitle_stream;

						if (!open_subtitle_codec()) {
							LOGE("failed to open subtitle codec");
						}
					}
					else {
						subtitle_stream->discard = AVDISCARD_ALL;
						LOGI("Discard m_subtitle_streamIndex stream: #%d codec_id %d(%s)", i, codec_id, avcodec_get_name(codec_id));
					}

					// add subtitle track
					SubtitleCodecId sub_codec_id = SUBTITLE_CODEC_ID_NONE;
					if (codec_id == AV_CODEC_ID_ASS || codec_id == AV_CODEC_ID_SSA)
					{
						sub_codec_id = SUBTITLE_CODEC_ID_ASS;
					}
					else if(codec_id == AV_CODEC_ID_TEXT || codec_id == AV_CODEC_ID_SRT || codec_id == AV_CODEC_ID_SUBRIP)
					{
						sub_codec_id = SUBTITLE_CODEC_ID_TEXT;
					}
					else {
						LOGW("unsupported subtitle stream #%d codec: %d(%s)", i, codec_id, avcodec_get_name(codec_id));
						continue;
					}

					const char* extraData = (const char*)subtitle_stream->codec->extradata;
					int dataLen = subtitle_stream->codec->extradata_size;
					char *langcode = NULL;
					char *langtitle = NULL;
					if (!getStreamLangTitle(&langcode, &langtitle, i, subtitle_stream)) {
						langcode = (char *)"N/A";
						langtitle = (char *)"N/A";
					}

					int track_index = mISubtitle->addEmbeddingSubtitle(sub_codec_id, langcode/*"chs"*/, langtitle/*"chs"*/, extraData, dataLen);
					if (track_index < 0) {
						LOGE("failed to add embedding subtitle");
						break;
					}

					LOGI("subtitle track %d added", track_index);

					if (mSubtitleTrackIndex == -1) {
						mSubtitleTrackFirstIndex	= i;
						mSubtitleTrackIndex			= track_index;
						LOGI("subtitle track from #%d (sub select #%d)", i, track_index);
					}
				} // end of 5 subtitle type
			}
			else {
				// disable all stream at first
				m_fmt_ctx->streams[i]->discard = AVDISCARD_ALL;
			}
		}
	}

	/* dump input information to stderr */
	av_dump_format(m_fmt_ctx, 0, m_url, 0);

	if (TYPE_LOCAL_FILE == m_sorce_type)  
		m_min_play_buf_count = 1;
	else if (TYPE_LIVE == m_sorce_type)
		m_min_play_buf_count = 25 / 5; // 200 msec for guess
	else
		m_min_play_buf_count = 25 * 4; // 4 sec for vod "smooth" play 

	LOGI("setDataSource done");
	m_status = FFEXTRACTOR_PREPARED;
	return OK;
}

bool FFExtractor::open_subtitle_codec()
{
	LOGI("subtitle extradata size %d", m_subtitle_stream->codec->extradata_size);
		
	m_subtitle_dec_ctx = m_subtitle_stream->codec;
	AVCodec* SubCodec = avcodec_find_decoder(m_subtitle_dec_ctx->codec_id);
	// Open codec
    if (avcodec_open2(m_subtitle_dec_ctx, SubCodec, NULL) < 0) {
    	LOGE("failed to open subtitle decoder: id %d, name %s", 
			m_subtitle_dec_ctx->codec_id, avcodec_get_name(m_subtitle_dec_ctx->codec_id));
		return NULL;
	}

	LOGI("subtitle codec id: %d(%s), codec_name: %s", 
		m_subtitle_dec_ctx->codec_id, avcodec_get_name(m_subtitle_dec_ctx->codec_id), 
		SubCodec->long_name);
	
	if (!mAVSubtitle)
		mAVSubtitle = new AVSubtitle;

	LOGI("subtitle codec opened");
	return true;
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
		format->codec_id		= (int32_t)codec_id;
		format->width		= c->width;
		format->height		= c->height;
		format->ar			= av_q2d(c->sample_aspect_ratio);
		format->duration_us	= m_fmt_ctx->duration;

		// get pps and sps
		if (strstr(m_fmt_ctx->iformat->name, "matroska,webm") != NULL ||
			strstr(m_fmt_ctx->iformat->name, "mp4") != NULL ||
			strstr(m_fmt_ctx->iformat->name, "flv") != NULL)
		{
			/*
			bits    
			8   version ( always 0x01 )
			8   avc profile ( sps[0][1] )
			8   avc compatibility ( sps[0][2] )
			8   avc level ( sps[0][3] )
			6   reserved ( all bits on )
			2   NALULengthSizeMinusOne
			3   reserved ( all bits on )
			5   number of SPS NALUs (usually 1)
			repeated once per SPS:
			  16     SPS size
			  variable   SPS NALU data
			8   number of PPS NALUs (usually 1)
			repeated once per PPS
			  16    PPS size
			  variable PPS NALU data
			*/

			if (c->extradata == NULL) {
				LOGE("codec extra data is null");
				return ERROR;
			}

			uint8_t *data = c->extradata;
			LOGI("avc version 0x%02x", *(data++));
			LOGI("avc profile 0x%02x", *(data++));
			LOGI("avc compatibility 0x%02x", *(data++));
			LOGI("avc level 0x%02x", *(data++));
			m_NALULengthSizeMinusOne = (*data & 0x03);
			LOGI("NALULengthSizeMinusOne %d", m_NALULengthSizeMinusOne);
			data++;
			m_num_of_sps = (*data & 0x1F);
			LOGI("number of SPS NALUs %d", m_num_of_sps);
			data++;
			for (int i=0;i<m_num_of_sps;i++) {
				int sps_len = get_size(data, 2);
				LOGI("#%d SPS size %d", i, sps_len);
				data += 2;
				if (i == 0) {
					format->csd_0_size	= sps_len + m_NALULengthSizeMinusOne + 1;
					format->csd_0 = new uint8_t[format->csd_0_size];
					memcpy(format->csd_0, nalu_header + (3 - m_NALULengthSizeMinusOne), m_NALULengthSizeMinusOne + 1);
					memcpy(format->csd_0 + m_NALULengthSizeMinusOne + 1, data, sps_len);
				}
				data += sps_len;
			}

			m_num_of_pps = *data;
			LOGI("number of PPS NALUs %d", m_num_of_pps);
			data++;
			for (int i=0;i<m_num_of_pps;i++) {
				int pps_len = get_size(data, 2);
				LOGI("#%d PPS size %d", i, pps_len);
				data += 2;
				if (i == 0) {
					format->csd_1_size	= pps_len + m_NALULengthSizeMinusOne + 1;
					format->csd_1 = new uint8_t[format->csd_1_size];
					memcpy(format->csd_1, nalu_header + (3 - m_NALULengthSizeMinusOne), m_NALULengthSizeMinusOne + 1);
					memcpy(format->csd_1 + m_NALULengthSizeMinusOne + 1, data, pps_len);
				}
				data += pps_len;
			}

			// old method(NOT compatible with some mp4 file)
			
			/*
			uint8_t unit_nb;
			const uint8_t *extradata = c->extradata+4;  //jump first 4 bytes
			// retrieve sps and pps unit(s)  
			unit_nb = *extradata++ & 0x1f; // number of sps unit(s) 
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
			*/
		}
		else if (strstr(m_fmt_ctx->iformat->name, "mpegts") != NULL ||
			strstr(m_fmt_ctx->iformat->name, "hls,applehttp") != NULL) 
		{	
			// mpegts and hls has no extra data
			m_fmt_ctx->streams[index]->discard = AVDISCARD_DEFAULT;

			AVPacket pkt;
			av_init_packet(&pkt);
			pkt.size = 0;
			pkt.data = NULL;

			int ret;
			while (m_sps_data == NULL || m_pps_data == NULL) {
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
					// just drop audio pkt because its stream_id is unknown till NOW
					AVPacket* pPacket = (AVPacket*)av_malloc(sizeof(AVPacket));
					memset(pPacket, 0, sizeof(AVPacket));
					av_copy_packet(pPacket, &pkt);
					m_video_q.put(pPacket);
					LOGI("add video pkt to queue in getTrackFormat()");

					if (!m_video_keyframe_sync && pkt.flags & AV_PKT_FLAG_KEY) {
						LOGI("video sync(pre-read) done!");
						m_video_keyframe_sync = true;
					}

					find_sps_pps(&pkt);
				}

				av_free_packet(&pkt);
			}

			if (!m_sps_data || !m_pps_data) {
				LOGE("failed to find sps and pps");
				return ERROR;
			}

			m_fmt_ctx->streams[index]->discard = AVDISCARD_ALL;

			format->csd_0		= m_sps_data;
			format->csd_0_size	= m_sps_size;
			format->csd_1		= m_pps_data;
			format->csd_1_size	= m_pps_size;
		}
		else {
			LOGE("unsupported media format: %s", m_fmt_ctx->iformat->name);
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
		LOGI("audio codec: codec_id %d, channels %d, channel_layout %lld, sample_rate %d, sample_fmt %d",
			codec_id, c->channels, c->channel_layout, c->sample_rate, c->sample_fmt);

/*
#define FF_PROFILE_UNKNOWN -99
#define FF_PROFILE_RESERVED -100

#define FF_PROFILE_AAC_MAIN 0
#define FF_PROFILE_AAC_LOW  1
#define FF_PROFILE_AAC_SSR  2
#define FF_PROFILE_AAC_LTP  3
#define FF_PROFILE_AAC_HE   4
#define FF_PROFILE_AAC_HE_V2 28
#define FF_PROFILE_AAC_LD   22
#define FF_PROFILE_AAC_ELD  38
#define FF_PROFILE_MPEG2_AAC_LOW 128
#define FF_PROFILE_MPEG2_AAC_HE  131
*/
		if (AV_CODEC_ID_AAC == codec_id)
			LOGI("aac profile %d", c->profile);

		format->media_type		= PPMEDIA_TYPE_AUDIO;
		format->codec_id			= (int32_t)codec_id;
		format->channels			= c->channels;
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
		}
	}
	else if (AVMEDIA_TYPE_SUBTITLE == type) {
		format->media_type	= PPMEDIA_TYPE_SUBTITLE;
		format->codec_id		= (int32_t)codec_id;
	}
	else if (AVMEDIA_TYPE_DATA == type) {
		format->media_type	= PPMEDIA_TYPE_DATA;
		format->codec_id		= (int32_t)codec_id;
	}
	else {
		format->media_type	= PPMEDIA_TYPE_UNKNOWN;
		format->codec_id		= PPMEDIA_CODEC_ID_NONE;
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
	int sample_rate = c->sample_rate;
	// That's not a bug. HE-AAC files contain half their actual sampling rate in their headers
	// mkvmerge will issue warnings in such a case, and you have to select with --aac-is-sbr (or mmg's corresponding GUI element).
	if (c->profile == FF_PROFILE_AAC_HE || c->profile == FF_PROFILE_AAC_HE_V2)
		sample_rate /= 2;

	switch (sample_rate) {
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
		LOGE("unsupported audio sample rate %d", sample_rate);
		return ERROR;
	}

	if (c->channels != 0) {
		numChannels = c->channels;
	}
	else {
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
	}

	return (aacObjectType << 11) | (sampleRateIdx << 7) | (numChannels << 3);
}

status_t FFExtractor::selectTrack(int32_t index)
{
	LOGI("selectTrack %d", index);

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
	LOGI("unselectTrack %d", index);

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

	int64_t seek_min = INT64_MIN;
	int64_t seek_max = INT64_MAX;
    //if (av_seek_frame(m_fmt_ctx, stream_index, seek_target, m_seek_flag) < 0) {
    if (avformat_seek_file(m_fmt_ctx, stream_index, seek_min, seek_target, seek_max, m_seek_flag) < 0) {
		LOGE("failed to seek to: %lld msec", m_seek_time_msec);
		return false;
    }
				
    LOGI("after seek to :%lld msec", m_seek_time_msec);

	flush_l();

	m_video_clock_msec = m_audio_clock_msec = m_seek_time_msec;

	LOGI("put flush packet"); 
	if (m_video_stream) {
		AVPacket* flush_pkt = (AVPacket*)av_malloc(sizeof(AVPacket));
		av_init_packet(flush_pkt);

		flush_pkt->stream_index	= m_video_stream_idx;
		flush_pkt->data			= (uint8_t*)"FLUSH";
		flush_pkt->size			= 5;

		m_video_q.put(flush_pkt);
	}

	if (m_audio_stream) {
		AVPacket* flush_pkt = (AVPacket*)av_malloc(sizeof(AVPacket));
		av_init_packet(flush_pkt);

		flush_pkt->stream_index	= m_audio_stream_idx;
		flush_pkt->data			= (uint8_t*)"FLUSH";
		flush_pkt->size			= 5;

		m_audio_q.put(flush_pkt);
	}

	if (mISubtitle)
		mISubtitle->seekTo(0); // do flush

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
			m_sample_pkt = NULL;
			m_sample_track_idx = -1;
			return OK;
		}

		m_buffering = true;

		if (m_sorce_type != TYPE_LOCAL_FILE) {
			LOGI("notifyListener_l MEDIA_INFO_BUFFERING_START");
			notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_START);
		}

		LOGI("start to buffering");
		while (m_buffering) {
			av_usleep(10000); // 10 msec
			if (FFEXTRACTOR_STOPPING == m_status) {
				LOGI("advance was interrputd by stop");
				return ERROR;
			}
		}

		if (m_sorce_type != TYPE_LOCAL_FILE) {
			LOGI("notifyListener_l MEDIA_INFO_BUFFERING_END");
			notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_END);
		}
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
		LOGW("video pts is AV_NOPTS_VALUE, use last corrent value, set to %lld", m_video_clock_msec);
		video_msec = m_video_clock_msec;
	}
	else {
		m_video_clock_msec = video_msec;
	}

	if (audio_msec == AV_NOPTS_VALUE) {
		LOGW("audio pts is AV_NOPTS_VALUE, use last corrent value, set to %lld", m_audio_clock_msec);
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

	if (video_msec - audio_msec < m_video_ahead_msec)
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

	// 20151226 michael.ma added to fix m_buffered_size<0 bug 
	// cause m_max_buffersize dead-loop for "Double max buffer size"
	if (m_buffered_size < 0)
		m_buffered_size = 0;
	if (m_cached_duration_msec < 0)
		m_cached_duration_msec = 0;
	
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
		// 2015.12.24 use "h264_mp4toannexb" will cause read_sample error after seek
		// missing picture in access unit with size xxx
		// cannot get new video packet
		/*if (strncmp((const char *)m_sample_pkt->data, "FLUSH", 5) != 0 && m_pBsfc_h264) {
			// ONLY video pkt NEED do this job
			// flush pkt just copy

			// Apply MP4 to H264 Annex B filter on buffer
			//int origin_size = m_sample_pkt->size;
			int isKeyFrame = m_sample_pkt->flags & AV_PKT_FLAG_KEY;
			//av_bitstream_filter_filter(m_pBsfc_h264, m_video_stream->codec, NULL, &m_sample_pkt->data, &m_sample_pkt->size, 
			//	m_sample_pkt->data, m_sample_pkt->size, isKeyFrame);
		}*/

		if (m_nalu_convert && strncmp((const char *)m_sample_pkt->data, "FLUSH", 5) != 0) {
			int offset = 0;
			while (offset < m_sample_pkt->size) {
				int nalu_size = get_size(m_sample_pkt->data + offset, m_NALULengthSizeMinusOne + 1);
				memcpy(m_sample_pkt->data + offset, nalu_header + (3 - m_NALULengthSizeMinusOne), m_NALULengthSizeMinusOne + 1);
				offset += (m_NALULengthSizeMinusOne + 1 + nalu_size);
			}
		}

		// in some case
		// CANNOT only replace 1st nalu_size to nalu_start_code simplely
		// because maybe more than 1 nalu units exist in avpacket
		// SHOULD look up for all nalu units

		memcpy(data, m_sample_pkt->data, m_sample_pkt->size);
		*sampleSize = m_sample_pkt->size;
	}
	else if (m_audio_stream_idx == m_sample_pkt->stream_index) {
		if (strncmp((const char *)m_sample_pkt->data, "FLUSH", 5) != 0 && m_pBsfc_aac) {
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
	return (m_sample_pkt && m_sample_pkt->data);
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
    AVStream *st = NULL;
    AVCodecContext *dec_ctx = NULL;

	if (stream_idx < 0 || stream_idx >= (int)m_fmt_ctx->nb_streams) {
		LOGE("stream id #%d is invalid, nb_streams %d", stream_idx, m_fmt_ctx->nb_streams);
		return -1;
	}
    
	st = m_fmt_ctx->streams[stream_idx];
	st->discard = AVDISCARD_DEFAULT;

    dec_ctx = st->codec;

	AVMediaType type = dec_ctx->codec_type;
	if (AVMEDIA_TYPE_VIDEO == type) {
		m_video_stream		= st;
		m_video_stream_idx	= stream_idx;
		m_video_dec_ctx		= m_video_stream->codec;

		if (m_video_dec_ctx->extradata)
			LOGI("extradata %p, extradata_size %d", m_video_dec_ctx->extradata, m_video_dec_ctx->extradata_size);

		// mpegets format: (00) 00 00 01 nalu_header1 + nalu_payload1 | (00) 00 00 01 nalu_header2 + nalu_payload2
		// mkv,mp4,flv format: nalu_size1 nalu_header1 + nalu_payload1 | nalu_size2 nalu_header2 + nalu_payload2
		if (strstr(m_fmt_ctx->iformat->name, "matroska") != NULL ||
				strstr(m_fmt_ctx->iformat->name, "mp4") != NULL ||
				strstr(m_fmt_ctx->iformat->name, "flv") != NULL)
		{
			// Retrieve required h264_mp4toannexb filter
			// this filter will do two job:
			// 1) add sps+pps before IDR(iskeyframe)
			// 2) replace 4 byte nalu size(big endian) to nalu_start_code (00) 00 00 01
			
			/*m_pBsfc_h264 = av_bitstream_filter_init("h264_mp4toannexb");
			if (!m_pBsfc_h264) {
				LOGE("Could not aquire h264_mp4toannexb filter");
				return ERROR;
			}*/

			m_nalu_convert = true;
		}

		m_framerate = 25;//default
		AVRational fr;
		fr = av_guess_frame_rate(m_fmt_ctx, m_video_stream, NULL);
		if (fr.num > 0 && fr.den > 0) {
			m_framerate = fr.num / fr.den;
			if(m_framerate > 100 || m_framerate <= 0)
				m_framerate = 25;
		}

		LOGI("media framerate: %d", m_framerate);
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

	pthread_create(&mThread, NULL, demux_thread, this);

	m_buffering = true;
	m_status = FFEXTRACTOR_STARTED;
	advance();

	return 0;
}

void FFExtractor::find_sps_pps(AVPacket *pPacket)
{		
	int32_t last_nalu_start = -1;
	for (int32_t offset=0; offset < pPacket->size; offset++ ) {
		if (m_sps_data && m_pps_data) {
			LOGI("sps and pps found!");
			break;
		}

		if (memcmp(pPacket->data + offset, nalu_header, 4) == 0 || 
			memcmp(pPacket->data + offset, nalu_header + 1, 3) == 0 || 
			offset == pPacket->size - 1) {
			//LOGI("find start code: %d", offset);

			if (last_nalu_start != -1) {
				uint8_t* pNAL = NULL;
				int32_t sizeNAL = 0;

				// 00 00 00 00 xx data ...
				pNAL = pPacket->data + last_nalu_start;
				if (offset == pPacket->size - 1)
					sizeNAL = pPacket->size - last_nalu_start;
				else
					sizeNAL = offset - last_nalu_start;

				//int32_t nal_ref_idc   = pNAL[4] >> 5;
				int32_t nal_unit_type = 0;
				if (pNAL[2] == 0x01)
					nal_unit_type = pNAL[3] & 0x1F;
				else
					nal_unit_type = pNAL[4] & 0x1F;
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

			if (memcmp(pPacket->data + offset, nalu_header, 4) == 0)
				offset += 3; // +1 by for()
			else
				offset += 2; // +1 by for()
		}
	}
}

void FFExtractor::thread_impl()
{
	int ret;

	LOGI("FFExtractor start to demux media");
	
	while (1) {
		if (FFEXTRACTOR_STOPPING == m_status || FFEXTRACTOR_STOPPED ==  m_status) {
            LOGI("work thead break");
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

		if (m_buffered_size > (int32_t)m_max_buffersize) {
            LOGD("Buffering reaches max size %d %d, vQueueSize %d, aQueueSize %d", 
				m_buffered_size, m_max_buffersize, m_video_q.count(), m_audio_q.count());
				
			if (m_buffering) {
                m_max_buffersize *= 2;
                LOGI("Double max buffer size to: %d", m_max_buffersize);
            }
            else {
				// too much data to decode, just wait for decoder consuming some data
				while (m_buffered_size > (int32_t)m_max_buffersize) {
					struct timespec ts;
					ts.tv_sec = 0;
					ts.tv_nsec = 100000000ll; // 100 msec
					AutoLock autoLock(&mLock);
#if defined(__CYGWIN__) || defined(_MSC_VER) || defined(__aarch64__)
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
						LOGI("buffer too much, sleep was interrputed by stoping || seek || buffer");
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

			if (!m_eof) {
				LOGI("av_read_frame() eof");
				m_eof = true;
			}
			
			// 2014.8.25 guoliangma added, to fix cannot play clip which duration is less than 3sec
			if (m_buffering) {
				m_buffering = false;

				if (m_sorce_type != TYPE_LOCAL_FILE) {
					LOGI("MEDIA_INFO_BUFFERING_END because of stream end");
					notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_END);
				}
			}

			// continue for seek back
			av_usleep(10 * 1000); // 10 msec
			continue;
		}
		else if (ret < 0) {
			char msg[128] = {0};
			av_make_error_string(msg, 128, ret);
			LOGW("failed to read frame %d(%s)", ret, msg);

			if (ret == -11) {
				LOGW("just read frame later");
				continue;
			}

			break;
		}

		if (pPacket->stream_index == m_video_stream_idx) {
			if (!m_video_keyframe_sync) {
				if (pPacket->flags & AV_PKT_FLAG_KEY) {
					LOGI("video sync done!");
					m_video_keyframe_sync = true;
				}
				else {
					LOGW("drop no sync video pkt");
					av_free_packet(pPacket);
					pPacket = NULL;
					continue;
				}
			}

			m_video_q.put(pPacket);
		}
		else if(pPacket->stream_index == m_audio_stream_idx) {
			// 2015.12.14 some clip audio/video packet isn't well interleaved written
			// cannot simply drop audio packet here!
			/*if (!m_video_keyframe_sync) {
				LOGW("drop no sync audio pkt");
				av_free_packet(pPacket);
				pPacket = NULL;
				continue;
			}*/

			m_audio_q.put(pPacket);

			if (pPacket->stream_index == m_audio_stream_idx) {
				int64_t cached_pos_msec = get_packet_pos(pPacket);
				if (cached_pos_msec == AV_NOPTS_VALUE)
					m_cached_duration_msec += get_packet_duration(pPacket);
				else
					m_cached_duration_msec = cached_pos_msec;
			}
		}
		else if (pPacket->stream_index == m_subtitle_stream_idx && m_subtitle_stream) {
			AVPacket *orig_pkt = pPacket;
			int got_sub;
			int ret;

			AutoLock autoLock(&mSubtitleLock);

			do {
				ret = avcodec_decode_subtitle2(m_subtitle_stream->codec, mAVSubtitle, &got_sub, pPacket);
				if (ret < 0) {
					LOGW("failed to decode subtitle");
					break;
				}

				if (got_sub) {
					LOGI("got subtitle format: %d, type: %d, content: %s", 
						mAVSubtitle->format, (*(mAVSubtitle->rects))->type, (*(mAVSubtitle->rects))->ass);
					int64_t start_time ,stop_time;
					start_time = av_rescale_q(mAVSubtitle->pts + mAVSubtitle->start_display_time * 1000,
						AV_TIME_BASE_Q, m_subtitle_stream->time_base);
					stop_time = av_rescale_q(mAVSubtitle->pts + mAVSubtitle->end_display_time * 1000,
						AV_TIME_BASE_Q, m_subtitle_stream->time_base);
					if (SUBTITLE_ASS == (*(mAVSubtitle->rects))->type) {
						mISubtitle->addEmbeddingSubtitleEntity(mSubtitleTrackIndex, 
							start_time, stop_time - start_time, 
							(const char*)pPacket->data, pPacket->size);
					}
					else {
						mISubtitle->addEmbeddingSubtitleEntity(mSubtitleTrackIndex, 
							start_time, stop_time - start_time, 
							(*(mAVSubtitle->rects))->text, 0);
					}
					avsubtitle_free(mAVSubtitle);
				}

				pPacket->data += ret;
				pPacket->size -= ret;
			} while (pPacket->size > 0);

			av_free_packet(orig_pkt);
            av_free(orig_pkt);
		} // end of subtitle case
		else {
			LOGD("invalid packet found: stream_idx %d", pPacket->stream_index);
			av_free_packet(pPacket);
			av_free(pPacket);
			continue;
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

	LOGI("thread detached");
#endif
}

static void ff_log_callback(void* avcl, int level, const char* fmt, va_list vl)
{
    AVClass* avc = avcl ? *(AVClass**)avcl : NULL;
	const char * class_name = ((avc != NULL) ? avc->class_name : "N/A");
	
	static char msg[1024] = {0};
	vsnprintf(msg, sizeof(msg), fmt, vl);
	static char log[4096] = {0};
#ifdef _MSC_VER
	_snprintf(log, 4096, "ffmpeg[%d][%s] %s", level, class_name, msg);
#else
	snprintf(log, 4096, "ffmpeg[%d][%s] %s", level, class_name, msg);
#endif

	switch(level) {
		case AV_LOG_PANIC:
		case AV_LOG_FATAL:
		case AV_LOG_ERROR:
			LOGE("%s", log);
			break;
		case AV_LOG_WARNING:
            LOGW("%s", log);
			break;
		case AV_LOG_INFO:
            LOGI("%s", log);
			break;
		case AV_LOG_DEBUG:
            LOGD("%s", log);
			break;
		case AV_LOG_VERBOSE:
            LOGV("%s", log);
			break;
		case AV_LOG_MAX_OFFSET:
			break;
		default:
			LOGI("%s", log);
			break;
	}
}

static int aac_decode_extradata(ADTSContext *adts, unsigned char *pbuf, int bufsize)
{
	int aot, aotext, samfreindex;
	int channelconfig;
	unsigned char *p = pbuf;

	if (!adts || !pbuf || bufsize < 2)
		return -1;

	aot = (p[0]>>3)&0x1f;
	if (aot == 31) {
		aotext = (p[0]<<3 | (p[1]>>5)) & 0x3f;
		aot = 32 + aotext;
		samfreindex = (p[1]>>1) & 0x0f;

		if (samfreindex == 0x0f)
			channelconfig = ((p[4]<<3) | (p[5]>>5)) & 0x0f;
		else
			channelconfig = ((p[1]<<3)|(p[2]>>5)) & 0x0f;
	}
	else {
		samfreindex = ((p[0]<<1)|p[1]>>7) & 0x0f;
		if (samfreindex == 0x0f)
			channelconfig = (p[4]>>3) & 0x0f;
		else
			channelconfig = (p[1]>>3) & 0x0f;
	}

#ifdef AOT_PROFILE_CTRL
	if (aot < 2) aot = 2;
#endif
	adts->objecttype = aot-1;
	adts->sample_rate_index = samfreindex;
	adts->channel_conf = channelconfig;
	adts->write_adts = 1;

	return 0;
}

static int aac_set_adts_head(ADTSContext *acfg, unsigned char *buf, int size)
{       
	unsigned char byte;

	if (size < ADTS_HEADER_SIZE)
		return -1;

	buf[0] = 0xff;
	buf[1] = 0xf1;
	byte = 0;
	byte |= (acfg->objecttype & 0x03) << 6;
	byte |= (acfg->sample_rate_index & 0x0f) << 2;
	byte |= (acfg->channel_conf & 0x07) >> 2;
	buf[2] = byte;
	byte = 0;
	byte |= (acfg->channel_conf & 0x07) << 6;
	byte |= (ADTS_HEADER_SIZE + size) >> 11;
	buf[3] = byte;
	byte = 0;
	byte |= (ADTS_HEADER_SIZE + size) >> 3;
	buf[4] = byte;
	byte = 0;
	byte |= ((ADTS_HEADER_SIZE + size) & 0x7) << 5;
	byte |= (0x7ff >> 6) & 0x1f;
	buf[5] = byte;
	byte = 0;
	byte |= (0x7ff & 0x3f) << 2;
	buf[6] = byte;

	return 0;
}

static int get_size(uint8_t *s, int len)
{
	bigedian_size b_size;
	memset(&b_size, 0, sizeof(b_size));

	int offset = 0;
	for (int i=len - 1;i>=0;i--,offset++) {
		b_size.uc[i] = s[offset];
	}
	return b_size.size;
}

static bool getStreamLangTitle(char** langcode, char** langtitle, int index, AVStream* stream)
{
    bool gotlanguage = false;

	if (langcode == NULL || langtitle == NULL)
		return false;

	if (stream == NULL || stream->metadata == NULL)
		return false;

	const char *stream_type = "other";
	if (stream->codec->codec_type == AVMEDIA_TYPE_AUDIO)
		stream_type = "audio";
	else if (stream->codec->codec_type == AVMEDIA_TYPE_SUBTITLE)
		stream_type = "subtitle";

    AVDictionaryEntry* elem = NULL;

	elem = av_dict_get(stream->metadata, "language", NULL, 0);
    if (elem && elem->value != NULL) {
		int len = strlen(elem->value) + 1;
		*langcode = new char[len];
		memset(*langcode, 0, len);
        strcpy(*langcode, elem->value);
        gotlanguage = true;
    }

    elem = av_dict_get(stream->metadata, "title", NULL, 0);
    if (elem && elem->value != NULL) {
		int len = strlen(elem->value) + 1;
		*langtitle = new char[len];
		memset(*langtitle, 0, len);
        strcpy(*langtitle, elem->value);
        gotlanguage = true;
    }

	if (gotlanguage) {
		LOGI("%s stream index: #%d(lang %s, title: %s)", 
			stream_type, index, 
			*langcode ? *langcode : "N/A", 
			*langtitle ? *langcode : "N/A");
	}
	else {
		LOGW("%s stream index: #d lang and title are both empty", stream_type, index);
	}

    return gotlanguage;
}

