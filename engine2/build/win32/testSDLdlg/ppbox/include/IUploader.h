// IUploader.h

#ifndef _PPBOX_PPBOX_I_UPLOADER_H_
#define _PPBOX_PPBOX_I_UPLOADER_H_

#include "IPpbox.h"
#include "IDemuxer.h"

#if __cplusplus
extern "C" {
#endif // __cplusplus


    enum PPBOX_AvcConfigTypeEnum
    {
        avc_config_packet = 0, 
        avc_config_stream = 1, 
    };

    enum PPBOX_SortTypeEnum
    {
        en_streams_un_sort = 0,
        en_streams_sort = 1, 
    };

    // refine
    typedef void * PPBOX_Upload_Handle;

    typedef void (* PPBOX_Upload_Callback)(
        PPBOX_Upload_Handle, 
        PP_int32);

    typedef struct tag_PPBOX_UpSample // IN
    {
        PP_uint32 itrack;     // 流的编号
        PP_uint32 flags;  // 1 key frame
        PP_uint32 time;       // Sample对应的时间戳
        PP_uint32 dts;       // Sample对应的时间戳
        PP_uint32 cts_delta;       // dts cts差值，无差值为0
        PP_uint32 size;    // Frame的大小
        PP_uchar* buf;    // Frame的内容
    } PPBOX_UpSample;


    typedef struct tag_PPBOX_UpStreamInfo
    {
        PP_int32 time_scale;
        PP_int32 bitrate;

        union 
        {
            PPBOX_VideoInfo video_format;
            PPBOX_AudioInfo audio_format;
        };

        PP_uint32 format_type;  // 0 packet  1 stream
        PP_uint32 format_size;  // 格式说明的大小
        PP_uchar const * format_buffer;   // 不同的解码不同的结构体，格式说明的的内容
    } PPBOX_UpStreamInfo;


    typedef struct tag_PPBOX_UpConfig // IN
    {
        PP_uint32 stream_count;       // 流的个数
        PP_uint32 thread_count;       // 线程个数 1 表示单线程序
        PP_uint16 sort_type;       // 1  需要排序, 0不需要排序
    } PPBOX_UpConfig;


    PPBOX_DECL PPBOX_Upload_Handle PPBOX_UploadOpen(
                char const * playlink,
                char const * format,
                char const * outUrl,
                PPBOX_Upload_Callback resp);

    PPBOX_DECL void PPBOX_UploadClose(
        PPBOX_Upload_Handle hander);

    PPBOX_DECL void PPBOX_UploadParam(
        PPBOX_Upload_Handle hander,
        PP_char const * key,
        PP_char const * value);

    PPBOX_DECL void PPBOX_UploadConfig(
        PPBOX_Upload_Handle hander,
        PPBOX_UpConfig conf);

    PPBOX_DECL PP_int32 PPBOX_UploadAddStream(
        PPBOX_Upload_Handle hander,
        PP_uint32 index,
        PPBOX_UpStreamInfo info);

    PPBOX_DECL void PPBOX_UploadPutSample(
        PPBOX_Upload_Handle hander,
        PPBOX_UpSample sample);

    typedef struct tag_PPBOX_UploadStatistic
    {
        PP_uint32 time; //send time
        PP_uint32 remaining_time;// 单位毫秒
    } PPBOX_UploadStatistic;

    PPBOX_DECL PP_int32 PPBOX_GetUploadInfo(
        PPBOX_Upload_Handle hander,
        PPBOX_UploadStatistic * stat);

#if __cplusplus
}
#endif // __cplusplus

#endif // _PPBOX_PPBOX_I_UploadER_H_
