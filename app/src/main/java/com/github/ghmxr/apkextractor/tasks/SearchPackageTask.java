package com.github.ghmxr.apkextractor.tasks;

import android.support.annotation.NonNull;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.utils.PinyinUtil;

import java.util.ArrayList;
import java.util.List;

public class SearchPackageTask extends Thread {

    private volatile boolean isInterrupted=false;
    private final String search_info;
    private final List<ImportItem> importItemList;
    private final ArrayList<ImportItem> result_importItems=new ArrayList<>();
    private final SearchTaskCompletedCallback callback;

    public SearchPackageTask(@NonNull List<ImportItem>importItemList, @NonNull String info, @NonNull SearchTaskCompletedCallback callback){
        this.search_info=info.trim().toLowerCase();
        this.importItemList=importItemList;
        this.callback=callback;
    }

    @Override
    public void run() {
        super.run();
        for(ImportItem importItem:importItemList){
            if(isInterrupted){
                result_importItems.clear();
                return;
            }
            try{
                boolean b=(getFormatString(importItem.getItemName()).contains(search_info)||getFormatString(importItem.getDescription()).contains(search_info)
                        ||getFormatString(PinyinUtil.getFirstSpell(importItem.getItemName())).contains(search_info)
                        ||getFormatString(PinyinUtil.getFullSpell(importItem.getItemName())).contains(search_info)
                        ||getFormatString(PinyinUtil.getPinYin(importItem.getItemName())).contains(search_info)
                        ||getFormatString(PinyinUtil.getFirstSpell(importItem.getDescription())).contains(search_info)
                        ||getFormatString(PinyinUtil.getFullSpell(importItem.getDescription())).contains(search_info)
                        ||getFormatString(PinyinUtil.getPinYin(importItem.getDescription())).contains(search_info))
                        &&!search_info.trim().equals("");
                if(b)result_importItems.add(importItem);
            }catch (Exception e){e.printStackTrace();}
        }
        Global.handler.post(new Runnable() {
            @Override
            public void run() {
                if(!isInterrupted)callback.onSearchTaskCompleted(result_importItems,search_info);
            }
        });
    }

    public void setInterrupted(){
        isInterrupted=true;
    }

    private String getFormatString(@NonNull String s){
        return s.trim().toLowerCase();
    }

    public interface SearchTaskCompletedCallback{
        void onSearchTaskCompleted(@NonNull List<ImportItem> importItems,@NonNull String keyword);
    }
}
