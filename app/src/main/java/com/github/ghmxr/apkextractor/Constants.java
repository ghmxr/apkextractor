package com.github.ghmxr.apkextractor;

import android.support.v7.app.AppCompatDelegate;

import com.github.ghmxr.apkextractor.utils.StorageUtil;

public class Constants {

    /**
     * this preference stands for a string value;
     */
    public static final String PREFERENCE_NAME="settings";
    /**
     * this preference stands for a string value;
     */
    public static final String PREFERENCE_SAVE_PATH="savepath";
    public static final String PREFERENCE_SAVE_PATH_DEFAULT= StorageUtil.getMainExternalStoragePath()+"/Backup";
    /**
     * this preference stands for a boolean value;
     */
    public static final String PREFERENCE_STORAGE_PATH_EXTERNAL="save_external";
    public static final boolean PREFERENCE_STORAGE_PATH_EXTERNAL_DEFAULT=false;

    /**
     * this stands for a String value
     */
    public static final String PREFERENCE_SAVE_PATH_URI="savepath_uri";
    /**
     * this stands for a string value
     */
    public static final String PREFERENCE_SAVE_PATH_SEGMENT="savepath_segment";

    /**
     * this preference stands for a string value;
     */
    public static final String PREFERENCE_FILENAME_FONT_APK="font_apk";
    /**
     * this preference stands for a string value;
     */
    public static final String PREFERENCE_FILENAME_FONT_ZIP="font_zip";
    /**
     * this preference stands for a int value;
     */
    public static final String PREFERENCE_ZIP_COMPRESS_LEVEL="zip_level";
    public static final int PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT=-1;
    /**
     * this preference stands for a int value;
     */
    public static final String PREFERENCE_SHAREMODE="share_mode";
    /**
     * this preference stands for a int value;
     */
    public static final String PREFERENCE_SORT_CONFIG="sort_config";
    /**
     * this preference stands for a int value
     */
    public static final String PREFERENCE_MAIN_PAGE_VIEW_MODE="main_view_mode";
    public static final int PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT=0;
    /**
     * this preference stands for a boolean value;
     */
    public static final String PREFERENCE_SHOW_SYSTEM_APP="show_system_app";

    public static final boolean PREFERENCE_SHOW_SYSTEM_APP_DEFAULT=false;

    /**
     * stands for a boolean value
     */
    public static final String PREFERENCE_LOAD_PERMISSIONS="load_permissions";
    /**
     * stands for a boolean value
     */
    public static final String PREFERENCE_LOAD_ACTIVITIES="load_activities";
    /**
     * stands for a boolean value
     */
    public static final String PREFERENCE_LOAD_RECEIVERS="load_receivers";
    /**
     * stands for a boolean value
     */
    public static final String PREFERENCE_LOAD_STATIC_LOADERS="load_static_receivers";

    /**
     * stands for a int value
     */
    public static final String PREFERENCE_NIGHT_MODE="night_mode";

    public static final int PREFERENCE_NIGHT_MODE_DEFAULT= AppCompatDelegate.MODE_NIGHT_NO;

    public static final boolean PREFERENCE_LOAD_PERMISSIONS_DEFAULT=true;
    public static final boolean PREFERENCE_LOAD_ACTIVITIES_DEFAULT=true;
    public static final boolean PREFERENCE_LOAD_RECEIVERS_DEFAULT=true;
    public static final boolean PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT=false;



    public static final int SHARE_MODE_DIRECT=-1;
    public static final int SHARE_MODE_AFTER_EXTRACT=0;
    public static final int PREFERENCE_SHAREMODE_DEFAULT=SHARE_MODE_DIRECT;

    public static final String FONT_APP_NAME="?N";
    public static final String FONT_APP_PACKAGE_NAME="?P";
    public static final String FONT_APP_VERSIONCODE="?C";
    public static final String FONT_APP_VERSIONNAME="?V";

    public static final int ZIP_LEVEL_STORED=0;
    public static final int ZIP_LEVEL_LOW=1;
    public static final int ZIP_LEVEL_NORMAL=5;
    public static final int ZIP_LEVEL_HIGH=9;

    public static final String PREFERENCE_FILENAME_FONT_DEFAULT=FONT_APP_PACKAGE_NAME+"-"+FONT_APP_VERSIONCODE;


}
