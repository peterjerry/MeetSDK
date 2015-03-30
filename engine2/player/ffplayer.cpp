/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */
#define __STDINT_LIMITS
#include "ffplayer.h"
#include <sys/types.h>
#include <sys/stat.h>
#ifndef _MSC_VER
#include <sys/resource.h>
#include <sys/time.h>
#include <dlfcn.h>
#include <unistd.h>
#endif
#include <stdio.h>
#include <stdint.h>

#define LOG_TAG "Neon-FFPlayer"
#include "log.h"
#include "ppffmpeg.h"
#include "utils.h"
#include "ffrender.h"
#include "autolock.h"
#include "filesource.h"
#ifdef __ANDROID__
#include <cpu-features.h> // for get cpu core count
#endif

#ifdef _MSC_VER
#ifndef INT64_MIN
#define INT64_MIN        (INT64_C(-9223372036854775807)-1)
#define INT64_MAX        (INT64_C(9223372036854775807))
#endif
#pragma comment(lib, "avutil")
#pragma comment(lib, "avcodec")
#pragma comment(lib, "avformat")
#pragma comment(lib, "swresample")
#pragma comment(lib, "swscale")
#ifdef USE_AV_FILTER
#pragma comment(lib, "avfilter")
#endif

#pragma comment(lib, "pthreadVC2")
#endif

#ifdef __ANDROID__
#include <jni.h>
#include "platforminfo.h"
JavaVM* gs_jvm = NULL;
jobject gs_androidsurface = NULL;
PlatformInfo* platformInfo = NULL;
LogFunc pplog = NULL;
#endif

// unit msec(audio_time - video_time)
#define AV_LATENCY_THR1 250//40
#define AV_LATENCY_THR2 300//80
#define AV_LATENCY_THR3 350//120
#define AV_LATENCY_THR4 500//160
#define AV_LATENCY_THR5 750//200

#define AV_LATENCY_BACK_THR1 -10
#define AV_LATENCY_BACK_THR2 -60000

int autorotate = 1;

enum NalUnitType
{
  NAL_UNIT_CODED_SLICE_TRAIL_N = 0,   // 0
  NAL_UNIT_CODED_SLICE_TRAIL_R,   // 1
  
  NAL_UNIT_CODED_SLICE_TSA_N,     // 2
  NAL_UNIT_CODED_SLICE_TLA,       // 3   // Current name in the spec: TSA_R
  
  NAL_UNIT_CODED_SLICE_STSA_N,    // 4
  NAL_UNIT_CODED_SLICE_STSA_R,    // 5

  NAL_UNIT_CODED_SLICE_RADL_N,    // 6
  NAL_UNIT_CODED_SLICE_DLP,       // 7 // Current name in the spec: RADL_R
  
  NAL_UNIT_CODED_SLICE_RASL_N,    // 8
  NAL_UNIT_CODED_SLICE_TFD,       // 9 // Current name in the spec: RASL_R

  NAL_UNIT_RESERVED_10,
  NAL_UNIT_RESERVED_11,
  NAL_UNIT_RESERVED_12,
  NAL_UNIT_RESERVED_13,
  NAL_UNIT_RESERVED_14,
  NAL_UNIT_RESERVED_15,

  NAL_UNIT_CODED_SLICE_BLA,       // 16   // Current name in the spec: BLA_W_LP
  NAL_UNIT_CODED_SLICE_BLANT,     // 17   // Current name in the spec: BLA_W_DLP
  NAL_UNIT_CODED_SLICE_BLA_N_LP,  // 18
  NAL_UNIT_CODED_SLICE_IDR,       // 19  // Current name in the spec: IDR_W_DLP
  NAL_UNIT_CODED_SLICE_IDR_N_LP,  // 20
  NAL_UNIT_CODED_SLICE_CRA,       // 21
  NAL_UNIT_RESERVED_22,
  NAL_UNIT_RESERVED_23,

  NAL_UNIT_RESERVED_24,
  NAL_UNIT_RESERVED_25,
  NAL_UNIT_RESERVED_26,
  NAL_UNIT_RESERVED_27,
  NAL_UNIT_RESERVED_28,
  NAL_UNIT_RESERVED_29,
  NAL_UNIT_RESERVED_30,
  NAL_UNIT_RESERVED_31,

  NAL_UNIT_VPS,                   // 32
  NAL_UNIT_SPS,                   // 33
  NAL_UNIT_PPS,                   // 34
  NAL_UNIT_ACCESS_UNIT_DELIMITER, // 35
  NAL_UNIT_EOS,                   // 36
  NAL_UNIT_EOB,                   // 37
  NAL_UNIT_FILLER_DATA,           // 38
  NAL_UNIT_SEI,                   // 39 Prefix SEI
  NAL_UNIT_SEI_SUFFIX,            // 40 Suffix SEI
  NAL_UNIT_RESERVED_41,
  NAL_UNIT_RESERVED_42,
  NAL_UNIT_RESERVED_43,
  NAL_UNIT_RESERVED_44,
  NAL_UNIT_RESERVED_45,
  NAL_UNIT_RESERVED_46,
  NAL_UNIT_RESERVED_47,
  NAL_UNIT_UNSPECIFIED_48,
  NAL_UNIT_UNSPECIFIED_49,
  NAL_UNIT_UNSPECIFIED_50,
  NAL_UNIT_UNSPECIFIED_51,
  NAL_UNIT_UNSPECIFIED_52,
  NAL_UNIT_UNSPECIFIED_53,
  NAL_UNIT_UNSPECIFIED_54,
  NAL_UNIT_UNSPECIFIED_55,
  NAL_UNIT_UNSPECIFIED_56,
  NAL_UNIT_UNSPECIFIED_57,
  NAL_UNIT_UNSPECIFIED_58,
  NAL_UNIT_UNSPECIFIED_59,
  NAL_UNIT_UNSPECIFIED_60,
  NAL_UNIT_UNSPECIFIED_61,
  NAL_UNIT_UNSPECIFIED_62,
  NAL_UNIT_UNSPECIFIED_63,
  NAL_UNIT_INVALID,
};

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

static int open_codec_context(int *stream_idx,
                              AVFormatContext *fmt_ctx, enum AVMediaType type);

static void ff_log_callback(void* avcl, int level, const char* fmt, va_list vl);

extern "C" IPlayer* getPlayer(void* context)
{
#ifdef __ANDROID__
    platformInfo = (PlatformInfo*)context;
    gs_jvm = (JavaVM*)(platformInfo->jvm);
    pplog = (LogFunc)(platformInfo->pplog_func);
#endif
    return new FFPlayer();
}

extern "C" void releasePlayer(IPlayer* player)
{
	if (player) {
		delete player;
		player = NULL;
	}
}

FFPlayer::FFPlayer()
{
	LOGD("FFPlayer constructor");
    mListener		= NULL;
    mDurationMs		= 0;
    mVideoFrameRate = 0;
	mVideoGapMs		= 0;
	mIOBitrate		= 0;
	mVideoBitrate	= 0;
    mPlayerFlags	= 0;
    mVideoWidth		= 0;
    mVideoHeight	= 0;
    mVideoFormat	= 0;
    mVideoTimeMs	= 0;
    mLooping		= false;
    mSeekTimeMs		= 0;
    mSeeking			= false;
    mRenderFirstFrame	= true;
    mNeedSyncFrame		= true;
    mBroadcastRefreshed = false;
	mIsBuffering		= false;
    mRunningCompatibilityTest	= false;
    mPlayerStatus				= MEDIA_PLAYER_IDLE;
	mAveVideoDecodeTimeMs		= 0;
    mCompensateMs				= 0;
	mVideoPlayingTimeMs			= 0;

	// optimize
    mDiscardCount	= 0;
    mDiscardLevel	= AVDISCARD_NONE;

    mUri					= NULL;
	mSource					= NULL;
    mDataStream				= NULL;
    mAudioPlayer			= NULL;
    mAudioStream			= NULL;
    mSurface				= NULL;
    mVideoFrame				= NULL;
    mIsVideoFrameDirty		= true;
    mVideoRenderer			= NULL;
    mVideoStream			= NULL;
	mMediaFile				= NULL;
    mReachEndStream			= false;
	mSubtitleStream			= NULL;
	mAudioStreamIndex		= -1;
	mVideoStreamIndex		= -1;
	mSubtitleStreamIndex	= -1;
	mSubtitles				= NULL;

    mPrepareEventPending			= false;
    mVideoEventPending				= false;
    mStreamDoneEventPending			= false;
	mBufferingUpdateEventPending	= false;
    mSeekingEventPending			= false;
    mAudioStatusEventPending		= false;
    mBufferingStartEventPending		= false;
    mBufferingEndEventPending		= false;
    mSeekingCompleteEventPending	= false;

	mTotalStartTimeMs		= 0;
	mGapStartTimeMs			= 0;
	mRenderGapStartTimeMs	= 0;
	mAveRenderTimeMs		= 0;
	mDecodedFrames			= 0;
	mRenderedFrames			= 0;

	mSyncType				= AV_SYNC_AUDIO_MASTER;
	mLastFrameMs			= 0;
	mLastDelayMs			= 0;
	mFrameTimerMs			= 0;
	mAVDiffMs				= 0;

	//snapshot
	mSnapshotPic	= NULL;
	mSnapShotFrame	= NULL;
	mSwsCtx			= NULL;

#ifdef USE_AV_FILTER
	mFilterGraph	= NULL;
	mFilterOutputs	= 0;
	mFilterInputs	= NULL;  
	mBufferSinkCtx	= NULL;  
    mBufferSrcCtx	= NULL;
	mVideoFiltFrame	= NULL;
	mLastFilter		= NULL;
	int i;
	for(i=0;i<MAX_FILTER_CNT;i++)
		mFilterDescr[i] = NULL;
#endif

    pthread_mutex_init(&mLock, NULL);
	pthread_mutex_init(&mPlayerLock, NULL);
	pthread_mutex_init(&mPreparedLock, NULL);
	pthread_mutex_init(&mSnapShotLock, NULL);
    pthread_cond_init(&mPreparedCondition, NULL);

    // register all codecs, demux and protocols
    av_register_all();
#ifdef USE_AV_FILTER
	avfilter_register_all();
#endif
    avformat_network_init();

	av_log_set_callback(ff_log_callback);
#ifndef NDEBUG
	av_log_set_level(AV_LOG_DEBUG);
#endif
}

FFPlayer::~FFPlayer()
{
	LOGI("FFPlayer destructor()");

    reset_l();

    pthread_mutex_destroy(&mLock);
	pthread_mutex_destroy(&mPlayerLock);
	pthread_mutex_destroy(&mPreparedLock);
	pthread_mutex_destroy(&mSnapShotLock);
    pthread_cond_destroy(&mPreparedCondition);
	
    avformat_network_deinit();
	LOGI("FFPlayer destructor finished");
}

void FFPlayer::SwapResolution(int32_t *width, int32_t *height)
{
	int32_t tmp;
	tmp = *height;
	*height = *width;
	*width = tmp;
}

bool FFPlayer::FixInterlace(AVStream *video_st)
{
#ifdef USE_AV_FILTER
	mFilterDescr[0] = "yadif";
	if (!init_filters(mFilterDescr)) {
		LOGE("failed to init filters");
		return false;
	}
	mVideoFiltFrame = av_frame_alloc();
#endif
	return true;
}

bool FFPlayer::FixRotateVideo(AVStream *video_st)
{
#ifdef USE_AV_FILTER
	if (autorotate) {
        AVDictionaryEntry *rotate_tag = av_dict_get(video_st->metadata, "rotate", NULL, 0);
        if (rotate_tag && *rotate_tag->value && strcmp(rotate_tag->value, "0")) {
			LOGI("video rotate is not 0: %s", rotate_tag->value);
            if (!strcmp(rotate_tag->value, "90")) {
                mFilterDescr[0] = "transpose=clock";
				SwapResolution(&mVideoWidth, &mVideoHeight);
            } else if (!strcmp(rotate_tag->value, "180")) {
                mFilterDescr[0] = "hflip";
                mFilterDescr[1] = "vflip";
            } else if (!strcmp(rotate_tag->value, "270")) {
                mFilterDescr[0] = "transpose=cclock";
				SwapResolution(&mVideoWidth, &mVideoHeight);
            } else {
				char rotate_buf[64] = {0};
#ifdef _MSC_VER
                _snprintf_s(rotate_buf, sizeof(rotate_buf), "rotate=%s*PI/180", rotate_tag->value);
#else
				snprintf(rotate_buf, sizeof(rotate_buf), "rotate=%s*PI/180", rotate_tag->value);
#endif
                mFilterDescr[0] = rotate_buf;
				LOGE("not suppport rotation %s", rotate_tag->value);
				return false;
            }

			int i = 0;
			while(mFilterDescr[i]) {
				LOGI("set filter descr[%d]: %s", i, mFilterDescr[i]);
				i++;
			}

			if (!init_filters(mFilterDescr)) {
				LOGE("failed to init filters");
				return false;
			}
			mVideoFiltFrame = av_frame_alloc();
        }// end of need rotate

#ifdef _MSC_VER_TEST
		//mFilterDescr[0] = "drawtext=fontfile=msyh.ttf:text='Source is ppbox': "
		//				  "x=100: y=x/dar:draw='if(gt(n,0),lt(n,250))': fontsize=48: fontcolor=yellow@0.2: box=1: boxcolor=red@0.2";
		mFilterDescr[0] = "drawtext=fontfile=msyh.ttf:text='Source is ppbox': \
						  x=100: y=x: fontsize=48: fontcolor=yellow@0.2: box=1: boxcolor=red@0.2";
		//mFilterDescr[0] = "movie=11111.png[logo];[in][logo] overlay=20:10:1[out]";
		if (!init_filters(mFilterDescr)) {
			LOGE("failed to init filters");
			return false;
		}
		mVideoFiltFrame = av_frame_alloc();
#endif
    } // end of if (autorotate)
#endif

	return true;
}

#ifdef USE_AV_FILTER
bool FFPlayer::insert_filter(const char *name, const char* arg, AVFilterContext **last_filter)
{
	AVFilterContext *filt_ctx;
	int ret;
	char fltr_name[64] ={0};
#ifdef _MSC_VER
	_snprintf_s(fltr_name, sizeof(fltr_name), "ffplayer_%s", name);
#else
	snprintf(fltr_name, sizeof(fltr_name), "ffplayer_%s", name);
#endif
	ret = avfilter_graph_create_filter(&filt_ctx, avfilter_get_by_name(name),
		fltr_name, arg, NULL, mFilterGraph);
	if (ret < 0) {
		LOGE("Cannot create filter: %s", name);
		return false;
	}

	ret = avfilter_link(filt_ctx, 0, *last_filter, 0);
	if (ret < 0) {
		LOGE("Cannot link filter: %s", name);
		return false;
	}

	*last_filter = filt_ctx;
	return true;
}

bool FFPlayer::init_filters(const char **filters_descr)
{
	char args[512] = {0};
    int ret = 0;
	int index;
    AVFilter *buffersrc  = avfilter_get_by_name("buffer");
    AVFilter *buffersink = avfilter_get_by_name("buffersink");
	AVCodecContext *dec_ctx = mVideoStream->codec;

	mFilterOutputs = avfilter_inout_alloc();
    mFilterInputs  = avfilter_inout_alloc();
    enum AVPixelFormat pix_fmts[] = { AV_PIX_FMT_YUV420P, AV_PIX_FMT_NONE };

    mFilterGraph = avfilter_graph_alloc();
    if (!mFilterOutputs || !mFilterInputs || !mFilterGraph) {
        ret = AVERROR(ENOMEM);
        goto end;
    }

    /* buffer video source: the decoded frames from the decoder will be inserted here. */
#ifdef _MSC_VER
    _snprintf_s(args, sizeof(args),
            "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
            dec_ctx->width, dec_ctx->height, dec_ctx->pix_fmt,
            dec_ctx->time_base.num, dec_ctx->time_base.den,
            dec_ctx->sample_aspect_ratio.num, dec_ctx->sample_aspect_ratio.den);
#else
	snprintf(args, sizeof(args),
            "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
            dec_ctx->width, dec_ctx->height, dec_ctx->pix_fmt,
            dec_ctx->time_base.num, dec_ctx->time_base.den,
            dec_ctx->sample_aspect_ratio.num, dec_ctx->sample_aspect_ratio.den);
#endif

    ret = avfilter_graph_create_filter(&mBufferSrcCtx, buffersrc, "in",
                                       args, NULL, mFilterGraph);
    if (ret < 0) {
        LOGE("Cannot create buffer source");
        goto end;
    }

    /* buffer video sink: to terminate the filter chain. */
    ret = avfilter_graph_create_filter(&mBufferSinkCtx, buffersink, "out",
                                       NULL, NULL, mFilterGraph);
    if (ret < 0) {
        LOGE("Cannot create buffer sink");
        goto end;
    }

    ret = av_opt_set_int_list(mBufferSinkCtx, "pix_fmts", pix_fmts,
                              AV_PIX_FMT_NONE, AV_OPT_SEARCH_CHILDREN);
    if (ret < 0) {
        LOGE("Cannot set output pixel format");
        goto end;
    }

	mLastFilter = mBufferSinkCtx;
	index = 1;
	while(filters_descr[index]) {
		const char* name = NULL;
		const char* arg = NULL;

		char tmp[64] = {0};
		char* pos = NULL;
		strncpy(tmp, filters_descr[index], 64);
		pos = strchr(tmp, '=');
		if (pos != NULL) {
			*pos = '\0';
			name = tmp;
			arg = pos + 1;
		}
		else {
			name = filters_descr[index];
			arg = NULL;
		}
		insert_filter(name, arg, &mLastFilter);
		index++;
	}

    /* Endpoints for the filter graph. */
    mFilterOutputs->name       = av_strdup("in");
    mFilterOutputs->filter_ctx = mBufferSrcCtx;
    mFilterOutputs->pad_idx    = 0;
    mFilterOutputs->next       = NULL;

    mFilterInputs->name       = av_strdup("out");
    mFilterInputs->filter_ctx = mLastFilter;//mBufferSinkCtx;
    mFilterInputs->pad_idx    = 0;
    mFilterInputs->next       = NULL;

    if ((ret = avfilter_graph_parse_ptr(mFilterGraph, filters_descr[0],
                                    &mFilterInputs, &mFilterOutputs, NULL)) < 0) {
		LOGE("Cannot avfilter_graph_parse_ptr");
        goto end;
	}

    if ((ret = avfilter_graph_config(mFilterGraph, NULL)) < 0) {
		LOGE("Cannot avfilter_graph_config");
        goto end;
	}

end:
    avfilter_inout_free(&mFilterInputs);
    avfilter_inout_free(&mFilterOutputs);

    return (ret == 0) ? true : false;
}
#endif

void FFPlayer::cancelPlayerEvents_l()
{
    LOGD("cancelPlayerEvents");
	mMsgLoop.cancelEvent(VIDEO_RENDER_EVENT);
    mVideoEventPending = false;
	mMsgLoop.cancelEvent(CHECK_AUDIO_STATUS_EVENT);
    mAudioStatusEventPending = false;
}

status_t FFPlayer::setDataSource(const char *uri)
{
	LOGI("player op setDataSource() %s", uri);

    if(uri == NULL ||
        (mPlayerStatus != MEDIA_PLAYER_IDLE &&
        mPlayerStatus != MEDIA_PLAYER_INITIALIZED))
    {
        return INVALID_OPERATION;
    }

	int32_t size = strlen(uri);
    mUri = new char[size+1];
    memset(mUri, 0, size+1);
    strncpy(mUri, uri, size);
    mPlayerStatus = MEDIA_PLAYER_INITIALIZED;
    return OK;
}

status_t FFPlayer::setDataSource(int32_t fd, int64_t offset, int64_t length)
{
	LOGI("player op setDataSource(fd)");
    return INVALID_OPERATION;
}

status_t FFPlayer::selectAudioChannel(int32_t index)
{
	LOGI("player op selectAudioChannel(%d)", index);

	// save last codec context
	AVStream *last_audio_stream = mDataStream->getAudioStream();

	// verify if index is available
	if (mDataStream->selectAudioChannel(index) != OK)
		return ERROR;

	LOGI("after demuxer selectAudioChannel");

	mAudioStreamIndex = index;

	if (mAudioPlayer) {
		delete mAudioPlayer;
		mAudioPlayer = NULL;
	}

	LOGI("after release audio player");

	if (last_audio_stream)
		avcodec_close(last_audio_stream->codec);
	LOGI("after close audio codec");

	// 2015.3.18 guoliangma fix multi-channel diff channel_layout problem
	if (prepareAudio_l() != OK) {
     	LOGE("failed to Init audio player");
        return ERROR;
    }

	if (mAudioPlayer->start() != OK) {
        LOGE("failed to restart audio player");
        return ERROR;
    }

	mSeekTimeMs = mVideoTimeMs;
	postSeekingEvent_l();
	
	ResetStatics();
	return OK;
}

status_t FFPlayer::setVideoSurface(void* surface)
{
#ifdef __ANDROID__
#ifdef USE_NDK_SURFACE_REF
	LOGI("use java side surface object");
	gs_androidsurface = (jobject)surface;
	if (mVideoRenderer) {
		delete mVideoRenderer;

		// realloc render
		mVideoRenderer = new FFRender(mSurface, mVideoWidth, mVideoHeight, mVideoFormat);
		bool force_sw = false;
#ifdef USE_AV_FILTER
		if (mVideoFiltFrame) {
			LOGI("set video render to use software sws");
			force_sw = true;
		}
#endif
        if (mVideoRenderer->init(force_sw) != OK) {
         	LOGE("Initing video render failed");
            return ERROR;
        }

		LOGI("Java: realloc render");
	}
#else
	LOGI("use global info surface object");
    gs_androidsurface = platformInfo->javaSurface;
#endif
#endif

    if(mPlayerStatus != MEDIA_PLAYER_IDLE &&
        mPlayerStatus != MEDIA_PLAYER_INITIALIZED)
    {
        return INVALID_OPERATION;
    }

    mSurface = surface;
    return OK;
}

status_t FFPlayer::reset()
{
	LOGI("player op reset()");
	AutoLock autolock(&mPlayerLock);

    if (mPlayerStatus == MEDIA_PLAYER_IDLE) {
		LOGE("status to call reset() is wrong: %d", mPlayerStatus);
        return INVALID_OPERATION;
	}

    return reset_l();
}

status_t FFPlayer::prepare()
{
    LOGI("player op prepare()");
#ifndef _MSC_VER // fixme guolinagma
	AutoLock autolock(&mPlayerLock);
#endif

    if (mPlayerStatus != MEDIA_PLAYER_INITIALIZED) {
		LOGE("status to call prepare() is wrong: %d", mPlayerStatus);
        return INVALID_OPERATION;
	}

    return prepare_l();
}

status_t FFPlayer::prepareAsync()
{
    LOGI("player op prepareAsync()");
#ifndef _MSC_VER // fixme guoliangma
	AutoLock autolock(&mPlayerLock);
#endif

    if (mPlayerStatus != MEDIA_PLAYER_INITIALIZED) {
		LOGE("status to call prepareAsync() is wrong: %d", mPlayerStatus);
        return INVALID_OPERATION;
	}

    return prepareAsync_l();
}

status_t FFPlayer::start()
{
    LOGI("player op start()");

	AutoLock autolock(&mPlayerLock);

	// 20141124 guoliangma fix INVALID_OP(38) call start() twice when ad play finish
	if (mPlayerStatus == MEDIA_PLAYER_STARTED)
		return OK;

    if (mPlayerStatus != MEDIA_PLAYER_PREPARED &&
        mPlayerStatus != MEDIA_PLAYER_PAUSED)
	{
		LOGE("status to call start() is wrong: %d", mPlayerStatus);
        return INVALID_OPERATION;
	}

    return play_l();
}

status_t FFPlayer::stop()
{
    LOGI("player op stop()");
	AutoLock autolock(&mPlayerLock);

    if (mPlayerStatus == MEDIA_PLAYER_IDLE ||
        mPlayerStatus == MEDIA_PLAYER_INITIALIZED)
	{
		LOGE("status to call stop() is wrong: %d", mPlayerStatus);
        return INVALID_OPERATION;
	}

    if (mPlayerStatus == MEDIA_PLAYER_STOPPED || mPlayerStatus == MEDIA_PLAYER_STOPPING)
        return OK;

	LOGI("before stop_l()");
    return stop_l();
}

status_t FFPlayer::pause()
{
	LOGI("player op pause()");
	AutoLock autolock(&mPlayerLock);

	if (MEDIA_PLAYER_PAUSED == mPlayerStatus)
		return OK;

    if (mPlayerStatus != MEDIA_PLAYER_STARTED ) {
		LOGE("status to call pause() is wrong: %d", mPlayerStatus);
        return INVALID_OPERATION;
	}

    return pause_l();
}

status_t FFPlayer::seekTo(int32_t msec)
{
	LOGI("player op seekTo()");

    if (mPlayerStatus != MEDIA_PLAYER_PREPARED &&
        mPlayerStatus != MEDIA_PLAYER_STARTED &&
        mPlayerStatus != MEDIA_PLAYER_PAUSED)
	{
		LOGE("status to call seekTo() is wrong: %d", mPlayerStatus);
        return INVALID_OPERATION;
	}

    if (mPlayerFlags & (CAN_SEEK_FORWARD | CAN_SEEK_BACKWARD)) {
		if (mDataStream->getURLType() == FFStream::TYPE_LOCAL_FILE) { // m3u8 ffmpeg cannot get corrent duration
			if (msec < 0 || msec > mDurationMs) {
				LOGW("invalid seek position %d(msec) (duration %lld msec)", msec, mDurationMs);
				return OK;
			}
		}

        LOGI("seekto:%d ms", msec);
        mSeekTimeMs = (int64_t)msec;
        postSeekingEvent_l();
    }

    return OK;
}

bool FFPlayer::isPlaying()
{
    return (mPlayerStatus == MEDIA_PLAYER_STARTED);
}

status_t FFPlayer::getVideoWidth(int32_t *w)
{
    if(mVideoRenderer != NULL)
    {
        //Use optimized size from render
        uint32_t optWidth = 0;
        mVideoRenderer->width(optWidth);
        *w = optWidth;
    }
    else
    {
        *w = mVideoWidth;
    }
    return OK;
}

status_t FFPlayer::getVideoHeight(int32_t *h)
{
    if(mVideoRenderer != NULL)
    {
        //Use optimized size from render
        uint32_t optHeight = 0;
        mVideoRenderer->height(optHeight);
        *h = optHeight;
    }
    else
    {
        *h = mVideoHeight;
    }
    return OK;
}

status_t FFPlayer::getCurrentPosition(int32_t* positionMs)
{
	int64_t pos = 0;

	if (mSeekingEventPending || mSeeking) {
        pos = mSeekTimeMs;
    }
    else
    {
        if (mVideoStream != NULL) {
            //get video time
            pos = mVideoTimeMs;
        }
        else if (mAudioPlayer != NULL) {
            //get audio time
            pos = mAudioPlayer->getMediaTimeMs();
        }
        else {
			*positionMs = 0;
            LOGE("No available time reference");
			return ERROR;
        }
    }

	if (mDataStream) {
		int64_t start = mDataStream->getStartTime();
		if (pos > start)
			pos -= start;
	}
	
	*positionMs = (int32_t)pos;
    return OK;
}

status_t FFPlayer::getDuration(int32_t *durationUs)
{
	if (mDataStream && (mDataStream->getURLType() == FFStream::TYPE_LIVE)) {
		mDurationMs = mDataStream->getDurationMs();
	}

    *durationUs = (int32_t)mDurationMs;
    return OK;
}

status_t FFPlayer::getProcessBytes(int64_t *len)
{
	if (NULL == mDataStream) {
		*len = 0;
		return ERROR;
	}

	*len = mDataStream->getTotalReadBytes();
	return OK;
}

status_t FFPlayer::setAudioStreamType(int32_t type)
{
    return OK;
}

status_t FFPlayer::setLooping(int32_t loop)
{
	LOGI("player op setLooping %d", loop);
    mLooping = (loop != 0);
    return OK;
}

bool FFPlayer::isLooping()
{
    return mLooping;
}

status_t FFPlayer::setVolume(float leftVolume, float rightVolume)
{
    return OK;
}

status_t FFPlayer::setListener(MediaPlayerListener* listener)
{
    mListener = listener;
    return OK;
}

status_t FFPlayer::flags()
{
    return mPlayerFlags;
}

status_t FFPlayer::suspend()
{
	return INVALID_OPERATION;
}

status_t FFPlayer::resume()
{
    return INVALID_OPERATION;
}

//////////////////////////////////////////////////////////////////
void FFPlayer::FFPrepareEvent::action(void *opaque, int64_t now_us)
{
	FFPlayer *ins= (FFPlayer *)opaque;
	if (ins)
		ins->onPrepareImpl();
}

void FFPlayer::onPrepareImpl()
{
	LOGD("RUN FFPlayer::onPrepareAsyncEvent()");

	if(mPlayerStatus == MEDIA_PLAYER_STOPPING ||
        mPlayerStatus == MEDIA_PLAYER_STOPPED)
    {
        LOGD("Player is stopping");
        return;
    }

    if (!mPrepareEventPending)
        return;

    mPrepareEventPending = false;

	// Open stream
#ifdef CUSTOMIZED_IO
#if defined(__CYGWIN__)
	if(strstr(mUri, ":/") != NULL) // cygwin local file is like "e:/folder/1.mov"
#elif defined(_MSC_VER)
	if(strstr(mUri, ":\\") != NULL) // win32 local file is like "e:\folder\1.mov"
#endif
	mSource = new FileSource();
	if(!mSource->open(mUri)) {
		LOGE("open file source: %s failed", mUri);
		abortPrepare_l(ERROR);
		return;
	}
#endif

	mDataStream = new FFStream();
	if (mSource)
		mDataStream->setSource(mSource);
    mDataStream->setListener(this);
	mDataStream->setISubtitle(mSubtitles);
    mMediaFile = mDataStream->open(mUri);
    if (mMediaFile == NULL) {
		// 2014.12.17 guoliang.ma added to fix stop when in "preparing" state
		if (mPlayerStatus == MEDIA_PLAYER_STOPPING || mPlayerStatus == MEDIA_PLAYER_STOPPED) {
			LOGI("Player is stopped when preparing");
			return;
		}

     	LOGE("open stream: %s failed", mUri);
		abortPrepare_l(ERROR);
		return;
    }

    mDurationMs = mDataStream->getDurationMs();
    LOGI("mDurationMs: %lld", mDurationMs);
    if(mDurationMs > 0) {
        //vod
        //update mediasource capability
        mPlayerFlags = CAN_SEEK_BACKWARD | CAN_SEEK_FORWARD | CAN_PAUSE;
    }

    if (mPlayerStatus == MEDIA_PLAYER_STOPPING || mPlayerStatus == MEDIA_PLAYER_STOPPED) {
        LOGD("Player is stopping");
        return;
    }

#ifndef NO_AUDIO_PLAY
    if (prepareAudio_l() != OK) {
     	LOGE("Initing audio decoder failed");
        abortPrepare_l(ERROR);
        return;
    }
#endif

    if (mPlayerStatus == MEDIA_PLAYER_STOPPING || mPlayerStatus == MEDIA_PLAYER_STOPPED) {
        LOGD("Player is stopping");
        return;
    }

    if (prepareVideo_l() != OK) {
     	LOGE("Initing video decoder failed");
        abortPrepare_l(ERROR);
        return;
    }

	/*if(prepareSubtitle_l() != OK) {
     	LOGE("Initing subtitle decoder failed");
        abortPrepare_l(ERROR);
        return;
    }*/

    //mPrepareResult = OK;
    //mPlayerStatus = MEDIA_PLAYER_PREPARED;
    //pthread_cond_broadcast(&mPreparedCondition);
    //notifyListener_l(MEDIA_PREPARED);

	notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_PLAYER_TYPE, FF_PLAYER);
    mDataStream->start();

    if (mDataStream->getURLType() != FFStream::TYPE_LOCAL_FILE) // only network protocol will send buffering event
        postBufferingUpdateEvent_l();
}

int64_t FFPlayer::get_master_clock()
{
	if (AV_SYNC_VIDEO_MASTER == mSyncType)
		return get_video_clock();
	else if (AV_SYNC_AUDIO_MASTER == mSyncType)
		return get_audio_clock();
	else
		return get_external_clock();
}

int64_t FFPlayer::get_audio_clock()
{
	if (NULL == mAudioPlayer)
		return 0;

	return mAudioPlayer->getMediaTimeMs();
}

int64_t FFPlayer::get_video_clock()
{
	return mVideoTimeMs;
}

int64_t FFPlayer::get_external_clock()
{
	return av_gettime() / 1000;
}

void FFPlayer::calc_frame_delay()
{
#ifdef NO_AUDIO_PLAY
	mAVDiffMs = 0;
	mScheduleMs = 0;
#else
	int64_t videoFramePTS, videoFrameMs;
	int64_t delay_msec;
	int64_t ref_clock_msec, sync_threshold_msec;

	// calc pts from AVframe
#ifdef USE_AV_FILTER
	if (mVideoFiltFrame) {
		videoFramePTS = getFramePTS_l(mVideoFiltFrame);
	}
	else {
		videoFramePTS = getFramePTS_l(mVideoFrame);
	}
#else
	videoFramePTS = getFramePTS_l(mVideoFrame);
#endif

	videoFrameMs = (int64_t)(videoFramePTS * av_q2d(mVideoStream->time_base) * 1000); // pts -> msec
	delay_msec = videoFrameMs - mLastFrameMs; // always 1000 / framerate(40 msec)
	if (delay_msec < 0 || delay_msec > 1000) {
		// fix invalid pts
		delay_msec = mLastDelayMs;
	}

	mLastFrameMs = videoFrameMs;
	mLastDelayMs = delay_msec;
	
	ref_clock_msec = get_master_clock();
	mVideoTimeMs = videoFrameMs; // time for seek
	mAVDiffMs = videoFrameMs - ref_clock_msec;

	sync_threshold_msec = (delay_msec > AV_SYNC_THRESHOLD_MSEC) ? delay_msec : AV_SYNC_THRESHOLD_MSEC;
	if (mAVDiffMs < AV_NOSYNC_THRESHOLD && mAVDiffMs > -AV_NOSYNC_THRESHOLD) {
		if (mAVDiffMs <= -sync_threshold_msec) {
			delay_msec = 0;
		} else if (mAVDiffMs >= sync_threshold_msec && mAVDiffMs <= (sync_threshold_msec * 2)) {
			delay_msec = 2 * delay_msec;
		} else if (mAVDiffMs >= (sync_threshold_msec * 2)){
			delay_msec = mAVDiffMs; // for seek case
		}
	}

	mFrameTimerMs += delay_msec;

	LOGD("v: %I64d, a: %I64d, diff: %I64d", videoFrameMs, ref_clock_msec, mAVDiffMs);
	notifyVideoDelay(videoFrameMs, ref_clock_msec, mAVDiffMs);
#endif
}

void FFPlayer::notifyVideoDelay(int64_t video_clock, int64_t audio_clock, int64_t frame_delay)
{
	static int64_t start_msec = 0;
#ifdef TEST_PERFORMANCE
	if (mDecodedFrames % 5 == 0)
		notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_LATENCY_MSEC, (int)frame_delay);
#endif
	int64_t cur_msec = getNowMs();
	if (cur_msec - start_msec > 5000) {
#ifdef _MSC_VER
		LOGI("video_clock: %I64d, audio_clock: %I64d, delay: %I64d(msec)",
		    video_clock, audio_clock, frame_delay);
#else
		LOGI("video_clock: %lld, audio_clock: %lld, delay: %lld(msec)",
		    video_clock, audio_clock, frame_delay);
#endif
		start_msec = cur_msec;
	}
}

void FFPlayer::render_impl()
{
#ifdef USE_AV_FILTER
	if (mVideoFiltFrame) {
		if ( OK != mVideoRenderer->render(mVideoFiltFrame))
			notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_VIDEO_RENDER, 0);

		av_frame_unref(mVideoFiltFrame);
		return;
	}
#endif
	if (OK != mVideoRenderer->render(mVideoFrame))
		notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_VIDEO_RENDER, 0);
}

void FFPlayer::render_frame()
{
	LOGD("render_frame");

	if (mRenderFirstFrame) { 
		// must set audio start time before render frame!
        mIsVideoFrameDirty = true;
        mRenderFirstFrame = false;
    }

	calc_frame_delay();

	if (need_drop_frame()) {
		// delay is larger than video_gap, so next video vidoe event is asap.
		return;
	}

#ifdef TEST_PERFORMANCE
	LOGV("begin to render frame");
	int64_t begin_render = getNowMs();
#endif

	render_impl();
	mRenderedFrames++;

#ifdef TEST_PERFORMANCE
	int64_t end_render, costTime;
	end_render = getNowMs();
	costTime = end_render - begin_render;

	if (mAveRenderTimeMs == 0)
		mAveRenderTimeMs = costTime;
	else
		mAveRenderTimeMs = (mAveRenderTimeMs * 4 + costTime) / 5;

	notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_RENDER_FRAME, (int)mRenderedFrames);

	if (getNowMs() - mRenderGapStartTimeMs > 1000) {
		notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_RENDER_AVG_MSEC, (int)mAveRenderTimeMs);
		int64_t duration = getNowMs() - mTotalStartTimeMs;
		if (duration > 0)
			notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_RENDER_FPS, (int)(mRenderedFrames * 1000 / (double)duration + 0.3f));
		mRenderGapStartTimeMs = getNowMs();
	}
#endif
	LOGD("[liveplatform]: render pts:%lld", mVideoPlayingTimeMs);
	mIsVideoFrameDirty = true;
	LOGV("render frame end");

	// if video is too late ,just play it. otherwise frame will always frozen.
	if (mAVDiffMs >= AV_LATENCY_THR5) {
		mDiscardLevel = AVDISCARD_BIDIR;
		mDiscardCount = 7;
	}
}

bool FFPlayer::need_drop_frame()
{
	int32_t frame_delay = (int32_t)mAVDiffMs;
	bool ret = true;

	if (frame_delay <= AV_LATENCY_THR1) {
		// video is not late
		mDiscardLevel = AVDISCARD_NONE;
        mDiscardCount = 0;
		ret = false;
	}
	else if (frame_delay > AV_LATENCY_THR1 && frame_delay < AV_LATENCY_THR5) {
		// if latency is too much, just display video, otherwise frame is frozen for a long time
		if (frame_delay < AV_LATENCY_THR2) {
			mDiscardLevel = AVDISCARD_NONREF;
			mDiscardCount = 2;
		}
		else if (frame_delay < AV_LATENCY_THR3) {
			mDiscardLevel = AVDISCARD_NONREF;
			mDiscardCount = 3;
			if (mVideoFrameRate > 30)
				mDiscardCount = 4;
		}
		else if (frame_delay < AV_LATENCY_THR4) {
			mDiscardLevel = AVDISCARD_BIDIR;
			mDiscardCount = 4;
			if (mVideoFrameRate > 30)
				mDiscardCount = 5;
		}
		else {
			mDiscardLevel = AVDISCARD_BIDIR;
			mDiscardCount = 5;
			if (mVideoFrameRate > 30)
				mDiscardCount = 6;
		}

		mIsVideoFrameDirty = true;
		ret = true;
#ifdef TEST_PERFORMANCE
		notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_DROP_FRAME, (int)frame_delay);
		// drop frame!
		// We're more than 40ms(default) late.
		LOGI("drop frame: video late by %d ms, mDiscardLevel:%d, mDiscardCount:%d", frame_delay, mDiscardLevel, mDiscardCount);
#endif
	}
	else {
		// video lag too much, just play
		ret = false;
	}

	return ret;
}

void FFPlayer::FFVideoEvent::action(void *opaque, int64_t now_us)
{
	FFPlayer *ins= (FFPlayer *)opaque;
	if (ins)
		ins->onVideoImpl();
}

void FFPlayer::onVideoImpl()
{
    LOGD("on video event");

    if (!mVideoEventPending) {
		// avoid 2nd event sent when 1st hasn't been fired. 1->1fire->2->2fire is ok. 1->2->1fired->2fired(will return here)
        LOGW("mVideoEvent is Pending");
		// no any more onVideo()
		return;
	}

    mVideoEventPending = false; // reset it

	if (mIsBuffering) {
		postVideoEvent_l(100); // wait 100 msec
		return;
	}

	if (mIsVideoFrameDirty) {
		AVPacket* pPacket = NULL;
		status_t ret;

		// Step1 read packet
		ret = mDataStream->getPacket(mVideoStreamIndex, &pPacket);
		if (ret == FFSTREAM_OK) { 
			// got packet

			if (mNeedSyncFrame) { // after "start" or "seek"
				// 2014.9.3  guoliangma added strcmp(mMediaFile->iformat->name, "mpegts") to fix "sitv.ts" cannot play
				if (!(pPacket->flags & AV_PKT_FLAG_KEY) /*&& strcmp(mMediaFile->iformat->name, "mpegts") != 0*/) {
					LOGD("Discard non-key packet for first packet");
					//free packet resource
					av_free_packet(pPacket);
					av_free(pPacket);
					pPacket = NULL;
					LOGI("packet is not key frame, drop frame");
#ifdef TEST_PERFORMANCE
					notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_DROP_FRAME, (int)mAVDiffMs);
#endif
					postVideoEvent_l(0); //trigger next onvideo event asap.
					// drop frame!
					return;
				}

				mNeedSyncFrame = broadcast_refresh();

				if (!mNeedSyncFrame) {
					// 1st frame is good for render
					LOGI("video sync done!");
				}
			}

			optimizeDecode_l(pPacket);

			// picture is good for display now
			if (need_drop_pkt(pPacket)) { // parse h264 nal to decide if video is late.(not after decode)
				av_free_packet(pPacket);
				av_free(pPacket);
				pPacket = NULL;
				LOGI("packet is late, drop frame");
#ifdef TEST_PERFORMANCE
				notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_DROP_FRAME, (int)mAVDiffMs);
#endif
				postVideoEvent_l(0);
				// drop frame!
				return;
			}

			// Step2 decode video frame
			LOGD("before decode");
			ret = decode_l(pPacket);
			LOGD("after decode");

			//free packet resource
			av_free_packet(pPacket);
			av_free(pPacket);
			pPacket = NULL;

			if (ret != OK) {
	            postVideoEvent_l(0);
                return;
            }
		}
		else if (ret == FFSTREAM_ERROR_FLUSHING) {
			LOGI("onVideo FFSTREAM_ERROR_FLUSHING: flush codec");
			avcodec_flush_buffers(mVideoStream->codec);
			av_free(pPacket);
			pPacket = NULL;
			postVideoEvent_l(10);
			return;
		}
		else if(ret == FFSTREAM_ERROR_BUFFERING) {
			LOGD("video queue no data");
			postVideoEvent_l(10);
			return;
		}
		else if(ret == FFSTREAM_ERROR_EOF) {
			LOGI("reach clip stream end");

			/*
			AVPacket flush_Pkt;
			av_init_packet(&flush_Pkt);
			flush_Pkt.data = NULL;
			flush_Pkt.size = 0;

			status_t ret = decode_l(&flush_Pkt);
			av_free_packet(&flush_Pkt);
			if (OK != ret) {
			mReachEndStream = true;
			if (mAudioStream == NULL)
			postStreamDoneEvent_l();

			return;
			}*/

			mReachEndStream = true;
			LOGI("reach video stream end");
#ifdef NO_AUDIO_PLAY
			postStreamDoneEvent_l(); // just finish NOW!
#else
			
			if (mAudioStream == NULL)
				postStreamDoneEvent_l();

			// no any more onVideo()
			return;
#endif
		}
		else {
			LOGE("read video packet error: %d", ret);
			notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_FAIL_TO_READ_PACKET, ret);
			// no any more onVideo()
			return;
		}
	} // end of if(mIsVideoFrameDirty)

	// Step3 render picture
	if (mPlayerStatus == MEDIA_PLAYER_PAUSED) {
        //support frame update during paused. ONLY RENDER ONE FRAME
        LOGD("render paused frame frame");
        render_impl();
        mIsVideoFrameDirty = true;
        LOGD("render paused frame frame done");
        //mRenderFrameDuringPaused = false;
        return;
    }

	render_frame();

	int64_t schedule_msec = mFrameTimerMs - av_gettime() / 1000 - mAveVideoDecodeTimeMs;
	LOGD("schedule_msec %lld", schedule_msec);
	if (schedule_msec < 10) // 10 msec
		schedule_msec = 0;

	postVideoEvent_l(schedule_msec);
}

void FFPlayer::set_opt(const char *opt)
{

}

bool FFPlayer::broadcast_refresh()
{
	if (!mBroadcastRefreshed &&
		mDataStream->getURLType() == FFStream::TYPE_BROADCAST && // protocol rtmp set to TYPE_BROADCAST
		mDataStream->getRealtimeLevel() == FFStream::LEVEL_HIGH) {
		mDataStream->refresh(); // drop all a/v packet in the list
		mBroadcastRefreshed = true;
		return true;
	}
	
	return false;
}

void FFPlayer::FFBufferingUpdateEvent::action(void *opaque, int64_t now_us)
{
	FFPlayer *ins= (FFPlayer *)opaque;
	if (ins)
		ins->onBufferingUpdateImpl();
}

void FFPlayer::onBufferingUpdateImpl()
{
	AutoLock autolock(&mPlayerLock);

    if (!mBufferingUpdateEventPending)
        return;

    mBufferingUpdateEventPending = false;

    if(mDataStream == NULL || mDurationMs < 0) {
        LOGE("Invalid buffering status");
        //postBufferingUpdateEvent_l();
        return;
    }

    if(mDurationMs == 0) {
        LOGD("It is broadcasting, do not update buffering status");
        return;
    }

    int64_t cachedDurationMs; // abosolute position
	if (mSeeking)
		cachedDurationMs = mSeekTimeMs;
	else
		cachedDurationMs = mDataStream->getCachedDurationMs();
	int64_t pos_msec = get_master_clock();
	if (cachedDurationMs < pos_msec)
		cachedDurationMs = pos_msec;
	int percent100 = (int)(cachedDurationMs * 100 / mDurationMs) + 1; // 1 is for compensation.
	if (percent100 > 100) 
		percent100 = 100;
	LOGD("onBufferingUpdate, percent: %d", percent100);

    notifyListener_l(MEDIA_BUFFERING_UPDATE, percent100);

    postBufferingUpdateEvent_l();
}

void FFPlayer::FFSeekingEvent::action(void *opaque, int64_t now_us)
{
	FFPlayer *ins= (FFPlayer *)opaque;
	if (ins)
		ins->onSeekingImpl();
}

void FFPlayer::onSeekingImpl()
{
	LOGD("onSeeking");
	AutoLock autolock(&mPlayerLock);

    if (!mSeekingEventPending) // more than 1 seek request in event loop
		return;

    if (mVideoStream != NULL) {
		if(mSeekTimeMs > mVideoTimeMs)
			mSeekIncr = 1;
		else
			mSeekIncr = -1;
        mVideoTimeMs = mSeekTimeMs;
    }
	else {
		int64_t cur_pos;
		if (mAudioPlayer) {
			//get audio time
			cur_pos = (int32_t)mAudioPlayer->getMediaTimeMs();
			if (mSeekTimeMs < cur_pos)
				mSeekIncr = -1;
			else
				mSeekIncr = 1;
		}
		else {
			LOGW("No available time reference for seek");
			mSeekIncr = 1;
		}
		
	}

    mSeekingEventPending = false;

    if (!mSeeking) {
        mSeeking = true; //doing seek.
        seekTo_l();
        if (mVideoStream != NULL) {
            mRenderFirstFrame = true;
            mNeedSyncFrame = true;
        }
    }
	else {
		// mSeeking will be reset to false when seek is complete
		// last "seek" is doing, so just wait 
        postSeekingEvent_l(100); // handle continously seeking?
    }
}

void FFPlayer::FFStreamDoneEvent::action(void *opaque, int64_t now_us)
{
	FFPlayer *ins= (FFPlayer *)opaque;
	if (ins)
		ins->onStreamDoneImpl();
}

void FFPlayer::onStreamDoneImpl()
{
    LOGI("onStreamDone");
	AutoLock autolock(&mPlayerLock);

    if (!mStreamDoneEventPending)
        return;

    mStreamDoneEventPending = false;

    if (mLooping)
    {
        // implemented in FFStream
        //LOGE("Loop is not supported");
    }
    else
    {
        //Current thread should not stop itself.
        //stop_l();
        notifyListener_l(MEDIA_PLAYBACK_COMPLETE);
    }
}

void FFPlayer::FFCheckAudioStatusEvent::action(void *opaque, int64_t now_us)
{
	FFPlayer *ins= (FFPlayer *)opaque;
	if (ins)
		ins->onCheckAudioStatusImpl();
}

void FFPlayer::onCheckAudioStatusImpl()
{
	AutoLock autolock(&mPlayerLock);

    if (!mAudioStatusEventPending)
        return;

    mAudioStatusEventPending = false;

    if (mAudioPlayer->getStatus() == MEDIA_PLAYER_PLAYBACK_COMPLETE) {
        LOGI("Audio reaches stream end");
        postStreamDoneEvent_l();
    }
    else {
        postCheckAudioStatusEvent_l();
    }
}

void FFPlayer::FFBufferingStartEvent::action(void *opaque, int64_t now_us)
{
	FFPlayer *ins= (FFPlayer *)opaque;
	if (ins)
		ins->onBufferingStartImpl();
}

void FFPlayer::onBufferingStartImpl()
{
	LOGD("onBufferingStart");
	AutoLock autolock(&mPlayerLock);

    if (!mBufferingStartEventPending)
        return;

    mBufferingStartEventPending = false;

    mIsBuffering = true;
    if(mPlayerStatus == MEDIA_PLAYER_PREPARING)
    {
        //Do nothing
    }
    else
    {
        if(mDataStream->getURLType() != FFStream::TYPE_LOCAL_FILE)
        {
            LOGD("MEDIA_INFO_BUFFERING_START");
            notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_START);
        }
    }
}

void FFPlayer::FFBufferingEndEvent::action(void *opaque, int64_t now_us)
{
	FFPlayer *ins= (FFPlayer *)opaque;
	if (ins)
		ins->onBufferingEndImpl();
}

void FFPlayer::onBufferingEndImpl()
{
	LOGD("onBufferingEnd");
	AutoLock autolock(&mPlayerLock);

    if (!mBufferingEndEventPending)
        return;

    mBufferingEndEventPending = false;

    mIsBuffering = false;
    if(mPlayerStatus == MEDIA_PLAYER_PREPARING)
    {
        mPrepareResult = OK;
        mPlayerStatus = MEDIA_PLAYER_PREPARED;
		AutoLock autoLock(&mPreparedLock);
        pthread_cond_broadcast(&mPreparedCondition);
        LOGI("onBufferingEnd: MEDIA_PREPARED");
        notifyListener_l(MEDIA_PREPARED);
    }
    else
    {
        if(mDataStream->getURLType() != FFStream::TYPE_LOCAL_FILE)
        {
            LOGD("MEDIA_INFO_BUFFERING_END");
            notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_END);
        }
    }
}

void FFPlayer::FFSeekingCompleteEvent::action(void *opaque, int64_t now_us)
{
	FFPlayer *ins= (FFPlayer *)opaque;
	if (ins)
		ins->onSeekingCompleteImpl();
}

void FFPlayer::onSeekingCompleteImpl()
{
	LOGD("onSeekingComplete");
	AutoLock autolock(&mPlayerLock);

    if (!mSeekingCompleteEventPending)
        return;

    mSeekingCompleteEventPending = false;

    mSeeking = false;
	ResetStatics();
    LOGD("send event MEDIA_SEEK_COMPLETE");
    notifyListener_l(MEDIA_SEEK_COMPLETE);
}

void FFPlayer::FFIOBitrateInfoEvent::action(void *opaque, int64_t now_us)
{
	FFPlayer *ins= (FFPlayer *)opaque;
	if (ins)
		ins->onIOBitrateInfoImpl();
}

void FFPlayer::onIOBitrateInfoImpl()
{
	LOGD("onBitrateInfo");
    notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_IO_BITRATE, mIOBitrate);

	int msec = 0;
	getBufferingTime(&msec);
	notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_BUFFERING_MSEC, msec);
}

void FFPlayer::FFMediaBitrateInfoEvent::action(void *opaque, int64_t now_us)
{
	FFPlayer *ins= (FFPlayer *)opaque;
	if (ins)
		ins->onMediaBitrateInfoImpl();
}

void FFPlayer::onMediaBitrateInfoImpl()
{
	LOGD("onBitrateInfo");
    notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_MEDIA_BITRATE, mVideoBitrate);
}

status_t FFPlayer::setISubtitle(ISubtitles* subtitle)
{
    if(subtitle != 0)
    {
        mSubtitles = subtitle;
		return OK;
    }

	else
		return ERROR;
}

void FFPlayer::notify(int32_t msg, int32_t ext1, int32_t ext2)
{
	// called from audio_player or ffstream
    switch(msg)
    {
        case MEDIA_INFO:
            if(ext1 == MEDIA_INFO_BUFFERING_START) {
                postBufferingStartEvent_l();
            }
            else if(ext1 == MEDIA_INFO_BUFFERING_END) {
                postBufferingEndEvent_l();
            }
			else if(ext1 == MEDIA_INFO_TEST_IO_BITRATE){
				mIOBitrate = ext2;
				postIOBitrateInfoEvent_l();
			}
			else if(ext1 == MEDIA_INFO_TEST_MEDIA_BITRATE){
				mVideoBitrate = ext2;
				postMediaBitrateInfoEvent_l();
			}
            break;
        case MEDIA_SEEK_COMPLETE:
            postSeekingCompleteEvent_l();
            break;
		case MEDIA_ERROR:
			notifyListener_l(MEDIA_ERROR, ext1, ext2);
			break;
        default:
            break;
    }
}

////////////////////////////////////////////////////////////////
status_t FFPlayer::pause_l()
{
    cancelPlayerEvents_l();

    if (mAudioPlayer != NULL) {
        status_t ret = mAudioPlayer->pause();
        if(ret != OK) {
     	    LOGE("failed to pause audio player %d", ret);
            return ret;
        }
    }

    mPlayerStatus = MEDIA_PLAYER_PAUSED;
    return OK;
}

status_t FFPlayer::seekTo_l() // called from onSeekingImpl, so MUSTN'T LOCK IT HERE
{
	if (mAudioPlayer != NULL)
        mAudioPlayer->seekTo(mSeekTimeMs);

	// will notify seek_complete
    if (mDataStream->seek(mSeekTimeMs, mSeekIncr) != OK) {
        notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_FAIL_TO_SEEK, 0);
        mPlayerStatus = MEDIA_PLAYER_STATE_ERROR;
        return ERROR;
    }

    if (mPlayerStatus == MEDIA_PLAYER_PAUSED) {
        LOGD("seeking while paused");

        postVideoEvent_l(0);//to display seek target frame instantly
        //mRenderFrameDuringPaused = true;
    }

    mDiscardLevel = AVDISCARD_NONE;
    mDiscardCount = 0;
    mIsVideoFrameDirty = true;
    return OK;
}

void FFPlayer::postPrepareEvent_l()
{
    if (mPrepareEventPending)
        return;

    mPrepareEventPending = true;

	FFPrepareEvent *evt = new FFPrepareEvent(this);
	mMsgLoop.postEventTohHeader(evt);
}

void FFPlayer::postVideoEvent_l(int64_t delayMs)
{
    if (mVideoEventPending) //prevent duplicated onVideo()
        return;

    mVideoEventPending = true;

	FFVideoEvent *evt = new FFVideoEvent(this);
	mMsgLoop.postEventWithDelay(evt, delayMs < 0 ? 0 : delayMs);
}

void FFPlayer::postStreamDoneEvent_l()
{
    if (mStreamDoneEventPending)
        return;

    mStreamDoneEventPending = true;

	FFStreamDoneEvent *evt = new FFStreamDoneEvent(this);
	mMsgLoop.postEventTohHeader(evt);
}

void FFPlayer::postBufferingUpdateEvent_l()
{
    if (mBufferingUpdateEventPending)
        return;

    mBufferingUpdateEventPending = true;

	FFBufferingUpdateEvent *evt = new FFBufferingUpdateEvent(this);
	mMsgLoop.postEventWithDelay(evt, 1000); // 1sec refresh
}

void FFPlayer::postSeekingEvent_l(int64_t delayMs)
{
    if (mSeekingEventPending) // already seeking
        return;

    mSeekingEventPending = true;

	FFSeekingEvent *evt = new FFSeekingEvent(this);

    if (delayMs == -1)
		mMsgLoop.postEventTohHeader(evt);
    else
		mMsgLoop.postEventWithDelay(evt, delayMs);
}


void FFPlayer::postCheckAudioStatusEvent_l() {
    if (mAudioStatusEventPending)
        return;

    mAudioStatusEventPending = true;

	FFCheckAudioStatusEvent *evt = new FFCheckAudioStatusEvent(this);
	mMsgLoop.postEventWithDelay(evt, 1000);// 1sec
}

void FFPlayer::postBufferingStartEvent_l() {

    if (mBufferingStartEventPending)
        return;

    mBufferingStartEventPending = true;

	FFBufferingStartEvent *evt = new FFBufferingStartEvent(this);
	mMsgLoop.postEventTohHeader(evt);
}

void FFPlayer::postBufferingEndEvent_l() {

    if (mBufferingEndEventPending) {
        return;
    }

    mBufferingEndEventPending = true;
	
	FFBufferingEndEvent *evt = new FFBufferingEndEvent(this);
	mMsgLoop.postEventTohHeader(evt);
}

void FFPlayer::postSeekingCompleteEvent_l() {

    if (mSeekingCompleteEventPending) {
        return;
    }

    mSeekingCompleteEventPending = true;
	
	FFSeekingCompleteEvent *evt = new FFSeekingCompleteEvent(this);
	mMsgLoop.postEventTohHeader(evt);
}

void FFPlayer::postIOBitrateInfoEvent_l() {
	FFIOBitrateInfoEvent *evt = new FFIOBitrateInfoEvent(this);
	mMsgLoop.postEventTohHeader(evt);
}

void FFPlayer::postMediaBitrateInfoEvent_l() {
	FFMediaBitrateInfoEvent *evt = new FFMediaBitrateInfoEvent(this);
	mMsgLoop.postEventTohHeader(evt);
}

void FFPlayer::abortPrepare_l(status_t err)
{
    notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_FAIL_TO_OPEN, err);

    mPrepareResult = err;
    mPlayerStatus = MEDIA_PLAYER_STATE_ERROR;
	AutoLock autolock(&mPreparedLock);
    pthread_cond_broadcast(&mPreparedCondition);
}

status_t FFPlayer::prepareSubtitle_l()
{
	mSubtitleStreamIndex = mDataStream->getSubtitleStreamIndex();
	mSubtitleStream = mDataStream->getSubtitleStream();
	if (mSubtitleStream) {
		AVCodecContext *SubCodecCtx = mSubtitleStream->codec;
		AVCodec* SubCodec = avcodec_find_decoder(SubCodecCtx->codec_id);
		// Open codec
    	if (avcodec_open2(SubCodecCtx, SubCodec, NULL) < 0) {
    		LOGE("failed to open subtitle decoder: %d %s", SubCodecCtx->codec_id, avcodec_get_name(SubCodecCtx->codec_id));
			return ERROR;
		}

		LOGI("subtitle codec id: %d(%s), codec_name: %s", 
			SubCodecCtx->codec_id, avcodec_get_name(SubCodecCtx->codec_id), SubCodec->long_name);
	}

	return OK;
}

status_t FFPlayer::prepareAudio_l()
{
	mAudioStreamIndex = mDataStream->getAudioStreamIndex();
	mAudioStream = mDataStream->getAudioStream();
	if (mAudioStreamIndex == -1 || NULL == mAudioStream) {
        LOGI("No audio stream");
		mSyncType = AV_SYNC_VIDEO_MASTER; // fixme
		return OK;
	}
    
    // Get a pointer to the codec context for the video stream
    AVCodecContext* codec_ctx = mAudioStream->codec;
    AVCodec* codec = avcodec_find_decoder(codec_ctx->codec_id);

    if (NULL == codec) {
		LOGE("Failed to find audio decoder: %d %s", codec_ctx->codec_id, avcodec_get_name(codec_ctx->codec_id));
		//notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_UNSUPPORTED, 0);
		return ERROR;
	}

	LOGI("audio codec_ctx fmt %d", codec_ctx->sample_fmt);

    // Open codec
    //codec_ctx->request_sample_fmt = AV_SAMPLE_FMT_S16;
	// AV_SAMPLE_FMT_S16P,        ///< signed 16 bits, planar
    if (avcodec_open2(codec_ctx, codec, NULL) >= 0) {
        //init audioplayer
        if(codec_ctx->sample_rate > 0 && codec_ctx->channels > 0) {
            mAudioPlayer = new AudioPlayer(mDataStream, mAudioStream, mAudioStreamIndex);
            LOGD("audio codec name:%s", codec->long_name);
        }
        else {
            LOGE("invalid audio config data, codec:%s, samplerate:%d, channels:%d",
                codec->long_name, codec_ctx->sample_rate, codec_ctx->channels);
			// 2015.1.13 guoliangma fix crash
			return ERROR;
        }
    }
    else {
        LOGE("Failed to open audio decoder: %d %s", codec_ctx->codec_id, avcodec_get_name(codec_ctx->codec_id));
    }

    if (mAudioPlayer == NULL) {
        mDataStream->disableStream(mAudioStreamIndex);
        mAudioStreamIndex = -1;
        mAudioStream = NULL;
		LOGW("failed to open audio player");
        mSyncType = AV_SYNC_VIDEO_MASTER; // fixme
    }

    mAudioPlayer->setListener(this);
    status_t ret = mAudioPlayer->prepare();
    if(ret != OK){
     	LOGE("failed to prepare audio player");
        return ret;
    }

	return OK;
}

status_t FFPlayer::prepareVideo_l()
{
	mVideoStreamIndex = mDataStream->getVideoStreamIndex();
	mVideoStream = mDataStream->getVideoStream();
	if (mVideoStreamIndex == -1 || mVideoStream == NULL) {
        LOGD("No video stream");
        notifyListener_l(MEDIA_SET_VIDEO_SIZE, 0, 0);
		return OK;
	}
    else {
    	// Get a pointer to the codec context for the video stream
    	AVCodecContext* codec_ctx = mVideoStream->codec;
#ifdef OS_IOS
		// ios use auto thread_count
		//int core_number = sysconf(_SC_NPROCESSORS_CONF);
#endif
#ifdef __ANDROID__
		int core_number = android_getCpuCount();
		codec_ctx->thread_count = core_number;
		LOGI("set thread_count to %d", core_number);
		if (AV_CODEC_ID_HEVC == codec_ctx->codec_id) {
			codec_ctx->thread_type = FF_THREAD_SLICE;
			LOGI("set thread_type to FF_THREAD_SLICE(hevc)");
		}
#endif
		/*if (AV_CODEC_ID_H264 == codec_ctx->codec_id) {
			// decode_unregistered_user_data
			// key word "user data"
			codec_ctx->debug |= FF_DEBUG_BUGS; // gather x264 encoder config from SEI
		}*/

		AVCodec* codec = NULL;
		codec = avcodec_find_decoder(codec_ctx->codec_id);

		/*
		// for strongene decoder test
		if (AV_CODEC_ID_HEVC == codec_ctx->codec_id) {
			if (strcmp(mMediaFile->iformat->name, "mpegts") == 0 || strcmp(mMediaFile->iformat->name, "flv") == 0) {
				LOGI("force to use liblenthevc");
				codec = avcodec_find_decoder_by_name("liblenthevc");//liblenthevc
			}
			else {
				codec = avcodec_find_decoder(codec_ctx->codec_id);
			}
		}
		else {
			codec = avcodec_find_decoder(codec_ctx->codec_id);
		}*/

    	if (codec == NULL) {
			LOGE("failed to find video codec: %d %s", codec_ctx->codec_id, avcodec_get_name(codec_ctx->codec_id));
    		return ERROR;
    	}

    	// Open codec
    	if (avcodec_open2(codec_ctx, codec, NULL) < 0) {
    		LOGE("failed to open video decoder: %d %s", codec_ctx->codec_id, avcodec_get_name(codec_ctx->codec_id));
			return ERROR;
		}

    	mVideoWidth = codec_ctx->width;
    	mVideoHeight = codec_ctx->height;
        mVideoFormat = (int32_t)codec_ctx->pix_fmt;

		if (0 == mVideoWidth || 0 == mVideoHeight) {
			LOGE("failed to get video resolution: %d %d", mVideoWidth, mVideoHeight);
			return ERROR;
		}

        if (mVideoFormat == AV_PIX_FMT_NONE) {
            mVideoFormat = AV_PIX_FMT_YUV420P;
            LOGW("failed to get video color format, but still try to render it as YUV420P");
        }

        if (mVideoStream->avg_frame_rate.den > 0) {
            mVideoFrameRate = mVideoStream->avg_frame_rate.num / mVideoStream->avg_frame_rate.den;
        }
        else {
            mVideoFrameRate = 25;//give a guessed value.
        }
		mVideoGapMs = 1000 / mVideoFrameRate;

		/*if (!FixInterlace(mVideoStream)) {
			LOGE("failed to fix interlaced video");
			return ERROR;
		}*/

		// would change width and height
		if (!FixRotateVideo(mVideoStream)) {
			LOGE("failed to fix rotate video");
			return ERROR;
		}

        LOGI("mVideoWidth: %d, mVideoHeight: %d, mVideoFormat: %d", mVideoWidth, mVideoHeight, mVideoFormat);
        LOGI("mVideoFrameRate: %d fps(display gap %d msec)", mVideoFrameRate, mVideoGapMs);
		LOGI("video codec id: %d(%s), codec_name: %s", codec_ctx->codec_id, avcodec_get_name(codec_ctx->codec_id), codec->long_name);
		if (codec_ctx->bit_rate > 0)
			LOGI("bitrate %d bit/s", codec_ctx->bit_rate);
		else
			LOGI("bitrate N/A");

        //init videoplayer related data.
		mVideoFrame = av_frame_alloc();
        mIsVideoFrameDirty = true;

		uint32_t render_w, render_h;
		render_w = mVideoWidth;
		render_h = mVideoHeight;
#ifdef _MSC_VER
		if (render_w > MAX_DISPLAY_WIDTH) {
			double ratio	= (double)render_w / MAX_DISPLAY_WIDTH;
			render_w		= MAX_DISPLAY_WIDTH;
			render_h		= (int32_t)(render_h / ratio);
			LOGI("video resolution %d x %d, display resolution switch to %d x %d", 
				mVideoWidth, mVideoHeight, render_w, render_h);
		}

		if (render_h > MAX_DISPLAY_HEIGHT) {
			double ratio	= (double)render_h / MAX_DISPLAY_HEIGHT;
			render_h		= MAX_DISPLAY_HEIGHT;
			render_w		= (int32_t)(render_w / ratio);
			LOGI("video resolution %d x %d, display resolution switch to %d x %d",
				mVideoWidth, mVideoHeight, render_w, render_h);
		}
#endif

        mVideoRenderer = new FFRender(mSurface, render_w, render_h, mVideoFormat);
		bool force_sw = false;
#ifdef USE_AV_FILTER
		if (mVideoFiltFrame) {
			LOGI("set video render to use software sws");
			force_sw = true;
		}
#endif
        if (mVideoRenderer->init(force_sw) != OK) {
         	LOGE("Initing video render failed");
            return ERROR;
        }

        uint32_t surfaceWidth = 0, surfaceHeight = 0;
        mVideoRenderer->width(surfaceWidth);
        mVideoRenderer->height(surfaceHeight);
        notifyListener_l(MEDIA_SET_VIDEO_SIZE, surfaceWidth, surfaceHeight);
    }

	return OK;
}

void FFPlayer::notifyListener_l(int msg, int ext1, int ext2)
{
    if (mListener != NULL)
        mListener->notify(msg, ext1, ext2);
    else
		LOGE("mListener is null");
}

status_t FFPlayer::play_l()
{
	mDataStream->setLooping(mLooping);

	// 2015.3.28 guoliangma move "set status" ahead "kick video playback event"
	// fix cannot re-start problem paused seek
	ResetStatics();
    mPlayerStatus = MEDIA_PLAYER_STARTED;

	if (mAudioPlayer != NULL) {
        if(mAudioPlayer->start() != OK) {
            LOGE("audio player starts failed");
            return ERROR;
        }

        if(mAudioStream != NULL)
            postCheckAudioStatusEvent_l();
        else
            LOGI("no audio stream");
    }

    if (mVideoRenderer != NULL && mVideoStream != NULL) {
        // Kick off video playback event
		LOGI("Kick off video playback event %d", mVideoEventPending);
		// avoid 1st onVideo call render_frame but state is still MEDIA_PLAYER_PAUSED
        postVideoEvent_l(mVideoGapMs);
    }
    else {
        LOGI("no video stream");
    }

    return OK;
}

void FFPlayer::ResetStatics()
{
	mDecodedFrames		= 0;
	mRenderedFrames		= 0;
	mTotalStartTimeMs	= getNowMs();
	mFrameTimerMs		= av_gettime() / 1000;
}

status_t FFPlayer::reset_l()
{
	LOGI("reset_l()");

	if(stop_l() != OK)
        return ERROR;

    if(mUri != NULL) {
        delete mUri;
        mUri = NULL;
    }

    mSurface = NULL;
    mPlayerStatus = MEDIA_PLAYER_IDLE;
    return OK;
}

status_t FFPlayer::stop_l()
{
	if (MEDIA_PLAYER_STOPPED == mPlayerStatus)
		return OK;

    mPlayerStatus = MEDIA_PLAYER_STOPPING;

	if (mDataStream != NULL) {
		LOGI("interrupt data stream");
        mDataStream->interrupt_open();
	}
	LOGI("after interrupt mDataStream");

    LOGI("before mMsgLoop(Loop) stop");
	mMsgLoop.stop(false);

    // Shutdown audio first
    if (mAudioPlayer != NULL) {
        delete mAudioPlayer;
        mAudioPlayer = NULL;
    }
	LOGI("after delete mAudioPlayer");

    if (mVideoRenderer != NULL) {
        delete mVideoRenderer;
        mVideoRenderer = NULL;
    }
	LOGI("after delete mVideoRenderer");
#ifdef USE_AV_FILTER
	avfilter_graph_free(&mFilterGraph);
#endif
    if(mAudioStream != NULL) {
        LOGD("avcodec_close audio");
        avcodec_close(mAudioStream->codec);
        mAudioStream = NULL;
    }
    LOGI("after avcodec_close mAudioStream");

    if (mVideoStream != NULL) {
        LOGD("avcodec_close video");
        avcodec_close(mVideoStream->codec);
        mVideoStream = NULL;
    }
    LOGI("after avcodec_close mVideoStream");
	
	if (mVideoFrame != NULL)
		av_frame_free(&mVideoFrame);
    LOGI("after avcodec_free_frame mVideoFrame");

	if (mSnapshotPic)
		av_freep(mSnapshotPic);
	if (mSnapShotFrame != NULL)
		av_frame_free(&mSnapShotFrame);
	if(mSwsCtx != NULL) {
		sws_freeContext(mSwsCtx);
		mSwsCtx = NULL;
	}

#ifdef USE_AV_FILTER
	if(mVideoFiltFrame != NULL) {
		av_frame_free(&mVideoFiltFrame);
    }
    LOGI("after avcodec_free_frame mVideoFiltFrame");

	for (int i=0;i<MAX_FILTER_CNT;i++)
		mFilterDescr[i] = NULL;
#endif

	if (mSource) {
		delete mSource;
		mSource = NULL;
	}

	if (mDataStream != NULL) {
		LOGD("stop stream");
        mDataStream->stop();
        LOGI("delete data stream");
		delete mDataStream;
        mDataStream = NULL;
	}
    LOGI("after delete mDataStream");

    mReachEndStream = false;
	mAudioStreamIndex = -1;
	mVideoStreamIndex = -1;

    mDurationMs = 0;
    mVideoFrameRate = 0;
    mPlayerStatus = 0;
    mPlayerFlags = 0;
    mVideoWidth = 0;
    mVideoHeight = 0;
    mVideoFormat = 0;
    mVideoTimeMs = 0;
    mLooping = false;
    mSeekTimeMs = 0;
    //mRenderFrameDuringPaused = false;

    //mPlayerStatus = MEDIA_PLAYER_INITIALIZED;
    mPlayerStatus = MEDIA_PLAYER_STOPPED;

    LOGI("stop_l() finished");
    return OK;
}

status_t FFPlayer::prepare_l()
{
    status_t err = prepareAsync_l();

    if (err != OK) {
        return err;
    }

	// 2014.9.4 guoliangma change from while to if
    if (mPlayerStatus == MEDIA_PLAYER_PREPARING) {
		pthread_mutex_lock(&mPreparedLock);
        pthread_cond_wait(&mPreparedCondition, &mPreparedLock);
		pthread_mutex_unlock(&mPreparedLock);
    }

    return mPrepareResult;
}

status_t FFPlayer::prepareAsync_l()
{
    if (mPlayerStatus == MEDIA_PLAYER_PREPARING)
        return ERROR;

    if (mPlayerStatus == MEDIA_PLAYER_PREPARED)
        return OK;

	mPlayerStatus = MEDIA_PLAYER_PREPARING;

    mMsgLoop.start();

    postPrepareEvent_l();

    return OK;
}

status_t FFPlayer::decode_l(AVPacket *packet)
{
    int frameFinished = 0;
	AutoLock autolock(&mSnapShotLock);

	// Decode video frame
	int64_t begin_decode = getNowMs();
	int ret = avcodec_decode_video2(mVideoStream->codec,
        						 mVideoFrame,
        						 &frameFinished,
        						 packet);
	int64_t end_decode = getNowMs();
    int64_t costTime = end_decode - begin_decode;
    if (mAveVideoDecodeTimeMs == 0)
        mAveVideoDecodeTimeMs = costTime;
    else
        mAveVideoDecodeTimeMs = (mAveVideoDecodeTimeMs*4+costTime)/5;
	LOGD("decode video cost %lld[ms]", costTime);
	LOGD("mAveVideoDecodeTimeMs %lld[ms]", mAveVideoDecodeTimeMs);
	LOGD("key-frame %d", mVideoFrame->key_frame);
#ifdef TEST_PERFORMANCE
	if (getNowMs() - mGapStartTimeMs > 1000) {
		notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_DECODE_AVG_MSEC, (int)mAveVideoDecodeTimeMs);
		int64_t duration = getNowMs() - mTotalStartTimeMs;
		if (duration <= 0)
			duration = 1;
		notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_DECODE_FPS, 
			(int)(mDecodedFrames * 1000 / (double)duration + 0.3f));
		mGapStartTimeMs = getNowMs();
	}
#endif

#ifndef NDEBUG
	if (packet->data) {
		int64_t pts = (int64_t)(packet->pts*1000*av_q2d(mVideoStream->time_base));
		int64_t dts = (int64_t)(packet->dts*1000*av_q2d(mVideoStream->time_base));
#ifdef _MSC_VER
		LOGD("decoding packet->pts:%I64d ms", pts);
		LOGD("decoding packet->dts:%I64d ms", dts);
#else
		LOGD("decoding packet->pts:%lld ms", pts);
		LOGD("decoding packet->dts:%lld ms", dts);
#endif
	}
    int64_t frame_pts = (int64_t)(mVideoFrame->pts*1000*av_q2d(mVideoStream->time_base));
#ifdef _MSC_VER
	LOGD("mVideoFrame->pts:%I64d", frame_pts);
#else
	LOGD("mVideoFrame->pts:%lld", frame_pts);
#endif
    int64_t frame_pkt_pts = (int64_t)(mVideoFrame->pkt_pts*1000*av_q2d(mVideoStream->time_base));
    int64_t frame_pkt_dts = (int64_t)(mVideoFrame->pkt_dts*1000*av_q2d(mVideoStream->time_base));
#ifdef _MSC_VER
	LOGD("mVideoFrame->pkt_pts:%I64d", frame_pkt_pts);
	LOGD("mVideoFrame->pkt_dts:%I64d", frame_pkt_dts);
#else
	LOGD("mVideoFrame->pkt_pts:%lld", frame_pkt_pts);
	LOGD("mVideoFrame->pkt_dts:%lld", frame_pkt_dts);
#endif
#endif

	if (ret < 0) {
		LOGE("Failed to decode video frame with ret: %d", ret);
		return ERROR;
	}

	if (frameFinished == 0) {
		LOGD("Frame is not available for output"); //like B frame
		return ERROR;
	}
	else {
		LOGD("got a decoded frame");
#ifdef USE_AV_FILTER
		if (mVideoFiltFrame) {
			// push the decoded frame into the filtergraph
			if (av_buffersrc_add_frame_flags(mBufferSrcCtx, mVideoFrame, AV_BUFFERSRC_FLAG_KEEP_REF) < 0) {
				LOGE("Error while feeding the filtergraph");
				return ERROR;
			}

			// pull filtered frames from the filtergraph
			while (1) {
				ret = av_buffersink_get_frame(mBufferSinkCtx, mVideoFiltFrame);
				if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF)
					break;
				if (ret < 0)
					break;
			}
		}
#endif

        mIsVideoFrameDirty = false;
		mDecodedFrames++;
	}

	return OK;

	
	/*if (packet->dts == AV_NOPTS_VALUE && mFrame->opaque
			&& *(uint64_t*) mFrame->opaque != AV_NOPTS_VALUE) {
		pts = *(uint64_t *) mFrame->opaque;
	} else if (packet->dts != AV_NOPTS_VALUE) {
		pts = packet->dts;
	} else {
		pts = 0;
	}
	pts *= av_q2d(mStream->time_base);
	LOGV("Video pts:%d", pts);
	*/
}

static void decodeNAL(uint8_t* dst, uint8_t* src, int32_t size )
{
    uint8_t* end = src + size;
    while(src < end)
    {
        if(src < end-3 &&
            src[0] == 0x00 &&
            src[1] == 0x00 &&
            src[2] == 0x03 )
        {
            *dst++ = 0x00;
            *dst++ = 0x00;

            src += 3;
            continue;
        }
        *dst++ = *src++;
    }
}

//defines functions, structures for handling streams of bits
typedef struct bs_s
{
    uint8_t* p_start;
    uint8_t* p;
    uint8_t* p_end;

    int32_t i_left; /* i_count number of available bits */
} bs_t;

static inline void bs_init( bs_t* s, void* pData, int32_t iData )
{
    s->p_start = (uint8_t*)pData;
    s->p       = s->p_start;
    s->p_end   = s->p_start + iData;
    s->i_left  = 8;
}

static inline uint32_t bs_read( bs_t *s, int i_count )
{
     static const uint32_t i_mask[33] =
     {  0x00,
        0x01,      0x03,      0x07,      0x0f,
        0x1f,      0x3f,      0x7f,      0xff,
        0x1ff,     0x3ff,     0x7ff,     0xfff,
        0x1fff,    0x3fff,    0x7fff,    0xffff,
        0x1ffff,   0x3ffff,   0x7ffff,   0xfffff,
        0x1fffff,  0x3fffff,  0x7fffff,  0xffffff,
        0x1ffffff, 0x3ffffff, 0x7ffffff, 0xfffffff,
        0x1fffffff,0x3fffffff,0x7fffffff,0xffffffff};
    int      i_shr;
    uint32_t i_result = 0;

    while( i_count > 0 )
    {
        if( s->p >= s->p_end )
        {
            break;
        }

        if( ( i_shr = s->i_left - i_count ) >= 0 )
        {
            /* more in the buffer than requested */
            i_result |= ( *s->p >> i_shr )&i_mask[i_count];
            s->i_left -= i_count;
            if( s->i_left == 0 )
            {
                s->p++;
                s->i_left = 8;
            }
            return( i_result );
        }
        else
        {
            /* less in the buffer than requested */
           i_result |= (*s->p&i_mask[s->i_left]) << -i_shr;
           i_count  -= s->i_left;
           s->p++;
           s->i_left = 8;
        }
    }

    return( i_result );
}

static inline uint32_t bs_read1( bs_t *s )
{
    if( s->p < s->p_end )
    {
        unsigned int i_result;

        s->i_left--;
        i_result = ( *s->p >> s->i_left )&0x01;
        if( s->i_left == 0 )
        {
            s->p++;
            s->i_left = 8;
        }
        return i_result;
    }

    return 0;
}

static inline int bs_read_ue( bs_t *s )
{
    int i = 0;

    while( bs_read1( s ) == 0 && s->p < s->p_end && i < 32 )
    {
        i++;
    }
    return( ( 1 << i) - 1 + bs_read( s, i ) );
}

static inline int bs_read_se( bs_t *s )
{
    int val = bs_read_ue( s );

    return val&0x01 ? (val+1)/2 : -(val/2);
}

bool FFPlayer::need_drop_pkt(AVPacket* packet)
{
    if (mAudioPlayer == NULL)
        return false;

    if (packet->size <= 0)
        return true;

	enum AVCodecID codec_id = mVideoStream->codec->codec_id;
	if (codec_id == AV_CODEC_ID_HEVC) {
		/*if (mDiscardCount > 0 && mDiscardLevel > AVDISCARD_DEFAULT) {
			mDiscardCount--;
			LOGI("Discard packet as video playing under AVDISCARD_NONREF, mDiscardCount:%d,", mDiscardCount);
			return true;
		}*/
	}

    if (codec_id == AV_CODEC_ID_H264) {
#ifndef NDEBUG
        int64_t pts = (int64_t)(packet->pts*1000*av_q2d(mVideoStream->time_base));
        int64_t dts = (int64_t)(packet->dts*1000*av_q2d(mVideoStream->time_base));
    	LOGD("decoding packet->pts:%lld ms", pts);
    	LOGD("decoding packet->dts:%lld ms", dts);
#endif
        LOGD("AV_CODEC_ID_H264, size:%d", packet->size);
        LOGD("packet->pts:%lld", packet->pts);
        LOGD("packet->dts:%lld", packet->dts);
        LOGD("packet->flags:%d", packet->flags);

        uint8_t* pNAL = NULL;
        int32_t sizeNAL = 0;
        LOGD("mMediaFile->iformat->name:%s", mMediaFile->iformat->name);
        if(strstr(mMediaFile->iformat->name, "matroska") != NULL ||
            strstr(mMediaFile->iformat->name, "mp4") != NULL ||
            strstr(mMediaFile->iformat->name, "flv") != NULL)
        {
            uint8_t* pExtra = mVideoStream->codec->extradata;
            if(pExtra == NULL)
            {
                LOGD("Skip nal parse");
                return false;
            }
            if(pExtra[0] != 1 || mVideoStream->codec->extradata_size < 7)
            {
                LOGE("Invalid AVCC");
                return false;
            }
            int32_t avccLengthSize = (pExtra[4] & 0x03) + 1;
            LOGD("avccLengthSize:%d", avccLengthSize);
            pNAL = packet->data+avccLengthSize;
            sizeNAL = packet->size-avccLengthSize;
        }
        else
        {
            //other types input stream
            for(int32_t offset=0; offset < packet->size-3; offset++ )
            {
                if(packet->data[offset] == 0x00 &&
                    packet->data[offset+1] == 0x00 &&
                    packet->data[offset+2] == 0x01)
                {
                    //match it
                    LOGD("got 0x000001");
                    pNAL = packet->data+offset+3;
                    sizeNAL = packet->size-offset-3;
                    break;
                }
            }
        }

        if(pNAL == NULL)
        {
            LOGD("Skip nal parse");
            return false;
        }

        int32_t nalForb = pNAL[0]>>7;
        if(nalForb == 1)
        {
            LOGE("Packet is corrupted");
            return true;
        }

        int32_t nalType = pNAL[0]&0x1f;
        int32_t nalReferrenceIDC = (pNAL[0]>>5)&0x3;
        LOGD("nalType:%d", nalType);
        LOGD("nalReferrenceIDC:%d", nalReferrenceIDC);
        if(nalType >= NAL_SLICE && nalType <= NAL_IDR_SLICE) // frame type?
        {
            if (mDiscardCount > 0) {
				if (mDiscardLevel == AVDISCARD_NONREF) {
					if(nalReferrenceIDC == 0) // unit is not used for prediction, can drop this frame
					{
						mDiscardCount--;
						LOGI("Discard packet as video playing under AVDISCARD_NONREF, mDiscardCount:%d,", mDiscardCount);
						return true;
					}
				}
#ifndef _MSC_VER
				else if(0)//mDiscardLevel == AVDISCARD_BIDIR)
				{
					uint8_t* pVCL = pNAL+1;
					int32_t sizeVCL = sizeNAL-1;
					// do not convert the whole frame
					int32_t decSize = (sizeVCL < 60) ? sizeVCL : 60;
					uint8_t decData[decSize];
					decodeNAL(decData, pVCL, decSize);
					bs_t s;
					bs_init(&s, decData, decSize);
					bs_read_ue(&s);
					int32_t sliceType = bs_read_ue(&s);

					if(sliceType == 1 || sliceType == 6 ||nalReferrenceIDC == 0)
					{
						//BLOCK_FLAG_TYPE_B;
						mDiscardCount--;
						LOGW("Discard packet as video playing under AVDISCARD_BIDIR, mDiscardCount:%d,", mDiscardCount);
						return true;
					}
				}
				else if(0)//mDiscardLevel == AVDISCARD_NONKEY)
				{
					uint8_t* pVCL = pNAL+1;
					int32_t sizeVCL = sizeNAL-1;
					// do not convert the whole frame
					int32_t decSize = sizeVCL<60?sizeVCL:60;
					uint8_t decData[decSize];
					decodeNAL(decData, pVCL, decSize);
					bs_t s;
					bs_init(&s, decData, decSize);
					bs_read_ue(&s);
					int32_t sliceType = bs_read_ue(&s);

					if(sliceType != 2 && sliceType != 7)
					{
						//not BLOCK_FLAG_TYPE_I;
						mDiscardCount--;
						LOGW("Discard packet as video playing under AVDISCARD_NONKEY, mDiscardCount:%d,", mDiscardCount);
						return true;
					}
				}
#endif
			}

            if(packet->flags & AV_PKT_FLAG_KEY)
            {
                //do not discard key frame
                return false;
            }
            else if(nalReferrenceIDC == 0)
            {
                int64_t packetPTS = 0;
                if(packet->pts == AV_NOPTS_VALUE)
                {
                    LOGV("pPacket->pts is AV_NOPTS_VALUE");
                    packetPTS = packet->dts;
                }
                else
                {
                    packetPTS = packet->pts;
                }
                if(packetPTS == AV_NOPTS_VALUE)
                {
                    return false;
                }
                int64_t packetTimeMs = (int64_t)(packetPTS * 1000 * av_q2d(mVideoStream->time_base));
				int64_t audioPlayingTimeMs = get_audio_clock();
                int64_t latenessMs = audioPlayingTimeMs - packetTimeMs;

                //as audio time is playing, so time*2
                latenessMs = latenessMs + mVideoRenderer->swsMs() + mAveVideoDecodeTimeMs*2;
                LOGD("packetTimeMs: %lld, audioPlayingTimeMs: %lld, latenessMs: %lld, maxPacketLatenessMs: %lld",
            		    packetTimeMs, audioPlayingTimeMs, latenessMs, latenessMs);

                if(latenessMs > AV_LATENCY_THR1)
                {
                    LOGD("discard packet as packet is late");
                    return true;
                }
            }
        }

        /* keep the code for use later
        if(nalType >= NAL_SLICE && nalType <= NAL_IDR_SLICE)
        {
            LOGD("packet with NAL video frame");
            uint8_t* pVCL = pNAL+1;
            int32_t sizeVCL = sizeNAL-1;
            // do not convert the whole frame
            int32_t decSize = sizeVCL<60?sizeVCL:60;
            uint8_t decData[decSize];
            decodeNAL(decData, pVCL, decSize);
            bs_t s;
            bs_init(&s, decData, decSize);
            bs_read_ue(&s);
            int32_t sliceType = bs_read_ue(&s);
            LOGD("sliceType:%d", sliceType);

            switch(sliceType)
            {
            case 0: case 5:
                //BLOCK_FLAG_TYPE_P;
                LOGD("packet with VCL P frame");
                LOGD("packet->pts:%lld", packet->pts);
                LOGD("packet->dts:%lld", packet->dts);
                break;
            case 1: case 6:
                //BLOCK_FLAG_TYPE_B;
                LOGD("packet with VCL B frame");
                LOGD("packet->pts:%lld", packet->pts);
                LOGD("packet->dts:%lld", packet->dts);
                break;
            case 2: case 7:
                //BLOCK_FLAG_TYPE_I;
                LOGD("packet with VCL I frame");
                LOGD("packet->pts:%lld", packet->pts);
                LOGD("packet->dts:%lld", packet->dts);
                break;
            case 3: case 8:
                //BLOCK_FLAG_TYPE_P;
                LOGD("packet with VCL SP frame");
                break;
            case 4: case 9:
                //BLOCK_FLAG_TYPE_I;
                LOGD("packet with VCL SI frame");
                break;
            default:
                LOGD("packet with VCL unknown frame");
                break;
            }
        }
        else if(nalType == NAL_SPS)
        {
            LOGD("packet with NAL SPS");
            //Do nothing
        }
        else if(nalType == NAL_PPS)
        {
            LOGD("packet with NAL PPS");
            //Do nothing
        }
        else if(nalType == NAL_SEI)
        {
            LOGD("packet with NAL SEI");
            //Do nothing
        }
        else
        {
            LOGD("packet with NAL others");
            //Do nothing
        }
        */
    }

    return false;
}

void FFPlayer::optimizeDecode_l(AVPacket* packet)
{
    //notify ffmpeg to do more optimization.
    if(mDiscardCount > 0) {
        if(packet->flags & AV_PKT_FLAG_KEY) {
            //do not skip loop filter for key frame
            mVideoStream->codec->skip_loop_filter = AVDISCARD_DEFAULT;
			//for test
			//mVideoStream->codec->skip_frame = AVDISCARD_DEFAULT;
        }
        else {
			mVideoStream->codec->skip_loop_filter = (AVDiscard)mDiscardLevel;
			//for test
			//mVideoStream->codec->skip_frame = (AVDiscard)mDiscardLevel;
			//LOGD("notify ffmpeg to do more optimization. mDiscardLevel:%d, mDiscardCount:%d,", mDiscardLevel, mDiscardCount);
        }
    }
    else
    {
        mVideoStream->codec->skip_loop_filter = AVDISCARD_DEFAULT;
        //for test
		//mVideoStream->codec->skip_frame = AVDISCARD_DEFAULT;
    }
}

int64_t FFPlayer::getFramePTS_l(AVFrame* frame)
{
    int64_t pts = 0;

    if(av_frame_get_best_effort_timestamp(frame) != AV_NOPTS_VALUE)
    {
        LOGV("use av_frame_get_best_effort_timestamp");
        pts = av_frame_get_best_effort_timestamp(frame);
    }
    else if(frame->pts != AV_NOPTS_VALUE)
    {
        LOGV("use frame->pts");
        pts = frame->pts;
    }
    else if(frame->pkt_pts != AV_NOPTS_VALUE)
    {
        LOGV("use frame->pkt_pts");
        pts = frame->pkt_pts;
    }
    else if(frame->pkt_dts != AV_NOPTS_VALUE)
    {
        LOGV("use frame->pkt_dts");
        pts = frame->pkt_dts;
    }

    return pts;
}

#ifdef __ANDROID__
status_t FFPlayer::startCompatibilityTest()
{
    LOGD("startCompatibilityTest ffplayer");
    status_t ret = ERROR;
    AVFormatContext* movieFile = avformat_alloc_context();
    const int maxLen = 300;
    const char* fileName = "lib/libsample.so";
    int32_t pathLen = strlen("/data/data/com.pplive.androidphone") + strlen(fileName)+1;
    if(pathLen >= maxLen) return ERROR;
    char path[maxLen];
    memset(path, 0, maxLen);
    strcat(path, "/data/data/com.pplive.androidphone");
    strcat(path, fileName);
    LOGD("path:%s", path);
    if(!avformat_open_input(&movieFile, path, NULL, NULL))
    {
        AVStream* videoStream = NULL;
        uint32_t streamsCount = movieFile->nb_streams;
        int32_t videoStreamIndex = 0;
        for (int32_t j = 0; j < (int)streamsCount; j++)
        {
    		if (movieFile->streams[j]->codec->codec_type == AVMEDIA_TYPE_VIDEO)
            {
    		    videoStreamIndex = j;
                videoStream = movieFile->streams[videoStreamIndex];
                break;
    		}
    	}

        if(videoStream != NULL)
        {
            videoStream->codec->pix_fmt = AV_PIX_FMT_YUV420P;
            AVFrame* videoFrame = av_frame_alloc();

            AVCodecContext* codec_ctx = videoStream->codec;
        	AVCodec* codec = avcodec_find_decoder(codec_ctx->codec_id);
        	if (codec != NULL && avcodec_open2(codec_ctx, codec, NULL) >= 0)
            {
                LOGD("getpriority before:%d", getpriority(PRIO_PROCESS, 0));
                LOGD("sched_getscheduler:%d", sched_getscheduler(0));
                int videoThreadPriority = -16;
                if(setpriority(PRIO_PROCESS, 0, videoThreadPriority) != 0)
                {
                    LOGE("set video thread priority failed");
                }
                LOGD("getpriority after:%d", getpriority(PRIO_PROCESS, 0));

            	int64_t begin_test = getNowMs();
                int64_t testCostMs = 0;
                int32_t picCount = 0;
                int32_t minFPS = 25;
                int64_t minTestMs = 1200;
                while((picCount < minFPS) && (testCostMs < minTestMs))
                {
                    AVPacket* pPacket = (AVPacket*)av_malloc(sizeof(AVPacket));
                    int32_t ret = av_read_frame(movieFile, pPacket);
					if (ret < 0)
						break;

                    if (pPacket->stream_index == videoStreamIndex)
                    {
                        //video
                    	LOGV("video Packet->pts:%lld", pPacket->pts);
                    	LOGV("video Packet->dts:%lld", pPacket->dts);

                        // Decode video frame
                    	int64_t begin_decode = getNowMs();
                        int32_t gotPicture = 0;
                    	int len = avcodec_decode_video2(videoStream->codec,
                            						 videoFrame,
                            						 &gotPicture,
                            						 pPacket);
                    	int64_t end_decode = getNowMs();
                        int64_t costTime = end_decode-begin_decode;
                    	LOGI("decode video cost %lld[ms]", costTime);
                        if (len >= 0)
                        {
                    		if(gotPicture!=0)
                    		{
                    		    picCount++;
                    		}
                    	}
                    }
                    if(pPacket != NULL)
                    {
                        //free packet resource
            	        av_free_packet(pPacket);
                        av_free(pPacket);
                        pPacket = NULL;
                    }
                	testCostMs = getNowMs() - begin_test;
                }
                LOGI("picCount:%d, testCostMs:%lld", picCount, testCostMs);
                ret = (testCostMs<850)?OK:ERROR; //it is an experience value, do not change it.

                if(setpriority(PRIO_PROCESS, 0, 0) != 0)
                {
                    LOGE("set video thread priority back failed");
                }
                LOGD("getpriority after:%d", getpriority(PRIO_PROCESS, 0));

                /*
                {
                    FILE *pFile;
                    char* path = "/data/data/com.pplive.meetplayer/test.txt";
                	LOGD("Start open file %s", path);
                	pFile=fopen(path, "wb");
                	if(pFile==NULL)
                	{
                		LOGE("open file %s failed", path);
                		return ERROR;
                	}
                	LOGD("open file %s success", path);

                    int64_t data2 = 0x123;
                    int data[4] = {(int)testCostMs,data2};
                	fwrite(data, 1, 8, pFile);
                	fclose(pFile);
                }
                */

                if(codec_ctx != NULL)
                {
                    LOGD("avcodec_close video codec");
                    avcodec_close(codec_ctx);
                }
            }

            if(videoFrame != NULL)
            {
                LOGD("avcodec_free_frame");
                av_frame_free(&videoFrame);
                videoFrame = NULL;
            }
        }
    }
    else
    {
        LOGE("avformat_open_input failed");
    }

    if(movieFile != NULL)
    {
        // Close stream
        LOGD("avformat_close_input");
        avformat_close_input(&movieFile);
    }
    return ret;
}
#endif

status_t FFPlayer::getBufferingTime(int *msec)
{
    if(mDataStream != NULL)
    {
        return mDataStream->getBufferingTime(msec);
    }
    else
    {
        *msec = 0;
        return OK;
    }
}

bool FFPlayer::getMediaInfo(const char* url, MediaInfo* info)
{
    if(url == NULL || info == NULL) return false;

    bool ret = false;

	struct stat buf;
    int32_t iresult = stat(url, &buf);
    if (0 == iresult) {
		info->size_byte = buf.st_size;
	}
	else {
		LOGW("cannnot get filesize: %s, %d", url, iresult);
        info->size_byte = 0;
	}

    AVFormatContext* movieFile = avformat_alloc_context();
    LOGD("check file %s", url);
    if(!avformat_open_input(&movieFile, url, NULL, NULL))
    {
        if(movieFile->duration <= 0)
        {
        	if(avformat_find_stream_info(movieFile, NULL) >= 0)
            {
                ret = true;
                info->duration_ms = (int32_t)(movieFile->duration * 1000 / AV_TIME_BASE);
        	}
            else
            {
                LOGE("failed to avformat_find_stream_info: %s", url);
            }
        }
        else
        {
            ret = true;
            info->duration_ms = (int32_t)(movieFile->duration * 1000 / AV_TIME_BASE);
        }
    }
    else
    {
        LOGE("failed to avformat_open_input: %s", url);
    }

    if(movieFile != NULL)
    {
        // Close stream
        LOGD("avformat_close_input");
        avformat_close_input(&movieFile);
    }
    LOGD("File duration:%d", info->duration_ms);
    LOGD("File size:%lld", info->size_byte);
    return ret;
}

bool generateThumbnail(AVFrame* videoFrame,
    int32_t* thumbnail,
    int32_t width , int32_t height)
{
    //int32_t srcWidth = (width < height) ? width : height;
    //int32_t srcHeight = srcWidth;
	
	int ret;
	bool result = false;
	enum AVPixelFormat out_fmt = AV_PIX_FMT_NONE;
#ifdef __ANDROID__
	out_fmt = AV_PIX_FMT_BGRA;
#else
	// for common case(including IOS)
	out_fmt = AV_PIX_FMT_RGBA;
#endif

    struct SwsContext* convertCtx = sws_getContext(videoFrame->width,
								 videoFrame->height,
								 (PixelFormat)videoFrame->format,
								 width,
								 height,
								 out_fmt,
								 SWS_POINT,
								 NULL,
								 NULL,
								 NULL);
	if (convertCtx == NULL) {
		LOGE("failed to create sws context");
		return false;
	}

    LOGD("generateThumbnail: %dx%d, fmt %d", videoFrame->width, videoFrame->height, videoFrame->format);

    AVFrame* surfaceFrame = av_frame_alloc();
	if (surfaceFrame) {
		ret = avpicture_fill((AVPicture*)surfaceFrame,
                        (uint8_t*)thumbnail,
                        AV_PIX_FMT_BGRA,
                        width,
                        height);
		if (ret < 0) {
			LOGE("failed to avpicture_fill, ret: %d", ret);
		}
		else {
			// Convert the image
			ret = sws_scale(convertCtx,
				videoFrame->data,
				videoFrame->linesize,
				0,
				videoFrame->height,
				surfaceFrame->data,
				surfaceFrame->linesize);
			if (ret == height) {
				LOGD("sws_scale thumbnail success");
				result = true;
			}
			else {
				LOGE("failed to sws_scale, %d %d", ret, height);
			}
		}
	}
	else {
		LOGE("alloc frame failed");
	}

    if (surfaceFrame != NULL)
		av_frame_free(&surfaceFrame);

    sws_freeContext(convertCtx);
    convertCtx = NULL;

    /*
    {
        static bool pass = false;
        if(!pass)
        {
            FILE *pFile;
            char* path = "/sdcard/test/dump_thumbnail.565";
        	LOGD("Start open file %s", path);
        	pFile=fopen(path, "wb");
        	if(pFile==NULL)
        	{
        		LOGE("open file %s failed", path);
        		return result;
        	}
        	LOGD("open file %s success", path);

        	fwrite(thumbnail, 1, width*height*2, pFile);
        	fclose(pFile);
            pass = true;
        }
    }
    */
    return result;
}

bool getStreamLangTitle(char** langcode, char** langtitle, int index, AVStream* stream)
{
    bool gotlanguage = false;

	if (langcode == NULL || langtitle == NULL)
		return false;

	if (stream == NULL || stream->metadata == NULL)
		return false;

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
		LOGI("stream index: #%d(lang %s, title: %s)", index, 
			*langcode ? *langcode : "N/A", 
			*langtitle ? *langcode : "N/A");
	}

    return gotlanguage;
}

bool FFPlayer::getMediaDetailInfo(const char* url, MediaInfo* info)
{
    if (url == NULL || info == NULL)
		return false;

    struct stat buf;
    int32_t iresult = stat(url, &buf);
    if (0 == iresult) {
		info->size_byte = buf.st_size;
	}
	else {
		LOGW("cannnot get filesize: %s, %d", url, iresult);
        info->size_byte = 0;
	}

    AVFormatContext* movieFile = avformat_alloc_context();
    LOGD("check file %s", url);
    if (0 != avformat_open_input(&movieFile, url, NULL, NULL)) {
		LOGE("failed to avformat_open_input: %s", url);
		return false;
	}
    
	if(avformat_find_stream_info(movieFile, NULL) < 0) {
		LOGE("failed to avformat_find_stream_info: %s", url);
		return false;
	}

	info->duration_ms = (int32_t)(movieFile->duration * 1000 / AV_TIME_BASE);
	info->format_name = movieFile->iformat->name;

	uint32_t streamsCount = movieFile->nb_streams;
	LOGD("streamsCount:%d", streamsCount);

	info->channels = streamsCount;

	info->audio_channels	= 0;
	info->video_channels	= 0;
	info->subtitle_channels = 0;

	for (int32_t i = 0; i < (int)streamsCount; i++) {
		if (movieFile->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
			info->video_channels++;
			AVStream* stream = movieFile->streams[i];
			if(stream == NULL)
			{
				LOGE("stream is NULL");
				continue;
			}
			AVCodecContext* codec_ctx = stream->codec;
			if(codec_ctx == NULL)
			{
				LOGE("codec_ctx is NULL");
				continue;
			}
			info->width = codec_ctx->width;
			info->height = codec_ctx->height;

			AVCodec* codec = avcodec_find_decoder(codec_ctx->codec_id);
			if (codec == NULL)
			{
				LOGE("avcodec_find_decoder() video failed");
				continue;
			}

			info->videocodec_name = codec->name;
		}
		else if (movieFile->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
			AVStream* stream = movieFile->streams[i];
			AVCodec* codec = avcodec_find_decoder(stream->codec->codec_id);

			if (codec == NULL) {
				LOGE("avcodec_find_decoder audio failed");
				notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_UNSUPPORTED, 0);
				continue;
			}

			int audio_index = info->audio_channels;
			info->audio_streamIndexs[audio_index] = i;

			int len = strlen(codec->name) + 1;
			info->audiocodec_names[audio_index] = new char[len];
			strcpy(info->audiocodec_names[audio_index], codec->name);
			getStreamLangTitle(&(info->audio_languages[audio_index]), &(info->audio_titles[audio_index]), i, stream);

			info->audio_channels++;
		}
		else if(movieFile->streams[i]->codec->codec_type == AVMEDIA_TYPE_SUBTITLE) {
			AVStream* stream = movieFile->streams[i];
			AVCodec* codec = avcodec_find_decoder(stream->codec->codec_id);

			if (codec == NULL) {
				LOGW("avcodec_find_decoder subtitle failed");
				continue;
			}

			int subtitle_index = info->subtitle_channels;
			info->subtitle_streamIndexs[subtitle_index] = i;

			int len = strlen(codec->name) + 1;
			info->subtitlecodec_names[subtitle_index] = new char[len];
			strcpy(info->subtitlecodec_names[subtitle_index], codec->name);
			getStreamLangTitle(&(info->subtitle_languages[subtitle_index]), &(info->subtitle_titles[subtitle_index]), i, stream);

			info->subtitle_channels++;
		}
	} // end of for() get stream info

	if (movieFile != NULL) {
        // Close stream
        LOGD("avformat_close_input");
        avformat_close_input(&movieFile);
    }

    LOGD("File duration:%d", info->duration_ms);
    LOGD("File size:%lld", info->size_byte);
    LOGD("width:%d", info->width);
    LOGD("height:%d", info->height);
    LOGD("format name:%s", info->format_name!=NULL?info->format_name:"");
    LOGD("audio name:%s", info->audio_name!=NULL?info->audio_name:"");
    LOGD("video name:%s", info->video_name!=NULL?info->video_name:"");
    LOGD("audio_channels:%d", info->audio_channels);
    LOGD("video_channels:%d", info->video_channels);
    return true;
}

bool FFPlayer::getThumbnail(const char* url, MediaInfo* info)
{
	LOGD("getThumbnail()");

	if (url == NULL || info == NULL)
		return false;

    bool ret = false;

    struct stat buf;
    int32_t iresult = stat(url, &buf);
    if (iresult == 0)
        info->size_byte = buf.st_size;
    else {
		LOGE("failed to stat: %s", url);
        return false;
	}


    AVFormatContext* movieFile = avformat_alloc_context();
    LOGD("check file %s", url);
    if(!avformat_open_input(&movieFile, url, NULL, NULL))
    {
    	if(avformat_find_stream_info(movieFile, NULL) >= 0)
        {
            info->duration_ms = (int32_t)movieFile->duration * 1000 / AV_TIME_BASE;
            info->format_name = movieFile->iformat->name;

            uint32_t streamsCount = movieFile->nb_streams;
            LOGD("streamsCount:%d", streamsCount);

            info->audio_channels = 0;
            info->video_channels = 0;
        	for (int32_t i = 0; i < (int)streamsCount; i++) {
        		if (movieFile->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO)
                {
                    AVStream* stream = movieFile->streams[i];
					AVCodec* codec = avcodec_find_decoder(stream->codec->codec_id);
                    if (codec != NULL) {
						int len = strlen(codec->name) + 1;
						info->audiocodec_names[info->audio_channels] = new char[len];
                        strcpy(info->audiocodec_names[info->audio_channels], codec->name);
					}

					info->audio_channels++;
        		}
                else if(movieFile->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO
                    && info->video_channels == 0)
                {
                    info->video_channels++;
                    AVStream* stream = movieFile->streams[i];
                    if(stream == NULL)
                    {
                        LOGE("stream is NULL");
                        continue;
                    }
                    AVCodecContext* codec_ctx = stream->codec;
                    if(codec_ctx == NULL)
                    {
                        LOGE("codec_ctx is NULL");
                        continue;
                    }
                    info->width = codec_ctx->width;
                    info->height = codec_ctx->height;

                	AVCodec* codec = avcodec_find_decoder(codec_ctx->codec_id);
                	if (codec == NULL)
                    {
                        LOGE("avcodec_find_decoder failed");
                        continue;
                    }

                    info->videocodec_name = codec->name;
                	if (avcodec_open2(codec_ctx, codec, NULL) >= 0)
                    {
                        int32_t seekPosition = 15;
                        if(info->duration_ms > 0 && info->duration_ms < seekPosition * 1000)
                        {
                            seekPosition = info->duration_ms / 1000;
                        }
                        if(avformat_seek_file(movieFile,
                            -1,
                            INT64_MIN,
                            seekPosition * AV_TIME_BASE, //in AV_TIME_BASE
                            INT64_MAX,
                            0) >= 0)
                        {
                            bool tryThumbnail = true;
							AVFrame* videoFrame = av_frame_alloc();
                            while(tryThumbnail)
                            {
                                AVPacket* pPacket = (AVPacket*)av_malloc(sizeof(AVPacket));
                                memset(pPacket, 0, sizeof(AVPacket));
                                int32_t readRet = av_read_frame(movieFile, pPacket);
                                if(readRet != 0)
                                {
                                    LOGE("av_read_frame error: %d", readRet);
                                    return ret;
                                }

                                if (pPacket->stream_index == i)
                                {
                                    
                                    int32_t gotPicture = 0;
                                	int len = avcodec_decode_video2(codec_ctx,
                                        						 videoFrame,
                                        						 &gotPicture,
                                        						 pPacket);
                                    if (len >= 0 && gotPicture != 0)
                                    {
                                        LOGD("got picture");
                                        //todo:get 96x96 argb8888 data.
                                        info->thumbnail_width = 96;
                                        info->thumbnail_height = 96;
                                        info->thumbnail = (int*)malloc(info->thumbnail_width*info->thumbnail_height*4);
                                        if(generateThumbnail(videoFrame, info->thumbnail, info->thumbnail_width, info->thumbnail_height))
                                        {
                                            ret = true;
                                            LOGD("got thumbnail");
                                        }
                                        else
                                        {
                                            LOGE("failed to get thumbnail");
                                        }
                                        tryThumbnail = false;
                                	}
                                }
                                if(pPacket != NULL)
                                {
                                    //free packet resource
                                    LOGD("av_free_packet");
                        	        av_free_packet(pPacket);
                                    av_free(pPacket);
                                    pPacket = NULL;
                                }
                            } // end of while(read_frame)
							if (videoFrame != NULL) {
								av_frame_free(&videoFrame);
							}
                        }// end of seek_file

                        if(codec_ctx != NULL) {
                            LOGD("avcodec_close video codec");
                            avcodec_close(codec_ctx);
                        }
                    }
                }
        	}
    	}
        else
        {
            LOGE("failed to avformat_find_stream_info: %s", url);
        }
    }
    else
    {
        LOGE("failed to avformat_open_input: %s", url);
    }

    if(movieFile != NULL)
    {
        // Close stream
        LOGD("avformat_close_input");
        avformat_close_input(&movieFile);
    }
    LOGD("File duration:%d", info->duration_ms);
    LOGD("File size:%lld", info->size_byte);
    LOGI("File width %d, height %d", info->width, info->height);
    LOGD("format name:%s", info->format_name!=NULL ? info->format_name : "");
    LOGD("audio name:%s", info->audio_name!=NULL ? info->audio_name : "");
    LOGD("video name:%s", info->video_name!=NULL ? info->video_name : "");
    LOGI("thumbnail: width %d, height %d", info->thumbnail_width, info->thumbnail_height);
    LOGI("thumbnail:%d", (info->thumbnail!=NULL));
    LOGD("video_channels:%d, audio_channels:%d", info->video_channels, info->audio_channels);
    return ret;
}

bool FFPlayer::getThumbnail2(const char* url, MediaInfo* info)
{
	LOGI("getThumbnail2()");

	if (url == NULL || info == NULL)
		return false;

    struct stat buf;
    int32_t iresult = stat(url, &buf);
    if (iresult == 0)
        info->size_byte = buf.st_size;
    else {
		LOGE("failed to stat: %s", url);
        return false;
	}

	int ret = 0;
	int got_thumbnail = 0;

    AVFormatContext* movieFile = avformat_alloc_context();
	AVStream *video_stream = NULL;
	AVCodecContext *video_dec_ctx = NULL;
	AVFrame *frame;
	int video_stream_idx;

	AVStream *audio_stream = NULL;
	AVCodecContext *audio_dec_ctx = NULL;
	int audio_stream_idx;

	AVPacket pkt;

	int64_t seekPosition = 15; // sec
	int64_t seek_target;
	int stream_index = -1;

	/* open input file, and allocate format context */
    if (avformat_open_input(&movieFile, url, NULL, NULL) < 0) {
        LOGE("Could not open media file %s", url);
        return false;
    }

	/* retrieve stream information */
    if (avformat_find_stream_info(movieFile, NULL) < 0) {
        LOGE("Could not find stream information: %s", url);
        goto end;
    }

	if (open_codec_context(&video_stream_idx, movieFile, AVMEDIA_TYPE_VIDEO) >= 0) {
        video_stream = movieFile->streams[video_stream_idx];
        video_dec_ctx = video_stream->codec;
		info->width = video_dec_ctx->width;
        info->height = video_dec_ctx->height;
		info->videocodec_name = video_dec_ctx->codec->name;

        /* allocate image where the decoded image will be put */
        //alloc_picture(video_dec_ctx->pix_fmt, video_dec_ctx->width, video_dec_ctx->height);
    }

    if (open_codec_context(&audio_stream_idx, movieFile, AVMEDIA_TYPE_AUDIO) >= 0) {
        audio_stream = movieFile->streams[audio_stream_idx];
        audio_dec_ctx = audio_stream->codec;
		info->audiocodec_names[0] = (char *)audio_dec_ctx->codec->name;
    }

	/* dump input information to stderr */
    av_dump_format(movieFile, 0, url, 0);

	if (!audio_stream && !video_stream) {
        LOGE("Could not find audio or video stream in the input, aborting");
        ret = 1;
        goto end;
    }

	info->duration_ms = (int32_t)movieFile->duration * 1000 / AV_TIME_BASE;
	info->format_name = movieFile->iformat->name;

	frame = av_frame_alloc();
    if (!frame) {
        LOGE("Could not allocate frame");
        ret = AVERROR(ENOMEM);
        goto end;
    }

	if(info->duration_ms > 0 && info->duration_ms < seekPosition * 1000)
		seekPosition = info->duration_ms / 1000;
	seek_target = seekPosition * AV_TIME_BASE;

	if (video_stream_idx >= 0)
		stream_index = video_stream_idx;
	else if (audio_stream_idx >= 0)
		stream_index = audio_stream_idx;

	if (stream_index < 0) {
		LOGE("no stream to seek");
		goto end;
	}

#ifdef _MSC_VER
	AVRational ra;
	ra.num = 1;
	ra.den = AV_TIME_BASE;
	seek_target = av_rescale_q(seek_target, ra, movieFile->streams[stream_index]->time_base);
#else
	seek_target= av_rescale_q(seek_target, AV_TIME_BASE_Q, movieFile->streams[stream_index]->time_base);
#endif
    if (av_seek_frame(movieFile, -1, seek_target, 0) < 0) {
        LOGE("failed to seek file");
		goto end;
    }

	/* initialize packet, set data to NULL, let the demuxer fill it */
    av_init_packet(&pkt);
    pkt.data = NULL;
    pkt.size = 0;

	/* read frames from the file */
    while (av_read_frame(movieFile, &pkt) >= 0) {
		AVPacket orig_pkt = pkt;
		if(pkt.stream_index == video_stream_idx) {
			int got_frame;
			do {
				/* decode video frame */
				ret = avcodec_decode_video2(video_dec_ctx, frame, &got_frame, &pkt);
				if (ret < 0) {
					LOGE("Error decoding video frame (%d)\n", ret);
					break;
				}

				if (got_frame) {
					info->thumbnail_width = 96;
                    info->thumbnail_height = 96;
                    info->thumbnail = (int*)malloc(info->thumbnail_width * info->thumbnail_height * 4);
                    if (generateThumbnail(frame, info->thumbnail, info->thumbnail_width, info->thumbnail_height)) {
						got_thumbnail = 1;
						LOGI("generateThumbnail");
					}
					av_frame_unref(frame);
				}
					
				pkt.data += ret;
				pkt.size -= ret;
			} while (pkt.size > 0);
		}
        av_free_packet(&orig_pkt);

		if(got_thumbnail)
			break;
    }

end:
    avcodec_close(video_dec_ctx);
    avcodec_close(audio_dec_ctx);
    avformat_close_input(&movieFile);
    av_frame_free(&frame);

	return (got_thumbnail > 0);
}

SnapShot * FFPlayer::getSnapShot(int width, int height, int fmt, int msec)
{
	LOGI("getSnapShot()");

	if (mPlayerStatus != MEDIA_PLAYER_STARTED && mPlayerStatus != MEDIA_PLAYER_PAUSED) {
		LOGE("getSnapShot() player isn't ready");
		return NULL;
	}

	//AutoLock autolock(&mSnapShotLock);

	AVFrame *SrcFrame = mVideoFrame;
	if (NULL == SrcFrame || 
		SrcFrame->width == 0 || SrcFrame->height == 0 || 
		SrcFrame->format == AV_PIX_FMT_NONE) {
		LOGE("getSnapShot() source frame is null");
		return NULL;
	}

	LOGI("src frame: %d x %d, fmt=%d, data[] %p %p %p, linesize[] %d %d %d", SrcFrame->width, SrcFrame->height, SrcFrame->format, 
		SrcFrame->data[0], SrcFrame->data[1], SrcFrame->data[2], 
		SrcFrame->linesize[0], SrcFrame->linesize[1], SrcFrame->linesize[2]);

	const int swsFlags = SWS_POINT;
	int ret;
	
#if defined(__ANDROID__) || defined(_MSC_VER)
	AVPixelFormat output_fmt = AV_PIX_FMT_BGRA;
#else
	// for common case(including IOS)
	AVPixelFormat output_fmt = AV_PIX_FMT_RGBA;
#endif

	int output_width, output_height;

	if (width != 0 && height != 0) {
		output_width = width;
		output_height = height;
	}
	else {
		output_width = SrcFrame->width;
		output_height = SrcFrame->height;
	}

	if (!mSnapShotFrame) {
		mSnapShotFrame			= av_frame_alloc();
		if (NULL == mSnapShotFrame) {
			LOGE("failed to create snapshot frame");
			return NULL;
		}

		mSnapShotFrame->width	= output_width;
		mSnapShotFrame->height	= output_height;
		mSnapShotFrame->format	= output_fmt;
		
		ret = av_frame_get_buffer(mSnapShotFrame, 1);
		if (ret != 0) {
			LOGE("failed to get frame buffer: %d", ret);
			return NULL;
		}
	}

	if (!mSwsCtx) {
		mSwsCtx = sws_getContext(
			SrcFrame->width, SrcFrame->height, 
			(AVPixelFormat)SrcFrame->format,
			output_width, output_height, 
			output_fmt, 
			swsFlags, NULL, NULL, NULL);
		if (mSwsCtx == NULL) {
			LOGE("failed to create ctx: width:%d, height:%d, pix:%d",
				output_width, output_height, output_fmt);
			return NULL;
		}
	}

	LOGI("getSnapShot before do sws_scale");
	AutoLock autolock(&mSnapShotLock);
	ret = sws_scale(mSwsCtx,
		SrcFrame->data,
		SrcFrame->linesize,
		0,
		SrcFrame->height,
		mSnapShotFrame->data,
		mSnapShotFrame->linesize);
	if (ret < 0) {
		LOGE("failed to do snapshot sws: %d", ret);
		return NULL;
	}

	SnapShot *ss = (SnapShot *)malloc(sizeof(SnapShot));
	memset(ss, 0, sizeof(SnapShot));

	ss->width			= output_width;
	ss->height			= output_height;
	ss->stride			= output_width;
	ss->picture_fmt		= 0;
	ss->picture_data	= mSnapShotFrame->data[0];

	LOGI("getSnapShot() done! %dx%d(stride %d, fmt %d)", ss->width, ss->height, ss->stride, ss->picture_fmt);
	return ss;
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
		default:
			LOGI("%s", log);
			break;
	}
}

static int open_codec_context(int *stream_idx,
                              AVFormatContext *fmt_ctx, enum AVMediaType type)
{
    int ret;
    AVStream *st;
    AVCodecContext *dec_ctx = NULL;
    AVCodec *dec = NULL;
    AVDictionary *opts = NULL;

    ret = av_find_best_stream(fmt_ctx, type, -1, -1, NULL, 0);
    if (ret < 0) {
        LOGE("Could not find %s stream in input file", av_get_media_type_string(type));
        return ret;
    } else {
        *stream_idx = ret;
        st = fmt_ctx->streams[*stream_idx];

        /* find decoder for the stream */
        dec_ctx = st->codec;
        dec = avcodec_find_decoder(dec_ctx->codec_id);
        if (!dec) {
            fprintf(stderr, "Failed to find %s codec\n",
                    av_get_media_type_string(type));
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

