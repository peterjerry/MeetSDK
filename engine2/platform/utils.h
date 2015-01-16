/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */

#ifndef FF_UTILS_H
#define FF_UTILS_H

#include <stdint.h>

#if __cplusplus
extern "C" {
#endif

int64_t getNowSec();    
int64_t getNowMs();
int64_t getNowUs();

#ifdef OS_IOS
int32_t getStrTime(char* buff);
#endif
    
#if __cplusplus
}
#endif

#endif
