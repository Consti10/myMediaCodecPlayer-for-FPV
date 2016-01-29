package com.example.wilson.mymediacodecfpvplayer;


import android.opengl.GLES20;

public class MyOSD {
    //
    private volatile float angle_y;
    //

    //will be called from the openGl context;
    //we have to make sure it is multithreading-save
    public void draw(){
        //...GLES20.glUseProgram();
        //...float[] mLeftEyeMatrix = new float[16];
        drawLeftEye();
        drawRightEye();
    }

    private void drawLeftEye(){
        //...GLES20.glDrawArrays();
    }

    private void drawRightEye(){
        //...GLES20.glDrawArrays();
    }


    private class udpDataReceiver extends Thread{
        public void run(){
            while(!Thread.interrupted()){
                //receive udp data
                //parse data by protokoll (f.e Mavlink)
                angle_y=10;
            }
        }
    }
}
