/*
 * Copyright (C) 2014 yinfei.zhao  yinfei@pptv.com
 * for xcode6 unsatisfied link error
 */
#ifdef __APPLE__
#include <stdio.h>
extern "C" {
    size_t fwrite$UNIX2003(const void *a, size_t b, size_t c, FILE *d)
    {
        return fwrite(a, b, c, d);
    }
}#endif