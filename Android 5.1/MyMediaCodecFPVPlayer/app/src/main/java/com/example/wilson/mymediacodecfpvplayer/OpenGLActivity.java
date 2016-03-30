package com.example.wilson.mymediacodecfpvplayer;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.hardware.Sensor;

import com.google.vrtoolkit.cardboard.sensors.HeadTracker;

public class OpenGLActivity extends AppCompatActivity  {
    private GLSurfaceView mGLView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature((Window.FEATURE_NO_TITLE));
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        ;

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mGLView = new MyGLSurfaceView(this);
        //mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setContentView(mGLView);
    }
    @Override
    protected void onPause(){
        super.onPause();
        mGLView.onPause();
    }
    @Override
    protected void onResume(){
        super.onResume();
        mGLView.onResume();
    }

    private class MyGLSurfaceView extends GLSurfaceView {
        private MyGLRenderer mRenderer;
        private Context mContext;

        public MyGLSurfaceView(Context context) {
            super(context);
            mContext = context;
            setEGLContextClientVersion(2);
            mRenderer = new MyGLRenderer(mContext);
            setRenderer(mRenderer);
        }

        @Override
        public void onPause() {
            super.onPause();
            //mRenderer.onPause();
            //System.out.println("OGLActivity" + "On Pause");
        }

        @Override
        public void onResume() {
            super.onResume();
            //mRenderer.onResume();
            //System.out.println("OGLActivity" + "On Resume");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mRenderer.onSurfaceDestroyed();
            System.out.println("OGLActivity" + "Surface Destroyed");
        }


        @Override
        public boolean onTouchEvent(MotionEvent e) {
            Log.d("", "Hello");
            if(e.getActionMasked()==MotionEvent.ACTION_UP){
                if(mRenderer != null){
                    mRenderer.onTap();
                }
            }
            return true;
        }
        /*
        @Override
        public void onAccuracyChanged(Sensor sensor,int accuracy){
        }

        @Override
        public void onSensorChanged(SensorEvent event){
            float[] orientationValues=new float[3];
            float[] tempRotationM=new float[3];
            float[] rotationM=new float[4*4];
            switch(event.sensor.getType()){
                case Sensor.TYPE_ROTATION_VECTOR:
                    SensorManager.getRotationMatrixFromVector(rotationM, event.values);
                    //SensorManager.remapCoordinateSystem(tempRotationM,0,0,rotationM);
                    SensorManager.getOrientation(rotationM,orientationValues);

                    double x=-90-Math.toDegrees(orientationValues[2]);
                    mRenderer.angle_x=x;
                    //System.out.println("X: "+x);
                    double y=Math.toDegrees(orientationValues[0]);
                    mRenderer.angle_y=y;
                    //System.out.println("Y: "+y);
                    /*
                    double z=-2*Math.toDegrees(orientationValues[1]);
                    mRenderer.angle_z=z;
                    System.out.println("Z: "+z);
                    break;
                default: Log.w("OGLActivity","Unknown Sensor Event");

            }
        }*/
    }

}
