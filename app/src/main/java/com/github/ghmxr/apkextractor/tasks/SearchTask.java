package com.github.ghmxr.apkextractor.tasks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.ghmxr.apkextractor.DisplayItem;
import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.utils.PinyinUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchTask extends Thread {

    private boolean isInterrupted=false;
    private final String search_info;
    private final List<AppItem> appItemList;
    private final List<ImportItem> importItemList;
    private final ArrayList<AppItem> result_appItems =new ArrayList<>();
    private final ArrayList<ImportItem>result_importItems=new ArrayList<>();
    private SearchTaskCompletedCallback callback;

    public SearchTask(@NonNull List<AppItem>appItemList, @NonNull List<ImportItem>importItemList, @NonNull String info, @Nullable SearchTaskCompletedCallback callback){
        this.search_info=info;
        this.appItemList =appItemList;
        this.importItemList=importItemList;
        this.callback=callback;
    }

    @Override
    public void run() {
        super.run();
        for(AppItem item: appItemList){
            if(isInterrupted){
                break;
            }
            try{
                boolean b=(getFormatString(item.getAppName()).contains(search_info)
                        ||getFormatString(item.getPackageName()).contains(search_info)
                        ||getFormatString(item.getVersionName()).contains(search_info)
                        || PinyinUtil.getFirstSpell(item.getAppName()).contains(search_info)
                        ||PinyinUtil.getFullSpell(item.getAppName()).contains(search_info)
                        ||PinyinUtil.getPinYin(item.getAppName()).contains(search_info))&&!search_info.trim().equals("");
                if(b) result_appItems.add(item);
            }catch (Exception e){e.printStackTrace();}
        }

        for(ImportItem importItem:importItemList){
            if(isInterrupted){
                result_appItems.clear();
                result_importItems.clear();
                return;
            }
            try{
                boolean b=(getFormatString(importItem.getItemName()).contains(search_info)||getFormatString(importItem.getDescription()).contains(search_info))
                        &&!search_info.trim().equals("");
                if(b)result_importItems.add(importItem);
            }catch (Exception e){e.printStackTrace();}
        }

        Global.handler.post(new Runnable() {
            @Override
            public void run() {
                if(callback!=null&&!isInterrupted)callback.onSearchTaskCompleted(result_appItems,result_importItems);
            }
        });
    }

    public void setInterrupted(){
        isInterrupted=true;
    }

    private static @NonNull String getFormatString(@NonNull String s){
        return s.trim().toLowerCase(Locale.getDefault());
    }

    public interface SearchTaskCompletedCallback{
        void onSearchTaskCompleted(@NonNull List<AppItem>appItems,@NonNull List<ImportItem>importItems);
    }
}
