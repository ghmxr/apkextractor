package com.github.ghmxr.apkextractor;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.StrictMode;

import androidx.appcompat.app.AppCompatDelegate;

import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;

public class MyApplication extends Application {

    private static Application application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        SharedPreferences settings = SPUtil.getGlobalSharedPreferences(this);
        int night_mode = settings.getInt(Constants.PREFERENCE_NIGHT_MODE, Constants.PREFERENCE_NIGHT_MODE_DEFAULT);
        AppCompatDelegate.setDefaultNightMode(night_mode);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && EnvironmentUtil.getTargetSdkVersion() > 23) {
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static Application getApplication() {
        return application;
    }
}
