#ifndef ANDROID_OPENGLES_RENDER_
#define ANDROID_OPENGLES_RENDER_

#include "renderbase.h"
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <pthread.h>
#include <jni.h>

#define TEXTURE_BUFFER_SIZE 3

class android_gles_render : public RenderBase
{
public:
	android_gles_render();
	virtual ~android_gles_render();
public:
	// 初始render.
	virtual bool init_render(void* ctx, int w, int h, int pix_fmt, bool force_sw = false);

	// 渲染一帧.
	virtual bool render_one_frame(AVFrame* data, int pix_fmt);

	// 调整大小.
	virtual void re_size(int width, int height);

	// 设置宽高比.
	virtual void aspect_ratio(int srcw, int srch, bool enable_aspect);

	// 撤销render.
	virtual void destory_render();
	
	virtual bool use_overlay();

	void setRequestMethod(JNIEnv *env, jobject clazz, jmethodID mid);

	// gles
	bool ogl_init(int w, int h);
	
	void ogl_render();

private:
	void bind_texture(GLuint texture, const char *buffer, int w , int h);

	void ogl_uninit();
private:
	/* input: yuv image to display */
	pthread_mutex_t m_yuv_mutex;
	AVFrame* m_frame;

	/* GL resources */
	bool m_glResourcesInitialized;
	GLuint m_program;
	GLuint m_textures[TEXTURE_BUFFER_SIZE];
	GLint m_aPositionHandle;
	GLint m_aTexCoordHandle;
	GLint m_uTexHandle[TEXTURE_BUFFER_SIZE];
	
private:
	/* about JNI */
	jboolean m_ogl_ready;
	jobject m_clazz;
	jmethodID m_request_render_id;
};

extern "C" android_gles_render* getRenderer();

#endif //ANDROID_OPENGLES_RENDER_