#ifndef _ANDROID_RENDER_H_
#define _ANDROID_RENDER_H_

#include "renderbase.h"

struct ANativeWindow;
struct ANativeWindow_Buffer;

class AndroidRender : public RenderBase {
public:
	AndroidRender();
	~AndroidRender(void);

	virtual bool init(void* surface, uint32_t frameWidth, uint32_t frameHeight, int32_t format, bool force_sw = false);

	bool render(AVFrame* frame);

private:
	void close();

	bool sws_neon(AVFrame *frame, ANativeWindow_Buffer *buffer);

	bool sws_arm64(AVFrame *frame, ANativeWindow_Buffer *buffer);

	bool sws_sw(AVFrame *frame, ANativeWindow_Buffer *buffer);
private:
	ANativeWindow*		mWindow;
	bool				mForceSW;
	bool				mDoOnce;
	struct SwsContext*	mConvertCtx;
	AVFrame*			mSurfaceFrame;
};

#endif // _ANDROID_RENDER_H_