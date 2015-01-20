/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */


#ifndef FF_STREAM_H_

#define FF_STREAM_H_

#include <pthread.h>
#include "player.h"
#include "packetqueue.h"

#define FFSTREAM_OK					0
#define FFSTREAM_ERROR				-1
#define FFSTREAM_ERROR_EOF			-2
#define FFSTREAM_ERROR_BUFFERING	-3
#define FFSTREAM_ERROR_STREAMINDEX	-4
#define FFSTREAM_ERROR_FLUSHING		-5
#define FFSTERAM_ERROR_SWITCH_AUDIO	-6

#define FFSTREAM_INITED 1
#define FFSTREAM_PREPARED 2
#define FFSTREAM_STARTED 3
#define FFSTREAM_PAUSED 4
#define FFSTREAM_STOPPED 5
#define FFSTREAM_STOPPING 6

/* calc video realtime fps and bps time unit */
#define MAX_CALC_SEC 3

struct AVFormatContext;
struct AVStream;
struct AVSubtitle;
struct AVPacket;
class ISubtitles;
class FFSourceBase;

class FFStream
{
public:
	FFStream();
	~FFStream();

	enum URL_TYPE
	{
		TYPE_LOCAL_FILE,
		TYPE_ONDEMAND,
		TYPE_BROADCAST,
		TYPE_LIVE,
		TYPE_UNKNOWN
	};

	enum REALTIME_LEVEL
	{
		LEVEL_HIGH,
		LEVEL_LOW
	};
	
	status_t selectAudioChannel(int32_t index);

	void setSource(FFSourceBase *source){mSource = source;}
	FFSourceBase *getSource(){return mSource;}

	AVFormatContext* open(char* uri);

	status_t start();

	status_t stop();

	status_t interrupt_open();

	status_t seek(int64_t seekTimeMs, int incr);

	int current_bit_rate(){return m_real_bit_rate;}

    status_t setListener(MediaPlayerListener* listener);
	status_t getPacket(int32_t streamIndex, AVPacket** packet);
	status_t status(){return mStatus;}
	status_t isSeeking(){return mSeeking;}

	status_t setLooping(bool loop);
	bool isLooping(){return mLooping;}

	int64_t getDurationMs();

	int64_t getCachedDurationMs(){return mCachedDurationMs;}

	int64_t getStartTime(){return mStartTime;}

	int64_t getTotalReadBytes(){return m_total_read_bytes;}

	AVStream* getAudioStream(){return mAudioStream;}

	AVStream* getVideoStream(){return mVideoStream;}

	AVStream* getSubtitleStream(){return mSubtitleStream;}

	int32_t getAudioStreamIndex(){return mAudioStreamIndex;}

	int32_t getVideoStreamIndex(){return mVideoStreamIndex;}

	int32_t getSubtitleStreamIndex(){return mSubtitleStreamIndex;}

	void setISubtitle(ISubtitles* subtitle);

	URL_TYPE getURLType(){return mUrlType;}

	bool getTrackInfo(TrackInfo** info, int *max_num);

	REALTIME_LEVEL getRealtimeLevel(){return mRealtimeLevel;}
#if defined(__ANDROID__) || defined(_MSC_VER)
	status_t getBufferingTime(int *msec);
#endif
	status_t refresh();
	status_t disableStream(int32_t streamIndex);
	
private:
	status_t flush_l();
	static void* demux_thread(void* ptr);
	void run();
	//status_t join_l();
	static int interrupt_l(void *ctx);
    void notifyListener_l(int msg, int ext1 = 0, int ext2 = 0);

	static int ff_read_packet(void *opaque, uint8_t *buf, int buf_size);

	static int64_t ff_seek_packet(void *opaque, int64_t offset, int whence);

private:
	status_t mStatus;

	FFSourceBase*	mSource;
	void*			mPb;
	unsigned char*	mPbBuf;

	AVFormatContext* mMovieFile;
    int64_t mDurationMs;
	uint32_t mFrameRate;
	uint32_t mStreamsCount;
    MediaPlayerListener* mListener;
	uint32_t mBufferSize; // current video+audio packet total byte size
	uint32_t mMinPlayBufferCount; // minimum package count to play
	int64_t mMaxPlayBufferMs; // minimum pakcage duration to play
	uint32_t mMaxBufferSize;
	bool mReachEndStream; // set to true when av_read_frame() return AVERROR_EOF
	bool mIsBuffering; // flag that demuxer is caching enough video and audio data for play
	int64_t mSeekTimeMs;
	int mSeekFlag;
	bool mSeeking; // flag set to true when call "seek", reset to false when "seek" is done
	int64_t mCachedDurationMs;
	URL_TYPE mUrlType;
	bool mDelaying;
	bool mRefreshed;
	bool mLooping;
	int64_t mStartTime; // pts for loop play
	int64_t mGopDuration; // mGopEnd - mGopStart
	int64_t mGopStart;// video I frame pts
	int64_t mGopEnd;// video next I frame pts
	REALTIME_LEVEL mRealtimeLevel;
	
	//audio
	int32_t mAudioStreamIndex;
	AVStream* mAudioStream;
    PacketQueue mAudioQueue;
    //pthread_mutex_t mAudioQueueLock;
	//int32_t mAudioBufferSize;

	//video
	int32_t mVideoStreamIndex;
	AVStream* mVideoStream;
    PacketQueue mVideoQueue;
    //pthread_mutex_t mVideoQueueLock;
	//int32_t mVideoBufferSize;

	//subtitles
	int32_t			mSubtitleStreamIndex;
	AVStream*		mSubtitleStream;
	AVSubtitle*		mAVSubtitle;
	ISubtitles*		mISubtitle;
	
    pthread_t		mThread;
    pthread_cond_t	mCondition;
    pthread_mutex_t mLock;

	/* real time kbps */
	int				m_io_bit_rate;
	int				m_real_bit_rate;
	int				m_read_bytes[MAX_CALC_SEC]; /* 记录5秒内的字节数. */
	int				m_vb_index;
	int64_t			m_total_read_bytes;

	int64_t	mOpenStreamStartMs;
};


#endif

