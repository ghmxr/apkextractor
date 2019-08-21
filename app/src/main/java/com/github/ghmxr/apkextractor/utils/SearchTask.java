package com.github.ghmxr.apkextractor.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.ghmxr.apkextractor.AppItem;
import com.github.ghmxr.apkextractor.Global;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchTask extends Thread {

    private boolean isInterrupted=false;
    private final String search_info;
    private final List<AppItem>totalList;
    private final ArrayList<AppItem>search_result=new ArrayList<>();
    private SearchTaskCompletedCallback callback;

    public SearchTask(@NonNull List<AppItem>total_list, @NonNull String info, @Nullable SearchTaskCompletedCallback callback){
        this.search_info=info;
        this.totalList=total_list;
        this.callback=callback;
    }

    @Override
    public void run() {
        super.run();
        for(AppItem item:totalList){
            if(isInterrupted){
                search_result.clear();
                return;
            }
            try{
                boolean b=(getFormatString(item.getAppName()).contains(search_info)
                        ||getFormatString(item.getPackageName()).contains(search_info)
                        ||getFormatString(item.getVersionName()).contains(search_info)
                        ||PinyinUtil.getFirstSpell(item.getAppName()).contains(search_info)
                        ||PinyinUtil.getFullSpell(item.getAppName()).contains(search_info)
                        ||PinyinUtil.getPinYin(item.getAppName()).contains(search_info))&&!search_info.trim().equals("");
                if(b)search_result.add(item);
            }catch (Exception e){e.printStackTrace();}
        }

        Global.handler.post(new Runnable() {
            @Override
            public void run() {
                if(callback!=null&&!isInterrupted)callback.onSearchTaskCompleted(search_result);
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
        void onSearchTaskCompleted(@NonNull List<AppItem>result);
    }
}
