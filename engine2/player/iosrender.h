#ifndef _IOS_RENDER_H_
#define _IOS_RENDER_H_

#include "renderbase.h"

class IOSRender : public RenderBase {
public:
	IOSRender();
	~IOSRender(void);

	// 初始render.
	virtual bool init_render(void* ctx, int w, int h, int pix_fmt, bool force_sw = false);

	// 渲染一帧.
	virtual bool render_one_frame(AVFrame* frame, int pix_fmt);

private:
	void close();
	
	bool sws_sw(AVFrame *frame);
private:
	struct SwsContext*	mConvertCtx;
	AVFrame*			mSurfaceFrame;
};

#endif // _IOS_RENDER_H_