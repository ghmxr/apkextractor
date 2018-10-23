package com.github.ghmxr.apkextractor.activities;

import java.util.LinkedList;
import java.util.List;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.data.AppItemInfo;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.view.WindowManager;

public abstract class BaseActivity extends Activity {
	
	public static List <AppItemInfo>  listsum,listsearch;
	
	public static LinkedList<BaseActivity> queue = new LinkedList<BaseActivity>();
	
	public SharedPreferences settings;
	public SharedPreferences.Editor editor;
	
	//public static final String PREFERENCE_FIRSTUSE_VERSION_THIS="firstuse20";
	public static final String PREFERENCE_IF_REQUESTED_RWPERMISSIONS="ifrequestedrw";
	public static final String PREFERENCE_IF_EDITED_SAVEPATH="ifeditedsavepath";
	public static final String PREFERENCE_APKPATH="savepath";
	
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
	
	public static final String UNCHANGEDPATH=StorageUtil.getSDPath()+"/Backup";
	
	public static  String savepath=UNCHANGEDPATH;
	
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
		
		this.settings=this.getSharedPreferences("settings", Activity.MODE_PRIVATE);
		this.editor=this.settings.edit();
		/*if(settings.getBoolean(PREFERENCE_FIRSTUSE_VERSION_THIS, true)){
			AlertDialog firstuseatt=new AlertDialog.Builder(this).setTitle(this.getResources().getString(R.string.dialog_firstuse_title))
					.setMessage(this.getResources().getString(R.string.dialog_firstuse_message)).setPositiveButton("È·¶¨", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					}).create();
			//firstuseatt.show();
			//editor.putBoolean(PREFERENCE_FIRSTUSE_VERSION_THIS, false);
			//editor.commit();
		}  */
		
		if(!settings.getBoolean(PREFERENCE_IF_EDITED_SAVEPATH,false)){
			savepath=UNCHANGEDPATH;
			editor.putBoolean(PREFERENCE_IF_EDITED_SAVEPATH,true);
			editor.putString(PREFERENCE_APKPATH, UNCHANGEDPATH);
			editor.commit();
		}
		else{
			savepath=settings.getString(PREFERENCE_APKPATH, UNCHANGEDPATH);
		}
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
	
	

}
