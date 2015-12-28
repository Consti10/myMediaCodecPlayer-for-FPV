package com.example.wilson.mymediacodecfpvplayer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/*Constantin Geier, 28.12.2015;
 */
public class MainActivity extends Activity {
    Intent mSurfaceViewI;
    Intent mOpenGLI;
    Intent mTextureViewI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView mTextView=new TextView(this);
        mTextView.setText("MyMediaCodecFPVPlayer");
        mTextView.setTextColor(Color.BLUE);
        Button mSurfaceViewB=new Button(this);
        mSurfaceViewB.setText("Start SurfaceView Player");
        mSurfaceViewI=new Intent();
        mSurfaceViewI.setClass(this, SurfaceViewActivity.class);
        mSurfaceViewB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mSurfaceViewI);
            }
        });
        Button mOpenGLB=new Button(this);
        mOpenGLB.setText("Start OpenGL Player");
        mOpenGLI=new Intent();
        mOpenGLI.setClass(this, OpenGLActivity.class);
        mOpenGLB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mOpenGLI);
            }
        });
        Button mSurfaceTextureB=new Button(this);
        mSurfaceTextureB.setText("Start TextureView Player");
        mTextureViewI=new Intent();
        mTextureViewI.setClass(this,TextureViewActivity.class);
        mSurfaceTextureB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mTextureViewI);
            }
        });

        LinearLayout mLinearLayout=new LinearLayout(this);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
        mLinearLayout.addView(mTextView);
        mLinearLayout.addView(mSurfaceViewB);
        mLinearLayout.addView(mOpenGLB);
        mLinearLayout.addView(mSurfaceTextureB);

        setContentView(mLinearLayout);
    }

}
