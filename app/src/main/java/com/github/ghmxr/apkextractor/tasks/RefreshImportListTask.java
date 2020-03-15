package com.github.ghmxr.apkextractor.tasks;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RefreshImportListTask extends Thread{
    private Context context;
    //private FileItem fileItem;
    private RefreshImportListTaskCallback callback;
    public RefreshImportListTask(Context context, RefreshImportListTaskCallback callback){
        this.context=context;
        this.callback=callback;
        //boolean isExternal= SPUtil.getIsSaved2ExternalStorage(context);
        /*if(isExternal){
            try{
                fileItem=new FileItem(context, Uri.parse(SPUtil.getExternalStorageUri(context)), SPUtil.getSaveSegment(context));
            }catch (Exception e){e.printStackTrace();}
        }else{
            fileItem=new FileItem(SPUtil.getInternalSavePath(context));
        }*/
    }

    @Override
    public void run(){
        final ArrayList<ImportItem> arrayList=new ArrayList<>();
        if(callback!=null)Global.handler.post(new Runnable() {
            @Override
            public void run() {
                if(callback!=null)callback.onRefreshStarted();
            }
        });
        final int package_scope_value=SPUtil.getGlobalSharedPreferences(context).getInt(Constants.PREFERENCE_PACKAGE_SCOPE,Constants.PREFERENCE_PACKAGE_SCOPE_DEFAULT);
        FileItem fileItem=null;
        if(package_scope_value==Constants.PACKAGE_SCOPE_ALL){
            fileItem=new FileItem(StorageUtil.getMainExternalStoragePath());
        }else if(package_scope_value==Constants.PACKAGE_SCOPE_EXPORTING_PATH){
            fileItem=new FileItem(SPUtil.getInternalSavePath(context));
        }
        try{
            arrayList.addAll(getAllImportItemsFromPath(fileItem));
            if(!TextUtils.isEmpty(SPUtil.getExternalStorageUri(context))&&package_scope_value==Constants.PACKAGE_SCOPE_ALL){
                arrayList.addAll(getAllImportItemsFromPath(new FileItem(context, Uri.parse(SPUtil.getExternalStorageUri(context)), SPUtil.getSaveSegment(context))));
            }
            ImportItem.sort_config=SPUtil.getGlobalSharedPreferences(context).getInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS,0);
            Collections.sort(arrayList);
        }catch (Exception e){e.printStackTrace();}
        synchronized (Global.item_list){
            Global.item_list.clear();
            Global.item_list.addAll(arrayList);
        }
        HashTask.clearResultCache();
        GetSignatureInfoTask.clearCache();
        if(callback!=null){
            Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    if(callback!=null)callback.onRefreshCompleted(arrayList);
                }
            });
        }
    }

    private ArrayList<ImportItem> getAllImportItemsFromPath(FileItem fileItem){
        ArrayList<ImportItem>arrayList=new ArrayList<>();
        try{
            if (fileItem==null)return arrayList;
            //File file=new File(fileItem.getPath());
            if(!fileItem.isDirectory()){
                if(fileItem.getPath().trim().toLowerCase().endsWith(".apk")||fileItem.getPath().trim().toLowerCase().endsWith(".zip")
                ||fileItem.getPath().trim().toLowerCase().endsWith(".xapk")
                        ||fileItem.getPath().trim().toLowerCase().endsWith(SPUtil.getCompressingExtensionName(context).toLowerCase())){
                    arrayList.add(new ImportItem(context,fileItem));
                }
                return arrayList;
            }
            List<FileItem>fileItems=fileItem.listFileItems();
            for(FileItem fileItem1:fileItems){
                if(fileItem1.isDirectory())arrayList.addAll(getAllImportItemsFromPath(fileItem1));
                else {
                    if(fileItem1.getPath().trim().toLowerCase().endsWith(".apk")||fileItem1.getPath().trim().toLowerCase().endsWith(".zip")
                            ||fileItem1.getPath().trim().toLowerCase().endsWith(".xapk")
                            ||fileItem1.getPath().trim().toLowerCase().endsWith(SPUtil.getCompressingExtensionName(context).toLowerCase())){
                        try{
                            arrayList.add(new ImportItem(context,fileItem1));
                        }catch (Exception e){e.printStackTrace();}
                    }
                }
            }
        }catch (Exception e){e.printStackTrace();}
        return arrayList;
    }

    public interface RefreshImportListTaskCallback{
        void onRefreshStarted();
        void onRefreshCompleted(List<ImportItem> list);
    }
}
