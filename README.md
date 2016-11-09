# myMediaCodecPlayer-for-FPV


ATTENTION: If you want to use this app for fpv, you can find the newest version with OSD support here: https://github.com/Consti10/FPV_VR
If you are not a developer you can dwonload this app from here https://play.google.com/store/apps/details?id=com.constantin.wilson.FPV_VR&hl=en and support developement.


This app receives a raw h.264 byte stream on udp port 5000 ,parses it into NALU units and passes the data into a MediaCodec Instance.
It was mainly developed for HD FPV, where a low latency is essentially.
(Infos can be found on 
http://fpv-community.de/showthread.php?64819-FPV-Wifi-Broadcasting-HD-Video-Thread-zum-Raspberry-HD-Videolink-fon-Befi (German)
And https://befinitiv.wordpress.com/2015/04/18/porting-wifibroadcast-to-android/ )

Other use-cases are: Screencast/Screen Mirroring and GameStreaming 

Example (command-line for linux) to Mirror your pc screen to your Android Device:

1) avconv -f x11grab -r 180 -s 640x480 -i :0+0,20 -vcodec libx264 -preset ultrafast -tune zerolatency -threads 1 -f h264 -bf 0 -refs 120  -maxrate 300 -bufsize 1  -| socat -b 1024 - UDP4-SENDTO:192.168.43.1:5000

Performance: Higly depends on your Hardware chip encoder;
On my device (Huawei ascend p7 ; mali 450mp) the latency is about ~6ms parsing, ~10ms hw decoding and ~40ms additional lag from Android ( refer to "input lag" , can be as high as ~2-3 frames on Androd)
If your device has a  noticable higher lag/the decoder isn't working, your hw encoder may need special h.264 sps/pps parameters ,for a implementation you can look at https://github.com/moonlight-stream/moonlight-pc.


My further developing aims are : an OSD (on-screen-display)






