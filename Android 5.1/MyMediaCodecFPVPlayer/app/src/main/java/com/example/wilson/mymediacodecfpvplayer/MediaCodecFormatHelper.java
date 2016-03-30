package com.example.wilson.mymediacodecfpvplayer;


import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.Format;

public class MediaCodecFormatHelper {

    public MediaCodecFormatHelper(){

    }

    public static ByteBuffer getCsd0(){
        byte[] csd0={0,0,0,1,39,66,-128,40,-107,-96,60,6,127,-110,1,-30,68,-44};
        ByteBuffer BBcsd0=ByteBuffer.wrap(csd0);
        return BBcsd0;
    }


    public static ByteBuffer getCsd1(){
        byte[] csd1={0,0,0,1,40,-50,2,92,-128};
        ByteBuffer BBcsd1=ByteBuffer.wrap(csd1);
        return BBcsd1;
    }


    /*TESTINGS SPECIFIC FOR RASPBERRY PI CAMERA OUTPUT
        __________________________________________________________________________;
        //This is from moonlight;but for rpi it doesn't work
        if (n[4] == 0x67){Log.w("interpretNalu","SPS" );} //SPS 0x67=103
        if (n[4] == 0x68){Log.w("interpretNalu","PPS" );} //PPS      //0x68=104
        if (n[4] == 0x40){Log.w("interpretNalu","VPS" );} //VPS        //0x40=64
        if (n[4] == 0x42){Log.w("interpretNalu","SPS" );} //SPS too    //0x42=66
        if (n[4] == 0x44){Log.w("interpretNalu","PPS" );} //PPS too  //0x44=68
        __________________________________________________________________________;

        n[4]==constant 33  ,ausser ersten paar (0,39,40,37,,33...)
        n[5]== constant -102 ausser ersten paar  (0,66,-50,-120,...)
        n[6] steigt andauernd um 2 an ?!!
        n[7]= regelmäßigkeiten erkennbar,aber nicht wirklich regelmäßig

        it seems like when rpi
        1)puts 39,40 or 37 in the n[4] byte, if it's configuration data or
        2)puts       33 in the n[4] byte if it's a non I -frame
        by using -ih in the raspivid pipeline the rpi puts this config data not only in the beginning,but in intervalls between the
        non-i-(==33)frames (which makes sence,that's exactely the purpose of -ih )

        String x="";
        for(int i=0;i<60;i++){
            x+=n[i]+",";
        }
        Log.w("interpretNAlU: ", x);

        /*if(n[4]==33 || n[4]==39 || n[4]==40 || n[4]==37){ feedDecoder(n,len);}
        what happens,when packets get filtered out
        1)no n[4]==33 packets: only about 4 frames (15 sec video ) get displayed (häh ?? )
        I think: eather n[4]==39,40,oder 37 are key frames
        2)no n[4]==39 packets: nothing gets decoded
        3) no n[4]==40 packets: nothing gets displayed
        4) no n[4]==37 packets: stream works,but has some artefacts;

        _______________________________________________________________________
        Conclusion; I can definitely say: (for rpi)
        1)when n[4]==39 || n[4]==40 it is decoder configuration data (SPS or PPS)
        2)when n[4]==33 it is not configuration data, probably a b-frame
        3)when n[4]==37 it is an key frame
        but it's weird that i can decode the stream even without key-frames with just some Artefakts
        4)if((n[4] & 0x1f)==0x07){ it is definitely an sps frame (Info from stackoverflow) ;should be true general,too
        (this is true for n[4]==39) }
        _________________________________________________________________________
        */

}
