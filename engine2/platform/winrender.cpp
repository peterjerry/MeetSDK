#include "winrender.h"

#include "utils.h"
#include "ppffmpeg.h"
#define LOG_TAG "winrender"
#include "log.h"
#include "sdl.h"

#ifdef _MSC_VER
static int s_swsFlag = SWS_BICUBIC;
#else
static int s_swsFlag = SWS_POINT;
#endif

WinRender::WinRender()
	:mForceSW(false), mDoOnce(true), mConvertCtx(NULL), mSurfaceFrame(NULL)
{
}

bool WinRender::init(void* surface, uint32_t surfaceWidth, uint32_t surfaceHeight, int32_t format, bool force_sw)
{
	if (surface == NULL) {
		LOGE("surface is null");
        return false;
	}

	mSurface	= surface;
	mWidth		= surfaceWidth;
	mHeight		= surfaceHeight;
	mFormat		= format;
	mForceSW	= force_sw;

	mData = new char[mWidth * mHeight * 4];

	return true;
}

bool WinRender::render(AVFrame* frame)
{
	LOGD("render");
	
	int64_t begin_scale = getNowMs();

	//Convert format
	sws_sw(frame, mData, mWidth * mHeight * 4);

	LOGD("before rendering frame");
	SDL_Surface* surf = NULL;
	SDL_Surface* MainSurf = (SDL_Surface *)mSurface;
	surf = SDL_CreateRGBSurfaceFrom(mData, mWidth, mHeight, 32,
		mWidth * 4, 0, 0, 0, 0);
	SDL_BlitSurface(surf, NULL, MainSurf, NULL);
	SDL_Flip(MainSurf);
	SDL_FreeSurface(surf);
	surf = NULL;
	LOGD("after rendering frame");

	int64_t end_scale = getNowMs();
	int64_t costTime = end_scale - begin_scale;
	if (mAveScaleTimeMs == 0)
		mAveScaleTimeMs = costTime;
	else
		mAveScaleTimeMs = (mAveScaleTimeMs * 4 + costTime) / 5;

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
	return true;
}

bool WinRender::sws_sw(AVFrame *frame, char* out_data, int size)
{
	LOGD("sws_sw");

	if (mConvertCtx == NULL) {
		mConvertCtx = sws_getContext(
			frame->width, frame->height, (AVPixelFormat)frame->format,
			mWidth, mHeight, AV_PIX_FMT_RGB32, 
			s_swsFlag, NULL, NULL, NULL);
		if (mConvertCtx == NULL) {
			LOGE("failed to create convert ctx, %d x %d(fmt %d) -> %d x %d(fmt %d)",
				frame->width, frame->height, (AVPixelFormat)frame->format, 
				mWidth, mHeight, AV_PIX_FMT_RGB32);
			return false;
		}

		LOGI("sws context created: %d x %d(fmt: %d) -> %d x %d(fmt: %d)", 
			frame->width, frame->height, (AVPixelFormat)frame->format, 
			mWidth, mHeight, AV_PIX_FMT_RGB32);
	}

	if (mSurfaceFrame == NULL) {
		mSurfaceFrame = av_frame_alloc();
		if (mSurfaceFrame == NULL) {
			LOGE("alloc frame failed");
			return false;
		}
	}

	// Convert the image
	mSurfaceFrame->data[0] = (uint8_t*)out_data;
	mSurfaceFrame->linesize[0] = mWidth * 4;
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

WinRender::~WinRender(void)
{
	close();
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
	if (mData) {
		delete mData;
		mData = NULL;
	}
}
