#ifndef _RENDER_BASE_H_
#define _RENDER_BASE_H_

#include <stdint.h>

struct AVFrame;

class RenderBase
{
public:
	RenderBase()
		:mSurface(NULL), mWidth(0), mHeight(0), mAveScaleTimeMs(0)
	{
	}

	virtual ~RenderBase(void){}

	virtual bool init(void* surface, uint32_t frameWidth, uint32_t frameHeight, int32_t format, bool force_sw = false) = 0;

    uint32_t get_width() {
		return mWidth;
	}

	uint32_t get_height() {
		return mHeight;
	}

    uint32_t get_swsMs() {
        return mAveScaleTimeMs;
    }

    virtual bool render(AVFrame* frame) = 0;

protected:
	void* mSurface;
	uint32_t mWidth;
	uint32_t mHeight;
	int32_t mFormat;
	uint32_t mAveScaleTimeMs;
};

#endif // _RENDER_BASE_H_
