package com.github.ghmxr.apkextractor.tasks;

import android.content.Context;
import android.net.Uri;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.utils.SPUtil;

import java.util.ArrayList;
import java.util.List;

public class RefreshImportListTask extends Thread{
    private Context context;
    private FileItem fileItem;
    private RefreshImportListTaskCallback callback;
    public RefreshImportListTask(Context context, RefreshImportListTaskCallback callback){
        this.context=context;
        this.callback=callback;
        boolean isExternal= SPUtil.getIsSaved2ExternalStorage(context);
        if(isExternal){
            try{
                fileItem=new FileItem(context, Uri.parse(SPUtil.getExternalStorageUri(context)), SPUtil.getSaveSegment(context));
            }catch (Exception e){e.printStackTrace();}
        }else{
            fileItem=new FileItem(SPUtil.getInternalSavePath(context));
        }
    }

    @Override
    public void run(){
        final ArrayList<ImportItem> arrayList=new ArrayList<>();
        try{
            arrayList.addAll(getAllImportItemsFromPath(fileItem));
        }catch (Exception e){e.printStackTrace();}
        synchronized (Global.item_list){
            Global.item_list.clear();
            Global.item_list.addAll(arrayList);
        }
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
                if(fileItem.getPath().trim().toLowerCase().endsWith(".apk")||fileItem.getPath().trim().toLowerCase().endsWith(".zip"))arrayList.add(new ImportItem(context,fileItem));
                return arrayList;
            }
            List<FileItem>fileItems=fileItem.listFileItems();
            for(FileItem fileItem1:fileItems){
                if(fileItem1.isDirectory())arrayList.addAll(getAllImportItemsFromPath(fileItem1));
                else {
                    if(fileItem1.getPath().trim().toLowerCase().endsWith(".apk")||fileItem1.getPath().trim().toLowerCase().endsWith(".zip")){
                        arrayList.add(new ImportItem(context,fileItem1));
                    }
                }
            }
        }catch (Exception e){e.printStackTrace();}
        return arrayList;
    }

    public interface RefreshImportListTaskCallback{
        void onRefreshCompleted(List<ImportItem> list);
    }
}
