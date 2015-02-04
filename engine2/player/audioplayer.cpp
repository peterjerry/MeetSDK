/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */
#include "audioplayer.h"

#ifndef _MSC_VER
#include <sys/resource.h>
#include <sys/time.h>
#endif
#include <sched.h> // in pthread

#define LOG_TAG "AudioPlayer"
#include "log.h"
#include "ppffmpeg.h"
#include "player.h"
#include "utils.h"
#include "autolock.h"
#include "audiorender.h"
#include "audiotrack.h"

AudioPlayer::AudioPlayer(FFStream* dataStream, AVStream* context, int32_t streamIndex)
{
    mDataStream = dataStream;
    if (dataStream)
        mDurationMs = dataStream->getDurationMs();
    else
        mDurationMs = 0;

    mOutputBufferingStartMs = 0;
    mAvePacketDurationMs = 0;
    mLastPacketMs = 0;
    mAudioContext = context;
    mAudioStreamIndex = streamIndex;
    mSamplesSize = AVCODEC_MAX_AUDIO_FRAME_SIZE * 2;
    mSamples = (int16_t*)av_malloc(mSamplesSize);
	mAudioFrame = av_frame_alloc();
    mListener = NULL;
    mRender = NULL;
    mReachEndStream = false;
    
    pthread_mutex_init(&mLock, NULL);
    pthread_cond_init(&mCondition, NULL);

    mSeeking = false;
	mPlayerStatus = MEDIA_PLAYER_INITIALIZED;
}

AudioPlayer::~AudioPlayer()
{
	LOGI("AudioPlayer destructor()");
    
	if (stop()!= OK)
		LOGW("failed to stop AudioPlayer");

    if (mSamples != NULL) {
        // Free audio samples buffer
        av_free(mSamples);
        mSamples = NULL;
    }

    if (mAudioFrame != NULL)
        av_frame_free(&mAudioFrame);
        
    pthread_mutex_destroy(&mLock);
    pthread_cond_destroy(&mCondition);
	LOGI("AudioPlayer destructor() done.");
}

status_t AudioPlayer::prepare()
{
	status_t res;

	if (mPlayerStatus == MEDIA_PLAYER_PREPARED)
		return OK;
	if (mPlayerStatus != MEDIA_PLAYER_INITIALIZED)
		return INVALID_OPERATION;

	if (mAudioContext == NULL || mDataStream == NULL) {
		LOGE("audio stream is null");
		return ERROR;
	}

	AVCodecContext *CodecCtx = mAudioContext->codec;
	LOGI("channel layout:%lld, sample rate:%d, sample format:%d, channels:%d", 
		CodecCtx->channel_layout, 
		CodecCtx->sample_rate, CodecCtx->sample_fmt, CodecCtx->channels);

	mRender = new AudioRender();
	uint64_t channelLayout = AV_CH_LAYOUT_MONO;// default layout

	// 2015.1.19 guoliangma mark(it's very important)
	// fix channel_layout param is not accurate for some video.
	switch(CodecCtx->channels) {
	case 1:
		channelLayout = AV_CH_LAYOUT_MONO;
		break;
	case 2:
		channelLayout = AV_CH_LAYOUT_STEREO;
		break;
	case 3:
		channelLayout = AV_CH_LAYOUT_2POINT1;
		break;
	case 4:
		channelLayout = AV_CH_LAYOUT_3POINT1;
		break;
	case 5:
		channelLayout = AV_CH_LAYOUT_4POINT1;
		break;
	case 6:
		channelLayout = AV_CH_LAYOUT_5POINT1;
		break;
	case 7:
		channelLayout = AV_CH_LAYOUT_6POINT1;
		break;
	case 8:
		channelLayout = AV_CH_LAYOUT_7POINT1;
		break;
	default:
		channelLayout = AV_CH_LAYOUT_MONO;
		break;
	}

	res = mRender->open(mAudioContext->codec->sample_rate,
		channelLayout,
		mAudioContext->codec->channels,
		mAudioContext->codec->sample_fmt);
	if (res != OK) {
		LOGE("failed to open audio render");
		return ERROR;
	}

	mNumFramesPlayed = 0;
	mAudioPlayingTimeMs = 0;
	mAvePacketDurationMs = 0;
	mLatencyMs = mRender->get_latency();
	LOGI("mLatencyMs: %d", mLatencyMs);

	mPlayerStatus = MEDIA_PLAYER_PREPARED;
	return OK;
}

status_t AudioPlayer::start()
{
    if(mPlayerStatus == MEDIA_PLAYER_STARTED)
        return OK;
    if(mPlayerStatus != MEDIA_PLAYER_PREPARED &&
        mPlayerStatus != MEDIA_PLAYER_PAUSED)
        return INVALID_OPERATION;
    
    return start_l();
}

status_t AudioPlayer::pause()
{
    if(mPlayerStatus == MEDIA_PLAYER_PAUSED)
        return OK;
    if(mPlayerStatus != MEDIA_PLAYER_STARTED)
        return INVALID_OPERATION;
    
    return pause_l();
}

status_t AudioPlayer::flush()
{
    return flush_l();
}

int64_t AudioPlayer::getMediaTimeMs()
{
    if(mPlayerStatus != MEDIA_PLAYER_STARTED &&
        mPlayerStatus != MEDIA_PLAYER_PAUSED &&
        mPlayerStatus != MEDIA_PLAYER_PLAYBACK_COMPLETE)
        return 0;

	int64_t audioNowMs = mAudioPlayingTimeMs - mRender->get_latency(); // |-------pts#######latency#########play->audio_hardware
	if (audioNowMs < 0)
		audioNowMs = 0;
    return audioNowMs;
}

status_t AudioPlayer::seekTo(int64_t msec)
{
	mSeekTimeMs = msec;
	mSeeking	= true;
	if (mRender)
		mRender->flush();

	LOGI("audio seeking...");
    return OK;
}

status_t AudioPlayer::start_l()
{      
	if (mPlayerStatus == MEDIA_PLAYER_PREPARED) {
        pthread_create(&mThread, NULL, audio_thread, this);
    }
    else if(mPlayerStatus == MEDIA_PLAYER_PAUSED) {
        if (mRender->resume() != OK) {
            LOGE("AudioTrack_resume failed");
        	return ERROR;
        }
    }
    
    mPlayerStatus = MEDIA_PLAYER_STARTED;
    return OK;
}

status_t AudioPlayer::stop()
{
	LOGI("stop_l()");

	// avoid duplicated stop()
    if (mPlayerStatus == MEDIA_PLAYER_STOPPED)
        return OK;

	pthread_cond_signal(&mCondition);

	if (mPlayerStatus == MEDIA_PLAYER_STARTED || mPlayerStatus == MEDIA_PLAYER_PAUSED) {
		mPlayerStatus = MEDIA_PLAYER_STOPPING; // notify audio thread to exit
		LOGI("before pthread_join %p", mThread);
		if (pthread_join(mThread, NULL) != 0)
			LOGE("failed to join audioplayer thread");

		LOGI("after join");
	}

    if (mRender != NULL) {
		mRender->flush();
		mRender->close();
		LOGI("after audio render closed");
            
		delete mRender;
        mRender = NULL;
		LOGI("after audio render released");
    }

	mPlayerStatus = MEDIA_PLAYER_STOPPED;
    LOGI("after stop_l()");
    return OK;
}

status_t AudioPlayer::pause_l()
{
    if (mRender->pause() != OK)
        return ERROR;
    mPlayerStatus = MEDIA_PLAYER_PAUSED;
    pthread_cond_signal(&mCondition);

    return OK;
}

status_t AudioPlayer::flush_l()
{
    if (mRender)
		return mRender->flush();

    return OK;
}

int AudioPlayer::decode_l(AVPacket *packet)
{
    int got_frame = 0;

	av_frame_unref(mAudioFrame);

	int ret;
	while(1) {
		ret = avcodec_decode_audio4(mAudioContext->codec, mAudioFrame, &got_frame, packet);
		if (ret < 0) {
			LOGE("decode audio failed, ret:%d", ret);
			return -1;
		}

		packet->data += ret;
        packet->size -= ret;

		/* in-complete frame and have data in packet still, continue to decode packet. */
		if (!got_frame && packet->size > 0)
			continue;

		/* packet has no more data ,and is in-complete frame discard whole audio packet. */
		if (packet->size == 0 && !got_frame)
			break;

		if (mAudioFrame->linesize[0] != 0) { //got audio frame
			LOGD("decode audio samples: %d", mAudioFrame->nb_samples);
			if (mRender != NULL) {
				status_t stat = mRender->render(mAudioFrame);
				if (stat != OK) {
					LOGE("Audio render failed");
					return -1;
				}
			}
		}

		/* packet has no more data, decode next packet. */
		if (packet->size <= 0)
			break;

		if (mSeeking) {
			mAudioPlayingTimeMs = mSeekTimeMs;
			break;
		}
	}

    return 0;
}

void AudioPlayer::run()
{
    while(1)
    {
        if (mPlayerStatus == MEDIA_PLAYER_STOPPED ||
            mPlayerStatus == MEDIA_PLAYER_STOPPING)
        {
            LOGD("AudioPlayer is stopping");
            break;
        }

        if (mPlayerStatus == MEDIA_PLAYER_PAUSED)
        {
            LOGD("AudioPlayer is paused");
            AutoLock autoLock(&mLock);
			struct timespec ts;
            ts.tv_sec = 0;
            ts.tv_nsec = 10000000;//10 msec
#if defined(__CYGWIN__) || defined(_MSC_VER)
			int64_t now_usec = getNowUs();
			int64_t now_sec = now_usec / 1000000;
			now_usec	= now_usec - now_sec * 1000000;
			ts.tv_sec	+= now_sec;
            ts.tv_nsec	+= (long)now_usec * 1000;
            pthread_cond_timedwait(&mCondition, &mLock, &ts);
#else
            pthread_cond_timedwait_relative_np(&mCondition, &mLock, &ts);
#endif
            continue;
        }
        else
        {
            AutoLock autoLock(&mLock);
            AVPacket* pPacket = NULL;
            status_t ret = mDataStream->getPacket(mAudioStreamIndex, &pPacket);
            if (ret == FFSTREAM_OK) {
                if (!mSeeking) {
        	        int ret = decode_l(pPacket);

                    if (ret == 0) {
                        //update audio output buffering start time
        	            mOutputBufferingStartMs = getNowMs();
                        
                        mAvePacketDurationMs = (mAvePacketDurationMs * 4 + (int64_t)(pPacket->duration * 1000 * av_q2d(mAudioContext->time_base))) / 5;
                       	mAudioPlayingTimeMs = (int64_t)(pPacket->pts * av_q2d(mAudioContext->time_base) * 1000);
#ifdef _MSC_VER
						LOGD("mAudioPlayingTimeMs: %I64d", mAudioPlayingTimeMs);
#else
						LOGD("mAudioPlayingTimeMs: %lld", mAudioPlayingTimeMs);
#endif
                    }
                }
            
    	        av_free_packet(pPacket);
                av_free(pPacket);
                pPacket = NULL;
                continue;
            }
            else if (ret == FFSTREAM_ERROR_FLUSHING)
            {
				// seek is done!
				LOGI("audio seek is done!");
                mSeeking = false;
                mOutputBufferingStartMs = 0;
                avcodec_flush_buffers(mAudioContext->codec);
                av_free(pPacket);
                pPacket = NULL;
                continue;
            }
    		else if (ret == FFSTREAM_ERROR_BUFFERING)
    		{
		        LOGD("audio queue no data");
				struct timespec ts;
				ts.tv_sec = 0;
				ts.tv_nsec = 10000000; // 10 msec
#if defined(__CYGWIN__) || defined(_MSC_VER)
				int64_t now_usec = getNowUs();
				int64_t now_sec = now_usec / 1000000;
				now_usec	= now_usec - now_sec * 1000000;
				ts.tv_sec	+= now_sec;
				ts.tv_nsec	+= (long)now_usec * 1000;
				pthread_cond_timedwait(&mCondition, &mLock, &ts);
#else
				pthread_cond_timedwait_relative_np(&mCondition, &mLock, &ts);
#endif
                continue;
    		}
    		else if (ret == FFSTREAM_ERROR_EOF)
    		{
    		    LOGI("reach audio stream end");
                mReachEndStream = true;
                mPlayerStatus = MEDIA_PLAYER_PLAYBACK_COMPLETE;
#if defined(__CYGWIN__) || defined(_MSC_VER)
				SDL_PauseAudio(1);
#endif
                break;
            }
			else if (ret == FFSTERAM_ERROR_SWITCH_AUDIO)
			{
				continue;
			}
            else
            {
                LOGE("Read audio packet error:%d", ret);
                break;
            }
        }
    }

    LOGI("audio thread exited");
}

void* AudioPlayer::audio_thread(void* ptr)
{    
	LOGI("audio player thread started");

	AudioPlayer* audioPlayer = (AudioPlayer *) ptr;
	if (audioPlayer->mRender->start() != OK) {
		LOGE("failed to start audio render");
		return NULL;
	}
    
#ifdef __ANDROID__
    LOGD("getpriority before:%d", getpriority(PRIO_PROCESS, 0));
    int audioThreadPriority = -6;
    if(setpriority(PRIO_PROCESS, 0, audioThreadPriority) != 0)
    {
        LOGE("set audio thread priority failed");
    }
    LOGD("getpriority after:%d", getpriority(PRIO_PROCESS, 0));
#endif
    
    audioPlayer->run();

#if !defined(OSLES_IMPL) && !defined(__CYGWIN__) && !defined(_MSC_VER)
    AudioTrack_stop();
#endif

#ifdef __ANDROID__
    setpriority(PRIO_PROCESS, 0, 0);
#endif

	LOGI("audio player thread exited");
    return NULL;
}

status_t AudioPlayer::setListener(MediaPlayerListener* listener)
{
    mListener = listener;
    return OK;
}

void AudioPlayer::notifyListener_l(int msg, int ext1, int ext2)
{
    if (mListener != NULL)
        mListener->notify(msg, ext1, ext2);
    else
		LOGE("mListener is null");
}

status_t AudioPlayer::selectAudioChannel(int32_t index)
{
	mAudioStreamIndex = index;
	if (mRender)
		mRender->flush();
	LOGI("audioPlayer select audio #%d", index);
	return OK;
}