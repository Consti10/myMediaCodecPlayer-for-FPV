package com.example.wilson.mymediacodecfpvplayer;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MyUserSpecificDataHelper {
    private SharedPreferences settings;
    private Context mContext;

    public MyUserSpecificDataHelper(Context context){
        mContext=context;
        settings= PreferenceManager.getDefaultSharedPreferences(mContext);
    }

}
