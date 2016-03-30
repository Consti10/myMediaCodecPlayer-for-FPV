package com.example.wilson.mymediacodecfpvplayer;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.preference.PreferenceManager;
import android.view.Surface;

import com.google.vrtoolkit.cardboard.PhoneParams;
import com.google.vrtoolkit.cardboard.proto.Phone;
import com.google.vrtoolkit.cardboard.sensors.HeadTracker;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    private HeadTracker mHeadTracker;
    private float[] mRightEyeViewM=new float[16];
    private float[] mLeftEyeViewM=new float[16];
    private float[] mProjM=new float[16];
    private float[] mLeftEyeMVPM=new float[16];
    private float[] mRightEyeMVPM=new float[16];
    private float[] tempEyeViewM=new float[16];
    private float[] mLeftEyeTranslate=new float[16];
    private float[] mRightEyeTranslate=new float[16];
    int[] textures = new int[2];
    //public static boolean next_frame=true;
    //private static final float VIDEO_FORMAT_FOR_OPENGL=16.0f/9.0f;
    SharedPreferences settings;
    private boolean unlimitedOGLFps;
    private boolean swapIntervallZero=true;
    private boolean osd=true;
    private float videoFormat=1.3333f;
    private float modelDistance =3.0f;
    private float videoDistance =5.7f;
    private float interpupilarryDistance=0.2f;
    private float viewportScale=1.0f;
    private boolean headTracking=true;

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    private Context mContext;

    private int mProgram;
    private int mTextureID;
    private int maPositionHandle;
    private int maTextureHandle;
    private int maMVPMatrixHandle;
    private FloatBuffer mTriangleVertices;

    private int mDisplay_x,mDisplay_y;
    private int leftViewportWidth;
    private int leftViewPortHeight;
    private int leftViewportX;
    private int leftViewPortY;
    private int rightViewportWidth;
    private int rightViewportHeight;
    private int rightViewportX;
    private int rightViewportY;

    private SurfaceTexture mSurfaceTexture;
    private Surface mDecoderSurface;
    private MyOSDReceiverRenderer mOSD;
    //private boolean updateSurface = false;

    int zaehlerFramerate=0;
    long timeb=0;
    long fps=0;

    private UdpReceiverDecoderThread mDecoder;

    public MyGLRenderer(Context context){
        mContext =context;
        settings= PreferenceManager.getDefaultSharedPreferences(mContext);
        try{
            videoFormat=Float.parseFloat(settings.getString("videoFormat","1.3333"));
        }catch(Exception e){e.printStackTrace();}
        unlimitedOGLFps=settings.getBoolean("unlimitedOGLFps", false);
        osd=settings.getBoolean("osd", false);
        swapIntervallZero=settings.getBoolean("swapIntervallZero",true);
        try{
            videoDistance =Float.parseFloat(settings.getString("videoDistance","5.7"));
        }catch (Exception e){e.printStackTrace();
            videoDistance =5.7f;}
        if(videoDistance >10 || videoDistance <1){
            videoDistance =5.7f;}
        try{
            interpupilarryDistance=Float.parseFloat(settings.getString("interpupillaryDistance","0.2"));
        }catch (Exception e){e.printStackTrace();interpupilarryDistance=0.2f;}
        if(interpupilarryDistance>=1 || interpupilarryDistance<0.0f){interpupilarryDistance=0.2f;}
        headTracking=settings.getBoolean("headTracking", true);
        try{
            modelDistance =Float.parseFloat(settings.getString("modelDistance","3.0"));
        }catch (Exception e){e.printStackTrace();modelDistance =3.0f;}
        if(modelDistance >10 || modelDistance <2){modelDistance =3.0f;}
        try{
            viewportScale =Float.parseFloat(settings.getString("viewportScale","1.0"));
        }catch (Exception e){e.printStackTrace();viewportScale=1.0f;}
        if(viewportScale>1 || viewportScale <=0){viewportScale=1.0f;}

        mTriangleVertices = ByteBuffer.allocateDirect(
                MyGLRendererHelper.getTriangleVerticesDataByFormat(videoFormat, videoDistance).length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(MyGLRendererHelper.getTriangleVerticesDataByFormat(videoFormat, videoDistance)).position(0);
        System.out.println("MyGLRenderer: MyGLRenderer");
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {
        GLES20.glClearColor(0.0f, 0.0f, 0.07f, 0.0f);
        //
        mProgram = OpenGLHelper.createProgram(MyGLRendererHelper.getVertexShader(), MyGLRendererHelper.getFragmentShader());
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        OpenGLHelper.checkGlError("glGetAttribLocation aPosition");
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        OpenGLHelper.checkGlError("glGetAttribLocation aTextureCoord");
        maMVPMatrixHandle=GLES20.glGetUniformLocation(mProgram,"uMVPMatrix");
        OpenGLHelper.checkGlError("glGetAttribLocation uMVPMatrix");
        //we have to create the texture for the overdraw,too
        GLES20.glGenTextures(2, textures, 0);
        mTextureID = textures[0];
        //I don't know why,but it seems like when you use both external and normal textures,you have to use normal textures for the first,
        //and the external texture for the second unit; bug ?
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
        OpenGLHelper.checkGlError("glBindTexture mTextureID");
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        //mSurfaceTexture = new SurfaceTexture(mTextureID);
        //Enable double buffering,because MediaCodec and OpenGL don't have any synchronisation ?
        //For me,it seems like db hasn't really implemented or has no effect
        mSurfaceTexture = new SurfaceTexture(mTextureID,false);
        mDecoderSurface=new Surface(mSurfaceTexture);
        mDecoder=new UdpReceiverDecoderThread(mDecoderSurface,5000, mContext);
        mDecoder.startDecoding();
        mOSD=new MyOSDReceiverRenderer(mContext,textures,mLeftEyeViewM,mRightEyeViewM,mProjM,videoFormat, modelDistance, videoDistance);
        mOSD.startReceiving();
        mHeadTracker=HeadTracker.createFromContext(mContext);
        mHeadTracker.setNeckModelEnabled(true);
        final Phone.PhoneParams phoneParams = PhoneParams.readFromExternalStorage();
        if (phoneParams != null) {
            this.mHeadTracker.setGyroBias(phoneParams.gyroBias);
        }
        if (headTracking) {
            mHeadTracker.startTracking();
        }
        if(swapIntervallZero){
            EGL14.eglSwapInterval(EGL14.eglGetCurrentDisplay(), 0);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height){
        //Setup Matrices
        float ratio = (float) (width / 2) / height;
        Matrix.frustumM(mProjM, 0, -ratio, ratio, -1, 1, 1.0f, 10.0f);
        Matrix.setLookAtM(mLeftEyeViewM, 0, -(interpupilarryDistance / 2), 0.0f, 0.0f, 0.0f, 0.0f, -12, 0.0f, 1.0f, 0.0f);
        Matrix.setLookAtM(mRightEyeViewM, 0, (interpupilarryDistance / 2), 0.0f, 0.0f, 0.0f, 0.0f, - 12, 0.0f, 1.0f, 0.0f);
        Matrix.setIdentityM(mLeftEyeTranslate, 0);
        Matrix.setIdentityM(mRightEyeTranslate, 0);
        Matrix.translateM(mLeftEyeTranslate, 0, (interpupilarryDistance / 2), 0.0f, 0);
        Matrix.translateM(mRightEyeTranslate, 0, -(interpupilarryDistance / 2), 0.0f, 0);
        GLES20.glViewport(0, 0, width, height);
        mDisplay_x=width;
        mDisplay_y=height;
        mOSD.mDisplay_x=width;
        mOSD.mDisplay_y=height;
        leftViewportWidth=(int)((mDisplay_x/2*viewportScale));
        leftViewPortHeight=(int)((mDisplay_y*viewportScale));
        leftViewportX=(int)(((mDisplay_x/2)-leftViewportWidth)/2);
        leftViewPortY=(int)((mDisplay_y-leftViewPortHeight)/2);
        rightViewportWidth=leftViewportWidth;
        rightViewportHeight=leftViewPortHeight;
        rightViewportX=(mDisplay_x/2)+leftViewportX;
        rightViewportY=leftViewPortY;
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        //Tell android this frame should has been displayed at the time it was created
        if(unlimitedOGLFps){
            EGLExt.eglPresentationTimeANDROID(EGL14.eglGetCurrentDisplay(),EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW),System.nanoTime());
        }
        if(headTracking && mHeadTracker!=null){
            mHeadTracker.getLastHeadView(tempEyeViewM, 0);
            Matrix.multiplyMM(mLeftEyeViewM, 0,  mLeftEyeTranslate,  0, tempEyeViewM, 0);
            Matrix.multiplyMM(mRightEyeViewM, 0, mRightEyeTranslate, 0, tempEyeViewM, 0);
        }
        Matrix.multiplyMM(mLeftEyeMVPM, 0, mProjM, 0, mLeftEyeViewM, 0);
        Matrix.multiplyMM(mRightEyeMVPM, 0, mProjM, 0, mRightEyeViewM, 0);
        //Danger: getTimestamp can't be used to compare with System.nanoTime or System.currentTimeMillis
        //because it's zero point depends on the sources providing the image;
        mSurfaceTexture.updateTexImage();
        GLES20.glFinish();
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        //draw left eye
        GLES20.glUniformMatrix4fv(maMVPMatrixHandle, 1, false, mLeftEyeMVPM, 0);

        GLES20.glViewport(leftViewportX, leftViewPortY, leftViewportWidth, leftViewPortHeight);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, MyGLRendererHelper.triangleVerticesDataNumberOfTraingles);
        //draw right eye
        GLES20.glUniformMatrix4fv(maMVPMatrixHandle, 1, false, mRightEyeMVPM, 0);
        GLES20.glViewport(rightViewportX, rightViewportY, rightViewportWidth, rightViewportHeight);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, MyGLRendererHelper.triangleVerticesDataNumberOfTraingles);
        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maTextureHandle);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
        if(osd){
            mOSD.setupModelMatrices();
            GLES20.glViewport(leftViewportX, leftViewPortY, leftViewportWidth, leftViewPortHeight);
            mOSD.drawLeftEye(mLeftEyeViewM);
            GLES20.glViewport(rightViewportX, rightViewportY, rightViewportWidth, rightViewportHeight);
            mOSD.drawRightEye(mRightEyeViewM);
        }
        zaehlerFramerate++;
        if((System.currentTimeMillis()-timeb)>4000) {
            fps = (long)(zaehlerFramerate / 4.0f);
            System.out.println("fps:"+fps);
            if(fps >3 && mDecoder!=null && mOSD!=null){
                mDecoder.tellOpenGLFps(fps);
                mOSD.mDecoder_fps=mDecoder.getDecoderFps();
            }
            timeb = System.currentTimeMillis();
            zaehlerFramerate = 0;
        }

    }
    public void onSurfaceDestroyed() {
        if(mOSD!=null){
            mOSD.stopReceiving();
        }
        if(mDecoder!=null){
            mDecoder.stopDecoding();
        }
        if(mHeadTracker!=null){
            mHeadTracker.stopTracking();
        }
    }


    public void onTap(){
        if(mDecoder!=null){
            mDecoder.next_frame=true;
            /*
            angle_z+=10;
            System.out.println("Angle_z"+angle_z);*/
        }
    }

}
