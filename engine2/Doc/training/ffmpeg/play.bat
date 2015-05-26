ffplay "http://wx.live.bestvcdn.com.cn/live/program/live991/weixinssty/live.m3u8?se=weixin&ct=2&_cp=1&_fk=691522723945717F8FE821C2B908E20973D1C1EBC26DA8284E91400A3E7C017A"
@pause

http://wx.live.bestvcdn.com.cn/live/program/live991/weixindycj/index.m3u8?se=weixin&ct=2&_cp=1&_fk=691522723945717F8FE821C2B908E20973D1C1EBC26DA8284E91400A3E7C017A
rem E:\QQDownload\Manhattan.S01E08.720p.HDTV.x264-KILLERS\Manhattan.S01E08.HDTVrip.1024X576.mkv

ffplay -i D:\origin.pcm -f f32le -channels 2 -sample_rate 24000
@pause

rem  -vf yadif
@pause
ffplay rtmp://114.80.186.139:1935/live/8c8b1494040d4f29bfba70d95612a191
@pause
ffplay "http://117.135.161.38:80/2505f50d894ca732d86270eb1fd238bc.mp4?w=1&key=c1cbcb3af629ab72c8ae87525a43b87b&k=5d37085fe0bbb69153e76a3a64265f92-b849-1431437754&type=phone.android.vip&vvid=877a4382-f0e4-49ed-afea-8d59dbd11df1&sv=4.1.3&platform=android3&ft=1&accessType=wifi"
@pause

ffplay "http://117.135.161.23:80/2505f50d894ca732d86270eb1fd238bc.mp4?w=1&key=48fa61152ee969fd3e75071f1e42a097&k=d2e35ab63dc306789cc6c3cc22f31f6f-b6fe-1431437292&type=phone.android.vip&vvid=877a4382-f0e4-49ed-afea-8d59dbd11df1&sv=4.1.3&platform=android3&ft=1&accessType=wifi"
@pause

ffplay "http://127.0.0.1:9006/record.m3u8?type=pplive3&playlink=36522%%3Fft%%3D1%%26name%%3D02f1b124b3134c11b8b639bb10211dfd%%26svrhost%%3D60.55.12.166%%3A80%%26svrtime%%3D1420698951%%26delaytime%%3D0%%26bitrate%%3D400%%26interval%%3D5%%26bwtype%%3D0%%26sdkmode%%3D0%%26livepath%%3D%%2Flive%%26begin_time%%3D1418728413%%26end_time%%3D1418732127%%26type%%3Dipptv%%26platform%%3Daph%%26appplt%%3Daph%%26appid%%3DPPTVIBOBO%%26appver%%3D2.0.1&realtime=low&serialnum=3"
@pause

ffplay -i D:\FRIM_x86_version_1.23\x86\1.yuv -f rawvideo -pixel_format yuv420p -video_size 640x480 -framerate 10
@pause

ffplay -i E:\in1920.yuv -f rawvideo -pixel_format yuv420p -video_size 1920x1080 -framerate 10
@pause

ffplay -i "\\192.168.81.166\zodiac\Library\Application Support\iPhone Simulator\7.0\Applications\C8865DC9-755D-45DE-8EF5-C3309A813A6C\Documents\out.rgba" -f rawvideo -pixel_format rgba -video_size 1280x720 -framerate 10
@pause

rem ffplay -i udp://127.0.0.1:2003?localport=2003
ffplay -i udp://127.0.0.1:2003 -fifo_size 0
@pause


ffplay -i E:\in.yuv -f rawvideo -pixel_format yuv420p -video_size 1280x720 -framerate 10
@pause

E:\Software\FFMPEG64\ffplay.exe -i E:\Archive\Media\test\video\h264_quality\out_60.h264 -framerate 10
@pause

ffplay -i e:\out_neon.pcm -f s16le -channels 1 -ar 24000
@pause

ffplay -i e:\out.yuv2 -f rawvideo -pixel_format yuyv422 -video_size 640x480 -framerate 25
@pause

ffplay -i "\\192.168.81.165\zodiac\Library\Application Support\iPhone Simulator\7.0\Applications\C8865DC9-755D-45DE-8EF5-C3309A813A6C\Documents\out.rgba"  -f rawvideo -pixel_format rgba -video_size 640x480 -framerate 5
@pause

ffplay -i "\\192.168.81.179\cstest2\Library\Application Support\iPhone Simulator\6.1\Applications\0A65FF36-3BE7-4A91-8588-FBC7AA16ED70\Documents\out.rgba" -f rawvideo -pixel_format rgba -video_size 1280x720 -framerate 1
@pause

ffplay -i e:\480_320_100frame.nv21 -f rawvideo -pixel_format nv21 -video_size 480x320 -framerate 25
@pause

E:\Software\FFMPEG64\ffplay.exe -i C:\hls\hls_low_wide\index.m3u8
@pause

#PLSEXTM3U
#EXT-X-TARGETDURATION:2621
#EXT-X-VERSION:2
#EXT-X-DISCONTINUITY
#EXTINF:362

rem rgb565le 320x240
rem bgr565be 320x240
rem yuv420p 352x240
rem yuv420p 672x480
rem yuyv422

rem f16le flt
rem s16le
rem s8

rem ffplay -x 640 -y 480 E:\share\sample\video\新水浒传[第34集DVD].mp4
rem E:\resource\Rec_clip\cctv3\Part38.ts
rem E:\Archive\Media\sample\ironman3-clp_h720p.mov