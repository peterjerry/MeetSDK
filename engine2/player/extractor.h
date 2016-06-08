#ifndef IEXTRACTOR_H
#define IEXTRACTOR_H

#if defined(_MSC_VER) || defined(__CYGWIN__)
#include "pperrors.h"
#else
#include "errors.h"
#endif

#define SEEK_TO_CLOSEST_SYNC		2 // If possible, seek to the sync sample closest to the specified time
#define SEEK_TO_NEXT_SYNC			1 // If possible, seek to a sync sample at or after the specified time
#define SEEK_TO_PREVIOUS_SYNC		0 // If possible, seek to a sync sample at or before the specified time (0x00000000)

#define BUFFER_FLAG_SYNC_FRAME		1
#define BUFFER_FLAG_CODEC_CONFIG	2
#define BUFFER_FLAG_END_OF_STREAM	4

class MediaPlayerListener;
class ISubtitles;

#define MKTAG(a,b,c,d) ((a) | ((b) << 8) | ((c) << 16) | ((unsigned)(d) << 24))
#define MKBETAG(a,b,c,d) ((d) | ((c) << 8) | ((b) << 16) | ((unsigned)(a) << 24))

// player callback
enum pp_media_type {
	PPMEDIA_TYPE_UNKNOWN = -1,  ///< Usually treated as AVMEDIA_TYPE_DATA
    PPMEDIA_TYPE_VIDEO,
    PPMEDIA_TYPE_AUDIO,
	PPMEDIA_TYPE_DATA,          ///< Opaque data information usually continuous
	PPMEDIA_TYPE_SUBTITLE,
};

enum pp_media_coded_id {
	PPMEDIA_CODEC_ID_NONE,

	/* video codecs */
    PPMEDIA_CODEC_ID_MPEG1VIDEO,
    PPMEDIA_CODEC_ID_MPEG2VIDEO, ///< preferred ID for MPEG-1/2 video decoding

    PPMEDIA_CODEC_ID_MPEG2VIDEO_XVMC,

    PPMEDIA_CODEC_ID_H261,
    PPMEDIA_CODEC_ID_H263,
    PPMEDIA_CODEC_ID_RV10,
    PPMEDIA_CODEC_ID_RV20,
    PPMEDIA_CODEC_ID_MJPEG,
    PPMEDIA_CODEC_ID_MJPEGB,
    PPMEDIA_CODEC_ID_LJPEG,
    PPMEDIA_CODEC_ID_SP5X,
    PPMEDIA_CODEC_ID_JPEGLS,
    PPMEDIA_CODEC_ID_MPEG4,
    PPMEDIA_CODEC_ID_RAWVIDEO,
    PPMEDIA_CODEC_ID_MSMPEG4V1,
    PPMEDIA_CODEC_ID_MSMPEG4V2,
    PPMEDIA_CODEC_ID_MSMPEG4V3,
    PPMEDIA_CODEC_ID_WMV1,
    PPMEDIA_CODEC_ID_WMV2,
    PPMEDIA_CODEC_ID_H263P,
    PPMEDIA_CODEC_ID_H263I,
    PPMEDIA_CODEC_ID_FLV1,
  
    PPMEDIA_CODEC_ID_H264 = 28,
    
	PPMEDIA_CODEC_ID_VP8 = 141,
	PPMEDIA_CODEC_ID_VP9 = 169,
	
	PPMEDIA_CODEC_ID_HEVC_HM91 = 172 + 1000,
    PPMEDIA_CODEC_ID_HEVC_HM10,
    PPMEDIA_CODEC_ID_HEVC_STRONGNE,
	
	PPMEDIA_CODEC_ID_HEVC = MKBETAG('H','2','6','5'),

	 /* various PCM "codecs" */
    PPMEDIA_CODEC_ID_FIRST_AUDIO = 0x10000,     ///< A dummy id pointing at the start of audio codecs
    PPMEDIA_CODEC_ID_PCM_S16LE = 0x10000,
    PPMEDIA_CODEC_ID_PCM_S16BE,
    PPMEDIA_CODEC_ID_PCM_U16LE,
    PPMEDIA_CODEC_ID_PCM_U16BE,
    PPMEDIA_CODEC_ID_PCM_S8,
    PPMEDIA_CODEC_ID_PCM_U8,
    PPMEDIA_CODEC_ID_PCM_MULAW,
    PPMEDIA_CODEC_ID_PCM_ALAW,
    PPMEDIA_CODEC_ID_PCM_S32LE,
    PPMEDIA_CODEC_ID_PCM_S32BE,
    PPMEDIA_CODEC_ID_PCM_U32LE,
    PPMEDIA_CODEC_ID_PCM_U32BE,
    PPMEDIA_CODEC_ID_PCM_S24LE,
    PPMEDIA_CODEC_ID_PCM_S24BE,
    PPMEDIA_CODEC_ID_PCM_U24LE,
    PPMEDIA_CODEC_ID_PCM_U24BE,
    PPMEDIA_CODEC_ID_PCM_S24DAUD,
    PPMEDIA_CODEC_ID_PCM_ZORK,
    PPMEDIA_CODEC_ID_PCM_S16LE_PLANAR,
    PPMEDIA_CODEC_ID_PCM_DVD,
    PPMEDIA_CODEC_ID_PCM_F32BE,
    PPMEDIA_CODEC_ID_PCM_F32LE,
    PPMEDIA_CODEC_ID_PCM_F64BE,
    PPMEDIA_CODEC_ID_PCM_F64LE,
    PPMEDIA_CODEC_ID_PCM_BLURAY,
    PPMEDIA_CODEC_ID_PCM_LXF,
    PPMEDIA_CODEC_ID_S302M,
    PPMEDIA_CODEC_ID_PCM_S8_PLANAR,
    PPMEDIA_CODEC_ID_PCM_S24LE_PLANAR_DEPRECATED,
    PPMEDIA_CODEC_ID_PCM_S32LE_PLANAR_DEPRECATED,
    PPMEDIA_CODEC_ID_PCM_S24LE_PLANAR = MKBETAG(24,'P','S','P'),
    PPMEDIA_CODEC_ID_PCM_S32LE_PLANAR = MKBETAG(32,'P','S','P'),
    PPMEDIA_CODEC_ID_PCM_S16BE_PLANAR = MKBETAG('P','S','P',16),

    /* various ADPCM codecs */
    PPMEDIA_CODEC_ID_ADPCM_IMA_QT = 0x11000,
    PPMEDIA_CODEC_ID_ADPCM_IMA_WAV,
    PPMEDIA_CODEC_ID_ADPCM_IMA_DK3,
    PPMEDIA_CODEC_ID_ADPCM_IMA_DK4,
    PPMEDIA_CODEC_ID_ADPCM_IMA_WS,
    PPMEDIA_CODEC_ID_ADPCM_IMA_SMJPEG,
    PPMEDIA_CODEC_ID_ADPCM_MS,
    PPMEDIA_CODEC_ID_ADPCM_4XM,
    PPMEDIA_CODEC_ID_ADPCM_XA,
    PPMEDIA_CODEC_ID_ADPCM_ADX,
    PPMEDIA_CODEC_ID_ADPCM_EA,
    PPMEDIA_CODEC_ID_ADPCM_G726,
    PPMEDIA_CODEC_ID_ADPCM_CT,
    PPMEDIA_CODEC_ID_ADPCM_SWF,
    PPMEDIA_CODEC_ID_ADPCM_YAMAHA,
    PPMEDIA_CODEC_ID_ADPCM_SBPRO_4,
    PPMEDIA_CODEC_ID_ADPCM_SBPRO_3,
    PPMEDIA_CODEC_ID_ADPCM_SBPRO_2,
    PPMEDIA_CODEC_ID_ADPCM_THP,
    PPMEDIA_CODEC_ID_ADPCM_IMA_AMV,
    PPMEDIA_CODEC_ID_ADPCM_EA_R1,
    PPMEDIA_CODEC_ID_ADPCM_EA_R3,
    PPMEDIA_CODEC_ID_ADPCM_EA_R2,
    PPMEDIA_CODEC_ID_ADPCM_IMA_EA_SEAD,
    PPMEDIA_CODEC_ID_ADPCM_IMA_EA_EACS,
    PPMEDIA_CODEC_ID_ADPCM_EA_XAS,
    PPMEDIA_CODEC_ID_ADPCM_EA_MAXIS_XA,
    PPMEDIA_CODEC_ID_ADPCM_IMA_ISS,
    PPMEDIA_CODEC_ID_ADPCM_G722,
    PPMEDIA_CODEC_ID_ADPCM_IMA_APC,
    PPMEDIA_CODEC_ID_ADPCM_VIMA_DEPRECATED,
    PPMEDIA_CODEC_ID_ADPCM_VIMA = MKBETAG('V','I','M','A'),
    PPMEDIA_CODEC_ID_VIMA       = MKBETAG('V','I','M','A'),
    PPMEDIA_CODEC_ID_ADPCM_AFC  = MKBETAG('A','F','C',' '),
    PPMEDIA_CODEC_ID_ADPCM_IMA_OKI = MKBETAG('O','K','I',' '),
    PPMEDIA_CODEC_ID_ADPCM_DTK  = MKBETAG('D','T','K',' '),
    PPMEDIA_CODEC_ID_ADPCM_IMA_RAD = MKBETAG('R','A','D',' '),
    PPMEDIA_CODEC_ID_ADPCM_G726LE = MKBETAG('6','2','7','G'),
 
 /* AMR */
    PPMEDIA_CODEC_ID_AMR_NB = 0x12000,
    PPMEDIA_CODEC_ID_AMR_WB,

    /* RealAudio codecs*/
    PPMEDIA_CODEC_ID_RA_144 = 0x13000,
    PPMEDIA_CODEC_ID_RA_288,

    /* various DPCM codecs */
    PPMEDIA_CODEC_ID_ROQ_DPCM = 0x14000,
    PPMEDIA_CODEC_ID_INTERPLAY_DPCM,
    PPMEDIA_CODEC_ID_XAN_DPCM,
    PPMEDIA_CODEC_ID_SOL_DPCM,   

   /* audio codecs */
    PPMEDIA_CODEC_ID_MP2 = 0x15000,
    PPMEDIA_CODEC_ID_MP3, ///< preferred ID for decoding MPEG audio layer 1, 2 or 3
    PPMEDIA_CODEC_ID_AAC,
    PPMEDIA_CODEC_ID_AC3,
    PPMEDIA_CODEC_ID_DTS,
    PPMEDIA_CODEC_ID_VORBIS,
    PPMEDIA_CODEC_ID_DVAUDIO,
    PPMEDIA_CODEC_ID_WMAV1,
    PPMEDIA_CODEC_ID_WMAV2,
    PPMEDIA_CODEC_ID_MACE3,
    PPMEDIA_CODEC_ID_MACE6,
    PPMEDIA_CODEC_ID_VMDAUDIO,
    PPMEDIA_CODEC_ID_FLAC,
    PPMEDIA_CODEC_ID_MP3ADU,
    PPMEDIA_CODEC_ID_MP3ON4,
    PPMEDIA_CODEC_ID_SHORTEN,
    PPMEDIA_CODEC_ID_ALAC,
    PPMEDIA_CODEC_ID_WESTWOOD_SND1,
    PPMEDIA_CODEC_ID_GSM, ///< as in Berlin toast format
    PPMEDIA_CODEC_ID_QDM2,
    PPMEDIA_CODEC_ID_COOK,
    PPMEDIA_CODEC_ID_TRUESPEECH,
    PPMEDIA_CODEC_ID_TTA,
    PPMEDIA_CODEC_ID_SMACKAUDIO,
    PPMEDIA_CODEC_ID_QCELP,
    PPMEDIA_CODEC_ID_WAVPACK,
    PPMEDIA_CODEC_ID_DSICINAUDIO,
    PPMEDIA_CODEC_ID_IMC,
    PPMEDIA_CODEC_ID_MUSEPACK7,
    PPMEDIA_CODEC_ID_MLP,
    PPMEDIA_CODEC_ID_GSM_MS, /* as found in WAV */
    PPMEDIA_CODEC_ID_ATRAC3,

    PPMEDIA_CODEC_ID_VOXWARE,

    PPMEDIA_CODEC_ID_APE,
    PPMEDIA_CODEC_ID_NELLYMOSER,
    PPMEDIA_CODEC_ID_MUSEPACK8,
    PPMEDIA_CODEC_ID_SPEEX,
    PPMEDIA_CODEC_ID_WMAVOICE,
    PPMEDIA_CODEC_ID_WMAPRO,
    PPMEDIA_CODEC_ID_WMALOSSLESS,
    PPMEDIA_CODEC_ID_ATRAC3P,
    PPMEDIA_CODEC_ID_EAC3,

	/* subtitle codecs */
    PPMEDIA_CODEC_ID_FIRST_SUBTITLE = 0x17000,          ///< A dummy ID pointing at the start of subtitle codecs.
    PPMEDIA_CODEC_ID_DVD_SUBTITLE = 0x17000,
    PPMEDIA_CODEC_ID_DVB_SUBTITLE,
    PPMEDIA_CODEC_ID_TEXT,  ///< raw UTF-8 text
    PPMEDIA_CODEC_ID_XSUB,
    PPMEDIA_CODEC_ID_SSA,
    PPMEDIA_CODEC_ID_MOV_TEXT,
    PPMEDIA_CODEC_ID_HDMV_PGS_SUBTITLE,
    PPMEDIA_CODEC_ID_DVB_TELETEXT,
    PPMEDIA_CODEC_ID_SRT,
    PPMEDIA_CODEC_ID_MICRODVD   = MKBETAG('m','D','V','D'),
    PPMEDIA_CODEC_ID_EIA_608    = MKBETAG('c','6','0','8'),
    PPMEDIA_CODEC_ID_JACOSUB    = MKBETAG('J','S','U','B'),
    PPMEDIA_CODEC_ID_SAMI       = MKBETAG('S','A','M','I'),
    PPMEDIA_CODEC_ID_REALTEXT   = MKBETAG('R','T','X','T'),
    PPMEDIA_CODEC_ID_SUBVIEWER1 = MKBETAG('S','b','V','1'),
    PPMEDIA_CODEC_ID_SUBVIEWER  = MKBETAG('S','u','b','V'),
    PPMEDIA_CODEC_ID_SUBRIP     = MKBETAG('S','R','i','p'),
    PPMEDIA_CODEC_ID_WEBVTT     = MKBETAG('W','V','T','T'),
    PPMEDIA_CODEC_ID_MPL2       = MKBETAG('M','P','L','2'),
    PPMEDIA_CODEC_ID_VPLAYER    = MKBETAG('V','P','l','r'),
    PPMEDIA_CODEC_ID_PJS        = MKBETAG('P','h','J','S'),
    PPMEDIA_CODEC_ID_ASS        = MKBETAG('A','S','S',' '),  ///< ASS as defined in Matroska

    /* other specific kind of codecs (generally used for attachments) */
    PPMEDIA_CODEC_ID_FIRST_UNKNOWN = 0x18000,           ///< A dummy ID pointing at the start of various fake codecs.
    PPMEDIA_CODEC_ID_TTF = 0x18000,
    PPMEDIA_CODEC_ID_BINTEXT    = MKBETAG('B','T','X','T'),
    PPMEDIA_CODEC_ID_XBIN       = MKBETAG('X','B','I','N'),
    PPMEDIA_CODEC_ID_IDF        = MKBETAG( 0 ,'I','D','F'),
    PPMEDIA_CODEC_ID_OTF        = MKBETAG( 0 ,'O','T','F'),
    PPMEDIA_CODEC_ID_SMPTE_KLV  = MKBETAG('K','L','V','A'),
    PPMEDIA_CODEC_ID_DVD_NAV    = MKBETAG('D','N','A','V'),
    PPMEDIA_CODEC_ID_TIMED_ID3  = MKBETAG('T','I','D','3'),
    PPMEDIA_CODEC_ID_BIN_DATA   = MKBETAG('D','A','T','A'),
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

	virtual status_t stop() = 0;

	virtual status_t setVideoAhead(int32_t msec) = 0;

	virtual	status_t setISubtitle(ISubtitles* subtitle){return -1;}

	virtual status_t readPacket(int stream_index, unsigned char *data, int32_t *sampleSize) = 0;

	virtual status_t getBitrate(int32_t *kbps) = 0;

	virtual status_t decodeAudio(uint8_t *inbuf, int32_t size, uint8_t *out_pcm, int32_t *out_size) = 0;

	virtual ~IExtractor() {}
};

extern "C" IExtractor* getExtractor(void *context);

extern "C" void releaseExtractor(IExtractor *extractor);

#endif
