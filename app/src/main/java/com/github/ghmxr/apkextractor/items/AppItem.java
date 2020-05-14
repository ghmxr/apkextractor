package com.github.ghmxr.apkextractor.items;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.github.ghmxr.apkextractor.DisplayItem;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.FileUtil;
import com.github.ghmxr.apkextractor.utils.PinyinUtil;

import java.io.File;

/**
 * 单个应用项的所有信息
 */
public class AppItem implements Comparable<AppItem>, DisplayItem {

    public static transient int sort_config=0;

    /*public static final Creator<AppItem> CREATOR=new Creator<AppItem>() {
        @Override
        public AppItem createFromParcel(Parcel source) {
            return new AppItem(source);
        }

        @Override
        public AppItem[] newArray(int size) {
            return new AppItem[size];
        }
    };*/

    private PackageInfo info;

    private FileItem fileItem;

    /**
     * 程序名
     */
    private String title;
    /**
     * 应用图标
     */
    private Drawable drawable;

    /**
     *应用大小
     */
    private long size;

    private String installSource;

    private String launchingClass;

    //private String[]signatureInfos;

    //private HashMap<String, List<String>> static_receivers;

    private Bundle static_receivers_bundle;

    //仅当构造ExportTask时用
    public transient boolean exportData=false;
    public transient boolean exportObb=false;

    /**
     * 初始化一个全新的AppItem
     * @param context context实例，用来获取应用图标、名称等参数
     * @param info PackageInfo实例，对应的本AppItem的信息
     */
    public AppItem(@NonNull Context context, @NonNull PackageInfo info){
        PackageManager packageManager=context.getApplicationContext().getPackageManager();
        this.info=info;
        this.fileItem=new FileItem(new File(info.applicationInfo.sourceDir));
        this.title=packageManager.getApplicationLabel(info.applicationInfo).toString();
        this.size= FileUtil.getFileOrFolderSize(new File(info.applicationInfo.sourceDir));
        this.drawable=packageManager.getApplicationIcon(info.applicationInfo);
        String install_source=context.getResources().getString(R.string.word_unknown);
        try{
            final String installer_package_name=packageManager.getInstallerPackageName(info.packageName);
            final String installer_name=EnvironmentUtil.getAppNameByPackageName(context,installer_package_name);
            //install_source= TextUtils.isEmpty(installer_name)?installer_package_name:installer_name;
            if(!TextUtils.isEmpty(installer_name)){
                install_source=installer_name;
            }else{
                if(!TextUtils.isEmpty(installer_package_name)){
                    install_source=installer_package_name;
                }else{
                    install_source=context.getResources().getString(R.string.word_unknown);
                }
            }
        }catch (Exception e){e.printStackTrace();}
        this.installSource=install_source;

        String launchingClass=context.getResources().getString(R.string.word_unknown);
        try{
            Intent intent=packageManager.getLaunchIntentForPackage(info.packageName);
            if(intent==null)launchingClass=context.getResources().getString(R.string.word_none);
            else{
                launchingClass=intent.getComponent().getClassName();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        this.launchingClass=launchingClass;
        this.static_receivers_bundle=EnvironmentUtil.getStaticRegisteredReceiversOfBundleTypeForPackageName(context,info.packageName);
    }

    /**
     * 构造一个本Item的副本，用于ExportTask导出应用。
     * @param wrapper 用于创造副本的目标
     * @param flag_data 指定是否导出data
     * @param flag_obb 指定是否导出obb
     */
    public AppItem(AppItem wrapper,boolean flag_data,boolean flag_obb){
        this.title=wrapper.title;
        this.size=wrapper.size;
        this.info=wrapper.info;
        this.drawable=wrapper.drawable;
        this.installSource=wrapper.installSource;
        this.launchingClass=wrapper.launchingClass;
        this.exportData=flag_data;
        this.exportObb=flag_obb;
        //this.signatureInfos=wrapper.signatureInfos;
    }

    /*private AppItem(Parcel in){
        title=in.readString();
        size=in.readLong();
        info=in.readParcelable(PackageInfo.class.getClassLoader());
        //static_receivers=in.readHashMap(HashMap.class.getClassLoader());
        static_receivers_bundle=in.readBundle(Bundle.class.getClassLoader());
    }*/

    @Override
    public Drawable getIconDrawable() {
        return drawable;
    }

    @Override
    public String getTitle() {
        return title+"("+getVersionName()+")";
    }

    @Override
    public String getDescription() {
        return info.packageName;
    }

    @Override
    public boolean isRedMarked() {
        return (info.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)>0;
    }

    /**
     * 获取应用图标
     */
    public Drawable getIcon(){
        return drawable;
    }

    /**
     * 获取应用名称
     */
    public String getAppName(){
        return title;
    }

    /**
     * 获取包名
     */
    public String getPackageName(){
        return info.packageName;
    }

    /**
     * 获取应用源路径
     */
    public String getSourcePath(){
        return String.valueOf(info.applicationInfo.sourceDir);
    }

    /**
     * 获取应用大小（源文件），单位字节
     */
    @Override
    public long getSize(){
        return size;
    }

    /**
     * 获取应用版本名称
     */
    public String getVersionName(){
        return info.versionName;
    }

    /**
     * 获取应用版本号
     */
    public int getVersionCode(){
        return info.versionCode;
    }

    /**
     * 获取本应用Item对应的PackageInfo实例
     */
    public PackageInfo getPackageInfo(){
        return info;
    }

    public String getInstallSource() {
        return installSource;
    }

    public String getLaunchingClass() {
        return launchingClass;
    }

    public FileItem getFileItem() {
        return fileItem;
    }

    /*
     * 长度为8
     */
    /*public String[] getSignatureInfos() {
        return signatureInfos;
    }*/

    /*@Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeLong(size);
        dest.writeParcelable(info,0);
        //dest.writeMap(static_receivers);
        dest.writeBundle(static_receivers_bundle);
    }*/


    public @NonNull Bundle getStaticReceiversBundle(){
        return static_receivers_bundle;
    }

    /**
     * 排序模式。
     * 0 - 默认
     * 1 - 名称升序
     * 2 - 名称降序
     * 3 - 大小升序
     * 4 - 大小降序
     * 5 - 更新日期升序
     * 6 - 更新日期降序
     * 7 - 安装日期升序
     * 8 - 安装日期降序
     * 9 - 包名升序
     * 10 - 包名降序
     */
    @Override
    public int compareTo(@NonNull AppItem o) {
        switch (sort_config){
            default:break;
            case 1:{
                try{
                    if(PinyinUtil.getFirstSpell(title).toLowerCase().compareTo(PinyinUtil.getFirstSpell(o.title).toLowerCase())>0)return 1;
                    if(PinyinUtil.getFirstSpell(title).toLowerCase().compareTo(PinyinUtil.getFirstSpell(o.title).toLowerCase())<0) return -1;
                }catch (Exception e){e.printStackTrace();}
            }
            break;
            case 2:{
                try{
                    if(PinyinUtil.getFirstSpell(title).toLowerCase().compareTo(PinyinUtil.getFirstSpell(o.title).toLowerCase())<0)return 1;
                    if(PinyinUtil.getFirstSpell(title).toLowerCase().compareTo(PinyinUtil.getFirstSpell(o.title).toLowerCase())>0)return -1;
                }catch (Exception e){e.printStackTrace();}
            }
            break;
            case 3:{
                if(size-o.size>0)return 1;
                if(size-o.size<0)return -1;
            }
            break;
            case 4:{
                if(size-o.size<0)return 1;
                if(size-o.size>0)return -1;
            }
            break;
            case 5:{
                if(info.lastUpdateTime-o.info.lastUpdateTime>0)return 1;
                if(info.lastUpdateTime-o.info.lastUpdateTime<0)return -1;
            }
            break;
            case 6:{
                if(info.lastUpdateTime-o.info.lastUpdateTime<0)return 1;
                if(info.lastUpdateTime-o.info.lastUpdateTime>0)return -1;
            }
            break;
            case 7:{
                if(info.firstInstallTime-o.info.firstInstallTime>0)return 1;
                if(info.firstInstallTime-o.info.firstInstallTime<0)return -1;
            }
            break;
            case 8:{
                if(info.firstInstallTime-o.info.firstInstallTime<0)return 1;
                if(info.firstInstallTime-o.info.firstInstallTime>0)return -1;
            }
            break;
            case 9:{
                if(String.valueOf(getPackageName()).toLowerCase().compareTo(String.valueOf(o.getPackageName()).toLowerCase())>0)return 1;
                if(String.valueOf(getPackageName()).toLowerCase().compareTo(String.valueOf(o.getPackageName()).toLowerCase())<0)return -1;
            }
            break;
            case 10:{
                if(String.valueOf(getPackageName()).toLowerCase().compareTo(String.valueOf(o.getPackageName()).toLowerCase())<0)return 1;
                if(String.valueOf(getPackageName()).toLowerCase().compareTo(String.valueOf(o.getPackageName()).toLowerCase())>0)return -1;
            }
            break;
        }
        return 0;
    }
}
