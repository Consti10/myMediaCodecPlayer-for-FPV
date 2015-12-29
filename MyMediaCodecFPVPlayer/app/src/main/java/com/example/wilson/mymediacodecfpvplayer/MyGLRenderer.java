package com.example.wilson.mymediacodecfpvplayer;


import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    private static final float VIDEO_FORMAT_FOR_OPENGL=4.0f/3.0f;

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    private Context mcontext;

    private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];
    private int mProgram;
    private int mTextureID;
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;
    private FloatBuffer mTriangleVertices;

    private SurfaceTexture mSurfaceTexture;
    private Surface mDecoderSurface;
    private boolean updateSurface = false;

    int zaehlerFramerate=0;
    long timeb=0;
    long fps=0;

    private UdpReceiverDecoderThread mDecoder;

    public MyGLRenderer(Context context){
        mcontext=context;
        mTriangleVertices = ByteBuffer.allocateDirect(
                OpenGLHelper.getTriangleVerticesDataByFormat(VIDEO_FORMAT_FOR_OPENGL).length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(OpenGLHelper.getTriangleVerticesDataByFormat(VIDEO_FORMAT_FOR_OPENGL)).position(0);

        Matrix.setIdentityM(mSTMatrix, 0);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {
        System.out.println("onSurfaceCreated");
        GLES20.glClearColor(0.0f, 0.0f, 0.2f, 0.0f);
        //
        mProgram = OpenGLHelper.createProgram(OpenGLHelper.getVertexShader(), OpenGLHelper.getFragmentShader());
        if (mProgram == 0) {
            return;
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        OpenGLHelper.checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        OpenGLHelper.checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        OpenGLHelper.checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }
        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        OpenGLHelper.checkGlError("glGetUniformLocation uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }
        //
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureID = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
        OpenGLHelper.checkGlError("glBindTexture mTextureID");
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        //mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture = new SurfaceTexture(mTextureID,false);
        mSurfaceTexture.setDefaultBufferSize(10,10);
        /*mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                synchronized (this){
                    updateSurface=true;
                }
            }
        });*/
        mDecoderSurface=new Surface(mSurfaceTexture);
        mDecoder=new UdpReceiverDecoderThread(mDecoderSurface,5000,mcontext);
        mDecoder.start();

    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        //updateSurface=true;
        /*long timeBUpdate=System.currentTimeMillis();
        synchronized(this) {
            if (updateSurface) {
                mSurfaceTexture.updateTexImage();
                updateSurface = false;
            }
        }*/
        long timeBUpdate=System.currentTimeMillis();
        mSurfaceTexture.updateTexImage();
        //GLES20.glFinish();
        /*if((System.currentTimeMillis()-timeBUpdate)>=12){
            Log.w("MyGLRenderer","Time for updating:"+(System.currentTimeMillis()-timeBUpdate));
        }*/
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        Matrix.setIdentityM(mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 4, 8);
        //GLES20.glFinish();

        //
        zaehlerFramerate++;
        if((System.currentTimeMillis()-timeb)>1000){
            //System.out.println("fps:"+(zaehlerFramerate/1));
            fps=(zaehlerFramerate/1);
            timeb=System.currentTimeMillis();
            zaehlerFramerate=0;
            Log.w("MyGLRenderer", "fps:" + fps);
        }

    }
    public void onSurfaceDestroyed(){
        mDecoder.interrupt();
    }


}
