@echo off

set input_clip=F:\bt\sa\ep06.mp4
set output_clip=F:\bt\[mz]dump.avi

set iw=1280
set ih=720
set ow=176
set oh=128

set /A oh_r=%ow%*%ih%/%iw%
set /A a=176
set /A b=128
set /A c=0
set /A d=(%oh%-%oh_r%)/2

echo %oh% %oh_r% %a% %b% %c% %d%

ffmpeg -ss 00:00:55 -i %input_clip% -t 0:00:03 -vb 384k -bf 0 -r 20  -vf "scale=%ow%:-1,pad=%ow%:%oh%:0:%d%:black" -vcodec libxvid -ar 44100 -ab 128k -acodec mp3 %output_clip%

@pause
-vf "scale=176:128,pad=176:128:0:0:black" 
-s 176x128
-ss 00:00:05
-t 00:00:03 

scale=176:-1,pad=176:128:0:0:black

ffplay f:\out.rgb -pix_fmt rgb565le -s 512x512
ffmpeg -i F:\1.bmp -s 512x512 -pix_fmt rgb565 f:\out.rgb

ffmpeg -f concat -i files2.txt -c copy out.mp4

How to concatenate (join, merge) media files