#ifndef _IOS_RENDER_H_
#define _IOS_RENDER_H_

#include "renderbase.h"

class IOSRender : public RenderBase {
public:
	IOSRender();
	~IOSRender(void);

	virtual bool init(void* surface, uint32_t frameWidth, uint32_t frameHeight, int32_t format, bool force_sw = false);

	bool render(AVFrame* frame);

private:
	void close();
	
	bool sws_sw(AVFrame *frame);
private:
	struct SwsContext*	mConvertCtx;
	AVFrame*			mSurfaceFrame;
};

#endif // _IOS_RENDER_H_