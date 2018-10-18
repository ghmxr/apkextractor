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
	       File sdDir = null; 
	       boolean sdCardExist = Environment.getExternalStorageState()   
	       .equals(android.os.Environment.MEDIA_MOUNTED);//判断sd卡是否存在
	       if(sdCardExist)   
	       {                               
	         sdDir = Environment.getExternalStorageDirectory();//获取跟目录
	      }   
	       return sdDir.toString(); 
	}

}
