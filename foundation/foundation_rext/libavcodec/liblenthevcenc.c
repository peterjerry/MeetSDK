/*
 * HEVC encoding using the Strongene Lentoid HEVC encoder
 * Copyright (C) 2013 Strongene Inc.
 * James.DF <service@strongene.com>
 */

#include "config.h"
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#if HAVE_UNISTD_H
#include <unistd.h>
#endif
#if HAVE_IO_H
#include <io.h>
#endif
#include "../libavutil/opt.h"
#include "../libavutil/common.h"
#include "../libavutil/imgutils.h"
#include "../libavutil/avassert.h"
#include "../libavutil/timestamp.h"
#include "avcodec.h"
#include "internal.h"
#include "Lentoid.h"

#define HM_COMPATIBILITY_LAST	120 /* the last version is 120 currently */

typedef struct LentHEVCEncContext {
	AVClass * class;
	LENT_HANDLE enc;
	LENT_param_t param;
	char * preset_name;
	int thread_wpp;
	int disable_sei;
	char * hm_compatibility;
	char * recon_name;
	FILE * recon_file;
	int recon_reorder_size;
	int recon_reorder_len;
	LENT_picture_t * recon_reorder_pics;
} LentHEVCEncContext;


static int fwrite_pic(FILE *fp, LENT_picture_t *pic)
{
	int plane, line, width, height, stride;
	uint8_t *data;
	if ( NULL == fp )
		return 0;
	if ( NULL == pic ||
	     NULL == pic->img.plane[0] || NULL == pic->img.plane[1] || NULL == pic->img.plane[2] ||
	     pic->img.i_width[0] <= 0 || pic->img.i_width[1] <= 0 || pic->img.i_width[2] <= 0 || 
	     pic->img.i_height[0] <= 0 || pic->img.i_height[1] <= 0 || pic->img.i_height[2] <= 0 )
		return AVERROR(EINVAL);
	for ( plane = 0; plane < 3; plane++ ) {
		width = pic->img.i_width[plane];
		height = pic->img.i_height[plane];
		stride = pic->img.i_stride[plane];
		data = pic->img.plane[plane];
		for ( line = 0; line < height; line++ ) {
			fwrite(data, 1, width, fp);
			data += stride;
		}
	}
	return 0;
}

static int pic_cpy(LENT_picture_t *dst, LENT_picture_t *src)
{
	int plane, line, width, height, stride_src, stride_dst;
	uint8_t *data_src, *data_dst;
	if ( NULL == dst || NULL == src ||
	     NULL == dst->img.plane[0] || NULL == dst->img.plane[1] || NULL == dst->img.plane[2] ||
	     NULL == src->img.plane[0] || NULL == src->img.plane[1] || NULL == src->img.plane[2] )
		return AVERROR(EINVAL);
	dst->i_type = src->i_type;
	dst->i_pts = src->i_pts;
	dst->i_dts = src->i_dts;
	for ( plane = 0; plane < 3; plane++ ) {
		width = FFMIN(dst->img.i_width[plane], src->img.i_width[plane]);
		height = FFMIN(dst->img.i_height[plane], src->img.i_height[plane]);
		stride_dst = dst->img.i_stride[plane];
		stride_src = src->img.i_stride[plane];
		data_dst = dst->img.plane[plane];
		data_src = src->img.plane[plane];
		for ( line = 0; line < height; line++ ) {
			memcpy(data_dst, data_src, width);
			data_dst += stride_dst;
			data_src += stride_src;
		}
	}
	return 0;
}

#define PIC_SWAP(pic1, pic2) { LENT_picture_t tmp = *(pic1); *(pic1) = *(pic2); *(pic2) = tmp; }

static int output_recon(AVCodecContext *avctx, LENT_picture_t * pic)
{
	LentHEVCEncContext *lent = avctx->priv_data;
	LENT_picture_t *reorder_pics = lent->recon_reorder_pics;
	const int reorder_size = lent->recon_reorder_size;
	int i, ret, reorder_len = lent->recon_reorder_len;
	// no b frames, output reconstruction picture
	if ( NULL == reorder_pics || 0 == reorder_size ) {
		return fwrite_pic(lent->recon_file, pic);
	}
	// with b frames, reorder reconstruction picture with PTS, and output it
	ret = pic_cpy(&reorder_pics[reorder_len], pic);
	if ( ret < 0 )
		return ret;
	reorder_len++;
	// PTS descending order
	for ( i = reorder_len - 1; i > 0; i-- )
		if ( reorder_pics[i].i_pts > reorder_pics[i - 1].i_pts )
			PIC_SWAP(&reorder_pics[i], &reorder_pics[i - 1]);
	// output picture with mimimum PTS
	ret = 0;
	if ( reorder_len == reorder_size ) {
		ret = fwrite_pic(lent->recon_file, &reorder_pics[reorder_len - 1]);
		av_log(avctx, AV_LOG_DEBUG, "Output reconstruction picture, pts = %s\n",
		       av_ts2str(reorder_pics[reorder_len - 1].i_pts));
		reorder_len--;
	}
	lent->recon_reorder_len = reorder_len;
	return ret;
}

static int flush_recon(AVCodecContext *avctx)
{
	LentHEVCEncContext *lent = avctx->priv_data;
	LENT_picture_t *reorder_pics = lent->recon_reorder_pics;
	int i, ret, reorder_len = lent->recon_reorder_len;
	if ( NULL == reorder_pics || 0 == lent->recon_reorder_size )
		return 0;
	for ( i = reorder_len - 1; i >= 0; i-- ) {
		ret = fwrite_pic(lent->recon_file, &reorder_pics[i]);
		if ( ret < 0 )
			break;
		av_log(avctx, AV_LOG_DEBUG, "Flush reconstruction picture, pts = %s\n",
		       av_ts2str(reorder_pics[i].i_pts));
	}
	lent->recon_reorder_len = 0;
	return ret;
}



static av_cold int lent_hevc_enc_init(AVCodecContext *avctx)
{
	static const char * preset_name_tbl[LENT_PRESET_COUNT] = {
		"ultraslow",	// default
		"slow",
		"medium",
		"fast",
		"ultrafast",
	};
	static const int preset_tbl[LENT_PRESET_COUNT] = {
		LENT_PRESET_ULTRASLOW,	// default
		LENT_PRESET_SLOW,
		LENT_PRESET_MEDIUM,
		LENT_PRESET_FAST,
		LENT_PRESET_ULTRAFAST,
	};
	LentHEVCEncContext *lent = avctx->priv_data;
	char *preset_name = lent->preset_name;
	int idx, preset_idx, preset, compatibility;
	lent->enc = NULL;
	lent->recon_file = NULL;
	lent->recon_reorder_size = 0;
	lent->recon_reorder_len = 0;
	lent->recon_reorder_pics = NULL;
	for ( idx = 0; NULL != preset_name && idx < LENT_PRESET_COUNT; idx++ ) {
		if ( strcasecmp(preset_name, preset_name_tbl[idx]) == 0 )
			break;
	}
	if ( LENT_PRESET_COUNT == idx ) {
		av_log(avctx, AV_LOG_ERROR, "Error setting preset %s.\n", preset_name);
		av_log(avctx, AV_LOG_INFO, "Possible presets:");
		for ( idx = 0; idx < LENT_PRESET_COUNT; idx++ )
			av_log(avctx, AV_LOG_INFO, " %s", preset_name_tbl[idx]);
		av_log(avctx, AV_LOG_INFO, "\n");
		return AVERROR(EINVAL);
	}
	preset_idx = idx;
	preset = preset_tbl[idx];
	LENT_param_default( &lent->param, preset );
	if ( avctx->thread_count == 0 )
		lent->param.i_threads = 1;
	else if ( avctx->thread_count > 0 )
		lent->param.i_threads = avctx->thread_count;
	if ( lent->thread_wpp > 0 ) {
		lent->param.i_threads_wpp = 1;
		lent->param.i_threads_wpp_pic[LENT_PIC_IDR ] = lent->thread_wpp;
		lent->param.i_threads_wpp_pic[LENT_PIC_P   ] = lent->thread_wpp;
		lent->param.i_threads_wpp_pic[LENT_PIC_BREF] = lent->thread_wpp;
		lent->param.i_threads_wpp_pic[LENT_PIC_B   ] = lent->thread_wpp;
	}
	lent->param.b_SEI = (lent->disable_sei == 0);
	if ( AV_CODEC_ID_HEVC_HM91 == avctx->codec_id ) {
		compatibility = lent->param.i_compatibility = 91; /* HM9.1 */
	} else if ( AV_CODEC_ID_HEVC_HM10 == avctx->codec_id ) {
		compatibility = lent->param.i_compatibility = 100; /* HM10.0 */
	} else {
		if ( strcasecmp(lent->hm_compatibility, "last") == 0 ) {
			compatibility = lent->param.i_compatibility = HM_COMPATIBILITY_LAST;
		} else {
			double hm_ver;
			if ( sscanf(lent->hm_compatibility, "%lf", &hm_ver) != 1 ) {
				av_log(avctx, AV_LOG_ERROR, "Error setting HM compatibility '%s'.\n", lent->hm_compatibility);
				return AVERROR(EINVAL);
			}
			compatibility = lent->param.i_compatibility = (int)((hm_ver + 0.05) * 10);
			if ( compatibility < 91 || compatibility > HM_COMPATIBILITY_LAST ) {
				av_log(avctx, AV_LOG_ERROR, "Unsupported HM version '%.1f'.\n", compatibility / 10.0);
				return AVERROR(EINVAL);
			}
		}
	}
	lent->param.i_log_flag = (av_log_get_level() >= AV_LOG_INFO);
	if ( avctx->keyint_min > 0 )
		lent->param.i_idr_min = avctx->keyint_min;
//	if ( avctx->gop_size > 0 )	// to do ...
//		lent->param.i_idr_max = avctx->gop_size;
	lent->param.i_idr_max = lent->param.i_idr_min;	// fix me !!!
	if ( avctx->time_base.num != 0 && avctx->time_base.den != 0 ) {
		lent->param.i_fps_num = avctx->time_base.den;
		lent->param.i_fps_den = avctx->time_base.num;
	}
	lent->param.spatial[0].i_width = avctx->width;
	lent->param.spatial[0].i_height = avctx->height;
	if ( avctx->width % 8 != 0 || avctx->height % 8 != 0 )
		av_log(avctx, AV_LOG_WARNING, "Width(%d) or height(%d) is not a multiple of 8!\n", avctx->width, avctx->height);
	if ( avctx->bit_rate > 0 ) {
		lent->param.rc.i_rc_method = LENT_RC_ABR;
		lent->param.spatial[0].i_bitrate[0] = avctx->bit_rate / 1000;
	}
	if ( avctx->flags & CODEC_FLAG_QSCALE ) {
		lent->param.rc.i_rc_method = LENT_RC_CQP;
		lent->param.spatial[0].i_qp[0] = avctx->global_quality / FF_QP2LAMBDA;
	}
	lent->enc = LENT_encoder_open( &lent->param );
	if ( NULL == lent->enc ) {
		return AVERROR_EXTERNAL;
	}
	if ( lent->param.i_compatibility != compatibility ) {
		av_log(avctx, AV_LOG_ERROR,
		       "Specified version %.1f is not available, the available version is %.1f\n",
		       compatibility/10.0, lent->param.i_compatibility/10.0);
		LENT_encoder_close( lent->enc );
		lent->enc = NULL;
		return AVERROR_EXTERNAL;
	}
	/* test */
	/* lent->param.i_bframe = 5; */
	/* test end */
	avctx->has_b_frames = lent->param.i_bframe ? (lent->param.i_hierach_bframe + 1) : 0;
	avctx->coded_frame = avcodec_alloc_frame();
	if ( NULL == avctx->coded_frame ) {
		av_log(avctx, AV_LOG_ERROR, "Error allocating coded frame\n");
		LENT_encoder_close( lent->enc );
		lent->enc = NULL;
		return AVERROR(ENOMEM);
	}
	if ( avctx->flags & CODEC_FLAG_GLOBAL_HEADER ) {
		int seq_hdr_len = LENT_encoder_header_get( lent->enc, NULL, 0 );
		if ( seq_hdr_len > 0 ) {
			avctx->extradata = av_malloc(seq_hdr_len);
			if ( NULL == avctx->extradata ) {
				av_log(avctx, AV_LOG_ERROR, "Error allocating codec extra data buffer\n");
				LENT_encoder_close( lent->enc );
				lent->enc = NULL;
				return AVERROR(ENOMEM);
			}
			avctx->extradata_size = LENT_encoder_header_get( lent->enc, avctx->extradata, seq_hdr_len );
			av_assert0( avctx->extradata_size == seq_hdr_len );
		}
	}
	// open reconstruct file
	if ( NULL != lent->recon_name && lent->recon_name[0] ) {
		if ( access(lent->recon_name, F_OK) == 0 ) {
			av_log(avctx, AV_LOG_ERROR, "File '%s' already existed!\n", lent->recon_name);
			LENT_encoder_close( lent->enc );
			lent->enc = NULL;
			return AVERROR(EINVAL);
		}
		lent->recon_file = fopen(lent->recon_name, "wb");
		if ( NULL == lent->recon_file ) {
			av_log(avctx, AV_LOG_ERROR, "Couldn't create output recon file: %s\n", strerror(errno));
			LENT_encoder_close( lent->enc );
			lent->enc = NULL;
			return AVERROR(EINVAL);
		}
		if ( lent->param.i_bframe > 0 ) {
			lent->recon_reorder_size = lent->param.i_bframe;
			lent->recon_reorder_pics = av_mallocz(sizeof(LENT_picture_t) * lent->recon_reorder_size);
			if ( NULL == lent->recon_reorder_pics ) {
				av_log(avctx, AV_LOG_ERROR, "Error allocating reconstruction reorder buffer\n");
				LENT_encoder_close( lent->enc );
				lent->enc = NULL;
				return AVERROR(ENOMEM);
			}
			for ( idx = 0; idx < lent->recon_reorder_size; idx++ ) {
				if ( LENT_picture_alloc(&lent->recon_reorder_pics[idx], avctx->width, avctx->height) < 0 ) {
					av_log(avctx, AV_LOG_ERROR, "Error allocating reconstruction picture buffer\n");
					LENT_encoder_close( lent->enc );
					lent->enc = NULL;
					return AVERROR(ENOMEM);
				}
			}
		}
	}
	// info
	av_log(avctx, AV_LOG_VERBOSE, "Lentoid HEVC Encoder initialize successfully\n");
	av_log(avctx, AV_LOG_VERBOSE, "compatibility=HM%.1f, size=%dx%d, preset=%s, thread=%d, wpp=%d(%d), sei=%d, log=%d, idr_peroid=%d, %s=%d%s, bframe=%d, hierach_bframe=%d, delay=%d, recon=%s\n",
	       lent->param.i_compatibility / 10.0, lent->param.spatial[0].i_width, lent->param.spatial[0].i_height,
	       preset_name_tbl[preset_idx], lent->param.i_threads, lent->thread_wpp, lent->param.i_threads_wpp,
	       lent->param.b_SEI, lent->param.i_log_flag, lent->param.i_idr_max,
	       (LENT_RC_CQP == lent->param.rc.i_rc_method) ? "QP" : "bitrate",
	       (LENT_RC_CQP == lent->param.rc.i_rc_method) ? lent->param.spatial[0].i_qp[0] : lent->param.spatial[0].i_bitrate[0],
	       (LENT_RC_CQP == lent->param.rc.i_rc_method) ? "" : "kbps",
	       lent->param.i_bframe, lent->param.i_hierach_bframe, avctx->has_b_frames,
	       lent->recon_name ? lent->recon_name : "null");
	return 0;
}

static av_cold int lent_hevc_enc_close(AVCodecContext *avctx)
{
	LentHEVCEncContext *lent = avctx->priv_data;
	int i;
	if ( NULL != lent->enc )
		LENT_encoder_close( lent->enc );
	if ( NULL != avctx->extradata )
		av_freep(&avctx->extradata);
	if ( NULL != lent->recon_reorder_pics ) {
		flush_recon(avctx);
		for ( i = 0; i < lent->recon_reorder_size; i++ )
			LENT_picture_free(&lent->recon_reorder_pics[i]);
		av_freep(&lent->recon_reorder_pics);
	}
	if ( NULL != lent->recon_file )
		fclose(lent->recon_file);
	if ( NULL != avctx->coded_frame)
		av_freep(&avctx->coded_frame);
	return 0;
}

static int lent_hevc_enc_encode(AVCodecContext *avctx, AVPacket *pkt,
				const AVFrame *frame, int *got_packet)
{
	LentHEVCEncContext *lent = avctx->priv_data;
	LENT_picture_t _pic, *pic = NULL, pic_out;
	LENT_nal_t *nal;
	int nal_count, nal_size, i, ret;
	if ( NULL != frame ) {
		pic = &_pic;
		pic->i_type =
			frame->pict_type == AV_PICTURE_TYPE_I ? LENT_PIC_IDR :
			frame->pict_type == AV_PICTURE_TYPE_P ? LENT_PIC_P :
			frame->pict_type == AV_PICTURE_TYPE_B ? LENT_PIC_B : LENT_PIC_PENDING;
		pic->i_pts = frame->pts;
		pic->i_dts = 0; // pic->i_dts = frame->pkt_dts;	// fix me !
		pic->img.i_width[0] = avctx->width;
		pic->img.i_height[0] = avctx->height;
		pic->img.i_width[1] = pic->img.i_width[2] = pic->img.i_width[0] / 2;
		pic->img.i_height[1] = pic->img.i_height[2] = pic->img.i_height[0] / 2;
		for ( i = 0; i < 3; i++ ) {
			pic->img.i_stride[i] = frame->linesize[i];
			pic->img.plane[i] = frame->data[i];
		}
	}
	nal_count = nal_size = 0;
	memset(&pic_out, 0, sizeof(pic_out));
	if ( !(NULL == frame && !LENT_encoder_encoding(lent->enc)) ) do {
		nal_size = LENT_encoder_encode( lent->enc, &nal, &nal_count, pic, &pic_out );
		if ( nal_size < 0 )
			return -1;
		if ( nal_size > 0 ) {
			if ( (ret = ff_alloc_packet2(avctx, pkt, nal_size)) < 0 )
				return ret;
			memcpy(pkt->data, nal->p_payload, nal_size);
			if ( NULL != lent->recon_file ) {
				ret = output_recon(avctx, &pic_out);
				if ( 0 != ret )
					return ret;
			}
			break;
		}
	} while ( NULL == frame && LENT_encoder_encoding(lent->enc) );	// flush
	pkt->pts = pic_out.i_pts;
	pkt->dts = AV_NOPTS_VALUE; // pic_out.i_dts; // fix me !
	if ( LENT_PIC_IDR == pic_out.i_type )
		pkt->flags |= AV_PKT_FLAG_KEY;
	*got_packet = (nal_size > 0) ? 1 : 0;
	if ( nal_size > 0 )
		av_log(avctx, AV_LOG_DEBUG, "got packet, pts = %s, dts = %s, size = %d\n", av_ts2str(pkt->pts), av_ts2str(pkt->dts), nal_size);
	return 0;
}


#define OFFSET(x) offsetof(LentHEVCEncContext, x)
#define VE AV_OPT_FLAG_VIDEO_PARAM | AV_OPT_FLAG_ENCODING_PARAM
static const AVOption options[] = {
	{ "preset", "Set the encoding preset (\"ultraslow\", \"slow\", \"medium\", \"fast\", \"ultrafast\")",
	  OFFSET(preset_name), AV_OPT_TYPE_STRING, { .str = "ultraslow" }, 0, 0, VE },
	{ "wpp",    "Wavefront Parallel Processing",
	  OFFSET(thread_wpp), AV_OPT_TYPE_INT, { .i64 = 0 }, 0, 32, VE },
	{ "disable_sei", "Disable output SEI NALU",
	  OFFSET(disable_sei), AV_OPT_TYPE_INT, { .i64 = 0 }, 0, 1, VE },
	{ "HM_compatibility", "Compatible version of HM, \"9.1\" for HM9.1, \"12\" for HM12, Etc., \"last\" specify the latest version supported",
	  OFFSET(hm_compatibility), AV_OPT_TYPE_STRING, { .str = "last" }, 0, 0, VE },
	{ "recon", "Output reconstructed picture",
	  OFFSET(recon_name), AV_OPT_TYPE_STRING, { 0 }, 0, 0, VE },
	{ NULL },
};

static const AVClass lenthevc_enc_class = {
	.class_name = "liblenthevcenc",
	.item_name  = av_default_item_name,
	.option     = options,
	.version    = LIBAVUTIL_VERSION_INT,
};

static const AVClass lenthevchm10_enc_class = {
	.class_name = "liblenthevchm10enc",
	.item_name  = av_default_item_name,
	.option     = options,
	.version    = LIBAVUTIL_VERSION_INT,
};

static const AVClass lenthevchm91_enc_class = {
	.class_name = "liblenthevchm91enc",
	.item_name  = av_default_item_name,
	.option     = options,
	.version    = LIBAVUTIL_VERSION_INT,
};

AVCodec ff_liblenthevc_encoder = {
	.name		= "liblenthevc",
	.type		= AVMEDIA_TYPE_VIDEO,
	.id		= AV_CODEC_ID_HEVC,
	.priv_data_size	= sizeof(LentHEVCEncContext),
	.init		= lent_hevc_enc_init,
	.close		= lent_hevc_enc_close,
	.encode2	= lent_hevc_enc_encode,
	.capabilities	= CODEC_CAP_DELAY | CODEC_CAP_AUTO_THREADS,
	.long_name	= NULL_IF_CONFIG_SMALL("Strongene Lentoid HEVC"),
	.pix_fmts	= (const enum AVPixelFormat[]){ AV_PIX_FMT_YUV420P, PIX_FMT_NONE },
	.priv_class	= &lenthevc_enc_class,
};

AVCodec ff_liblenthevchm10_encoder = {
	.name		= "liblenthevchm10",
	.type		= AVMEDIA_TYPE_VIDEO,
	.id		= AV_CODEC_ID_HEVC_HM10,
	.priv_data_size	= sizeof(LentHEVCEncContext),
	.init		= lent_hevc_enc_init,
	.close		= lent_hevc_enc_close,
	.encode2	= lent_hevc_enc_encode,
	.capabilities	= CODEC_CAP_DELAY | CODEC_CAP_AUTO_THREADS,
	.long_name	= NULL_IF_CONFIG_SMALL("Strongene Lentoid HEVC (HM10.0)"),
	.pix_fmts	= (const enum AVPixelFormat[]){ AV_PIX_FMT_YUV420P, PIX_FMT_NONE },
	.priv_class	= &lenthevchm10_enc_class,
};

AVCodec ff_liblenthevchm91_encoder = {
	.name		= "liblenthevchm91",
	.type		= AVMEDIA_TYPE_VIDEO,
	.id		= AV_CODEC_ID_HEVC_HM91,
	.priv_data_size	= sizeof(LentHEVCEncContext),
	.init		= lent_hevc_enc_init,
	.close		= lent_hevc_enc_close,
	.encode2	= lent_hevc_enc_encode,
	.capabilities	= CODEC_CAP_DELAY | CODEC_CAP_AUTO_THREADS,
	.long_name	= NULL_IF_CONFIG_SMALL("Strongene Lentoid HEVC (HM9.1)"),
	.pix_fmts	= (const enum AVPixelFormat[]){ AV_PIX_FMT_YUV420P, PIX_FMT_NONE },
	.priv_class	= &lenthevchm91_enc_class,
};
