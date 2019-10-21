package com.github.ghmxr.apkextractor.tasks;

import android.content.Context;
import android.support.annotation.NonNull;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.utils.DocumentFileUtil;
import com.github.ghmxr.apkextractor.utils.OutputUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ImportTask extends Thread {

    private final ArrayList<ImportItem>importItemArrayList=new ArrayList<>();
    private Context context;
    private boolean isInterrupted=false;
    //private ArrayList<String>write_paths=new ArrayList<>();
    private String currentWritePath;
    private long progress=0;
    private long progress_check_length=0;
    private long speed_bytes=0;
    private long speed_check_time=0;
    private final boolean isExternal;

    private final StringBuilder error_info=new StringBuilder();

    private ImportTaskCallback callback;

    public ImportTask(@NonNull Context context, @NonNull List<ImportItem>importItems,ImportTaskCallback callback) {
        super();
        this.context=context;
        importItemArrayList.addAll(importItems);
        this.callback=callback;
        isExternal= SPUtil.getIsSaved2ExternalStorage(context);
    }

    @Override
    public void run() {
        super.run();
        for(ImportItem importItem:importItemArrayList){
            if(isInterrupted)break;
            try{
                ZipInputStream zipInputStream=new ZipInputStream(importItem.getZipInputStream());
                ZipEntry zipEntry=zipInputStream.getNextEntry();
                while (zipEntry!=null&&!isInterrupted){
                    String entryPath=zipEntry.getName().replaceAll("\\*", "/");
                    if((entryPath.toLowerCase().startsWith("android/data"))&&!zipEntry.isDirectory()&&importItem.importData){
                        unZipToFile(zipInputStream,entryPath);
                    }
                    else if((entryPath.toLowerCase().startsWith("android/obb"))&&!zipEntry.isDirectory()&&importItem.importObb){
                        unZipToFile(zipInputStream,entryPath);
                    }
                    else if((entryPath.toLowerCase().endsWith(".apk"))&&!zipEntry.isDirectory()&&!entryPath.contains("/")&&importItem.importApk){
                        OutputStream outputStream;
                        String fileName=entryPath.substring(entryPath.lastIndexOf("/")+1);
                        if(isExternal){
                            outputStream=OutputUtil.getOutputStreamForDocumentFile(context
                                    ,OutputUtil.getExportPathDocumentFile(context).createFile("application/vnd.android.package-archive",fileName));
                        }else{
                            String writePath=SPUtil.getInternalSavePath(context)+"/"+fileName;
                            outputStream=new FileOutputStream(new File(writePath));
                            currentWritePath=writePath;
                        }
                        BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(outputStream);
                        byte [] buffer=new byte[1024];
                        int len;
                        while ((len=zipInputStream.read(buffer))!=-1&&!isInterrupted){
                            bufferedOutputStream.write(buffer,0,len);
                            progress+=len;
                            checkSpeedAndPostToCallback(len);
                            checkProgressAndPostToCallback();
                        }
                        bufferedOutputStream.flush();
                        bufferedOutputStream.close();
                    }
                    zipEntry=zipInputStream.getNextEntry();
                }
                zipInputStream.close();
            }catch (Exception e){
                e.printStackTrace();
                error_info.append(e.toString());
                error_info.append("\n\n");
            }

        }
        if(isInterrupted){
            try{
                if(currentWritePath!=null)new File(currentWritePath).delete();
            }catch (Exception e){e.printStackTrace();}
        }else if(callback!=null)Global.handler.post(new Runnable() {
            @Override
            public void run() {
                callback.onImportTaskFinished(error_info.toString());
            }
        });
    }

    private void unZipToFile(ZipInputStream zipInputStream,String entryPath) throws Exception{
        File folder=new File(StorageUtil.getMainExternalStoragePath()+"/"+entryPath.substring(0,entryPath.lastIndexOf("/")));
        if(!folder.exists())folder.mkdirs();
        String writePath=StorageUtil.getMainExternalStoragePath()+"/"+entryPath;
        OutputStream outputStream=new BufferedOutputStream(new FileOutputStream(new File(writePath)));
        currentWritePath=writePath;
        byte [] buffer=new byte[1024];
        int len;
        while ((len=zipInputStream.read(buffer))!=-1&&!isInterrupted){
            outputStream.write(buffer,0,len);
            progress+=len;
            checkSpeedAndPostToCallback(len);
            checkProgressAndPostToCallback();
        }
        outputStream.flush();
        outputStream.close();
    }

    private void checkProgressAndPostToCallback(){
        if(progress-progress_check_length>100*1024){
            progress_check_length=progress;
            if(callback!=null)Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onImportTaskProgress(currentWritePath,progress);
                }
            });
        }
    }

    private void checkSpeedAndPostToCallback(long speed_plus_value){
        speed_bytes+=speed_plus_value;
        long current=System.currentTimeMillis();
        if(current-speed_check_time>1000){
            speed_check_time=current;
            final long speed_post=speed_bytes;
            speed_bytes=0;
            if(callback!=null)Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onRefreshSpeed(speed_post);
                }
            });
        }
    }

    public void setInterrupted(){
        this.isInterrupted=true;
    }

    public interface ImportTaskCallback{
        void onImportTaskStarted();
        void onRefreshSpeed(long speed);
        void onImportTaskProgress(@NonNull String writePath,long progress);
        void onImportTaskFinished(@NonNull String errorMessage);
    }
}
