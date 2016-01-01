package com.example.wilson.mymediacodecfpvplayer;

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
    int outputCounter=0;
    private GroundRecorder mGroundRecorder;
    private boolean groundRecord=false;
    int port;
    int nalu_search_state = 0;
    byte[] nalu_data;
    int nalu_data_position;
    int NALU_MAXLEN = 1024 * 1024;
    Context mContext;
    byte buffer2[] = new byte[18800 * 8 * 8 * 8];

    boolean running = true;

    long timeB = 0, timeA = 0;
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

        decoder.start();
    }
    @Override
    public void interrupt(){
        running=false;
    }
    @Override
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
                    decoder.queueInputBuffer(inputBufferIndex, 0, len, 0, 0);
                    //decoder.queueInputBuffer(inputBufferIndex,0,len,0,MediaCodec.BUFFER_FLAG_KEY_FRAME);
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
        //Here is the right place to do some changes to the data (f.e change sps fixup data
        /*H264 SPS
        if (n[4+3] == 0x67){Log.w("interpretNalu","SPS" );} //SPS
        if (n[4+3] == 0x68){Log.w("interpretNalu","PPS" );} //PPS
        if (n[4+3] == 0x40){Log.w("interpretNalu","VPS" );} //VPS
        if (n[4+3] == 0x42){Log.w("interpretNalu","SPS" );} //SPS too
        if (n[4+3] == 0x44){Log.w("interpretNalu","PPS" );} //PPS too*/

        feedDecoder(n, len); //takes beteen 2 and 40ms (1,1,46,1,1,40,...)
        //try {Thread.sleep(200,0);} catch (InterruptedException e) {e.printStackTrace();}
    }

    private void parseDatagram(byte[] p, int plen) {
        int i;
        try {
            for (i = 0; i < plen; ++i) {
                nalu_data[nalu_data_position++] = p[i];
                if (nalu_data_position == NALU_MAXLEN - 1) {
                    Log.w("parseDatagram","NALU Oveeflow");
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
            info = new MediaCodec.BufferInfo();
            int outputBufferIndex = decoder.dequeueOutputBuffer(info, 0);
            //long x=info.presentationTimeUs;
            //System.out.println("Time: "+x);
            if (outputBufferIndex >= 0) {
                timeA = System.currentTimeMillis();
                //System.out.println("Time between outputsAvailable: "+(timeA-timeB));
                //s += ((timeA - timeB) + ",");
                //timeB = timeA;
                decoder.releaseOutputBuffer(outputBufferIndex, true);
                System.out.println("Output availabele");

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
            } catch (IOException e) {
            }

            parseDatagram(message, p.getLength());
            if(groundRecord){mGroundRecorder.writeGroundRecording(buffer2);}
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
                sampleSize = in.read(buffer2, 0, 18800 * 8 * 8 * 8);
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

    private class GroundRecorder{
        private java.io.FileOutputStream out;
        public GroundRecorder(String s){
            out = null;
            try {
                out=new java.io.FileOutputStream(Environment.getExternalStorageDirectory()+"/"+s);
            } catch (FileNotFoundException e) {e.printStackTrace();Log.w("GroundRecorder", "couldn't create");}
        }
        public void writeGroundRecording(byte[] p){
            try {
                out.write(p);
            } catch (IOException e) {e.printStackTrace();Log.w("GroundRecorder", "couldn't write");}
        }
        public void stop(){
            try {out.close();} catch (IOException e) {e.printStackTrace();Log.w("GroundRecorder", "couldn't close");}
        }
    }
}
