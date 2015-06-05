#ifndef _WIN32_RENDER_H
#define _WIN32_RENDER_H

#include "renderbase.h"

class WinRender : public RenderBase {
public:
	WinRender();
	~WinRender(void);

	virtual bool init(void* surface, uint32_t surfaceWidth, uint32_t surfaceHeight, int32_t format, bool force_sw = false);

	bool render(AVFrame* frame);

private:
	void close();

	bool sws_sw(AVFrame *frame, char* out_data, int size);
private:
	char*				mData;
	bool				mForceSW;
	bool				mDoOnce;
	struct SwsContext*	mConvertCtx;
	AVFrame*			mSurfaceFrame;
};

#endif // _WIN32_RENDER_H