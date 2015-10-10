#include "apFormatConverter.h"
#define LOG_TAG "apFormatConverter"
#include "log.h"

#define PB_BUF_SIZE 65536
#define INIT_PTS	-1

int64_t apFormatConverter::m_start_pts = 0;

apFormatConverter::apFormatConverter(void)
	:m_ifmt_ctx(NULL), m_in_fmt(NULL), 
	m_in_pb(NULL), m_in_pb_buf(NULL), 
	m_indata(NULL), m_indata_len(0), m_indata_offset(0),
	m_ofmt_ctx(NULL), m_out_fmt(NULL), 
	m_out_pb(NULL), m_out_pb_buf(NULL), 
	m_outdata(NULL), m_outdata_len(0), m_outdata_offset(0),
	m_pBsfc_h264(NULL)
{
	av_register_all();
}


apFormatConverter::~apFormatConverter(void)
{
}

int apFormatConverter::interrupt_l(void* ctx)
{
    return 0;
}

int apFormatConverter::ff_read_packet(void *opaque, uint8_t *buf, int buf_size)
{
	apFormatConverter *pIns = (apFormatConverter *)opaque;
	if (pIns)
		return pIns->ff_read_packet_impl(buf, buf_size);

	return 0;
}

int apFormatConverter::ff_read_packet_impl(uint8_t *buf, int buf_size)
{
	int to_read = buf_size;
	if (m_indata_len - m_indata_offset < to_read)
		to_read = m_indata_len - m_indata_offset;

	memcpy(buf, m_indata + m_indata_offset, to_read);
	m_indata_offset += to_read;
	LOGD("apFormatConverter: read_packet %d", to_read);
	return to_read;
}

int64_t apFormatConverter::ff_seek_packet(void *opaque, int64_t offset, int whence)
{
#ifdef _MSC_VER
	LOGI("apFormatConverter: seek_packet offset %I64d, whence %d", offset, whence);
#else
	LOGI("apFormatConverter: seek_packet offset %lld, whence %d", offset, whence);
#endif
	
	apFormatConverter *pIns = (apFormatConverter *)opaque;
	if (pIns)
		return pIns->ff_seek_packet_impl(offset, whence);

	return 0;
}

int64_t apFormatConverter::ff_seek_packet_impl(int64_t offset, int whence)
{
	if (AVSEEK_SIZE == whence) {
		int64_t size = m_indata_len;
#ifdef _MSC_VER
		LOGI("AVSEEK_SIZE: filesize %I64d", size);
#else
		LOGI("AVSEEK_SIZE: filesize %lld", size);
#endif
		return (int64_t)size;
	}
	else if (AVSEEK_FORCE == whence) {
		LOGW("AVSEEK_FORCE");
	}

	if (offset > (int64_t)m_indata_len)
		return -1;

	m_indata_offset = (int)offset;
	return offset;
}

int apFormatConverter::ff_write_packet(void *opaque, uint8_t *buf, int buf_size)
{
	apFormatConverter *pIns = (apFormatConverter *)opaque;
	if (pIns)
		return pIns->ff_write_packet_impl(buf, buf_size);

	return 0;
}

int apFormatConverter::ff_write_packet_impl(uint8_t *buf, int buf_size)
{
	if (m_outdata) {
		int towrite = buf_size;
		if (m_outdata_len - m_outdata_offset < towrite)
			towrite = m_outdata_len - m_outdata_offset;
		memcpy(m_outdata + m_outdata_offset, buf, towrite);
		m_outdata_offset += towrite;
		return towrite;
	}

	return 0;
}

bool apFormatConverter::convert(uint8_t* from, int from_size, uint8_t *to, int *to_size, int process_timestamp, int first_seg)
{
	LOGI("convert() flv %p, size %d, process_timestamp %d, first_seg %d", from, from_size, process_timestamp, first_seg);

	if (process_timestamp && first_seg)
		apFormatConverter::m_start_pts = INIT_PTS;

	bool result = false;
	int ret;
	int video_stream_idx = -1;
	AVStream *video_stream = NULL;
	int audio_stream_idx = -1;

	// Step1: output
	m_ifmt_ctx = avformat_alloc_context();
    AVIOInterruptCB cb = {interrupt_l, this};
    m_ifmt_ctx->interrupt_callback = cb;

	m_indata = from;
	m_indata_len = from_size;
	m_indata_offset = 0;

	m_in_pb_buf	= (unsigned char *)av_mallocz(PB_BUF_SIZE);
	m_in_pb		= avio_alloc_context(m_in_pb_buf, PB_BUF_SIZE, 0, this, ff_read_packet,
		NULL, ff_seek_packet);
	if (!m_in_pb) {
		LOGE("failed to create input pb");
		goto end;
	}

	m_ifmt_ctx->pb = m_in_pb;

	m_in_fmt = av_find_input_format("flv");
	if (!m_in_fmt) {
		LOGE("Could not find MPEG-TS demuxer.");
		goto end;
	}

	if (avformat_open_input(&m_ifmt_ctx, "", m_in_fmt, NULL) != 0) {
        LOGE("failed to open flv");
        goto end;
	}

	// Retrieve stream information after disable variant streams, like m3u8
	if (avformat_find_stream_info(m_ifmt_ctx, NULL) < 0) {
        LOGE("failed to avformat_find_stream_info");
        goto end;
	}

	av_dump_format(m_ifmt_ctx, 0, "", 0);
	LOGI("input file opened");

	// Step2: output
	m_out_fmt = av_guess_format("mpegts", NULL, NULL);
    if (!m_out_fmt) {
        LOGE("FFMPEG fmt init error");
		goto end;
    }

    m_ofmt_ctx = avformat_alloc_context();
    if (!m_ofmt_ctx) {
        LOGE("FFMPEG oc init error.");
		goto end;
    }
    m_ofmt_ctx->oformat = m_out_fmt;

	m_out_pb_buf = (unsigned char *)av_mallocz(PB_BUF_SIZE);
	m_out_pb = avio_alloc_context(m_out_pb_buf, PB_BUF_SIZE, 1, this, 
		NULL, ff_write_packet, NULL);
	if(!m_out_pb){
		LOGE("failed to create otuput pb");
		goto end;
	}
	m_ofmt_ctx->pb = m_out_pb;

	for (int i = 0; i < (int)m_ifmt_ctx->nb_streams; i++) {
        AVStream *in_stream = m_ifmt_ctx->streams[i];
		if (in_stream->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
			video_stream_idx = i;
			video_stream = in_stream;
		}
		else if (in_stream->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
			audio_stream_idx = i;
		}
		else {
			LOGI("useless stream: #%d %d", i, in_stream->codec->codec_type);
			continue;
		}

		AVStream *out_stream = avformat_new_stream(m_ofmt_ctx, in_stream->codec->codec);
		if (!out_stream) {
			LOGE("failed allocating output stream");
			goto end;
		}

		ret = avcodec_copy_context(out_stream->codec, in_stream->codec);
		if (ret < 0) {
			LOGE("failed to copy context from input to output stream codec context");
			goto end;
		}
		if (m_ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
			out_stream->codec->flags |= CODEC_FLAG_GLOBAL_HEADER;
    }

	m_pBsfc_h264 = av_bitstream_filter_init("h264_mp4toannexb");
	if (!m_pBsfc_h264) {
		LOGE("Could not aquire h264_mp4toannexb filter");
		goto end;
	}

	av_dump_format(m_ofmt_ctx, 0, "", 1);
	LOGI("output file opened");

	m_outdata = to;
	m_outdata_len = *to_size;
	m_outdata_offset = 0;

	// step3 begin conversion job
	LOGI("begin convert job...");
	ret = avformat_write_header(m_ofmt_ctx, NULL);
    if (ret < 0) {
        LOGE("Error occurred when opening output file");
        goto end;
    }

	AVPacket pkt;
	av_init_packet(&pkt);
	pkt.size = 0;
	pkt.data = NULL;

	while (1) {
		AVStream *in_stream, *out_stream;

		ret = av_read_frame(m_ifmt_ctx, &pkt);
		if (ret < 0)
			break;

		if (pkt.stream_index != video_stream_idx && 
			pkt.stream_index != audio_stream_idx)
		{
			av_free_packet(&pkt);
			continue;
		}

		in_stream  = m_ifmt_ctx->streams[pkt.stream_index];
		out_stream = m_ofmt_ctx->streams[pkt.stream_index];

		/* copy packet */
		pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base, (AVRounding)(AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base, (AVRounding)(AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
		pkt.pos = -1;

		if (apFormatConverter::m_start_pts == INIT_PTS)
			apFormatConverter::m_start_pts = pkt.pts;

		if (process_timestamp) {
			pkt.pts -= apFormatConverter::m_start_pts;
			pkt.dts -= apFormatConverter::m_start_pts;
		}

		if (pkt.stream_index == video_stream_idx) {
			// Apply MP4 to H264 Annex B filter on buffer
			//int origin_size = m_sample_pkt->size;
			int isKeyFrame = pkt.flags & AV_PKT_FLAG_KEY;
			av_bitstream_filter_filter(m_pBsfc_h264, video_stream->codec, NULL, &pkt.data, &pkt.size, 
				pkt.data, pkt.size, isKeyFrame);
		}
		ret = av_interleaved_write_frame(m_ofmt_ctx, &pkt);
		if (ret < 0) {
			LOGE("Error muxing packet");
			goto end;
		}
		av_free_packet(&pkt);
	}

	av_write_trailer(m_ofmt_ctx);
	result = true; // all done!

end:
    if (m_ifmt_ctx)
		avformat_close_input(&m_ifmt_ctx);

	if (m_ofmt_ctx) {
		/* close output */
		if (m_ofmt_ctx && !(m_out_fmt->flags & AVFMT_NOFILE)) {
			m_ofmt_ctx->pb->opaque = NULL;//hard code,need fix
			avio_close(m_ofmt_ctx->pb);
		}
		avformat_free_context(m_ofmt_ctx);
	}

	if (m_pBsfc_h264) {
		av_bitstream_filter_close(m_pBsfc_h264);
		m_pBsfc_h264 = NULL;
	}

	if (result) {
		*to_size = m_outdata_offset;
		LOGI("conversion done! output file size %d", m_outdata_offset);
	}
	return result;
}
