package com.github.ghmxr.apkextractor.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StorageUtil {
    /**
     * 获取指定path的可写入存储容量，单位字节
     */
    public static long getAvaliableSizeOfPath(@NonNull String path){
        try{
            StatFs stat = new StatFs(path);
            int version=Build.VERSION.SDK_INT;
            long blockSize = version>=18?stat.getBlockSizeLong():stat.getBlockSize();
            long availableBlocks = version>=18?stat.getAvailableBlocksLong():stat.getAvailableBlocks();
            return  blockSize * availableBlocks;
        }catch(Exception e){e.printStackTrace();}
        return 0;
    }

    /**
     * 获取外部存储主路径
     */
    public static @NonNull String getMainExternalStoragePath(){
        try{
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    /**
     * 获取设备挂载的所有外部存储分区path
     */
    public static @NonNull List<String> getAvailableStoragePaths(@NonNull Context context){
        ArrayList<String>paths=new ArrayList<>();
        if(Build.VERSION.SDK_INT>=21){
            try{
                File[] files=context.getExternalFilesDirs(null);
                if(files==null)return paths;
                for(File file:files){
                    String path=file.getAbsolutePath().toLowerCase();
                    path=path.substring(0,path.indexOf("/android/data"));
                    paths.add(path);
                }
            }catch (Exception e){e.printStackTrace();}
        }else{
            try{
                String mainStorage=getMainExternalStoragePath().toLowerCase(Locale.getDefault()).trim();
                try{
                    paths.add(mainStorage);
                }catch(Exception e){}

                Runtime runtime=Runtime.getRuntime();
                Process process=runtime.exec("mount");
                BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while((line=reader.readLine())!=null){
                    //Log.d("aa", line);
                    //if (line.contains("proc") || line.contains("tmpfs") || line.contains("media") || line.contains("asec") || line.contains("secure") || line.contains("system") || line.contains("cache")
                    //       || line.contains("sys") || line.contains("data") || line.contains("shell") || line.contains("root") || line.contains("acct") || line.contains("misc") || line.contains("obb")){
                    //   continue;
                    //}
                    if (line.contains("fat") || line.contains("fuse") || line.contains("ntfs")||line.contains("sdcardfs")||line.contains("fuseblk")){
                        String [] items = line.split(" ");
                        for(String s:items){
                            s=s.trim().toLowerCase();
                            if((s.contains(File.separator)||s.contains("/"))&&!paths.contains(s)) paths.add(s);
                        }
                    }
                }
                //Log.d("StoragePaths", Arrays.toString(paths.toArray()));
                return paths;
            }catch(Exception e){e.printStackTrace();}
        }

        return paths;
    }
}
