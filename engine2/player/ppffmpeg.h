#ifndef PP_FFMPEG_H_
#define PP_FFMPEG_H_

#ifdef __cplusplus
extern "C" {
#endif

#ifdef __cplusplus
#ifdef _STDINT_H
#undef _STDINT_H
#endif
#include <stdint.h>
#endif

#ifdef _MSC_VER
#define inline _inline
#ifndef UINT64_C
#define UINT64_C(val) val##ui64
#define INT64_C(val)  val##i64
#endif
#ifndef INT64_MIN
#define INT64_MIN        (INT64_C(-9223372036854775807)-1)
#define INT64_MAX        (INT64_C(9223372036854775807))
#endif
#else
#ifndef UINT64_C
#define UINT64_C(value)__CONCAT(value,ULL)
#endif
#ifndef INT64_MIN
#define INT64_MIN        (__INT64_C(-9223372036854775807)-1)
#define INT64_MAX        (__INT64_C(9223372036854775807))
#endif
#endif

#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/avutil.h"
#include "libavutil/opt.h"
#include "libavutil/dict.h"
#include "libavutil/time.h" // for av_usleep()
#include "libavutil/imgutils.h" // for av_image_alloc()
#include "libswresample/swresample.h"
#ifdef USE_SWSCALE
#include "libswscale/swscale.h"
#endif
#ifdef USE_AV_FILTER
#include "libavfilter/avfilter.h"
#include "libavfilter/buffersrc.h"
#include "libavfilter/buffersink.h"
#endif

#ifdef __cplusplus
} // end of extern C
#endif

#endif // PP_FFMPEG_H_