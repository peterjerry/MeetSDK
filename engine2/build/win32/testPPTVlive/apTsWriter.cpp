#include "stdafx.h"
#include "apTsWriter.h"
#define LOG_TAG "apTsWriter"
#include "log.h"

apTsWriter::apTsWriter(void)
	:m_in_fmt_ctx(NULL), 
	m_out_fmt_ctx(NULL), m_out_fmt(NULL), m_pBsfc_h264(NULL)
{
	av_register_all();
	avformat_network_init();
}


apTsWriter::~apTsWriter(void)
{
	if (m_pBsfc_h264) {
		av_bitstream_filter_close(m_pBsfc_h264);
		m_pBsfc_h264 = NULL;
	}

	avformat_network_deinit();
}

bool apTsWriter::open(AVFormatContext *in_fmt_ctx, char *file_name)
{
	int ret;

	m_in_fmt_ctx = in_fmt_ctx;

	avformat_alloc_output_context2(&m_out_fmt_ctx, NULL, NULL, file_name);
    if (!m_out_fmt_ctx) {
        LOGE("Could not create output context");
        return false;
    }

    m_out_fmt = m_out_fmt_ctx->oformat;

	for (int i = 0; i < in_fmt_ctx->nb_streams; i++) {
		AVStream *in_stream = in_fmt_ctx->streams[i];

		if (in_stream->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
			m_video_stream_idx = i;
			m_video_stream = in_stream;
		}
		else if (in_stream->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
			m_audio_stream_idx = i;
			m_audio_stream = in_stream;
		}

        AVStream *out_stream = avformat_new_stream(m_out_fmt_ctx, in_stream->codec->codec);
        if (!out_stream) {
            LOGE("Failed allocating output stream");
			avformat_close_input(&m_out_fmt_ctx);
            return false;
        }

        ret = avcodec_copy_context(out_stream->codec, in_stream->codec);
        if (ret < 0) {
            LOGE("Failed to copy context from input to output stream codec context");
            avformat_close_input(&m_out_fmt_ctx);
            return false;
        }
        if (m_out_fmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
            out_stream->codec->flags |= CODEC_FLAG_GLOBAL_HEADER;
    }

    av_dump_format(m_out_fmt_ctx, 0, file_name, 1);

	if (!(m_out_fmt->flags & AVFMT_NOFILE)) {
        ret = avio_open(&m_out_fmt_ctx->pb, file_name, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("Could not open output file '%s'", file_name);
            avformat_close_input(&m_out_fmt_ctx);
            return false;
        }
    }

    ret = avformat_write_header(m_out_fmt_ctx, NULL);
    if (ret < 0) {
        LOGE("Error occurred when opening output file");
        avformat_close_input(&m_out_fmt_ctx);
		return false;
    }

	m_pBsfc_h264 = av_bitstream_filter_init("h264_mp4toannexb");
	if (!m_pBsfc_h264) {
		LOGE("Could not aquire h264_mp4toannexb filter");
		avformat_close_input(&m_out_fmt_ctx);
		return false;
	}

	return true;
}

bool apTsWriter::write_frame(AVPacket *pkt)
{
	/* Write the compressed frame to the media file. */
	int ret;

	if (pkt->stream_index == m_video_stream_idx) {
		// Apply MP4 to H264 Annex B filter on buffer
		//int origin_size = m_sample_pkt->size;
		int isKeyFrame = pkt->flags & AV_PKT_FLAG_KEY;
		av_bitstream_filter_filter(m_pBsfc_h264, m_video_stream->codec, NULL, &pkt->data, &pkt->size, 
			pkt->data, pkt->size, isKeyFrame);
	}

	AVStream *in_stream, *out_stream;

	in_stream  = m_in_fmt_ctx->streams[pkt->stream_index];
    out_stream = m_out_fmt_ctx->streams[pkt->stream_index];

    /* copy packet */
	pkt->pts = av_rescale_q_rnd(pkt->pts, in_stream->time_base, out_stream->time_base, (AVRounding)(AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
    pkt->dts = av_rescale_q_rnd(pkt->dts, in_stream->time_base, out_stream->time_base, (AVRounding)(AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
    pkt->duration = av_rescale_q(pkt->duration, in_stream->time_base, out_stream->time_base);
    pkt->pos = -1;

	ret = av_interleaved_write_frame(m_out_fmt_ctx, pkt);
	if ( ret != 0) {
		LOGE("failed to write audio frame. err = %d", ret);
		return false;
	}

	return true;
}

void apTsWriter::close()
{
	av_write_trailer(m_out_fmt_ctx);

    /* close output */
    if (m_out_fmt && !(m_out_fmt->flags & AVFMT_NOFILE))
        avio_close(m_out_fmt_ctx->pb);
    avformat_free_context(m_out_fmt_ctx);
}