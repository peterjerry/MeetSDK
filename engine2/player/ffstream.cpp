#include "ffstream.h"

#define __STDINT_LIMITS
#ifndef _MSC_VER
#include <sys/resource.h>
#include <stdint.h>
#include <unistd.h>
#include <sys/time.h>
#endif
#ifdef __ANDROID__
#include <sys/sysinfo.h>
#include <jni.h> // for detach jni thread
extern JavaVM* gs_jvm;
#endif
#include <sched.h> // in pthread

#define LOG_TAG "Neon-FFStream"
#include "log.h"
#include "ppffmpeg.h"
#if defined(_MSC_VER) || defined(__CYGWIN__)
#include "pperrors.h"
#else
#include "errors.h"
#endif
#include "autolock.h"
#include "utils.h"
#include "ffsourcebase.h"

#include "subtitle.h"

#define FF_PLAYER_MIN_BUFFER_SECONDS_LOCAL_FILE			1 //sec
#if defined(_MSC_VER) || defined(__CYGWIN__)
#define FF_PLAYER_MIN_BUFFER_SECONDS_VOD				1 //sec
#else
#define FF_PLAYER_MIN_BUFFER_SECONDS_VOD				4 //sec
#endif

#define FF_PLAYER_MIN_BUFFER_MILISECONDS_BROADCAST_HR	0 //ms
#define FF_PLAYER_MAX_BUFFER_MILISECONDS_BROADCAST_HR	400 //ms

#define FF_PLAYER_MIN_BUFFER_MILISECONDS_BROADCAST_LR	3000 //ms
#define FF_PLAYER_MAX_BUFFER_MILISECONDS_BROADCAST_LR	60000 //ms

#define FF_PLAYER_MAX_QUEUE_SIZE_DEFAULT				(1048576 * 4) //10MB -> 4MB

#define FF_PLAYER_DEFAULT_GOP_DURATION					1000 // 1 sec
#define FF_PLAYER_INVALID_GOP_DURATION					(100 * 1000) // 100 sec

#define PB_BUF_SIZE										65536

#define MEDIA_OPEN_TIMEOUT_MSEC							(120 * 1000) // 2 min
#define MEDIA_READ_TIMEOUT_MSEC							(300 * 1000) // 5 min

static bool getStreamLangTitle(char** langcode, char** langtitle, int index, AVStream* stream);

FFStream::FFStream()
{
	mSource				= NULL;
    mMovieFile			= NULL;
    mBufferSize			= 0;
    mMinPlayBufferCount = 0;
    mMaxPlayBufferMs	= 1000;
    mMaxBufferSize		= FF_PLAYER_MAX_QUEUE_SIZE_DEFAULT;
    mStatus				= FFSTREAM_INITED;

    mReachEndStream		= false;
    mIsBuffering		= false;
    mSeeking			= false;
    mSeekTimeMs			= 0;
    mCachedDurationMs	= 0;
    mListener			= NULL;
    mUrlType			= TYPE_UNKNOWN;
    mDelaying			= false;
    mRefreshed			= false;
	mLooping			= false;
	mStartTimeMs		= 0;
    mGopDuration		= FF_PLAYER_DEFAULT_GOP_DURATION;
    mGopStart			= 0;
    mGopEnd				= 0;
    mRealtimeLevel		= LEVEL_LOW;

    mAudioStream			= NULL;
    mVideoStream			= NULL;
	mSubtitleStream			= NULL;
	mAVSubtitle				= NULL;
	mISubtitle				= NULL;
	mAudioStreamIndex		= -1; // invalid stream id
	mVideoStreamIndex		= -1; // invalid stream id
	mSubtitleStreamIndex	= -1; // invalid stream id
	mSubtitleTrackFirstIndex= -1;
	mSubtitleTrackIndex		= -1; // for ISubtitle
    mStreamsCount			= 0;
    mDurationMs				= 0;
    mFrameRate				= 0;

    pthread_cond_init(&mCondition, NULL);
    pthread_mutex_init(&mLock, NULL);

	pthread_mutex_init(&mSubtitleLock, NULL);

	m_io_bit_rate			= 0;
	m_real_bit_rate			= 0;
	m_vb_index				= 0;
	m_total_read_bytes		= 0;
	mOpenStreamStartMs		= 0;
	mReadStreamStartMs		= 0;

	for(int i=0;i<MAX_CALC_SEC;i++)
		m_read_bytes[i] = 0;

    //audio
    //pthread_mutex_init(&mAudioQueueLock, NULL);
    //mAudioBufferSize = 0;

    //video
    //pthread_mutex_init(&mVideoQueueLock, NULL);
    //mVideoBufferSize = 0;
}

FFStream::~FFStream()
{
	LOGI("FFStream start to destruct");
    if (mStatus == FFSTREAM_STARTED)
        stop();

	if (mMovieFile != NULL) {
        // Close stream
        LOGI("avformat_close_input");

		if (mSubtitleStream) {
			avcodec_close(mSubtitleStream->codec);
			mSubtitleStream = NULL;
		}

		if (mAVSubtitle) {
			delete mAVSubtitle;
			mAVSubtitle = NULL;
		}

		if (mSource) {
			mMovieFile->pb->opaque = NULL;
			avio_close(mMovieFile->pb);
			av_free(mMovieFile);
			mMovieFile = NULL;
		}
		else {
			mMovieFile->interrupt_callback.callback = NULL;
			mMovieFile->interrupt_callback.opaque = NULL;
			avformat_close_input(&mMovieFile);
		}
    }

    //pthread_mutex_destroy(&mAudioQueueLock);
    //pthread_mutex_destroy(&mVideoQueueLock);
    pthread_cond_destroy(&mCondition);
    pthread_mutex_destroy(&mLock);
	pthread_mutex_destroy(&mSubtitleLock);
	LOGI("FFStream destructed");
}

int FFStream::ff_read_packet(void *opaque, uint8_t *buf, int buf_size)
{
	//LOGD("ff_read_packet");

	FFStream *pIns = (FFStream *)opaque;
	FFSourceBase *pSource = NULL;

	pSource = pIns->getSource();
	if(!pSource) {
		LOGE("source not set");
		return -1;
	}

	int ret = pSource->read_data((char *)buf, (unsigned int)buf_size);
	LOGD("FFDemuxer: read_packet %d", ret);
	return ret;
}

int64_t FFStream::ff_seek_packet(void *opaque, int64_t offset, int whence)
{
	LOGI("FFStream: seek_packet offset %lld, whence %d", offset, whence);
	
	FFStream *pIns = (FFStream *)opaque;
	FFSourceBase *pSource = NULL;

	pSource = pIns->getSource();
	if(!pSource) {
		LOGE("source not set");
		return -1;
	}

	if (AVSEEK_SIZE == whence) {
		int64_t size = pSource->get_size();
#ifdef _MSC_VER
		LOGI("AVSEEK_SIZE: filesize %I64d", size);
#else
		LOGI("AVSEEK_SIZE: filesize %lld", size);
#endif
		return (int64_t)size;
	}
	else if (AVSEEK_FORCE == whence) {
		LOGW("AVSEEK_FORCE");
	}

	return (int64_t)pSource->read_seek(offset, whence);
}

status_t FFStream::setLooping(bool loop)
{
	mLooping = loop;
    return OK;
}

status_t FFStream::selectAudioChannel(int32_t index)
{
	// 2014.12.2 guoliang.ma added to support audio track switch when playing
	if (FFSTREAM_INITED == mStatus || FFSTREAM_STOPPED == mStatus || FFSTREAM_STOPPING == mStatus) {
		LOGE("wrong state(%d) to selectAudioChannel: %d", mStatus, index);
		return INVALID_OPERATION;
	}

	if (index >= (int32_t)mStreamsCount) {
		LOGE("select stream index is invalid: #%d(total stream number %d)", index, mStreamsCount);
		return ERROR;
	}

	if (mMovieFile->streams[index]->codec->codec_type != AVMEDIA_TYPE_AUDIO) {
		LOGE("select stream is no an audio stream: %d", index);
		return ERROR;
	}

	// 2015.3.18 guoliangma fix unfree resource
	// 2015.3.28 guoliangma cannot release here because audio player is decoding
	// will cause crash
	//avcodec_close(mMovieFile->streams[mAudioStreamIndex]->codec);

	if (mAudioStreamIndex == index) {
		LOGI("audio channel is already in use: #%d", mAudioStreamIndex);
		return OK;
	}

	LOGI("audio channel change from #%d to #%d", mAudioStreamIndex, index);
	
	mAudioStreamIndex = index;
	mAudioStream = mMovieFile->streams[mAudioStreamIndex];

    return OK;
}

status_t FFStream::selectSubtitleChannel(int32_t index)
{
	// only set mISubtitle will do subtitle parse and decode
	if (!mISubtitle) {
		LOGW("ISubtitle is not set, cannot selectSubtitleChannel %d", index);
		return OK;
	}

	if (FFSTREAM_INITED == mStatus || FFSTREAM_STOPPED == mStatus || FFSTREAM_STOPPING == mStatus) {
		LOGE("wrong state(%d) to selectSubtitleChannel: %d", mStatus, index);
		return INVALID_OPERATION;
	}

	if (index >= (int32_t)mStreamsCount) {
		LOGE("select stream index is invalid: #%d(total stream number %d)", index, mStreamsCount);
		return ERROR;
	}

	if (mMovieFile->streams[index]->codec->codec_type != AVMEDIA_TYPE_SUBTITLE) {
		LOGE("select stream is no an subtitle stream: %d", index);
		return ERROR;
	}

	if (mSubtitleStreamIndex == index) {
		LOGI("subtitle channel is already in use: #%d", mSubtitleStreamIndex);
		return OK;
	}

	LOGI("subtitle channel change from #%d to #%d", mSubtitleStreamIndex, index);
	
	AutoLock autoLock(&mSubtitleLock);

	mMovieFile->streams[mSubtitleStreamIndex]->discard = AVDISCARD_ALL;
	mMovieFile->streams[index]->discard = AVDISCARD_NONE;

	avcodec_close(mSubtitleStream->codec);

	mSubtitleStreamIndex = index;
	mSubtitleStream = mMovieFile->streams[mSubtitleStreamIndex];

	if (!open_subtitle_codec()) {
		LOGE("failed to open subtitle codec");
		return ERROR;
	}

	mSubtitleTrackIndex = index - mSubtitleTrackFirstIndex;
	LOGI("to select sub track #%d", mSubtitleTrackIndex);
	if (!mISubtitle->setSelectedLanguage(mSubtitleTrackIndex))
		LOGE("sub track #%d selected", mSubtitleTrackIndex);

	mISubtitle->seekTo(0); // do flush

	return OK;
}

void FFStream::setISubtitle(ISubtitles* subtitle)
{
    mISubtitle = subtitle;
}

AVFormatContext* FFStream::open(char* uri)
{
    AutoLock autoLock(&mLock);

	if (mStatus != FFSTREAM_INITED) {
		LOGE("wrong state to open: %d", mStatus);
        return NULL;
	}

	// Open steam
	LOGI("ffstream open url: %s", uri);
    mMovieFile = avformat_alloc_context();
    AVIOInterruptCB cb = {interrupt_l, this};
    mMovieFile->interrupt_callback = cb;

	char *filename = NULL;
	if (mSource) {
		mPbBuf	= (unsigned char *)av_mallocz(PB_BUF_SIZE);
		mPb		= avio_alloc_context(mPbBuf, PB_BUF_SIZE, 0, this, ff_read_packet,
			NULL, ff_seek_packet);
		if (!mPb) {
			LOGE("failed to create input pb");
			return NULL;
		}

		mMovieFile->pb = (AVIOContext *)mPb;
		//filename = ""; // fix me!!!
	}
	else {
		filename = uri;
	}
	
	mOpenStreamStartMs = getNowMs();
	if (avformat_open_input(&mMovieFile, filename, NULL, NULL) != 0) {
        LOGE("failed to open url: %s", uri);
        return NULL;
	}
	
	mOpenStreamStartMs = 0;
    LOGI("open url successed");

    bool isM3u8Broadcast = false;
    if (mMovieFile->iformat->name != NULL
        && !strcmp(mMovieFile->iformat->name, "hls,applehttp"))
    {
        if(mMovieFile->duration == AV_NOPTS_VALUE)
        {
            isM3u8Broadcast = true;
			LOGI("m3u8 broadcast is on");
        }
    }

	if (strstr(uri, "appid%3DPPTVIBOBO") != NULL) {
#if defined(_MSC_VER) && !defined(_DEBUG)
		mMovieFile->max_analyze_duration2 = AV_TIME_BASE * 10; // 10 sec for wrong header ts(more than 10 sec)
#else
		mMovieFile->max_analyze_duration = AV_TIME_BASE * 10; // 10 sec for wrong header ts(more than 10 sec)
#endif
	}
	
	// Retrieve stream information after disable variant streams, like m3u8
	if (avformat_find_stream_info(mMovieFile, NULL) < 0) {
        LOGE("failed to avformat_find_stream_info: %s", uri);
        avformat_close_input(&mMovieFile);
        return NULL;
	}

    LOGI("avformat_find_stream_info successed");

    mStreamsCount = mMovieFile->nb_streams;
    LOGD("mStreamsCount:%d", mStreamsCount);
	for (int32_t i = 0; i < (int32_t)mStreamsCount; i++)
    {
		if (mMovieFile->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO)
        {
#ifdef NO_AUDIO_PLAY
			//by default, use the first audio stream, and discard others.
            mMovieFile->streams[i]->discard = AVDISCARD_ALL;
			LOGI("Discard audio stream: #%d", i);
#else
			mAudioStreamIndex = i;
            LOGI("mAudioStreamIndex: %d", mAudioStreamIndex);
            mAudioStream = mMovieFile->streams[mAudioStreamIndex];
			break;
#endif
		}
	}

    //Some audio file includes video stream as album. we need to skip it.
    //Todo: support displaying album picture when playing audio file
    if (mMovieFile->iformat->name != NULL
        && strcmp(mMovieFile->iformat->name, "mp3") != 0
        && strcmp(mMovieFile->iformat->name, "ogg") != 0
        && strcmp(mMovieFile->iformat->name, "wmav1") != 0
        && strcmp(mMovieFile->iformat->name, "wmav2") != 0)
    {
    	for (int32_t j = 0; j < (int32_t)mStreamsCount; j++) {
    		if (mMovieFile->streams[j]->codec->codec_type == AVMEDIA_TYPE_VIDEO)
            {
                if(mVideoStreamIndex == -1)
                {
    			    mVideoStreamIndex = j;
                    LOGI("mVideoStreamIndex:%d", mVideoStreamIndex);
                    mVideoStream = mMovieFile->streams[mVideoStreamIndex];
                }
                else
                {
                    //by default, use the first video stream, and discard others.
                    mMovieFile->streams[j]->discard = AVDISCARD_ALL;
                    LOGI("Discard video stream:%d", j);
                }
    		}
    	}
    }

	// only set mISubtitle will do subtitle parse and decode
	if (mISubtitle) {
		for (int32_t i = 0; i < (int32_t)mStreamsCount; i++) {
			if (mMovieFile->streams[i]->codec->codec_type == AVMEDIA_TYPE_SUBTITLE) {
				AVStream *subtitle_stream = mMovieFile->streams[i];
				AVCodecID codec_id = subtitle_stream->codec->codec_id;
				// only support 5 type subtitle
				if (codec_id == AV_CODEC_ID_ASS || codec_id == AV_CODEC_ID_SSA ||
						codec_id == AV_CODEC_ID_TEXT || codec_id == AV_CODEC_ID_SRT ||
						codec_id == AV_CODEC_ID_SUBRIP) {
					if (mSubtitleStreamIndex == -1) {
    					mSubtitleStreamIndex = i;
						LOGI("mSubtitleStreamIndex: %d", mSubtitleStreamIndex);
						mSubtitleStream = subtitle_stream;

						if (!open_subtitle_codec()) {
							LOGE("failed to open subtitle codec");
						}
					}
					else {
						subtitle_stream->discard = AVDISCARD_ALL;
						LOGI("Discard mSubtitleStreamIndex stream: #%d codec_id %d(%s)", i, codec_id, avcodec_get_name(codec_id));
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
				else {
					subtitle_stream->discard = AVDISCARD_ALL;
					LOGW("Discard unsupported subtitle stream: #%d codec_id %d(%s)", i, codec_id, avcodec_get_name(codec_id));
				}
			}
		}
	}

    if (mAudioStreamIndex == -1 && mVideoStreamIndex == -1)
    {
        LOGE("no audio and video stream!");
        avformat_close_input(&mMovieFile);
        return NULL;
    }

	int64_t duration;
	duration =  mMovieFile->duration;
	if (AV_NOPTS_VALUE == duration || duration < 0) {
		mDurationMs = 0;
		//avformat_close_input(&mMovieFile);
		//return NULL;
	}
	else {
		mDurationMs = duration * 1000 / AV_TIME_BASE;
	}
	LOGI("file duration got: %lld(msec)", mDurationMs);

    mFrameRate = 25;//default
    if (mVideoStream != NULL)
    {
        AVRational fr;
		fr = av_guess_frame_rate(mMovieFile, mVideoStream, NULL);
        if (fr.num > 0 && fr.den > 0) {
            mFrameRate = fr.num / fr.den;
            if(mFrameRate > 100 || mFrameRate <= 0)
				mFrameRate = 25;
		}
    }
    LOGI("media framerate: %d", mFrameRate);
	
	if (mVideoStream) {
		LOGI("video stream time_base %d/%d, codec time_base %d/%d", 
			mVideoStream->time_base.num, mVideoStream->time_base.den,
			mVideoStream->codec->time_base.num, mVideoStream->codec->time_base.den);
	}

	if (mAudioStream) {
		LOGI("audio stream time_base %d/%d, codec time_base %d/%d", 
			mAudioStream->time_base.num, mAudioStream->time_base.den,
			mAudioStream->codec->time_base.num, mAudioStream->codec->time_base.den);

		AVCodecContext *audio_codec = mAudioStream->codec;
		if (CODEC_ID_AAC == audio_codec->codec_id)
			LOGI("aac profile %d", audio_codec->profile);
	}
	
    //check url type
#if defined(__CYGWIN__)
	if(strstr(uri, ":/") != NULL) // cygwin local file is like "e:/folder/1.mov"
#elif defined(_MSC_VER)
	if(strstr(uri, ":\\") != NULL) // win32 local file is like "e:\folder\1.mov"
#else
    if(strncmp(uri, "/", 1) == 0 || strstr(uri, "file://") != NULL)
#endif    
	{
        //do not buffer for local file play(uri is like "/mnt/sdcard/xxx", first character is '/').
        mMinPlayBufferCount = 1;//mFrameRate*FF_PLAYER_MIN_BUFFER_SECONDS_LOCAL_FILE;
        mUrlType = TYPE_LOCAL_FILE;
		mMaxBufferSize = mMaxBufferSize / 4;
        LOGI("It is a local file with mMinPlayBufferCount: %d", mMinPlayBufferCount);
    }
	else if(strstr(uri, "type=pplive"))
	{
		mMinPlayBufferCount = mFrameRate * FF_PLAYER_MIN_BUFFER_SECONDS_VOD;
        mUrlType = TYPE_LIVE;
        LOGI("It is a online live stream with mMinPlayBufferCount:%d", mMinPlayBufferCount);
	}
    else if(mDurationMs == 0 || isM3u8Broadcast || strncmp(uri, "rtmp", 4) == 0) // m3u8 play cannot get duration, will cause "seek" and "flush" cache!
    {
        mUrlType = TYPE_BROADCAST;
        if(strstr(uri, "&realtime=high") != NULL || strstr(uri, "?realtime=high") != NULL || strncmp(uri, "rtmp", 4) == 0)
        {
            mRealtimeLevel = LEVEL_HIGH;
            mMinPlayBufferCount = mFrameRate * FF_PLAYER_MIN_BUFFER_MILISECONDS_BROADCAST_HR/1000;
            mMaxPlayBufferMs = FF_PLAYER_MAX_BUFFER_MILISECONDS_BROADCAST_HR;
        }
        else
        {
            mRealtimeLevel = LEVEL_LOW;
            mMinPlayBufferCount = mFrameRate * FF_PLAYER_MIN_BUFFER_MILISECONDS_BROADCAST_LR/1000;
            mMaxPlayBufferMs = FF_PLAYER_MAX_BUFFER_MILISECONDS_BROADCAST_LR;
        }
        LOGI("It is a broadcast stream with mMinPlayBufferCount:%d, mMaxPlayBufferMs:%lld",
            mMinPlayBufferCount,
            mMaxPlayBufferMs);
    }
    else {
        mMinPlayBufferCount = mFrameRate * FF_PLAYER_MIN_BUFFER_SECONDS_VOD;
        mUrlType = TYPE_ONDEMAND;
        LOGI("It is a online ondemand stream with mMinPlayBufferCount:%d", mMinPlayBufferCount);
    }

	av_dump_format(mMovieFile, 0, uri, 0);

    if (mStatus == FFSTREAM_STOPPED ||
        mStatus == FFSTREAM_STOPPING)
    {
        LOGI("player preparation is interrupted");
        return NULL;
    }

	if (TYPE_LIVE != mUrlType && TYPE_BROADCAST != mUrlType) {
		if (AV_NOPTS_VALUE != mMovieFile->start_time)
			mStartTimeMs = mMovieFile->start_time * 1000 / AV_TIME_BASE;
		else
			mStartTimeMs = 0;
	}
	else {
		mStartTimeMs = 0;
	}

    mStatus	= FFSTREAM_PREPARED;
	
    return mMovieFile;
}

bool FFStream::open_subtitle_codec()
{
	LOGI("subtitle extradata size %d", mSubtitleStream->codec->extradata_size);
		
	AVCodecContext *SubCodecCtx = mSubtitleStream->codec;
	AVCodec* SubCodec = avcodec_find_decoder(SubCodecCtx->codec_id);
	// Open codec
    if (avcodec_open2(SubCodecCtx, SubCodec, NULL) < 0) {
    	LOGE("failed to open subtitle decoder: id %d, name %s", SubCodecCtx->codec_id, avcodec_get_name(SubCodecCtx->codec_id));
		return NULL;
	}

	LOGI("subtitle codec id: %d(%s), codec_name: %s", 
		SubCodecCtx->codec_id, avcodec_get_name(SubCodecCtx->codec_id), SubCodec->long_name);
	
	if (!mAVSubtitle)
		mAVSubtitle = new AVSubtitle;

	LOGI("subtitle codec opened");
	return true;
}

status_t FFStream::start()
{
    AutoLock autoLock(&mLock);
    if(mStatus != FFSTREAM_PREPARED)
        return ERROR;

    pthread_create(&mThread, NULL, demux_thread, this);

    mReachEndStream = false;
    mStatus = FFSTREAM_STARTED;
    return OK;
}

status_t FFStream::interrupt_open()
{
	// 2014.12.16 guoliang.ma set FFSTREAM_STOPPED to fix "blocked" stop when preparing a clip in poor network environment
	if ((mStatus != FFSTREAM_STARTED) && (mStatus != FFSTREAM_PAUSED)) {
		// thread is not running, just exit
		mStatus = FFSTREAM_STOPPED;
	}

	LOGI("interrupt() done!");
    return OK;
}

status_t FFStream::stop()
{
	LOGI("stop()");

	// 2014.12.16 guoliang.ma set FFSTREAM_STOPPED to fix "blocked" stop when preparing a clip in poor network environment
	if (mStatus == FFSTREAM_STOPPED || mStatus == FFSTREAM_STOPPING) {
		return OK;
	}

    mStatus = FFSTREAM_STOPPING;
    //remove pthread_cond_signal(&mCondition);

    //AutoLock autoLock(&mLock); //fixme
	pthread_cond_signal(&mCondition);

	LOGI("stop(): before pthread_join %p", mThread);
    if (pthread_join(mThread, NULL) != 0)
        return ERROR;
	
	LOGI("stop() before flush");
    flush_l();
    mStatus = FFSTREAM_STOPPED;;

	LOGI("stop() done!");
    return OK;
}

status_t FFStream::seek(int64_t seekTimeMs, int incr)
{
    AutoLock autoLock(&mLock);
    mSeekTimeMs			= seekTimeMs + mStartTimeMs;
    mSeeking			= true;
    mReachEndStream		= false;
	mSeekFlag			= incr < 0 ? AVSEEK_FLAG_BACKWARD : 0;
#ifdef _MSC_VER
	LOGI("seekTimeMs %I64d(%I64d sec)(final seek_time %I64d msec), incr %d", seekTimeMs, seekTimeMs / 1000, mSeekTimeMs, incr);
#else
	LOGI("seekTimeMs %lld(%lld sec)(final seek_time %lld msec), incr %d", seekTimeMs, seekTimeMs / 1000, mSeekTimeMs, incr);
#endif
    pthread_cond_signal(&mCondition);
    return OK;
}

status_t FFStream::setListener(MediaPlayerListener* listener)
{
    mListener = listener;
    return OK;
}

status_t FFStream::getPacket(int32_t streamIndex, AVPacket** packet)
{
    AutoLock autoLock(&mLock);
    if (mSeeking) {
		LOGD("queue seeking");
        return FFSTREAM_ERROR_BUFFERING;
    }

    if (streamIndex == mAudioStreamIndex) {
        if (mIsBuffering) {
            if(!mReachEndStream)
                return FFSTREAM_ERROR_BUFFERING;
            
			mIsBuffering = false;
            LOGI("MEDIA_INFO_BUFFERING_END because of stream end(audio)");
            notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_END);
        }

        AVPacket* pPacket = mAudioQueue.get();
        if (pPacket != NULL) {
            if (pPacket->data && !strcmp((char*)pPacket->data, "FLUSH_AUDIO")) {
                *packet = pPacket;
                return FFSTREAM_ERROR_FLUSHING;
            }
            else {
                mBufferSize-=pPacket->size;
                *packet = pPacket;
                return FFSTREAM_OK;
            }
        }
		else {
		    if (mReachEndStream) {
				// support looping
				if (mLooping) {
					LOGI("looping is on, seek to begin of the clip(audio eof)");
					mSeekTimeMs			= mStartTimeMs;
					mSeeking			= true;
					mReachEndStream		= false;
					mSeekFlag			= AVSEEK_FLAG_BACKWARD;
					return FFSTREAM_ERROR_BUFFERING;
				}

				LOGI("audio queue is empty(eof)");
                return FFSTREAM_ERROR_EOF;
            }
            else { // queue is empty but not EOF
		        LOGD("audio queue empty");
                if (!mIsBuffering) {
                    mIsBuffering = true;
					int64_t offset = (mMovieFile->pb ? avio_tell(mMovieFile->pb) : 0);
#ifdef _MSC_VER	
					LOGI("audio MEDIA_INFO_BUFFERING_START, offset %I64d", offset);
#else
					LOGI("audio MEDIA_INFO_BUFFERING_START, offset %lld", offset);
#endif
                    notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_START);
                }

                return FFSTREAM_ERROR_BUFFERING;
            }
		}
    }
    else if (streamIndex == mVideoStreamIndex)
    {
        AVPacket* pPacket = mVideoQueue.get();
        if(pPacket != NULL) {
            if (pPacket->data && !strcmp((char*)pPacket->data, "FLUSH_VIDEO"))
            {
                *packet = pPacket;
                return FFSTREAM_ERROR_FLUSHING;
            }
            else
            {
                mBufferSize-=pPacket->size;
                *packet = pPacket;
                return FFSTREAM_OK;
            }
        }
		else {
		    if (mReachEndStream) {
				// support looping
				if (mLooping) {
					LOGI("looping is on, seek to begin of the clip(video eof)");
					mSeekTimeMs			= mStartTimeMs;
					mSeeking			= true;
					mReachEndStream		= false;
					mSeekFlag			= AVSEEK_FLAG_BACKWARD;
					return FFSTREAM_ERROR_BUFFERING;
				}

				LOGI("video queue is empty(eof)");
                return FFSTREAM_ERROR_EOF;
            }
            else { // queue is empty but not EOF
		        LOGD("video queue empty");
                if(!mIsBuffering) {
                    mIsBuffering = true;
					int64_t offset = (mMovieFile->pb ? avio_tell(mMovieFile->pb) : 0);
#ifdef _MSC_VER	
					LOGI("video MEDIA_INFO_BUFFERING_START, offset %I64d", offset);
#else
					LOGI("video MEDIA_INFO_BUFFERING_START, offset %lld", offset);
#endif
                    notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_START);
                }
                return FFSTREAM_ERROR_BUFFERING;
            }
		}
    }
    else {
        LOGD("Unknown stream index: %d", streamIndex);
        return FFSTERAM_ERROR_SWITCH_AUDIO;//FFSTREAM_ERROR_STREAMINDEX;
    }
}

void FFStream::thread_impl()
{
	// initial state is "buffering..."
#ifdef TEST_PERFORMANCE_BITRATE 
	int64_t start_msec;
	int64_t curr_msec;
	int64_t last_sum;

	if (mMovieFile->pb) // rtsp pb is null?
		last_sum = mMovieFile->pb->bytes_read;
	else
		last_sum = 0;

	start_msec = getNowMs();
#endif

	mIsBuffering = true;
	m_real_bit_rate = 0;

    LOGI("FFStrem start to demux media");
    notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_START);

    while (1) {
        if (mStatus == FFSTREAM_STOPPED || mStatus == FFSTREAM_STOPPING) {
            LOGI("FFStrem is stopping");
            break;
        }
        
		if (mStatus == FFSTREAM_PAUSED || mReachEndStream) {
			// loop when "pause"
			// wait for exit when "eof"
            struct timespec ts;
            ts.tv_sec = 0;
            ts.tv_nsec = 10000000; // 10 msec
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
            continue;
        }
        else {
            if (mSeeking) {
                AutoLock autoLock(&mLock);
                LOGI("before seek to :%lld(ms)", mSeekTimeMs);
                //if(avformat_seek_file(mMovieFile,
                //                    -1,
                //                    INT64_MIN,
                //                    (mSeekTimeMs/1000)*AV_TIME_BASE, //in AV_TIME_BASE
                //                    INT64_MAX,
                //                    0) < 0)
				int stream_index= -1;
				int64_t seek_target = mSeekTimeMs * AV_TIME_BASE / 1000;

				if (mVideoStreamIndex >= 0)
					stream_index = mVideoStreamIndex;
				else if (mAudioStreamIndex >= 0)
					stream_index = mAudioStreamIndex;

				if (stream_index < 0) {
					LOGW("no stream to seek");
					continue;
				}
#ifdef _MSC_VER
				AVRational ra;
				ra.num = 1;
				ra.den = AV_TIME_BASE;
				seek_target= av_rescale_q(seek_target, ra, mMovieFile->streams[stream_index]->time_base);
#else
				seek_target= av_rescale_q(seek_target, AV_TIME_BASE_Q, mMovieFile->streams[stream_index]->time_base);
#endif
                if (av_seek_frame(mMovieFile, stream_index, seek_target, mSeekFlag) < 0) {
#ifdef _MSC_VER
					LOGE("failed to seek to: %I64d(ms)", mSeekTimeMs);
#else
                    LOGE("failed to seek to: %lld(ms)", mSeekTimeMs);
#endif
                    mSeeking = false;
					notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_FAIL_TO_SEEK, (int)mSeekTimeMs);
                    break; 
                }
				
                LOGI("after seek to :%lld(ms)", mSeekTimeMs);

                flush_l(); // seek op will clean all video and audio packet, which cause buffering pct to 0%
                mGopDuration = 0;
                mGopStart = 0;
                mGopEnd = 0;
#ifdef TEST_PERFORMANCE_BITRATE
				if (mMovieFile->pb)
					last_sum = mMovieFile->pb->bytes_read;
				else
					last_sum = 0;
				start_msec = getNowMs();
				for(int i=0;i<MAX_CALC_SEC;i++)
					m_read_bytes[i] = 0;
#endif

                if (mAudioStream != NULL) {
                    AVPacket* flushAudioPkt = (AVPacket*)av_malloc(sizeof(AVPacket));
                    av_init_packet(flushAudioPkt);
                    flushAudioPkt->size = 0;
                    flushAudioPkt->data = (uint8_t*)"FLUSH_AUDIO";
                    mAudioQueue.put(flushAudioPkt);
                }

                if (mVideoStream != NULL) {
                    AVPacket* flushVideoPkt = (AVPacket*)av_malloc(sizeof(AVPacket));
                    av_init_packet(flushVideoPkt);
                    flushVideoPkt->size = 0;
                    flushVideoPkt->data = (uint8_t*)"FLUSH_VIDEO";
                    mVideoQueue.put(flushVideoPkt);
                }

				mCachedDurationMs = mSeekTimeMs;
                mSeeking = false;
                LOGI("send event MEDIA_SEEK_COMPLETE");
                notifyListener_l(MEDIA_SEEK_COMPLETE);
                continue;
            } // end of isSeeking

			// video queue count == frame
			// but audio queue count NOT
            if (mIsBuffering) {
				bool video_enough = ((mVideoStream != NULL) ? (mVideoQueue.count() > mMinPlayBufferCount) : true);
				bool audio_enough = false;
				if (mAudioStream == NULL)
					audio_enough = true;
				else {
					double min_duration = mMinPlayBufferCount * 1000 / (double)mFrameRate;
					double a_duration = mAudioQueue.duration() * 1000 * av_q2d(mAudioStream->time_base);
					if (a_duration >= min_duration)
						audio_enough = true;
				}
				
				if ( video_enough && audio_enough) {
					// packet queue is enough for play
					mIsBuffering = false;
					int64_t offset = (mMovieFile->pb ? avio_tell(mMovieFile->pb) : 0);
#ifdef _MSC_VER	
					LOGI("MEDIA_INFO_BUFFERING_END, offset %I64d", offset);
#else
					LOGI("MEDIA_INFO_BUFFERING_END, offset %lld", offset);
#endif
					notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_END);
				}
			}

            LOGD("mBufferSize:%d", mBufferSize);
            if (mBufferSize > mMaxBufferSize)
            {
                LOGD("Buffering reaches max size %d %d, vQueueSize %d, aQueueSize %d", 
					mBufferSize, mMaxBufferSize, mVideoQueue.count(), mAudioQueue.count());
				
				if (mIsBuffering) {
                    mMaxBufferSize *= 2;
                    LOGI("Double max buffer size to: %d", mMaxBufferSize);
                }
                else {
					// too much data to decode, just wait for decoder consuming some data
					while (mBufferSize > mMaxBufferSize ) {
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
						if (FFSTREAM_STOPPING == mStatus || FFSTREAM_STOPPED == mStatus)
							break;
						if (mSeeking)
							break;
						// 2015.1.8 guoliangma added to avoid no video frame blocking
						if (mIsBuffering)
							break;
					}
                }
                continue;
            }

            LOGD("before av_read_frame()");

			mReadStreamStartMs = getNowMs();
            AVPacket* pPacket = (AVPacket*)av_malloc(sizeof(AVPacket));
            memset(pPacket, 0, sizeof(AVPacket));
            int ret = av_read_frame(mMovieFile, pPacket);

            LOGD("after av_read_frame(), ret: %d", ret);

            // remove is good????? AutoLock autoLock(&mLock);
            if (mStatus == FFSTREAM_STOPPED || mStatus == FFSTREAM_STOPPING) {
                //this is to fix ffmpeg issue that av_read_frame does not return AVERROR_EXIT 
				//after stop by application as expected.
                av_free_packet(pPacket);
                av_free(pPacket);
                LOGI("mStatus is FFSTREAM_STOPPED or FFSTREAM_STOPPING, break main loop");
                break; // exit loop here!!!
            }

            if (mSeeking) {
                av_free_packet(pPacket);
                av_free(pPacket);
                LOGI("Seek during reading frame");
                continue;
            }

            if (ret == AVERROR_EOF) {
                av_free_packet(pPacket);
                av_free(pPacket);

                //end of stream
				if (!mReachEndStream) {
					mReachEndStream = true;
					LOGI("reach end of stream");
				}
				
				// 2014.8.25 guoliangma added, to fix cannot play clip which duration is less than 3sec
				if (mIsBuffering) {
					mIsBuffering = false;
					LOGI("MEDIA_INFO_BUFFERING_END because of stream end");
					notifyListener_l(MEDIA_INFO, MEDIA_INFO_BUFFERING_END);
				}

				// continue for seek back
				av_usleep(10 * 1000); // 10 msec
                continue;
            }
            else if(ret == AVERROR_EXIT) {
                //May never come here
                //abort read
                mReachEndStream = true;
                av_free_packet(pPacket);
                av_free(pPacket);
                continue;
            }
            else if(ret == AVERROR(EIO) || ret == AVERROR(ENOMEM)) {
                LOGE("av_read_frame() error: %d", ret);
                mReachEndStream = true;
                if (pPacket->data)
                    av_free_packet(pPacket);
                av_free(pPacket);
                continue;
            }
            else if(ret == AVERROR_INVALIDDATA ||
                    pPacket->size <= 0 ||
                    pPacket->size > (int)mMaxBufferSize ||
                    pPacket->data == NULL)
            {
                LOGE("read invalid packet, ret: %d", ret);

                if(pPacket->data)
                    av_free_packet(pPacket);
                av_free(pPacket);

                struct timespec ts;
                ts.tv_sec = 0;
                ts.tv_nsec = 10000000; // 10 msec
				AutoLock autoLock(&mLock);
#if defined(__CYGWIN__) || defined(_MSC_VER) || defined(__aarch64__)
				int64_t now_usec = getNowUs();
				int64_t now_sec = now_usec / 1000000;
				now_usec = now_usec - now_sec * 1000000;
				ts.tv_sec	+= now_sec;
                ts.tv_nsec	+= (long)(now_usec * 1000);
				pthread_cond_timedwait(&mCondition, &mLock, &ts);
#else		
				pthread_cond_timedwait_relative_np(&mCondition, &mLock, &ts);
#endif
                continue;
            }
            else if(ret < 0) {
                //error
                mReachEndStream = true;
                av_free_packet(pPacket);
                av_free(pPacket);
                LOGE("read frame error: %d, break main loop", ret);
                break;
            }

#ifdef _MSC_VER
			// guoliang.ma 2014.12.18 comment out to fix 第一财经 live seek invalid pts
			// process wrong wrap_pts in live mode
			if (TYPE_LIVE == mUrlType) {
				AVStream *st;
				st = mMovieFile->streams[pPacket->stream_index];
				if (st->pts_wrap_behavior == AV_PTS_WRAP_ADD_OFFSET &&
					pPacket->pts > (int64_t)(1ULL << st->pts_wrap_bits))
						pPacket->pts -= (1ULL << st->pts_wrap_bits);
				else if (st->pts_wrap_behavior == AV_PTS_WRAP_SUB_OFFSET &&
					pPacket->pts < (int64_t)(1ULL << st->pts_wrap_bits))
						pPacket->pts += (1ULL << st->pts_wrap_bits);
			}
#endif

			// process packet
        	if (pPacket->stream_index == mAudioStreamIndex) {
                // audio data
#ifndef NDEBUG
                int64_t pts = (int64_t)(pPacket->pts*1000*av_q2d(mAudioStream->time_base));
                int64_t dts = (int64_t)(pPacket->dts*1000*av_q2d(mAudioStream->time_base));
            	LOGD("audio Packet->pts:%lld ms", pts);
            	LOGD("audio Packet->dts:%lld ms", dts);
#endif
                int64_t duration = (int64_t)(mAudioQueue.duration() * 1000 * av_q2d(mAudioStream->time_base));
                LOGD("audio duration: %lld", duration);
                if (!mDelaying || duration < mMaxPlayBufferMs) {
                    mAudioQueue.put(pPacket);
                    mBufferSize += pPacket->size;
					LOGD("audio_queue: count %d, size %d", mAudioQueue.count(), mAudioQueue.size());

                    //update cached duration
                    int64_t packetPTS = 0;
                    if (pPacket->pts == AV_NOPTS_VALUE)
                        packetPTS = pPacket->dts;
                    else
                        packetPTS = pPacket->pts;

                    if (packetPTS == AV_NOPTS_VALUE) // aggregate value
                        mCachedDurationMs += (int64_t)((double)pPacket->duration * 1000 * av_q2d(mAudioStream->time_base));
                    else // absolute value
						mCachedDurationMs = (int64_t)(packetPTS * 1000 * av_q2d(mAudioStream->time_base));
                    LOGD("mCachedDurationMs: %lld", mCachedDurationMs);
                }
                else {
                    av_free_packet(pPacket);
                    av_free(pPacket);
                    LOGI("[liveplatform]:drop delayed audio packet");
                }
			}
			else if (pPacket->stream_index == mVideoStreamIndex) {
				//video data

#ifdef TEST_PERFORMANCE_BITRATE		
				curr_msec = getNowMs();
				if (curr_msec - start_msec >= 1000)
				{
					/* calc IO bitrate(kbps)	*/
					int64_t curr_sum = 0;
					if (mMovieFile->pb)
						curr_sum = mMovieFile->pb->bytes_read;

					int64_t readed = curr_sum - last_sum;
					m_io_bit_rate = (int)(readed / (curr_msec - start_msec)) * 8;

					start_msec = curr_msec;
					last_sum = curr_sum;

					notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_IO_BITRATE, m_io_bit_rate);

					/* calc media bitrate(kbps)	*/
					if (++m_vb_index == MAX_CALC_SEC)
						m_vb_index = 0;

					/* calc bit/second. */
					do
					{
						int sum = 0;
						int i = 0;
						for (; i < MAX_CALC_SEC; i++)
							sum += m_read_bytes[i];
						m_real_bit_rate = (int)(((double)sum / (double)MAX_CALC_SEC) * 8.0f / 1024.0f);
						notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_MEDIA_BITRATE, m_real_bit_rate);
					} while (0);
					/* clear. */
					m_read_bytes[m_vb_index] = 0;
				}

				/* update read bytes. */
				m_read_bytes[m_vb_index] += pPacket->size;
				m_total_read_bytes		 += pPacket->size;
#endif

#ifndef NDEBUG
				int64_t pts = (int64_t)(pPacket->pts*1000*av_q2d(mVideoStream->time_base));
                int64_t dts = (int64_t)(pPacket->dts*1000*av_q2d(mVideoStream->time_base));
            	LOGD("video Packet->pts:%lld ms, flags:%d", pts, pPacket->flags);
            	LOGD("video Packet->dts:%lld ms", dts);
                LOGD("[liveplatform]:receive pts:%lld, cached:%d", pts, mVideoQueue.count());
#endif
                if (mGopStart == 0 || mGopEnd == 0) {
					// need calculate new gop duration
					if (mGopStart == 0 &&
						(pPacket->flags & AV_PKT_FLAG_KEY) &&
						pPacket->pts > 0)
					{
						// only note down 1st valid I frame' pts
						mGopStart = pPacket->pts;
					}

                    if (mGopEnd == 0 &&
                        mGopStart != 0 &&
                        mGopStart != pPacket->pts && // not 0 duration
                        (pPacket->flags & AV_PKT_FLAG_KEY) &&
                        pPacket->pts > 0)
                    {
						// only note down 2nd valid I frame's pts(when mGopStart has been got)
                        mGopEnd = pPacket->pts;
                    }

                    if (mGopStart && mGopEnd) {
						// ahha! we got duration now!
                        mGopDuration = (int64_t)((mGopEnd - mGopStart) * 1000 * av_q2d(mVideoStream->time_base));
                        if (mGopDuration < FF_PLAYER_MAX_BUFFER_MILISECONDS_BROADCAST_HR || mGopDuration > FF_PLAYER_INVALID_GOP_DURATION) {
#ifdef _MSC_VER
							LOGW("GOP duration is invalid: %I64d msec", mGopDuration);
#else
							LOGW("GOP duration is invalid: %lld msec", mGopDuration);
#endif	
							mGopDuration = FF_PLAYER_DEFAULT_GOP_DURATION;
						}
                        LOGI("got GOP duration: %lld(msec)", mGopDuration);

                        if (mUrlType == TYPE_BROADCAST && mRealtimeLevel == LEVEL_HIGH) {
                            mMinPlayBufferCount = 0;
                            mMaxPlayBufferMs = mGopDuration / 2;
                            LOGI("Update broadcast stream with mMinPlayBufferCount:%d, mMaxPlayBufferMs:%lld",
                                mMinPlayBufferCount, mMaxPlayBufferMs);
						}
					}
				}

				int64_t duration = (int64_t)(mVideoQueue.duration()*1000*av_q2d(mVideoStream->time_base));
				LOGD("video duration:%lld", duration);
				if ((mUrlType == TYPE_BROADCAST) &&
					(mRealtimeLevel == LEVEL_HIGH) &&
					(duration > mMaxPlayBufferMs))
				{
					mDelaying = true;
					LOGD("[liveplatform]:switch to delay state");
					if(mRefreshed &&
						mMaxPlayBufferMs < mGopDuration * 3 &&
						mRealtimeLevel == LEVEL_HIGH)
					{
						mMaxPlayBufferMs+=200;
						LOGD("[liveplatform] increase mMaxPlayBufferMs to: %lld", mMaxPlayBufferMs);
					}
				}

				if (mDelaying &&
					(pPacket->flags & AV_PKT_FLAG_KEY) &&
                    (duration <= mMaxPlayBufferMs))
                {
                    mDelaying = false;
                    LOGD("[liveplatform]:switch to normal state");
                }

				if (mDelaying) {
                    av_free_packet(pPacket);
                    av_free(pPacket);
                    LOGD("[liveplatform]:drop delayed video packet");
                }
				else {
                    mVideoQueue.put(pPacket);
                    mBufferSize += pPacket->size;

					LOGD("video_queue: count %d, size %d", mVideoQueue.count(), mVideoQueue.size());
                }
            }
			else if (pPacket->stream_index == mSubtitleStreamIndex)
			{
				AVPacket *orig_pkt = pPacket;

				if (mSubtitleStream) {
					int got_sub;
					int ret;

					AutoLock autoLock(&mSubtitleLock);
					
					do {
						ret = avcodec_decode_subtitle2(mSubtitleStream->codec, mAVSubtitle, &got_sub, pPacket);
						if (ret < 0) {
							LOGW("failed to decode subtitle");
							break;
						}

						if (got_sub) {
							LOGI("got subtitle format: %d, type: %d, content: %s", 
								mAVSubtitle->format, (*(mAVSubtitle->rects))->type, (*(mAVSubtitle->rects))->ass);
							int64_t start_time ,stop_time;
#ifdef _MSC_VER
							AVRational ra;
							ra.num = 1;
							ra.den = AV_TIME_BASE;
							start_time = av_rescale_q(mAVSubtitle->pts + mAVSubtitle->start_display_time * 1000,
										ra, mSubtitleStream->time_base);
							stop_time = av_rescale_q(mAVSubtitle->pts + mAVSubtitle->end_display_time * 1000,
										ra, mSubtitleStream->time_base);
#else
							start_time = av_rescale_q(mAVSubtitle->pts + mAVSubtitle->start_display_time * 1000,
										AV_TIME_BASE_Q, mSubtitleStream->time_base);
							stop_time = av_rescale_q(mAVSubtitle->pts + mAVSubtitle->end_display_time * 1000,
										AV_TIME_BASE_Q, mSubtitleStream->time_base);
#endif
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
				}

				av_free_packet(orig_pkt);
                av_free(orig_pkt);
			}
            else {
                // Free the packet that was allocated by av_read_frame
                av_free_packet(pPacket);
                av_free(pPacket);
                //LOGD("Unknown stream type:%d", pPacket->stream_index);
            }
        }
    }
}

status_t FFStream::flush_l()
{
    mAudioQueue.flush();
    mVideoQueue.flush();
    mBufferSize = 0;
    return OK;
}

void* FFStream::demux_thread(void* ptr)
{
	LOGI("demux_thread thread started");
    FFStream* stream = (FFStream*)ptr;

	// 2015.6.17 guoliangma added to fix seek onError crash bug
#ifdef __ANDROID__
	JNIEnv *env = NULL;
    gs_jvm->AttachCurrentThread(&env, NULL);
#endif

    stream->thread_impl();

#ifdef __ANDROID__
    gs_jvm->DetachCurrentThread();
#endif

	LOGI("demux_thread thread exited");
    return NULL;
}

int FFStream::interrupt_l(void* ctx)
{
    //LOGD("Checking interrupt_l");
	//return 1: error
    
	FFStream* stream = (FFStream*)ctx;
    if (stream == NULL)
		return 1;

	if (stream->mOpenStreamStartMs != 0) {
		if (getNowMs() - stream->mOpenStreamStartMs > MEDIA_OPEN_TIMEOUT_MSEC) {
			LOGE("interrupt_l: open stream time out");
			stream->notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_TIMED_OUT, 0);
			return 1;
		}
	}

	if (stream->status() == FFSTREAM_STARTED) {
		if (getNowMs() - stream->mReadStreamStartMs > MEDIA_READ_TIMEOUT_MSEC) {
			LOGE("interrupt_l: read stream time out");
			stream->notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_TIMED_OUT, 0);
			return 1;
		}
	}

    if (stream->status() == FFSTREAM_STOPPED ||
        stream->status() == FFSTREAM_STOPPING)
    {
        //abort av_read_frame or avformat_open_input, avformat_find_stream_info
        LOGI("interrupt_l: FFSTREAM_STOPPED");
        return 1;
    }
    else if(stream->isSeeking())
    {
		// now use av_seek_frame
        //as avformat_seek_file also comes here,
        // so do not abort.
        //LOGD("interrupt_l: isSeeking");
        //return 0;//1;
    }
	
	return 0;
}

void FFStream::notifyListener_l(int msg, int ext1, int ext2)
{
	if (mListener == NULL)
		LOGE("mListener is null");
	else
		mListener->notify(msg, ext1, ext2);
}

status_t FFStream::getBufferingTime(int *msec)
{
    if (mBufferSize <= 0) {
		LOGI("Buffer is zero");
        *msec = 0;
    }
    else {
		if (mVideoStream == NULL) {
			if (mAudioStream == NULL)
				*msec = 0;
			else {
				uint32_t cacheMs = (uint32_t)(mAudioQueue.duration() * 1000 * av_q2d(mAudioStream->time_base));
				*msec = cacheMs;
			}
		}
		else {
			uint32_t cacheMs = mVideoQueue.count() * 1000 / mFrameRate;
			*msec = cacheMs;
		}
    }

    //LOGI("buffering time: %d[ms]", *msec);
    return OK;
}

int64_t FFStream::getDurationMs()
{
	if (TYPE_LIVE == mUrlType) {
		// update hls duration here
		int64_t new_duration;
		new_duration =  mMovieFile->duration;
	
		mDurationMs = new_duration * 1000 / AV_TIME_BASE;
		LOGD("getDurationMs: update duration %lld", mDurationMs);
	}

	return mDurationMs;
}

status_t FFStream::refresh()
{
    AutoLock autoLock(&mLock);
    LOGD("refresh buffering");
    mDelaying = false;
    mRefreshed = true;
    return flush_l();
}

status_t FFStream::disableStream(int32_t streamIndex)
{
    if(streamIndex == -1)
        return OK;
    if(mAudioStreamIndex == streamIndex)
    {
        mAudioStreamIndex = -1;
        mAudioStream = NULL;
    }
    else if(mVideoStreamIndex == streamIndex)
    {
        mVideoStreamIndex = -1;
        mVideoStream = NULL;
    }

    return OK;
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


