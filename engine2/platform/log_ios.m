#import <Foundation/NSObjCRuntime.h>
#import <Foundation/NSString.h>
#include "log_ios.h"
#include "utils.h"

#define MAX_STR_LEN 2048

static const char* getPrio(int prio);

void __ios_log(int prio, const char* tag, const char *fmt, ...)
{
    va_list vl;
    va_start(vl, fmt);
    
    char msg[MAX_STR_LEN] = {0};
    char header[256] = {0};
    char timestamp[128] = {0};
    getStrTime(timestamp);
    snprintf(header, 128, "%s [%s] [%s] %s", timestamp, getPrio(prio), tag, fmt);
    vsnprintf(msg, sizeof(msg), header, vl);
    printf("%s\n", msg);
    
    va_end(vl);
}

static const char* getPrio(int prio)
{
    const char *str_prio = NULL;
    switch (prio) {
        case IOS_LOG_UNKNOWN:
            str_prio = "unknown";
            break;
        case IOS_LOG_VERBOSE:
            str_prio = "verbose";
            break;
        case IOS_LOG_DEBUG:
            str_prio = "debug";
            break;
        case IOS_LOG_INFO:
            str_prio = "info";
            break;
        case IOS_LOG_WARN:
            str_prio = "warning";
            break;
        case IOS_LOG_ERROR:
            str_prio = "error";
            break;
        default:
            str_prio = "unknown";
            break;
    }
    
    return str_prio;
}