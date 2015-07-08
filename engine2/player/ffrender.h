#ifndef FF_RENDER_H
#define FF_RENDER_H

#if defined(_MSC_VER) || defined(__CYGWIN__)
#include "pperrors.h"
#else
#include "errors.h"
#endif
#include <stdint.h>

struct AVFrame;

class FFRender
{
public:
    FFRender(void* surface, uint32_t frameWidth, uint32_t frameHeight, int32_t format);
	~FFRender();

    status_t init(bool force_sw = false);

    status_t width(uint32_t& surfaceWidth)
    {
        surfaceWidth = mFrameWidth;
        return OK;
    }

    status_t height(uint32_t& surfaceHeight)
    {
        surfaceHeight = mFrameHeight;
        return OK;
    }

    uint32_t swsMs()
    {
        return (uint32_t)mAveScaleTimeMs;
    }

    status_t render(AVFrame* frame);

    // for android neon accelerate
    status_t render_neon(AVFrame* frame);

	// for ios render
    status_t render_opengl(AVFrame* frame);

	// for cygwin win32 and android common, use ffmpeg sws_scale
    status_t render_sws(AVFrame* frame);

private:

    //For debug
    void saveFrameRGB(void* data, int stride, int height, char* path);

private:
    void* mSurface;
#ifdef __ANDROID__
	void* mNativeWindow;
#endif
    uint32_t mFrameWidth;
    uint32_t mFrameHeight;
    int32_t mFrameFormat;
    struct SwsContext* mConvertCtx;
    AVFrame* mSurfaceFrame;
    AVFrame* mScaleFrame;
    void* mScalePixels;
    uint32_t mSurfaceWidth;
    uint32_t mSurfaceHeight;
    uint32_t mSurfaceStride;
    int64_t mAveScaleTimeMs;
	bool mForceSW;
	bool mDoOnce;
};

#endif // FF_RENDER_H