package com.github.ghmxr.apkextractor.activities;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Menu;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.utils.SPUtil;

import java.lang.reflect.Method;
import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    /**
     * @deprecated 使用传递包名的方式
     */
    public static final String EXTRA_PARCELED_APP_ITEM = "app_item";

    public static final String EXTRA_PACKAGE_NAME = "package_name";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setAndRefreshLanguage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
    }

    public void setIconEnable(Menu menu, boolean enable) {
        try {
            Class<?> clazz = Class.forName("android.support.v7.view.menu.MenuBuilder"); //Class.forName("com.android.internal.view.menu.MenuBuilder");
            Method m = clazz.getDeclaredMethod("setOptionalIconsVisible", boolean.class);
            m.setAccessible(true);
            m.invoke(menu, enable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAndRefreshLanguage() {
        // 获得res资源对象
        Resources resources = getResources();
        // 获得屏幕参数：主要是分辨率，像素等。
        DisplayMetrics metrics = resources.getDisplayMetrics();
        // 获得配置对象
        Configuration config = resources.getConfiguration();
        //区别17版本（其实在17以上版本通过 config.locale设置也是有效的，不知道为什么还要区别）
        //在这里设置需要转换成的语言，也就是选择用哪个values目录下的strings.xml文件
        int value = SPUtil.getGlobalSharedPreferences(this).getInt(Constants.PREFERENCE_LANGUAGE, Constants.PREFERENCE_LANGUAGE_DEFAULT);
        Locale locale = null;
        switch (value) {
            default:
                break;
            case Constants.LANGUAGE_FOLLOW_SYSTEM:
                locale = Locale.getDefault();
                break;
            case Constants.LANGUAGE_CHINESE:
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case Constants.LANGUAGE_ENGLISH:
                locale = Locale.ENGLISH;
                break;
        }
        if (locale == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        resources.updateConfiguration(config, metrics);
    }
}
