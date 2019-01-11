package com.github.ghmxr.apkextractor.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;


public class StorageUtil {
	
	/**
	 * 获取SD卡剩余空间的字节数
	 * 
	 */
	@SuppressLint("NewApi")
	public static long getMainStorageAvaliableSize(){
		try{
			if(Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
				 File path = Environment.getExternalStorageDirectory();  
			       StatFs stat = new StatFs(path.getPath());
			       int version=Build.VERSION.SDK_INT;
			       long blockSize = version>=18?stat.getBlockSizeLong():stat.getBlockSize();  
			       long availableBlocks = version>=18?stat.getAvailableBlocksLong():stat.getAvailableBlocks();   
			       return  blockSize * availableBlocks;
			}			
		}catch(Exception e){e.printStackTrace();}
		return 0;
	}
	
	
	/**
	 * 获取SD卡或者模拟SD卡路径
	 */
	public static String getMainStoragePath(){ 
		try{
			if(Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){                               
			       return Environment.getExternalStorageDirectory().toString();
			  }  
		}catch(Exception e){e.printStackTrace();}	   
	    return ""; 
	}
	
	/**
	 * get all available storage paths on the device.
	 */
	public static List<String> getAvailableStoragePaths(){
		try{
			List<String> paths=new ArrayList<String>();
			String mainStorage=getMainStoragePath().toLowerCase(Locale.getDefault()).trim();
			try{
				paths.add(mainStorage);
			}catch(Exception e){}
			
			Runtime runtime=Runtime.getRuntime();
			Process process=runtime.exec("mount");
			BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while((line=reader.readLine())!=null){
				//Log.d("", line);
				if (line.contains("proc") || line.contains("tmpfs") || line.contains("media") || line.contains("asec") || line.contains("secure") || line.contains("system") || line.contains("cache")
						|| line.contains("sys") || line.contains("data") || line.contains("shell") || line.contains("root") || line.contains("acct") || line.contains("misc") || line.contains("obb")){
					continue;
				}
				if (line.contains("fat") || line.contains("fuse") || (line.contains("ntfs"))){
					
					String items[] = line.split(" ");
					if (items != null && items.length > 1){
						String path = items[1].toLowerCase(Locale.getDefault());						
						if(!path.toLowerCase(Locale.getDefault()).trim().equals(mainStorage)) paths.add(path);					
					}
				}
				

			}
			Log.d("StoragePaths", Arrays.toString(paths.toArray()));
			return paths;
		}catch(Exception e){e.printStackTrace();}
		return new ArrayList<String>();
	}
	
	@SuppressLint("NewApi")
	public static long getAvaliableSizeOfPath(String path){
		try{
			StatFs stat = new StatFs(path);
			int version=Build.VERSION.SDK_INT;
		    long blockSize = version>=18?stat.getBlockSizeLong():stat.getBlockSize();  
		    long availableBlocks = version>=18?stat.getAvailableBlocksLong():stat.getAvailableBlocks();   
		    return  blockSize * availableBlocks;
		}catch(Exception e){e.printStackTrace();}
		return 0;
	}

}
