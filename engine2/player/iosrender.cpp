#include "iosrender.h"
#include "ppffmpeg.h"
#include "surface.h"
#include "utils.h"

#define LOG_TAG "IOSRender"
#include "log.h"

static int s_swsFlag = SWS_POINT; // high speed

static AVFrame * alloc_picture(AVPixelFormat pix_fmt, int width, int height);

static void saveFrameRGB(void* data, int stride, int height, char* path);

IOSRender::IOSRender()
	:mConvertCtx(NULL), mSurfaceFrame(NULL)
{
}

IOSRender::~IOSRender(void)
{
	close();
}

bool IOSRender::init(void* surface, uint32_t frameWidth, uint32_t frameHeight, int32_t format, bool force_sw)
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
	
	if (Surface_open(mSurface, mWidth, mHeight, mFormat) != OK)
		return false;

	return true;
}

bool IOSRender::render(AVFrame* frame)
{
	LOGD("render");
	
	int64_t begin_scale = getNowMs();

	AVFrame *disp_frame = frame;
	if (frame->format != AV_PIX_FMT_YUV420P && frame->format != AV_PIX_FMT_YUVJ420P) {
		if (!sws_sw(frame))
			return false;
		
		disp_frame = mSurfaceFrame;
	}
	
	if (Surface_displayPicture(disp_frame) != OK) {
		LOGE("Failed to render picture");
		return false;
	}

	int64_t end_scale = getNowMs();
	int64_t costTime = end_scale-begin_scale;
	if (mAveScaleTimeMs == 0)
		mAveScaleTimeMs = costTime;
	else
		mAveScaleTimeMs = (mAveScaleTimeMs*4+costTime)/5;
	LOGD("opengl scale picture cost %lld ms, mAveScaleTimeMs %lld ms", costTime, mAveScaleTimeMs);

	return true;
}

void IOSRender::close()
{
	if (mConvertCtx != NULL) {
		sws_freeContext(mConvertCtx);
		mConvertCtx = NULL;
	}
	if (mSurfaceFrame != NULL) {
		av_frame_free(&mSurfaceFrame);
	}
	
	Surface_close();
	LOGI("destructor()");
}

bool IOSRender::sws_sw(AVFrame *frame)
{
	LOGD("sws_sw");

	if (mConvertCtx == NULL) {
		//just do color format conversion
		//avoid doing scaling as it cost lots of cpu
		AVPixelFormat out_fmt = AV_PIX_FMT_YUV420P;
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
		
		/*mSurfaceFrame->width	= mWidth;
		mSurfaceFrame->height	= mHeight;
		mSurfaceFrame->format	= AV_PIX_FMT_YUV420P;
		if (av_frame_get_buffer(mSurfaceFrame, 0) < 0) {
			LOGE("get frame buffer failed");
			return false;
		}*/
	}

	// Convert the image
	int ret = sws_scale(mConvertCtx,
		frame->data,
		frame->linesize,
		0,
		frame->height,
		mSurfaceFrame->data,
		mSurfaceFrame->linesize);
	if (ret != (int)frame->height) {
		LOGW("sws_scale ret: %d, %dx%d(fmt %d) -> %dx%d(fmt %d)", 
			ret, frame->width, frame->height, frame->format, mWidth, mHeight, AV_PIX_FMT_YUV420P);
	}
	
	return true;
}

static AVFrame * alloc_picture(AVPixelFormat pix_fmt, int width, int height)
{
    AVFrame *picture;
    uint8_t *picture_buf;
    int size;

    picture = av_frame_alloc();
    if (!picture)
        return NULL;
		
    size = avpicture_get_size(pix_fmt, width, height);
    picture_buf = (uint8_t *)av_malloc(size);
    if (!picture_buf) {
        av_free(picture);
        return NULL;
    }
	
    avpicture_fill((AVPicture *)picture, picture_buf,
                   (AVPixelFormat)pix_fmt, width, height);
    return picture;
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