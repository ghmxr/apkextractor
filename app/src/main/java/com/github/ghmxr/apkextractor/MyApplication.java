package com.github.ghmxr.apkextractor;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
