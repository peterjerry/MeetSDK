ffmpeg -i D:\Archive\media\ironman3-clp_h720p.mov -vcodec libx265 -acodec copy -t 10 E:\sample.mp4
@pause

ffmpeg -ss 00:00:05 -i D:\Archive\media\ironman3-clp_h720p.mov -f rawvideo -pix_fmt yuyv422 -s 640x480 -t 1 E:\in640.yuy2
@pause

ffmpeg -i d:\TimeCode.mov -vcodec h263 -s 352x288 -acodec libvo_aacenc e:\timecode.3gp
@pause

ffmpeg -i D:\Archive\media\ironman3-clp_h720p.mov -vf "movie=my_logo.png[wm];[in][wm] overlay=0:0 [out]" -vcodec libx264 -vb 512k -t 30 E:\out.mp4
@pause

ffmpeg -ss 00:00:05 -i D:\Archive\media\ironman3-clp_h720p.mov -s 320x240 -t 3 -f gif E:\iron.gif
@pause

ffmpeg -s 320x240 -vcodec rawvideo -f rawvideo -pix_fmt nv21 -i D:\Archive\media\test\in320.nv21 -f rawvideo -pix_fmt yuyv422 E:\in320.yuy2
@pause

ffmpeg -ss 00:00:05 -i D:\Archive\media\ironman3-clp_h720p.mov -f rawvideo -pix_fmt nv21 -s 1280x720 -r 15 -t 30 E:\in1280.nv21
@pause

ffmpeg -re -i E:\Archive\Media\720p\A.Bite.Of.China.ep07.2012.BluRay.720p.x264.AAC-LZHD.mp4 -vcodec libx264 -vb 8000000 -preset ultrafast -tune zerolatency -x264opts ratetol=0.1:vbv-maxrate=8000:vbv-bufsize=4000:no-cabac=0:nal-hrd=cbr:me=umh -g 12 -s 1280x720 -aspect 16:9 -r 25 -acodec libvo_aacenc -ab 96000 -ar 48000 -ac 2 -f rtsp -muxdelay 0.1 rtsp://192.168.81.163:2003 
rem udp://127.0.0.1:2003
@pause

ffmpeg -s 640x480 -vcodec rawvideo -f rawvideo -pix_fmt yuv420p -i E:\Archive\Media\test\video\in640.yuv -f rawvideo -pix_fmt rgb32 E:\in640.rgb
@pause

ffmpeg -re -i E:\Archive\Media\720p\A.Bite.Of.China.ep07.2012.BluRay.720p.x264.AAC-LZHD.mp4 -vcodec libx264 -vb 8000000 -preset ultrafast -tune zerolatency -x264opts ratetol=0.1:vbv-maxrate=8000:vbv-bufsize=4000:no-cabac=0:nal-hrd=cbr:me=umh -g 12 -s 1280x720 -aspect 16:9 -r 25 -acodec libvo_aacenc -ab 96000 -ar 48000 -ac 2 -f mpegts -packetsize 1316 udp://192.168.104.201:2003 
rem udp://127.0.0.1:2003
@pause

ffmpeg -s 640x480 -vcodec rawvideo -f rawvideo -pix_fmt nv21 -i E:\Archive\Media\test\video\in640.nv21 -f rawvideo -pix_fmt yuv420p E:\in640.yuv
@pause

ffmpeg -i E:\Archive\Media\720p\A.Bite.Of.China.ep07.2012.BluRay.720p.x264.AAC-LZHD.mp4 -vcodec libx264 -preset medium -b:v 555k -pass 1 -an -t 30 -f mp4 NUL && ffmpeg -i E:\Archive\Media\720p\A.Bite.Of.China.ep07.2012.BluRay.720p.x264.AAC-LZHD.mp4 -c:v libx264 -preset medium -b:v 555k -pass 2 -an -t 30 e:\out.mp4
@pause

ffmpeg -ss 00:10:30 -i E:\Archive\Media\720p\A.Bite.Of.China.ep07.2012.BluRay.720p.x264.AAC-LZHD.mp4 -vcodec libx264 -b:v 512k -s 640x480 -r 29.97 -an -t 5 E:\out.mp4
@pause

ffmpeg -i E:\Archive\Media\sd\5min\jiangsu_1.ts -t 30 -vn -ar 24000 -ac 1 -f s16le -acodec pcm_s16le e:\in.pcm 
@pause

@pause

ffmpeg -i E:\out.mp4 -f rawvideo -pixel_format nv12 -video_size 640x480 E:\out.nv12

@pause

-vbsf h264_mp4toannexb

-sample_fmt s16
name   depth
u8        8
s16      16
s32      32
flt      32
dbl      64
u8p       8
s16p     16
s32p     32
fltp     32
dblp     64

ffmpeg -ss 00:03:30 -i E:\Work\SDK\SentiSight\Resource\Rec_clip\gehua\anhui.ts -t 30 e:\2.mp4
@pause

ffmpeg -ss 00:05:00 -i E:\resource\Rec_clip\gehua\anhui.ts -vcodec copy -acodec copy -t 180 e:\anhui_1.ts
@pause
ffmpeg -i E:\share\sample\111.mp3 -vn -ar 48000 -ac 2 d:\8.wav

@-s 320x240 -r 15 -ab 56 -ar 22050 -b 500 an -s 320*240 -ss 3 -t 10 -vb 1000000 -aspect 1.3333
@ffmpeg -i E:\Rec_clip\cctv3\Part38.ts e:\dd\image%%d.bmp
@ffmpeg -ss START -t DURATION -i INPUT -vcodec copy -acodec copy OUTPUT
@ffmpeg -ss 00:00:20 -t 00:00:10 -i D:/MyVideo.mpg -vcodec copy -acopy copy D:/Split.mpg
@ffmpeg -i INPUT -sameq -intra OUTPUT
@-sameq 表示保持同样的视频质量；
@-intra， 帧内编码
@ffmpeg -i D:/MyVideo.mpg -sameq -intra D:/temp.mpg
@ffmpeg -ss 00:00:30 -vsync 0 -t 00:00:30 -i D:/temp.mpg -vcodec libx264-acodec libfaac D:/result.mpg
@ffmpeg -i INPUT -f mpeg  OUTPUT
@转换格式
@ffmpeg -i D:/temp1.avi -f mpeg  D:/result1.mpg
@copy -b INPUT+INPUT OUTPUT
@copy /b "D:/result1.mpg"+"D:/result1.mpg" "D:/result.mpge" /y
@ffmpeg -i INPUT -f FORMAT OUTPUT
@ffmpeg -i "D:/result.mpge" -f mp4 "D:/result.mp4"
@ffmpeg.exe -i "1.flv" -f mpeg -sameq -y -r 29.97 "1.flv.actemp"
@ffmpeg -i 2.avi -i 3.mp3 -vcodec copy -acodec copy 0.avi  

copy /b "e:\1\1.ts"+"e:\1\2.ts"+"e:\1\3.ts"+"e:\1\4.ts"+"e:\1\5.ts"+"e:\1\6.ts"+"e:\1\7.ts"+"e:\1\8.ts"+"e:\1\9.ts"+"e:\1\10.ts"+"e:\1\11.ts"+"e:\1\12.ts"+"e:\1\13.ts"+"e:\1\14.ts"+"e:\1\15.ts" "e:\2.ts" /y
ffmpeg -i e:\2.ts -vcodec copy e:\out.mov
@pause

libvo_aacenc
libfdk_aac

ffmpeg -i test.mpg -vcodec libx264 -s 1024x768 -b:v 700k -r 25 -deinterlace -acodec libmp3lame -ar 22050 -f flv -y test.flv

fmpeg -i test.mpg -vcodec libx264  -s 1280x768 -b:v 700k -r 25 -vf yadif -acodec libmp3lame -ar 22050 -f flv -y test.flv

@split video(no dec/enc) ffmpeg -ss 00:05:00 -i E:\resource\Rec_clip\gehua\chongqing.ts -vcodec copy -acodec copy -t 300 e:\chongqing_1.ts