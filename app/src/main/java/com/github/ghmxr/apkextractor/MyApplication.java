package com.github.ghmxr.apkextractor;

import android.app.Application;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatDelegate;

import com.github.ghmxr.apkextractor.utils.SPUtil;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences settings = SPUtil.getGlobalSharedPreferences(this);
        int night_mode = settings.getInt(Constants.PREFERENCE_NIGHT_MODE, Constants.PREFERENCE_NIGHT_MODE_DEFAULT);
        AppCompatDelegate.setDefaultNightMode(night_mode);
    }
}
