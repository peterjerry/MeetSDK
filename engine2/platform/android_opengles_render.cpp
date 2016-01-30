#include "android_opengles_render.h"
#include "ppffmpeg.h"
#include <pthread.h>
#define LOG_TAG "android_opengles_render"
#include "log.h"

extern JavaVM *gs_jvm;
jobject g_renderer;

extern "C" android_gles_render* getRenderer()
{
	return new android_gles_render();
}

static void printGLString(const char *name, GLenum s) {
    const char *v = (const char *) glGetString(s);
    LOGI("GL %s = %s\n", name, v);
}

static void checkGlError(const char* op) {
    for (GLint error = glGetError(); error; error = glGetError()) {
        LOGI("after %s() glError (0x%x)", op, error);
    }
}

static const char gVertexShader[] = 
    "attribute vec4 vPosition;\n"
    "void main() {\n"
    "  gl_Position = vPosition;\n"
    "}\n";

static const char gFragmentShader[] = 
    "precision mediump float;\n"
    "void main() {\n"
    "  gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0);\n"
    "}\n";

static const char gYUVVertexShader[] = 
    "attribute vec4 aPosition;\n"
	"attribute vec2 aTexCoord;\n"
	"varying vec2 v_TexCoord;\n"
    "void main() {\n"
    "  gl_Position = aPosition;\n"
	"  v_TexCoord = aTexCoord;\n"
    "}\n";

static const char gYUVFragmentShader[] = 
	"precision mediump float;\n"
	"varying vec2 v_TexCoord;\n"
	"\n"
	"uniform sampler2D u_tex_y;\n"
	"uniform sampler2D u_tex_u;\n"
	"uniform sampler2D u_tex_v;\n"
	"\n"
	"void main()\n"
	"{\n"
	"	vec4 c = vec4((texture2D(u_tex_y, v_TexCoord).r - 16./255.) * 1.164);\n"
	"	vec4 U = vec4(texture2D(u_tex_u, v_TexCoord).r - 128./255.);\n"
	"	vec4 V = vec4(texture2D(u_tex_v, v_TexCoord).r - 128./255.);\n"
	"\n"
	"	c += V * vec4(1.596, -0.813, 0, 0);\n"
	"	c += U * vec4(0, -0.392, 2.017, 0);\n"
	"	c.a = 1.0;\n"
	"\n"
	"	gl_FragColor = c;\n"
	"}\n";

GLuint loadShader(GLenum shaderType, const char* pSource)
{
    GLuint shader = glCreateShader(shaderType);
    if (shader) {
        glShaderSource(shader, 1, &pSource, NULL);
        glCompileShader(shader);
        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen) {
                char* buf = (char*) malloc(infoLen);
                if (buf) {
                    glGetShaderInfoLog(shader, infoLen, NULL, buf);
                    LOGE("Could not compile shader %d:\n%s\n",
                            shaderType, buf);
                    free(buf);
                }
                glDeleteShader(shader);
                shader = 0;
            }
        }
    }
    return shader;
}

GLuint createProgram(const char* pVertexSource, const char* pFragmentSource) {
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, pVertexSource);
    if (!vertexShader) {
        return 0;
    }

    GLuint pixelShader = loadShader(GL_FRAGMENT_SHADER, pFragmentSource);
    if (!pixelShader) {
        return 0;
    }

    GLuint program = glCreateProgram();
    if (program) {
        glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        glLinkProgram(program);
        GLint linkStatus = GL_FALSE;
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
        if (linkStatus != GL_TRUE) {
            GLint bufLength = 0;
            glGetProgramiv(program, GL_INFO_LOG_LENGTH, &bufLength);
            if (bufLength) {
                char* buf = (char*) malloc(bufLength);
                if (buf) {
                    glGetProgramInfoLog(program, bufLength, NULL, buf);
                    LOGE("Could not link program:\n%s\n", buf);
                    free(buf);
                }
            }
            glDeleteProgram(program);
            program = 0;
        }
    }
    return program;
}

bool android_gles_render::ogl_init(int w, int h)
{
	LOGI("olg_init %d x %d", w, h);

    printGLString("Version", GL_VERSION);
    printGLString("Vendor", GL_VENDOR);
    printGLString("Renderer", GL_RENDERER);
    printGLString("Extensions", GL_EXTENSIONS);

    m_program = createProgram(gYUVVertexShader, gYUVFragmentShader);
    if (!m_program) {
        LOGE("Could not create program.");
        return false;
    }

    m_aPositionHandle = glGetAttribLocation(m_program, "aPosition");
	if (m_aPositionHandle < 0) {
		LOGE("aPosition not found ...");
		return false;
	}
    checkGlError("glGetAttribLocation aPosition");
    LOGI("glGetAttribLocation(\"aPosition\") = %d", m_aPositionHandle);

	m_aTexCoordHandle = glGetAttribLocation(m_program, "aTexCoord");
	if (m_aTexCoordHandle < 0) {
		LOGE("aTexCoord not found ...");
		return false;
	}
    checkGlError("glGetAttribLocation aTexCoord");
    LOGI("glGetAttribLocation(\"aTexCoord\") = %d", m_aTexCoordHandle);

	m_uTexHandle[0] = glGetUniformLocation(m_program, "u_tex_y");
	m_uTexHandle[1] = glGetUniformLocation(m_program, "u_tex_u");
	m_uTexHandle[2] = glGetUniformLocation(m_program, "u_tex_v");
	checkGlError("glGetUniformLocation yuv");

	glGenTextures(3, m_textures);
	checkGlError("glGenTextures");

    glViewport(0, 0, w, h);
    checkGlError("glViewport");

	m_glResourcesInitialized = true;
    return true;
}

void android_gles_render::ogl_uninit()
{
	if (m_glResourcesInitialized) {
		glDeleteTextures(3, m_textures);
		glDeleteProgram(m_program);
	}

	m_glResourcesInitialized = false;
}

const GLfloat gTriangleVertices[] = { 
	0.0f, 0.5f, 
	-0.5f,-0.5f,
	0.5f, -0.5f };

const GLfloat vVertices[] = {
		-1.0f,  1.0f, 0.0f,// Position 0 
		 0.0f, 0.0f, // TexCoord 0 
		-1.0f, -1.0f, 0.0f,// Position 1 
		 0.0f, 1.0f, // TexCoord 1 
		 1.0f, -1.0f, 0.0f,// Position 2 
		 1.0f, 1.0f, // TexCoord 2 
		 1.0f,  1.0f, 0.0f,// Position 3 
		 1.0f, 0.0f // TexCoord 3 
	};

GLushort indices[] = { 0, 1, 2, 0, 2, 3 };

void android_gles_render::ogl_render() {
	if (!m_glResourcesInitialized || !m_ogl_ready) {
		LOGI("ogl not ready");
		return;
	}

    /*static float grey;
    grey += 0.01f;
    if (grey > 1.0f) {
        grey = 0.0f;
    }
    glClearColor(grey, grey, grey, 1.0f);*/
	glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    checkGlError("glClearColor");
    glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
    checkGlError("glClear");

    glUseProgram(m_program);
    checkGlError("glUseProgram");

    //glVertexAttribPointer(m_aPositionHandle, 2, GL_FLOAT, GL_FALSE, 0, vVertices);

	GLsizei stride = 5 * sizeof(GLfloat); // 3 for position, 2 for texture 
	// Load the vertex position 
	glVertexAttribPointer(m_aPositionHandle, 3, GL_FLOAT, GL_FALSE, stride, vVertices); 
	checkGlError("m_aPositionHandle");
	glEnableVertexAttribArray(m_aPositionHandle);
	checkGlError("glEnableVertexAttribArray vPosition");
	// Load the texture coordinate 
	glVertexAttribPointer(m_aTexCoordHandle, 2, GL_FLOAT, GL_FALSE, stride, vVertices + 3); 
	checkGlError("m_aTexCoordHandle"); 
	glEnableVertexAttribArray(m_aTexCoordHandle); 
	checkGlError("glEnableVertexAttribArray aTexCoord");

	pthread_mutex_lock(&m_yuv_mutex);

	bind_texture(m_textures[0], (char *)m_frame->data[0], 
		m_frame->width, m_frame->height);
	bind_texture(m_textures[1], (char *)m_frame->data[1], 
		m_frame->width >> 1, m_frame->height >> 1);
	bind_texture(m_textures[2], (char *)m_frame->data[2], 
		m_frame->width >> 1, m_frame->height >> 1);

	/* upload Y plane */
	glActiveTexture(GL_TEXTURE0);  
    checkGlError("glActiveTexture");  
    glBindTexture(GL_TEXTURE_2D, m_textures[0]);  
    checkGlError("glBindTexture");  
	glUniform1i(m_uTexHandle[0], 0);  
    checkGlError("glUniform1i");

	/* upload U plane */
	glActiveTexture(GL_TEXTURE1);  
    checkGlError("glActiveTexture");  
    glBindTexture(GL_TEXTURE_2D, m_textures[1]);  
    checkGlError("glBindTexture");  
	glUniform1i(m_uTexHandle[1], 1);  
    checkGlError("glUniform1i");

	/* upload V plane */
	glActiveTexture(GL_TEXTURE2);  
    checkGlError("glActiveTexture");  
    glBindTexture(GL_TEXTURE_2D, m_textures[2]);  
    checkGlError("glBindTexture");  
	glUniform1i(m_uTexHandle[2], 2);  
    checkGlError("glUniform1i");

	pthread_mutex_unlock(&m_yuv_mutex);

    //glDrawArrays(GL_TRIANGLES, 0, 3);
	//checkGlError("glDrawArrays");
	glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, indices);
    checkGlError("glDrawElements");
}

void android_gles_render::bind_texture(GLuint texture, const char *buffer, int w , int h)
{
	glBindTexture(GL_TEXTURE_2D, texture);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, 
		w, h, 
		0, GL_LUMINANCE, GL_UNSIGNED_BYTE, buffer);
}

android_gles_render::android_gles_render()
{
	pthread_mutex_init(&m_yuv_mutex, 0);
	m_frame = NULL;
	memset(m_textures, 0, sizeof(m_textures));
	m_aPositionHandle = -1;
	m_aTexCoordHandle = -1;
	memset(m_uTexHandle, 0, sizeof(m_uTexHandle));
	m_ogl_ready = false;
	m_glResourcesInitialized = false;
	m_request_render_id = 0;
}

android_gles_render::~android_gles_render()
{
	if (m_frame)
		av_frame_free(&m_frame);
	pthread_mutex_destroy(&m_yuv_mutex);
}

bool android_gles_render::init_render(void* ctx, int w, int h, int pix_fmt, bool force_sw)// w ,h is video size
{
	(void)ctx;

	LOGI("init_render %d x %d", w, h);

	bool res = true;
	if (pix_fmt != AV_PIX_FMT_YUV420P) {
		LOGE("only support YUV420p render");
		return false;
	}

	m_frame = av_frame_alloc();
	m_frame->width = w;
	m_frame->height = h;
	m_frame->format = pix_fmt;
	av_frame_get_buffer(m_frame, true);
	LOGI("m_frame allocated data %p %p %p(linesize %d %d %d)", m_frame->data[0], m_frame->data[1], m_frame->data[2],
		m_frame->linesize[0], m_frame->linesize[1], m_frame->linesize[2]);

	m_ogl_ready = true;
	return res;
}

bool android_gles_render::render_one_frame(AVFrame* frame, int pix_fmt)
{
	if (pix_fmt != AV_PIX_FMT_YUV420P) {
		LOGE("not yuv420p");
		return false;
	}

	pthread_mutex_lock(&m_yuv_mutex);
	av_frame_copy(m_frame, frame);
	pthread_mutex_unlock(&m_yuv_mutex);

	//ogl_display_set_yuv_to_display(yuv);

	/*JNIEnv *env = NULL;
	jint status;
	status = gs_jvm->GetEnv((void**) &env, JNI_VERSION_1_4);
	if (status == JNI_EDETACHED) {
		status = gs_jvm->AttachCurrentThread(&env, NULL);
		if (status != JNI_OK) {
			LOGE("AttachCurrentThread failed %d", status);
			return false;
		}
	}
	else if (status != JNI_OK) {
		return false;
	}

	LOGI("4444444444");
	env->CallVoidMethod(g_renderer, m_request_render_id);*/
	return true;
}

void android_gles_render::setRequestMethod(JNIEnv *env, jobject clazz, jmethodID mid)
{
	g_renderer = env->NewGlobalRef(clazz);
	m_request_render_id = mid;
}

void android_gles_render::re_size(int width, int height)
{
}

void android_gles_render::aspect_ratio(int srcw, int srch, bool enable_aspect)
{

}

void android_gles_render::destory_render()
{
	m_ogl_ready = false;
}

bool android_gles_render::use_overlay()
{
	return false;
}

unsigned int align_on_power_of_2(unsigned int value) 
{
	int i;
	/* browse all power of 2 value, and find the one just >= value */
	for(i=0; i<32; i++) 
	{
		unsigned int c = 1 << i;
		if (value <= c)
			return c;
	}
	return 0;
}
