/*
 * HEVC decoding using the Strongene Lentoid HEVC decoder
 * Copyright (C) 2013 Strongene Inc.
 * James.DF <service@strongene.com>
 */


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "../libavutil/common.h"
#include "../libavutil/imgutils.h"
#include "../libavutil/avassert.h"
#include "../libavutil/timestamp.h"
#include "avcodec.h"
#include "lenthevcdec.h"

typedef struct LentHEVCDecContext {
	lenthevcdec_ctx	dec;
	int		threads;
	int             length_size;
	uint8_t*        temp_buf;
	int             temp_buf_size;
	int		drop_ref;
	uint8_t*	reframe_buf;
	int		reframe_buf_size;
	int		reframe_buf_idx;
	int		reframe_buf_offset;
} LentHEVCDecContext;

static const uint8_t start_code[] = {0x00, 0x00, 0x01};

enum hevc_nal_unit_type_e
{
	NAL_UNIT_CODED_SLICE_TRAIL_N = 0,   // 0
	NAL_UNIT_CODED_SLICE_TRAIL_R,   // 1

	NAL_UNIT_CODED_SLICE_TSA_N,     // 2
	NAL_UNIT_CODED_SLICE_TLA,       // 3   // Current name in the spec: TSA_R

	NAL_UNIT_CODED_SLICE_STSA_N,    // 4
	NAL_UNIT_CODED_SLICE_STSA_R,    // 5

	NAL_UNIT_CODED_SLICE_RADL_N,    // 6
	NAL_UNIT_CODED_SLICE_DLP,       // 7 // Current name in the spec: RADL_R

	NAL_UNIT_CODED_SLICE_RASL_N,    // 8
	NAL_UNIT_CODED_SLICE_TFD,       // 9 // Current name in the spec: RASL_R

	NAL_UNIT_RESERVED_10,
	NAL_UNIT_RESERVED_11,
	NAL_UNIT_RESERVED_12,
	NAL_UNIT_RESERVED_13,
	NAL_UNIT_RESERVED_14,
	NAL_UNIT_RESERVED_15,

	NAL_UNIT_CODED_SLICE_BLA,       // 16   // Current name in the spec: BLA_W_LP
	NAL_UNIT_CODED_SLICE_BLANT,     // 17   // Current name in the spec: BLA_W_DLP
	NAL_UNIT_CODED_SLICE_BLA_N_LP,  // 18
	NAL_UNIT_CODED_SLICE_IDR,       // 19  // Current name in the spec: IDR_W_DLP
	NAL_UNIT_CODED_SLICE_IDR_N_LP,  // 20
	NAL_UNIT_CODED_SLICE_CRA,       // 21
	NAL_UNIT_RESERVED_22,
	NAL_UNIT_RESERVED_23,

	NAL_UNIT_RESERVED_24,
	NAL_UNIT_RESERVED_25,
	NAL_UNIT_RESERVED_26,
	NAL_UNIT_RESERVED_27,
	NAL_UNIT_RESERVED_28,
	NAL_UNIT_RESERVED_29,
	NAL_UNIT_RESERVED_30,
	NAL_UNIT_RESERVED_31,

	NAL_UNIT_VPS,                   // 32
	NAL_UNIT_SPS,                   // 33
	NAL_UNIT_PPS,                   // 34
	NAL_UNIT_ACCESS_UNIT_DELIMITER, // 35
	NAL_UNIT_EOS,                   // 36
	NAL_UNIT_EOB,                   // 37
	NAL_UNIT_FILLER_DATA,           // 38
	NAL_UNIT_SEI,                   // 39 Prefix SEI
	NAL_UNIT_SEI_SUFFIX,            // 40 Suffix SEI

	NAL_UNIT_RESERVED_41,
	NAL_UNIT_RESERVED_42,
	NAL_UNIT_RESERVED_43,
	NAL_UNIT_RESERVED_44,
	NAL_UNIT_RESERVED_45,
	NAL_UNIT_RESERVED_46,
	NAL_UNIT_RESERVED_47,
	NAL_UNIT_UNSPECIFIED_48,
	NAL_UNIT_UNSPECIFIED_49,
	NAL_UNIT_UNSPECIFIED_50,
	NAL_UNIT_UNSPECIFIED_51,
	NAL_UNIT_UNSPECIFIED_52,
	NAL_UNIT_UNSPECIFIED_53,
	NAL_UNIT_UNSPECIFIED_54,
	NAL_UNIT_UNSPECIFIED_55,
	NAL_UNIT_UNSPECIFIED_56,
	NAL_UNIT_UNSPECIFIED_57,
	NAL_UNIT_UNSPECIFIED_58,
	NAL_UNIT_UNSPECIFIED_59,
	NAL_UNIT_UNSPECIFIED_60,
	NAL_UNIT_UNSPECIFIED_61,
	NAL_UNIT_UNSPECIFIED_62,
	NAL_UNIT_UNSPECIFIED_63,
	NAL_UNIT_INVALID,
};

/* read integer from memory with network byte order */
/* max size is 4, means read 32bit integer */
static uint32_t mem_read_int(void* addr, size_t size)
{
	uint8_t* p = (uint8_t*)addr;
	uint32_t ret = 0;
	av_assert0(size <= 4);
	for ( ; size > 0; size-- ) {
		ret = (ret << 8) | *p++;
	}
	return ret;
}

/* convert length prefix to start code */
/* if dst is NULL, convertion operate in src buffer, otherwise convert to dst buffer */
/* return destination data length, or negative if failed */
static int length_prefix_to_start_code(size_t length_size,
				       void* src, size_t src_size,
				       void* dst, size_t dst_size)
{
	uint8_t *s = (uint8_t*)src, *s_end = s + src_size;
	uint8_t *d_start = (uint8_t*)dst, *d = d_start, *d_end = d + dst_size;
	uint32_t length;
	if ( NULL == dst ) {/* inplace operation */
		if ( length_size < sizeof(start_code) ) {
			/* inplace operation is impossible, */
			/* length size less than start code size */
			return -1;
		}
		d_start = d = s;
		d_end = s_end;
	}
	while ( (s + length_size) < s_end ) {
		if ( (d + sizeof(start_code)) > d_end )
			return -2;/* dst buffer too small */
		length = mem_read_int(s, length_size);
		/* write start code */
		if ( NULL == dst ) {
			memset(d, 0, length_size);
			d += length_size - sizeof(start_code);
		}
		memcpy(d, start_code, sizeof(start_code));
		s += length_size;
		d += sizeof(start_code);
		/* write data */
		if ( (s + length) > s_end )
			length = s_end - s;
		if ( (d + length) > d_end )
			return -2;/* dst buffer too small */
		if ( NULL != dst ) {
			memcpy(d, s, length);
		}
		s += length;
		d += length;
	}
	return (d - d_start);
}

static int get_nal(void* bs_buf, size_t bs_len, int *start_ptr, int *len_ptr, enum hevc_nal_unit_type_e *type_ptr)
{
	uint8_t *buf;
	int start, start_next;
	if ( NULL != start_ptr ) *start_ptr = 0;
	if ( NULL != len_ptr ) *len_ptr = 0;
	if ( NULL != type_ptr ) *type_ptr = NAL_UNIT_INVALID;
	buf = (uint8_t*)bs_buf;
	// find start code
	for ( start = 0; start <= (bs_len - sizeof(start_code)); start++ ) {
		if ( memcmp(buf + start, start_code, sizeof(start_code)) == 0 )
			break;
	}
	if ( start > (bs_len - sizeof(start_code)) )
		return -1;
	if ( NULL != start_ptr )
		*start_ptr = start;
	if ( NULL != type_ptr ) {
		if ( bs_len < (start + sizeof(start_code) + 1) )
			return -2;
		*type_ptr = (enum hevc_nal_unit_type_e)((buf[start + sizeof(start_code)] >> 1) & 0x3f);
	}
	// find next start code
	if ( NULL != len_ptr ) {
		for ( start_next = start + sizeof(start_code); start_next <= (bs_len - sizeof(start_code)); start_next++ ) {
			if ( memcmp(buf + start_next, start_code, sizeof(start_code)) == 0 )
				break;
		}
		if ( start_next > (bs_len - sizeof(start_code)) )
			start_next = bs_len;
		*len_ptr = start_next - start;
	}
	return 0;
}

/* input bitstream and output one frame */
/* return -1 if error occured; */
/* return  0 if no frame output; */
/* return  1 if one frame output and no more frame can be output; */
/* return  2 if one frame output and have more frame can be output(not implement) */
#define FRAME_FLAG_KEY		0x01/* [output flag] return frame is a key-frame */
#define FRAME_FLAG_NOREF	0x02/* [output flag] return frame is a non-reference frame */
#define FRAME_FLAG_FLUSH	0x80/* [input flag] force to flush the remain data if next AU delimiter not found */
static int reframe(AVCodecContext *avctx, void* data, int size,
		   uint8_t** frame, int* frame_size, int* frame_flags)
{
	uint8_t *pos;
	int expect_buf_size, remain, is_no_ref, is_key_frame;
	int nal_start, nal_len, flush;
	enum hevc_nal_unit_type_e nal_type;
	LentHEVCDecContext *lent;
	if ( size < 0 )
		return -1;
	lent = (NULL != avctx) ? avctx->priv_data : NULL;
	if ( NULL == lent )
		return -1;
	flush = (*frame_flags & FRAME_FLAG_FLUSH) != 0;
	*frame = NULL;
	*frame_size = 0;
	*frame_flags = 0;
	/* reframe mode decide */
	if ( 0 == lent->reframe_buf_size ) {
		if ( 0 == size )/* flush in uncertain status */
			return 0;
		if ( get_nal(data, size, NULL, NULL, &nal_type) >= 0 &&
		     NAL_UNIT_ACCESS_UNIT_DELIMITER == nal_type ) {
			lent->reframe_buf = av_malloc(size);
			if ( NULL == lent->reframe_buf )
				return -1;
			lent->reframe_buf_size = size;
		} else {
			lent->reframe_buf_size = -1;/* AU delimiter not found, use bypass mode */
		}
	}
	/* bypass mode */
	if ( lent->reframe_buf_size < 0 ) {
		*frame = (uint8_t*)data;
		*frame_size = size;
	}
	/* rebuild mode */
	else {
		/* remove prevoius frame */
		av_assert0(lent->reframe_buf_idx >= lent->reframe_buf_offset);
		if ( lent->reframe_buf_offset > 0 ) {
			remain = lent->reframe_buf_idx - lent->reframe_buf_offset;
			if ( remain > 0 )
				memmove(lent->reframe_buf, lent->reframe_buf + lent->reframe_buf_offset, remain);
			lent->reframe_buf_idx = remain;
			lent->reframe_buf_offset = 0;
		}
		/* copy packet to reframe buffer */
		expect_buf_size = lent->reframe_buf_idx + size;
		if ( expect_buf_size > lent->reframe_buf_size ) {
			lent->reframe_buf = av_realloc_f(lent->reframe_buf, expect_buf_size, 1);
			if ( NULL == lent->reframe_buf ) {
				lent->reframe_buf_size = 0;
				return -1;
			}
			lent->reframe_buf_size = expect_buf_size;
		}
		if ( size > 0 )
			memcpy(lent->reframe_buf + lent->reframe_buf_idx, data, size);
		lent->reframe_buf_idx += size;
		/* find first AU delimiter */
		pos = lent->reframe_buf;
		remain = lent->reframe_buf_idx;
		if ( get_nal(pos, remain, &nal_start, &nal_len, &nal_type) < 0 )
			return 0;
		*frame = pos;
		if ( NAL_UNIT_ACCESS_UNIT_DELIMITER == nal_type )
			*frame  = pos + nal_start;
		pos    += nal_start + nal_len;
		remain -= nal_start + nal_len;
		/* find next AU delimiter */
		while ( remain > 0 && get_nal(pos, remain, &nal_start, &nal_len, &nal_type) >= 0 ) {
			if ( NAL_UNIT_ACCESS_UNIT_DELIMITER == nal_type )
				break;
			pos    += nal_start + nal_len;
			remain -= nal_start + nal_len;
		}
		if ( !(remain > 0 && NAL_UNIT_ACCESS_UNIT_DELIMITER == nal_type) ) {/* next AU delimiter not found */
			if ( flush ) {
				pos += remain;/* flush remain data */
				remain = 0;
			} else {
				return 0;/* wait more data */
			}
		}
		*frame_size = pos - *frame;
		lent->reframe_buf_offset = pos - lent->reframe_buf;
	}
	/* find frame flags */
	pos = *frame;
	remain = *frame_size;
	is_no_ref = 1;
	is_key_frame = 0;
	while ( remain > 0 && get_nal(pos, remain, &nal_start, &nal_len, &nal_type) >= 0 ) {
	  if ( NAL_UNIT_CODED_SLICE_TRAIL_N != nal_type &&
	       NAL_UNIT_ACCESS_UNIT_DELIMITER != nal_type &&
	       NAL_UNIT_SEI != nal_type &&
	       NAL_UNIT_SEI_SUFFIX != nal_type )
	    is_no_ref = 0;
	  if ( NAL_UNIT_CODED_SLICE_BLA      == nal_type ||
	       NAL_UNIT_CODED_SLICE_BLANT    == nal_type ||
	       NAL_UNIT_CODED_SLICE_BLA_N_LP == nal_type ||
	       NAL_UNIT_CODED_SLICE_IDR      == nal_type ||
	       NAL_UNIT_CODED_SLICE_IDR_N_LP == nal_type ||
	       NAL_UNIT_CODED_SLICE_CRA      == nal_type ) {
	    is_key_frame = 1;
	  }
	  pos    += nal_start + nal_len;
	  remain -= nal_start + nal_len;
	}
	*frame_flags = (is_no_ref ? FRAME_FLAG_NOREF : 0) | (is_key_frame ? FRAME_FLAG_KEY : 0);
	return (*frame_size > 0 ) ? 1 : 0;
}

/* data contain "avcC" box data */
static int lent_hevc_dec_decode_extradata(AVCodecContext *avctx,
					  void* data, int size)
{
	int bytes_used, got_frame, w, h, stride[3];
	int64_t pts;
	uint8_t* pixels[3];
	uint8_t *p, *p_end, *seq_hdr_buf;
	int seq_hdr_buf_size, seq_hdr_len, sps_count, pps_count, nal_len, i;
	LentHEVCDecContext *lent = (NULL != avctx) ? avctx->priv_data : NULL;
	lenthevcdec_ctx	dec = (NULL != lent) ? lent->dec : NULL;

	if ( NULL == avctx || NULL == dec )
		return AVERROR_EXTERNAL;
	if ( size < 3 )
		return AVERROR_INVALIDDATA;

	p = (uint8_t*)data;
	p_end = p + size;
	seq_hdr_buf = NULL;
	seq_hdr_len = 0;

	/* for NALUs */
	if ( p[0] == 0 && p[1] == 0 && (p[2] == 1 || (size >= 4 && p[2] == 0 && p[3] == 1)) ) {
		seq_hdr_buf = p;
		seq_hdr_len = size;
	}
	/* for AVCDecoderConfigurationRecord in AVCc box */
	else if ( p[0] == 1 ) { /* configurationVersion==1 */

		if ( size < 7 )
			return AVERROR_INVALIDDATA;
		seq_hdr_buf_size = size * 2;
		seq_hdr_buf = (uint8_t*)av_malloc(seq_hdr_buf_size);
		if ( NULL == seq_hdr_buf )
			return AVERROR_EXTERNAL;
		seq_hdr_len = 0;

		/* parse length size */
		lent->length_size = (p[4] & 0x03) + 1;
	
		/* extract SPS from avcC */
		sps_count = p[5] & 0x1F;
		p += 6;
		for ( i = 0; i < sps_count && (p + 2) < p_end; i++ ) {
			/* get nal length */
			nal_len = ((int)p[0] << 8) | ((int)p[1]);
			if ( (p + nal_len) > p_end ||
			     (seq_hdr_len + sizeof(start_code) + nal_len) > seq_hdr_buf_size )
				break;/* buffer overflow */
			/* write nal start code */
			memcpy(seq_hdr_buf + seq_hdr_len, start_code, sizeof(start_code));
			p += 2;
			seq_hdr_len += sizeof(start_code);
			/* write nal payload */
			memcpy(seq_hdr_buf + seq_hdr_len, p, nal_len);
			p += nal_len;
			seq_hdr_len += nal_len;
		}
		if ( i < sps_count ) {
			av_freep(&seq_hdr_buf);
			return AVERROR_INVALIDDATA;
		}
	
		/* extract PPS from avcC */
		pps_count = *p++;
		for ( i = 0; i < pps_count && (p + 2) < p_end; i++ ) {
			/* get nal length */
			nal_len = ((int)p[0] << 8) | ((int)p[1]);
			if ( (p + nal_len) > p_end ||
			     (seq_hdr_len + sizeof(start_code) + nal_len) > seq_hdr_buf_size )
				break;/* buffer overflow */
			/* write nal start code */
			memcpy(seq_hdr_buf + seq_hdr_len, start_code, sizeof(start_code));
			p += 2;
			seq_hdr_len += sizeof(start_code);
			/* write nal payload */
			memcpy(seq_hdr_buf + seq_hdr_len, p, nal_len);
			p += nal_len;
			seq_hdr_len += nal_len;
		}
		if ( i < pps_count ) {
			av_freep(&seq_hdr_buf);
			return AVERROR_INVALIDDATA;
		}
	} else {
		av_assert1( 0 );
		av_log(avctx, AV_LOG_WARNING,
		       "extra data type unknown! [%02x %02x %02x ...]\n",
		       p[0], p[1], p[2]);
	}
	if ( seq_hdr_len <= 0 )
		return 0;
	
	/* decode sequence header */
	w = h = 0;
	bytes_used = lenthevcdec_decode_frame(dec, seq_hdr_buf, seq_hdr_len, 0,
					      &got_frame, &w, &h, stride,
					      (void**)pixels, &pts);
	if ( seq_hdr_buf != (uint8_t*)data )
		av_freep(&seq_hdr_buf);
	if ( bytes_used < 0 && 0 == w && 0 == h ) {
		/* liblenthevcdec expect whole frame input one time, in this case, we */
		/* 	only sequence header, no any slice data, so call will be failed, */
		/* 	but width and height maybe update right */
		return AVERROR_INVALIDDATA;
	}
	if ( 0 != w && 0 != h && (w != avctx->width || h != avctx->height) ) {
		av_log(avctx,AV_LOG_INFO, "dimension change! %dx%d -> %dx%d\n",
		       avctx->width, avctx->height, w, h);
		if ( av_image_check_size(w, h, 0, avctx) )
			return AVERROR_INVALIDDATA;
		avcodec_set_dimensions(avctx, w, h);
	}
	return 0;
}


static av_cold int lent_hevc_dec_init(AVCodecContext *avctx)
{
	int32_t compatibility, ret;
	LentHEVCDecContext *lent = avctx->priv_data;
	if ( AV_CODEC_ID_HEVC_HM91 == avctx->codec_id )
		compatibility = 91; /* HM9.1 */
	else if ( AV_CODEC_ID_HEVC_HM10 == avctx->codec_id )
		compatibility = 100; /* HM10.0 */
	else
		compatibility = INT32_MAX; /* use last version */
	lent->length_size = 0; /* means user start code, we donot need convert */
	lent->temp_buf = NULL;
	lent->temp_buf_size = 0;
	lent->reframe_buf = NULL;
	lent->reframe_buf_size = 0;
	lent->reframe_buf_idx = 0;
	lent->reframe_buf_offset = 0;
	lent->threads = (avctx->thread_count < 8) ? avctx->thread_count : 8;
	if ( lent->threads == 0 ) { /* Auto threads */
		int cores = av_cpu_count();
		lent->threads = (cores > 1) ? ((cores < 4) ? (cores * 2) : 8) : 1;
		av_log(avctx,AV_LOG_INFO, "%d cpu core(s) detected, use %d thread(s)\n", cores, lent->threads);
	}
	lent->dec = lenthevcdec_create(lent->threads, compatibility, NULL);
	if ( NULL == lent->dec ) {
		av_log(avctx,AV_LOG_ERROR, "failed to create lenthevcdec");
		return AVERROR_EXTERNAL;
	}
	if ( avctx->extradata_size > 0 && avctx->extradata ) {
		ret = lent_hevc_dec_decode_extradata(avctx, avctx->extradata,
						     avctx->extradata_size);
		if ( ret < 0 ) {
			av_log(avctx,AV_LOG_ERROR, "failed to lent_hevc_dec_decode_extradata");
			lenthevcdec_destroy(lent->dec);
			return ret;
		}
	}
	avctx->pix_fmt = PIX_FMT_YUV420P;
	return 0;
}

static av_cold int lent_hevc_dec_close(AVCodecContext *avctx)
{
	LentHEVCDecContext *lent = avctx->priv_data;
	if ( NULL != lent->dec ) {
		lenthevcdec_destroy(lent->dec);
		lent->dec = NULL;
		if ( NULL != lent->temp_buf )
			av_freep(&lent->temp_buf);
		if ( NULL != lent->reframe_buf )
			av_freep(&lent->reframe_buf);
	}
	return 0;
 }

static int lent_hevc_dec_decode(AVCodecContext *avctx, void *data,
				int *got_frame, AVPacket *avpkt)
{
	LentHEVCDecContext *lent = avctx->priv_data;
	AVFrame *pict = data;
	uint8_t *buf = avpkt->data;
	int buf_size = avpkt->size;
	uint8_t *frame = NULL;
	int frame_size = 0, frame_flags = 0;
	int ret, frame_flush, bytes_used, w, h;
	int64_t pts;
	if ( NULL == lent->dec )
		return -1;
	*got_frame = 0;
	/* rebuild frame from input bitstream */
	/* first, we flush the inner reframe buffer for big-packet that contain multi-frame in on packet */
	frame_flags = 0;
	ret = reframe(avctx, NULL, 0, &frame, &frame_size, &frame_flags);
	if ( ret > 0 ) {
		/* flush inner reframe buffer */
		frame_flush = 1;
	} else {
		frame_flush = 0;
		/* convert length prefix to start code */
		if ( buf_size > 0 && 0 != lent->length_size ) {
			int len_size = lent->length_size;
			av_assert0(len_size <= 4);
			if ( len_size < 3 ) { /* we must expan data buffer */
				int expand_size = buf_size * 3;
				if ( NULL == lent->temp_buf || lent->temp_buf_size < expand_size ) {
					lent->temp_buf = av_realloc_f(lent->temp_buf, expand_size, 1);
					if ( NULL == lent->temp_buf ) {
						lent->temp_buf_size = 0;
						return -1;
					}
					lent->temp_buf_size = expand_size;
				}
				buf_size = length_prefix_to_start_code(len_size, buf, buf_size,
								       lent->temp_buf, lent->temp_buf_size);
				if ( buf_size < 0 )
					return -1;
				buf = lent->temp_buf;
			} else { /* replace length prefix with start code */
				buf_size = length_prefix_to_start_code(len_size, buf, buf_size, NULL, 0);
				if ( buf_size < 0 )
					return -1;
			}
		}
		/* second, we use input packet data to rebuild frame */
		if ( 0 == buf_size )/* ffmpeg flush decoder */
			frame_flags = FRAME_FLAG_FLUSH;/* if next AU delimiter not found, flush remain data */
		ret = reframe(avctx, buf, buf_size, &frame, &frame_size, &frame_flags);
		if ( ret < 0 )
			return ret;
		if ( 0 == ret && NULL != buf ) {
			/* no frame output, wait more data intput */
			return avpkt->size;
		}
	}

	/* discard */
	if ( buf_size > 0 && (avctx->skip_frame >= AVDISCARD_NONREF || lent->drop_ref) ) {
		int is_no_ref = (frame_flags & FRAME_FLAG_NOREF) != 0;
		int is_key_frame = (frame_flags & FRAME_FLAG_KEY) != 0;
		if ( lent->drop_ref && !is_key_frame ) {
			av_log(avctx, AV_LOG_VERBOSE, "skip nonkey frame because reference frame droped. dts:%s pts:%s\n",
			       av_ts2str(avpkt->dts), av_ts2str(avpkt->pts));
			return avpkt->size;
		}
		if ( is_key_frame )
			lent->drop_ref = 0;
		if ( avctx->skip_frame >= AVDISCARD_NONREF && is_no_ref ) {
			av_log(avctx, AV_LOG_VERBOSE, "skip no reference frame. dts:%s pts:%s\n",
			       av_ts2str(avpkt->dts), av_ts2str(avpkt->pts));
			return avpkt->size;
		}
		if ( (avctx->skip_frame == AVDISCARD_NONKEY && !is_key_frame) ||
		     (avctx->skip_frame == AVDISCARD_ALL) ) {
			lent->drop_ref = 1;
			av_log(avctx, AV_LOG_VERBOSE, "skip %s frame. dts:%s pts:%s\n",
			       is_key_frame ? "key" : "reference", av_ts2str(avpkt->dts), av_ts2str(avpkt->pts));
			return avpkt->size;
		}
	}
	/* decode one frame */
	bytes_used = lenthevcdec_decode_frame(lent->dec, frame, frame_size, avpkt->pts,
					      got_frame, &w, &h, pict->linesize,
					      (void**) pict->data, &pts);
	av_log(avctx, AV_LOG_DEBUG, "decode frame: %d bytes, used %d, got_frame %d, w %d, h %d, pts %s\n",
	       frame_size, bytes_used, *got_frame, w, h, av_ts2str((*got_frame<=0)?AV_NOPTS_VALUE:pts));
	if ( bytes_used < 0 )
		return AVERROR_INVALIDDATA;
	if ( 0 != w && 0 != h && (w != avctx->width || h != avctx->height) ) {
		av_log(avctx,AV_LOG_INFO, "dimension change! %dx%d -> %dx%d\n",
		       avctx->width, avctx->height, w, h);
		if ( av_image_check_size(w, h, 0, avctx) )
			return AVERROR_INVALIDDATA;
		avcodec_set_dimensions(avctx, w, h);
	}
	if ( 0 ==  bytes_used ) {
		bytes_used = 1; /* avoid infinite loops */
	}
	if ( *got_frame <= 0 ) {
		av_log(avctx, AV_LOG_VERBOSE, "no frame output\n");
		return /*bytes_used*/avpkt->size;
	}
	pict->data[3] = NULL;
	pict->linesize[3] = 0;
	pict->pict_type = AV_PICTURE_TYPE_I;
	pict->key_frame = 1;
	pict->pts = pts;
	pict->pkt_pts = pts;
	av_log(avctx, AV_LOG_DEBUG, "got frame, pts = %s\n", av_ts2str(pict->pts));
	return frame_flush ? 0 : (/* bytes_used */avpkt->size);
}

static void lent_hevc_dec_flush(AVCodecContext *avctx)
{
	LentHEVCDecContext *lent = avctx->priv_data;
	if ( NULL == lent->dec )
		return;
	/* set discontinuity flash */
	lent->drop_ref = 1;
	/* clear reframe buffer */
	lent->reframe_buf_idx = 0;
	lent->reframe_buf_offset = 0;
}


AVCodec ff_liblenthevchm91_decoder = {
	.name		= "liblenthevchm91",
	.type		= AVMEDIA_TYPE_VIDEO,
	.id		= AV_CODEC_ID_HEVC_HM91,
	.priv_data_size	= sizeof(LentHEVCDecContext),
	.init		= lent_hevc_dec_init,
	.close		= lent_hevc_dec_close,
	.decode		= lent_hevc_dec_decode,
	.flush		= lent_hevc_dec_flush,
	.capabilities	= CODEC_CAP_DELAY | CODEC_CAP_AUTO_THREADS,
	.long_name	= NULL_IF_CONFIG_SMALL("Strongene Lentoid HEVC (HM9.1)"),
	.pix_fmts	= (const enum PixelFormat[]){ PIX_FMT_YUV420P, PIX_FMT_NONE },
};

AVCodec ff_liblenthevchm10_decoder = {
	.name		= "liblenthevchm10",
	.type		= AVMEDIA_TYPE_VIDEO,
	.id		= AV_CODEC_ID_HEVC_HM10,
	.priv_data_size	= sizeof(LentHEVCDecContext),
	.init		= lent_hevc_dec_init,
	.close		= lent_hevc_dec_close,
	.decode		= lent_hevc_dec_decode,
	.flush		= lent_hevc_dec_flush,
	.capabilities	= CODEC_CAP_DELAY | CODEC_CAP_AUTO_THREADS,
	.long_name	= NULL_IF_CONFIG_SMALL("Strongene Lentoid HEVC (HM10.0)"),
	.pix_fmts	= (const enum PixelFormat[]){ PIX_FMT_YUV420P, PIX_FMT_NONE },
};

AVCodec ff_liblenthevc_decoder = {
	.name		= "liblenthevc",
	.type		= AVMEDIA_TYPE_VIDEO,
	.id		= AV_CODEC_ID_HEVC,
	.priv_data_size	= sizeof(LentHEVCDecContext),
	.init		= lent_hevc_dec_init,
	.close		= lent_hevc_dec_close,
	.decode		= lent_hevc_dec_decode,
	.flush		= lent_hevc_dec_flush,
	.capabilities	= CODEC_CAP_DELAY | CODEC_CAP_AUTO_THREADS,
	.long_name	= NULL_IF_CONFIG_SMALL("Strongene Lentoid HEVC"),
	.pix_fmts	= (const enum PixelFormat[]){ PIX_FMT_YUV420P, PIX_FMT_NONE },
};
