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

#define DEFAULT_DEVICE_CH_LAYOUT	AV_CH_LAYOUT_STEREO
#define DEFAULT_DEVICE_CHANNELS		2

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

#ifdef OSLES_IMPL
	a_render = NULL;
#endif
#if defined(__CYGWIN__) || defined(_MSC_VER)
	mAudioLogCnt = 0;
#endif

	mStopping	= false;
	mClosed		= false;

#ifdef PCM_DUMP
	mEncoder	= NULL;
	mDumpUrl	= NULL;
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
	if (mChannelLayout <= 0) {
		mChannelLayout = AV_CH_LAYOUT_MONO;
		LOGW("channelLayout is invalid, use AV_CH_LAYOUT_MONO as default");
	}

	mChannels = channels;
	if(mChannels <= 0)
		mChannels = 1;

	mSampleFormatOutput = mSampleFormat;
	mSampleRateOutput = mSampleRate;

	switch (mSampleFormat) {
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
			if (mSamples == NULL) {
				LOGE("No enough memory for audio conversion");
				return ERROR;
			}
		}

		switch (mSampleFormat) {
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

		LOGI("mSampleFormatOutput: %d, mFormatSizeOutput: %d", 
			mSampleFormatOutput, mFormatSizeOutput);

		// overwrite bit_per_sample to 1 or 2
		mBitPerSample = mFormatSizeOutput * 8;
		LOGI("bitPerSample reset to(need convert): %d", mBitPerSample);

		// valid sample rate is 4k - 48k
		if (mSampleRateOutput < 4000)
			mSampleRateOutput = 4000;
		else if (mSampleRateOutput > 48000)
			mSampleRateOutput = 48000;
		LOGI("mSampleRateOutput:%d", mSampleRateOutput);

		// android and ios device force use 2 channels stereo
		// we assume android/ios devices have two speakers.
		// todo: need to support devices which have more speakers.
		mChannelLayoutOutput = DEFAULT_DEVICE_CH_LAYOUT;
		mChannelsOutput = DEFAULT_DEVICE_CHANNELS;
		LOGI("mChannelsOutput:%d, mChannelLayoutOutput:%lld", mChannelsOutput, mChannelLayoutOutput);

		if (!init_swr()) {
			LOGE("failed to init swr");
			return ERROR;
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
		LOGE("failed to SDL_OpenAudio(): %s", SDL_GetError());
		return ERROR;
	}

	LOGI("SDL_AudioSpec got: chn %d, fmt 0x%x, freq %d", spec.channels, spec.format, spec.freq);
	//		#define AUDIO_S16LSB	0x8010	/**< Signed 16-bit samples */

	mFifo.create(FIFO_BUFFER_SIZE);

#ifdef PCM_DUMP
	if (mDumpUrl != NULL) {
		mEncoder = new apAudioEncoder();
		if (!mEncoder->init(mDumpUrl, mChannelsOutput, mSampleRateOutput, mSampleFormatOutput, 64000)) {
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
	if (mDumpUrl != NULL) {
		mEncoder = new apAudioEncoder();
		if (!mEncoder->init(mDumpUrl, mChannelsOutput, mSampleRateOutput, mSampleFormatOutput, 64000)) {
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

bool AudioRender::init_swr()
{
	if (mConvertCtx) {
		swr_free(&mConvertCtx);
		mConvertCtx = NULL;
	}

	mConvertCtx = swr_alloc_set_opts(mConvertCtx,
		mChannelLayoutOutput,
		mSampleFormatOutput,
		mSampleRateOutput,
		mChannelLayout,
		mSampleFormat,
		mSampleRate,
		0, 0);
	int ret = swr_init(mConvertCtx);
	if (ret < 0 || mConvertCtx == NULL) {
		LOGE("swr_init failed: %d %p", ret, mConvertCtx);
		return false;
	}

	char src_audio_layout_name[64] = {0};
	char dst_audio_layout_name[64] = {0};
	char src_audio_fmt_name[64] = {0};
	char dst_audio_fmt_name[64] = {0};
	av_get_channel_layout_string(src_audio_layout_name, 64, mChannels, mChannelLayout);
	av_get_channel_layout_string(dst_audio_layout_name, 64, mChannelsOutput, mChannelLayoutOutput);
	av_get_sample_fmt_string(src_audio_fmt_name, 64, mSampleFormat);
	av_get_sample_fmt_string(dst_audio_fmt_name, 64, mSampleFormatOutput);

#ifdef _MSC_VER
	LOGI("swr ctx inited: layout %I64d(%s), fmt %d(%s), rate %d -> %I64d(%s) %d(%s) %d",
		mChannelLayout, dst_audio_layout_name, mSampleFormat, src_audio_fmt_name, mSampleRate,
		mChannelLayoutOutput, dst_audio_layout_name, mSampleFormatOutput, dst_audio_fmt_name, mSampleRateOutput);
#else
	LOGI("swr ctx inited: layout %lld(%s), fmt %d(%s), rate %d -> %lld(%s) %d(%s) %d",
		mChannelLayout, dst_audio_layout_name, mSampleFormat, src_audio_fmt_name, mSampleRate,
		mChannelLayoutOutput, dst_audio_layout_name, mSampleFormatOutput, dst_audio_fmt_name, mSampleRateOutput);
#endif

	return true;
}

status_t AudioRender::render(AVFrame* audioFrame)//int16_t* buffer, uint32_t buffer_size)
{
	void* audio_buffer = NULL;
	uint32_t audio_buffer_size = 0;

	// 2015.8.12 guoliangma added to fix some audio resample crash
	// root cause: some audio frame prop changed from channel_layout 5.1(channels 6) to channel_layout 2(channels 2) 
	// 2015.8.27 guoliangma added "NOT zero" because some audio file has no channel_layout attribute(e.g. wma)
	if (audioFrame->channel_layout != 0 &&
		(audioFrame->channel_layout != mChannelLayout || audioFrame->channels != mChannels)) {
		char frame_layout_name[64] = {0};
		char audio_layout_name[64] = {0};
		av_get_channel_layout_string(frame_layout_name, 64, audioFrame->channels, audioFrame->channel_layout);
		av_get_channel_layout_string(audio_layout_name, 64, mChannels, mChannelLayout);
		LOGW("audio frame channel_layout NOT match %lld(%s) -> %lld(%s)", 
			mChannelLayout, audio_layout_name, 
			audioFrame->channel_layout, frame_layout_name);

		// update audio params
		mChannels		= audioFrame->channels;
		mChannelLayout	= audioFrame->channel_layout;

		LOGI("re-alloc swr convert");
		// re-init swr
		if (!init_swr()) {
			LOGE("failed to init swr");
			return ERROR;
		}
	}

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
			(uint8_t**)(&mSamples), sampleOutCount,
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
	while (!mStopping) {
		if (a_render->free_size() >= (int)audio_buffer_size)
			break;
		
		usleep(1000 * 5);// 5 msec
	}

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

	// interrupt blocked write
	mStopping = true;

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

