package com.github.ghmxr.apkextractor.utils;

import java.io.File;
import java.util.List;

import com.github.ghmxr.apkextractor.activities.BaseActivity;
import com.github.ghmxr.apkextractor.data.AppItemInfo;

public class FileChecker {
	
	private List <AppItemInfo> applist;
	private String existResult,checkPath,absendResult;
	private boolean isAPKAlreadyExist,isAPKAbsent;
	//private boolean isSelected[];
	//private boolean[] ifneedExtract;
	String mask;
	
	public FileChecker(List<AppItemInfo> applist,String mask){
		this.checkPath=BaseActivity.savepath+"/";
		this.applist=applist;
		this.existResult="";
		this.absendResult="";
		this.isAPKAlreadyExist=false;
		this.isAPKAbsent=false;
		//this.isSelected=isSelected;
		//this.ifneedExtract=new boolean[this.applist.size()];
		this.mask=mask;
	}
	
	public FileChecker startCheck(){
		if(applist==null||mask==null) return this;
		for(int i=0;i<this.applist.size();i++){
			//for(String mask:masks){
				String checkpath=this.checkPath+this.applist.get(i).getPackageName()+"-"+this.applist.get(i).getVersionCode()+"."+mask;
				File file = new File(checkpath);
				if(file.exists()&&!file.isDirectory()){
					this.isAPKAlreadyExist=true;
					//Log.e("FileChecker", "has apk exists");
					this.existResult+=checkpath+"\n\n";
				}
				else{
					this.isAPKAbsent=true;
					//this.ifneedExtract[i]=true;
					this.absendResult+=checkpath+"\n\n";
				}
			//}
		}
		return this;
	}
	
	public boolean getIsApkAlreadyExist(){
		return this.isAPKAlreadyExist;
	}
	
	public String getDuplicatedAPKInfo(){
		return this.existResult;
	}
	
	public boolean getIsApkAbsent(){
		return this.isAPKAbsent;
	}
	
	/*public boolean[] getIf1NeedExtract(){
		return this.ifneedExtract;
	}*/
	
	public String getAbsentAPKInfo(){
		return this.absendResult;
	}

}
