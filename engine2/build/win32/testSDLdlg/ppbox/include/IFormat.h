#ifndef _PPBOX_PPBOX_I_FORMAT_H_
#define _PPBOX_PPBOX_I_FORMAT_H_

#include "IPpbox.h"

namespace ppbox
{
    namespace demux
    {
#ifdef BOOST_BIG_ENDIAN
#define MAKE_FOURC_TYPE(c1, c2, c3, c4) \
    ((((PP_uint32)c1) << 24) | \
    (((PP_uint32)c2) << 16) | \
    (((PP_uint32)c3) << 8) | \
    (((PP_uint32)c4)))
#else
#define MAKE_FOURC_TYPE(c1, c2, c3, c4) \
    ((((PP_uint32)c1)) | \
    (((PP_uint32)c2) << 8) | \
    (((PP_uint32)c3)) << 16 | \
    (((PP_uint32)c4) << 24))
#endif

        static const PP_uint32 MEDIA_TYPE_VIDE = MAKE_FOURC_TYPE('V', 'I', 'D', 'E');
        static const PP_uint32 MEDIA_TYPE_AUDI = MAKE_FOURC_TYPE('A', 'U', 'D', 'I');
        static const PP_uint32 MEDIA_TYPE_NONE = 0;

        static const PP_uint32 VIDEO_TYPE_AVC1 = MAKE_FOURC_TYPE('A', 'V', 'C', '1');
        static const PP_uint32 VIDEO_TYPE_MP4V = MAKE_FOURC_TYPE('M', 'P', '4', 'V');
        static const PP_uint32 VIDEO_TYPE_WMV2 = MAKE_FOURC_TYPE('W', 'M', 'V', '2');
        static const PP_uint32 VIDEO_TYPE_WMV3 = MAKE_FOURC_TYPE('W', 'M', 'V', '3');
        static const PP_uint32 VIDEO_TYPE_RV40 = MAKE_FOURC_TYPE('R', 'V', '4', '0');
        static const PP_uint32 VIDEO_TYPE_MPEG4 = MAKE_FOURC_TYPE('M', 'P', 'G', '4');
        static const PP_uint32 VIDEO_TYPE_VC1 = MAKE_FOURC_TYPE('V', 'C', '1', '0');
        static const PP_uint32 VIDEO_TYPE_RGBT = MAKE_FOURC_TYPE('R', 'G', 'B', 'T');
        static const PP_uint32 VIDEO_TYPE_HEVC = MAKE_FOURC_TYPE('H', 'E', 'V', 'C');
        static const PP_uint32 VIDEO_TYPE_HVC1 = MAKE_FOURC_TYPE('H', 'V', 'C', '1');
        static const PP_uint32 VIDEO_TYPE_NONE = 0;

        static const PP_uint32 AUDIO_TYPE_MP4A = MAKE_FOURC_TYPE('M', 'P', '4', 'A');
        static const PP_uint32 AUDIO_TYPE_MP1A = MAKE_FOURC_TYPE('M', 'P', '1', 'A');
        static const PP_uint32 AUDIO_TYPE_WMA2 = MAKE_FOURC_TYPE('W', 'M', 'A', '2');
        static const PP_uint32 AUDIO_TYPE_MP3 = MAKE_FOURC_TYPE('M', 'P', '3', 0);
        static const PP_uint32 AUDIO_TYPE_MP2 = MAKE_FOURC_TYPE('M', 'P', '2', 0);
        static const PP_uint32 AUDIO_TYPE_AC3 = MAKE_FOURC_TYPE('A', 'C', '3', 0);
        static const PP_uint32 AUDIO_TYPE_EAC3 = MAKE_FOURC_TYPE('E', 'A', 'C', '3');
        static const PP_uint32 AUDIO_TYPE_COOK = MAKE_FOURC_TYPE('C', 'O', 'O', 'K');
        static const PP_uint32 AUDIO_TYPE_PCM = MAKE_FOURC_TYPE('P', 'C', 'M', 0);

        static const PP_uint32 AUDIO_TYPE_NONE = 0;

    }//end demux
}//end ppbox

#endif


