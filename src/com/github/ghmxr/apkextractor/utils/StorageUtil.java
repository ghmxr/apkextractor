package com.github.ghmxr.apkextractor.utils;

import java.io.File;

import android.os.Environment;
import android.os.StatFs;

public class StorageUtil {
	
	/**
	 * 获取SD卡剩余空间的字节数
	 * 
	 */
	public static long getSDAvaliableSize(){			
		if(Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
			 File path = Environment.getExternalStorageDirectory();  
		       StatFs stat = new StatFs(path.getPath());  
		       long blockSize = stat.getBlockSize();  
		       long availableBlocks = stat.getAvailableBlocks();   
		       return  blockSize * availableBlocks;
		}
		else{
			return 0;
		}		  
	}
	
	
	/**
	 * 获取SD卡或者模拟SD卡路径
	 */
	public static String getSDPath(){ 	   	   
	  if(Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){                               
	       return Environment.getExternalStorageDirectory().toString();//获取跟目录
	  }   
	    return ""; 
	}

}
