#ifndef FF_EXTRACTOR_H_

#define FF_EXTRACTOR_H_

#include "extractor.h"
#include "pthread.h"
#include "packetqueue.h"

struct AVFormatContext;
struct AVStream;
struct AVCodecContext;
struct AVFrame;
struct AVBitStreamFilterContext;
struct AVSubtitle;
struct SwrContext;

class MediaPlayerListener;

#define FFEXTRACTOR_INITED		1
#define FFEXTRACTOR_PREPARING	2
#define FFEXTRACTOR_PREPARED	3
#define FFEXTRACTOR_STARTED		4
#define FFEXTRACTOR_PAUSED		5
#define FFEXTRACTOR_STOPPING	6
#define FFEXTRACTOR_STOPPED		7

class FFExtractor : public IExtractor
{
public:
	FFExtractor();
	~FFExtractor();

	status_t setDataSource(const char *path);

    status_t getTrackCount(int32_t *track_count);

    status_t getTrackFormat(int32_t index, MediaFormat *format);

    status_t selectTrack(int32_t index);

    status_t unselectTrack(int32_t index);

    status_t seekTo(int64_t timeUs, int mode);
    
    status_t advance();

    status_t readSampleData(unsigned char *data, int32_t *sampleSize);

	status_t getSampleTrackIndex(int32_t *trackIndex);

    status_t getSampleTime(int64_t *sampleTimeUs);

    status_t getSampleFlags(uint32_t *sampleFlags);

    status_t getCachedDuration(int64_t *durationUs, bool *eos);

	status_t setListener(MediaPlayerListener* listener);

	status_t stop();

	status_t setVideoAhead(int32_t msec);

	status_t setISubtitle(ISubtitles* subtitle);

	status_t readPacket(int stream_index, unsigned char *data, int32_t *sampleSize);

	status_t getBitrate(int32_t *kbps);

	status_t decodeAudio(uint8_t *inbuf, int32_t size, uint8_t *out_pcm, int32_t *out_size);

private:
	int open_codec_context(int *stream_idx, int media_type);

	int open_codec_context_idx(int stream_idx);

	int start(int fill_pkt);

	void close();

	bool is_packet_valid();

	static void* demux_thread(void* ptr);

	void thread_impl();

	int64_t get_packet_duration(AVPacket *pPacket);

	int64_t get_packet_pos(AVPacket *pPacket);

	void addADTStoPacket(uint8_t *packet, int packetLen);

	bool seek_l();

	void flush_l();

	void find_sps_pps(AVPacket *pPacket);

	int16_t get_aac_extradata(AVCodecContext *c);

	void notifyListener_l(int msg, int ext1 = 0, int ext2 = 0);

	static int interrupt_l(void* ctx);

	bool open_subtitle_codec();

	bool init_swr();

private:

enum DATASOURCE_TYPE
{
	TYPE_LOCAL_FILE,
	TYPE_VOD,
	TYPE_LIVE,
	TYPE_UNKNOWN
};

	MediaPlayerListener*	mListener;

	int32_t				m_status;

	int					m_sorce_type;
	AVFormatContext*	m_fmt_ctx;
	char*				m_url;
	AVStream *			m_video_stream;
	int					m_video_stream_idx;
	AVCodecContext*		m_video_dec_ctx;
	int					m_framerate;
	int64_t				m_video_clock_msec; // sec
	AVBitStreamFilterContext*	m_pBsfc_h264;
	bool				m_nalu_convert;
	bool				m_video_keyframe_sync;
	uint8_t *			m_sps_data;
	uint32_t			m_sps_size;
	uint8_t *			m_pps_data;
	uint32_t			m_pps_size;
	int32_t				m_video_ahead_msec;

	AVStream *			m_audio_stream;
	int					m_audio_stream_idx;
	AVCodecContext*		m_audio_dec_ctx;
	int64_t				m_audio_clock_msec; // sec
	AVBitStreamFilterContext*	m_pBsfc_aac;

	int64_t				m_video_frame_count;
	int64_t				m_audio_frame_count;

	int					m_sample_track_idx;
	int64_t				m_sample_clock_msec;
	AVPacket*			m_sample_pkt;

	pthread_t			mThread;
    pthread_cond_t		mCondition;
    pthread_mutex_t		mLock;
	pthread_mutex_t		mLockNotify;

	// 20151226 michael.ma change from uint32_t to int32_t
	// unsigned type will cause nagative treated as very big value
	// < 0 contidtion is always false!
	int32_t				m_buffered_size;
	uint32_t			m_max_buffersize;
	uint32_t			m_min_play_buf_count;
	int64_t				m_cached_duration_msec;
	bool				m_first_pkt;
	PacketQueue			m_video_q;
	PacketQueue			m_audio_q;
	int64_t				m_start_msec;

	int					m_seek_flag;
	int64_t				m_seek_time_msec; // for seek pos

	// for interrupt
	int64_t				m_open_stream_start_msec;
	//int64_t				m_read_stream_start_msec;

	bool				m_thread_created;
	bool				m_buffering;
	bool				m_seeking;
	bool				m_eof;

	// AVCC for flv,mp4,mkv video codec extra_data
	int					m_NALULengthSizeMinusOne;
	int					m_num_of_sps;
	int					m_num_of_pps;

	//subtitles
	AVStream*			m_subtitle_stream;
	int					m_subtitle_stream_idx;
	AVCodecContext*		m_subtitle_dec_ctx;
	AVSubtitle*			mAVSubtitle;
	int32_t				mSubtitleTrackFirstIndex;
	int32_t				mSubtitleTrackIndex;
	ISubtitles*			mISubtitle;

	pthread_mutex_t		mSubtitleLock;

#ifdef USE_SWRESAMPLE
	// decode audio
	AVFrame*			m_audio_frame;
	int					m_src_channels;
	uint64_t			m_src_channel_layout;
	SwrContext*			m_swr;
	int					m_audio_need_convert;
	int16_t*			mSamples;
#endif
};

#endif // FF_EXTRACTOR_H_