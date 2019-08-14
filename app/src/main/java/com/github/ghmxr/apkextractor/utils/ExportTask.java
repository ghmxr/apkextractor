package com.github.ghmxr.apkextractor.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.ghmxr.apkextractor.AppItem;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.data.Constants;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportTask extends Thread {

    private Context context;
    private final List<AppItem> list;
    private ExportProgressListener listener;

    private boolean isInterrupted=false;
    private long progress=0,total=0;
    private long progress_check_zip =0;
    private long zipTime=0;
    private long zipWriteLength_second=0;

    private String currentWritePath=null;

    private final ArrayList<String>write_paths=new ArrayList<>();
    private final StringBuilder error_message=new StringBuilder();

    /**
     * 导出任务构造方法
     * @param list 要导出的AppItem集合
     * @param callback 任务进度回调，在主UI线程
     */
    public ExportTask(@NonNull Context context, @NonNull List<AppItem>list,@Nullable ExportProgressListener callback){
        super();
        this.context=context;
        this.list=list;
        this.listener=callback;
    }

    public void setExportProgressListener(ExportProgressListener listener){
        this.listener=listener;
    }

    @Override
    public void run() {
        try{
            //初始化导出路径
            File export_path=new File(Global.getSavePath(context));
            if(export_path.exists()&&!export_path.isDirectory()){
                export_path.delete();
            }
            if(!export_path.exists()){
                export_path.mkdirs();
            }
        }catch (Exception e){
            e.printStackTrace();
            if(listener!=null)listener.onExportTaskFinished(new ArrayList<String>(),e.toString());
            return;
        }

        total= getTotalLength();
        long progress_check_apk=0;
        long bytesPerSecond=0;
        long startTime=System.currentTimeMillis();

        for(int i=0;i<list.size();i++){
            if(isInterrupted)return;
            try{
                final AppItem item=list.get(i);
                final int order_this_loop=i+1;

                if(!item.exportData&&!item.exportObb){

                    this.currentWritePath=Global.getAbsoluteWritePath(context,item,"apk");
                    postCallback2Listener(new Runnable() {
                        @Override
                        public void run() {
                            if(listener!=null)listener.onExportAppItemStarted(order_this_loop,item,list.size(),currentWritePath);
                        }
                    });


                    InputStream in = new FileInputStream(String.valueOf(item.getSourcePath())); //读入原文件
                    BufferedOutputStream out= new BufferedOutputStream(new FileOutputStream(this.currentWritePath));

                    int byteread;
                    byte[] buffer = new byte[1024*10];
                    while ( (byteread = in.read(buffer)) != -1&&!this.isInterrupted) {
                        out.write(buffer, 0, byteread);
                        progress += byteread;
                        bytesPerSecond+=byteread;
                        long endTime=System.currentTimeMillis();
                        if((endTime-startTime)>1000){
                            startTime=endTime;
                            final long speed=bytesPerSecond;
                            bytesPerSecond=0;
                            /*Long speed=Long.valueOf(bytesPerSecond);
				            	 bytesPerSecond=0;
				            	 Message msg_speed = new Message();
				            	 msg_speed.what=BaseActivity.MESSAGE_COPYFILE_REFRESH_SPEED;
				            	 msg_speed.obj=speed;
				            	 BaseActivity.sendMessage(msg_speed);*/
                            postCallback2Listener(new Runnable() {
                                @Override
                                public void run() {
                                    if(listener!=null)listener.onExportSpeedUpdated(speed);
                                }
                            });

                        }

                        if((progress-progress_check_apk)>100*1024){   //每写100K发送一次更新进度的Message
                            progress_check_apk=progress;
                            /*Message msg_progress=new Message();
                            Long progressinfo[]  = new Long[]{Long.valueOf(progress),Long.valueOf(total)};
                            msg_progress.what=BaseActivity.MESSAGE_COPYFILE_REFRESH_PROGRESS;
                            msg_progress.obj=progressinfo;
                            BaseActivity.sendMessage(msg_progress);*/
                            postCallback2Listener(new Runnable() {
                                @Override
                                public void run() {
                                    if(listener!=null)listener.onExportProgressUpdated(progress,total,currentWritePath);
                                }
                            });
                        }
                    }
                    out.flush();
                    in.close();
                    out.close();
                    write_paths.add(this.currentWritePath);
                }

                else{
                    this.currentWritePath=Global.getAbsoluteWritePath(context,item,"zip");
                    postCallback2Listener(new Runnable() {
                        @Override
                        public void run() {
                            if(listener!=null)listener.onExportAppItemStarted(order_this_loop,item,list.size(),currentWritePath);
                        }
                    });

                    ZipOutputStream zos=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(this.currentWritePath))));
                    zos.setComment("Packaged by com.github.ghmxr.apkextractor \nhttps://github.com/ghmxr/apkextractor");
                    int zip_level=context.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE).getInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT);

                    if(zip_level>=0&&zip_level<=9) zos.setLevel(zip_level);

                    writeZip(new File(String.valueOf(item.getSourcePath())),"",zos,zip_level);
                    if(item.exportData){
                        writeZip(new File(Storage.getMainExternalStoragePath()+"/android/data/"+item.getPackageName()),"Android/data/",zos,zip_level);
                    }
                    if(item.exportObb){
                        writeZip(new File(Storage.getMainExternalStoragePath()+"/android/obb/"+item.getPackageName()),"Android/obb/",zos,zip_level);
                    }
                    zos.flush();
                    zos.close();
                    write_paths.add(currentWritePath);
                }


            }catch (Exception e){
                this.error_message.append(e.toString());
                this.error_message.append("\n\n");
            }

        }

        if(isInterrupted){
            try{
                File file = new File(this.currentWritePath);
                if(file.exists()&&!file.isDirectory()){
                    file.delete();
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        postCallback2Listener(new Runnable() {
            @Override
            public void run() {
                if(listener!=null&&!isInterrupted)listener.onExportTaskFinished(write_paths,error_message.toString());
            }
        });

    }


    private void postCallback2Listener(Runnable runnable){
        if(listener==null)return;
        Global.handler.post(runnable);
    }


    /**
     * 获取本次导出的总计长度
     * @return 总长度，字节
     */
    private long getTotalLength(){
        long total=0;
        for(AppItem item:list){
            total+=item.getSize();
            if(item.exportData){
                total+=FileUtil.getFileOrFolderSize(new File(Storage.getMainExternalStoragePath()+"/android/data/"+item.getPackageName()));
            }
            if(item.exportObb){
                total+=FileUtil.getFileOrFolderSize(new File(Storage.getMainExternalStoragePath()+"/android/obb/"+item.getPackageName()));
            }
        }
        return total;
    }

    /**
     * 将本Runnable停止，删除当前正在导出而未完成的文件，使线程返回
     */
    public void setInterrupted(){
        this.isInterrupted=true;
    }

    private void writeZip(final File file, String parent, ZipOutputStream zos, final int zip_level) {
        if(file==null||parent==null||zos==null) return;
        if(isInterrupted) return;
        if(file.exists()){
            if(file.isDirectory()){
                parent+=file.getName()+File.separator;
                File [] files=file.listFiles();
                if(files.length>0){
                    for(File f:files){
                        writeZip(f,parent,zos,zip_level);
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
                    ZipEntry zipentry=new ZipEntry(parent+file.getName());

                    if(zip_level== Constants.ZIP_LEVEL_STORED){
                        zipentry.setMethod(ZipOutputStream.STORED);
                        zipentry.setCompressedSize(file.length());
                        zipentry.setSize(file.length());
                        zipentry.setCrc(FileUtil.getCRC32FromFile(file).getValue());
                    }

                    zos.putNextEntry(zipentry);
                    byte[] buffer=new byte[1024];
                    int length;

                    /*Message msg_currentfile = new Message();
                    msg_currentfile.what=BaseActivity.MESSAGE_COPYFILE_CURRENTFILE;
                    String currentPath=file.getAbsolutePath();
                    if(currentPath.length()>90) currentPath="..."+currentPath.substring(currentPath.length()-90,currentPath.length());
                    msg_currentfile.obj=context.getResources().getString(R.string.copytask_zip_current)+currentPath;
                    BaseActivity.sendMessage(msg_currentfile);*/
                    postCallback2Listener(new Runnable() {
                        @Override
                        public void run() {
                            if(listener!=null)listener.onExportZipProgressUpdated(file.getAbsolutePath());
                        }
                    });

                    while((length=in.read(buffer))!=-1&&!isInterrupted){
                        zos.write(buffer,0,length);
                        this.progress+=length;
                        this.zipWriteLength_second+=length;
                        Long endTime=System.currentTimeMillis();
                        if(endTime-this.zipTime>1000){
                            this.zipTime=endTime;
                            /*Message msg_speed=new Message();
                            msg_speed.what=BaseActivity.MESSAGE_COPYFILE_REFRESH_SPEED;
                            msg_speed.obj=this.zipWriteLength_second;
                            BaseActivity.sendMessage(msg_speed);*/
                            final long zip_speed=zipWriteLength_second;
                            postCallback2Listener(new Runnable() {
                                @Override
                                public void run() {
                                    if(listener!=null)listener.onExportSpeedUpdated(zip_speed);
                                }
                            });
                            this.zipWriteLength_second=0;
                        }
                        if(this.progress- progress_check_zip >100*1024){
                            progress_check_zip =this.progress;
                            /*Message msg=new Message();
                            msg.what=Main.MESSAGE_COPYFILE_REFRESH_PROGRESS;
                            msg.obj=new Long[]{this.progress,this.total};
                            BaseActivity.sendMessage(msg);*/
                            postCallback2Listener(new Runnable() {
                                @Override
                                public void run() {
                                    if(listener!=null)listener.onExportProgressUpdated(progress,total,file.getAbsolutePath());
                                }
                            });
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



    public interface ExportProgressListener{
        void onExportAppItemStarted(int order, AppItem item, int total, String write_path);
        void onExportProgressUpdated(long current, long total, String write_path);
        void onExportZipProgressUpdated(String write_path);
        void onExportSpeedUpdated(long speed);
        //void onAppItemFinished(int order,AppItem item,int total);
        void onExportTaskFinished(List<String>write_paths, String error_message);
    }
}
