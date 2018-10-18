package com.github.ghmxr.apkextractor.data;

import java.io.File;
import java.util.Locale;

public class FileItemInfo implements Comparable<FileItemInfo>{
	
	/**
	 * 排序控制，0=默认,1=名称升序（A到Z）,2=名称降序（Z到A）
	 */
	public static int SortConfig=1;
	
	
	
	public File file;
	
	
	public FileItemInfo(File file){
		this.file=file;
	}
			
	@Override
	public int compareTo(FileItemInfo o) {
		// TODO Auto-generated method stub
		int returnvalue=0;
		switch(SortConfig){
		default:break;
		case 1:returnvalue= this.file.getName().trim().toLowerCase(Locale.ENGLISH).compareTo(o.file.getName().trim().toLowerCase(Locale.ENGLISH));break;
		case 2:returnvalue=0-this.file.getName().trim().toLowerCase(Locale.ENGLISH).compareTo(o.file.getName().trim().toLowerCase(Locale.ENGLISH));break;		
		}
		return returnvalue;
	}

}
