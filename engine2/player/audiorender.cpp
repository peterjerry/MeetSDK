#include "audiorender.h"
#define LOG_TAG "AudioRender"
#include "log.h"
#include "audiotrack.h"
#ifndef _MSC_VER
#include <unistd.h> // for usleep
#endif

#ifdef OSLES_IMPL
#include "oslesrender.h"
#endif
#if defined(__CYGWIN__) || defined(_MSC_VER)
#include "sdl.h"
#define SDL_AUDIO_BUFFER_SIZE	1024
#define FIFO_BUFFER_SIZE		65536
#endif

AudioRender::AudioRender()
{
	mSamplesSize = AVCODEC_MAX_AUDIO_FRAME_SIZE * 2;
	mSamples = NULL;
	mConvertCtx = NULL;

	//we assume android/ios devices have two speakers.
	//todo: need to support devices which have more speakers.
	mDeviceChannelLayoutOutput = AV_CH_LAYOUT_STEREO;
	mDeviceChannels = 2; // hard code to 2 channels?
#ifdef OSLES_IMPL
	a_render = NULL;
#endif
}

AudioRender::~AudioRender()
{
	if(mSamples != NULL)
	{
		// Free audio samples buffer
		av_free(mSamples);
		mSamples = NULL;
	}
	if(mConvertCtx != NULL)
	{
		swr_free(&mConvertCtx);
		mConvertCtx = NULL;
	}
#ifdef OSLES_IMPL
	if(a_render) {
		delete a_render;
		a_render = NULL;
	}
#endif
}

bool AudioRender::need_convert()
{
	// only support sample rate between 4k to 48k
	if (mSampleRateOutput < 4000)
		return true;
	if (mSampleRateOutput > 48000)
		return true;

	// channel suport 1 or 2
	if (mChannelsOutput != 2/*> mDeviceChannels*/) // mDeviceChannels is 2
		return true;

	// only support U8 and S16
	if ((mSampleFormatOutput != AV_SAMPLE_FMT_U8) &&
		(mSampleFormatOutput != AV_SAMPLE_FMT_S16))
		return true;

	return false;
}

status_t AudioRender::open(int sampleRate, 
						   uint64_t channelLayout,
						   int channels,
						   AVSampleFormat sampleFormat)
{
#ifdef _MSC_VER
	LOGI("open audio render: sr %d, layout %I64d, channels %d, fmt %d", sampleRate, channelLayout, channels, sampleFormat);
#else
	LOGI("open audio render: sr %d, layout %lld, channels %d, fmt %d", sampleRate, channelLayout, channels, sampleFormat);
#endif
	mSampleRate = sampleRate;
	mSampleFormat = sampleFormat;

	mChannelLayout = channelLayout;
	if(mChannelLayout <= 0)
		mChannelLayout = AV_CH_LAYOUT_MONO;

	mChannels = channels;
	if(mChannels <= 0)
		mChannels = 1;

	mSampleFormatOutput = mSampleFormat;
	mSampleRateOutput = mSampleRate;
	mChannelLayoutOutput = mChannelLayout;
	mChannelsOutput = mChannels;

#if defined(__CYGWIN__) || defined(OSLES_IMPL) || defined(_MSC_VER)
	switch(mSampleFormat)
	{
	case AV_SAMPLE_FMT_U8:
	case AV_SAMPLE_FMT_U8P:
		mBitPerSample = 8;
		break;
	case AV_SAMPLE_FMT_S16:
	case AV_SAMPLE_FMT_S16P:
		mBitPerSample = 16;
		break;
	case AV_SAMPLE_FMT_S32:
	case AV_SAMPLE_FMT_S32P:
	case AV_SAMPLE_FMT_FLT:
	case AV_SAMPLE_FMT_FLTP:
		mBitPerSample = 32;
		break;
	case AV_SAMPLE_FMT_DBL:
	case AV_SAMPLE_FMT_DBLP:
		mBitPerSample = 64;
		break;
	default:
		mBitPerSample = 16;
		LOGW("unsupported sample format(for osles) %d", mSampleFormat);
		break;
	}
#endif   

	if (need_convert()) {
		LOGI("need do audio conversion");

		if (mSamples == NULL) {
			mSamples = (int16_t*)av_malloc(mSamplesSize);
			if(mSamples == NULL) {
				LOGE("No enough memory for audio conversion");
				return ERROR;
			}
		}

		if (mConvertCtx == NULL)
		{
			switch(mSampleFormat)
			{
			case AV_SAMPLE_FMT_U8:
			case AV_SAMPLE_FMT_U8P:
				mFormatSize = 1;
				break;
			case AV_SAMPLE_FMT_S16:
			case AV_SAMPLE_FMT_S16P:
				mFormatSize = 2;
				break;
			case AV_SAMPLE_FMT_S32:
			case AV_SAMPLE_FMT_S32P:
			case AV_SAMPLE_FMT_FLT:
			case AV_SAMPLE_FMT_FLTP:
				mFormatSize = 4;
				break;
			case AV_SAMPLE_FMT_DBL:
			case AV_SAMPLE_FMT_DBLP:
				mFormatSize = 8;
				break;
			default:
				mFormatSize = 2;
				LOGW("unsupported sample format %d", mSampleFormat);
				break;
			}

			mFormatSizeOutput = mFormatSize;
			if(mSampleFormatOutput < AV_SAMPLE_FMT_U8) {
				mSampleFormatOutput = AV_SAMPLE_FMT_U8;
				mFormatSizeOutput = 1;
			}
			else if(mSampleFormatOutput > AV_SAMPLE_FMT_S16) {
				mSampleFormatOutput = AV_SAMPLE_FMT_S16;
				mFormatSizeOutput = 2;
			}
			LOGI("mSampleFormatOutput:%d", mSampleFormatOutput);
			LOGI("mFormatSizeOutput:%d", mFormatSizeOutput);

#if defined(__CYGWIN__) || defined(_MSC_VER) || defined(OSLES_IMPL)				
			mBitPerSample = mFormatSizeOutput * 8;
			LOGI("bitPerSample reset to(need convert): %d", mBitPerSample);
#endif

			if(mSampleRateOutput < 4000)
				mSampleRateOutput = 4000;
			else if(mSampleRateOutput > 48000)
				mSampleRateOutput = 48000;
			LOGI("mSampleRateOutput:%d", mSampleRateOutput);

			if(mChannelsOutput > mDeviceChannels) {
				mChannelLayoutOutput = mDeviceChannelLayoutOutput;
				mChannelsOutput = mDeviceChannels;
			}
			else if(mChannelsOutput < 1) {
				mChannelLayoutOutput = AV_CH_LAYOUT_MONO;
				mChannelsOutput = 1;
			}
			LOGI("mChannelLayoutOutput:%lld", mChannelLayoutOutput);
			LOGI("mChannelsOutput:%d", mChannelsOutput);

			/*
			mConvertCtx = swr_alloc();
			av_opt_set_int(mConvertCtx, "in_channel_layout", mChannelLayout, 0);
			av_opt_set_int(mConvertCtx, "out_channel_layout", mChannelLayoutOutput, 0);
			av_opt_set_int(mConvertCtx, "in_sample_rate", mSampleRate, 0);
			av_opt_set_int(mConvertCtx, "out_sample_rate", mSampleRateOutput, 0);
			av_opt_set_sample_fmt(mConvertCtx, "in_sample_fmt", mSampleFormat, 0);
			av_opt_set_sample_fmt(mConvertCtx, "out_sample_fmt", mSampleFormatOutput, 0);
			*/
			mConvertCtx = swr_alloc_set_opts(mConvertCtx,
				mChannelLayoutOutput,
				mSampleFormatOutput,
				mSampleRateOutput,
				mChannelLayout,
				mSampleFormat,
				mSampleRate,
				0, 0);                   
			if (swr_init(mConvertCtx) < 0 || mConvertCtx == NULL) {
				LOGE("swr_init failed");
				return ERROR;
			}
#ifdef _MSC_VER
			LOGI("swr ctx inited: layout %I64d, fmt %d, rate %d -> %I64d %d %d",
				mChannelLayout, mSampleFormat, mSampleRate,
				mChannelLayoutOutput, mSampleFormatOutput, mSampleRateOutput);
#else
			LOGI("swr ctx inited: layout %lld, fmt %d, rate %d -> %lld %d %d",
				mChannelLayout, mSampleFormat, mSampleRate,
				mChannelLayoutOutput, mSampleFormatOutput, mSampleRateOutput);
#endif
		}
	}

#if defined(__CYGWIN__) || defined(_MSC_VER)
	Uint16 fmt;
	if (mBitPerSample == 8)
		fmt = AUDIO_S8;
	else if(mBitPerSample == 16)
		fmt = AUDIO_S16SYS;
	else {
		LOGE("bitPerSample: %d not supported in sdl audio", mBitPerSample);
		return ERROR;
	}

	SDL_AudioSpec wanted_spec, spec;
	memset(&wanted_spec, 0, sizeof(SDL_AudioSpec));
	memset(&spec, 0, sizeof(SDL_AudioSpec));
	wanted_spec.freq = mSampleRateOutput;
	wanted_spec.format = fmt;
	wanted_spec.channels = mChannelsOutput;
	wanted_spec.silence = 0;
	wanted_spec.samples = SDL_AUDIO_BUFFER_SIZE;
	wanted_spec.callback = audio_callback;
	wanted_spec.userdata = this;

	if(SDL_OpenAudio(&wanted_spec, &spec) < 0) {
		LOGE("SDL_OpenAudio: %s", SDL_GetError());
		return ERROR;
	}

	LOGI("SDL_AudioSpec got: chn %d, fmt 0x%x, freq %d", spec.channels, spec.format, spec.freq);
	//		#define AUDIO_S16LSB	0x8010	/**< Signed 16-bit samples */

	mFifo.create(FIFO_BUFFER_SIZE);

	return OK;
#elif defined(OSLES_IMPL)
	LOGI("implement osles audio render");
	a_render = new and_osles;
	if(!a_render)
		return ERROR;

	int ret;
	ret = a_render->open(mSampleRateOutput, mChannelsOutput, mBitPerSample);
	if(ret != 0)
		return ERROR;

	return OK;
#else
	LOGI("implement audiotrack audio render");
	return AudioTrack_open(mSampleRateOutput, mChannelLayoutOutput, mSampleFormatOutput);
#endif
}

status_t AudioRender::render(AVFrame* audioFrame)//int16_t* buffer, uint32_t buffer_size)
{
	void* audio_buffer = NULL;
	uint32_t audio_buffer_size = 0;
	if(mConvertCtx != NULL)
	{
#ifndef NDEBUG
		int64_t begin_decode = getNowMs();
#endif
		uint32_t sampleInCount = audioFrame->nb_samples;
		LOGD("sampleInCount:%d", sampleInCount);
		int sampleOutCount = (int)av_rescale_rnd(
			swr_get_delay(mConvertCtx, mSampleRate) + sampleInCount,
			mSampleRateOutput,
			mSampleRate,
			AV_ROUND_UP);

		int sampleCountOutput = swr_convert(mConvertCtx,
			(uint8_t**)(&mSamples), (int)sampleOutCount,
			(const uint8_t**)(audioFrame->extended_data), (int)sampleInCount);
		if (sampleCountOutput < 0) {
			LOGE("Audio convert sampleformat(%d) failed, ret %d", mSampleFormat, sampleCountOutput);
			return ERROR;
		}
		else if(sampleCountOutput > 0)
		{
			audio_buffer = mSamples;
			audio_buffer_size = sampleCountOutput * mChannelsOutput * mFormatSizeOutput;
			LOGD("sampleCountOutput:%d", sampleCountOutput);
			LOGD("buffer_size:%d", audio_buffer_size);
		}
#ifndef NDEBUG
		int64_t end_decode = getNowMs();
		LOGD("convert audio cost %lld[ms]", end_decode - begin_decode);
#endif
	}
	else
	{
		audio_buffer = audioFrame->data[0];
		audio_buffer_size = audioFrame->linesize[0];
	}

	int32_t size = 0;
#if defined(__CYGWIN__) || defined(_MSC_VER)
	int left;
	int count = 0;
	while (count < 50) { // 1sec
		left = mFifo.size() - mFifo.used();
		if (left >= (int)audio_buffer_size) {
			count = 0;
			break;
		}

		SDL_Delay(10); // 50msec
		count++;
	}

	size = mFifo.write((char *)audio_buffer, audio_buffer_size);
	if (size != (int32_t)audio_buffer_size)
		LOGW("fifo overflow(sdl audio) %d -> %d", audio_buffer_size, size);
#elif defined(OSLES_IMPL)
	int count = 0;
	while (a_render->free_size() < (int)audio_buffer_size) {
		usleep(1000 * 5);// 5 msec
		count++;
		if (count > 100) { // 500 msec
			LOGW("write audio buffer(osles) timeout 500 msec");
			break;
		}
	}
	size = a_render->write_data((const char *)audio_buffer, audio_buffer_size);
	if (size != (int32_t)audio_buffer_size)
		LOGW("fifo overflow(osles) %d -> %d", audio_buffer_size, size);
#else
	LOGD("before AudioTrack_write");
	size = AudioTrack_write(audio_buffer, audio_buffer_size);
	LOGD("after AudioTrack_write");
	if (size < 0) {
		LOGE("failed to write audio sample %d %d", audio_buffer_size, size);
		return ERROR;
	}
	else if (size < (int)audio_buffer_size) {
		LOGW("write audio sample partially %d %d", audio_buffer_size, size);
	}

	LOGD("Write audio sample size:%d", size);
#endif
	return OK;
}

status_t AudioRender::start()
{
#if defined(__CYGWIN__) || defined(_MSC_VER)
	SDL_PauseAudio(0);
	return OK;
#elif defined(OSLES_IMPL)
	if(a_render->play() != 0)
		return ERROR;
	return OK;
#else
	return AudioTrack_start();
#endif
}

status_t AudioRender::close()
{
#if defined(__CYGWIN__) || defined(_MSC_VER)
	SDL_CloseAudio();
#elif defined(OSLES_IMPL)
	if (a_render)
		a_render->close();
#else
    AudioTrack_stop();
	AudioTrack_close();
#endif

	return OK;
}

status_t AudioRender::pause()
{
#if defined(__CYGWIN__) || defined(_MSC_VER)
	SDL_PauseAudio(1);
	return OK;
#elif defined(OSLES_IMPL)
	if(a_render) {
		if(a_render->pause() != 0)
			return ERROR;
		return OK;
	}
	return ERROR;
#else
	return AudioTrack_pause();
#endif
}

status_t AudioRender::resume()
{
#if defined(__CYGWIN__) || defined(_MSC_VER)
	SDL_PauseAudio(0);
	return OK;
#elif defined(OSLES_IMPL) // android with osles audio render
	if(a_render) {
		if(a_render->resume() != 0)
			return ERROR;
		return OK;
	}
	return ERROR;
#else
	// android with audiotrack audio render
	return AudioTrack_resume();
#endif
}

status_t AudioRender::flush()
{
#if defined(__CYGWIN__) || defined(_MSC_VER)
	mFifo.reset();
	return OK;
#elif defined(OSLES_IMPL)
	if(a_render) {
		//todo
		return OK;
	}
	return ERROR;
#else
	return AudioTrack_flush();
#endif
}

int AudioRender::get_latency()
{
#if defined(OSLES_IMPL) // android with osles
	if (a_render)
		return a_render->get_latency();
	else
		return 0;
#elif defined(__CYGWIN__) || defined(_MSC_VER)
	int one_sec_size = mSampleRateOutput * mChannelsOutput * mBitPerSample / 8;
	int latency = FIFO_BUFFER_SIZE  * 1000 / one_sec_size;
	return latency;
#else // android with audiotrack and ios with openal
	return AudioTrack_getLatency();
#endif

	return 0;
}

#if defined(__CYGWIN__) || defined(_MSC_VER)
void AudioRender::audio_callback(void *userdata, Uint8 *stream, int len)
{
	AudioRender *ins = (AudioRender *)userdata;
	if (ins) {
		ins->audio_callback_impl(stream, len);
		LOGD("audio_callback %d", len);
	}
}

void AudioRender::audio_callback_impl(Uint8 *stream, int len)
{
	int ret;
	static int count = 0;

	ret = mFifo.read((char *)stream, len);
	if (ret != len) {
		memset(stream, 0, len);
		count++;
		if (count < 10)
			LOGW("SDL audio buffer underflow #%d: %d.%d", count, ret, len);
	}
}
#endif

