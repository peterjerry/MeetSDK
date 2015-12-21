#include "log_android.h"
#include "common.h"
#include <stdio.h> // for NULL

#ifdef __cplusplus
extern "C" {
#endif

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

#ifdef __cplusplus
}
#endif