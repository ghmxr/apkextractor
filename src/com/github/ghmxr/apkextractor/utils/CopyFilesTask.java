package com.github.ghmxr.apkextractor.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.github.ghmxr.apkextractor.activities.BaseActivity;
import com.github.ghmxr.apkextractor.data.AppItemInfo;

import android.os.Message;

public class CopyFilesTask implements Runnable{
			
	private InputStream in;
	private FileOutputStream out;
	private boolean[] isSelected;
	private List<AppItemInfo> applist;
	private String savepath=BaseActivity.SAVEPATH,currentWritePath="";
	private boolean isInterrupted=false;
	
	
	public CopyFilesTask(List<AppItemInfo> applist,boolean [] isSelected){
		this.isSelected=isSelected;
		this.applist=applist;
		this.isInterrupted=false;
		this.savepath=BaseActivity.SAVEPATH;
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
		long bytesum=0;
		long bytetemp=0;
		int filenum=0;
		long bytesPerSecond=0;
		long startTime=System.currentTimeMillis();
		for(int i=0;i<this.applist.size();i++){
			
			if(!this.isInterrupted){
				if(isSelected[i]){
					int byteread=0;															
					try{
						String writepath=this.savepath+"/"+this.applist.get(i).getPackageName()+"-"+this.applist.get(i).getVersionCode()+".apk";
						this.in = new FileInputStream(this.applist.get(i).getResourcePath()); //读入原文件   
						this.out= new FileOutputStream(writepath);
						this.currentWritePath=writepath;
				        byte[] buffer = new byte[1024];   
				        filenum++;
				        Message msg_currentfile = new Message();
				        Integer[] currentmsg=new Integer[3];
				        currentmsg[0]=filenum;
				        currentmsg[1]=i;
				        currentmsg[2]=this.getCopyingFilesTotal();
				        msg_currentfile.what=BaseActivity.MESSAGE_COPYFILE_CURRENTFILE;
				        msg_currentfile.obj=currentmsg;
				        BaseActivity.sendMessage(msg_currentfile);
				        
				        while ( (byteread = this.in.read(buffer)) != -1&&!this.isInterrupted) { 		        	 
				             out.write(buffer, 0, byteread);		           
				             bytesum += byteread; 		             
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
				             
				             if((bytesum-bytetemp)>100*1024){   //每写100K发送一次更新进度的Message
				            	 bytetemp=bytesum;
				            	 Message msg_progress=new Message();
				            	 Long progressinfo  = Long.valueOf(bytesum);			            	
				            	 msg_progress.what=BaseActivity.MESSAGE_COPYFILE_REFRESH_PROGRESS;
				            	 msg_progress.obj=progressinfo;
				            	 BaseActivity.sendMessage(msg_progress);
				             }
				            
				            
				         } 
				         		        		        
					}
					catch(FileNotFoundException fe){
						fe.printStackTrace();
						bytesum+=this.applist.get(i).getPackageSize();
						Message msg_filenotfound_exception = new Message();
						String filename = this.applist.get(i).getAppName()+this.applist.get(i).getVersion();
						msg_filenotfound_exception.what=BaseActivity.MESSAGE_COPYFILE_FILE_NOTFOUND_EXCEPTION;
						msg_filenotfound_exception.obj=filename;
						BaseActivity.sendMessage(msg_filenotfound_exception);
					}
					
					catch(IOException e){
						e.printStackTrace();				
						BaseActivity.sendEmptyMessage(BaseActivity.MESSAGE_COPYFILE_IOEXCEPTION);
					}
					
					finally{
						if(this.in!=null){
							try{
								this.in.close();
							}catch(IOException e){
								e.printStackTrace();
							}
							this.in=null;
						}
						if(this.out!=null){
							try{
								this.out.close();
							}catch(IOException e){
								e.printStackTrace();
							}
							this.out=null;
						}
						
					}
					
				}
				
			}
			
			else{
				break;
			}
												
		}
		
		if(!this.isInterrupted)
		BaseActivity.sendEmptyMessage(BaseActivity.MESSAGE_COPYFILE_COMPLETE);
	
	}
	
	public void setInterrupted(){
		this.isInterrupted=true;
		File file = new File(this.currentWritePath);
		if(file.exists()&&!file.isDirectory()){
			file.delete();
		}
	}
	
	private int getCopyingFilesTotal(){
		if(this.isSelected==null){
			return 0;
		}
		else{
			int total=0;
			for (int i=0;i<this.applist.size();i++){
				if(this.isSelected[i]){
					total++;
				}
			}
			return total;
		}
	}
	
	
	
}
