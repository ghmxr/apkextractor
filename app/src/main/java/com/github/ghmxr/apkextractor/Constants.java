package com.github.ghmxr.apkextractor;

import android.support.v7.app.AppCompatDelegate;

import com.github.ghmxr.apkextractor.utils.StorageUtil;

public class Constants {

    /**
     * this preference stands for a string value;
     */
    public static final String PREFERENCE_NAME = "settings";
    /**
     * this preference stands for a string value;
     */
    public static final String PREFERENCE_SAVE_PATH = "savepath";
    public static final String PREFERENCE_SAVE_PATH_DEFAULT = StorageUtil.getMainExternalStoragePath() + "/Backup";
    /**
     * this preference stands for a boolean value;
     */
    public static final String PREFERENCE_STORAGE_PATH_EXTERNAL = "save_external";
    public static final boolean PREFERENCE_STORAGE_PATH_EXTERNAL_DEFAULT = false;

    /**
     * this stands for a String value
     */
    public static final String PREFERENCE_SAVE_PATH_URI = "savepath_uri";
    /**
     * this stands for a string value
     */
    public static final String PREFERENCE_SAVE_PATH_SEGMENT = "savepath_segment";

    /**
     * this preference stands for a string value;
     */
    public static final String PREFERENCE_FILENAME_FONT_APK = "font_apk";
    /**
     * this preference stands for a string value;
     */
    public static final String PREFERENCE_FILENAME_FONT_ZIP = "font_zip";
    /**
     * this preference stands for a int value;
     */
    public static final String PREFERENCE_ZIP_COMPRESS_LEVEL = "zip_level";
    public static final int PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT = -1;
    /**
     * this preference stands for a int value;
     */
    public static final String PREFERENCE_SHAREMODE = "share_mode";
    /**
     * this preference stands for a int value;
     */
    public static final String PREFERENCE_SORT_CONFIG = "sort_config";
    /**
     * 安装包项目的排序方式，int值
     */
    public static final String PREFERENCE_SORT_CONFIG_IMPORT_ITEMS = "sort_config_import";
    /**
     * this preference stands for a int value
     */
    public static final String PREFERENCE_MAIN_PAGE_VIEW_MODE = "main_view_mode";
    public static final int PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT = 1;
    public static final String PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT = "main_view_mode_import";
    public static final int PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT_DEFAULT = 0;
    /**
     * this preference stands for a boolean value;
     */
    public static final String PREFERENCE_SHOW_SYSTEM_APP = "show_system_app";

    public static final boolean PREFERENCE_SHOW_SYSTEM_APP_DEFAULT = false;

    /**
     * stands for a boolean value
     */
    public static final String PREFERENCE_LOAD_PERMISSIONS = "load_permissions";
    /**
     * stands for a boolean value
     */
    public static final String PREFERENCE_LOAD_ACTIVITIES = "load_activities";
    /**
     * stands for a boolean value
     */
    public static final String PREFERENCE_LOAD_RECEIVERS = "load_receivers";
    /**
     * boolean value
     */
    public static final String PREFERENCE_LOAD_SERVICES = "load_services";
    /**
     * boolean value
     */
    public static final String PREFERENCE_LOAD_PROVIDERS = "load_providers";
    /**
     * stands for a boolean value
     */
    public static final String PREFERENCE_LOAD_STATIC_LOADERS = "load_static_receivers";
    /**
     * stands for a boolean value
     */
    public static final String PREFERENCE_LOAD_APK_SIGNATURE = "load_apk_signature";
    /**
     * stands for a boolean value
     */
    public static final String PREFERENCE_LOAD_FILE_HASH = "load_file_hash";

    public static final String PREFERENCE_LOAD_NATIVE_FILE = "load_native_file";

    /**
     * stands for a int value
     */
    public static final String PREFERENCE_NIGHT_MODE = "night_mode";
    /**
     * int value
     */
    public static final String PREFERENCE_LANGUAGE = "language";
    public static final int LANGUAGE_FOLLOW_SYSTEM = 0;
    public static final int LANGUAGE_CHINESE = 1;
    public static final int LANGUAGE_ENGLISH = 2;
    public static final int PREFERENCE_LANGUAGE_DEFAULT = LANGUAGE_FOLLOW_SYSTEM;

    public static final int PREFERENCE_NIGHT_MODE_DEFAULT = AppCompatDelegate.MODE_NIGHT_NO;

    public static final boolean PREFERENCE_LOAD_PERMISSIONS_DEFAULT = true;
    public static final boolean PREFERENCE_LOAD_ACTIVITIES_DEFAULT = true;
    public static final boolean PREFERENCE_LOAD_RECEIVERS_DEFAULT = false;
    public static final boolean PREFERENCE_LOAD_SERVICES_DEFAULT = false;
    public static final boolean PREFERENCE_LOAD_PROVIDERS_DEFAULT = false;
    public static final boolean PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT = false;
    public static final boolean PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT = true;
    public static final boolean PREFERENCE_LOAD_FILE_HASH_DEFAULT = true;
    public static final boolean PREFERENCE_LOAD_NATIVE_FILE_DEFAULT = true;


    public static final int SHARE_MODE_DIRECT = -1;
    public static final int SHARE_MODE_AFTER_EXTRACT = 0;
    public static final int PREFERENCE_SHAREMODE_DEFAULT = SHARE_MODE_DIRECT;

    public static final String FONT_AUTO_SEQUENCE_NUMBER = "?A";
    public static final String FONT_APP_NAME = "?N";
    public static final String FONT_APP_PACKAGE_NAME = "?P";
    public static final String FONT_APP_VERSIONCODE = "?C";
    public static final String FONT_APP_VERSIONNAME = "?V";
    public static final String FONT_YEAR = "?Y";
    public static final String FONT_MONTH = "?M";
    public static final String FONT_DAY_OF_MONTH = "?D";
    public static final String FONT_HOUR_OF_DAY = "?H";
    public static final String FONT_MINUTE = "?I";
    public static final String FONT_SECOND = "?S";

    public static final int ZIP_LEVEL_STORED = 0;
    public static final int ZIP_LEVEL_LOW = 1;
    public static final int ZIP_LEVEL_NORMAL = 5;
    public static final int ZIP_LEVEL_HIGH = 9;

    public static final String PREFERENCE_FILENAME_FONT_DEFAULT = FONT_APP_PACKAGE_NAME + "-" + FONT_APP_VERSIONCODE;

    public static final String ACTION_REFRESH_APP_LIST = "com.github.ghmxr.apkextractor.refresh_applist";
    public static final String ACTION_REFRESH_IMPORT_ITEMS_LIST = "com.github.ghmxr.apkextractor.refresh_import_items_list";
    public static final String ACTION_REFRESH_AVAILIBLE_STORAGE = "com.github.ghmxr.apkextractor.refresh_storage";

    /**
     * 绑定的端口号，1024~65535之间，int值
     */
    public static final String PREFERENCE_NET_PORT = "port_number";
    public static final int PREFERENCE_NET_PORT_DEFAULT = 6565;
    /**
     * 设备名称
     */
    public static final String PREFERENCE_DEVICE_NAME = "device_name";
    public static final String PREFERENCE_DEVICE_NAME_DEFAULT = "MyDevice";

    /**
     * 导出压缩包的扩展名
     */
    public static final String PREFERENCE_COMPRESSING_EXTENSION = "compressing_extension";
    public static final String PREFERENCE_COMPRESSING_EXTENSION_DEFAULT = "zip";

    /**
     * 安装包扫描范围
     */
    public static final String PREFERENCE_PACKAGE_SCOPE = "package_scope";
    public static final int PACKAGE_SCOPE_ALL = 0;
    public static final int PACKAGE_SCOPE_EXPORTING_PATH = 1;
    public static final int PREFERENCE_PACKAGE_SCOPE_DEFAULT = PACKAGE_SCOPE_EXPORTING_PATH;
    /**
     * 批量复制包名的分隔内容
     */
    public static final String PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR = "copying_package_name_separator";
    public static final String PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR_DEFAULT = ",";
}
