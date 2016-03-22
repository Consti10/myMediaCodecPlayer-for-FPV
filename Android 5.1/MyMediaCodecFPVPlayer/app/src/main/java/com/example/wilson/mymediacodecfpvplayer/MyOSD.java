package com.example.wilson.mymediacodecfpvplayer;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.text.TextPaint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class MyOSD {
    int i=0;
    public OverdrawLayer mOverdrawLayer;
    private int mProgram;
    private FloatBuffer vertexBuffer;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    private int mDisplay_x,mDisplay_y;

    float[] mRightEyeViewM=new float[16];
    float[] mLeftEyeViewM=new float[16];
    float[] mProjM=new float[16];
    float[] mHorizonModelM=new float[16];

    private float[] mMVPM=new float[16];

    private volatile boolean UpdateOverdrawLayer=false;
    private volatile int[] UpdateOverdrawValues=new int[1];

    //Circular refreshing
    private int circular_refresh_count=0;
    /*THOUGHTS:
    1) 3 Triangles,rotating in 3 axes, representing my airplane/kopter*/
    //Danger: when angle up_down=0 (=zero degree) ,you can't see the triangle;
    private volatile float angle_up_down=1.0f;
    private volatile float angle_left_right=0;
    private volatile float angle_vertically=0;
    /*
    2)Numbers:
    //Drawn on a simple "texture atlas". Each unit has 200*50 pixels for drawings available, and is being rendered on a quad (2Triangles)*/
     private volatile float battery_life_percentage=0;
    //green when full, orange when normal and red when below 30%
     private volatile float latitude,longitude;
    private volatile float speed=10;
    //3) Home Pfeil


    public MyOSD(int width,int height,int[] textures){
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                OpenGLHelper.triangleCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(OpenGLHelper.triangleCoords);
        vertexBuffer.position(0);

        int   vertexShader=OpenGLHelper.loadShader(GLES20.GL_VERTEX_SHADER,OpenGLHelper.getVertexShader2());
        int fragmentShader=OpenGLHelper.loadShader(GLES20.GL_FRAGMENT_SHADER, OpenGLHelper.getFragmentShader2());

        mProgram=GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram,vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);

        GLES20.glLinkProgram(mProgram);

        mDisplay_x=width;
        mDisplay_y=height;
        float ratio = (float) (width/2) / height;
        Matrix.frustumM(mProjM, 0, -ratio, ratio, -1, 1, 1.0f, 10.0f);
        Matrix.setLookAtM(mLeftEyeViewM, 0, -0.1f, 0.0f, 3.0f, 0.0f, 0.0f, -9.0f, 0.0f, 1.0f, 0.0f);
        Matrix.setLookAtM(mRightEyeViewM, 0, 0.1f, 0.0f, 3.0f, 0.0f, 0.0f, -9.0f, 0.0f, 1.0f, 0.0f);

        mOverdrawLayer=new OverdrawLayer(textures);
    }


    public void drawLeftEye(){
        //draw left eye osd
        GLES20.glViewport(0, 0, mDisplay_x / 2, mDisplay_y);
        draw(mLeftEyeViewM);
        mOverdrawLayer.drawOverlay(mLeftEyeViewM);


    }

    public void drawRightEye(){
        //draw right eye osd
        GLES20.glViewport(mDisplay_x / 2, 0, mDisplay_x / 2, mDisplay_y);
        draw(mRightEyeViewM);
        mOverdrawLayer.drawOverlay(mRightEyeViewM);
    }

    //will be called from the openGl context;
    //we have to make sure it is multithreading-save
    public void draw(float[] viewM){
        Matrix.setIdentityM(mHorizonModelM, 0);
        //left_right
        //angle_left_right=0;
        angle_left_right+=0.1;
        if(angle_left_right>=80){angle_left_right=0;}
        //up_down
        //angle_up_down=1.0f;
        angle_up_down+=0.1;
        if(angle_up_down>=80){angle_up_down=1.0f;}
        //rotating vertically (only for Kopter)
        //angle_vertically=0.0f;
        angle_vertically+=0.1f;
        if(angle_vertically>=80){angle_vertically=0;}
        Matrix.rotateM(mHorizonModelM,0,angle_up_down,1.0f,0.0f,0.0f);
        Matrix.rotateM(mHorizonModelM, 0, angle_left_right, 0.0f, 0.0f, 1.0f);
        Matrix.rotateM(mHorizonModelM, 0, angle_vertically, 0.0f, 1.0f, 0.0f);
        Matrix.multiplyMM(mMVPM, 0, viewM, 0, mHorizonModelM, 0);
        Matrix.multiplyMM(mMVPM, 0, mProjM, 0, mMVPM, 0);

        GLES20.glUseProgram(mProgram);
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, OpenGLHelper.COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                OpenGLHelper.vertexStride, vertexBuffer);
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, OpenGLHelper.color, 0);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPM, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, OpenGLHelper.vertexCount);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mColorHandle);
        GLES20.glDisableVertexAttribArray(mMVPMatrixHandle);
    }




    private class OverdrawLayer {
        private float[] mOverdrawMVPM=new float[16];
        private Canvas c;
        private Paint textPaint;
        private Bitmap bmp;
        private int mProgram2;
        private int mTextureID;
        private int maPositionHandle2;
        private int maTextureHandle2;
        private int mMVPMatrixHandle2;
        private FloatBuffer mTriangleVertices2;

        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;


        public OverdrawLayer(int[] textures){
            mTriangleVertices2 = ByteBuffer.allocateDirect(
                    OpenGLHelper.getOverdrawCoord().length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices2.put(OpenGLHelper.getOverdrawCoord()).position(0);


            mProgram2 = OpenGLHelper.createProgram(OpenGLHelper.getVertexShader3(), OpenGLHelper.getFragmentShader3());
            if (mProgram2 == 0) {
                return;
            }
            maPositionHandle2 = GLES20.glGetAttribLocation(mProgram2, "vPosition");
            OpenGLHelper.checkGlError("glGetAttribLocation vPosition");
            maTextureHandle2 = GLES20.glGetAttribLocation(mProgram2, "a_texCoord");
            OpenGLHelper.checkGlError("glGetAttribLocation a_texCoord");
            mMVPMatrixHandle2 = GLES20.glGetUniformLocation(mProgram2, "uMVPMatrix");
            OpenGLHelper.checkGlError("glGetAttribLocation uMVPMatrix");

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
            bmp=Bitmap.createBitmap(14*200,50, Bitmap.Config.ARGB_8888);
            bmp.setHasAlpha(true);
            bmp.eraseColor(Color.TRANSPARENT);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);

            c=new Canvas(bmp);
            //draw OSD's
            textPaint=new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(40);
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bmp);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            bmp.recycle();
            bmp=Bitmap.createBitmap(200,50, Bitmap.Config.ARGB_8888);
            bmp.setHasAlpha(true);
            c=new Canvas(bmp);
            Timer timer=new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    i++;
                    if(i>100){i=0;}
                    circular_refresh_count++;
                    switch(circular_refresh_count){
                        case  1: refreshSpecifificOverdraw( 0*200,i+"% Bat");break;
                        case  2: refreshSpecifificOverdraw( 1*200,i+"m/s");break;
                        case  3: refreshSpecifificOverdraw( 2*200,"Lat:"+i); break;
                        case  4: refreshSpecifificOverdraw( 3*200,"Lon:"+i); break;
                        case  5: refreshSpecifificOverdraw( 4*200,i+"X"); break;
                        case  6: refreshSpecifificOverdraw( 5*200,i+"X");;break;
                        case  7: refreshSpecifificOverdraw( 6*200,i+"m");;break;
                        case  8: refreshSpecifificOverdraw( 7*200,i+"m");;break;
                        case  9: refreshSpecifificOverdraw( 8*200,i+"A");;break;
                        case 10: refreshSpecifificOverdraw( 9*200,i+"V");;break;
                        case 11: refreshSpecifificOverdraw(10*200,i+"X");;break;
                        case 12: refreshSpecifificOverdraw(11*200,i+"X");;break;
                        case 13: refreshSpecifificOverdraw(12*200,i+"X");;break;
                        case 14: refreshSpecifificOverdraw(13*200,i+"X");circular_refresh_count=0;break;
                    }

                }
            },0,100);
        }
        public void refreshSpecifificOverdraw(int xOffset,String text){
            //Select the Text unit by specifying an Offset,and make Sure,the text isn't too long
            bmp.eraseColor(Color.TRANSPARENT);
            c.drawText(text, 0, 50, textPaint);
            UpdateOverdrawValues[0]=xOffset;
            UpdateOverdrawLayer=true;
        }
        public void drawOverlay(float[] viewM) {
            Matrix.multiplyMM(mOverdrawMVPM, 0, mProjM, 0, viewM, 0);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            if(UpdateOverdrawLayer){
                GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D,0,UpdateOverdrawValues[0],0,bmp);
                UpdateOverdrawLayer=false;
            }
            GLES20.glUseProgram(mProgram2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
            mTriangleVertices2.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maPositionHandle2, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices2);
            GLES20.glEnableVertexAttribArray(maPositionHandle2);
            mTriangleVertices2.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle2, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices2);
            GLES20.glEnableVertexAttribArray(maTextureHandle2);
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle2, 1, false, mOverdrawMVPM, 0);
            int mSamplerLoc = GLES20.glGetUniformLocation (mProgram2,
                    "s_texture" );
            GLES20.glUniform1i(mSamplerLoc, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,6*14);
            GLES20.glDisableVertexAttribArray(maPositionHandle2);
            GLES20.glDisableVertexAttribArray(maTextureHandle2);

            GLES20.glDisable(GLES20.GL_BLEND);
        }
    }




    private class udpDataReceiver extends Thread{
        public void run(){
            while(!Thread.interrupted()){
                //receive udp data
                //parse data by protokoll (f.e Mavlink)

            }
        }
    }
}
