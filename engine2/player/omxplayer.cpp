#include "omxplayer.h"
#include "common.h"

#include <assert.h>
#include <jni.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>

#define LOG_TAG "omxplayer"
#include "log.h"

#ifdef _MSC_VER
#define my_strdup _strdup
#else
#define my_strdup strdup
#endif

// for native window JNI
#include <android/native_window_jni.h>

// engine interfaces
static XAObjectItf engineObject = NULL;
static XAEngineItf engineEngine = NULL;

// output mix interfaces
static XAObjectItf outputMixObject = NULL;

// streaming media player interfaces
static XAObjectItf             playerObj = NULL;
static XAPlayItf               playerPlayItf = NULL;
static XAAndroidBufferQueueItf playerBQItf = NULL;
static XAStreamInformationItf  playerStreamInfoItf = NULL;
static XAVolumeItf             playerVolItf = NULL;

// number of required interfaces for the MediaPlayer creation
#define NB_MAXAL_INTERFACES 3 // XAAndroidBufferQueueItf, XAStreamInformationItf and XAPlayItf

// video sink for the player
static ANativeWindow* theNativeWindow;

// number of buffers in our buffer queue, an arbitrary number
#define NB_BUFFERS 8

// we're streaming MPEG-2 transport stream data, operate on transport stream block size
#define MPEG2_TS_PACKET_SIZE 188

// number of MPEG-2 transport stream blocks per buffer, an arbitrary number
#define PACKETS_PER_BUFFER 10

// determines how much memory we're dedicating to memory caching
#define BUFFER_SIZE (PACKETS_PER_BUFFER*MPEG2_TS_PACKET_SIZE)

// where we cache in memory the data to play
// note this memory is re-used by the buffer queue callback
static char dataCache[BUFFER_SIZE * NB_BUFFERS];

// handle of the file to play
static FILE *file;

// has the app reached the end of the file
static jboolean reachedEof = JNI_FALSE;

// constant to identify a buffer context which is the end of the stream to decode
static const int kEosBufferCntxt = 1980; // a magic value we can compare against

// For mutual exclusion between callback thread and application thread(s).
// The mutex protects reachedEof, discontinuity,
// The condition is signalled when a discontinuity is acknowledged.

static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond = PTHREAD_COND_INITIALIZER;

// whether a discontinuity is in progress
static jboolean discontinuity = JNI_FALSE;

static jboolean enqueueInitialBuffers(jboolean discontinuity);

static void createEngine();

static XAresult AndroidBufferQueueCallback(
        XAAndroidBufferQueueItf caller,
        void *pCallbackContext,        /* input */
        void *pBufferContext,          /* input */
        void *pBufferData,             /* input */
        XAuint32 dataSize,             /* input */
        XAuint32 dataUsed,             /* input */
        const XAAndroidBufferItem *pItems,/* input */
        XAuint32 itemsLength           /* input */);

static void StreamChangeCallback(XAStreamInformationItf caller,
        XAuint32 eventId,
        XAuint32 streamIndex,
        void * pEventData,
        void * pContext );

extern "C" IPlayer* getOMXPlayer(void* context)
{
#ifdef __ANDROID__
#ifdef BUILD_ONE_LIB
	pplog = __pp_log_vprint;
#else
    platformInfo = (PlatformInfo*)context;
    gs_jvm = (JavaVM*)(platformInfo->jvm);
	pplog = (LogFunc)(platformInfo->pplog_func); 
#endif
#endif
    return new OMXPlayer();
}

extern "C" void releaseOMXPlayer(IPlayer* player)
{
	if (player) {
		delete player;
		player = NULL;
	}
}

OMXPlayer::OMXPlayer()
{
	createEngine();

	mPrepareEventPending = false;
}

OMXPlayer::~OMXPlayer()
{
}

status_t OMXPlayer::setDataSource(const char* url)
{
	m_url = my_strdup(url);
	return OK;
}

status_t OMXPlayer::setDataSource(int32_t fd, int64_t offset, int64_t length)
{
	return OK;
}

status_t OMXPlayer::selectAudioChannel(int32_t index)
{
	return OK;
}

status_t OMXPlayer::selectSubtitleChannel(int32_t index)
{
	return OK;
}

status_t OMXPlayer::setVideoSurface(void* surface)
{
	LOGI("omxpalayer op setVideoSurface %p", surface);

	if (surface == NULL)
        return ERROR;

	JNIEnv* env = NULL;
    jint status;
	status = gs_jvm->GetEnv((void**) &env, JNI_VERSION_1_4);
	if (JNI_OK != status){
		LOGE("GetEnv failed %d", status);
		return ERROR;
	}
	
	// obtain a native window from a Java surface
    theNativeWindow = ANativeWindow_fromSurface(env, (jobject)surface);
	if (!theNativeWindow) {
		LOGE("failed to get window");
		return ERROR;
	}

	LOGI("get native window %p", theNativeWindow);
	return OK;
}

status_t OMXPlayer::prepare()
{
	return OK;
}

status_t OMXPlayer::prepareAsync()
{
	mMsgLoop.start();

    postPrepareEvent_l();
	return OK;
}

void OMXPlayer::postPrepareEvent_l()
{
	if (mPrepareEventPending)
        return;

    mPrepareEventPending = true;

	OMXPrepareEvent *evt = new OMXPrepareEvent(this);
	mMsgLoop.postEventTohHeader(evt);
}

void OMXPlayer::onPrepare(void *opaque)
{
	OMXPlayer *ins = (OMXPlayer *)opaque;
	if (ins)
		ins->onPrepareImpl();
}

#define XA_ANDROID_MIME_AVC              ((XAchar *) "video/avc")

void OMXPlayer::onPrepareImpl()
{
	LOGI("onPrepareImpl()");

	XAresult res;

	// open the file to play
    file = fopen(m_url, "rb");
    if (file == NULL) {
		LOGE("failed to open file %s", m_url);
		notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_FAIL_TO_OPEN);
        return;
    }

	// configure data source
    XADataLocator_AndroidBufferQueue loc_abq = { XA_DATALOCATOR_ANDROIDBUFFERQUEUE, NB_BUFFERS };
    XADataFormat_MIME format_mime = {
            XA_DATAFORMAT_MIME, XA_ANDROID_MIME_MP2TS, XA_CONTAINERTYPE_MPEG_TS };
	XADataSource dataSrc = {&loc_abq, &format_mime};

    // configure audio sink
    XADataLocator_OutputMix loc_outmix = { XA_DATALOCATOR_OUTPUTMIX, outputMixObject };
    XADataSink audioSnk = { &loc_outmix, NULL };

    // configure image video sink
    XADataLocator_NativeDisplay loc_nd = {
            XA_DATALOCATOR_NATIVEDISPLAY,        // locatorType
            // the video sink must be an ANativeWindow created from a Surface or SurfaceTexture
            (void*)theNativeWindow,              // hWindow
            // must be NULL
            NULL                                 // hDisplay
    };
    XADataSink imageVideoSink = {&loc_nd, NULL};

    // declare interfaces to use
    XAboolean     required[NB_MAXAL_INTERFACES]
                           = {XA_BOOLEAN_TRUE, XA_BOOLEAN_TRUE,           XA_BOOLEAN_TRUE};
    XAInterfaceID iidArray[NB_MAXAL_INTERFACES]
                           = {XA_IID_PLAY,     XA_IID_ANDROIDBUFFERQUEUESOURCE,
                                               XA_IID_STREAMINFORMATION};

	LOGI("before create mediaplayer");
    // create media player
    res = (*engineEngine)->CreateMediaPlayer(engineEngine, &playerObj, &dataSrc,
            NULL, &audioSnk, &imageVideoSink, NULL, NULL,
            NB_MAXAL_INTERFACES /*XAuint32 numInterfaces*/,
            iidArray /*const XAInterfaceID *pInterfaceIds*/,
            required /*const XAboolean *pInterfaceRequired*/);
    assert(XA_RESULT_SUCCESS == res);

    // realize the player
    res = (*playerObj)->Realize(playerObj, XA_BOOLEAN_FALSE);
    assert(XA_RESULT_SUCCESS == res);

	LOGI("before get interface");
    // get the play interface
    res = (*playerObj)->GetInterface(playerObj, XA_IID_PLAY, &playerPlayItf);
    assert(XA_RESULT_SUCCESS == res);

    // get the stream information interface (for video size)
    res = (*playerObj)->GetInterface(playerObj, XA_IID_STREAMINFORMATION, &playerStreamInfoItf);
    assert(XA_RESULT_SUCCESS == res);

    // get the volume interface
    res = (*playerObj)->GetInterface(playerObj, XA_IID_VOLUME, &playerVolItf);
    assert(XA_RESULT_SUCCESS == res);

    // get the Android buffer queue interface
    res = (*playerObj)->GetInterface(playerObj, XA_IID_ANDROIDBUFFERQUEUESOURCE, &playerBQItf);
    assert(XA_RESULT_SUCCESS == res);

    // specify which events we want to be notified of
    res = (*playerBQItf)->SetCallbackEventsMask(playerBQItf, XA_ANDROIDBUFFERQUEUEEVENT_PROCESSED);
    assert(XA_RESULT_SUCCESS == res);

    // register the callback from which OpenMAX AL can retrieve the data to play
    res = (*playerBQItf)->RegisterCallback(playerBQItf, AndroidBufferQueueCallback, NULL);
    assert(XA_RESULT_SUCCESS == res);

    // we want to be notified of the video size once it's found, so we register a callback for that
    res = (*playerStreamInfoItf)->RegisterStreamChangeCallback(playerStreamInfoItf,
            OMXPlayer::StreamChangeCallback, this);
    assert(XA_RESULT_SUCCESS == res);

	LOGI("before enqueueInitialBuffers");
    // enqueue the initial buffers
    if (!enqueueInitialBuffers(JNI_FALSE)) {
		notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_FAIL_TO_OPEN);
        return;
    }

    // prepare the player
    res = (*playerPlayItf)->SetPlayState(playerPlayItf, XA_PLAYSTATE_PAUSED);
    assert(XA_RESULT_SUCCESS == res);

    // set the volume
    res = (*playerVolItf)->SetVolumeLevel(playerVolItf, 0);
    assert(XA_RESULT_SUCCESS == res);

	// start the playback
	LOGI("before start playback()");
    res = (*playerPlayItf)->SetPlayState(playerPlayItf, XA_PLAYSTATE_PLAYING);
        assert(XA_RESULT_SUCCESS == res);

	LOGI("before notifyListener_l()");
	notifyListener_l(MEDIA_INFO, MEDIA_INFO_TEST_PLAYER_TYPE, FF_PLAYER);
}

void OMXPlayer::notifyListener_l(int msg, int ext1, int ext2)
{
	LOGI("notifyListener_l() %d %d %d", msg, ext1, ext2);
    if (mListener != NULL)
        mListener->notify(msg, ext1, ext2);
    else
		LOGE("mListener is null");
}

status_t OMXPlayer::start()
{
	setPlaying(true);
	return OK;
}

status_t OMXPlayer::stop()
{
	// destroy streaming media player object, and invalidate all associated interfaces
    if (playerObj != NULL) {
        (*playerObj)->Destroy(playerObj);
        playerObj = NULL;
        playerPlayItf = NULL;
        playerBQItf = NULL;
        playerStreamInfoItf = NULL;
        playerVolItf = NULL;
    }

    // destroy output mix object, and invalidate all associated interfaces
    if (outputMixObject != NULL) {
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = NULL;
    }

    // destroy engine object, and invalidate all associated interfaces
    if (engineObject != NULL) {
        (*engineObject)->Destroy(engineObject);
        engineObject = NULL;
        engineEngine = NULL;
    }

    // close the file
    if (file != NULL) {
        fclose(file);
        file = NULL;
    }

    // make sure we don't leak native windows
    if (theNativeWindow != NULL) {
        ANativeWindow_release(theNativeWindow);
        theNativeWindow = NULL;
    }

	return OK;
}

status_t OMXPlayer::pause()
{
	setPlaying(false);
	return OK;
}

status_t OMXPlayer::reset()
{
	return OK;
}

status_t OMXPlayer::seekTo(int32_t msec)
{
	XAresult res;

    // make sure the streaming media player was created
    if (NULL != playerPlayItf) {

        // set the player's state
		res = (*playerPlayItf)->SetMarkerPosition(playerPlayItf, msec);
        assert(XA_RESULT_SUCCESS == res);
		return OK;
    }

	return ERROR;
}

status_t OMXPlayer::getVideoWidth(int32_t* w)
{
	*w = m_width;
	return OK;
}

status_t OMXPlayer::getVideoHeight(int32_t* h)
{
	*h = m_height;
	return OK;
}

status_t OMXPlayer::getCurrentPosition(int32_t* msec)
{
	XAresult res;

    // make sure the streaming media player was created
    if (NULL != playerPlayItf) {
		XAmillisecond pos_msec;
		res = (*playerPlayItf)->GetPosition(playerPlayItf, &pos_msec);
        assert(XA_RESULT_SUCCESS == res);
		*msec = pos_msec;
		return OK;
    }

	return ERROR;
}

status_t OMXPlayer::getDuration(int32_t* msec)
{
	XAresult res;

    // make sure the streaming media player was created
    if (NULL != playerPlayItf) {
		XAmillisecond duration_msec;
		res = (*playerPlayItf)->GetDuration(playerPlayItf, &duration_msec);
        assert(XA_RESULT_SUCCESS == res);
		*msec = duration_msec;
		return OK;
    }

	return ERROR;
}

status_t OMXPlayer::getProcessBytes(int64_t *len)
{
	return OK;
}

status_t OMXPlayer::setAudioStreamType(int32_t type)
{
	return OK;
}

status_t OMXPlayer::setLooping(int32_t loop)
{
	return OK;
}

status_t OMXPlayer::setVolume(float leftVolume, float rightVolume)
{
	return OK;
}

status_t OMXPlayer::setListener(MediaPlayerListener* listener)
{
	LOGI("setListener %p", listener);
    mListener = listener;
    return OK;
}

int32_t OMXPlayer::flags()
{
	return OK;
}

bool OMXPlayer::isLooping()
{
	return false;
}

bool OMXPlayer::isPlaying()
{
	XAresult res;

    // make sure the streaming media player was created
    if (NULL != playerPlayItf) {

        // set the player's state
		XAuint32 state;
		res = (*playerPlayItf)->GetPlayState(playerPlayItf, &state);
        assert(XA_RESULT_SUCCESS == res);

		return (state == XA_PLAYSTATE_PLAYING);
    }

	return false;
}

status_t OMXPlayer::getBufferingTime(int *msec)
{
	return OK;
}

status_t OMXPlayer::suspend()
{
	return OK;
}

status_t OMXPlayer::resume()
{
	return OK;
}

// callback invoked whenever there is new or changed stream information
void OMXPlayer::StreamChangeCallback(XAStreamInformationItf caller,
        XAuint32 eventId,
        XAuint32 streamIndex,
        void * pEventData,
        void * pContext )
{
    LOGI("StreamChangeCallback called for stream %u", streamIndex);
    // pContext was specified as NULL at RegisterStreamChangeCallback and is unused here
    assert(NULL == pContext);
    switch (eventId) {
      case XA_STREAMCBEVENT_PROPERTYCHANGE: {
        /** From spec 1.0.1:
            "This event indicates that stream property change has occurred.
            The streamIndex parameter identifies the stream with the property change.
            The pEventData parameter for this event is not used and shall be ignored."
         */

        XAresult res;
        XAuint32 domain;
        res = (*caller)->QueryStreamType(caller, streamIndex, &domain);
        assert(XA_RESULT_SUCCESS == res);
        switch (domain) {
          case XA_DOMAINTYPE_VIDEO: {
            XAVideoStreamInformation videoInfo;
            res = (*caller)->QueryStreamInformation(caller, streamIndex, &videoInfo);
            assert(XA_RESULT_SUCCESS == res);
            LOGI("Found video size %u x %u, codec ID=%u, frameRate=%u, bitRate=%u, duration=%u ms",
                        videoInfo.width, videoInfo.height, videoInfo.codecId, videoInfo.frameRate,
                        videoInfo.bitRate, videoInfo.duration);
			OMXPlayer *ins	= (OMXPlayer *)pContext;
			ins->m_width	= videoInfo.width;
			ins->m_height	= videoInfo.height;

			ins->notifyListener_l(MEDIA_SET_VIDEO_SIZE, videoInfo.width, videoInfo.height);
			ins->notifyListener_l(MEDIA_PREPARED);
          } break;
		  case XA_DOMAINTYPE_AUDIO: {
			XAAudioStreamInformation audioInfo;
            res = (*caller)->QueryStreamInformation(caller, streamIndex, &audioInfo);
            assert(XA_RESULT_SUCCESS == res);
            LOGI("Found audio codec ID=%u, channels=%u, sampleRate=%u, bitRate=%u, lang=%s, duration=%u ms",
				audioInfo.codecId, audioInfo.channels,
				audioInfo.sampleRate, audioInfo.bitRate, 
				audioInfo.langCountry, audioInfo.duration);
          } break;
		  case XA_DOMAINTYPE_TIMEDTEXT: {
			XATimedTextStreamInformation subtitleInfo;
            res = (*caller)->QueryStreamInformation(caller, streamIndex, &subtitleInfo);
            assert(XA_RESULT_SUCCESS == res);
            LOGI("Found subtitle size %u x %u, codec ID=%u, lang=%s, duration=%u ms",
				subtitleInfo.width, subtitleInfo.height,
				subtitleInfo.langCountry, subtitleInfo.duration);
          } break;
          default:
            fprintf(stderr, "Unexpected domain %u\n", domain);
            break;
        }
      } break;
      default:
        fprintf(stderr, "Unexpected stream event ID %u\n", eventId);
        break;
    }
}

//////////////////////////////////////////////////////////////////
void OMXPlayer::OMXPrepareEvent::action(void *opaque, int64_t now_us)
{
	OMXPlayer *ins= (OMXPlayer *)opaque;
	if (ins)
		ins->onPrepareImpl();
}

// create the engine and output mix objects
static void createEngine()
{
    XAresult res;

    // create engine
    res = xaCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(XA_RESULT_SUCCESS == res);

    // realize the engine
    res = (*engineObject)->Realize(engineObject, XA_BOOLEAN_FALSE);
    assert(XA_RESULT_SUCCESS == res);

    // get the engine interface, which is needed in order to create other objects
    res = (*engineObject)->GetInterface(engineObject, XA_IID_ENGINE, &engineEngine);
    assert(XA_RESULT_SUCCESS == res);

    // create output mix
    res = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    assert(XA_RESULT_SUCCESS == res);

    // realize the output mix
    res = (*outputMixObject)->Realize(outputMixObject, XA_BOOLEAN_FALSE);
    assert(XA_RESULT_SUCCESS == res);

}

// AndroidBufferQueueItf callback to supply MPEG-2 TS packets to the media player
static XAresult AndroidBufferQueueCallback(
        XAAndroidBufferQueueItf caller,
        void *pCallbackContext,        /* input */
        void *pBufferContext,          /* input */
        void *pBufferData,             /* input */
        XAuint32 dataSize,             /* input */
        XAuint32 dataUsed,             /* input */
        const XAAndroidBufferItem *pItems,/* input */
        XAuint32 itemsLength           /* input */)
{
    XAresult res;
    int ok;

    // pCallbackContext was specified as NULL at RegisterCallback and is unused here
    assert(NULL == pCallbackContext);

    // note there is never any contention on this mutex unless a discontinuity request is active
    ok = pthread_mutex_lock(&mutex);
    assert(0 == ok);

    // was a discontinuity requested?
    if (discontinuity) {
        // Note: can't rewind after EOS, which we send when reaching EOF
        // (don't send EOS if you plan to play more content through the same player)
        if (!reachedEof) {
            // clear the buffer queue
            res = (*playerBQItf)->Clear(playerBQItf);
            assert(XA_RESULT_SUCCESS == res);
            // rewind the data source so we are guaranteed to be at an appropriate point
            rewind(file);
            // Enqueue the initial buffers, with a discontinuity indicator on first buffer
            (void) enqueueInitialBuffers(JNI_TRUE);
        }
        // acknowledge the discontinuity request
        discontinuity = JNI_FALSE;
        ok = pthread_cond_signal(&cond);
        assert(0 == ok);
        goto exit;
    }

    if ((pBufferData == NULL) && (pBufferContext != NULL)) {
        const int processedCommand = *(int *)pBufferContext;
        if (kEosBufferCntxt == processedCommand) {
            LOGV("EOS was processed\n");
            // our buffer with the EOS message has been consumed
            assert(0 == dataSize);
            goto exit;
        }
    }

    // pBufferData is a pointer to a buffer that we previously Enqueued
    assert((dataSize > 0) && ((dataSize % MPEG2_TS_PACKET_SIZE) == 0));
    assert(dataCache <= (char *) pBufferData && (char *) pBufferData <
            &dataCache[BUFFER_SIZE * NB_BUFFERS]);
    assert(0 == (((char *) pBufferData - dataCache) % BUFFER_SIZE));

    // don't bother trying to read more data once we've hit EOF
    if (reachedEof) {
        goto exit;
    }

    size_t nbRead;
    // note we do call fread from multiple threads, but never concurrently
    size_t bytesRead;
    bytesRead = fread(pBufferData, 1, BUFFER_SIZE, file);
    if (bytesRead > 0) {
        if ((bytesRead % MPEG2_TS_PACKET_SIZE) != 0) {
            LOGV("Dropping last packet because it is not whole");
        }
        size_t packetsRead = bytesRead / MPEG2_TS_PACKET_SIZE;
        size_t bufferSize = packetsRead * MPEG2_TS_PACKET_SIZE;
        res = (*caller)->Enqueue(caller, NULL /*pBufferContext*/,
                pBufferData /*pData*/,
                bufferSize /*dataLength*/,
                NULL /*pMsg*/,
                0 /*msgLength*/);
        assert(XA_RESULT_SUCCESS == res);
    } else {
        // EOF or I/O error, signal EOS
        XAAndroidBufferItem msgEos[1];
        msgEos[0].itemKey = XA_ANDROID_ITEMKEY_EOS;
        msgEos[0].itemSize = 0;
        // EOS message has no parameters, so the total size of the message is the size of the key
        //   plus the size if itemSize, both XAuint32
        res = (*caller)->Enqueue(caller, (void *)&kEosBufferCntxt /*pBufferContext*/,
                NULL /*pData*/, 0 /*dataLength*/,
                msgEos /*pMsg*/,
                sizeof(XAuint32)*2 /*msgLength*/);
        assert(XA_RESULT_SUCCESS == res);
        reachedEof = JNI_TRUE;
    }

exit:
    ok = pthread_mutex_unlock(&mutex);
    assert(0 == ok);
    return XA_RESULT_SUCCESS;
}

// Enqueue the initial buffers, and optionally signal a discontinuity in the first buffer
static jboolean enqueueInitialBuffers(jboolean discontinuity)
{

    /* Fill our cache.
     * We want to read whole packets (integral multiples of MPEG2_TS_PACKET_SIZE).
     * fread returns units of "elements" not bytes, so we ask for 1-byte elements
     * and then check that the number of elements is a multiple of the packet size.
     */
    size_t bytesRead;
    bytesRead = fread(dataCache, 1, BUFFER_SIZE * NB_BUFFERS, file);
    if (bytesRead <= 0) {
        // could be premature EOF or I/O error
        return JNI_FALSE;
    }
    if ((bytesRead % MPEG2_TS_PACKET_SIZE) != 0) {
        LOGV("Dropping last packet because it is not whole");
    }
    size_t packetsRead = bytesRead / MPEG2_TS_PACKET_SIZE;
    LOGV("Initially queueing %zu packets", packetsRead);

    /* Enqueue the content of our cache before starting to play,
       we don't want to starve the player */
    size_t i;
    for (i = 0; i < NB_BUFFERS && packetsRead > 0; i++) {
        // compute size of this buffer
        size_t packetsThisBuffer = packetsRead;
        if (packetsThisBuffer > PACKETS_PER_BUFFER) {
            packetsThisBuffer = PACKETS_PER_BUFFER;
        }
        size_t bufferSize = packetsThisBuffer * MPEG2_TS_PACKET_SIZE;
        XAresult res;
        if (discontinuity) {
            // signal discontinuity
            XAAndroidBufferItem items[1];
            items[0].itemKey = XA_ANDROID_ITEMKEY_DISCONTINUITY;
            items[0].itemSize = 0;
            // DISCONTINUITY message has no parameters,
            //   so the total size of the message is the size of the key
            //   plus the size if itemSize, both XAuint32
            res = (*playerBQItf)->Enqueue(playerBQItf, NULL /*pBufferContext*/,
                    dataCache + i*BUFFER_SIZE, bufferSize, items /*pMsg*/,
                    sizeof(XAuint32)*2 /*msgLength*/);
            discontinuity = JNI_FALSE;
        } else {
            res = (*playerBQItf)->Enqueue(playerBQItf, NULL /*pBufferContext*/,
                    dataCache + i*BUFFER_SIZE, bufferSize, NULL, 0);
        }
        assert(XA_RESULT_SUCCESS == res);
        packetsRead -= packetsThisBuffer;
    }

    return JNI_TRUE;
}

// set the playing state for the streaming media player
void OMXPlayer::setPlaying(bool isPlaying)
{
	LOGI("setPlaying() %d", isPlaying);

    XAresult res;

    // make sure the streaming media player was created
    if (NULL != playerPlayItf) {

        // set the player's state
        res = (*playerPlayItf)->SetPlayState(playerPlayItf, isPlaying ?
            XA_PLAYSTATE_PLAYING : XA_PLAYSTATE_PAUSED);
        assert(XA_RESULT_SUCCESS == res);

    }

}
