package com.example.wilson.mymediacodecfpvplayer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaCodec;
import android.media.MediaCodec.CodecException;
import android.media.MediaFormat;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

/*
receives raw h.264 byte stream on udp port 5000,parses the data into NALU units,and passes them into a MediaCodec Instance.
Original: https://bitbucket.org/befi/h264viewer
Edited by Constantin Geier
 */
public class UdpReceiverDecoderThread {
    public volatile boolean next_frame=false;
    SharedPreferences settings;
    private Surface surface;
    private GroundRecorder mGroundRecorder;
    private boolean decoderConfigured=false;
    private boolean groundRecord=false;
    private boolean DecoderMultiThread=true;
    private boolean userDebug=false;
    DatagramSocket s = null;
    int port;
    int nalu_search_state = 0;
    byte[] nalu_data;
    int nalu_data_position;
    int NALU_MAXLEN = 1024 * 1024;
    Context mContext;
    int readBufferSize=1024*1024*60;
    byte buffer2[] = new byte[readBufferSize];
    private volatile boolean running = true;
    private int zaehlerFramerate=0;
    private long timeB = 0;
    private long timeB2=0;
    private long fpsSum=0,fpsCount=0,averageDecoderfps=0;
    private int current_fps=0;
    private long presentationTimeMs=0;
    private long averageHWDecoderLatency=0;
    private long HWDecoderlatencySum=0;
    private int outputCount=0;
    private long OpenGLFpsSum=-1;
    private int OpenGLFpsCount=0;
    //time we have to wait for an Buffer to fill
    private long averageWaitForInputBufferLatency=0;
    private long waitForInputBufferLatencySum=0;
    private long naluCount=0;

    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo info;
    private MediaCodec decoder;
    private MediaFormat format;


    public UdpReceiverDecoderThread(Surface surface1, int port, Context context) {
        surface=surface1;
        mContext = context;
        this.port = port;
        nalu_data = new byte[NALU_MAXLEN];
        nalu_data_position = 0;
        settings= PreferenceManager.getDefaultSharedPreferences(mContext);
        DecoderMultiThread=settings.getBoolean("decoderMultiThread", true);
        userDebug=settings.getBoolean("userDebug", false);
        groundRecord=settings.getBoolean("groundRecording", false);
        if(userDebug){
            SharedPreferences.Editor editor=settings.edit();
            if(settings.getString("debugFile","").length()>=5000){
                editor.putString("debugFile","new Session !\n");
            }else{
                editor.putString("debugFile",settings.getString("debugFile","")+"\n\nnew Session !\n");
            }
            editor.commit();
        }
        info = new MediaCodec.BufferInfo();
    }
    private void configureDecoder(ByteBuffer csd0,ByteBuffer csd1){
        try {
            if(settings.getString("decoder","HW").equals("SW")){
                //This Decoder Seems to exist on most android devices,but is pretty slow
                decoder=MediaCodec.createByCodecName("OMX.google.h264.decoder");
            }else{
                decoder = MediaCodec.createDecoderByType("video/avc");
            }
        } catch (Exception e) {
            System.out.println("Error creating decoder");
            handleDecoderException(e, "create decoder");
            running=false;
            return;
        }
        System.out.println("Codec Info: " + decoder.getCodecInfo().getName());
        if(userDebug){ makeToast("Selected decoder: " + decoder.getCodecInfo().getName());}
        format = MediaFormat.createVideoFormat("video/avc", 1920, 1080);
        format.setByteBuffer("csd-0",csd0);
        format.setByteBuffer("csd-1", csd1);
        try {
            //This configuration will be overwritten anyway when we put an sps into the buffer
            //But: My decoder agrees with this,but some may not; to be improved
            decoder.configure(format, surface, null, 0);
            if (decoder == null) {
                System.out.println("Can't configure decoder!");
                if(userDebug){makeToast("Can't configure decoder!");}
                running=false;
                return;
            }
        } catch (Exception e) {
            System.out.println("error config decoder");
            handleDecoderException(e,"configure decoder");
        }
        decoder.start();
        decoderConfigured=true;
        if(DecoderMultiThread){
            Thread thread2=new Thread(){
                @Override
                public void run() {while(running){checkOutput();}}
            };
            thread2.setPriority(Thread.MAX_PRIORITY);
            thread2.start();
        }
    }

    public void startDecoding(){
        running=true;
        Thread thread=new Thread(){
            @Override
            public void run() {
                startFunction();
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }
    public void stopDecoding(){
        running=false;
        writeLatencyFile();
    }

    public void startFunction() {
        mGroundRecorder=new GroundRecorder(settings.getString("fileName","Ground"));
        if(settings.getString("dataSource","UDP").equals("FILE")){
            receiveFromFile(settings.getString("fileNameVideoSource","rpi960mal810.h264"));
        }else{receiveFromUDP();}
    }

    private void receiveFromUDP() {
        int server_port = this.port;
        byte[] message = new byte[1024];
        DatagramPacket p = new DatagramPacket(message, message.length);
        try {
            s = new DatagramSocket(server_port);
            s.setSoTimeout(500);
        } catch (SocketException e) {e.printStackTrace();}
        boolean exception=false;
        while (running && s != null) {
            try {
                s.receive(p);
            } catch (IOException e) {
                if(! (e instanceof SocketTimeoutException)){
                    e.printStackTrace();
                }
                exception=true;
            }
            if(!exception){
                parseDatagram(message, p.getLength());
                if(groundRecord){mGroundRecorder.writeGroundRecording(message, p.getLength());}
            }else{exception=false;} //The timeout happened
        }
        if (s != null) {
            s.close();
        }
        if(groundRecord){mGroundRecorder.stop();}
        if(decoder!=null){
            decoder.flush();
            decoder.stop();
            decoder.release();
        }
    }

    private void receiveFromFile(String fileName) {
        java.io.FileInputStream in;
        try {
            in=new java.io.FileInputStream(Environment.getExternalStorageDirectory()+"/"+fileName);
        } catch (FileNotFoundException e) {
            System.out.println("Error opening File");
            makeToast("Error opening File !");
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
                makeToast("File end of stream");
                running = false;
                break;
            }
        }
        //if(groundRecord){mGroundRecorder.stop();}
        if(decoder != null){
            decoder.flush();
            decoder.stop();
            decoder.release();
        }
    }

    private void parseDatagram(byte[] p, int plen) {
        //Maybe: use System.arraycopy ...
        try {
            for (int i = 0; i < plen; ++i) {
                nalu_data[nalu_data_position++] = p[i];
                if (nalu_data_position == NALU_MAXLEN - 1) {
                    Log.w("parseDatagram", "NALU Overflow");
                    if(userDebug){makeToast("NALU Overflow");}
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
            System.out.println("error parsing");
            handleDecoderException(e,"parseDatagram");
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
 /*TESTING XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
        while(!next_frame){
            try {Thread.sleep(5,0);} catch (InterruptedException e) {e.printStackTrace();}
        }
        next_frame=false;
 //TESTING XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX*/
        if(decoderConfigured==true){
            timeB=System.currentTimeMillis();
            feedDecoder(n, len); //takes beteen 2 and 20ms (1ms,1ms,20ms,1ms,1ms,20ms,... in this order),
            // beacause there isn't always an input buffer available immediately;
            //may be improved (multithreading)

        }else{
            configureDecoder(MediaCodecFormatHelper.getCsd0(), MediaCodecFormatHelper.getCsd1());
        }
        long time=System.currentTimeMillis()-timeB;
        if(time>=0 && time<=200){
            naluCount++;
            waitForInputBufferLatencySum+=time;
            averageWaitForInputBufferLatency=(waitForInputBufferLatencySum/naluCount);
            //Log.w("1","Time spent waiting for an input buffer:"+time);
            //Log.w("2","average Time spent waiting for an input buffer:"+averageWaitForInputBufferLatency);
        }
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
                    presentationTimeMs=System.nanoTime();
                    decoder.queueInputBuffer(inputBufferIndex, 0, len,presentationTimeMs,0);
                    break;
                }else if(inputBufferIndex!=MediaCodec.INFO_TRY_AGAIN_LATER){
                    if(userDebug){
                        makeToast("queueInputBuffer unusual: "+inputBufferIndex);
                        makeDebugFile("queueInputBuffer unusual: "+inputBufferIndex);
                    }
                }
                if(!DecoderMultiThread){
                    checkOutput();
                }
            } catch (Exception e) {
                handleDecoderException(e,"feedDecoder");
            }
        }


    }


    private void checkOutput() {
        try {
            //outputBuffers = decoder.getOutputBuffers();
            int outputBufferIndex = decoder.dequeueOutputBuffer(info, 0);
            if (outputBufferIndex >= 0) {
                //
                zaehlerFramerate++;
                if((System.currentTimeMillis()-timeB2)>1000) {
                    int fps = (zaehlerFramerate );
                    current_fps=(int)fps;
                    timeB2 = System.currentTimeMillis();
                    zaehlerFramerate = 0;
                    //Log.w("ReceiverDecoderThread", "fps:" + fps);
                    fpsSum+=fps;
                    fpsCount++;
                    if(fpsCount==1){
                        makeToast("First video frame has been decoded");
                    }
                }
                long latency=((System.nanoTime()-info.presentationTimeUs)/1000000);
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

            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED || outputBufferIndex==MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                Log.d("UDP", "output format / buffers changed");
            } else if(outputBufferIndex!=MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.d("dequeueOutputBuffer", "not normal");
                if(userDebug){
                    makeToast("dequeueOutputBuffer;" + "not normal;" + "number:"+outputBufferIndex);
                    makeDebugFile("dequeueOutputBuffer;" + "not normal;" + "number:" + outputBufferIndex);
                }

            }
        }catch(Exception e) {
            handleDecoderException(e,"checkOutput");
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
        if(fpsCount==0){fpsCount=1;}
        if(OpenGLFpsCount==0){OpenGLFpsCount=1;}
        averageDecoderfps=fpsSum/fpsCount;
        lf+="\n Average HW Decoder fps: "+(averageDecoderfps);
        if(OpenGLFpsSum>=0){lf+="\n OpenGL as Output;Average OpenGL FPS: "+(OpenGLFpsSum/(long)OpenGLFpsCount);}
        lf+="\n Average measured app Latency: "+(averageWaitForInputBufferLatency+averageHWDecoderLatency);
        lf+="\n Average time waiting for an input Buffer:"+averageWaitForInputBufferLatency;
        lf+="\n Average time HW decoding:"+averageHWDecoderLatency;
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
    public int getDecoderFps(){
        return current_fps;
    }
    private void makeToast(final String message) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void makeDebugFile(String message){
        SharedPreferences.Editor editor=settings.edit();
        editor.putString("debugFile",message+settings.getString("debugFile",""));
        editor.commit();
    }
    private void handleDecoderException(Exception e,String tag){
        if(userDebug) {
            makeToast("Exception on "+tag+": ->exception file");
            if (e instanceof CodecException) {
                CodecException codecExc = (CodecException) e;
                makeDebugFile("CodecException on " + tag + " :" + codecExc.getDiagnosticInfo());
            } else {
                makeDebugFile("Exception on "+tag+":"+Log.getStackTraceString(e));
            }
            try {Thread.sleep(100,0);} catch (InterruptedException e2) {e2.printStackTrace();}
        }
        e.printStackTrace();
    }

    private class GroundRecorder{
        private java.io.FileOutputStream out;
        public GroundRecorder(String s){
            out = null;
            try {
                out=new java.io.FileOutputStream(Environment.getExternalStorageDirectory()+"/"+s,false);
            } catch (FileNotFoundException e) {e.printStackTrace();Log.w("GroundRecorder", "couldn't create");}
        }
        public void writeGroundRecording(byte[] p,int len){
            try {
                out.write(p,0,len);
            } catch (IOException e) {e.printStackTrace();Log.w("GroundRecorder", "couldn't write");}
        }
        public void stop(){
            try {
                out.close();
            } catch (IOException e) {e.printStackTrace();Log.w("GroundRecorder", "couldn't close");}
        }
    }
}
