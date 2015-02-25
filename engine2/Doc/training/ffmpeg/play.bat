ffplay -i E:\out.rgb -f rawvideo -pixel_format rgb565le -video_size 640x480 -framerate 10
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

rem ffplay -x 640 -y 480 E:\share\sample\video\新水浒传[第34集DVD].mp4
rem E:\resource\Rec_clip\cctv3\Part38.ts
rem E:\Archive\Media\sample\ironman3-clp_h720p.mov