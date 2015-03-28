#include "logutil.h"
#include <stdio.h>
#include <stdarg.h>

int win32_log_print(int prio, const char *tag,  const char *fmt, ...)
{
	static char msg[1024] = {0};

	va_list ap;
    va_start(ap, fmt);

#ifdef SAVE_LOG_FILE
	static int log_opened = 0;
	apLog::LOG_LEVEL lvl = apLog::info;
#ifdef DEBUG_LOG_LEVEL
	lvl = apLog::detail;
#endif
	if(!log_opened) {
		apLog::init("c:\\log\\libplayer.log", lvl);
		log_opened = 1;
	}

	char header[128] = {0};
	snprintf(header, 128, "[%s] %s", tag, fmt);

	int cnt = _vsnprintf_s(msg, sizeof(msg), header, ap);
	apLog::print(0, get_log_level2(prio), "%s", msg); // "%s", msg : avoid url "%" error
#else
	_snprintf(msg, 1024, "[%.3f sec] [%s] [%s] %s\n", 0, prio, tag, fmt);
	int cnt = vprintf(msg, ap);
#endif

    va_end(ap);

	return cnt;
}