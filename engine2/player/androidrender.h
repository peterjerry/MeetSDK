#ifndef _ANDROID_RENDER_H_
#define _ANDROID_RENDER_H_

#include "renderbase.h"

struct ANativeWindow;
struct ANativeWindow_Buffer;

class AndroidRender : public RenderBase {
public:
	AndroidRender();
	~AndroidRender(void);

	// ≥ı ºrender.
	virtual bool init_render(void* ctx, int w, int h, int pix_fmt, bool force_sw = false);

	// ‰÷»æ“ª÷°.
	virtual bool render_one_frame(AVFrame* frame, int pix_fmt);

private:
	void close();

	bool sws_neon(AVFrame *frame, ANativeWindow_Buffer *buffer);

	bool sws_arm64(AVFrame *frame, ANativeWindow_Buffer *buffer);

	bool sws_sw(AVFrame *frame, ANativeWindow_Buffer *buffer);
private:
	ANativeWindow*		mWindow;
	bool				mForceSW;
	bool				mDoOnce;
#ifdef USE_SWSCALE
	struct SwsContext*	mConvertCtx;
#endif
	AVFrame*			mSurfaceFrame;
};

#endif // _ANDROID_RENDER_H_