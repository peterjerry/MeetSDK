#include "log_android.h"

#ifdef __cplusplus
extern "C" {
#endif

#include "libavformat/avformat.h"

extern LogFunc pplog;

int __pp_log_print(int prio, const char *tag,  const char *fmt, ...)
{
    int log_res = 0;
    va_list ap;
    va_start(ap, fmt);
    if (pplog != NULL){
        log_res = pplog(prio, tag, fmt, ap);
    }
    if (log_res != 0){
        __android_log_vprint(prio, tag, fmt, ap);
    }
	
    va_end(ap);
	return log_res;
}

void ff_log_callback(void* avcl, int level, const char* fmt, va_list vl)
{
    AVClass* avc = avcl ? *(AVClass**)avcl : NULL;
	static char msg[4096] = {0};
	vsnprintf(msg, sizeof(msg), fmt, vl);
    int32_t androidLevel = ANDROID_LOG_UNKNOWN;
	switch(level)
    {
		case AV_LOG_PANIC:
            androidLevel = ANDROID_LOG_FATAL;
			break;
		case AV_LOG_FATAL:
            androidLevel = ANDROID_LOG_FATAL;
			break;
		case AV_LOG_ERROR:
            androidLevel = ANDROID_LOG_ERROR;
			break;
		case AV_LOG_WARNING:
            androidLevel = ANDROID_LOG_WARN;
			break;

		case AV_LOG_INFO:
            androidLevel = ANDROID_LOG_INFO;
			break;

		case AV_LOG_DEBUG:
            androidLevel = ANDROID_LOG_DEBUG;
			break;

		case AV_LOG_VERBOSE:
            androidLevel = ANDROID_LOG_VERBOSE;
			break;

	}
	__android_log_print(androidLevel, "ffmpeg", "[%s]%s", (avc != NULL) ? avc->class_name : "", msg);
}

#ifdef __cplusplus
}
#endif