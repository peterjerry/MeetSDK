/*
 * Copyright (C) 2015 guoliangma@pptv.com
 *
 */
 
#ifndef FF_AUDIO_PLAYER_H
#define FF_AUDIO_PLAYER_H

#include <pthread.h>
//#include <sys/types.h>

#include "player.h"
#include "packetqueue.h"
#include "ffstream.h"

struct AVStream;
struct AVFrame;
class AudioRender;

class AudioPlayer
{
public:
	AudioPlayer();
    AudioPlayer(FFStream* dataStream, AVStream* context, int32_t streamIndex);
    ~AudioPlayer();
	
	status_t prepare();

	status_t start();

	status_t stop();

	status_t pause();

	status_t flush();

    int32_t getStatus(){ return mPlayerStatus;}

	int64_t getMediaTimeMs(); 

    status_t setListener(MediaPlayerListener* listener);

    status_t seekTo(int64_t msec);

	status_t selectAudioChannel(int32_t index);

private:
	status_t start_l();

	status_t stop_l();

	status_t pause_l();

	status_t flush_l();

    int process_pkt(AVPacket *packet);

	void wait(int msec);

	static void* audio_thread(void* ptr);

	void audio_thread_impl();

    void notifyListener_l(int msg, int ext1 = 0, int ext2 = 0);

	int64_t get_channel_layout(uint64_t channel_layout, int channels);

	status_t setup_render();

	int64_t get_time_msec();

	void set_time_msec(int64_t msec);
private:
    uint32_t mPlayerStatus;
    int16_t* mSamples;
    uint32_t mSamplesSize;
	AVFrame* mAudioFrame;
    int64_t mNumFramesPlayed;
    uint32_t mLatencyMs;
	int64_t mAudioPlayingTimeMs;
	int64_t mSeekTimeMs;
	bool mReachEndStream;
	int32_t mAudioStreamIndex;
	bool mSeeking;
    MediaPlayerListener* mListener;
	AudioRender* mRender;

	FFStream* mDataStream;
    AVStream* mAudioContext;
    int64_t mDurationMs;
	int64_t mLastPacketMs;
	
    pthread_t mThread;
    pthread_mutex_t mLock;
    pthread_cond_t mCondition;
	pthread_mutex_t mClockLock;

};

#endif //FF_AUDIO_PLAYER_H