package com.example.wilson.mymediacodecfpvplayer;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class OpenGLActivity extends AppCompatActivity {
    private GLSurfaceView mGLView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature((Window.FEATURE_NO_TITLE));
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        };

        View decorView= getWindow().getDecorView();
        int uiOptions=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                ;
        decorView.setSystemUiVisibility(uiOptions);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mGLView = new MyGLSurfaceView(this);
        setContentView(mGLView);
    }


    private class MyGLSurfaceView extends GLSurfaceView  {

        private MyGLRenderer mRenderer;
        private Context mContext;

        public MyGLSurfaceView(Context context){
            super(context);
            mContext=context;
            setEGLContextClientVersion(2);
            mRenderer = new MyGLRenderer(mContext);
            setRenderer(mRenderer);
        }
        @Override
        public void onPause(){
        }
        @Override
        public void onResume() {
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder){
            mRenderer.onSurfaceDestroyed();
        }


    }

}
