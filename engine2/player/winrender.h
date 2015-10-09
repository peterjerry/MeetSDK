#ifndef _WIN_RENDER_H_
#define _WIN_RENDER_H_

#include "renderbase.h"

class WinRender : public RenderBase {
public:
	WinRender();
	~WinRender(void);

	virtual bool init(void* surface, uint32_t frameWidth, uint32_t frameHeight, int32_t format, bool force_sw = false);

	bool render(AVFrame* frame);

private:
	void close();

	bool sws_sw(AVFrame *frame);
private:
	bool				mDoOnce;
	struct SwsContext*	mConvertCtx;
	AVFrame*			mSurfaceFrame;
};

#endif // _WIN_RENDER_H_