package com.example.wilson.mymediacodecfpvplayer;


import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.preference.PreferenceManager;
import android.content.Context;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.FloatBuffer;

//call startReceiving to start OSD;
//Threads: 2, first for refreshing the Overdraw Canvas, second for receiving udp data
public class MyOSDReceiverRenderer {
    private DatagramSocket s = null;
    private Thread circularRefreshThread,receiveFromUDPThread;
    private volatile boolean running=true;
    private boolean countUp1=true,countUp2=true,countUp3=true,countUp4true;
    private final int mBytesPerFloat = 4;
    private final int mStrideBytes = 7 * mBytesPerFloat;
    private final int mPositionDataSize = 3;
    private final int mColorOffset = 3;
    private final int mColorDataSize = 4;
    private int buffers[] = new int[2];
    int i=0,i2=1;
    public OverdrawLayer mOverdrawLayer;
    private int mProgram;
    private FloatBuffer vertexBuffer;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    public int mDisplay_x,mDisplay_y;
    float[] mProjM=new float[16];
    float[] mKopterModelM =new float[16];
    float[] mHomeArrowModelM=new float[16];
    private float[] mHeightModelM=new float[16];
    private float[] mOSDModelM=new float[16];
    private float[] mMVPM=new float[16];
    private float[] mWorldDistanceTranslationM=new float[16];
    private float[] scratch=new float[16];
    private float[] scratch2=new float[16];
    private volatile boolean UpdateOverdrawLayer=false;
    private volatile int[] UpdateOverdrawValues=new int[1];
    //Circular refreshing
    private int circular_refresh_count=0;
    //Booleans for OSD
    private boolean enable_model;
    private boolean enable_home_arrow;
    private boolean enable_fps;
    private boolean enable_battery_life;
    private boolean enable_lattitude_longitude;
    private boolean enable_X1;
    private boolean enable_X2;
    private boolean enable_height;
    private boolean enable_voltage;
    private boolean enable_ampere;
    private boolean enable_home_distance;
    private boolean enable_X3;
    private boolean enable_speed;
    private boolean enable_X4;
    private int paint_size;
    /*THOUGHTS:
    1) 5 Triangles,rotating in 3 axes, representing my airplane/kopter*/
    //Danger: when angle up_down=0 (=zero degree) ,you can't see the triangle;
    private volatile float angle_x =1.0f;
    private volatile float angle_z =30;
    private volatile float angle_y =0;
    /*
    2)Numbers:
    //Drawn on a simple "texture atlas". Each unit has 200*50 pixels for drawings available, and is being rendered on a quad (2Triangles)*/
    //green when full, orange when normal and red when below 30%
    public volatile int mDecoder_fps=49;
    private volatile float mBattery_life_percentage=0;
    private volatile float mLatitude=10,mLongitude=10;
    private volatile int x1=0,x2=0;
    private volatile float mHeight_m=50;
    private volatile float mVoltage=6;
    private volatile float mAmpere=10;
    private volatile float mHome_Distance_m=100;
    private volatile float x3=0;
    private volatile float mSpeed_ms=10;
    private volatile float x4=0;
    //3) Home Pfeil
    private volatile float mHome_Arrow_angle_y=0;


    public MyOSDReceiverRenderer(Context context, int[] textures,float[] leftEyeViewM,float[] rightEyeViewM,float[] projectionM,float videoFormat,float modelDistance,float videoDistance){
        SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(context);
        enable_model=settings.getBoolean("enable_model", true);
        enable_home_arrow=settings.getBoolean("enable_home_arrow", true);
        enable_fps=settings.getBoolean("enable_fps", true);
        enable_battery_life=settings.getBoolean("enable_battery_life", true);
        enable_lattitude_longitude=settings.getBoolean("enable_lattitude_longitude", true);
        enable_X1=settings.getBoolean("enable_x1", true);
        enable_X2=settings.getBoolean("enable_x2", true);
        enable_height=settings.getBoolean("enable_height", true);
        enable_voltage=settings.getBoolean("enable_voltage", true);
        enable_ampere=settings.getBoolean("enable_ampere", true);
        enable_home_distance=settings.getBoolean("enable_home_distance", true);
        enable_X3=settings.getBoolean("enable_x3", true);
        enable_speed=settings.getBoolean("enable_speed", true);
        enable_X4=settings.getBoolean("enable_x4", true);
        vertexBuffer = OpenGLHelper.getFloatBuffer(MyOSDReceiverRendererHelper.getTriangleCoords());
        GLES20.glGenBuffers(2, buffers, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuffer.capacity() * mBytesPerFloat,
                vertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        int   vertexShader=OpenGLHelper.loadShader(GLES20.GL_VERTEX_SHADER,MyOSDReceiverRendererHelper.getVertexShader2());
        int fragmentShader=OpenGLHelper.loadShader(GLES20.GL_FRAGMENT_SHADER, MyOSDReceiverRendererHelper.getFragmentShader2());
        mProgram=GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgram, "a_Color");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        mProjM=projectionM;
        Matrix.setIdentityM(mWorldDistanceTranslationM,0);
        Matrix.translateM(mWorldDistanceTranslationM,0,0.0f,0.0f,-modelDistance);
        mOverdrawLayer=new OverdrawLayer(textures,videoFormat,videoDistance);
        receiveFromUDPThread=new Thread(){
            @Override
            public void run() {
                receiveFromUDP();}
        };
        circularRefreshThread=new Thread(){
            @Override
            public void run() {
                refreshCircular();}
        };
    }
    public void startReceiving(){
        running=true;
        receiveFromUDPThread.start();
        circularRefreshThread.start();
    }
    public void stopReceiving(){
        running=false;
    }

    public void refreshCircular(){
        while(running){
            i++;
            if(i>100){i=0;}
            circular_refresh_count++;
            switch(circular_refresh_count){
                case  1: if(! enable_fps)                {circular_refresh_count++;}else{break;};
                case  2: if(! enable_battery_life)       {circular_refresh_count++;}else{break;};
                case  3: if(! enable_lattitude_longitude){circular_refresh_count++;}else{break;};
                case  4: if(! enable_lattitude_longitude){circular_refresh_count++;}else{break;};
                case  5: if(! enable_X1)                 {circular_refresh_count++;}else{break;};
                case  6: if(! enable_X2)                 {circular_refresh_count++;}else{break;};
                case  7: if(! enable_height)             {circular_refresh_count++;}else{break;};
                case  8: if(! enable_height)             {circular_refresh_count++;}else{break;};
                case  9: if(! enable_voltage)            {circular_refresh_count++;}else{break;};
                case 10: if(! enable_ampere)             {circular_refresh_count++;}else{break;};
                case 11: if(! enable_home_distance)      {circular_refresh_count++;}else{break;};
                case 12: if(! enable_X3)                 {circular_refresh_count++;}else{break;};
                case 13: if(! enable_speed)              {circular_refresh_count++;}else{break;};
                case 14: if(! enable_X4)                 {circular_refresh_count=0;};break;

            }
            switch(circular_refresh_count){
                case  1: mOverdrawLayer.refreshSpecifificTextureUnit(0, "D:" + mDecoder_fps + "fps");break;
                case  2: mOverdrawLayer.refreshSpecifificTextureUnit(1, mBattery_life_percentage + "% Bat.");break;
                case  3: mOverdrawLayer.refreshSpecifificTextureUnit(2, "Lat." + mLatitude); break;
                case  4: mOverdrawLayer.refreshSpecifificTextureUnit(3, "Lon.:" + mLongitude); break;
                case  5: mOverdrawLayer.refreshSpecifificTextureUnit(4, i + "X"); break;
                case  6: mOverdrawLayer.refreshSpecifificTextureUnit(5, i + "X");break;
                case  7: mOverdrawLayer.refreshSpecifificTextureUnit(6, ((int) mHeight_m) + "m");break;
                case  8: mOverdrawLayer.refreshSpecifificTextureUnit(7, ((int) mHeight_m) + "m");break;
                case  9: mOverdrawLayer.refreshSpecifificTextureUnit(8, mVoltage + "V");break;
                case 10: mOverdrawLayer.refreshSpecifificTextureUnit(9, mAmpere + "A");break;
                case 11: mOverdrawLayer.refreshSpecifificTextureUnit(10, mHome_Distance_m + "m");break;
                case 12: mOverdrawLayer.refreshSpecifificTextureUnit(11, i + "X");break;
                case 13: mOverdrawLayer.refreshSpecifificTextureUnit(12, mSpeed_ms + "m/s");break;
                case 14: mOverdrawLayer.refreshSpecifificTextureUnit(13, i + "X");circular_refresh_count=0;break;
                default:break;
            }
        }
    }
    private void receiveFromUDP() {
        int server_port = 6000;
        byte[] message = new byte[1024];
        DatagramPacket p = new DatagramPacket(message, message.length);
        boolean exception=false;
        try {s = new DatagramSocket(server_port);
            s.setSoTimeout(500);
        } catch (SocketException e) {e.printStackTrace();}
        while (running && s != null) {
            try {
                s.receive(p);
            } catch (IOException e) {
                exception=true;
                if(! (e instanceof SocketTimeoutException)){
                    e.printStackTrace();
                }
            }
            if(!exception){
                System.out.println("Receiving OSD Data; Parsing required");
                //we have to parse Telemetry Data
            }else{exception=false;}
        }
        if (s != null) {
            s.close();
            s=null;
        }
    }

    public void setupModelMatrices(){
        //Setup ModelMatrices(needs only be done one Time per Frame)
        if(countUp1){angle_z +=0.2;}else{angle_z -=0.2;}if(angle_z >=40){countUp1=false;}if(angle_z <=-40){countUp1=true;}
        //up_down
        if(countUp2){angle_x +=0.2;}else{angle_x -=0.2;}if(angle_x >=40){countUp2=false;}if(angle_x <=-40){countUp2=true;}
        //rotating vertically (only for Kopter)
        if(countUp3){angle_y +=0.2;}else{angle_y -=0.2;}if(angle_y >=40){countUp3=false;}if(angle_y <=-40){countUp3=true;}
        mHome_Arrow_angle_y+=0.4;if(mHome_Arrow_angle_y>=360){mHome_Arrow_angle_y=0;}
        mHeight_m+=0.1;if(mHeight_m>=100){mHeight_m=0;}
        //
        Matrix.setIdentityM(scratch,0);
        Matrix.rotateM(scratch, 0, angle_x, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(scratch, 0, angle_y, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(scratch, 0, angle_z, 0.0f, 0.0f, 1.0f);
        //Lines and their canvases
        float translate_height=(-50.0f+mHeight_m)/25.0f;
        float[] temp=new float[16];
        Matrix.setIdentityM(scratch2, 0);
        Matrix.translateM(scratch2, 0, 0.0f, -translate_height, 0);
        Matrix.multiplyMM(temp, 0, scratch, 0, scratch2, 0);
        Matrix.multiplyMM(mHeightModelM, 0,mWorldDistanceTranslationM, 0, temp, 0);
        //Kpter and side arrows
        Matrix.multiplyMM(mKopterModelM,0,mWorldDistanceTranslationM,0,scratch,0);
        //home arrow
        Matrix.setIdentityM(scratch2, 0);
        Matrix.rotateM(scratch2, 0, mHome_Arrow_angle_y, 0.0f, 1.0f, 0.0f);
        Matrix.multiplyMM(mHomeArrowModelM,0,mWorldDistanceTranslationM,0,scratch2,0);
        //OSD
        /*Matrix.setIdentityM(scratch2,0);
        Matrix.multiplyMM(mOSDModelM,0,mWorldDistanceTranslationM,0,scratch2,0);*/

    }

    public void drawLeftEye(float[] mLeftEyeViewM){
        draw(mLeftEyeViewM);
        mOverdrawLayer.drawOverlay(mLeftEyeViewM);
    }

    public void drawRightEye(float[] mRightEyeViewM){
        draw(mRightEyeViewM);
        mOverdrawLayer.drawOverlay(mRightEyeViewM);
    }

    //will be called from the openGl context;
    //we have to make sure it is multithreading-save
    public void draw(float[] viewM){
        GLES20.glUseProgram(mProgram);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false, mStrideBytes, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize,
                GLES20.GL_FLOAT, false, mStrideBytes, mColorOffset * mBytesPerFloat);
        if(enable_height){
            Matrix.multiplyMM(mMVPM, 0, viewM, 0, mHeightModelM, 0);
            Matrix.multiplyMM(mMVPM, 0, mProjM, 0, mMVPM, 0);
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPM, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 18 * 6);
        }
        if(enable_model){
            Matrix.multiplyMM(mMVPM, 0, viewM, 0, mKopterModelM, 0);
            Matrix.multiplyMM(mMVPM, 0, mProjM, 0, mMVPM, 0);
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPM, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 18*6, 5*3);
        }
        if(enable_height){
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, (18*6)+(5*3), 2*3);
        }
        if(enable_home_arrow){
            Matrix.multiplyMM(mMVPM, 0, viewM, 0, mHomeArrowModelM, 0);
            Matrix.multiplyMM(mMVPM, 0, mProjM, 0, mMVPM, 0);
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPM, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, (18*6)+(7*3), 3);
        }
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mColorHandle);
        GLES20.glDisableVertexAttribArray(mMVPMatrixHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }




    private class OverdrawLayer {
        //14 for OSD info, 5 for the height representation
        private int number_of_units=19;
        private int unitWidth=200,unitHeight=50;
        private int atlasWidth=(number_of_units*unitWidth),atlasHeight=unitHeight;
        private float[] mOverdrawMVPM=new float[16];
        private Canvas c;
        private Paint textPaint;
        private Bitmap bmp;
        private int mProgram2;
        private int mTextureID;
        private int maPositionHandle2;
        private int maTextureHandle2;
        private int mMVPMatrixHandle2;
        private int mSamplerLoc;
        private FloatBuffer mTriangleVertices2;
        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

        public OverdrawLayer(int[] textures,float videoFormat,float videoDistance){
            mTriangleVertices2 = OpenGLHelper.getFloatBuffer(MyOSDReceiverRendererHelper.getOverdrawCoordByFormat(number_of_units,videoFormat,videoDistance));
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mTriangleVertices2.capacity() * mBytesPerFloat,
                    mTriangleVertices2, GLES20.GL_STATIC_DRAW);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            mProgram2 = OpenGLHelper.createProgram(MyOSDReceiverRendererHelper.getVertexShader3(), MyOSDReceiverRendererHelper.getFragmentShader3());
            if (mProgram2 == 0) {return;}
            maPositionHandle2 = GLES20.glGetAttribLocation(mProgram2, "vPosition");
            OpenGLHelper.checkGlError("glGetAttribLocation vPosition");
            maTextureHandle2 = GLES20.glGetAttribLocation(mProgram2, "a_texCoord");
            OpenGLHelper.checkGlError("glGetAttribLocation a_texCoord");
            mMVPMatrixHandle2 = GLES20.glGetUniformLocation(mProgram2, "uMVPMatrix");
            OpenGLHelper.checkGlError("glGetAttribLocation uMVPMatrix");
            mSamplerLoc = GLES20.glGetUniformLocation (mProgram2, "s_texture" );
            OpenGLHelper.checkGlError("glGetAttribLocation s_texture");
            mTextureID = textures[1];
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
            OpenGLHelper.checkGlError("glBindTexture mTextureID");
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_CONSTANT_ALPHA);
            GLES20.glDisable(GLES20.GL_BLEND);
            //Texture Atlas-place for 2 "Text" unit's, each 200 by 50 pixels huge
            bmp=Bitmap.createBitmap(atlasWidth,atlasHeight, Bitmap.Config.ARGB_8888);
            bmp.setHasAlpha(true);
            bmp.eraseColor(Color.TRANSPARENT);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
            c=new Canvas(bmp);
            //draw OSD's
            textPaint=new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(unitHeight);
            c.drawText("100m",14*unitWidth,unitHeight-((int)(unitHeight/5)), textPaint);
            c.drawText("75m", 15*unitWidth,unitHeight-((int)(unitHeight/5)), textPaint);
            c.drawText("50m", 16*unitWidth,unitHeight-((int)(unitHeight/5)), textPaint);
            c.drawText("25m", 17*unitWidth,unitHeight-((int)(unitHeight/5)), textPaint);
            c.drawText("0m" , 18*unitWidth,unitHeight-((int)(unitHeight/5)), textPaint);
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bmp);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            bmp.recycle();
            bmp=Bitmap.createBitmap(unitWidth,unitHeight, Bitmap.Config.ARGB_8888);
            bmp.setHasAlpha(true);
            c=new Canvas(bmp);
        }
        public void refreshSpecifificTextureUnit(int unitNumber, String text){
            //Select the Text unit by specifying an Offset,and make Sure,the text isn't too long
            while(!UpdateOverdrawLayer){
                //wait,until the last bitmap change get's updated onto the screen
                try{Thread.sleep(1,0);}catch(Exception e){e.printStackTrace();}
            }
            bmp.eraseColor(Color.TRANSPARENT);
            c.drawText(text, 0, unitHeight-((int)(unitHeight/5)), textPaint);
            UpdateOverdrawValues[0]=(unitNumber*unitWidth);
            UpdateOverdrawLayer=false;
        }

        public void drawOverlay(float[] viewM) {

            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            //Update every second frame (at 30fps)
            if(i2==1){
                GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, UpdateOverdrawValues[0],0,bmp);
                UpdateOverdrawLayer=true;
                i2=4;
            }else{i2-=1;}
            GLES20.glUseProgram(mProgram2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);
            GLES20.glEnableVertexAttribArray(maPositionHandle2);
            GLES20.glVertexAttribPointer(maPositionHandle2, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, 0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);
            GLES20.glEnableVertexAttribArray(maTextureHandle2);
            GLES20.glVertexAttribPointer(maTextureHandle2, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, 3 * mBytesPerFloat);
            GLES20.glUniform1i(mSamplerLoc, 0);
            if(true){
                /*
                Matrix.multiplyMM(mMVPM, 0, viewM, 0, mOSDModelM, 0);
                Matrix.multiplyMM(mMVPM, 0, mProjM, 0, mMVPM, 0);*/
                Matrix.multiplyMM(mMVPM,0,mProjM,0,viewM,0);
                GLES20.glUniformMatrix4fv(mMVPMatrixHandle2, 1, false, mMVPM, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6 * 14);
            }
            if(enable_height){
                Matrix.multiplyMM(mMVPM, 0, viewM, 0, mHeightModelM, 0);
                Matrix.multiplyMM(mMVPM, 0, mProjM, 0, mMVPM, 0);
                GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPM, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 6 * 14, 10*6);
            }
            GLES20.glDisableVertexAttribArray(maPositionHandle2);
            GLES20.glDisableVertexAttribArray(maTextureHandle2);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            GLES20.glDisable(GLES20.GL_BLEND);
        }
    }
}
