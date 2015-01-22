#ifndef IPLAYER_H
#define IPLAYER_H

class ISubtitles;

#if defined(_MSC_VER) || defined(__CYGWIN__)
#include "pperrors.h"
#else
#include "errors.h"
#endif

#ifdef __APPLE__
#include <stdint.h> // fix ios uint8_t undefined error
#endif

#define MAX_CHANNEL_CNT 8
#define SAFE_DELETE(p)       { if(p) { delete (p);     (p)=NULL; } }

typedef struct MediaInfo {
	int32_t duration_ms; //in millisecond 
	long long size_byte; //in byte
	int32_t width;
	int32_t height;

	const char* format_name;
	//const char* audio_name;
	const char* videocodec_name;
	int32_t thumbnail_width;
	int32_t thumbnail_height;
	int32_t* thumbnail;
	int32_t audio_channels;
	int32_t video_channels;
	int32_t subtitle_channels;
	
	//we do not use dynamic mem alloc, for easy mem management.
	//use the ISO 639 language code (3 letters)
	//for detail, refer to http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
	//chinese language needs to map this logic:
	//	chi:chinese
	//	zho:chinese
	//	chs:simplified chinese
	//	cht:tranditional chinese
	int audio_streamIndexs[MAX_CHANNEL_CNT];
	char* audiocodec_names[MAX_CHANNEL_CNT];
	char* audio_languages[MAX_CHANNEL_CNT];
	char* audio_titles[MAX_CHANNEL_CNT];
	
	int subtitle_streamIndexs[MAX_CHANNEL_CNT];
	char* subtitlecodec_names[MAX_CHANNEL_CNT];
	char* subtitle_languages[MAX_CHANNEL_CNT];
	char* subtitle_titles[MAX_CHANNEL_CNT];

	//all channels count, include audio / video / subtitle
	int32_t channels;

    MediaInfo() :
        duration_ms(0),
        size_byte(0),
        width(0),
        height(0),

        format_name(NULL),
        videocodec_name(NULL),

        thumbnail_width(0),
        thumbnail_height(0),
        thumbnail(NULL),

        audio_channels(0),
        video_channels(0),
		subtitle_channels(0),

        channels(0) {
			for(int i=0 ; i<MAX_CHANNEL_CNT ; i++) {
				audio_streamIndexs[i]		= -1;
				audiocodec_names[i]			= NULL;
				audio_languages[i]			= NULL;
				audio_titles[i]				= NULL;

				subtitle_streamIndexs[i]	= -1;
				subtitlecodec_names[i]		= NULL;
				subtitle_languages[i]		= NULL;
				subtitle_titles[i]			= NULL;
			}

	}

	~MediaInfo() {
		for (int i=0 ; i<MAX_CHANNEL_CNT ; i++) {
			SAFE_DELETE(audiocodec_names[i]);
			SAFE_DELETE(audio_languages[i]);
			SAFE_DELETE(audio_titles[i]);

			SAFE_DELETE(subtitlecodec_names[i]);
			SAFE_DELETE(subtitle_languages[i]);
			SAFE_DELETE(subtitle_titles[i]);
		}
	}
    
} MediaInfo;

typedef struct SnapShot {
	int32_t		width;
	int32_t		height;
	int32_t		stride;
	int32_t		picture_fmt; // reserved, default is RGBX
	uint8_t*	picture_data;
} SnapShot;

typedef struct MediaFormat {
	// for video
	int32_t		width;
	int32_t		height;
	int32_t		pixel_fmt;
	int32_t		frame_rate;
	// for audio
	int32_t		channels;
	int32_t		sample_rate;
	int32_t		sample_fmt;
} MediaFormat;

typedef struct TrackInfo {
	MediaFormat	fmt;
	int32_t		type;
	char*		language;
} TrackInfo;

// player callback
enum media_event_type {
    MEDIA_NOP               = 0, // interface test message
    MEDIA_PREPARED          = 1,
    MEDIA_PLAYBACK_COMPLETE = 2,
    MEDIA_BUFFERING_UPDATE  = 3,
    MEDIA_SEEK_COMPLETE     = 4,
    MEDIA_SET_VIDEO_SIZE    = 5,
    MEDIA_ERROR             = 100,
    MEDIA_INFO              = 200,
    MEDIA_COMPATIBILITY_TEST_COMPLETE              = 300,
};

// player callback info detail
enum media_info_type {
    MEDIA_INFO_UNKNOWN				= 1,
    MEDIA_INFO_VIDEO_TRACK_LAGGING	= 700,
    MEDIA_INFO_BUFFERING_START		= 701,
    MEDIA_INFO_BUFFERING_END		= 702,
    MEDIA_INFO_NETWORK_BANDWIDTH	= 703,
    
    MEDIA_INFO_BAD_INTERLEAVING		= 800,
    MEDIA_INFO_NOT_SEEKABLE			= 801,
    MEDIA_INFO_METADATA_UPDATE		= 802,

	MEDIA_INFO_TEST_DECODE_AVG_MSEC	= 901,
	MEDIA_INFO_TEST_RENDER_AVG_MSEC	= 902,
	MEDIA_INFO_TEST_DECODE_FPS		= 903,
	MEDIA_INFO_TEST_RENDER_FPS		= 904, 
	MEDIA_INFO_TEST_RENDER_FRAME	= 905, // total render frames
	MEDIA_INFO_TEST_LATENCY_MSEC	= 906, // audio_clock - video_clock
	MEDIA_INFO_TEST_DROP_FRAME		= 907, // this frame is drop
	MEDIA_INFO_TEST_IO_BITRATE		= 921, // read from disk or network(maybe zero) kbps
	MEDIA_INFO_TEST_MEDIA_BITRATE	= 922, // media bitrate kbps

	MEDIA_INFO_TEST_PLAYER_TYPE		= 911,
};

enum PLAYER_IMPL_TYPE {
	SYSTEM_PLAYER = 10001,
	NU_PLAYER,
	FF_PLAYER,
	PP_PLAYER,
};

enum {
	MEDIA_TRACK_TYPE_AUDIO		= 0x00000002, // api 16
	MEDIA_TRACK_TYPE_SUBTITLE	= 0x00000004, // api 21
	MEDIA_TRACK_TYPE_TIMEDTEXT	= 0x00000003, // api 16
	MEDIA_TRACK_TYPE_UNKNOWN	= 0x00000000, // api 16
	MEDIA_TRACK_TYPE_VIDEO		= 0x00000001, // api 16
};

// player callback error detail
enum media_error_type {
    MEDIA_ERROR_UNKNOWN 							= 1,
    MEDIA_ERROR_SERVER_DIED 						= 100,
    MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK 	= 200,
	MEDIA_ERROR_FAIL_TO_READ_PACKET					= 301,
	MEDIA_ERROR_FAIL_TO_OPEN						= 303,
	MEDIA_ERROR_FAIL_TO_SEEK						= 304,

	MEDIA_ERROR_AUDIO_PLAYER						= 311,
	MEDIA_ERROR_AUDIO_RENDER						= 312,
	MEDIA_ERROR_VIDEO_PLAYER						= 313,
	MEDIA_ERROR_VIDEO_RENDER						= 314,
	MEDIA_ERROR_DEMUXER								= 315,

	MEDIA_ERROR_TIMED_OUT							= -110,
	MEDIA_ERROR_IO									= -1004,
	MEDIA_ERROR_MALFORMED							= -1007,
	MEDIA_ERROR_UNSUPPORTED							= -1010,
};

enum media_player_states {
    MEDIA_PLAYER_STATE_ERROR        = 0,
    MEDIA_PLAYER_IDLE               = 1 << 0,
    MEDIA_PLAYER_INITIALIZED        = 1 << 1,
    MEDIA_PLAYER_PREPARING          = 1 << 2,
    MEDIA_PLAYER_PREPARED           = 1 << 3,
    MEDIA_PLAYER_STARTED            = 1 << 4,
    MEDIA_PLAYER_PAUSED             = 1 << 5,
    MEDIA_PLAYER_STOPPED            = 1 << 6,
    MEDIA_PLAYER_PLAYBACK_COMPLETE	= 1 << 7,
    MEDIA_PLAYER_STOPPING			= 1 << 8
};

class MediaPlayerListener
{
public:
    virtual void notify(int msg, int ext1, int ext2){}
	virtual ~MediaPlayerListener(){}
};

class IPlayer
{
public:
	// init
	virtual status_t setDataSource(const char *url) = 0;
	virtual status_t setVideoSurface(void* surface) = 0;
	virtual status_t prepare() = 0;
	virtual status_t prepareAsync() = 0;

	// controll
	virtual status_t start() = 0;
	virtual status_t stop() = 0;
	virtual status_t pause() = 0;
	virtual status_t seekTo(int msec) = 0;
	virtual status_t selectAudioChannel(int32_t index){return -1;} 
	virtual	status_t setISubtitle(ISubtitles* subtitle){return -1;}

	// listener
	virtual status_t setListener(MediaPlayerListener* listener) = 0;

	// query
	virtual status_t	getVideoWidth(int *w) = 0;
	virtual status_t	getVideoHeight(int *h) = 0;
	virtual status_t	getCurrentPosition(int *msec) = 0;
	virtual status_t	getDuration(int *msec) = 0;
	virtual status_t	getProcessBytes(int64_t *len) = 0;
	virtual bool		getMediaInfo(const char* url, MediaInfo* info){return false;} 
	virtual bool		getMediaDetailInfo(const char* url, MediaInfo* info){return false;}
	virtual bool		getThumbnail(const char* url, MediaInfo* info){return false;}
	virtual SnapShot*	getSnapShot(int width, int height, int fmt, int msec = -1){return NULL;}
	virtual bool getTrackInfo(TrackInfo* info, int *max_num){return false;}
	virtual int			flags() = 0;
	
	virtual ~IPlayer() {}

	// rare used
	
#ifdef __ANDROID__
	virtual status_t startCompatibilityTest() = 0;
	virtual void stopCompatibilityTest() = 0;
	virtual status_t getBufferingTime(int *msec) = 0;
#endif
	virtual status_t startP2PEngine() = 0;
	virtual void stopP2PEngine() = 0;
	virtual status_t reset() = 0;
	virtual status_t setAudioStreamType(int type) = 0;
	virtual status_t setLooping(int loop) = 0;
	virtual bool isLooping() = 0;
	virtual status_t setVolume(float leftVolume, float rightVolume) = 0;
	virtual bool isPlaying() = 0;
	virtual status_t setDataSource(int fd, int64_t offset, int64_t length) = 0;
	virtual void notify(int msg, int ext1, int ext2) = 0;
	virtual void disconnect() = 0;
	virtual status_t suspend() = 0;
	virtual status_t resume() = 0;
	//virtual status_t setPlayRate(double rate) = 0;
};

extern "C" IPlayer* getPlayer(void* context);

#endif
