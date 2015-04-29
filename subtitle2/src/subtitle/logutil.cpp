#include "logutil.h"
#include <stdio.h>
#include <stdarg.h>
#include <stdint.h>

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

int win32_log_print(int prio, const char *tag,  const char *fmt, ...)
{
	static char msg[1024] = {0};

	va_list ap;
    va_start(ap, fmt);

#ifdef SAVE_LOG_FILE
	char header[128] = {0};
	_snprintf_s(header, 128, "[%s] %s", tag, fmt);

	int cnt = _vsnprintf_s(msg, sizeof(msg), header, ap);
	apLog::print(0, get_log_level2(prio), "%s", msg); // "%s", msg : avoid url "%" error
#else
	int64_t msec = 0;//getNowMs();
	_snprintf_s(msg, 1024, "[0 sec] [%s] [%s] %s\n", get_log_level(prio), tag, fmt);
	int cnt = vprintf(msg, ap);
#endif

    va_end(ap);

	return cnt;
}
