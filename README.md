# myMediaCodecPlayer-for-FPV
This app receives a raw h.264 byte stream on udp port 5000 ,parses it into NALU units and passes the data into a MediaCodec Instance.
It was mainly developed for HD FPV, where a low latency is essentially.
(Infos can be found on 
http://fpv-community.de/showthread.php?64819-FPV-Wifi-Broadcasting-HD-Video-Thread-zum-Raspberry-HD-Videolink-fon-Befi (German)
And https://befinitiv.wordpress.com/2015/04/18/porting-wifibroadcast-to-android/ )

Other use-cases are: Screencast/Screen Mirroring and GameStreaming 

Example (command-line for linux) to Mirror your pc screen to your Android Device:
1)(Best) avconv -f x11grab -r 120 -s 800x600 --vcodec libx264 -preset ultrafast -tune zerolatency -threads 0 -f h264 -bf 0 -refs 120 -b 2000000 -maxrate 3000 -bufsize 100 -| socat -b 1024 - UDP4-SENDTO:10.24.193.196:5000
2) gst-launch -e ximagesrc ! queue ! videoscale ! video/x-raw-rgb, framerate=25/1, width=640, height=360 ! queue ! ffmpegcolorspace ! x264enc tune=zerolatency profile=baseline byte-stream=1 ! fdsink -| socat -b 1024 - UDP4-SENDTO:10.133.103.202:5000
3)(using Istanbuls istximagesink) gst-launch -e istximagesrc ! ffmpegcolorspace ! x264enc tune=zerolatency profile=baseline byte-stream=1 ! fdsink -| socat -b 1024 - UDP4-SENDTO:10.133.103.202:5000

Performance: Higly depends on your Hardware chip encoder;
On my device (Huawei ascend p7 ; mali 450mp) the latency is about ~6ms parsing, ~10ms hw decoding and ~40ms additional lag from Android ( refer to "input lag" , can be as high as ~2-3 frames on Androd)
If your device has a  noticable higher lag, your hw encoder may need special h.264 sps/pps parameters ,for a implementation you can look at https://github.com/moonlight-stream/moonlight-pc  .

My further developing aims are : an OSD (on-screen-display)






