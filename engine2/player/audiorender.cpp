#include "audiorender.h"
#define LOG_TAG "AudioRender"
#include "log.h"
#include "audiotrack.h"
#ifdef PCM_DUMP
#include "apAudioEncoder.h"
#endif
#ifndef _MSC_VER
#include <unistd.h> // for usleep
#endif

#ifdef OSLES_IMPL
#include "oslesrender.h"
#endif
#if defined(__CYGWIN__) || defined(_MSC_VER)
#include "sdl.h"
#define SDL_AUDIO_SAMPLES		1024
// 2015.4.3 64k * 4 buf_size cause hls_live always buffring problem
// change to 64k * 4 because some WMV clip has GIANT audio pkt (about 1/3 sec)
#define FIFO_BUFFER_SIZE		(65536 * 4)
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
#if defined(__CYGWIN__) || defined(_MSC_VER)
	mAudioLogCnt = 0;
#endif

	mClosed		= false;

#ifdef PCM_DUMP
	mEncoder	= NULL;
	mIpAddr		= NULL;
	mPort		= 0;
#endif
}

AudioRender::~AudioRender()
{
	close();

	if (mSamples != NULL) {
		// Free audio samples buffer
		av_free(mSamples);
		mSamples = NULL;
	}
	if (mConvertCtx != NULL) {
		swr_free(&mConvertCtx);
		mConvertCtx = NULL;
	}
#ifdef OSLES_IMPL
	if (a_render) {
		delete a_render;
		a_render = NULL;
	}
#endif

#ifdef PCM_DUMP
	if (mEncoder) {
		delete mEncoder;
		mEncoder = NULL;
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

	// channel support 1 or 2
	if (mChannelsOutput != mDeviceChannels)
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
	if (mChannelLayout <= 0)
		mChannelLayout = AV_CH_LAYOUT_MONO;

	mChannels = channels;
	if(mChannels <= 0)
		mChannels = 1;

	mSampleFormatOutput = mSampleFormat;
	mSampleRateOutput = mSampleRate;
	mChannelLayoutOutput = mChannelLayout;
	mChannelsOutput = mChannels;

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
			if (mSampleFormatOutput < AV_SAMPLE_FMT_U8) {
				mSampleFormatOutput = AV_SAMPLE_FMT_U8;
				mFormatSizeOutput = 1;
			}
			else if (mSampleFormatOutput > AV_SAMPLE_FMT_S16) {
				mSampleFormatOutput = AV_SAMPLE_FMT_S16;
				mFormatSizeOutput = 2;
			}
			LOGI("mSampleFormatOutput:%d", mSampleFormatOutput);
			LOGI("mFormatSizeOutput:%d", mFormatSizeOutput);

            // overwrite bit_per_sample to 1 or 2
			mBitPerSample = mFormatSizeOutput * 8;
			LOGI("bitPerSample reset to(need convert): %d", mBitPerSample);

			// valid sample rate is 4k - 48k
			if (mSampleRateOutput < 4000)
				mSampleRateOutput = 4000;
			else if (mSampleRateOutput > 48000)
				mSampleRateOutput = 48000;
			LOGI("mSampleRateOutput:%d", mSampleRateOutput);

			mChannelLayoutOutput = mDeviceChannelLayoutOutput;
			mChannelsOutput = mDeviceChannels;
			LOGI("mChannelsOutput:%d, mChannelLayoutOutput:%lld", mChannelsOutput, mChannelLayoutOutput);

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

	mOneSecSize = mSampleRateOutput * mChannelsOutput * mBitPerSample / 8;

#if defined(__CYGWIN__) || defined(_MSC_VER)
	// 2015.6.15 guoliangma added
	// sdl must init at the same thread
	SDL_Init(SDL_INIT_AUDIO);

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
	wanted_spec.samples = SDL_AUDIO_SAMPLES;
	wanted_spec.callback = audio_callback;
	wanted_spec.userdata = this;

	if (SDL_OpenAudio(&wanted_spec, &spec) < 0) {
		LOGE("SDL_OpenAudio: %s", SDL_GetError());
		return ERROR;
	}

	LOGI("SDL_AudioSpec got: chn %d, fmt 0x%x, freq %d", spec.channels, spec.format, spec.freq);
	//		#define AUDIO_S16LSB	0x8010	/**< Signed 16-bit samples */

	mFifo.create(FIFO_BUFFER_SIZE);

#ifdef PCM_DUMP
	if (mIpAddr != NULL && mPort != 0) {
		mEncoder = new apAudioEncoder();
		if (!mEncoder->init(mIpAddr, mPort, mChannelsOutput, mSampleRateOutput, mSampleFormatOutput, 64000)) {
			LOGW("failed to init audio encoder");
			delete mEncoder;
			mEncoder = NULL;
		}
	}
#endif

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

#ifdef PCM_DUMP
	if (mIpAddr != NULL && mPort != 0) {
		mEncoder = new apAudioEncoder();
		if (!mEncoder->init(mIpAddr, mPort, mChannelsOutput, mSampleRateOutput, mSampleFormatOutput, 64000)) {
			LOGW("failed to init audio encoder");
			delete mEncoder;
			mEncoder = NULL;
		}
	}
#endif

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

	if (mConvertCtx != NULL) {
#ifndef NDEBUG
		int64_t begin_decode = getNowMs();
#endif
		int32_t sampleInCount = audioFrame->nb_samples;
		LOGD("sampleInCount: %d", sampleInCount);
		int sampleOutCount = (int)av_rescale_rnd(
			swr_get_delay(mConvertCtx, mSampleRate) + sampleInCount,
			mSampleRateOutput,
			mSampleRate,
			AV_ROUND_UP);

		int sampleCountOutput = swr_convert(mConvertCtx,
			(uint8_t**)(&mSamples), (int)sampleOutCount,
			(const uint8_t**)(audioFrame->extended_data), sampleInCount);
		if (sampleCountOutput < 0) {
			LOGE("Audio convert sampleformat(%d) failed, ret %d", mSampleFormat, sampleCountOutput);
			return ERROR;
		}
		else if (sampleCountOutput == 0) {
			LOGW("no audio data in the frame");
			return OK;
		}
		else {
			audio_buffer = mSamples;
			audio_buffer_size = sampleCountOutput * mChannelsOutput * mFormatSizeOutput;
			LOGD("swr output: sample:%d, size:%d", sampleCountOutput, audio_buffer_size);
		}

#ifndef NDEBUG
		int64_t end_decode = getNowMs();
		LOGD("convert audio cost %lld[ms]", end_decode - begin_decode);
#endif
	}
	else {
		audio_buffer = audioFrame->data[0];
		// 2015.1.28 guoliangma fix noisy audio play problem 
		// some clip linesize is bigger than actual data size
		// e.g. linesize[0] = 2048 and nb_samples = 502
		audio_buffer_size = audioFrame->nb_samples * mChannels * mBitPerSample / 8;
	}

	LOGD("audio nb_samples %d, linesize %d, audio_buffer_size %d", audioFrame->nb_samples, audioFrame->linesize[0], audio_buffer_size);

#ifdef PCM_DUMP
	if (mEncoder) {
		if (!mEncoder->write_audio_frame((uint8_t *)audio_buffer, audio_buffer_size))
			LOGW("failed to write audio data to encoder %p %d", audio_buffer, audio_buffer_size);
	}
#endif

#ifdef _DUMP_PCM_DATA_
	static FILE *pFile = NULL;
	if (!pFile)
		pFile = fopen("d:\\test.pcm", "wb");
	fwrite(audio_buffer, 1, audio_buffer_size, pFile);
#endif

#if defined(__CYGWIN__) || defined(_MSC_VER)
	int left;
	int count = 0;
	int written;
	while (count < 50) { // 500 msec
		left = mFifo.size() - mFifo.used();
		if (left >= (int)audio_buffer_size) {
			count = 0;
			break;
		}

		SDL_Delay(10); // 10 msec
		count++;
	}

	written = mFifo.write((char *)audio_buffer, audio_buffer_size);
	if (written != (int)audio_buffer_size)
		LOGW("fifo overflow(sdl audio) %d -> %d", audio_buffer_size, written);
#elif defined(OSLES_IMPL)
	int written;
	written = a_render->write_data((const char *)audio_buffer, audio_buffer_size);
	if (written != (int)audio_buffer_size) // may occur when stopping
		LOGW("fifo overflow(osles) %d -> %d", audio_buffer_size, written);
#else
	LOGD("before AudioTrack_write");
	int32_t written;
	written = AudioTrack_write(audio_buffer, audio_buffer_size);
	LOGD("after AudioTrack_write");
	if (written < 0) {
		LOGE("failed to write audio sample %d %d", audio_buffer_size, written);
		return ERROR;
	}
	else if (written < (int)audio_buffer_size) {
		LOGW("write audio sample partially %d %d", audio_buffer_size, written);
	}

	LOGD("Write audio sample size:%d", written);
#endif
	return OK;
}

status_t AudioRender::start()
{
#if defined(__CYGWIN__) || defined(_MSC_VER)
	SDL_PauseAudio(0);
	mAudioLogCnt = 0;
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
	if (mClosed)
		return OK;

#if defined(__CYGWIN__) || defined(_MSC_VER)
	SDL_CloseAudio();
#elif defined(OSLES_IMPL)
	if (a_render)
		a_render->close();
#else
    AudioTrack_stop();
	AudioTrack_close();
#endif

	mClosed = true;
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
	mAudioLogCnt = 0;
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
	if (a_render) {
		a_render->flush();
		return OK;
	}
	return ERROR;
#else // for ios
	return AudioTrack_flush();
#endif
}

int AudioRender::get_one_sec_size()
{
	return mOneSecSize;
}

int AudioRender::get_latency()
{
#if defined(OSLES_IMPL) // android with osles
	if (a_render)
		return a_render->get_latency();
	else
		return 0;
#elif defined(__CYGWIN__) || defined(_MSC_VER)
	int latency = mFifo.used() * 1000 / mOneSecSize; // msec
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
	if (ins)
		ins->audio_callback_impl(stream, len);
}

void AudioRender::audio_callback_impl(Uint8 *stream, int len)
{
	int ret;

	ret = mFifo.read((char *)stream, len);
	if (ret != len) {
		memset(stream, 0, len);
		mAudioLogCnt++;
		if (mAudioLogCnt < 10)
			LOGW("SDL audio buffer underflow #%d: %d.%d", mAudioLogCnt, ret, len);
	}
}
#endif

