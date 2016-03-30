package com.example.wilson.mymediacodecfpvplayer;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class SurfaceViewActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private UdpReceiverDecoderThread mDecoder = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        setContentView(sv);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mDecoder == null) {
            mDecoder = new UdpReceiverDecoderThread(holder.getSurface(), 5000,this);
            mDecoder.startDecoding();
        }
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(mDecoder != null) {
            mDecoder.stopDecoding();
        }
    }

}
