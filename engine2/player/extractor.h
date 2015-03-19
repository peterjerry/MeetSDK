#ifndef IEXTRACTOR_H
#define IEXTRACTOR_H

#if defined(_MSC_VER) || defined(__CYGWIN__)
#include "pperrors.h"
#else
#include "errors.h"
#endif

class MediaPlayerListener;

// player callback
enum pp_media_type {
	PPMEDIA_TYPE_UNKNOWN = -1,  ///< Usually treated as AVMEDIA_TYPE_DATA
    PPMEDIA_TYPE_VIDEO,
    PPMEDIA_TYPE_AUDIO,
	PPMEDIA_TYPE_DATA,          ///< Opaque data information usually continuous
	PPMEDIA_TYPE_SUBTITLE,
};

typedef struct MediaFormat
{
	MediaFormat(){}
	~MediaFormat() {
		if (csd_0) {
			delete csd_0;
			csd_0 = NULL;
		}
		if (csd_1) {
			delete csd_1;
			csd_1 = NULL;
		}
	}

	pp_media_type	media_type;
	int32_t			codec_id;
	int32_t			bitrate;
	int64_t			duration_us;
	// for video
	int32_t			width;
	int32_t			height;
	float			frame_rate;
	double			ar;
	uint8_t*		csd_0;
	uint32_t		csd_0_size;
	uint8_t*		csd_1;
	uint32_t		csd_1_size;
	// for audio
	int32_t			channels;
	uint64_t		channel_layout;
	int32_t			sample_rate;
	int32_t			sample_fmt;
}MediaFormat;

class IExtractor
{
public:
	virtual status_t setDataSource(const char *path) = 0;

    virtual status_t getTrackCount(int32_t *track_count) = 0;

    virtual status_t getTrackFormat(int32_t index, MediaFormat *format) = 0;

    virtual status_t selectTrack(int32_t index) = 0;

    virtual status_t unselectTrack(int32_t index) = 0;

    virtual status_t seekTo(int64_t timeUs, int mode) = 0;
    
    virtual status_t advance() = 0;

    virtual status_t readSampleData(unsigned char *data, int32_t *sampleSize) = 0;

    virtual status_t getSampleTrackIndex(int32_t *trackIndex) = 0;

    virtual status_t getSampleTime(int64_t *sampleTimeUs) = 0;

    virtual status_t getSampleFlags(uint32_t *sampleFlags) = 0;

    virtual status_t getCachedDuration(int64_t *durationUs, bool *eos) = 0;

	virtual status_t setListener(MediaPlayerListener* listener) = 0;

	virtual ~IExtractor() {}
};

extern "C" IExtractor* getExtractor();

extern "C" void releaseExtractor(IExtractor *extractor);

#endif
