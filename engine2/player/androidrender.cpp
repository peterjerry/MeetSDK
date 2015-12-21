#include "androidrender.h"
#include "ppffmpeg.h"
#include "utils.h" // getMsec()
#include <cpu-features.h> // for decide render implement
#include <android/native_window.h>
#include <jni.h>
#include <android/native_window_jni.h>

#if defined(__arm__)
#ifdef RENDER_RGB565
extern "C" {
#include "yuv2rgb.h" // new implement
}
#include "yuv2rgb565.h" // from strongplayer
#else
#include "yuv_rgb.h" // old implement
#endif
#elif defined(__aarch64__)
#include "libyuv/row.h"
#endif

#define LOG_TAG "AndroidRender"
#include "log.h"

#ifdef USE_SWSCALE
static int s_swsFlag = SWS_POINT; // high speed
#endif
extern JavaVM* gs_jvm;

static void saveFrameRGB(void* data, int stride, int height, char* path);

#ifdef __aarch64__
// Convert I420 to ARGB.
static int I420ToABGR(const uint8_t* src_y, int src_stride_y,
               const uint8_t* src_u, int src_stride_u,
               const uint8_t* src_v, int src_stride_v,
               uint8_t* dst_argb, int dst_stride_argb,
               int width, int height);
#endif

AndroidRender::AndroidRender()
	:mWindow(NULL), mForceSW(false), mDoOnce(true), 
#ifdef USE_SWSCALE
	mConvertCtx(NULL),
#endif
	mSurfaceFrame(NULL)
{
}

AndroidRender::~AndroidRender(void)
{
	close();
}

bool AndroidRender::init(void* surface, uint32_t frameWidth, uint32_t frameHeight, int32_t format, bool force_sw)
{
	LOGI("surface %p, frame_resolution %d x %d, format %d", surface, frameWidth, frameHeight, format);

	if (surface == NULL) {
		LOGE("surface is null");
        return false;
	}

	mSurface	= surface;
	mWidth		= frameWidth;
	mHeight		= frameHeight;
	mFormat		= format;
	mForceSW	= force_sw;

	JNIEnv* env = NULL;
    if (gs_jvm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		LOGE("GetEnv failed");
		return false;
	}

    mWindow = ANativeWindow_fromSurface(env, (jobject)mSurface);
    if (!mWindow) {
        LOGE("failed to get window");
		return false;
	}

	return true;
}

bool AndroidRender::render(AVFrame* frame)
{
	LOGD("render");
	
	if (!mWindow) {
		LOGW("native window is null");
	}
	
	int64_t begin_scale = getNowMs();

	ANativeWindow_Buffer buffer = {0};
	int32_t ret = ANativeWindow_lock(mWindow, &buffer, NULL);
	if (ret != 0) {
		LOGE("failed to lock native window: %d", ret);
		return false;
	}

	if ((WINDOW_FORMAT_RGBX_8888 != buffer.format) && (WINDOW_FORMAT_RGBA_8888 != buffer.format)) {
		// 2015.3.31 guoliangma added to fix "already locked" problem when window_format is invalid
		ANativeWindow_unlockAndPost(mWindow);

		LOGE("native window format is not valid: %d", buffer.format);
		return false;
	}

	if (NULL == buffer.bits) {
		// 2015.3.31 guoliangma added
		ANativeWindow_unlockAndPost(mWindow);

		LOGE("native window bits is null");
		return false;
	}

	if (mDoOnce) {
		LOGI("frame->data: %p, %p, %p", frame->data[0], frame->data[1], frame->data[2]);
		LOGI("frame->linesize: %d,%d,%d", frame->linesize[0], frame->linesize[1], frame->linesize[2]);
		LOGI("frame->width: %d, frame->height: %d", frame->width, frame->height);
		LOGI("native_buffer width %d, height %d, stride %d", buffer.width, buffer.height, buffer.stride);
		mDoOnce = false;
	}

	if (buffer.stride < frame->width || buffer.height < frame->height) {
		LOGW("surface memory is too small: surf_w %d, surf_h %d, surf_stride %d, frame_w %d, frame_h %d", 
			buffer.width, buffer.height, buffer.stride, frame->width, frame->height);
		// 2015.9.29 guolinagma added to fix some clip failed to play
		ANativeWindow_unlockAndPost(mWindow);
		return true; // just OK
	}

	//Convert format
#if defined(__arm__) // arm implement
	if (mForceSW) {
		sws_sw(frame, &buffer);
	}
	else {
		uint64_t cpuFeatures = android_getCpuFeatures();
		if ((cpuFeatures & ANDROID_CPU_ARM_FEATURE_NEON) != 0 &&
			(frame->format == AV_PIX_FMT_YUV420P ||
			frame->format == AV_PIX_FMT_NV12 ||
			frame->format == AV_PIX_FMT_NV21))
		{
			sws_neon(frame, &buffer);
		}
		else {
			sws_sw(frame, &buffer);
		}
	}
#elif defined(__aarch64__)
	if (frame->format == AV_PIX_FMT_YUV420P)
		sws_arm64(frame, &buffer);
	else
		sws_sw(frame, &buffer);
#else // android_x86 implement
	sws_sw(frame, &buffer);
#endif

	LOGD("before rendering frame");
	if (ANativeWindow_unlockAndPost(mWindow) != 0) {
		LOGE("ANativeWindow_unlockAndPost failed");
		return false;
	}
	LOGD("after rendering frame");

	int64_t end_scale = getNowMs();
	int64_t costTime = end_scale - begin_scale;
	if (mAveScaleTimeMs == 0)
		mAveScaleTimeMs = costTime;
	else
		mAveScaleTimeMs = (mAveScaleTimeMs * 4 + costTime) / 5;

	LOGD("scale picture cost %lld[ms]", costTime);

#ifdef DEBUG_DUMP_PIC
	char path[1024] = {0};
	static int num=0;
	num++;
	sprintf(path, "/mnt/sdcard/frame_rgb_%d", num);
	LOGD("mSurfaceFrame->linesize[0]:%d, mOptiSurfaceHeight:%d", mSurfaceFrame->linesize[0], mOptiSurfaceHeight);
	saveFrameRGB(mSurfaceFrame->data[0], mSurfaceFrame->linesize[0], mOptiSurfaceHeight, path);
#endif

	if (0)
		saveFrameRGB(NULL, 0, 0, (char *)"/mnt/sdcard/1.rgb");

	return true;
}

void AndroidRender::close()
{
#ifdef USE_SWSCALE
	if (mConvertCtx != NULL) {
		sws_freeContext(mConvertCtx);
		mConvertCtx = NULL;
	}
#endif
	if (mSurfaceFrame != NULL) {
		av_frame_free(&mSurfaceFrame);
	}
	if (mWindow) {
		ANativeWindow_release(mWindow);
		mWindow = NULL;
	}

	LOGI("destructor()");
}

bool AndroidRender::sws_neon(AVFrame *frame, ANativeWindow_Buffer *buffer)
{
	LOGD("sws_neon");

#ifdef __arm__
	//Convert format
	switch(frame->format) {
	case AV_PIX_FMT_YUV420P:
		{
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
			struct yuv_pack out = {buffer->bits, (int32_t)buffer->stride * 4};
			struct yuv_planes in = {frame->data[0], frame->data[1], frame->data[2], frame->linesize[0]};
			i420_rgb_neon(&out, &in, frame->width, frame->height);
#endif // end of RENDER_RGB565
			break;
		}
	case AV_PIX_FMT_NV12:
	case AV_PIX_FMT_NV21:
		{
			struct yuv_pack out = {buffer->bits, (int32_t)buffer->stride * 4};
			struct yuv_planes in = {frame->data[0], frame->data[1], frame->data[2], frame->linesize[0]};
			if (AV_PIX_FMT_NV12 == frame->format)
				nv12_rgb_neon(&out, &in, frame->width, frame->height);
			else
				nv21_rgb_neon(&out, &in, frame->width, frame->height);
			break;
		}
	default:
		LOGE("Video output format:%d(%s) does NOT support", frame->format, av_get_pix_fmt_name((AVPixelFormat)frame->format));
		return false;
	}

	return true;
#else
	return false;
#endif
}

bool AndroidRender::sws_arm64(AVFrame *frame, ANativeWindow_Buffer *buffer)
{
	LOGD("sws_arm64");

#ifdef __aarch64__
	//Convert format
	switch(frame->format) {
	case AV_PIX_FMT_YUV420P:
		I420ToABGR(frame->data[0], frame->linesize[0],
			frame->data[1], frame->linesize[1],
			frame->data[2], frame->linesize[2],
			(uint8_t *)buffer->bits, buffer->stride * 4,
			frame->width, frame->height);
		break;
	default:
		LOGE("Video output format:%d(%s) does NOT support", frame->format, av_get_pix_fmt_name((AVPixelFormat)frame->format));
		return false;
	}
	
	return true;
#else
	return false;
#endif
}

bool AndroidRender::sws_sw(AVFrame *frame, ANativeWindow_Buffer *buffer)
{
	LOGD("sws_sw");
#ifdef USE_SWSCALE
	if (mConvertCtx == NULL) {
		//just do color format conversion
		//avoid doing scaling as it cost lots of cpu
		AVPixelFormat out_fmt;
#ifdef RENDER_RGB565
		out_fmt = AV_PIX_FMT_RGB565;
#else
		out_fmt = AV_PIX_FMT_RGB0;
#endif
		// android sws ONYL convert pixel format, resolution ALWAYS no change
		// android surface will do swich scale job
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

	// Convert the image
	if (buffer->stride < frame->width || buffer->height < frame->height) {
		LOGW("surface is too small: buffer %d x %d, frame %d x %d", buffer->stride, buffer->height, frame->width, frame->height);
		return false;
	}

	mSurfaceFrame->data[0] = (uint8_t*)buffer->bits;
#ifdef RENDER_RGB565
	mSurfaceFrame->linesize[0] = buffer->stride * 2;
#else
	mSurfaceFrame->linesize[0] = buffer->stride * 4;
#endif
	int ret = sws_scale(mConvertCtx,
		frame->data,
		frame->linesize,
		0,
		frame->height,
		mSurfaceFrame->data,
		mSurfaceFrame->linesize);
	if (ret != (int)frame->height) {
		LOGW("sws_scale ret: %d, %dx%d(fmt %d) -> %dx%d(fmt %d)", 
			ret, frame->width, frame->height, frame->format, mWidth, mHeight, AV_PIX_FMT_RGB32);
	}

	return true;
#else
	LOGE("swscale NOT built-in, not support sws_sw");
	return false;
#endif
}

#if defined(__aarch64__)
// Convert I420 to ARGB.
static int I420ToABGR(const uint8_t* src_y, int src_stride_y,
               const uint8_t* src_u, int src_stride_u,
               const uint8_t* src_v, int src_stride_v,
               uint8_t* dst_argb, int dst_stride_argb,
               int width, int height)
{
	int y;
	for (y = 0; y < height; ++y) {
		libyuv::I422ToABGRRow_NEON(src_y, src_u, src_v, dst_argb, width);
		dst_argb += dst_stride_argb;
		src_y += src_stride_y;
		if (y & 1) {
			src_u += src_stride_u;
			src_v += src_stride_v;
		}
	}
	return 0;
}
#endif

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