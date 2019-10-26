package com.github.ghmxr.apkextractor.tasks;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;

import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.utils.FileUtil;
import com.github.ghmxr.apkextractor.utils.OutputUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportTask extends Thread {

    private Context context;
    private final List<AppItem> list;
    private ExportProgressListener listener;
    /**
     * 本次导出任务的目的存储路径是否为外置存储
     */
    private final boolean isExternal;
    private boolean isInterrupted=false;
    private long progress=0,total=0;
    private long progress_check_zip =0;
    private long zipTime=0;
    private long zipWriteLength_second=0;

    private FileItem currentWritingFile=null;

    private final ArrayList<FileItem>write_paths=new ArrayList<>();
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
        isExternal= SPUtil.getIsSaved2ExternalStorage(context);
    }

    public void setExportProgressListener(ExportProgressListener listener){
        this.listener=listener;
    }

    @Override
    public void run() {
        /*try{
            //初始化导出路径
            if(!isExternal){
                File export_path=new File(SPUtil.getInternalSavePath(context));
                if(export_path.exists()&&!export_path.isDirectory()){
                    export_path.delete();
                }
                if(!export_path.exists()){
                    export_path.mkdirs();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            if(listener!=null)listener.onExportTaskFinished(new ArrayList<String>(),e.toString());
            return;
        }*/

        total= getTotalLength();
        long progress_check_apk=0;
        long bytesPerSecond=0;
        long startTime=System.currentTimeMillis();

        for(int i=0;i<list.size();i++){
            if(isInterrupted)break;
            try{
                final AppItem item=list.get(i);
                final int order_this_loop=i+1;

                if(!item.exportData&&!item.exportObb){

                    OutputStream outputStream;
                    if(isExternal) {
                        DocumentFile documentFile = OutputUtil.getWritingDocumentFileForAppItem(context,item,"apk");
                        this.currentWritingFile=new FileItem(context,documentFile);
                        outputStream= OutputUtil.getOutputStreamForDocumentFile(context,documentFile);
                    }
                    else {
                        this.currentWritingFile=new FileItem(OutputUtil.getAbsoluteWritePath(context,item,"apk"));
                        outputStream=new FileOutputStream(new File(OutputUtil.getAbsoluteWritePath(context,item,"apk")));
                    }

                    postCallback2Listener(new Runnable() {
                        @Override
                        public void run() {
                            if(listener!=null)listener.onExportAppItemStarted(order_this_loop,item,list.size(),currentWritingFile.getPath());
                        }
                    });

                    InputStream in = new FileInputStream(String.valueOf(item.getSourcePath())); //读入原文件

                    BufferedOutputStream out= new BufferedOutputStream(outputStream);

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
                                    if(listener!=null)listener.onExportProgressUpdated(progress,total,currentWritingFile.getPath());
                                }
                            });
                        }
                    }
                    out.flush();
                    in.close();
                    out.close();
                    write_paths.add(currentWritingFile);
                }

                else{
                    OutputStream outputStream;
                    if(isExternal){
                        DocumentFile documentFile= OutputUtil.getWritingDocumentFileForAppItem(context,item,"zip");
                        this.currentWritingFile=new FileItem(context,documentFile);
                        outputStream= OutputUtil.getOutputStreamForDocumentFile(context,documentFile);
                    }
                    else {
                        this.currentWritingFile= new FileItem(OutputUtil.getAbsoluteWritePath(context,item,"zip"));
                        outputStream=new FileOutputStream(new File(OutputUtil.getAbsoluteWritePath(context,item,"zip")));
                    }
                    postCallback2Listener(new Runnable() {
                        @Override
                        public void run() {
                            if(listener!=null)listener.onExportAppItemStarted(order_this_loop,item,list.size(),currentWritingFile.getPath());
                        }
                    });

                    ZipOutputStream zos=new ZipOutputStream(new BufferedOutputStream(outputStream));
                    zos.setComment("Packaged by com.github.ghmxr.apkextractor \nhttps://github.com/ghmxr/apkextractor");
                    int zip_level= SPUtil.getGlobalSharedPreferences(context).getInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT);

                    if(zip_level>=0&&zip_level<=9) zos.setLevel(zip_level);

                    writeZip(new File(String.valueOf(item.getSourcePath())),"",zos,zip_level);
                    if(item.exportData){
                        writeZip(new File(StorageUtil.getMainExternalStoragePath()+"/android/data/"+item.getPackageName()),"Android/data/",zos,zip_level);
                    }
                    if(item.exportObb){
                        writeZip(new File(StorageUtil.getMainExternalStoragePath()+"/android/obb/"+item.getPackageName()),"Android/obb/",zos,zip_level);
                    }
                    zos.flush();
                    zos.close();
                    write_paths.add(currentWritingFile);
                }


            }catch (Exception e){
                e.printStackTrace();
                this.error_message.append(e.toString());
                this.error_message.append("\n\n");
            }

        }

        if(isInterrupted){
            try{
                /*File file = new File(this.currentWritingFile.getPath());
                if(file.exists()&&!file.isDirectory()){
                    file.delete();
                }*/
                currentWritingFile.delete();//没有写入完成的文件为破损文件，尝试删除
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        postCallback2Listener(new Runnable() {
            @Override
            public void run() {
                if(listener!=null&&!isInterrupted)listener.onExportTaskFinished(write_paths,error_message.toString());
                context.sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
                context.sendBroadcast(new Intent(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE));
            }
        });

    }


    private void postCallback2Listener(Runnable runnable){
        if(listener==null||runnable==null)return;
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
                total+= FileUtil.getFileOrFolderSize(new File(StorageUtil.getMainExternalStoragePath()+"/android/data/"+item.getPackageName()));
            }
            if(item.exportObb){
                total+=FileUtil.getFileOrFolderSize(new File(StorageUtil.getMainExternalStoragePath()+"/android/obb/"+item.getPackageName()));
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
        void onExportTaskFinished(List<FileItem>write_paths, String error_message);
    }
}
