package com.github.ghmxr.apkextractor.data;

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
	public static final String PREFERENCE_SAVE_PATH_DEFAULT=StorageUtil.getMainStoragePath()+"/Backup";
	/**
	 * this preference stands for a string value;
	 */
	public static final String PREFERENCE_STORAGE_PATH="storage_path";
	public static final String PREFERENCE_STORAGE_PATH_DEFAULT=StorageUtil.getMainStoragePath();
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
	public static final String PREFERENCE_SHAREMODE="share_mode";
	/**
	 * this preference stands for a int value;
	 */
	public static final String PREFERENCE_SORT_CONFIG="sort_config";
	/**
	 * this preference stands for a boolean value;
	 */
	public static final String PREFERENCE_SHOW_SYSTEM_APP="show_system_app";
	
	public static final int SHARE_MODE_DIRECT=-1;
	public static final int SHARE_MODE_AFTER_EXTRACT=0;
	public static final int PREFERENCE_SHAREMODE_DEFAULT=SHARE_MODE_DIRECT;
		
	public static final String FONT_APP_NAME="?N";
	public static final String FONT_APP_PACKAGE_NAME="?P";
	public static final String FONT_APP_VERSIONCODE="?C";
	public static final String FONT_APP_VERSIONNAME="?V";
	
	public static final String PREFERENCE_FILENAME_FONT_DEFAULT=FONT_APP_PACKAGE_NAME+"-"+FONT_APP_VERSIONCODE;
	
}
