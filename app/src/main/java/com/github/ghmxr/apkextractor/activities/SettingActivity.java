package com.github.ghmxr.apkextractor.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.github.ghmxr.apkextractor.R;

public class SettingActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_settings);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar_settings));
        try{
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }catch (Exception e){e.printStackTrace();}
    }
}
