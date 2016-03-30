package com.example.wilson.mymediacodecfpvplayer;



public class DeletedStuff {
    /*
        double x=-1*Math.cos(Math.toRadians(angle_x))*Math.cos(Math.toRadians(90-angle_y));
        double y=-1*Math.sin(Math.toRadians(angle_x));
        double z=   Math.cos(Math.toRadians(angle_x))*Math.sin(Math.toRadians(90-angle_y));
        double x2=  Math.sin(Math.toRadians(angle_z));
        double y2=  Math.cos(Math.toRadians(angle_z));
        float drei_d_factor=0.1f;
        float r=12.0f;
        float eyeX = 0.0f;
        float eyeY = 0.0f;
        float eyeZ = 3.0f;
        float lookX =(float)(eyeX-(r*x));
        float lookY =(float)(eyeY-(r*y));;
        float lookZ =(float)(eyeZ-(r*z));
        float upX = (float)x2;
        float upY= (float)y2;
        float upZ = 0;
        Matrix.setLookAtM(mLeftEyeViewM  , 0, eyeX-drei_d_factor, eyeY,eyeZ, lookX, lookY,lookZ, upX, upY, upZ);
        Matrix.setLookAtM(mRightEyeViewM , 0, eyeX+drei_d_factor, eyeY,eyeZ, lookX, lookY,lookZ, upX, upY, upZ);*/

    //When looking straight forward,all angles are 0
        /*
        angle_x+=0.05f;
        if(angle_x>=360){angle_x=0;}
        System.out.println("angle_x" + angle_x);*/
        /*
        angle_y+=0.1f;
        if(angle_y>=360){angle_y=0;}
        System.out.println("angle_y" + angle_y);*/
        /*
        angle_z+=0.5f;
        if(angle_z>=360){angle_z=0;}*/
        /*
        float[] view=new float[16];

        for(int i=0;i<view.length;i++){
            view[i]=0;
        }
        for(int i=0;i<view.length;i++){
            System.out.print(""+view[i]+", ");
        }
        System.out.println("");
        mHeadTracker.getLastHeadView(view, 0);
        for(int i=0;i<view.length;i++){
            System.out.print(""+view[i]+", ");
        }
        System.out.println("");
        System.out.println("");*/

     /*mSurfaceTexture.updateTexImage();
        GLES20.glFinish();
        mSurfaceTexture.updateTexImage();
        GLES20.glFinish();
        mSurfaceTexture.updateTexImage();
        GLES20.glFinish();
        mSurfaceTexture.updateTexImage();
        GLES20.glFinish();*/
    //Log.w("renderer", "since last time: " + ((mSurfaceTexture.getTimestamp() - SurfaceTextureTimeB) / 1000));
    //SurfaceTextureTimeB=mSurfaceTexture.getTimestamp();
    //Log.w("MyGLRenderer","Time for updating:"+(System.currentTimeMillis()-timeBUpdate));
    //GLES20.glFinish();
    //GLES20.glFlush();
        /*if((System.currentTimeMillis()-timeBUpdate)>=12){
            Log.w("MyGLRenderer","Time for updating:"+(System.currentTimeMillis()-timeBUpdate));
        }*/
}
