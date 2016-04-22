#ifndef _AUDIO_RENDER_H
#define _AUDIO_RENDER_H

#include "player.h"
#include "ppffmpeg.h"

#if defined(__CYGWIN__) || defined(_MSC_VER)
#include "sdl.h"
#include "fifobuffer.h"
#endif

#define AVCODEC_MAX_AUDIO_FRAME_SIZE 192000 // 1 second of 48khz 32bit audio

class and_osles;
class apAudioEncoder;

class AudioRender
{
public:
    AudioRender();
    
    ~AudioRender();

    status_t open(int sampleRate, uint64_t channelLayout, int channels, AVSampleFormat sampleFormat);

    status_t render(AVFrame* audioFrame);

	status_t start();

	status_t close();

	status_t pause();

	status_t resume();

	status_t flush();

	int get_latency(); // msec

	int get_one_sec_size();

#ifdef PCM_DUMP
	void set_dump(const char *url) {
		mDumpUrl = url;
	}
#endif

#if defined(__CYGWIN__) || defined(_MSC_VER)
private:
	static void audio_callback(void *userdata, Uint8 *stream, int len);

	void audio_callback_impl(Uint8 *stream, int len);
#endif

private:
	bool need_convert();

	bool init_swr();

private:
    struct SwrContext * mConvertCtx;
    int mSampleRate;
    int mSampleRateOutput;
    uint64_t mChannelLayout;
    uint64_t mChannelLayoutOutput;
    int mChannels;
    int mChannelsOutput;
    int mDeviceChannels;
    int mDeviceChannelLayoutOutput;
    AVSampleFormat mSampleFormat;
    AVSampleFormat mSampleFormatOutput;
    uint32_t mFormatSize;
    uint32_t mFormatSizeOutput;

    int16_t* mSamples;
    uint32_t mSamplesSize;
#ifdef OSLES_IMPL
	// added new opensl es audio render
	and_osles* a_render;
#endif
#if defined(__CYGWIN__) || defined(_MSC_VER)
	and_fifobuffer mFifo;
	int mAudioLogCnt;
#endif
	int mBitPerSample; // 8, 16, ...
	int mOneSecSize;

	bool mStopping;
	bool mClosed;

#ifdef PCM_DUMP
	apAudioEncoder* mEncoder;
	const char*		mDumpUrl;
#endif
};

#endif // _AUDIO_RENDER_H