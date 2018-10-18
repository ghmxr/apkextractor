package com.github.ghmxr.apkextractor.utils;

import java.io.File;
import java.util.List;

import com.github.ghmxr.apkextractor.activities.BaseActivity;
import com.github.ghmxr.apkextractor.data.AppItemInfo;

public class APKChecker {
	
	private List <AppItemInfo> applist;
	private String existResult,checkPath,absendResult;
	private boolean isAPKAlreadyExist,isAPKAbsent;
	private boolean isSelected[];
	private boolean[] ifneedExtract;
	
	
	public APKChecker(List<AppItemInfo> applist,boolean[] isSelected){
		this.checkPath=BaseActivity.SAVEPATH+"/";
		this.applist=applist;
		this.existResult="";
		this.absendResult="";
		this.isAPKAlreadyExist=false;
		this.isAPKAbsent=false;
		this.isSelected=isSelected;
		this.ifneedExtract=new boolean[this.applist.size()];
		
	}
	
	public APKChecker startCheck(){
		for(int i=0;i<this.applist.size();i++){
			if(isSelected[i]){
				String checkpath=this.checkPath+this.applist.get(i).getPackageName()+"-"+this.applist.get(i).getVersionCode()+".apk";
				File file = new File(checkpath);
				if(file.exists()&&!file.isDirectory()){
					this.isAPKAlreadyExist=true;
					this.existResult+=checkpath+"\n\n";
				}
				else{
					this.isAPKAbsent=true;
					this.ifneedExtract[i]=true;
					this.absendResult+=checkpath+"\n\n";
				}
			}
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
	
	public boolean[] getIfNeedExtract(){
		return this.ifneedExtract;
	}
	
	public String getAbsentAPKInfo(){
		return this.absendResult;
	}

}
