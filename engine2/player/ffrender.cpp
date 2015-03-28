#include "ffrender.h"

#define LOG_TAG "FFRender"
#include "log.h"
#include "ppffmpeg.h"
#include "yuv_rgb.h"
#include "surface.h"
#include "utils.h"
#include "autolock.h"
#ifdef __ANDROID__
#include <cpu-features.h> // for decide render implement
#endif

#ifdef RENDER_RGB565
extern "C" {
#include "yuv2rgb.h" // new implement
}
#include "yuv2rgb565.h" // from strongplayer
#endif

#ifdef _MSC_VER
static int s_swsFlag = SWS_BICUBIC;
#else
static int s_swsFlag = SWS_POINT;
#endif

FFRender::FFRender(void* surface, uint32_t frameWidth, uint32_t frameHeight, int32_t format)
{
	mSurface		= surface;
	mFrameWidth		= frameWidth;
	mFrameHeight	= frameHeight;
	mFrameFormat	= format;
	mSurfaceWidth	= 0;
	mSurfaceHeight	= 0;
	mSurfaceStride	= 0;
	mAveScaleTimeMs = 0;
	mConvertCtx		= NULL;
	mSurfaceFrame	= NULL;
	mScaleFrame		= NULL;
	mScalePixels	= NULL;
	mForceSW		= false;
	mDoOnce			= true;
}

status_t FFRender::init(bool force_sw)
{
	mForceSW = force_sw;

#ifdef __ANDROID__
	if (Surface_open(mSurface)!= OK)
		return ERROR;
#endif

#ifdef OS_IOS
	if (Surface_open(mSurface, mFrameWidth, mFrameHeight, mFrameFormat) != OK)
		return ERROR;
#endif
	//adjust();
	return OK;
}

status_t FFRender::render(AVFrame* frame)
{
#if defined(__CYGWIN__) || defined(_MSC_VER)
	return render_sws(frame);
#elif defined(__ANDROID__)
#ifdef __arm__ // // android_arm implement
	if (mForceSW)
		return render_sws(frame);

	uint64_t cpuFeatures = android_getCpuFeatures();
	if ((cpuFeatures & ANDROID_CPU_ARM_FEATURE_NEON) != 0 &&
		(mFrameFormat == AV_PIX_FMT_YUV420P ||
		mFrameFormat == AV_PIX_FMT_NV12 ||
		mFrameFormat == AV_PIX_FMT_NV21))
	{
		return render_neon(frame);
	}
	else
	{
		return render_sws(frame);
	}
#else // android_x86 implement
	return render_sws(frame);
#endif
#else // ios implement
	return render_opengl(frame);
#endif
}

// for android neon accelerate
status_t FFRender::render_neon(AVFrame* frame)
{
#if defined(__ANDROID__) && defined(__arm__)
	void* surfacePixels = NULL;
	if (Surface_getPixels(&mSurfaceWidth, &mSurfaceHeight, &mSurfaceStride, &surfacePixels) != OK)
		return ERROR;

	int64_t begin_scale = getNowMs();

	//Convert format
	switch(mFrameFormat)
	{
	case AV_PIX_FMT_YUV420P:
		{
			if (mDoOnce) {
				LOGI("frame->data: %p,%p,%p", frame->data[0], frame->data[1], frame->data[2]); //((((int32_t)frame->data[0])+0x20)&0xffffffe0)
				LOGI("frame->linesize: %d,%d,%d", frame->linesize[0], frame->linesize[1], frame->linesize[2]);
				LOGI("frame->width: %d, frame->height: %d", frame->width, frame->height);
				LOGI("mSurface width %d, height %d, stride %d", mSurfaceWidth, mSurfaceHeight, mSurfaceStride);
				mDoOnce = false;
			}

			if (mSurfaceStride >= mFrameWidth) {
#ifdef RENDER_RGB565
				/*yuv420_2_rgb565((uint8_t *)surfacePixels, 
					frame->data[0], frame->data[1], frame->data[2],
					frame->width, frame->height, // picture width and height
					frame->linesize[0], //Y span/pitch
					frame->linesize[1], //UV span/pitch //frame->linesize[1]
					mSurfaceStride<<1, //bitmap span/pitch
					yuv2rgb565_table,
					0);*/
				ConvertYCbCrToRGB565(
					frame->data[0], frame->data[1], frame->data[2],
					(uint8_t*)surfacePixels,
					frame->width, frame->height, // picture width and height
					frame->linesize[0], //Y span/pitch
					frame->linesize[1], //UV span/pitch //frame->linesize[1]
					mSurfaceStride<<1, //bitmap span/pitch
					420);
#else
				struct yuv_pack out = {surfacePixels, (int32_t)mSurfaceStride * 4};
				struct yuv_planes in = {frame->data[0], frame->data[1], frame->data[2], frame->linesize[0]};
				i420_rgb_neon(&out, &in, frame->width, frame->height);
#endif
			}
			else {
				LOGW("surface memory is too small: surf_w %d, surf_h %d, surf_stride %d, frame_w %d", 
					mSurfaceWidth, mSurfaceHeight, mSurfaceStride, frame->width);
			}


			break;
		}
	case AV_PIX_FMT_NV12:
		{
			struct yuv_pack out = {surfacePixels, (int32_t)mSurfaceStride * 4};
			struct yuv_planes in = {frame->data[0], frame->data[1], frame->data[2], frame->linesize[0]};
			nv12_rgb_neon (&out, &in, frame->width, frame->height);
			break;
		}
	case AV_PIX_FMT_NV21:
		{
			struct yuv_pack out = {surfacePixels, (int32_t)mSurfaceStride * 4};
			struct yuv_planes in = {frame->data[0], frame->data[1], frame->data[2], frame->linesize[0]};
			nv21_rgb_neon (&out, &in, frame->width, frame->height);
			break;
		}
	default:
		LOGE("Video output format:%d does not support", mFrameFormat);
		return ERROR;
	}

	LOGD("before rendering frame");
	if(Surface_updateSurface() != OK)
	{
		LOGE("Failed to render picture");
		return ERROR;
	}
	LOGD("after rendering frame");

	int64_t end_scale = getNowMs();
	int64_t costTime = end_scale - begin_scale;
	if(mAveScaleTimeMs == 0)
	{
		mAveScaleTimeMs = costTime;
	}
	else
	{
		mAveScaleTimeMs = (mAveScaleTimeMs*4+costTime)/5;
	}
	LOGD("neon scale picture cost %lld[ms]", costTime);
	LOGV("mAveScaleTimeMs %lld[ms]", mAveScaleTimeMs);

	//For debug
	/*
	char path[1024] = {0};
	static int num=0;
	num++;
	sprintf(path, "/mnt/sdcard/frame_rgb_%d", num);
	LOGD("mSurfaceFrame->linesize[0]:%d, mOptiSurfaceHeight:%d", mSurfaceFrame->linesize[0], mOptiSurfaceHeight);
	saveFrameRGB(mSurfaceFrame->data[0], mSurfaceFrame->linesize[0], mOptiSurfaceHeight, path);
	*/
	return OK;
#else
	return ERROR;
#endif
}

// for ios render
status_t FFRender::render_opengl(AVFrame* frame)
{
#ifdef OS_IOS
// ios implement
	int64_t begin_scale = getNowMs();

	if (mFrameFormat == AV_PIX_FMT_YUV420P || mFrameFormat == AV_PIX_FMT_YUVJ420P)
	{
		if(Surface_displayPicture(frame) != OK)
		{
			LOGE("Failed to render picture");
			return ERROR;
		}
	}
	else
	{
		//TODO
		LOGE("picture format unsupported");
	}

	int64_t end_scale = getNowMs();
	int64_t costTime = end_scale-begin_scale;
	if(mAveScaleTimeMs == 0)
	{
		mAveScaleTimeMs = costTime;
	}
	else
	{
		mAveScaleTimeMs = (mAveScaleTimeMs*4+costTime)/5;
	}
	LOGD("opengl scale picture cost %lld[ms]", costTime);
	LOGV("mAveScaleTimeMs %lld[ms]", mAveScaleTimeMs);

	return OK;
#else
	return ERROR;
#endif
}

// use ffmpeg sws_scale, for cygwin win32 and android common
// not for IOS
status_t FFRender::render_sws(AVFrame* frame)
{
#ifdef OS_IOS
	return ERROR;
#else
	if (mConvertCtx == NULL || mSurfaceFrame == NULL) {
		if (mConvertCtx != NULL) {
			sws_freeContext(mConvertCtx);
			mConvertCtx = NULL;
		}
		if (mSurfaceFrame != NULL) {
			av_frame_free(&mSurfaceFrame);
		}
		//just do color format conversion
		//avoid doing scaling as it cost lots of cpu
		AVPixelFormat out_fmt;
#if defined(__CYGWIN__) || defined(_MSC_VER)
		out_fmt = AV_PIX_FMT_RGB32;
#else
#ifdef RENDER_RGB565
		out_fmt = PIX_FMT_RGB565;
#else
		out_fmt = AV_PIX_FMT_RGB0;
#endif
#endif
		mConvertCtx = sws_getContext(
			frame->width, frame->height,
			(AVPixelFormat)frame->format,
			mFrameWidth, mFrameHeight,
			out_fmt, 
			s_swsFlag, NULL, NULL, NULL);
		if (mConvertCtx == NULL) {
			LOGE("create convert ctx failed, width:%d, height:%d, pix:%d",
				mFrameWidth,
				mFrameHeight,
				mFrameFormat);
			return ERROR;
		}
		LOGI("sws context created %dx%d %d->%d", mFrameWidth, mFrameHeight, mFrameFormat, AV_PIX_FMT_BGR24);

		mSurfaceFrame = av_frame_alloc();
		if (mSurfaceFrame == NULL) {
			LOGE("alloc frame failed");
			return ERROR;
		}

	}

	void* surfacePixels = NULL;
	if (Surface_getPixels(&mSurfaceWidth, &mSurfaceHeight, &mSurfaceStride, &surfacePixels) != OK)
		return ERROR;

	// Convert the image
	int64_t begin_scale = getNowMs();
	if (mSurfaceStride >= mFrameWidth) {
		mSurfaceFrame->data[0] = (uint8_t*)surfacePixels;
#ifdef RENDER_RGB565
		mSurfaceFrame->linesize[0] = mSurfaceStride * 2;
#else
		mSurfaceFrame->linesize[0] = mSurfaceStride * 4;
#endif
		sws_scale(mConvertCtx,
			frame->data,
			frame->linesize,
			0,
			frame->height,
			mSurfaceFrame->data,
			mSurfaceFrame->linesize);
		LOGD("sws_scale frame width:%d", mFrameWidth);
		LOGD("sws_scale frame height:%d", mFrameHeight);
		LOGD("sws_scale surface width:%d", mSurfaceWidth);
		LOGD("sws_scale surface height:%d", mSurfaceHeight);
		LOGD("sws_scale surface stride:%d", mSurfaceStride);
	}
	else
	{
		LOGE("Surface memory is too small");
	}

	LOGD("before rendering frame");
	if(Surface_updateSurface() != OK)
	{
		LOGE("Failed to render picture");
		return ERROR;
	}
	LOGD("after rendering frame");

	int64_t end_scale = getNowMs();
	int64_t costTime = end_scale-begin_scale;
	if(mAveScaleTimeMs == 0)
	{
		mAveScaleTimeMs = costTime;
	}
	else
	{
		mAveScaleTimeMs = (mAveScaleTimeMs*4+costTime)/5;
	}
	LOGD("sws scale picture cost %lld[ms]", costTime);
	LOGV("mAveScaleTimeMs %lld[ms]", mAveScaleTimeMs);

	//For debug
	/*
	char path[1024] = {0};
	static int num=0;
	num++;
	sprintf(path, "/mnt/sdcard/frame_rgb_%d", num);
	LOGD("mSurfaceFrame->linesize[0]:%d, mOptiSurfaceHeight:%d", mSurfaceFrame->linesize[0], mOptiSurfaceHeight);
	saveFrameRGB(mSurfaceFrame->data[0], mSurfaceFrame->linesize[0], mOptiSurfaceHeight, path);
	*/
	return OK;
#endif
}

FFRender::~FFRender()
{
	if(mConvertCtx != NULL) {
		sws_freeContext(mConvertCtx);
		mConvertCtx = NULL;
	}
	if(mSurfaceFrame != NULL) {
		av_frame_free(&mSurfaceFrame);
	}
	if(mScalePixels != NULL) {
		free(mScalePixels);
		mScalePixels = NULL;
	}
	if(mScaleFrame != NULL) {
		av_frame_free(&mScaleFrame);
	}
#if defined(__CYGWIN__)
	//todo
#else
	Surface_close();
	mSurface = NULL;
#endif
	LOGD("FFRender destructor");
}

//For debug
void FFRender::saveFrameRGB(void* data, int stride, int height, char* path)
{
	if(path==NULL) return;

	FILE *pFile = NULL;
	LOGD("Start open file %s", path);
	pFile = fopen(path, "wb");
	if(pFile == NULL) {
		LOGE("open file %s failed", path);
		return;
	}
	LOGD("open file %s success", path);

	fwrite(data, 1, stride*height, pFile);
	fclose(pFile);
}
