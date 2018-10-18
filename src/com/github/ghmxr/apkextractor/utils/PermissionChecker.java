package com.github.ghmxr.apkextractor.utils;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.activities.BaseActivity;
import com.github.ghmxr.apkextractor.activities.Main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;


public class PermissionChecker {
	
	public static boolean isHaveRWPermissions(Context context){		
		if((ActivityCompat.checkSelfPermission(context, "android.permission.WRITE_EXTERNAL_STORAGE")!=PackageManager.PERMISSION_GRANTED
		||ActivityCompat.checkSelfPermission(context, "android.permission.READ_EXTERNAL_STORAGE")!=PackageManager.PERMISSION_GRANTED)
		&&Build.VERSION.SDK_INT>=23){
			return false;							
		}
		else{
			return true;
		}
	}
	
	public static void requestRWPermissions(final Activity activity){
		String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE","android.permission.WRITE_EXTERNAL_STORAGE" };
		SharedPreferences settings =activity.getSharedPreferences("settings", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		if(!isHaveRWPermissions(activity)&&!settings.getBoolean(BaseActivity.PREFERENCE_IF_REQUESTED_RWPERMISSIONS, false)){
			ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,1);
			editor.putBoolean(BaseActivity.PREFERENCE_IF_REQUESTED_RWPERMISSIONS, true);
			editor.commit();
			new ActivityCompat.OnRequestPermissionsResultCallback() {
				
				@Override
				public void onRequestPermissionsResult(int arg0, String[] arg1, int[] arg2) {
					// TODO Auto-generated method stub
					boolean isGranted=true;
					for(int i=0;i<arg2.length;i++){
						if(arg2[i]!=PackageManager.PERMISSION_GRANTED){
							isGranted=false;
						}
					}
										
					if(!isGranted){
						Main.sendEmptyMessage(Main.MESSAGE_PERMISSION_DENIED);
						//Toast.makeText(Main.this, "未获取到读写权限，请检查本应用权限", Toast.LENGTH_LONG).show();
					}
					else{
						Main.sendEmptyMessage(Main.MESSAGE_PERMISSION_GRANTED);
					}
				}
			};
			
		}
		else{
			new AlertDialog.Builder(activity)
			.setTitle("未获得读写权限")
			.setIcon(R.drawable.ic_warn)
			.setMessage("未获取到读写权限，请检查本应用权限然后重试。")
			.setCancelable(true)
			.setPositiveButton("前往设置", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Intent appdetail = new Intent();  
					appdetail.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);  
					appdetail.setData(Uri.fromParts("package", activity.getApplicationContext().getPackageName(), null));   
					activity.startActivity(appdetail); 
					//Main.this.finish();
					
				}
			})			
			.show();
		}
		
	}

}
