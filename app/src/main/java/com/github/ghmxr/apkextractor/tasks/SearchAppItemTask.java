package com.github.ghmxr.apkextractor.tasks;

import android.support.annotation.NonNull;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.utils.PinyinUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchAppItemTask extends Thread {

    private boolean isInterrupted=false;
    private final String search_info;
    private final List<AppItem> appItemList;
    private final ArrayList<AppItem> result_appItems=new ArrayList<>();
    private final SearchTaskCompletedCallback callback;


    public SearchAppItemTask(@NonNull List<AppItem>appItems,@NonNull String info,@NonNull SearchTaskCompletedCallback callback){
        this.search_info=info;
        this.appItemList=appItems;
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
        Global.handler.post(new Runnable() {
            @Override
            public void run() {
                if(!isInterrupted)callback.onSearchTaskCompleted(result_appItems);
            }
        });
    }

    public void setInterrupted(){
        isInterrupted=true;
    }

    private String getFormatString(@NonNull String s){
        return s.trim().toLowerCase(Locale.getDefault());
    }

    public interface SearchTaskCompletedCallback{
        void onSearchTaskCompleted(@NonNull List<AppItem> appItems);
    }
}
