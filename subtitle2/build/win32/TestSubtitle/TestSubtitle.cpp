// TestSubtitle.cpp : 定义控制台应用程序的入口点。
//

#include "stdafx.h"

#include <atlbase.h>
#include <atlconv.h>
#include "subtitle.h"
#include "ppffmpeg.h"
#define LOG_TAG "test_subtitle"
#include "log.h"

#pragma comment(lib, "ass")
#pragma comment(lib, "pthreadVC2")

#pragma comment(lib, "avutil")
#pragma comment(lib, "avcodec")
#pragma comment(lib, "avformat")
#pragma comment(lib, "swresample")
#pragma comment(lib, "swscale")

#define EMBEDDING_SUBTITLE
#define SUB_FILE_PATH "E:\\QQDownload\\Manhattan.S01E08.720p.HDTV.x264-KILLERS\\Manhattan.S01E08.720p.HDTV.x264-KILLERS.简体.srt"
#define LOCAL_FILE "D:\\Archive\\media\\test\\subtitle\\Manhattan.S01E08.HDTVrip.1024X576_sub.mkv"
//#define LOCAL_FILE "E:\\archive\\BaiduYunDownload\\魔女宅急便.mkv"

static int open_codec_context(AVFormatContext* fmt_ctx, int *stream_idx, AVMediaType media_type);

int my_strlen_utf8_c(char *s) {
  int i = 0, j = 0;
  while (s[i]) {
    if ((s[i] & 0xc0) != 0x80) j++;
    i++;
  }
  return j;
}

int _tmain(int argc, _TCHAR* argv[])
{
    ISubtitles* subtitle = NULL;
    if (!ISubtitles::create(&subtitle)) {
		printf("failed to create subtitle instance.\n");
        return 1;
    }
    
#ifdef EMBEDDING_SUBTITLE
	av_register_all();

	avformat_network_init();

	AVFormatContext *fmt_ctx = NULL;
	char *url = LOCAL_FILE;
	int subtitle_stream_idx = -1;
	AVStream* subtitle_stream;
	AVCodecContext* subtitle_dec_ctx;
	AVPacket pkt;
	AVSubtitle sub;
	int got_sub;
	int ret;
	int index = 0;

	/* open input file, and allocate format context */
    if (avformat_open_input(&fmt_ctx, url, NULL, NULL) < 0) {
		LOGE("Could not open source file");
        return 1;
    }

	/* retrieve stream information */
    if (avformat_find_stream_info(fmt_ctx, NULL) < 0) {
        LOGE("Could not find stream information\n");
        return 1;
    }

	if (open_codec_context(fmt_ctx, &subtitle_stream_idx, AVMEDIA_TYPE_SUBTITLE) < 0) {
		LOGE("failed to find subttile track");
		return 1;
	}

	subtitle_stream = fmt_ctx->streams[subtitle_stream_idx];
	subtitle_dec_ctx = subtitle_stream->codec;

	/* dump input information to stderr */
	av_dump_format(fmt_ctx, 0, url, 0);

	SubtitleCodecId codec_id;
	if (subtitle_dec_ctx->codec_id == AV_CODEC_ID_ASS ||
		subtitle_dec_ctx->codec_id == AV_CODEC_ID_SSA)
		codec_id = SUBTITLE_CODEC_ID_ASS;
	else
		codec_id = SUBTITLE_CODEC_ID_TEXT;
	ret = subtitle->addEmbeddingSubtitle(codec_id, "chs", "chs", 
		(const char *)subtitle_dec_ctx->extradata, subtitle_dec_ctx->extradata_size);
	if (ret < 0) {
		LOGE("failed to addEmbeddingSubtitle");
		return 1;
	}

	/* initialize packet, set data to NULL, let the demuxer fill it */
    av_init_packet(&pkt);
    pkt.data = NULL;
    pkt.size = 0;

    /* read frames from the file */
    while (av_read_frame(fmt_ctx, &pkt) >= 0 && index < 10 ) {
		if (pkt.stream_index == subtitle_stream_idx) {
			AVPacket orig_pkt = pkt;
			do {
				ret = avcodec_decode_subtitle2(subtitle_dec_ctx, &sub, &got_sub, &pkt);
				if (ret < 0) {
					break;
				}
				if (got_sub) {
					LOGI("got subtitle");

					for (int i=0;i<sub.num_rects;i++) {
						if (sub.rects[i]->ass) {
							int64_t start_time ,stop_time;
							AVRational ra;
							ra.num = 1;
							ra.den = AV_TIME_BASE;
							start_time = av_rescale_q(sub.pts + sub.start_display_time * 1000,
									 ra, subtitle_stream->time_base);
							stop_time = av_rescale_q(sub.pts + sub.end_display_time * 1000,
									 ra, subtitle_stream->time_base);
							subtitle->addEmbeddingSubtitleEntity(0, start_time, stop_time - start_time, 
								sub.rects[i]->ass, strlen(sub.rects[i]->ass)); // my_strlen_utf8_c

							index++;
						}
					}
					avsubtitle_free(&sub);
				}
				pkt.data += ret;
				pkt.size -= ret;
			} while (pkt.size > 0);
			av_free_packet(&orig_pkt);
		}
		else {
			av_free_packet(&pkt);
		}
    }

#else
	if (!subtitle->loadSubtitle(SUB_FILE_PATH, false)) {
		printf("failed to load subtitle: %s", SUB_FILE_PATH);
		return 1;
	}
#endif

    STSSegment* segment = NULL;
	char subtitleText[1024] = {0};

	int line = 0;
    while(line < 20 && subtitle->getNextSubtitleSegment(&segment)) {
        int64_t startTime = segment->getStartTime();
        int64_t stopTime = segment->getStopTime();
		segment->getSubtitleText(subtitleText, 1024);
        LOGI("%01d:%02d:%02d.%02d  --> %01d:%02d:%02d.%02d %s",
            int(startTime/1000/3600), int(startTime/1000%3600/60), int(startTime/1000%60), int(startTime%1000)/10,
            int(stopTime/1000/3600), int(stopTime/1000%3600/60), int(stopTime/1000%60), int(stopTime%1000)/10,
			CW2A(CA2W(subtitleText, CP_UTF8)));

		//getchar();
		line++;
    }

    subtitle->close();

	return 0;
}

static int open_codec_context(AVFormatContext* fmt_ctx, int *stream_idx, AVMediaType media_type)
{
    int ret;
    AVStream *st;
    AVCodecContext *dec_ctx = NULL;
    AVCodec *dec = NULL;
    AVDictionary *opts = NULL;

    ret = av_find_best_stream(fmt_ctx, media_type, -1, -1, NULL, 0);
    if (ret < 0) {
		LOGE("Could not find best %s stream in input file",
			av_get_media_type_string(media_type));
        return ret;
    } else {
        *stream_idx = ret;
        st = fmt_ctx->streams[*stream_idx];

        /* find decoder for the stream */
        dec_ctx = st->codec;
        dec = avcodec_find_decoder(dec_ctx->codec_id);
        if (!dec) {
            LOGE("Failed to find %s codec", av_get_media_type_string(media_type));
            return AVERROR(EINVAL);
        }

        /* Init the decoders, with or without reference counting */
        av_dict_set(&opts, "refcounted_frames", "1", 0);
        if ((ret = avcodec_open2(dec_ctx, dec, &opts)) < 0) {
            LOGE("Failed to open %s codec", av_get_media_type_string(media_type));
            return ret;
        }
    }

    return 0;
}




