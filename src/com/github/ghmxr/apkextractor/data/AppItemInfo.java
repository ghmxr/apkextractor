package com.github.ghmxr.apkextractor.data;

import com.github.ghmxr.apkextractor.utils.PinYin;

import android.graphics.drawable.Drawable;

public class AppItemInfo implements Comparable<AppItemInfo>{ 
	public String appName="";             // 程序名
	public String packageName="";         // 程序包名 
	public Drawable icon;                 // 程序图标 
	public long appsize=0;                // 应用大小
	public String path="";	              // apk资源位置
	public String version="";             // 应用版本
	public int versioncode=0;             // 应用版本int值  
	public long lastupdatetime=0;    	  // 应用更新安装时间
	public int minsdkversion=0;		      // 应用要求的最低api版本
	//仅当构造CopyFilesTask时用
	public boolean exportData=false;
	public boolean exportObb=false;
	
	public AppItemInfo(){
		
	}
	
	public AppItemInfo(AppItemInfo item){
		this.appName=new String(item.appName);
		this.packageName=new String(item.packageName);
		this.icon=item.icon;
		this.appsize=item.appsize;
		this.path=new String(item.path);
		this.version=new String(item.version);
		this.versioncode=item.versioncode;
		this.lastupdatetime=item.lastupdatetime;
		this.minsdkversion=item.minsdkversion;
		this.exportData=false;
		this.exportObb=false;
	}
	
	// Set resources
	public void setIcon(Drawable icon) {  
        this.icon = icon;  
    }  
	
	public void setAppName(String name) {  
        this.appName = name;  
    } 
	
	public void setPackageName(String packageName) {  
        this.packageName = packageName;  
    }
	
	public void setResourcePath(String apkpath){
    	this.path=apkpath;
    }
	
	public void setPackageSize(long size){
    	this.appsize=size;
    }
	
	public void setVersion(String version){
    	this.version=version;
    }
	
	public void setVersionCode(int value){
		this.versioncode=value;
	}
	
	public void setLastUpdateTime(long millis){
		this.lastupdatetime=millis;
	}
	
	public void setMinSDKVersion(int value){
		this.minsdkversion=value;
	}
	
	
	
	
	//Get resources
	
	public Drawable getIcon() {		
		return this.icon; 		
    }  
	
    public String getAppName() {  
        return this.appName;  
    }
           
    public String getPackageName() {  
        return this.packageName;  
    }
           
    public long getPackageSize(){
    	return this.appsize;
    }
       
    public String getResourcePath(){
    	return this.path;
    }
    
    public String getVersion(){
    	return this.version;
    }
       
    public int getVersionCode(){
	   return this.versioncode;
    }
    
    public long getLastUpdateTime(){
    	return this.lastupdatetime;
    }
    
    public int getMinSDKVersion(){
    	return this.minsdkversion;
    }

	@Override
	public int compareTo(AppItemInfo o) {
		// TODO Auto-generated method stub
		int returnvalue=0;
		switch(AppItemInfoSortConfig.SortConfig){
			default:break;
			case 0:break;
			case 1:returnvalue=PinYin.getFirstSpell(this.appName).compareTo(PinYin.getFirstSpell(o.appName));break;
			case 2:returnvalue=0-PinYin.getFirstSpell(this.appName).compareTo(PinYin.getFirstSpell(o.appName));break;
			case 3:returnvalue=Long.valueOf(this.appsize).compareTo(o.appsize);break;
			case 4:returnvalue=0-Long.valueOf(this.appsize).compareTo(o.appsize);break;
			case 5:returnvalue=Long.valueOf(this.lastupdatetime).compareTo(o.lastupdatetime);break;
			case 6:returnvalue=0-Long.valueOf(this.lastupdatetime).compareTo(o.lastupdatetime);break;
		}
		return returnvalue;
		
	}
   
   
}

