package com.github.ghmxr.apkextractor.items;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.github.ghmxr.apkextractor.DisplayItem;
import com.github.ghmxr.apkextractor.R;

import java.text.DateFormat;
import java.util.Date;

public class ImportItem implements DisplayItem {

    public enum ImportType{
        APK,ZIP
    }

    private FileItem fileItem;
    private long length;
    private ImportType importType=ImportType.ZIP;

    private Drawable drawable=null;
    private String version_name="";
    private long lastModified;

    public transient boolean importData=false;
    public transient boolean importObb=false;

    public ImportItem(@NonNull Context context,@NonNull FileItem fileItem){
        this.fileItem=fileItem;
        if(fileItem.getName().trim().toLowerCase().endsWith(".zip")){
            importType=ImportType.ZIP;
            drawable=context.getResources().getDrawable(R.drawable.icon_zip);
        }
        if(fileItem.getName().trim().toLowerCase().endsWith(".apk")){
            importType=ImportType.APK;
            PackageManager packageManager=context.getApplicationContext().getPackageManager();
            if(fileItem.isFileInstance()){
                PackageInfo packageInfo=packageManager.getPackageArchiveInfo(fileItem.getPath(),0);
                packageInfo.applicationInfo.sourceDir=fileItem.getPath();
                packageInfo.applicationInfo.publicSourceDir=fileItem.getPath();
                drawable=packageInfo.applicationInfo.loadIcon(packageManager);
                version_name=packageInfo.versionName;
            }else{
                drawable=context.getResources().getDrawable(R.mipmap.ic_launcher);
                version_name="";
            }

        }
        length=fileItem.length();
        lastModified =fileItem.lastModified();
    }

    public ImportItem(@NonNull ImportItem wrapper,boolean importData,boolean importObb){
        this.drawable=wrapper.drawable;
        this.version_name=wrapper.version_name;
        this.fileItem=wrapper.fileItem;
        this.importType=wrapper.importType;
        this.length=wrapper.length;
        this.lastModified =wrapper.lastModified;
        this.importData=importData;
        this.importObb=importObb;
    }

    @Override
    public Drawable getIconDrawable() {
        return drawable;
    }

    @Override
    public String getTitle() {
        return fileItem.getName();
    }

    @Override
    public String getDescription() {
        DateFormat dateFormat=DateFormat.getDateTimeInstance();
        if(importType==ImportType.APK)return dateFormat.format(new Date(lastModified))+"("+version_name+")";
        return dateFormat.format(new Date(lastModified));
    }

    @Override
    public long getSize() {
        return length;
    }

    @Override
    public boolean isRedMarked() {
        return false;
    }

    public String getItemName(){
        return fileItem.getName();
    }

    public long getLastModified(){
        return lastModified;
    }

    public ImportType getImportType(){
        return importType;
    }

    /**
     * 只针对apk的版本名
     */
    public String getVersionName(){
        return version_name;
    }

    public Drawable getItemIconDrawable(){
        return drawable;
    }

}
