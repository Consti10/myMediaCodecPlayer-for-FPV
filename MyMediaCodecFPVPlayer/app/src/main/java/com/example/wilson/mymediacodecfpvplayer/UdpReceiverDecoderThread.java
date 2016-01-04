package com.example.wilson.mymediacodecfpvplayer;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;

/*
receives raw h.264 byte stream on udp port 5000,parses the data into NALU units,and passes them into a MediaCodec Instance.
Original: https://bitbucket.org/befi/h264viewer
Edited by Constantin Geier at 28.12.2015;
For Testing, replace "receiveFromUDP" by "receiveFromFile" , and add the path to your file. (Has to be a raw h.264 file)
 */
public class UdpReceiverDecoderThread extends Thread {
    SharedPreferences settings;
    private GroundRecorder mGroundRecorder;
    private boolean groundRecord=false;
    int port;
    int nalu_search_state = 0;
    byte[] nalu_data;
    int nalu_data_position;
    int NALU_MAXLEN = 1024 * 1024;
    Context mContext;
    //byte buffer2[] = new byte[18800 * 8 * 8 * 8];
    byte buffer2[] = new byte[1024];

    boolean running = true;

    long timeB = 0, timeA = 0;
    long presentationTimeMs=0;
    long averageHWDecoderLatency=0;
    long HWDecoderlatencySum=0;
    int outputCount=0;
    //time we have to wait for an Buffer to fill
    long averageWaitForInputBufferLatency=0;
    long waitForInputBufferLatencySum=0;
    long naluCount=0;
    String s = "Time between output buffers: ";

    ByteBuffer[] inputBuffers;
    ByteBuffer[] outputBuffers;
    MediaCodec.BufferInfo info;

    private MediaCodec decoder;
    private MediaFormat format;


    public UdpReceiverDecoderThread(Surface surface, int port, Context context) {
        mContext = context;
        this.port = port;
        nalu_data = new byte[NALU_MAXLEN];
        nalu_data_position = 0;
        try {
            decoder = MediaCodec.createDecoderByType("video/avc");
            //This Decoder Seems to exist on most android devices,but is pretty slow
            //decoder=MediaCodec.createByCodecName("OMX.google.h264.decoder");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error creating decoder");
            return;
        }

        System.out.println("Codec Info: "+decoder.getCodecInfo().getName());

        format = MediaFormat.createVideoFormat("video/avc", 1280, 720);

        try {
            decoder.configure(format, surface, null, 0);
            if (decoder == null) {
                System.out.println("Can't configure decoder!");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("error config decoder");
        }
        //decoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        info = new MediaCodec.BufferInfo();

        decoder.start();
    }
    @Override
    public void interrupt(){
        running=false;
        writeLatencyFile();
    }

    public void run() {
        setPriority(Thread.MAX_PRIORITY);
        settings= PreferenceManager.getDefaultSharedPreferences(mContext);
        mGroundRecorder=new GroundRecorder(settings.getString("fileName","Ground"));
        groundRecord=settings.getBoolean("groundRecording", false);

        new Thread(new Runnable() {
            public void run() {
                setPriority(Thread.MAX_PRIORITY);
                System.out.println("Thread Priority: " + getPriority()); //Thread priority:5
                while (running) {
                    checkOutput();
                }
            }
        }).start();

        if(settings.getString("dataSource","UDP").equals("FILE")){
            receiveFromFile(settings.getString("fileNameVideoSource","rpi960mal810.h264"));
        }else{receiveFromUDP();}
    }

    private void feedDecoder(byte[] n, int len) {
        for (; ; ) {
            try {
                inputBuffers = decoder.getInputBuffers();
                int inputBufferIndex = decoder.dequeueInputBuffer(0);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.put(n, 0, len);
                    //decoder.queueInputBuffer(inputBufferIndex, 0, len, 0, 0);
                    presentationTimeMs=System.currentTimeMillis();
                    decoder.queueInputBuffer(inputBufferIndex, 0, len,presentationTimeMs,0);
                    break;
                }
            } catch (Exception e) {
                //e.printStackTrace();
                System.out.println("Error Qeueing in/out Buffers");
            }
            /*
            int count1=0;
            try{for(int i=0;i<100;i++){ByteBuffer xyz=inputBuffers[i];count1=i;}}catch(Exception e){e.printStackTrace();}
            System.out.println("number of inputBuffers:"+count1+"");
            try{for(int i=0;i<100;i++){ByteBuffer xyz=outputBuffers[i];count1=i;}}catch(Exception e){e.printStackTrace();}
            System.out.println("number of outputBuffers:"+count1+"");
            */
        }


    }

    private void interpretNalu(byte[] n, int len) {
        //Here is the right place to do some changes to the data (f.e sps fix up )
        timeB=System.currentTimeMillis();
        feedDecoder(n, len); //takes beteen 2 and 40ms (1ms,1ms,46ms,1ms,1ms,40ms,... in this order),
        // beacause there isn't always an input buffer available immediately;
        long time=System.currentTimeMillis()-timeB;
        if(time>=0 && time<=200){
            naluCount++;
            waitForInputBufferLatencySum+=time;
            averageWaitForInputBufferLatency=(waitForInputBufferLatencySum/naluCount);
            //Log.w("1","Time spent waiting for an input buffer:"+time);
            //Log.w("2","average Time spent waiting for an input buffer:"+averageWaitForInputBufferLatency);
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
        //
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
        I can definitely say: (for rpi)
        1)when n[4]==39 || n[4]==40 it is decoder configuration data (SPS or PPS)
        2)when n[4]==33 it is not configuration data, probably a b-frame
        3)when n[4]==37 it is an key frame
        but it's weird that i can decode the stream even without key-frames with just some Artefakts

        if((n[4] & 0x1f)==0x07){ it is definitely an sps frame (Info from stackoverflow) ;should be true general,too
        this is true for n[4]==39 }
        _________________________________________________________________________
        */


    }

    private void parseDatagram(byte[] p, int plen) {
        //Maybe: use System.arraycopy ...
        try {
            for (int i = 0; i < plen; ++i) {
                nalu_data[nalu_data_position++] = p[i];
                if (nalu_data_position == NALU_MAXLEN - 1) {
                    Log.w("parseDatagram","NALU Overflow");
                    nalu_data_position = 0;
                }
                switch (nalu_search_state) {
                    case 0:
                    case 1:
                    case 2:
                        if (p[i] == 0)
                            nalu_search_state++;
                        else
                            nalu_search_state = 0;
                        break;
                    case 3:
                        if (p[i] == 1) {
                            //nalupacket found
                            nalu_data[0] = 0;
                            nalu_data[1] = 0;
                            nalu_data[2] = 0;
                            nalu_data[3] = 1;
                            interpretNalu(nalu_data, nalu_data_position - 4);
                            nalu_data_position = 4;
                        }
                        nalu_search_state = 0;
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("error parsing");
        }
    }

    private void checkOutput() {
        //try {Thread.sleep(200,0);} catch (InterruptedException e) {e.printStackTrace();}
        try {
            //outputBuffers = decoder.getOutputBuffers();
            int outputBufferIndex = decoder.dequeueOutputBuffer(info, 0);
            if (outputBufferIndex >= 0) {
                long latency=System.currentTimeMillis()-info.presentationTimeUs;
                if(latency>=0 && latency<=200){
                    outputCount++;
                    HWDecoderlatencySum+=latency;
                    averageHWDecoderLatency=HWDecoderlatencySum/outputCount;
                    //Log.w("checkOutput 2","hw decoder latency:"+latency);
                    //Log.w("checkOutput 1","Average HW decoder latency:"+averageHWDecoderLatency);
                }
                decoder.releaseOutputBuffer(outputBufferIndex, true);
                //decoder.releaseOutputBuffer(outputBufferIndex,(long) 0); //needs api 21

            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d("UDP", "output format changed");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void receiveFromUDP() {
        int server_port = this.port;
        byte[] message = new byte[1024];
        DatagramPacket p = new DatagramPacket(message, message.length);
        DatagramSocket s = null;
        try {
            s = new DatagramSocket(server_port);
        } catch (SocketException e) {
            System.out.println("error opening port");
        }

        while (!Thread.interrupted() && s != null) {
            try {
                s.receive(p);
            } catch (IOException e) {}
            parseDatagram(message, p.getLength());
            if(groundRecord){mGroundRecorder.writeGroundRecording(message, p.getLength());}
        }
        if (s != null) {
            s.close();
        }
        if(groundRecord){mGroundRecorder.stop();}
        decoder.flush();
        decoder.stop();
        decoder.release();
    }

    private void receiveFromFile(String fileName) {
        java.io.FileInputStream in = null;
        try {
            //in = new java.io.FileInputStream(Environment.getExternalStorageDirectory() + "/rpi.h264");
            //in = new java.io.FileInputStream(Environment.getExternalStorageDirectory() + "/rpi30fps1280mal720.h264");
            //in = new java.io.FileInputStream(Environment.getExternalStorageDirectory() + "/rpi960mal810.h264");
            in=new java.io.FileInputStream(Environment.getExternalStorageDirectory()+"/"+fileName);
        } catch (FileNotFoundException e) {
            System.out.println("Error opening File");
            return;
        }
        for (; ; ) {
            if(Thread.interrupted()){running=false;break;}
            int sampleSize = 0;
            try {
                //sampleSize = in.read(buffer2, 0, 18800 * 8 * 8 * 8);
                sampleSize=in.read(buffer2,0,1024);
            } catch (IOException e) {
            }
            if (sampleSize < 0) {
                Log.d("File", "End of stream");
                System.out.println(s);
                running = false;
                break;
            } else {
                parseDatagram(buffer2, sampleSize);
                //if(groundRecord){mGroundRecorder.writeGroundRecording(buffer2);}
            }
        }
        //if(groundRecord){mGroundRecorder.stop();}
        decoder.flush();
        decoder.stop();
        decoder.release();
    }

    public void writeLatencyFile(){
        java.io.PrintWriter out;
        String lf="everything in ms:";
        lf+="\n Average measured app Latency: "+(averageWaitForInputBufferLatency+averageHWDecoderLatency);
        lf+="\n Average time waiting for an input Buffer:"+averageWaitForInputBufferLatency;
        lf+="\n Average time HW encoding:"+averageHWDecoderLatency;
        lf+="\n .";
        //Todo: measure time between realeasing output buffer and rendering it onto Screen
        try {
            out=new java.io.PrintWriter(Environment.getExternalStorageDirectory()+"/latencyFile.txt");
            out.println(lf);
            out.close();
        } catch (Exception e) {e.printStackTrace();}
    }

    private class GroundRecorder{
        private java.io.FileOutputStream out;
        public GroundRecorder(String s){
            out = null;
            try {
                out=new java.io.FileOutputStream(Environment.getExternalStorageDirectory()+"/"+s);
            } catch (FileNotFoundException e) {e.printStackTrace();Log.w("GroundRecorder", "couldn't create");}
        }
        public void writeGroundRecording(byte[] p,int len){
            try {
                out.write(p,0,len);
            } catch (IOException e) {e.printStackTrace();Log.w("GroundRecorder", "couldn't write");}
        }
        public void stop(){
            try {out.close();} catch (IOException e) {e.printStackTrace();Log.w("GroundRecorder", "couldn't close");}
        }
    }
}
