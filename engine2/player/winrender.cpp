#include "winrender.h"
#include "ppffmpeg.h"
#include "surface.h"
#include "utils.h"
#include "autolock.h"

#define LOG_TAG "WinRender"
#include "log.h"

static int s_swsFlag = SWS_BICUBIC; // high quality

static void saveFrameRGB(void* data, int stride, int height, char* path);

WinRender::WinRender()
	:mDoOnce(true), mConvertCtx(NULL), mSurfaceFrame(NULL)
{
}

WinRender::~WinRender(void)
{
	close();
}

bool WinRender::init_render(void* ctx, int w, int h, int pix_fmt, bool force_sw)
{
	(void)force_sw;

	LOGI("surface %p, frame_resolution %d x %d, format %d", ctx, w, h, pix_fmt);

	mSurface	= ctx;
	mWidth		= w;
	mHeight		= h;
	mFormat		= pix_fmt;

	return true;
}

bool WinRender::render_one_frame(AVFrame* frame, int pix_fmt)
{
	LOGD("render_one_frame");

	int64_t begin_scale = getNowMs();

	sws_sw(frame);
	
	if (Surface_updateSurface() != OK) {
		LOGE("Failed to render picture");
		return false;
	}

	LOGD("after rendering frame");
	LOGD("after rendering frame");

	int64_t end_scale = getNowMs();
	int64_t costTime = end_scale - begin_scale;
	if (mAveScaleTimeMs == 0)
		mAveScaleTimeMs = (uint32_t)costTime;
	else
		mAveScaleTimeMs = (uint32_t)(mAveScaleTimeMs * 4 + costTime) / 5;

	LOGD("scale picture cost %lld[ms]", costTime);

	//For debug
	/*
	char path[1024] = {0};
	static int num=0;
	num++;
	sprintf(path, "/mnt/sdcard/frame_rgb_%d", num);
	LOGD("mSurfaceFrame->linesize[0]:%d, mOptiSurfaceHeight:%d", mSurfaceFrame->linesize[0], mOptiSurfaceHeight);
	saveFrameRGB(mSurfaceFrame->data[0], mSurfaceFrame->linesize[0], mOptiSurfaceHeight, path);
	*/

	if (0)
		saveFrameRGB(NULL, 640, 480, (char *)"/mnt/sdcard/1.rgb");

	return true;
}

void WinRender::close()
{
	if (mConvertCtx != NULL) {
		sws_freeContext(mConvertCtx);
		mConvertCtx = NULL;
	}
	if (mSurfaceFrame != NULL) {
		av_frame_free(&mSurfaceFrame);
	}
	LOGI("destructor()");
}

bool WinRender::sws_sw(AVFrame *frame)
{
	if (mConvertCtx == NULL) {
		//just do color format conversion
		//avoid doing scaling as it cost lots of cpu
		AVPixelFormat out_fmt = AV_PIX_FMT_RGB32;;
		mConvertCtx = sws_getContext(
			frame->width, frame->height, (AVPixelFormat)frame->format,
			mWidth, mHeight, out_fmt, 
			s_swsFlag, NULL, NULL, NULL);
		if (mConvertCtx == NULL) {
			LOGE("failed to create convert ctx, %d x %d(fmt %d[%s]) -> %d x %d(fmt %d[%s])",
				frame->width, frame->height, frame->format, av_get_pix_fmt_name((AVPixelFormat)frame->format),
				mWidth, mHeight, out_fmt, av_get_pix_fmt_name(out_fmt));
			return false;
		}

		LOGI("sws context created: %d x %d(fmt: %d[%s]) -> %d x %d(fmt: %d[%s])", 
			frame->width, frame->height, frame->format, av_get_pix_fmt_name((AVPixelFormat)frame->format),
			mWidth, mHeight, out_fmt, av_get_pix_fmt_name(out_fmt));
	}

	if (mSurfaceFrame == NULL) {
		mSurfaceFrame = av_frame_alloc();
		if (mSurfaceFrame == NULL) {
			LOGE("alloc frame failed");
			return false;
		}
	}

	void* surfacePixels = NULL;
	uint32_t width, height, stride;
	if (Surface_getPixels(&width, &height, &stride, &surfacePixels) != OK)
		return false;


	// Convert the image
	if (stride < mWidth || height < mHeight) {
		LOGW("surface is too small: buffer %d x %d, frame %d x %d", stride, height, mWidth, mHeight);
		return false;
	}

	mSurfaceFrame->data[0] = (uint8_t*)surfacePixels;
	mSurfaceFrame->linesize[0] = stride * 4;
	int ret = sws_scale(mConvertCtx,
		frame->data,
		frame->linesize,
		0,
		frame->height,
		mSurfaceFrame->data,
		mSurfaceFrame->linesize);
	if (ret != mHeight) {
		LOGW("sws_scale ret: %d, %dx%d(fmt %d) -> %dx%d(fmt %d)", 
			ret, frame->width, frame->height, frame->format, mWidth, mHeight, AV_PIX_FMT_RGB32);
	}
	
	return true;
}

//For debug
static void saveFrameRGB(void* data, int stride, int height, char* path)
{
	if (path==NULL)
		return;

	FILE *pFile = NULL;
	LOGD("Start open file %s", path);
	pFile = fopen(path, "wb");
	if (pFile == NULL) {
		LOGE("open file %s failed", path);
		return;
	}
	LOGD("open file %s success", path);

	fwrite(data, 1, stride*height, pFile);
	fclose(pFile);
}