#ifndef FFMPEG_H
#define FFMPEG_H

extern "C" {
#ifdef __CYGWIN__
#ifndef   UINT64_C
#define   UINT64_C(value)__CONCAT(value,ULL)
#endif
#define INT64_MIN        (__INT64_C(-9223372036854775807)-1)
#define INT64_MAX        (__INT64_C(9223372036854775807))
#endif

#ifdef __cplusplus
#ifndef __STDC_CONSTANT_MACROS
#define __STDC_CONSTANT_MACROS
#endif
#ifdef _STDINT_H
#undef _STDINT_H
#endif
#include <stdint.h>
#endif

#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/avutil.h"
#include "libavutil/opt.h"
#include "libswscale/swscale.h"
#include "libswresample/swresample.h"
} // end of extern C

#endif // FFMPEG_H