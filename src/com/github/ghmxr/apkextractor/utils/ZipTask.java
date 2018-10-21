package com.github.ghmxr.apkextractor.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.*;

import com.github.ghmxr.apkextractor.activities.BaseActivity;
import com.github.ghmxr.apkextractor.data.AppItemInfo;

public class ZipTask implements Runnable {

	private boolean isInterrupted=false;
	private final String savepath=BaseActivity.savepath;
	private List<AppItemInfo> list;
	
	public void ZipTask(List<AppItemInfo> list){
		this.list=list;
		File initialpath=new File(savepath);
		if(initialpath.exists()&&!initialpath.isDirectory()){
			initialpath.delete();
		}
		if(!initialpath.exists()){
			initialpath.mkdirs();
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(list==null) return;
		for(int i=0;i<list.size();i++){
			if(!isInterrupted){
				
				try{
					ZipOutputStream zipout = new ZipOutputStream(new FileOutputStream(savepath+"/"+list.get(i).packageName+"-"+list.get(i).getVersionCode()+".zip"));
					
		
					
				}catch(Exception e){
					e.printStackTrace();
				}
				
				
			}
		}
	}

}
