package com.github.ghmxr.apkextractor.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.utils.SPUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 刷新已安装的应用列表
 */
public class RefreshInstalledListTask extends Thread{
    private Context context;
    private boolean flag_system;
    private RefreshInstalledListTaskCallback listener;
    private List<AppItem> list_sum=new ArrayList<>();
    public RefreshInstalledListTask(@NonNull Context context, boolean flag_system, @Nullable RefreshInstalledListTaskCallback callback){
        this.context=context;
        this.flag_system=flag_system;
        this.listener=callback;
    }
    @Override
    public void run(){
        PackageManager manager=context.getApplicationContext().getPackageManager();
        SharedPreferences settings= SPUtil.getGlobalSharedPreferences(context);
        int flag=PackageManager.GET_SIGNATURES;
        if(settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS,Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT))flag|=PackageManager.GET_PERMISSIONS;
        if(settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES,Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT))flag|=PackageManager.GET_ACTIVITIES;
        if(settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS,Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT))flag|=PackageManager.GET_RECEIVERS;

        final List<PackageInfo> list = manager.getInstalledPackages(flag);
        Global.handler.post(new Runnable() {
            @Override
            public void run() {
                if(listener!=null)listener.onRefreshProgressStarted(list.size());
            }
        });
        for(int i=0;i<list.size();i++){
            PackageInfo info=list.get(i);
            boolean info_is_system_app=((info.applicationInfo.flags& ApplicationInfo.FLAG_SYSTEM)>0);
            final int current=i+1;
            Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    if(listener!=null)listener.onRefreshProgressUpdated(current,list.size());
                }
            });
            if(!flag_system&&info_is_system_app)continue;
            list_sum.add(new AppItem(context,info));
        }
        AppItem.sort_config=settings.getInt(Constants.PREFERENCE_SORT_CONFIG,0);
        Collections.sort(list_sum);
        synchronized (Global.app_list){
            Global.app_list.clear();
            Global.app_list.addAll(list_sum);//向全局list保存一个引用
        }
        Global.handler.post(new Runnable() {
            @Override
            public void run() {
                if(listener!=null)listener.onRefreshCompleted(list_sum);
            }
        });

    }

    public interface RefreshInstalledListTaskCallback{
        void onRefreshProgressStarted(int total);
        void onRefreshProgressUpdated(int current, int total);
        void onRefreshCompleted(List<AppItem> appList);
    }
}
