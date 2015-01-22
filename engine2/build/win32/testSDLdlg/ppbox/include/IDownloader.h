// IDownloader.h

#ifndef _PPBOX_PPBOX_I_DOWNLOADER_H_
#define _PPBOX_PPBOX_I_DOWNLOADER_H_

#include "IPpbox.h"

#if __cplusplus
extern "C" {
#endif // __cplusplus

    // refine
    typedef void * PPBOX_Download_Handle;

    static const PPBOX_Download_Handle PPBOX_INVALID_DOWNLOAD_HANDLE = NULL;

    typedef struct tag_PPBOX_frame // IN
    {
        PP_uint32 stream_index;     // 流的编号
        PP_uint32 start_time;       // Sample对应的时间戳
        PP_uint32 cts_delta;       // dts cts差值，无差值为0
        PP_uint32 buffer_length;    // Frame的大小
        PP_uchar const * buffer;    // Frame的内容
    } PPBOX_Frame;

    typedef void (* PPBOX_Download_Callback)(
        PPBOX_Download_Handle, 
        PP_int32);

    //打开一个下载用例
    PPBOX_DECL PPBOX_Download_Handle PPBOX_DownloadOpen(
        char const * playlink,
        char const * format,
        char const * save_filename,
        PPBOX_Download_Callback resp);

    //关闭指定的下载用例
    PPBOX_DECL void PPBOX_DownloadClose(
        PPBOX_Download_Handle hander);

    //删除指定的下载用例
    PPBOX_DECL void PPBOX_DownloadRemove(
        char const * playlink
        , char const * format);

    // 检测已下载数据的完整性
    PPBOX_DECL PP_int32 PPBOX_CheckDownload(
        char const * playlink
        , char const * format);

    PPBOX_DECL const PP_char* PPBOX_DownloadFileList(
        PP_char const * playlink
        , PP_char const * format);

    typedef struct tag_PPBOX_DownloadStatistic
    {
        PP_uint64 total_size;
        PP_uint64 finish_size;
        PP_uint32 speed; 
    } PPBOX_DownloadStatistic;

    // 获取指定下载用例的实时统计信息
    PPBOX_DECL PP_int32 PPBOX_GetDownloadInfo(
        PPBOX_Download_Handle hander,
        PPBOX_DownloadStatistic * stat);

    typedef struct tag_PPBOX_DownloadResult
    {
        PP_uint32 his_max_speed;  //历史最大速度
        PP_uint32 cur_max_speed;  //当前最大速度
        PP_uint32 bwtype;    
        PP_uint32 speed_limit; //0未限速 1已限速
        char error_code[32];   //原因码 
        char reason[64];   //原因信息
        char cur_cdn[32];   //当前cdn
        char main_cdn[32];   //主cdn
        char bakup_cdn[128]; //备cdn
    } PPBOX_DownloadResult;

    // 获取指定下载的结果
    PPBOX_DECL PP_int32 PPBOX_GetDownloadResult(
        PPBOX_Download_Handle hander,
        PPBOX_DownloadResult * stat);

#if __cplusplus
}
#endif // __cplusplus

#endif // _PPBOX_PPBOX_I_DOWNLOADER_H_
