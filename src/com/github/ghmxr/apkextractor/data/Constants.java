package com.github.ghmxr.apkextractor.data;

import com.github.ghmxr.apkextractor.utils.StorageUtil;

public class Constants {

	public static final String PREFERENCE_NAME="settings";
	public static final String PREFERENCE_SAVE_PATH="savepath";
	public static final String PREFERENCE_SAVE_PATH_DEFAULT=StorageUtil.getMainStoragePath()+"/Backup";
	public static final String PREFERENCE_FILENAME_FONT_APK="font_apk";
	public static final String PREFERENCE_FILENAME_FONT_ZIP="font_zip";
		
	public static final String FONT_APP_NAME="?N";
	public static final String FONT_APP_PACKAGE_NAME="?P";
	public static final String FONT_APP_VERSIONCODE="?C";
	public static final String FONT_APP_VERSIONNAME="?V";
	
	public static final String PREFERENCE_FILENAME_FONT_DEFAULT=FONT_APP_PACKAGE_NAME+"-"+FONT_APP_VERSIONCODE;
	
}
