package com.github.ghmxr.apkextractor.items;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;

import com.github.ghmxr.apkextractor.utils.DocumentFileUtil;
import com.github.ghmxr.apkextractor.utils.PinyinUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 对File,DocumentFile的封装
 */
public class FileItem implements Comparable<FileItem>{

    public static int sort_config=0;
    private Context context;
    private File file=null;
    private DocumentFile documentFile=null;
    private Uri contentUri;

    /**
     * 构造一个documentFile实例的FileItem
     */
    public FileItem(@NonNull Context context, @NonNull Uri treeUri,@Nullable String segments) throws Exception{
        this.context=context;
        DocumentFile documentFile=DocumentFile.fromTreeUri(context, treeUri);
        if(documentFile==null)throw new Exception("Can not get documentFile by the treeUri");
        this.documentFile= DocumentFileUtil.getDocumentFileBySegments(documentFile,segments);
    }

    /**
     * 构造一个File 实例的FileItem
     */
    public FileItem(@NonNull String path){
        this.file=new File(path);
    }

    public FileItem(@NonNull Context context,@NonNull DocumentFile documentFile){
        this.context=context;
        this.documentFile=documentFile;
    }

    public FileItem(File file){
        //this.context=context;
        this.file=file;
    }

    public FileItem (@NonNull Context context,@NonNull Uri contentUri){
        this.context=context;
        this.contentUri=contentUri;
    }

    public String getName(){
        if(documentFile!=null)return documentFile.getName();
        if(file!=null)return file.getName();
        if(contentUri!=null)return contentUri.getLastPathSegment();
        return "";
    }

    public boolean isDirectory(){
        if(documentFile!=null)return documentFile.isDirectory();
        if(file!=null)return file.isDirectory();
        return false;
    }

    public boolean exists(){
        if(documentFile!=null)return documentFile.exists();
        if(file!=null)return file.exists();
        return false;
    }

    /**
     * 此方法只针对file生效
     */
    public boolean mkdirs(){
        if(file!=null)return file.mkdirs();
        return false;
    }

    /**
     * 针对documentFile创建文件夹
     * @param name 文件夹名称
     * @return 创建的文件夹对应的documentFile
     */
    public @Nullable DocumentFile createDirectory(@NonNull String name){
        if(documentFile!=null)return documentFile.createDirectory(name);
        return null;
    }

    /**
     * 本FileItem实例存储的是否为一个documentFile实例
     * @return true-documentFile
     */
    public boolean isDocumentFile(){
        return documentFile!=null;
    }

    /**
     * 本FileItem实际存储的是否为一个File实例
     * @return true-File实例
     */
    public boolean isFileInstance(){return file!=null;}

    /**
     * 本FileItem是否为通过provider获取的uri实例
     */
    public boolean isShareUriInstance(){
        return contentUri!=null;
    }

    /**
     * 如果为documentFile实例，则会返回以“external/”开头的片段；如果为File实例，则返回正常的完整路径
     */
    public String getPath(){
        if(documentFile!=null){
            String uriPath= documentFile.getUri().getPath();
            if(uriPath==null)return "";
            int index=uriPath.lastIndexOf(":")+1;
            if(index<=uriPath.length())return "external/"+uriPath.substring(index);
        }
        if(file!=null)return file.getAbsolutePath();
        if(contentUri!=null)return contentUri.getLastPathSegment();
        return "";
    }

    public boolean delete(){
        if(documentFile!=null)return documentFile.delete();
        if(file!=null)return file.delete();
        return false;
    }

    public @NonNull List<FileItem> listFileItems(){
        ArrayList<FileItem>arrayList=new ArrayList<>();
        if(documentFile!=null){
            try{
                DocumentFile[]documentFiles=documentFile.listFiles();
                for(DocumentFile documentFile:documentFiles){
                    arrayList.add(new FileItem(context,documentFile));
                    //Log.e("DFile",documentFile.getClass().getName());
                }
            }catch (Exception e){e.printStackTrace();}
            return arrayList;
        }else if(file!=null){
            try{
                File[]files=file.listFiles();
                if(files==null)return arrayList;
                for(File file:files)arrayList.add(new FileItem(file));
            }catch (Exception e){e.printStackTrace();}
            return arrayList;
        }
        return arrayList;
    }

    public long length(){
        try{
            if(documentFile!=null)return documentFile.length();
            if(file!=null)return file.length();
            if(contentUri!=null&&context!=null)return getInputStream().available();
        }catch (Exception e){e.printStackTrace();}
        return 0;
    }

    public long lastModified(){
        try{
            if(documentFile!=null)return documentFile.lastModified();
            if(file!=null)return file.lastModified();
        }catch (Exception e){e.printStackTrace();}
        return 0;
    }

    public @Nullable FileItem getParent(){
        if(file!=null){
            File parent=file.getParentFile();
            if(parent!=null)return new FileItem(parent);
        }else if(documentFile!=null){
            DocumentFile parent=documentFile.getParentFile();
            if(parent!=null)return new FileItem(context,parent);
        }
        return null;
    }

    public boolean isHidden(){
        if(file!=null)return file.isHidden();
        return false;
    }

    public InputStream getInputStream() throws Exception{
        if(documentFile!=null) return context.getContentResolver().openInputStream(documentFile.getUri());
        if(file!=null)return new FileInputStream(file);
        if(contentUri!=null&&context!=null)return context.getContentResolver().openInputStream(contentUri);
        return null;
    }

    public OutputStream getOutputStream() throws Exception{
        if(documentFile!=null)return context.getContentResolver().openOutputStream(documentFile.getUri());
        if(file!=null)return new FileOutputStream(file);
        if(contentUri!=null&&context!=null)return context.getContentResolver().openOutputStream(contentUri);
        return null;
    }

    public DocumentFile getDocumentFile(){
        return documentFile;
    }

    public File getFile(){return file;}

    public Uri getContentUri(){
        return contentUri;
    }

    public static synchronized void setSort_config(int value){
        sort_config=value;
    }

    @Override
    public int compareTo(@NonNull FileItem o) {
        switch (sort_config){
            default:break;
            case 0:{
                try{
                    if(PinyinUtil.getFirstSpell(String.valueOf(this.getName())).toLowerCase().compareTo(PinyinUtil.getFirstSpell(String.valueOf(o.getName())).toLowerCase())>0)return 1;
                    if(PinyinUtil.getFirstSpell(String.valueOf(this.getName())).toLowerCase().compareTo(PinyinUtil.getFirstSpell(String.valueOf(o.getName())).toLowerCase())<0)return -1;
                }catch (Exception e){e.printStackTrace();}
            }
            break;
            case 1:{
                try{
                    if(PinyinUtil.getFirstSpell(String.valueOf(this.getName())).toLowerCase().compareTo(PinyinUtil.getFirstSpell(String.valueOf(o.getName())).toLowerCase())>0)return -1;
                    if(PinyinUtil.getFirstSpell(String.valueOf(this.getName())).toLowerCase().compareTo(PinyinUtil.getFirstSpell(String.valueOf(o.getName())).toLowerCase())<0)return 1;
                }catch (Exception e){e.printStackTrace();}
            }
            break;
        }
        return 0;
    }

    @Override
    public @NonNull String toString() {
        if(documentFile!=null)return documentFile.getUri().toString();
        if(file!=null)return file.getAbsolutePath();
        return super.toString();
    }
}
