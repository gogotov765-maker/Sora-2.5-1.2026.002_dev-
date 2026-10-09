#!/bin/bash
# Ручной тест вотермарки: ./watermark.sh video.mp4 wm.mp4 out.mp4
ffmpeg -i "$1" -stream_loop -1 -i "$2" -filter_complex \
"[1:v]crop=1560:390:141:223,format=rgba,chromakey=0x8900CB:0.10:0.06,colorchannelmixer=aa=0.6,scale=160:-1[wm]; \
[0:v][wm]overlay=\
x='if(lt(mod(t,10),5),15,W-w-15)':\
y='if(lt(mod(t,10),5),15,H-h-15)':shortest=1" \
-c:a copy "$3"
