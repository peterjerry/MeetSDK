#include "log_win32.h"
#include "utils.h"

#ifdef SAVE_LOG_FILE
#include "apFilelog.h"

static apLog::LOG_LEVEL get_log_level2(int lvl)
{
	apLog::LOG_LEVEL ret_level;

	switch(lvl) {
	case WIN32_LOG_VERBOSE:
	case WIN32_LOG_DEBUG:
		ret_level = apLog::detail;
		break;
	case WIN32_LOG_INFO:
		ret_level = apLog::info;
		break;
	case WIN32_LOG_WARN:
		ret_level = apLog::warning;
		break;
	case WIN32_LOG_ERROR:
		ret_level = apLog::error;
		break;
	default:
		ret_level = apLog::info;
		break;
	}

	return ret_level;
}
#endif

#ifdef _MSC_VER
#define snprintf _snprintf
#endif
#define TOSTRING(x) #x

static const char *get_log_level(int lvl)
{
	if (WIN32_LOG_VERBOSE == lvl)
		return "verbose";
	else if (WIN32_LOG_DEBUG == lvl)
		return "debug";
	else if (WIN32_LOG_INFO == lvl)
		return "info";
	else if (WIN32_LOG_WARN == lvl)
		return "warn";
	else if (WIN32_LOG_ERROR == lvl)
		return "error";
	else
		return "unknown";
}

int __pp_log_print(int prio, const char *tag,  const char *fmt, ...)
{
	// 2015.6.5 michael.ma remove static because this func is not thread-safe
	char msg[1024] = {0};

	va_list ap;
    va_start(ap, fmt);

#ifdef SAVE_LOG_FILE
	char header[256] = {0};
	snprintf(header, 256, "[%s] %s", tag, fmt);

	int cnt = _vsnprintf_s(msg, sizeof(msg), header, ap);
	apLog::print(0, get_log_level2(prio), "%s", msg); // "%s", msg : avoid url "%" error
#else
	int64_t msec = getNowMs();
	_snprintf_s(msg, 1024, "[%.3f sec] [%s] [%s] %s\n", (double)(msec % 1000000000) / (double)1000000, get_log_level(prio), tag, fmt);
	int cnt = vprintf(msg, ap);
#endif

    va_end(ap);

	return cnt;
}
