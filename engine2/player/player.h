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
#include <stdlib.h> // for free

#define MAX_CHANNEL_CNT				64
#define DEFAULT_THUMBNAIL_WIDTH		96
#define DEFAULT_THUMBNAIL_HEIGHT	96

#define SAFE_DELETE(p)       { if(p) { delete (p);     (p)=NULL; } }
#define SAFE_FREE(p)       { if(p) { free (p);     (p)=NULL; } }

typedef struct DictEntry {
    char *key;
    char *value;
	DictEntry *next;
} DictEntry;

typedef struct MediaInfo {
	char* format_name;
	int32_t duration_ms; //in millisecond 
	long long size_byte; //in byte
	int32_t width;
	int32_t height;
	double frame_rate;
	int32_t	bitrate;
	DictEntry *meta_data;

	char* videocodec_name;
	char* videocodec_profile;
	DictEntry *video_meta_data;
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
	char* audiocodec_profiles[MAX_CHANNEL_CNT];
	char* audio_languages[MAX_CHANNEL_CNT];
	char* audio_titles[MAX_CHANNEL_CNT];
	
	int subtitle_streamIndexs[MAX_CHANNEL_CNT];
	char* subtitlecodec_names[MAX_CHANNEL_CNT];
	char* subtitle_languages[MAX_CHANNEL_CNT];
	char* subtitle_titles[MAX_CHANNEL_CNT];

	//all channels count, include audio / video / subtitle
	int32_t channels;

    MediaInfo() :
		format_name(NULL),
        duration_ms(0),
        size_byte(0),
        width(0),
        height(0),
		frame_rate(0.0),
		bitrate(0),
		meta_data(NULL),

        videocodec_name(NULL),
		videocodec_profile(NULL),
		video_meta_data(NULL),

        thumbnail_width(0),
        thumbnail_height(0),
        thumbnail(NULL),

        audio_channels(0),
        video_channels(0),
		subtitle_channels(0),

        channels(0) {
			for (int i=0 ; i<MAX_CHANNEL_CNT ; i++) {
				audio_streamIndexs[i]		= -1;
				audiocodec_names[i]			= NULL;
				audiocodec_profiles[i]		= NULL;
				audio_languages[i]			= NULL;
				audio_titles[i]				= NULL;

				subtitle_streamIndexs[i]	= -1;
				subtitlecodec_names[i]		= NULL;
				subtitle_languages[i]		= NULL;
				subtitle_titles[i]			= NULL;
			}
	}

	~MediaInfo() {
		SAFE_FREE(format_name);
		free_entry_list(meta_data);
		meta_data = NULL;

		SAFE_FREE(videocodec_name);
		SAFE_FREE(videocodec_profile);
		free_entry_list(video_meta_data);
		video_meta_data = NULL;

		for (int i=0 ; i<MAX_CHANNEL_CNT ; i++) {
			SAFE_FREE(audiocodec_names[i]);
			SAFE_FREE(audiocodec_profiles[i]);
			SAFE_FREE(audio_languages[i]);
			SAFE_FREE(audio_titles[i]);

			SAFE_FREE(subtitlecodec_names[i]);
			SAFE_FREE(subtitle_languages[i]);
			SAFE_FREE(subtitle_titles[i]);
		}

		SAFE_FREE(thumbnail);
	}

	void free_entry_list(DictEntry *list) {
		if (!list)
			return;

		DictEntry *p = list;
		while (p) {
			DictEntry *temp = p->next;
			if (p->key)
				free(p->key);
			if (p->value)
				free(p->value);
			delete p;
			p = temp;
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
	MEDIA_INFO_STARTED_AS_NEXT		= 2,
	MEDIA_INFO_VIDEO_RENDERING_START= 3,

    MEDIA_INFO_VIDEO_TRACK_LAGGING	= 700,
    MEDIA_INFO_BUFFERING_START		= 701,
    MEDIA_INFO_BUFFERING_END		= 702,
    MEDIA_INFO_NETWORK_BANDWIDTH	= 703,
    
    MEDIA_INFO_BAD_INTERLEAVING		= 800,
    MEDIA_INFO_NOT_SEEKABLE			= 801,
    MEDIA_INFO_METADATA_UPDATE		= 802,

	MEDIA_INFO_TIMED_TEXT_ERROR		= 900,
	MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901,
	MEDIA_INFO_SUBTITLE_TIMED_OUT	= 902,

	MEDIA_INFO_TEST_DECODE_AVG_MSEC	= 2001,
	MEDIA_INFO_TEST_RENDER_AVG_MSEC	= 2002,
	MEDIA_INFO_TEST_DECODE_FPS		= 2003,
	MEDIA_INFO_TEST_RENDER_FPS		= 2004, 
	MEDIA_INFO_TEST_RENDER_FRAME	= 2005, // total render frames
	MEDIA_INFO_TEST_LATENCY_MSEC	= 2006, // video_clock - ref_clock
	MEDIA_INFO_TEST_DROP_FRAME		= 2007, // this frame is drop
	MEDIA_INFO_TEST_BUFFERING_MSEC	= 2008, // msec
	MEDIA_INFO_TEST_IO_BITRATE		= 2021, // read from disk or network(maybe zero) kbps
	MEDIA_INFO_TEST_MEDIA_BITRATE	= 2022, // media bitrate kbps
	MEDIA_INFO_TEST_RTMP_FLUSH		= 2030, // rtmp flush buffer to keep real-time

	MEDIA_INFO_TEST_PLAYER_TYPE		= 2100,
};

enum PLAYER_IMPL_TYPE {
	SYSTEM_PLAYER = 10001,
	XO_PLAYER,
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

	MEDIA_ERROR_AUDIO_DECODER						= 321,
	MEDIA_ERROR_VIDEO_DECODER						= 322,
	MEDIA_ERROR_UNSUPPORTED_AUDIO					= 331,
	MEDIA_ERROR_UNSUPPORTED_VIDEO					= 332,

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

	virtual status_t selectSubtitleChannel(int32_t index){return -1;} 

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

	virtual bool		getCurrentMediaInfo(MediaInfo* info){return false;}

	virtual bool		getThumbnail(const char* url, MediaInfo* info, 
		int width = DEFAULT_THUMBNAIL_WIDTH, int height = DEFAULT_THUMBNAIL_HEIGHT){return false;}

	virtual SnapShot*	getSnapShot(int width, int height, int fmt, int msec = -1){return NULL;}

	virtual int			flags() = 0;
	
	virtual ~IPlayer() {}

	// rare used
	
#ifdef __ANDROID__
	virtual status_t startCompatibilityTest() = 0;
	virtual void stopCompatibilityTest() = 0;
#endif

	virtual status_t getBufferingTime(int *msec) = 0;

	virtual status_t reset() = 0;

	virtual status_t setAudioStreamType(int type) = 0;

	virtual status_t setLooping(int loop) = 0;

	virtual bool isLooping() = 0;

	virtual status_t setVolume(float leftVolume, float rightVolume) = 0;

	virtual bool isPlaying() = 0;

	virtual status_t setDataSource(int fd, int64_t offset, int64_t length) = 0;

	virtual void notify(int msg, int ext1, int ext2) = 0;

	virtual status_t suspend() = 0;

	virtual status_t resume() = 0;

	virtual void set_opt(const char *opt) = 0;

	//virtual status_t setPlayRate(double rate) = 0;
};

extern "C" IPlayer* getPlayer(void* context);

extern "C" void releasePlayer(IPlayer *player);

extern "C" bool my_convert(uint8_t* flv_data, int flv_data_size, uint8_t* ts_data, int *out_size, int process_timestamp, int first_seg);

#endif
