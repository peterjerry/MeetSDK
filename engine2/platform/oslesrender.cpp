#include "oslesrender.h"

#include <linux/stddef.h>
#include <jni.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#define LOG_TAG "OpenSLesPlayer"
#include "log.h"
#include "fifobuffer.h"
#include <stdlib.h>

#define AUDIO_BLOCK_SIZE	8192 // for audio player

// 2014.9.17 guoliangma wmv video audio frame is too big, about 200ms per frame(about 50k data)
// so need a big fifo to hold this buffer
#define AUDIO_FIFO_SIZE		65536 //16384// 64k -> 16k

// aux effect on the output mix, used by the buffer queue player
//const SLEnvironmentalReverbSettings reverbSettings = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;

typedef struct osles_handle{
	// engine interfaces
	SLObjectItf engineObject;
	SLEngineItf engineEngine;

	// output mix interfaces
	SLObjectItf outputMixObject;
	SLEnvironmentalReverbItf outputMixEnvironmentalReverb;

	// buffer queue player interfaces
	SLObjectItf bqPlayerObject;
	SLPlayItf bqPlayerPlay;
	SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;
	SLEffectSendItf bqPlayerEffectSend;
	SLMuteSoloItf bqPlayerMuteSolo;
	SLVolumeItf bqPlayerVolume;
}osles_handle;

static void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context);

#define CheckErr(x) if(SL_RESULT_SUCCESS != (x)) \
	{LOGE("and_osles osles function error %d", (x)); \
	return -1;}

and_osles::and_osles(void)
:m_osles_handle(NULL), m_fifo(NULL), m_pBuf(NULL), m_latency(0), m_one_sec_byte(0)
{
}

and_osles::~and_osles(void)
{
	close();
}

int and_osles::open(int sample_rate, int channel, int bitsPerSample)
{
	LOGI("and_osles open()");

	close();

	LOGI("sample_rate %d, channel %d, bitsPerSample %d", 
		sample_rate, channel, bitsPerSample);

	m_osles_handle = (osles_handle *)malloc(sizeof(osles_handle));
	if(!m_osles_handle) {
		LOGE("failed to alloc osles handle");
		return -1;
	}
	
	if( createEngine() < 0){
		LOGE("failed to create engine");
		return -1;
	}

	if( createBufferQueueAudioPlayer(sample_rate, channel, bitsPerSample) < 0) {
		LOGE("failed to create player");
		return -1;
	}

	m_fifo = new and_fifobuffer;
	m_fifo->create(AUDIO_FIFO_SIZE);

	m_one_sec_byte = sample_rate * channel * bitsPerSample / 8;
	m_latency = AUDIO_FIFO_SIZE  * 1000 / m_one_sec_byte;

	LOGI("osles opened: rate %d, chn %d, bit %d, latency %d", 
		sample_rate, channel, bitsPerSample, m_latency);
	return 0;
}

int and_osles::play()
{
	LOGI("and_osles play()");

	SLresult result;
	osles_handle *handle = (osles_handle *)m_osles_handle;

	// set the player's state to playing
	result = (*(handle->bqPlayerPlay))->SetPlayState(handle->bqPlayerPlay, SL_PLAYSTATE_PLAYING);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("and_osles: failed to play");
		return -1;
	}

	// enqueue 1st buffer
	result = (*(handle->bqPlayerBufferQueue))->Enqueue(handle->bqPlayerBufferQueue,
		m_pBuf, AUDIO_BLOCK_SIZE);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to Enqueue 1st data: %d", result);
		return -1;
	}

	return 0;
}

int and_osles::pause()
{
	LOGI("and_osles pause()");

	SLresult result;
	osles_handle *handle = (osles_handle *)m_osles_handle;

	result = (*(handle->bqPlayerPlay))->SetPlayState(handle->bqPlayerPlay, SL_PLAYSTATE_PAUSED);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("and_osles: failed to pause");
		return -1;
	}

	return 0;
}

int and_osles::resume()
{
	LOGI("and_osles resume()");
	
	SLresult result;
	osles_handle *handle = (osles_handle *)m_osles_handle;

	result = (*(handle->bqPlayerPlay))->SetPlayState(handle->bqPlayerPlay, SL_PLAYSTATE_PLAYING);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("and_osles: failed to resume");
		return -1;
	}

	return 0;
}

void and_osles::close()
{
	//and_log_writeline_simple(0, LOG_DEBUG, "and_osles close()");

	if(!m_osles_handle) {
		return;
	}

	SLresult result;
	osles_handle *handle = (osles_handle *)m_osles_handle;

	LOGI("before stop ogles player");
	// set the player's state to playing
	result = (*(handle->bqPlayerPlay))->SetPlayState(handle->bqPlayerPlay, SL_PLAYSTATE_STOPPED);
	if (SL_RESULT_SUCCESS != result)
		LOGE("and_osles: failed to stop");

	// destroy buffer queue audio player object, and invalidate all associated interfaces
	LOGI("before del ogles player");
	if (handle->bqPlayerObject != NULL) {
		(*(handle->bqPlayerObject))->Destroy(handle->bqPlayerObject);
		handle->bqPlayerObject = NULL;
		handle->bqPlayerPlay = NULL;
		handle->bqPlayerBufferQueue = NULL;
		handle->bqPlayerEffectSend = NULL;
		handle->bqPlayerMuteSolo = NULL;
		handle->bqPlayerVolume = NULL;
	}
	
	// destroy output mix object, and invalidate all associated interfaces
	LOGI("before del ogles mix");
	if (handle->outputMixObject != NULL) {
		(*(handle->outputMixObject))->Destroy(handle->outputMixObject);
		handle->outputMixObject = NULL;
		handle->outputMixEnvironmentalReverb = NULL;
	}

	// destroy engine object, and invalidate all associated interfaces
	LOGI("before del ogles engine");
	if (handle->engineObject != NULL) {
		(*(handle->engineObject))->Destroy(handle->engineObject);
		handle->engineObject = NULL;
		handle->engineEngine = NULL;
	}

	LOGI("before del ogles handle");
	free(m_osles_handle);
	m_osles_handle = NULL;

	if(m_pBuf) {
		free(m_pBuf);
		m_pBuf = NULL;
	}

	if(m_fifo) {
		delete m_fifo;
		m_fifo = NULL;
	}

	LOGI("and_osles closed");
}

int and_osles::getVol()
{
	return 0;
}

int and_osles::setVol(int vol)
{
	SLresult    res; 
	//SLVolumeItf  volumeItf; 
	SLmillibel  bel_vol;
	
	osles_handle * handle = (osles_handle *)m_osles_handle;

	int increment;
	if(vol >= 0)
		increment =  250;
	else
		increment = -250;

	res = (*(handle->bqPlayerVolume))->GetVolumeLevel(handle->bqPlayerVolume, &bel_vol);
	if (SL_RESULT_SUCCESS != res) {
		LOGE("and_osles: failed to get vol %d", bel_vol);
		return -1;
	}

	bel_vol += increment;
	res = (*(handle->bqPlayerVolume))->SetVolumeLevel(handle->bqPlayerVolume, bel_vol + increment);
	if (SL_RESULT_SUCCESS != res) {
		LOGE("and_osles: failed to set vol %d.%d", vol, bel_vol);
		return -1;
	}

	LOGI("and_osles: set vol to %d.%d", vol, bel_vol);
	return bel_vol;
}

// create the engine and output mix objects
int and_osles::createEngine()
{	
	LOGI("createEngine()");

	SLresult result;

	osles_handle * handle = (osles_handle *)m_osles_handle;
	
	// create engine
	result = slCreateEngine(&handle->engineObject, 0, NULL, 0, NULL, NULL);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to CreateEngine");
		return -1;
	}

	// realize the engine
	result = (*(handle->engineObject))->Realize(handle->engineObject, SL_BOOLEAN_FALSE);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to Realize");
		return -1;
	}

	// get the engine interface, which is needed in order to create other objects
	result = (*(handle->engineObject))->GetInterface(handle->engineObject,
		SL_IID_ENGINE, &handle->engineEngine);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to GetInterface");
		return -1;
	}

	// create output mix, with environmental reverb specified as a non-required interface
	const SLInterfaceID ids[1] = {SL_IID_ENVIRONMENTALREVERB};
	const SLboolean req[1] = {SL_BOOLEAN_FALSE};
	result = (*(handle->engineEngine))->CreateOutputMix(handle->engineEngine,
		&handle->outputMixObject, 1, ids, req);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to CreateOutputMix");
		return -1;
	}

	// realize the output mix
	result = (*(handle->outputMixObject))->Realize(handle->outputMixObject, SL_BOOLEAN_FALSE);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to Realize");
		return -1;
	}
	return 0;
}

int and_osles::free_size()
{
	return m_fifo->size() - m_fifo->used();
}

int and_osles::write_data(const char *buf, int size)
{
	if (!m_fifo)
		return -1;

	//LOGV("write_data %d", size);
	return m_fifo->write((char *)buf, size);
}

int and_osles::read_data(char *buf, int size)
{
	if (!m_fifo)
		return -1;

	//LOGV("read_data %d", size);
	return m_fifo->read(buf, size);
}

void and_osles::flush()
{
	if (m_fifo)
		m_fifo->reset();
}

int and_osles::get_latency()
{
	return m_fifo->used() * 1000 / m_one_sec_byte; // msec
}

// create buffer queue audio player
int and_osles::createBufferQueueAudioPlayer(int sample_rate, int channel, int bitsPerSample)
{
	LOGI("createBufferQueueAudioPlayer()");

	SLresult result;
	osles_handle * handle = (osles_handle *)m_osles_handle;

	// configure audio source
	SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
		SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
	SLDataFormat_PCM format_pcm;  
	format_pcm.formatType = SL_DATAFORMAT_PCM;  
	format_pcm.numChannels = channel;  
	format_pcm.samplesPerSec = sample_rate * 1000;//SL_SAMPLINGRATE_44_1  
	format_pcm.bitsPerSample = bitsPerSample;  
	format_pcm.containerSize = 16;  
	if(channel == 2)  
		format_pcm.channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;  
	else  
		format_pcm.channelMask = SL_SPEAKER_FRONT_CENTER;  
	format_pcm.endianness = SL_BYTEORDER_LITTLEENDIAN;  

	SLDataSource audioSrc = {&loc_bufq, &format_pcm};

	// configure audio sink
	SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, handle->outputMixObject};
	SLDataSink audioSnk = {&loc_outmix, NULL};

	// create audio player
	const SLInterfaceID ids[3] = {SL_IID_BUFFERQUEUE, SL_IID_EFFECTSEND, SL_IID_VOLUME};
	const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
	result = (*(handle->engineEngine))->CreateAudioPlayer(handle->engineEngine,
		&handle->bqPlayerObject, 
		&audioSrc, &audioSnk, 3, ids, req);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to CreateAudioPlayer");
		return -1;
	}

	// realize the player
	result =  (*(handle->bqPlayerObject))->Realize(handle->bqPlayerObject, SL_BOOLEAN_FALSE);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to Realize the player");
		return -1;
	}

	// get the play interface
	result = (*(handle->bqPlayerObject))->GetInterface(handle->bqPlayerObject,
		SL_IID_PLAY, &handle->bqPlayerPlay);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to GetInterface SL_IID_PLAY");
		return -1;
	}

	// get the buffer queue interface
	result = (*(handle->bqPlayerObject))->GetInterface(handle->bqPlayerObject,
		SL_IID_BUFFERQUEUE, 
		(void *)&handle->bqPlayerBufferQueue);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to GetInterface SL_IID_BUFFERQUEUE");
		return -1;
	}

	// register callback on the buffer queue
	result = (*(handle->bqPlayerBufferQueue))->RegisterCallback(handle->bqPlayerBufferQueue,
		bqPlayerCallback, this);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to RegisterCallback");
		return -1;
	}

	// get the effect send interface
	result = (*(handle->bqPlayerObject))->GetInterface(handle->bqPlayerObject, 
		SL_IID_EFFECTSEND, 
		&handle->bqPlayerEffectSend);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to GetInterface SL_IID_EFFECTSEND");
		return -1;
	}

	// get the volume interface
	result = (*(handle->bqPlayerObject))->GetInterface(handle->bqPlayerObject, 
		SL_IID_VOLUME, &handle->bqPlayerVolume);
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to GetInterface SL_IID_VOLUME");
		return -1;
	}
	
	m_pBuf = (char *)malloc(AUDIO_BLOCK_SIZE);
	if(!m_pBuf) {
		LOGE("failed to alloc buf");
		return -1;
	}
	memset(m_pBuf, 0, AUDIO_BLOCK_SIZE);

	// all done
	LOGI("createBufferQueueAudioPlayer all done!");
	return 0;
}

// this callback handler is called every time a buffer finishes playing
static void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
	//LOGD("bqPlayerCallback");
	static int count = 0;

	int readed;
	SLresult result;

	and_osles *ins = (and_osles *)context;
	char *buf = ins->get_buf();
	readed = ins->read_data(buf, AUDIO_BLOCK_SIZE);
	if (readed < 0) {
		LOGE("failed to read audio data");
		return;
	}
	else if(readed < AUDIO_BLOCK_SIZE) {
		memset(buf, 0, AUDIO_BLOCK_SIZE);
		if (count < 5)
			LOGW("audio data underflow %d %d, fill mute(cnt %d)", AUDIO_BLOCK_SIZE, readed, count++);
	}
	else {
		count = 0;
	}

	// enqueue another buffer
	osles_handle *handle =  (osles_handle *)ins->get_handle();
	result = (*(handle->bqPlayerBufferQueue))->Enqueue(handle->bqPlayerBufferQueue,
		buf, AUDIO_BLOCK_SIZE);
	// the most likely other result is SL_RESULT_BUFFER_INSUFFICIENT,
	// which for this code example would indicate a programming error
	if (SL_RESULT_SUCCESS != result) {
		LOGE("failed to Enqueue %d", result);
		return;
	}

	LOGV("osles audio data enqueued %d", readed);
}

