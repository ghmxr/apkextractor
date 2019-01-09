package com.github.ghmxr.apkextractor.activities;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.data.AppItemInfo;
import com.github.ghmxr.apkextractor.data.Constants;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.view.WindowManager;

public abstract class BaseActivity extends Activity {
	
	public static List <AppItemInfo>  listsum=new ArrayList<AppItemInfo>();
	public static List <AppItemInfo> listsearch=new ArrayList<AppItemInfo>();
	
	public static LinkedList<BaseActivity> queue = new LinkedList<BaseActivity>();
	
	public SharedPreferences settings;
	public SharedPreferences.Editor editor;
		
	/**
	 * @deprecated
	 */
	public static final String PREFERENCE_IF_REQUESTED_RWPERMISSIONS="ifrequestedrw";
	/**
	 * @deprecated
	 */
	public static final String PREFERENCE_IF_EDITED_SAVEPATH="ifeditedsavepath";
	public boolean isatFront=false;
	
	public static final int MESSAGE_COPYFILE_COMPLETE         							= 0x0001;
	public static final int MESSAGE_COPYFILE_INTERRUPT        							= 0x0002;
	public static final int MESSAGE_COPYFILE_FILE_NOTFOUND_EXCEPTION  					= 0x0003;
	
	public static final int MESSAGE_STORAGE_NOTENOUGH        							= 0x0004;
	
	public static final int MESSAGE_COPYFILE_REFRESH_SPEED            					= 0x0005;
	public static final int MESSAGE_COPYFILE_REFRESH_PROGRESS         					= 0x0006;
	
	public static final int MESSAGE_COPYFILE_IOEXCEPTION              					= 0x0007;
	
	public static final int MESSAGE_COPYFILE_CURRENTFILE 								= 0x0008;
	public static final int MESSAGE_COPYFILE_CURRENTAPP									= 0x0009;
				
	public static final int MESSAGE_LOADLIST_REFRESH_PROGRESS							= 0x0010;
	public static final int MESSAGE_LOADLIST_COMPLETE									= 0x0011;
	
	public static  String savepath=Constants.PREFERENCE_SAVE_PATH_DEFAULT;
	
	public static String storage_path=StorageUtil.getMainStoragePath();
	
	public static Handler handler = new Handler(){
		public void handleMessage(Message msg){
			if(queue.size()>0){
				queue.getLast().processMessage(msg);
			}
		}
	};
	
	@SuppressLint("NewApi")
	public void onCreate(Bundle mybundle){
		super.onCreate(mybundle);
		if(!queue.contains(this)){
			queue.add(this);
		}
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
			Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor(this.getResources().getString(R.color.color_actionbar)));
		}
		
		settings=this.getSharedPreferences(Constants.PREFERENCE_NAME, Activity.MODE_PRIVATE);
		editor=this.settings.edit();
		
		savepath=settings.getString(Constants.PREFERENCE_SAVE_PATH, Constants.PREFERENCE_SAVE_PATH_DEFAULT);
	}
	
	public void onResume(){
		super.onResume();
		this.isatFront=true;
	}
	
	public void onPause(){
		super.onPause();
		this.isatFront=false;
	}
	
	public static void sendEmptyMessage(int what){
		handler.sendEmptyMessage(what);
	}
	
	public static void sendMessage(Message msg){
		handler.sendMessage(msg);
	}
	
	public abstract void processMessage(Message msg);
	
	public void finish(){
		super.finish();
		if(queue.contains(this)){
			queue.remove(this);
		}
	}
	
	/**
	 * returns the absolute write path for this item
	 * @param item the AppItemInfo
	 * @param extension must be "apk" or "zip",or this method will return a blank string
	 * @return the absolute write path for this AppItemInfo
	 */
	public static String getAbsoluteWritePath(Context context,AppItemInfo item,String extension){
		try{
			SharedPreferences settings=context.getSharedPreferences(Constants.PREFERENCE_NAME, Activity.MODE_PRIVATE);
			if(extension.toLowerCase(Locale.ENGLISH).equals("apk")){
				return savepath+"/"+settings.getString(Constants.PREFERENCE_FILENAME_FONT_APK, Constants.PREFERENCE_FILENAME_FONT_DEFAULT).replace(Constants.FONT_APP_NAME, String.valueOf(item.appName))
						.replace(Constants.FONT_APP_PACKAGE_NAME, String.valueOf(item.packageName))
						.replace(Constants.FONT_APP_VERSIONCODE, String.valueOf(item.versioncode))
						.replace(Constants.FONT_APP_VERSIONNAME, String.valueOf(item.version)).toString()+".apk";
			}
			if(extension.toLowerCase(Locale.ENGLISH).equals("zip")){
				return savepath+"/"+settings.getString(Constants.PREFERENCE_FILENAME_FONT_ZIP, Constants.PREFERENCE_FILENAME_FONT_DEFAULT).replace(Constants.FONT_APP_NAME, String.valueOf(item.appName))
						.replace(Constants.FONT_APP_PACKAGE_NAME, String.valueOf(item.packageName))
						.replace(Constants.FONT_APP_VERSIONCODE, String.valueOf(item.versioncode))
						.replace(Constants.FONT_APP_VERSIONNAME, String.valueOf(item.version)).toString()+".zip";
			}
		}catch(Exception e){e.printStackTrace();}
		return "";
	}
}
