#import <Foundation/NSObjCRuntime.h>
#import <Foundation/NSString.h>
#include "log_ios.h"
#include "utils.h"


void __ios_log(const char* tag, const char *fmt, ...)
{
    va_list vl;
    va_start(vl, fmt);
    
    char msg[2048] = {0};
    int32_t len = getStrTime(msg);
    strcpy(msg+len, tag);
    len+=strlen(tag);
    strcpy(msg+len, " ");
    len+=strlen(" ");
    if(len < sizeof(msg))
    {
        vsnprintf(msg+len, sizeof(msg)-len, fmt, vl);
        printf("%s\n", msg);
    }
    
    va_end(vl);
}

void ff_log_callback(void* avcl, int level, const char* fmt, va_list vl)
{
    char msg[2048];
    int32_t len = getStrTime(msg);
    strcpy(msg+len, "ffmpeg ");
    len+=7;
    if(len < sizeof(msg))
    {
        vsnprintf(msg+len, sizeof(msg)-len, fmt, vl);
        printf("%s\n", msg);
        //NSLog(@"%s:%s\n", "ffmpeg", msg);
    }
}

