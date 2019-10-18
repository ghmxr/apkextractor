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
    private final boolean isExternal;

    public ImportTask(@NonNull Context context, @NonNull List<ImportItem>importItems) {
        super();
        this.context=context;
        importItemArrayList.addAll(importItems);
        isExternal= SPUtil.getIsSaved2ExternalStorage(context);
    }

    @Override
    public void run() {
        super.run();
        for(ImportItem importItem:importItemArrayList){
            if(isInterrupted)break;
            if(importItem.importData){
                try{
                    unzipFile(importItem,"data");
                }catch (Exception e){e.printStackTrace();}

            }
            if(importItem.importObb){
                try{
                    unzipFile(importItem,"obb");
                }catch (Exception e){e.printStackTrace();}
            }
        }
        if(isInterrupted){
            try{
                if(currentWritePath!=null)new File(currentWritePath).delete();
            }catch (Exception e){e.printStackTrace();}
        }
    }

    private void unzipFile(ImportItem importItem,String type) throws Exception{
        ZipInputStream zipInputStream=new ZipInputStream(importItem.getZipInputStream());
        ZipEntry zipEntry=zipInputStream.getNextEntry();
        while (zipEntry!=null&&!isInterrupted){
            String entryPath=zipEntry.getName().replaceAll("\\*", "/");
            if((entryPath.toLowerCase().startsWith("android/"+type))&&!zipEntry.isDirectory()){
                File folder=new File(StorageUtil.getMainExternalStoragePath()+entryPath.substring(0,entryPath.lastIndexOf("/")));
                if(!folder.exists())folder.mkdirs();
                String writePath=StorageUtil.getMainExternalStoragePath()+"/"+entryPath;
                OutputStream outputStream=new BufferedOutputStream(new FileOutputStream(new File(writePath)));
                currentWritePath=writePath;
                byte [] buffer=new byte[1024];
                int len;
                while ((len=zipInputStream.read(buffer))!=-1&&!isInterrupted){
                    outputStream.write(buffer,0,len);
                }
                outputStream.flush();
                outputStream.close();
            }
            zipEntry=zipInputStream.getNextEntry();
        }
        zipInputStream.close();
    }

    public void setInterrupted(){
        this.isInterrupted=true;
    }
}
