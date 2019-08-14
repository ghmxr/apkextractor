package com.github.ghmxr.apkextractor.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.Menu;

import java.lang.reflect.Method;

public abstract class BaseActivity extends AppCompatActivity {

    public static final String EXTRA_PARCELED_APP_ITEM ="app_item";

    @Override
    protected void onCreate(Bundle bundle){
        super.onCreate(bundle);
    }

    @Override
    protected void onResume(){
        super.onResume();
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
    }
    public void setIconEnable(Menu menu, boolean enable) {
        try {
            Class<?> clazz =Class.forName("android.support.v7.view.menu.MenuBuilder"); //Class.forName("com.android.internal.view.menu.MenuBuilder");
            Method m = clazz.getDeclaredMethod("setOptionalIconsVisible", boolean.class);
            m.setAccessible(true);
            m.invoke(menu, enable);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
