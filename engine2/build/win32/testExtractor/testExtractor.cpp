// testExtractor.cpp : 定义控制台应用程序的入口点。
//

#include "stdafx.h"

#define LOG_TAG "testExtractor"
#include "log.h"
#include "player.h"
#include "ppffmpeg.h"
#include "apFileLog.h"
#include "IPpbox.h"
#include "IDemuxer.h"
#include "sdl.h"
#undef main

#pragma comment(lib, "sdl")
#pragma comment(lib, "libppbox")

#define PLAYLINK	17493573
#define FT			1
#define URL_FMT		"http://127.0.0.1:%d/record.m3u8?type=ppvod2&playlink=%d"

#define URL_SURFIX "%3Fft%3D2%26bwtype%3D3%26platform%3Dandroid3" \
	"%26type%3Dphone.android.vip%26sv%3D4.1.3%26param%3DuserTypeD1&mux.M3U8.segment_duration=5"

#define LOCAL_FILE "D:\\Archive\\media\\dragon_trainer_4audio.mkv"

#define MAX_PKT_SIZE (65536 * 10)

static bool startP2P();

static int16_t get_aac_extradata(int32_t channels, int32_t sample_rate, int64_t channel_layout);

static void parse_aac_header(int16_t header);

class MyMediaPlayerListener: public MediaPlayerListener
{
public:
    virtual void notify(int msg, int ext1, int ext2);
	~MyMediaPlayerListener(){}
};

void MyMediaPlayerListener::notify(int msg, int ext1, int ext2)
{
}

int _tmain(int argc, _TCHAR* argv[])
{
	apLog::init("c:\\log\\testExtractor.log");

	if( SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO | SDL_INIT_TIMER)) {
		char msg[256] = {0};
		sprintf(msg, "Could not initialize SDL - %s\n", SDL_GetError());
		printf(msg);
		return 1;
	}

	int surface_w, surface_h;
	surface_w = 640;
	surface_h = 480;

	SDL_Surface *surface = SDL_SetVideoMode(surface_w, surface_h, 32, 
		SDL_HWSURFACE | SDL_DOUBLEBUF);
	if (!surface) {
		printf("failed to create surface\n");
		return 1;
	}

	if (!startP2P())
		return 1;

	MyMediaPlayerListener listener;

	FFExtractor ext;
	status_t stat;

	char url[4096] = {0};

	uint16_t httpPort = PPBOX_GetPort("http");
	_snprintf(url, 4096, URL_FMT, httpPort, PLAYLINK);
	strcat(url, URL_SURFIX);

	ext.setListener(&listener);
	stat = ext.setDataSource(/*url*/LOCAL_FILE);

	int32_t count;
	stat = ext.getTrackCount(&count);
	int64_t duration_msec;

	bool video_selected = false;
	int audio_stream_idx = -1;
	int video_stream_idx = -1;

	for (int i=0;i<count;i++) {
		MediaFormat mf;
		memset(&mf, 0, sizeof(mf));
		stat = ext.getTrackFormat(i, &mf);
		if (mf.media_type == PPMEDIA_TYPE_VIDEO) {
			video_stream_idx = i;
			duration_msec = mf.duration_us / 1000;
			ext.selectTrack(i);
			video_selected = true;
		}
		else if (mf.media_type == PPMEDIA_TYPE_AUDIO) {
			audio_stream_idx = i;
			ext.selectTrack(i);
			if (video_selected)
				break;
		}
	}
	
	int32_t index;
	unsigned char *pkt_data = new unsigned char[MAX_PKT_SIZE];
	int64_t total_count= 0;
	int32_t sample_size;
	int64_t time_usec;

	bool seeked = false;

	//Our event structure
	SDL_Event e;
	bool paused = false;
	bool quit = false;

	AVCodecContext* c			= NULL;
	AVCodec* codec				= NULL;
	AVFrame * decoded_frame		= NULL;

	codec = avcodec_find_decoder(AV_CODEC_ID_AAC);
    if (!codec) {
        fprintf(stderr, "Codec not found\n");
        return 1;
    }

	c = avcodec_alloc_context3(codec);
    if (!c) {
        fprintf(stderr, "Could not allocate audio codec context\n");
        return 1;
    }

    /* open it */
    if (avcodec_open2(c, codec, NULL) < 0) {
        fprintf(stderr, "Could not open codec\n");
        return 1;
    }

	if (!(decoded_frame = av_frame_alloc())) {
		fprintf(stderr, "Could not allocate audio frame\n");
		return 1;
	}

	while (!quit){
		while (SDL_PollEvent(&e)) { // would block!
			if (e.type == SDL_QUIT) {
				printf("click quit\n");
				SDL_SetTimer(0, NULL);
				quit = 1;
				break;
			}
			else if (e.type == SDL_KEYDOWN) {
				switch(e.key.keysym.sym) {
				case SDLK_LEFT:
					break;
				case SDLK_RIGHT:
					break;
				default:
					break;
				}
			}
			else if (e.type == SDL_MOUSEBUTTONDOWN) {
				Uint16 x = e.motion.x;
				printf("mouse x: %d\n", x);

				int64_t new_pos;
				new_pos = duration_msec * 1000 * x / surface_w;
				ext.seekTo(new_pos, SEEK_TO_NEXT_SYNC);
				printf("seek %d/%d %I64d\n", x, surface_w, duration_msec);
			}
		}

		stat = ext.getSampleTrackIndex(&index);
		if (stat != OK) {
			printf("failed to getSampleTrackIndex");
			break;
		}

		stat = ext.getSampleTime(&time_usec);
		if (stat != OK) {
			printf("failed to getSampleTime");
			break;
		}

		stat = ext.readSampleData(pkt_data, &sample_size);
		if (stat != OK) {
			printf("failed to readSampleData");
			break;
		}

		printf("read sample count %I64d stream #%d size %d time %I64d msec\n", 
			total_count, index, sample_size, time_usec / 1000);
		LOGI("read sample size %d", sample_size);
		total_count++;

		if (0/*index == audio_stream_idx*/) {
			int len;
			AVPacket avpkt;
			av_init_packet(&avpkt);
			avpkt.data = pkt_data;
			avpkt.size = sample_size;
			
			while (avpkt.size > 0) {
				int got_frame = 0;

				len = avcodec_decode_audio4(c, decoded_frame, &got_frame, &avpkt);
				if (len < 0) {
					fprintf(stderr, "Error while decoding\n");
					break;
				}
				if (got_frame) {
					printf("got audio frame\n");
				}

				avpkt.size -= len;
				avpkt.data += len;
				avpkt.dts =
				avpkt.pts = AV_NOPTS_VALUE;
			}
		}

		stat = ext.advance();
		if (stat != OK) {
			printf("failed to advance");
			break;
		}

		SDL_Delay(40);
	}

	return 0;
}

static bool startP2P()
{
	const char *gid = "13";
	const char *pid = "162";
	const char *auth = "08ae1acd062ea3ab65924e07717d5994";

	//PPBOX_SetConfig("", "HttpManager", "addr", "127.0.0.1:9106+");
	//PPBOX_SetConfig("", "RtspManager", "addr", "127.0.0.1:5156+");

	int32_t ec = PPBOX_StartP2PEngine(gid, pid, auth);
    if (ppbox_success != ec) {
		printf("start p2p engine: %s\n", PPBOX_GetLastErrorMsg());
        return false;
    }

	uint16_t rtspPort = PPBOX_GetPort("rtsp");
	uint16_t httpPort = PPBOX_GetPort("http");

	printf("p2pEngine: rtsp port %d, http port %d\n", rtspPort, httpPort);

	return true;
}

static void parse_aac_header(int16_t aac_header)
{
	unsigned char tmp[2] = {0};
	memcpy(tmp, (unsigned char *)&aac_header + 1, 1);
	memcpy(tmp + 1, &aac_header, 1);
	printf("aac_header: 0x%02x 0x%02x\n", tmp[0], tmp[1]);

	int16_t aacObjectType;
	int16_t sampleRateIdx;
	int16_t numChannels;

	aacObjectType	= aac_header >> 11;
	sampleRateIdx	= 0x0F & (aac_header >> 7);
	numChannels		= 0x0F & (aac_header >> 3);

	char * sample_rate_list[] = {
	// Sampling Frequencies
	"9600", //0: 96000 Hz
	"88200", //1: 88200 Hz
	"64000", //2: 64000 Hz
	"48000", //3: 48000 Hz
	"44100", //4: 44100 Hz
	"32000", //5: 32000 Hz
	"24000", //6: 24000 Hz
	"22050", //7: 22050 Hz
	"16000", //8: 16000 Hz
	"12000", //9: 12000 Hz
	"11025", //10: 11025 Hz
	"8000", //11: 8000 Hz
	"7350", //12: 7350 Hz
	"Reserved", //13: Reserved
	"Reserved", //14: Reserved
	"explictly", //15: frequency is written explictly
	};
	
	printf("aacObjectType %d, sampleRateIdx %d(%s), numChannels %d\n", 
		aacObjectType, sampleRateIdx, sample_rate_list[sampleRateIdx], numChannels);
}

static int16_t get_aac_extradata(int32_t channels, int32_t sample_rate, int64_t channel_layout)
{
	int16_t aacObjectType = 2; // 2: AAC LC (Low Complexity)
	int16_t sampleRateIdx;
	int16_t numChannels;

	/*Sampling Frequencies
	0: 96000 Hz
	1: 88200 Hz
	2: 64000 Hz
	3: 48000 Hz
	4: 44100 Hz
	5: 32000 Hz
	6: 24000 Hz
	7: 22050 Hz
	8: 16000 Hz
	9: 12000 Hz
	10: 11025 Hz
	11: 8000 Hz
	12: 7350 Hz
	13: Reserved
	14: Reserved
	15: frequency is written explictly
	*/
	switch(sample_rate / 2) {
	case 96000:
		sampleRateIdx = 0;
		break;
	case 88200:
		sampleRateIdx = 1;
		break;
	case 64000:
		sampleRateIdx = 2;
		break;
	case 48000:
		sampleRateIdx = 3;
		break;
	case 44100:
		sampleRateIdx = 4;
		break;
	case 32000:
		sampleRateIdx = 5;
		break;
	case 24000:
		sampleRateIdx = 6;
		break;
	case 22050:
		sampleRateIdx = 7;
		break;
	case 16000:
		sampleRateIdx = 8;
		break;
	case 12000:
		sampleRateIdx = 9;
		break;
	case 11025:
		sampleRateIdx = 10;
		break;
	case 8000:
		sampleRateIdx = 11;
		break;
	case 7350:
		sampleRateIdx = 12;
		break;
	default:
		printf("unsupported audio sample rate %d\n", sample_rate);
		return ERROR;
	}

	if (channels != 0) {
		numChannels = channels;
	}
	else {
		/*Channel Configurations
		0: Defined in AOT Specifc Config
		1: 1 channel: front-center
		2: 2 channels: front-left, front-right
		3: 3 channels: front-center, front-left, front-right
		4: 4 channels: front-center, front-left, front-right, back-center
		5: 5 channels: front-center, front-left, front-right, back-left, back-right
		6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
		7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel
		8-15: Reserved*/
		switch(channel_layout) {
		case AV_CH_LAYOUT_MONO:
			numChannels = 1; 
			break;
		case AV_CH_LAYOUT_STEREO:
			numChannels = 2; 
			break;
		case AV_CH_LAYOUT_2POINT1:
		case AV_CH_LAYOUT_2_1:
		case AV_CH_LAYOUT_SURROUND:
			numChannels = 3; 
			break;
		case AV_CH_LAYOUT_4POINT0:
			numChannels = 4; 
			break;
		case AV_CH_LAYOUT_5POINT0_BACK:
			numChannels = 5; 
			break;
		case AV_CH_LAYOUT_5POINT1_BACK:
			numChannels = 6; 
			break;
		default:
			printf("unsupported audio channel layout %I64d\n", channel_layout);
			return -1;
		}
	}

	return (aacObjectType << 11) | (sampleRateIdx << 7) | (numChannels << 3);
}