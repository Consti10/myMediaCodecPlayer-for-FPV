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
import android.view.Display;
import android.view.Surface;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;

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
    int readBufferSize=1024*1024*60;
    byte buffer2[] = new byte[readBufferSize];

    private volatile boolean running = true;

    int zaehlerFramerate=0;
    long timeB = 0;
    long timeB2=0;
    long fpsSum=0,fpsCount=0,averageDecoderfps=0;
    long presentationTimeMs=0;
    long averageHWDecoderLatency=0;
    long HWDecoderlatencySum=0;
    int outputCount=0;
    private long OpenGLFpsSum=-1;
    private int OpenGLFpsCount=0;
    //time we have to wait for an Buffer to fill
    long averageWaitForInputBufferLatency=0;
    long waitForInputBufferLatencySum=0;
    long naluCount=0;

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
        format.setInteger(MediaFormat.KEY_FRAME_RATE,90);

        try {
            //This configuration will be overwritten anyway when we put an sps into the buffer
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
    private void receiveFromUDP() {
        int server_port = this.port;
        byte[] message = new byte[1024];
        DatagramPacket p = new DatagramPacket(message, message.length);
        DatagramSocket s = null;
        try {
            s = new DatagramSocket(server_port);
            s.setSoTimeout(1000);
        } catch (SocketException e) {e.printStackTrace();}
        while (running && s != null) {
            try {
                s.receive(p);
            } catch (IOException e) {}
            if(p.getLength()>0){
                parseDatagram(message, p.getLength());
                if(groundRecord){mGroundRecorder.writeGroundRecording(message, p.getLength());}
            } //else: the timeout happend
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
        java.io.FileInputStream in;
        try {
            in=new java.io.FileInputStream(Environment.getExternalStorageDirectory()+"/"+fileName);
        } catch (FileNotFoundException e) {
            System.out.println("Error opening File");
            return;
        }
        for (; ; ) {
            if(!running){break;}
            int sampleSize = 0;
            try {
                sampleSize=in.read(buffer2,0,readBufferSize);
            } catch (IOException e) {e.printStackTrace();}
            if(sampleSize>0){
                parseDatagram(buffer2, sampleSize);
                //if(groundRecord){mGroundRecorder.writeGroundRecording(buffer2);}
            }else {
                Log.d("File", "End of stream");
                running = false;
                break;
            }
        }
        //if(groundRecord){mGroundRecorder.stop();}
        decoder.flush();
        decoder.stop();
        decoder.release();
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

    private void interpretNalu(byte[] n, int len) {
        //Here is the right place to do some changes to the data (f.e sps fix up )
        //some example code:
        /*if(n[4]==39){
            ByteBuffer spsBuf = ByteBuffer.wrap(n, 0, len);
            // Skip to the start of the NALU data
            spsBuf.position(5);
            // The H264Utils.readSPS function safely handles
            // Annex B NALUs (including NALUs with escape sequences)
            SeqParameterSet sps = H264Utils.readSPS(spsBuf);
            //System.out.println("sps profile idc:"+sps.profile_idc);
            //change constants
            //sps.level_idc=0;
            //done with configuration
            spsBuf.position(5);
            sps.write(spsBuf);
            spsBuf.position(0);
            spsBuf.get(n,0,len);
        }
        //---------------------------------------------------------*/
        timeB=System.currentTimeMillis();
        feedDecoder(n, len); //takes beteen 2 and 40ms (1ms,1ms,46ms,1ms,1ms,40ms,... in this order),
                             // beacause there isn't always an input buffer available immediately;
                             //may be improved (multithreading)
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
    @SuppressWarnings("deprecation")
    private void feedDecoder(byte[] n, int len) {
        //
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
                e.printStackTrace();
                //System.out.println("Error Qeueing in/out Buffers");
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


    private void checkOutput() {
        //try {Thread.sleep(200,0);} catch (InterruptedException e) {e.printStackTrace();}
        try {
            //outputBuffers = decoder.getOutputBuffers();
            int outputBufferIndex = decoder.dequeueOutputBuffer(info, 0);
            if (outputBufferIndex >= 0) {
                //
                zaehlerFramerate++;
                if((System.currentTimeMillis()-timeB2)>1000) {
                    int fps = (zaehlerFramerate );
                    timeB2 = System.currentTimeMillis();
                    zaehlerFramerate = 0;
                    //Log.w("ReceiverDecoderThread", "fps:" + fps);
                    fpsSum+=fps;
                    fpsCount++;
                    averageDecoderfps=(fpsSum/fpsCount);
                }
                //
                long latency=System.currentTimeMillis()-info.presentationTimeUs;
                if(latency>=0 && latency<=400){
                    outputCount++;
                    HWDecoderlatencySum+=latency;
                    averageHWDecoderLatency=HWDecoderlatencySum/outputCount;
                    //Log.w("checkOutput 1","hw decoder latency:"+latency);
                    //Log.w("checkOutput 2","Average HW decoder latency:"+averageHWDecoderLatency);
                }
                //on my device this code snippet from Moonlight is not needed,after testing I doubt if it is really working at all;
                //if(decoder.dequeueOutputBuffer(info, 0) >= 0){ Log.w("...","second available");}

                //for GLSurfaceView,to drop the latest frames except the newest one,the timestamp has to be near the VSYNC signal.
                //requires android 5
                decoder.releaseOutputBuffer(outputBufferIndex,System.nanoTime()); //needs api 21
                //decoder.releaseOutputBuffer(outputBufferIndex,true);

            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d("UDP", "output format changed");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void writeLatencyFile(){
        //Todo: measure time between realeasing output buffer and rendering it onto Screen
        /*
        Display mDisplay=getWindowManager().getDefaultDisplay();
        long PresentationDeadlineMillis=mDisplay.getPresentationDeadlineNanos()/1000000;
        Log.w(TAG,"Display:"+PresentationDeadlineMillis);
         */

        String lf=settings.getString("latencyFile","ERROR");
        if(lf.length()>=1000 || lf.length()<=20){
            lf="These values only show the measured lag of the app; \n"+
            "The overall App latency may be much more higher,because you have to add the 'input lag' of your phone-about 32-48ms on android \n"+
            "Every 'time' values are in ms. \n";
        }
        lf+="\n Average HW Decoder fps: "+(averageDecoderfps);
        if(OpenGLFpsSum>=0){lf+="\n OpenGL as Output;Average OpenGL FPS: "+(OpenGLFpsSum/(long)OpenGLFpsCount);}
        lf+="\n Average measured app Latency: "+(averageWaitForInputBufferLatency+averageHWDecoderLatency);
        lf+="\n Average time waiting for an input Buffer:"+averageWaitForInputBufferLatency;
        lf+="\n Average time HW encoding:"+averageHWDecoderLatency;
        lf+="\n ";
        SharedPreferences.Editor editor=settings.edit();
        editor.putString("latencyFile",lf);
        editor.commit();

    }

    public void tellOpenGLFps(long OGLFps){
        if(OpenGLFpsSum<=0){OpenGLFpsSum=0;OpenGLFpsCount=0;}
        OpenGLFpsCount++;
        OpenGLFpsSum+=OGLFps;
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
