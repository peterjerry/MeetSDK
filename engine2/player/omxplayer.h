/*
 * Copyright (C) 2015 Michael Ma guoliangma@pptv.com
 *
 */


#ifndef OMX_PLAYER_H_
#define OMX_PLAYER_H_
#include "player.h"
#include "loop.h"

class OMXPlayer : public IPlayer, MediaPlayerListener
{
public:
	OMXPlayer();
    ~OMXPlayer();

    status_t setDataSource(const char* url);
	status_t setDataSource(int32_t fd, int64_t offset, int64_t length);
	status_t selectAudioChannel(int32_t index);
	status_t selectSubtitleChannel(int32_t index);
	status_t setVideoSurface(void* surface);
	status_t prepare();
	status_t prepareAsync();
    status_t start();
    status_t stop();
    status_t pause();
    status_t reset();
    status_t seekTo(int32_t msec);
    status_t getVideoWidth(int32_t* w);
    status_t getVideoHeight(int32_t* h);
    status_t getCurrentPosition(int32_t* msec);
    status_t getDuration(int32_t* msec);
	status_t getProcessBytes(int64_t *len);
    status_t setAudioStreamType(int32_t type);
	status_t setLooping(int32_t loop);
	status_t setVolume(float leftVolume, float rightVolume);
    status_t setListener(MediaPlayerListener* listener);
	int32_t flags();
	bool isLooping();
    bool isPlaying();

	status_t getBufferingTime(int *msec);

	status_t suspend();

    status_t resume();

	status_t startCompatibilityTest(){return OK;}
	void stopCompatibilityTest(){}
	void set_opt(const char *opt){}
	void notify(int msg, int ext1, int ext2){}

private:
	enum PlayerEvent {
		PREPRARE_EVENT,
		VIDEO_RENDER_EVENT,
		STREAM_READ_EVENT,
		STREAM_DONE_EVENT,
		BUFFERING_UPDATE_EVENT,
		SEEKING_EVENT,
		CHECK_AUDIO_STATUS_EVENT,
		BUFFERING_START_EVENT,
		BUFFERING_END_EVENT,
		SEEKING_COMPLETE_EVENT,
	};

	// event
	class OMXPrepareEvent:public Event {
	public:
		OMXPrepareEvent(void * opaque){
			m_id		= PREPRARE_EVENT;
			m_opaque	= opaque;
		}
		~OMXPrepareEvent(){}
		virtual void action(void *opaque, int64_t now_us);
	};

	void notifyListener_l(int msg, int ext1 = 0, int ext2 = 0);

	void setPlaying(bool isPlaying);
	void postPrepareEvent_l();

	static void onPrepare(void *opaque);

	void onPrepareImpl();
private:
	char *m_url;
	MediaPlayerListener*	mListener;
	EventLoop mMsgLoop;

	bool mPrepareEventPending;
};

#endif // OMX_PLAYER_H_