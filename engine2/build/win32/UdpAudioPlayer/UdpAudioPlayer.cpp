// UdpAudioPlayer.cpp : 定义控制台应用程序的入口点。
//

#include "stdafx.h"
#include "ppffmpeg.h"
#include "fifobuffer.h"
#include <sdl.h>
#undef main

#pragma comment(lib, "avutil")
#pragma comment(lib, "avcodec")
#pragma comment(lib, "avformat")
#pragma comment(lib, "swresample")
#pragma comment(lib, "swscale")

#pragma comment(lib, "pthreadVC2")

#pragma comment(lib, "sdl")

#define SDL_AUDIO_SAMPLES		1024

static void ff_log_callback(void* user, int level, const char* fmt, va_list vl);

static int open_codec_context(int *stream_idx,
                              AVFormatContext *fmt_ctx, enum AVMediaType type);

static int decode_packet(int *got_frame, int cached);

static void audio_callback(void *userdata, Uint8 *stream, int len);

static AVFormatContext *fmt_ctx = NULL;
static AVCodecContext *audio_dec_ctx = NULL;
static AVStream *audio_stream = NULL;

static uint8_t *audio_dst_data = NULL;
static int       audio_dst_linesize;
static int audio_dst_bufsize;

static uint32_t audio_buffer_size;

static SwrContext *swr_ctx = NULL;

static and_fifobuffer audio_fifo;

static int audio_stream_idx = -1;
static AVFrame *frame = NULL;
static AVPacket pkt;
static int audio_frame_count = 0;

int _tmain(int argc, _TCHAR* argv[])
{
	int ret = 0, got_frame;

	if (argc != 3) {
        fprintf(stderr, "usage: %s input_file fifo_size(in byte)\n"
                "\n", argv[0]);
        exit(1);
    }

	av_register_all();

	avformat_network_init();

	//av_log_set_callback(ff_log_callback);

	/* open input file, and allocate format context */
    if (avformat_open_input(&fmt_ctx, argv[1], NULL, NULL) < 0) {
		printf("Could not open source %s\n", argv[1]);
        return 1;
    }

	/* retrieve stream information */
    if (avformat_find_stream_info(fmt_ctx, NULL) < 0) {
        printf("Could not find stream information\n");
        return 1;
    }

	if (open_codec_context(&audio_stream_idx, fmt_ctx, AVMEDIA_TYPE_AUDIO) >= 0) {
        audio_stream = fmt_ctx->streams[audio_stream_idx];
        audio_dec_ctx = audio_stream->codec;

		audio_dst_bufsize = 192000 * 2;
        audio_dst_data = (uint8_t*)av_malloc(audio_dst_bufsize);
		if (audio_dst_data == NULL) {
			printf("No enough memory for audio conversion\n");
			return 1;
		}

		swr_ctx = swr_alloc_set_opts(swr_ctx,
			audio_dec_ctx->channel_layout,
			AV_SAMPLE_FMT_S16,
			audio_dec_ctx->sample_rate,
			audio_dec_ctx->channel_layout,
			audio_dec_ctx->sample_fmt,
			audio_dec_ctx->sample_rate,
			0, 0);                   
		if (swr_init(swr_ctx) < 0 || swr_ctx == NULL) {
			printf("swr_init failed\n");
			goto end;
		}
    }

	/* dump input information to stderr */
    av_dump_format(fmt_ctx, 0, argv[1], 0);

	frame = av_frame_alloc();
    if (!frame) {
        fprintf(stderr, "Could not allocate frame\n");
        ret = AVERROR(ENOMEM);
        goto end;
    }

	int fifo_size = atoi(argv[2]);
	printf("fifo_size %d\n", fifo_size);
	audio_fifo.create(fifo_size);

	// init sdl audio
	SDL_Init(SDL_INIT_AUDIO);

	SDL_AudioSpec wanted_spec, spec;
	memset(&wanted_spec, 0, sizeof(SDL_AudioSpec));
	memset(&spec, 0, sizeof(SDL_AudioSpec));
	wanted_spec.freq		= audio_dec_ctx->sample_rate;
	wanted_spec.format		= AUDIO_S16SYS;
	wanted_spec.channels	= audio_dec_ctx->channels;
	wanted_spec.silence		= 0;
	wanted_spec.samples		= SDL_AUDIO_SAMPLES;
	wanted_spec.callback	= audio_callback;
	wanted_spec.userdata	= &audio_fifo;

	if (SDL_OpenAudio(&wanted_spec, &spec) < 0) {
		printf("SDL_OpenAudio: %s\n", SDL_GetError());
		return 1;
	}

	printf("SDL_AudioSpec got: chn %d, fmt 0x%x, freq %d\n", spec.channels, spec.format, spec.freq);

	SDL_PauseAudio(0);

    /* initialize packet, set data to NULL, let the demuxer fill it */
    av_init_packet(&pkt);
    pkt.data = NULL;
    pkt.size = 0;

    /* read frames from the file */
    while (av_read_frame(fmt_ctx, &pkt) >= 0) {
        decode_packet(&got_frame, 0);
        av_free_packet(&pkt);
    }

end:
    if (audio_dec_ctx)
        avcodec_close(audio_dec_ctx);
    avformat_close_input(&fmt_ctx);
    av_free(frame);
    av_free(audio_dst_data);
	if (swr_ctx)
		swr_free(&swr_ctx);
	return 0;
}

static void audio_callback(void *userdata, Uint8 *stream, int len)
{
	if (audio_fifo.used() < len) {
		memset(stream, len, 0);
		printf("fill mute %d %d\n", audio_fifo.used(), len);
		return;
	}

	audio_fifo.read((char *)stream, len);
}

static int decode_packet(int *got_frame, int cached)
{
    int ret = 0;

	if (pkt.stream_index == audio_stream_idx) {
        /* decode audio frame */
        ret = avcodec_decode_audio4(audio_dec_ctx, frame, got_frame, &pkt);
        if (ret < 0) {
            fprintf(stderr, "Error decoding audio frame\n");
            return ret;
        }

        if (*got_frame) {
            printf("audio_frame%s n:%d nb_samples:%d pts:%.3f sec\r",
                   cached ? "(cached)" : "",
                   audio_frame_count++, frame->nb_samples,
				   pkt.pts * av_q2d(audio_stream->time_base));

			// res
			if (swr_ctx != NULL) {
				int32_t sampleInCount = frame->nb_samples;
				int sampleOutCount = (int)av_rescale_rnd(
					swr_get_delay(swr_ctx, frame->sample_rate) + sampleInCount,
					frame->sample_rate,
					frame->sample_rate,
					AV_ROUND_UP);

				int sampleCountOutput = swr_convert(swr_ctx,
					(uint8_t**)(&audio_dst_data), (int)sampleOutCount,
					(const uint8_t**)(frame->extended_data), sampleInCount);
				if (sampleCountOutput < 0) {
					printf("Audio convert sampleformat failed, ret %d\n", sampleCountOutput);
					return -1;
				}
				
				audio_buffer_size = sampleCountOutput * 2 * 2;
				int written = audio_fifo.write((char *)audio_dst_data, audio_buffer_size); 
				//printf("swr output: sample:%d, size:%d, written %d\n", sampleCountOutput, audio_buffer_size, written);
			}
        }
    }

    return ret;
}

static int open_codec_context(int *stream_idx,
                              AVFormatContext *fmt_ctx, enum AVMediaType type)
{
    int ret;
    AVStream *st;
    AVCodecContext *dec_ctx = NULL;
    AVCodec *dec = NULL;
    AVDictionary *opts = NULL;

    ret = av_find_best_stream(fmt_ctx, type, -1, -1, NULL, 0);
    if (ret < 0) {
		printf("Could not find best %s stream\n",
			av_get_media_type_string(type));
        return ret;
    } else {
        *stream_idx = ret;
        st = fmt_ctx->streams[*stream_idx];

        /* find decoder for the stream */
        dec_ctx = st->codec;
        dec = avcodec_find_decoder(dec_ctx->codec_id);
        if (!dec) {
            printf("Failed to find %s codec\n", av_get_media_type_string(type));
            return AVERROR(EINVAL);
        }

        /* Init the decoders, with or without reference counting */
        av_dict_set(&opts, "refcounted_frames", "1", 0);
        if ((ret = avcodec_open2(dec_ctx, dec, &opts)) < 0) {
            printf("Failed to open %s codec\n", av_get_media_type_string(type));
            return ret;
        }
    }

    return 0;
}


static void ff_log_callback(void* user, int level, const char* fmt, va_list vl)
{
	char szLog[2048] = {0};
	vsprintf_s(szLog, fmt, vl);

	if (strstr(szLog, "first_dts") != NULL)
		return;

	switch (level) {
		case AV_LOG_PANIC:
		case AV_LOG_FATAL:
		case AV_LOG_ERROR:
			printf("ffmpeg(%d) %s\n", level, szLog);
			break;
		case AV_LOG_WARNING:
            printf("ffmpeg(%d) %s\n", level, szLog);
			break;
		case AV_LOG_INFO:
            printf("ffmpeg(%d) %s\n", level, szLog);
			break;
		case AV_LOG_DEBUG:
            //printf("ffmpeg(%d) %s\n", level, szLog);
			break;
		case AV_LOG_VERBOSE:
            //printf("ffmpeg(%d) %s\n", level, szLog);
			break;
		default:
			printf("ffmpeg(%d) %s", level, szLog);
			break;
	}
}
