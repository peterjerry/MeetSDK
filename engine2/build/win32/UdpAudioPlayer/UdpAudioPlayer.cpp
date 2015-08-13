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
#pragma comment(lib, "avfilter")

#pragma comment(lib, "pthreadVC2")

#pragma comment(lib, "sdl")

#define SDL_AUDIO_SAMPLES		1024

static void ff_log_callback(void* user, int level, const char* fmt, va_list vl);

static int open_codec_context(int *stream_idx,
                              AVFormatContext *fmt_ctx, enum AVMediaType type);

static int decode_packet(int *got_frame, int cached);

static void audio_callback(void *userdata, Uint8 *stream, int len);

static int init_filters(AVStream *audio_stream, int audio_stream_index);

static int insert_filter(const char *name, const char* arg, AVFilterContext **last_filter);

static AVFormatContext *fmt_ctx = NULL;
static AVCodecContext *audio_dec_ctx = NULL;
static AVStream *audio_stream = NULL;
static int64_t audio_channel_layout = 0;
static int audio_channels = 0;

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

AVFilterContext *buffersink_ctx;
AVFilterContext *buffersrc_ctx;
AVFilterContext *last_flt_ctx;
AVFilterGraph *filter_graph;

static const char *filter_descr[] = {"aresample=48000", "aformat=sample_fmts=s16:channel_layouts=stereo", NULL};

int _tmain(int argc, _TCHAR* argv[])
{
	int ret = 0, got_frame;

	if (argc != 3) {
        fprintf(stderr, "usage: %s input_file fifo_size(in byte)\n"
                "\n", argv[0]);
		fprintf(stderr, "example: %s \"rtmp://172.16.204.106/live/test01 live=1\" 128000\n", argv[0]);
        exit(1);
    }

	av_register_all();
	avfilter_register_all();

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

		// it may changed when decode
		audio_channel_layout = audio_dec_ctx->channel_layout;
		audio_channels = audio_dec_ctx->channels;

		swr_ctx = swr_alloc_set_opts(swr_ctx,
			AV_CH_LAYOUT_STEREO,
			AV_SAMPLE_FMT_S16,
			audio_dec_ctx->sample_rate,
			audio_dec_ctx->channel_layout,
			audio_dec_ctx->sample_fmt,
			audio_dec_ctx->sample_rate,
			0, 0);
		if (!swr_ctx) {
			printf("failed to alloc swr_ctx\n");
			return 1;
		}

		if (swr_init(swr_ctx) < 0 || swr_ctx == NULL) {
			printf("swr_init failed\n");
			goto end;
		}

		printf("swr_init done!\n");

		//if (init_filters(audio_stream, audio_stream_idx) < 0)
		//	printf("failed to init_filters!\n");
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
	wanted_spec.channels	= 2;
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
	static int fill_mute_count = 0;

	if (audio_fifo.used() < len) {
		memset(stream, len, 0);

		fill_mute_count++;
		if (fill_mute_count < 5)
			printf("fill mute %d %d\n", audio_fifo.used(), len);
		return;
	}

	fill_mute_count = 0;
	audio_fifo.read((char *)stream, len);
}

static int decode_packet(int *got_frame, int cached)
{
    int ret = 0;

	if (pkt.stream_index != audio_stream_idx)
		return 0;

	while (1) {
        /* decode audio frame */
        ret = avcodec_decode_audio4(audio_dec_ctx, frame, got_frame, &pkt);
        if (ret < 0) {
            fprintf(stderr, "Error decoding audio frame\n");
            return ret;
        }

		pkt.data += ret;
        pkt.size -= ret;

        if (*got_frame) {
            printf("audio_frame%s n:%d nb_samples:%d pts:%.3f sec\r",
                   cached ? "(cached)" : "",
                   audio_frame_count++, frame->nb_samples,
				   pkt.pts * av_q2d(audio_stream->time_base));

			if (frame->channel_layout != audio_channel_layout || frame->channels != audio_channels) {
				char frame_channel_layout_name[64] = {0};
				char audio_channel_layout_name[64] = {0};
				av_get_channel_layout_string(frame_channel_layout_name, 64, frame->channels, frame->channel_layout);
				av_get_channel_layout_string(audio_channel_layout_name, 64, audio_channels, audio_channel_layout);
				printf("frame NOT match prop %I64d(%s) %d -> %I64d(%s) %d\n",
					audio_channel_layout, audio_channel_layout_name, audio_channels, 
					frame->channel_layout, frame_channel_layout_name, frame->channels);
			}
			else {
				// resample
				if (swr_ctx != NULL) {
					int32_t sampleInCount = frame->nb_samples;
					int sampleOutCount = (int)av_rescale_rnd(
						swr_get_delay(swr_ctx, frame->sample_rate) + sampleInCount,
						frame->sample_rate,
						frame->sample_rate,
						AV_ROUND_UP);

					int sampleCountOutput = swr_convert(swr_ctx,
						(uint8_t**)(&audio_dst_data), sampleOutCount,
						(const uint8_t**)(frame->extended_data), sampleInCount);
					if (sampleCountOutput < 0) {
						printf("Audio convert sampleformat failed, ret %d\n", sampleCountOutput);
						return -1;
					}
				
					audio_buffer_size = sampleCountOutput * 2 * 2;
					int written = audio_fifo.write((char *)audio_dst_data, audio_buffer_size);
					//printf("swr output: sample:%d, size:%d, written %d\n", sampleCountOutput, audio_buffer_size, written);
				}
				else {
					int written = audio_fifo.write((char *)frame->data, frame->linesize[0]);
					//printf("swr output: sample:%d, size:%d, written %d\n", sampleCountOutput, audio_buffer_size, written);
				}
			}
        }

		/* packet has no more data, decode next packet. */
		if (pkt.size <= 0)
			break;
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

static int init_filters(AVStream *audio_stream, int audio_stream_index)
{
	AVCodecContext *dec_ctx = audio_stream->codec;

    char args[512];
    int ret = 0;
    AVFilter *abuffersrc  = avfilter_get_by_name("abuffer");
    AVFilter *abuffersink = avfilter_get_by_name("abuffersink");
    AVFilterInOut *outputs = avfilter_inout_alloc();
    AVFilterInOut *inputs  = avfilter_inout_alloc();
    static const enum AVSampleFormat out_sample_fmts[] = { AV_SAMPLE_FMT_S16, AV_SAMPLE_FMT_NONE };
	static const int64_t out_channel_layouts[] = { AV_CH_LAYOUT_STEREO, -1 };
    static const int out_sample_rates[] = { dec_ctx->sample_rate, -1 };
    const AVFilterLink *outlink;
    AVRational time_base = fmt_ctx->streams[audio_stream_index]->time_base;

    filter_graph = avfilter_graph_alloc();
    if (!outputs || !inputs || !filter_graph) {
        ret = AVERROR(ENOMEM);
        goto end;
    }

    /* buffer audio source: the decoded frames from the decoder will be inserted here. */
    if (!dec_ctx->channel_layout)
        dec_ctx->channel_layout = av_get_default_channel_layout(dec_ctx->channels);
    _snprintf_s(args, sizeof(args),
            "time_base=%d/%d:sample_rate=%d:sample_fmt=%s:channel_layout=0x%I64d",
             time_base.num, time_base.den, dec_ctx->sample_rate,
             av_get_sample_fmt_name(dec_ctx->sample_fmt), dec_ctx->channel_layout);
    ret = avfilter_graph_create_filter(&buffersrc_ctx, abuffersrc, "in",
                                       args, NULL, filter_graph);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot create audio buffer source\n");
        goto end;
    }

    /* buffer audio sink: to terminate the filter chain. */
    ret = avfilter_graph_create_filter(&buffersink_ctx, abuffersink, "out",
                                       NULL, NULL, filter_graph);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot create audio buffer sink\n");
        goto end;
    }

    ret = av_opt_set_int_list(buffersink_ctx, "sample_fmts", out_sample_fmts, -1,
                              AV_OPT_SEARCH_CHILDREN);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot set output sample format\n");
        goto end;
    }

    ret = av_opt_set_int_list(buffersink_ctx, "channel_layouts", out_channel_layouts, -1,
                              AV_OPT_SEARCH_CHILDREN);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot set output channel layout\n");
        goto end;
    }

    ret = av_opt_set_int_list(buffersink_ctx, "sample_rates", out_sample_rates, -1,
                              AV_OPT_SEARCH_CHILDREN);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot set output sample rate\n");
        goto end;
    }

	last_flt_ctx = buffersink_ctx;
	int index = 1;
	while(filter_descr[index]) {
		const char* name = NULL;
		const char* arg = NULL;

		char tmp[64] = {0};
		char* pos = NULL;
		strncpy_s(tmp, 64, filter_descr[index], 64);
		pos = strchr(tmp, '=');
		if (pos != NULL) {
			*pos = '\0';
			name = tmp;
			arg = pos + 1;
		}
		else {
			name = filter_descr[index];
			arg = NULL;
		}
		insert_filter(name, arg, &last_flt_ctx);
		index++;
	}

    /* Endpoints for the filter graph. */
    outputs->name       = av_strdup("in");
    outputs->filter_ctx = buffersrc_ctx;
    outputs->pad_idx    = 0;
    outputs->next       = NULL;

    inputs->name       = av_strdup("out");
    inputs->filter_ctx = last_flt_ctx;
    inputs->pad_idx    = 0;
    inputs->next       = NULL;

    if ((ret = avfilter_graph_parse_ptr(filter_graph, filter_descr[0],
                                        &inputs, &outputs, NULL)) < 0)
        goto end;

    if ((ret = avfilter_graph_config(filter_graph, NULL)) < 0)
        goto end;

    /* Print summary of the sink buffer
     * Note: args buffer is reused to store channel layout string */
    outlink = buffersink_ctx->inputs[0];
    av_get_channel_layout_string(args, sizeof(args), -1, outlink->channel_layout);
    av_log(NULL, AV_LOG_INFO, "Output: srate:%dHz fmt:%s chlayout:%s\n",
           (int)outlink->sample_rate,
		   (char *)av_x_if_null(av_get_sample_fmt_name((AVSampleFormat)outlink->format), "?"),
           args);

end:
    avfilter_inout_free(&inputs);
    avfilter_inout_free(&outputs);

    return ret;
}

static int insert_filter(const char *name, const char* arg, AVFilterContext **last_filter)
{
	AVFilterContext *filt_ctx;
	int ret;
	char fltr_name[64] ={0};
	_snprintf_s(fltr_name, sizeof(fltr_name), "ffplayer_%s", name);
	ret = avfilter_graph_create_filter(&filt_ctx, avfilter_get_by_name(name),
		fltr_name, arg, NULL, filter_graph);
	if (ret < 0) {
		printf("Cannot create filter: %s\n", name);
		return -1;
	}

	ret = avfilter_link(filt_ctx, 0, *last_filter, 0);
	if (ret < 0) {
		printf("Cannot link filter: %s\n", name);
		return -1;
	}

	*last_filter = filt_ctx;
	return 0;
}
