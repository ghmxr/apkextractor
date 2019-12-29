package com.github.ghmxr.apkextractor.items;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.ghmxr.apkextractor.DisplayItem;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.utils.PinyinUtil;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImportItem implements DisplayItem,Comparable<ImportItem> {

    public enum ImportType{
        APK,ZIP
    }

    public static int sort_config=0;

    private Context context;

    private FileItem fileItem;
    private long length;
    private ImportType importType=ImportType.ZIP;

    private Drawable drawable=null;
    private String version_name="";
    private long lastModified;

    public transient boolean importData=false;
    public transient boolean importObb=false;
    public transient boolean importApk=false;

    public ImportItem(@NonNull Context context,@NonNull FileItem fileItem){
        this.fileItem=fileItem;
        this.context=context;
        if(fileItem.getName().trim().toLowerCase().endsWith(".zip")){
            importType=ImportType.ZIP;
            drawable=context.getResources().getDrawable(R.drawable.icon_zip);
        }
        if(fileItem.getName().trim().toLowerCase().endsWith(".apk")){
            importType=ImportType.APK;
            PackageManager packageManager=context.getApplicationContext().getPackageManager();
            if(fileItem.isFileInstance()){
                PackageInfo packageInfo=null;
                try{
                    packageInfo=packageManager.getPackageArchiveInfo(fileItem.getPath(),0);
                }catch (Exception e){e.printStackTrace();}
                if(packageInfo!=null){
                    drawable=packageInfo.applicationInfo.loadIcon(packageManager);
                    version_name=packageInfo.versionName;
                }else{
                    drawable=context.getResources().getDrawable(R.drawable.icon_apk);
                    version_name=context.getResources().getString(R.string.word_unknown);
                }
            }else{
                drawable=context.getResources().getDrawable(R.drawable.icon_apk);
                version_name=context.getResources().getString(R.string.word_unknown);
            }

        }
        length=fileItem.length();
        lastModified =fileItem.lastModified();
    }

    public ImportItem(@NonNull ImportItem wrapper,boolean importData,boolean importObb,boolean importApk){
        this.drawable=wrapper.drawable;
        this.version_name=wrapper.version_name;
        this.fileItem=wrapper.fileItem;
        this.context=wrapper.context;
        this.importType=wrapper.importType;
        this.length=wrapper.length;
        this.lastModified =wrapper.lastModified;
        this.importData=importData;
        this.importObb=importObb;
        this.importApk=importApk;
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
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if(importType==ImportType.APK)return simpleDateFormat.format(new Date(lastModified))+"("+version_name+")";
        return simpleDateFormat.format(new Date(lastModified));
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

    public FileItem getFileItem(){
        return fileItem;
    }
    /**
     * 当本项目为zip包时的输入流
     */
    public @Nullable InputStream getZipInputStream() throws Exception{
        if(importType==ImportType.ZIP) return fileItem.getInputStream();
        return null;
    }

    public @Nullable Uri getUri(){
        if(fileItem.isDocumentFile()){
            return fileItem.getDocumentFile().getUri();
        }
        if(fileItem.isFileInstance()){
            return Global.getUriForFileByFileProvider(context,fileItem.getFile());
        }
        return null;
    }

    /**
     * 如果此导入项为存储到内置存储的Uri.fromFile()
     * @return uri
     */
    public @Nullable Uri getUriFromFile(){
        if(fileItem.isFileInstance())return Uri.fromFile(fileItem.getFile());
        return null;
    }

    @Override
    public int compareTo(@NonNull ImportItem o) {
        switch (sort_config){
            default:break;
            case 0:break;
            case 1:{
                try{
                    if(PinyinUtil.getFirstSpell(getTitle()).toLowerCase().compareTo(PinyinUtil.getFirstSpell(o.getTitle()).toLowerCase())>0)return 1;
                    if(PinyinUtil.getFirstSpell(getTitle()).toLowerCase().compareTo(PinyinUtil.getFirstSpell(o.getTitle()).toLowerCase())<0) return -1;
                }catch (Exception e){e.printStackTrace();}
            }
            break;
            case 2:{
                try{
                    if(PinyinUtil.getFirstSpell(getTitle()).toLowerCase().compareTo(PinyinUtil.getFirstSpell(o.getTitle()).toLowerCase())<0)return 1;
                    if(PinyinUtil.getFirstSpell(getTitle()).toLowerCase().compareTo(PinyinUtil.getFirstSpell(o.getTitle()).toLowerCase())>0)return -1;
                }catch (Exception e){e.printStackTrace();}
            }
            break;
            case 3:{
                if(getSize()-o.getSize()>0)return 1;
                if(getSize()-o.getSize()<0)return -1;
            }
            break;
            case 4:{
                if(getSize()-o.getSize()<0)return 1;
                if(getSize()-o.getSize()>0)return -1;
            }
            break;
            case 5:{
                if(getLastModified()-o.getLastModified()>0)return 1;
                if(getLastModified()-o.getLastModified()<0)return -1;
            }
            break;
            case 6:{
                if(getLastModified()-o.getLastModified()<0)return 1;
                if(getLastModified()-o.getLastModified()>0)return -1;
            }
            break;
        }
        return 0;
    }
}
