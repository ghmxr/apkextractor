package com.github.ghmxr.apkextractor.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.github.ghmxr.apkextractor.activities.BaseActivity;
import com.github.ghmxr.apkextractor.activities.Main;
import com.github.ghmxr.apkextractor.data.AppItemInfo;

import android.os.Message;

/**
 * 拷贝指定AppItemInfo的内容至指定path
 * @author MXR  mxremail@qq.com  https://github.com/ghmxr/apkextractor
 */

public class CopyFilesTask implements Runnable{
			
	public List<AppItemInfo> applist;
	private String savepath=BaseActivity.savepath,currentWritePath="";
	private boolean isInterrupted=false;
	private long progress=0,total=0;
	private long zipTime=0;
	private long zipWriteLength_second=0;
	/**
	 * 导出指定AppItemInfo的apk或者包含数据包的zip至savepath
	 * @param list 要导出的APK的list
	 * @param savepath  导出保存位置
	 */
	public CopyFilesTask(List<AppItemInfo> list,String savepath){
		applist=list;
		this.savepath=savepath;
		this.isInterrupted=false;
		File initialpath=new File(this.savepath);
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
		total=getTotalLenth();
		long bytetemp=0;		
		long bytesPerSecond=0;
		long startTime=System.currentTimeMillis();
		for(int i=0;i<this.applist.size();i++){
			AppItemInfo item=applist.get(i);
			if(!this.isInterrupted){
				Message msg_currentapp=new Message();
				msg_currentapp.what=Main.MESSAGE_COPYFILE_CURRENTAPP;
				msg_currentapp.obj=Integer.valueOf(i);
				Main.sendMessage(msg_currentapp);
				if((!item.exportData)&&(!item.exportObb)){
					int byteread=0;															
					try{
						String writepath=this.savepath+"/"+item.getPackageName()+"-"+item.getVersionCode()+".apk";					
						InputStream in = new FileInputStream(item.getResourcePath()); //读入原文件   
						BufferedOutputStream out= new BufferedOutputStream(new FileOutputStream(writepath));					
						this.currentWritePath=writepath;
						
						Message msg_currentfile = new Message();				        	       
				        msg_currentfile.what=BaseActivity.MESSAGE_COPYFILE_CURRENTFILE;
				        msg_currentfile.obj="正在复制到 "+writepath;
				        BaseActivity.sendMessage(msg_currentfile);
						
				        byte[] buffer = new byte[1024*10];   				       				        			        
				        while ( (byteread = in.read(buffer)) != -1&&!this.isInterrupted) { 		        	 
				             out.write(buffer, 0, byteread);					             
				             progress += byteread;				            
				             bytesPerSecond+=byteread;
				             long endTime=System.currentTimeMillis();		             
				             if((endTime-startTime)>1000){
				            	 startTime=endTime;
				            	 Long speed=Long.valueOf(bytesPerSecond);
				            	 bytesPerSecond=0;
				            	 Message msg_speed = new Message();				            	 		            	 
				            	 msg_speed.what=BaseActivity.MESSAGE_COPYFILE_REFRESH_SPEED;
				            	 msg_speed.obj=speed;
				            	 BaseActivity.sendMessage(msg_speed);
				            	
				             }
				             
				             if((progress-bytetemp)>100*1024){   //每写100K发送一次更新进度的Message
				            	 bytetemp=progress;
				            	 Message msg_progress=new Message();
				            	 Long progressinfo[]  = new Long[]{Long.valueOf(progress),Long.valueOf(total)};			            	
				            	 msg_progress.what=BaseActivity.MESSAGE_COPYFILE_REFRESH_PROGRESS;
				            	 msg_progress.obj=progressinfo;
				            	 BaseActivity.sendMessage(msg_progress);
				             }
				            
				            
				         } 
				        out.flush();
				        in.close();
				        out.close();				      		        		        
					}
					catch(FileNotFoundException fe){
						fe.printStackTrace();
						progress+=item.getPackageSize();
						Message msg_filenotfound_exception = new Message();
						String filename = item.getAppName()+item.getVersion();
						msg_filenotfound_exception.what=BaseActivity.MESSAGE_COPYFILE_FILE_NOTFOUND_EXCEPTION;
						msg_filenotfound_exception.obj=filename+"\nError Message:"+fe.toString();
						BaseActivity.sendMessage(msg_filenotfound_exception);
					}
					
					catch(IOException e){
						e.printStackTrace();				
						BaseActivity.sendEmptyMessage(BaseActivity.MESSAGE_COPYFILE_IOEXCEPTION);
					}
					catch(Exception e){
						e.printStackTrace();
					}
								
					
				}else {
					try{
						String writePath=this.savepath+"/"+item.getPackageName()+"-"+item.getVersionCode()+".zip";
						this.currentWritePath=writePath;
						ZipOutputStream zos=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(writePath))));
						zos.setComment("Packaged by com.github.ghmxr.apkextractor \nhttps://github.com/ghmxr/apkextractor");
						writeZip(new File(item.getResourcePath()),"",zos);
						if(item.exportData){
							writeZip(new File(StorageUtil.getSDPath()+"/android/data/"+item.packageName),"Android/data/",zos);
						}
						if(item.exportObb){
							writeZip(new File(StorageUtil.getSDPath()+"/android/obb/"+item.packageName),"Android/obb/",zos);
						}
						zos.flush();
						zos.close();
					}catch(Exception e){
						e.printStackTrace();
						Message msg_filenotfound_exception = new Message();
						String filename = item.getAppName()+item.getVersion();
						msg_filenotfound_exception.what=BaseActivity.MESSAGE_COPYFILE_FILE_NOTFOUND_EXCEPTION;
						msg_filenotfound_exception.obj=filename+"\n"+"Error Message:"+e.toString();
						BaseActivity.sendMessage(msg_filenotfound_exception);
					}
					if(isInterrupted) new File(this.currentWritePath).delete();
				}
				
			}
			
			else{
				break;
			}
												
		}
		
		if(!this.isInterrupted)
		BaseActivity.sendEmptyMessage(BaseActivity.MESSAGE_COPYFILE_COMPLETE);
	
	}
	
	private void writeZip(File file,String parent,ZipOutputStream zos) {
		if(file==null||parent==null||zos==null) return;
		if(isInterrupted) return;
		if(file.exists()){
			if(file.isDirectory()){
				parent+=file.getName()+File.separator;
				File files[]=file.listFiles();	
				if(files.length>0){
					for(File f:files){
						writeZip(f,parent,zos);
					}
				}else{
					try{
						zos.putNextEntry(new ZipEntry(parent));
					}catch(IOException e){
						e.printStackTrace();
					}
				}
			}else{
				try{					
					FileInputStream in=new FileInputStream(file);
					ZipEntry zipextry=new ZipEntry(parent+file.getName());
					zos.putNextEntry(zipextry);
					byte[] buffer=new byte[1024];
					int length;
					long progressCheck=this.progress;
					
					Message msg_currentfile = new Message();			       		       
			        msg_currentfile.what=BaseActivity.MESSAGE_COPYFILE_CURRENTFILE;
			        String currentPath=file.getAbsolutePath();
			        if(currentPath.length()>50) currentPath="..."+currentPath.substring(currentPath.length()-50,currentPath.length());
			        msg_currentfile.obj="正在压缩 "+currentPath;
			        BaseActivity.sendMessage(msg_currentfile);
					
					while((length=in.read(buffer))!=-1&&!isInterrupted){
						zos.write(buffer,0,length);
						this.progress+=length;
						this.zipWriteLength_second+=length;
						Long endTime=System.currentTimeMillis();
						if(endTime-this.zipTime>1000){
							this.zipTime=endTime;
							Message msg_speed=new Message();
							msg_speed.what=BaseActivity.MESSAGE_COPYFILE_REFRESH_SPEED;
			            	 msg_speed.obj=this.zipWriteLength_second;
			            	 BaseActivity.sendMessage(msg_speed);
			            	 this.zipWriteLength_second=0;
						}
						if(this.progress-progressCheck>100*1024){
							progressCheck=this.progress;
							Message msg=new Message();
							msg.what=Main.MESSAGE_COPYFILE_REFRESH_PROGRESS;
							msg.obj=new Long[]{this.progress,this.total};
							BaseActivity.sendMessage(msg);
						}
						
					}					
					zos.flush();
					in.close();					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	private long getTotalLenth(){
		long total=0;
		for(AppItemInfo item:applist){
			total+=item.appsize;
			if(item.exportData){
				total+=FileSize.getFileOrFolderSize(new File(StorageUtil.getSDPath()+"/android/data/"+item.packageName));
			}
			if(item.exportObb){
				total+=FileSize.getFileOrFolderSize(new File(StorageUtil.getSDPath()+"/android/obb/"+item.packageName));
			}
		}
		return total;
	}
	
	public void setInterrupted(){
		this.isInterrupted=true;
		File file = new File(this.currentWritePath);
		if(file.exists()&&!file.isDirectory()){
			file.delete();
		}
	}
}
