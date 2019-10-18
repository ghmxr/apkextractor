package com.github.ghmxr.apkextractor.utils;

import android.support.annotation.NonNull;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFileUtil {

    /**
     * 获取一个zip文件中data或者obb的大小，为耗时阻塞方法
     * @return 字节
     */
    public static long getDataOrObbSizeOfZipInputStream(@NonNull InputStream inputStream, @NonNull String type){
        long total=0;
        try{
            ZipInputStream zipInputStream=new ZipInputStream(inputStream);
            ZipEntry zipEntry=zipInputStream.getNextEntry();
            while (zipEntry!=null){
                String entryPath=zipEntry.getName().replaceAll("\\*", "/");
                if((entryPath.toLowerCase().startsWith("android/"+type))&&!zipEntry.isDirectory()){
                    int len;
                    byte[] buffer=new byte[1024];
                    while ((len=zipInputStream.read(buffer))!=-1){
                        total+=len;
                    }
                }
                zipEntry=zipInputStream.getNextEntry();
            }
            zipInputStream.close();
        }catch (Exception e){e.printStackTrace();}
        return total;
    }
}
