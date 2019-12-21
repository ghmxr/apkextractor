package com.github.ghmxr.apkextractor.utils;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.R;

public class SPUtil {

    public static @NonNull String getDisplayingExportPath(@NonNull Context context){
        if(getIsSaved2ExternalStorage(context)){
            String segment=getSaveSegment(context);
            if(segment==null)segment="";
            return context.getResources().getString(R.string.external_storage)+"/"+segment;
        }else return getInternalSavePath(context);
    }
    /**
     * 获取当前应用导出的内置主路径
     * @return 应用导出内置路径，最后没有文件分隔符，例如 /storage/emulated/0
     */
    public static @NonNull
    String getInternalSavePath(@NonNull Context context){
        try{
            return getGlobalSharedPreferences(context).getString(Constants.PREFERENCE_SAVE_PATH, Constants.PREFERENCE_SAVE_PATH_DEFAULT);
        }catch (Exception e){e.printStackTrace();}
        return Constants.PREFERENCE_SAVE_PATH_DEFAULT;
    }

    /**
     * 获取全局配置
     */
    public static SharedPreferences getGlobalSharedPreferences(@NonNull Context context){
        return context.getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE);
    }

    /**
     * 判断是否存储到了外置设备上
     * @return true-存储到了外置存储上
     */
    public static boolean getIsSaved2ExternalStorage(@NonNull Context context){
        return getGlobalSharedPreferences(context).getBoolean(Constants.PREFERENCE_STORAGE_PATH_EXTERNAL,Constants.PREFERENCE_STORAGE_PATH_EXTERNAL_DEFAULT);
    }

    /**
     * 获取外置存储的uri值
     */
    public static @NonNull String getExternalStorageUri(@NonNull Context context){
        return getGlobalSharedPreferences(context).getString(Constants.PREFERENCE_SAVE_PATH_URI,"");
    }

    /**
     * 获取存储到外置存储的路径片段
     */
    public static @Nullable
    String getSaveSegment(@NonNull Context context){
        String value=getGlobalSharedPreferences(context).getString(Constants.PREFERENCE_SAVE_PATH_SEGMENT,"");
        if(value.equals(""))return null;
        return value;
    }

    /**
     * 发送/接收 端口号，默认6565
     */
    public static int getPortNumber(@NonNull Context context){
        return getGlobalSharedPreferences(context).getInt(Constants.PREFERENCE_NET_PORT,Constants.PREFERENCE_NET_PORT_DEFAULT);
    }

    /**
     * 获取设备名称
     */
    public static @NonNull String getDeviceName(@NonNull Context context){
        try{
            return getGlobalSharedPreferences(context)
                    .getString(Constants.PREFERENCE_DEVICE_NAME, Build.BRAND);
        }catch (Exception e){
            e.printStackTrace();
        }
        return Constants.PREFERENCE_DEVICE_NAME_DEFAULT;
    }
}
