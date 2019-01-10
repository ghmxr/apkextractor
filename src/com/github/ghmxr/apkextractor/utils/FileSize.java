package com.github.ghmxr.apkextractor.utils;

import java.io.File;

public class FileSize {
		
	/**
	 * 获取指定文件或文件夹大小	
	 */	
	public static long getFileOrFolderSize(File file){		
		try{
			if(file==null) return 0;
			if(!file.exists()) return 0;
			if(!file.isDirectory()) return file.length();
			else{
				long total=0;
				File [] files=file.listFiles();
				if(files==null||files.length==0) return 0;
				for(File f:files){
					total+=getFileOrFolderSize(f);
				}
				return total;
			}
		}catch(Exception e){e.printStackTrace();}
		return 0;				
	}
	
	
	public static long getFileSize(String filepath){
		return filepath!=null?getFileOrFolderSize(new File(filepath)):0;
	}
			
	public static long getFilesSize(String [] filepaths){		
		if(filepaths==null||filepaths.length==0){
			return 0;
		}
		else{
			long total=0;
			for(int i=0;i<filepaths.length;i++){
				total+=getFileSize(filepaths[i]);
			}
			return total;
		}
	}
	
}

