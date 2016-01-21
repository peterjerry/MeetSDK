#ifndef FF_LOG_H
#define FF_LOG_H

#if defined(__ANDROID__)
	#include "log_android.h"
#elif defined(OS_IOS) // __APPLE__
	#include "log_ios.h"
#else
	#include "log_win32.h"
#endif

#endif // FF_LOG_H