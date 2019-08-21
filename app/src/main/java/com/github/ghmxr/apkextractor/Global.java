package com.github.ghmxr.apkextractor;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.data.Constants;
import com.github.ghmxr.apkextractor.ui.DataObbDialog;
import com.github.ghmxr.apkextractor.ui.ExportingDialog;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.ExportTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Global {

    /**
     * 全局Handler，用于向主UI线程发送消息
     */
    public static final Handler handler=new Handler(Looper.getMainLooper());

    /**
     * 用于持有对读取出的list的引用
     */
    public static List<AppItem> list;

    public static void showRequestingWritePermissionSnackBar(@NonNull final Activity activity){
        Snackbar snackbar=Snackbar.make(activity.findViewById(android.R.id.content),activity.getResources().getString(R.string.permission_write),Snackbar.LENGTH_SHORT);
        snackbar.setAction(activity.getResources().getString(R.string.permission_grant), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
                activity.startActivity(intent);
            }
        });
        snackbar.show();
    }

    public interface ExportTaskFinishedListener{
        void onFinished(@NonNull String error_message);
    }

    /**
     * 选择data,obb项，确认重复文件，导出list集合中的应用，并向activity显示一个dialog，传入接口来监听完成回调（在主线程）
     * @param list AppItem的副本，除singleList外data obb值须为false
     */
    public static void checkAndExportCertainAppItemsToSetPathWithoutShare(@NonNull final Activity activity, @NonNull final List<AppItem>list, @Nullable final ExportTaskFinishedListener listener){
        if(list.size()==0)return;
        if(list.size()==1){
            String dulplicated_info=getDuplicatedFileInfo(activity,list);
            if(!dulplicated_info.trim().equals("")){
                new AlertDialog.Builder(activity)
                        .setTitle(activity.getResources().getString(R.string.dialog_duplicate_title))
                        .setMessage(activity.getResources().getString(R.string.dialog_duplicate_msg)+dulplicated_info)
                        .setPositiveButton(activity.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                exportCertainAppItemsToSetPathAndShare(activity, list, false,listener);
                            }
                        })
                        .setNegativeButton(activity.getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                        .show();
                return;
            }
            exportCertainAppItemsToSetPathAndShare(activity, list, false,listener);
            return;
        }

        DataObbDialog dialog=new DataObbDialog(activity, list, new DataObbDialog.DialogDataObbConfirmedCallback() {
            @Override
            public void onDialogDataObbConfirmed(@NonNull final List<AppItem> export_list) {
                String dulplicated_info=getDuplicatedFileInfo(activity,export_list);
                if(!dulplicated_info.trim().equals("")){
                    new AlertDialog.Builder(activity)
                            .setTitle(activity.getResources().getString(R.string.dialog_duplicate_title))
                            .setMessage(activity.getResources().getString(R.string.dialog_duplicate_msg)+dulplicated_info)
                            .setPositiveButton(activity.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    exportCertainAppItemsToSetPathAndShare(activity, export_list, false,listener);
                                }
                            })
                            .setNegativeButton(activity.getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {}
                            })
                            .show();
                    return;
                }
                exportCertainAppItemsToSetPathAndShare(activity, export_list, false,listener);
            }
        });
        dialog.show();
    }

    /**
     * 导出list集合中的应用，并向activity显示一个dialog，传入接口来监听完成回调（在主线程）
     * @param if_share 完成后是否执行分享操作
     */
    private static void exportCertainAppItemsToSetPathAndShare(@NonNull final Activity activity, @NonNull List<AppItem>export_list, final boolean if_share, @Nullable final ExportTaskFinishedListener listener){
        final ExportingDialog dialog=new ExportingDialog(activity);
        final ExportTask task=new ExportTask(activity,export_list,null);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, activity.getResources().getString(R.string.dialog_export_stop), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.setInterrupted();
                dialog.cancel();
            }
        });
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        task.setExportProgressListener(new ExportTask.ExportProgressListener() {
            @Override
            public void onExportAppItemStarted(int order, AppItem item, int total, String write_path) {
                dialog.setProgressOfApp(order,total,item,write_path);
            }

            @Override
            public void onExportProgressUpdated(long current, long total, String write_path) {
                dialog.setProgressOfWriteBytes(current,total);
            }

            @Override
            public void onExportZipProgressUpdated(String write_path) {
                dialog.setProgressOfCurrentZipFile(write_path);
            }

            @Override
            public void onExportSpeedUpdated(long speed) {
                dialog.setSpeed(speed);
            }

            @Override
            public void onExportTaskFinished(List<String> write_paths, String error_message) {
                dialog.cancel();
                if(listener!=null)listener.onFinished(error_message);
                if(if_share) shareCertainApps(activity,write_paths,activity.getResources().getString(R.string.share_title));
            }
        });
        task.start();
    }

    /**
     * 通过包名获取指定list中的item
     * @param list 要遍历的list
     * @param package_name 要定位的包名
     * @return 查询到的AppItem
     */
    public static @Nullable AppItem getAppItemByPackageNameFromList(@NonNull List<AppItem>list,@NonNull String package_name){
        try{
            for(AppItem item:list){
                if(item.getPackageName().trim().toLowerCase().equals(package_name.trim().toLowerCase()))return item;
            }
        }catch (Exception e){e.printStackTrace();}
        return null;
    }

    public static String getDuplicatedFileInfo(@NonNull Context context,@NonNull List<AppItem>items){
        if(items.size()==0)return "";
        StringBuilder builder=new StringBuilder();
        for(AppItem item:items){
            File file=new File(getAbsoluteWritePath(context,item,(item.exportData||item.exportObb)?"zip":"apk"));
            if(file.exists()){
                builder.append(file.getAbsolutePath());
                builder.append("\n\n");
            }
        }
        return builder.toString();
    }

    /**
     * 分享指定item应用
     * @param items 传入AppItem的副本，data obb为false
     */
    public static void shareCertainAppsByItems(@NonNull final Activity activity,@NonNull final List<AppItem>items){
        if(items.size()==0)return;
        boolean ifNeedExport= Global.getGlobalSharedPreferences(activity)
                .getInt(Constants.PREFERENCE_SHAREMODE,Constants.PREFERENCE_SHAREMODE_DEFAULT)==Constants.SHARE_MODE_AFTER_EXTRACT;
        if(ifNeedExport){
            DataObbDialog dialog=new DataObbDialog(activity, items, new DataObbDialog.DialogDataObbConfirmedCallback() {
                @Override
                public void onDialogDataObbConfirmed(@NonNull List<AppItem> export_list) {
                    String dulplicated_info=getDuplicatedFileInfo(activity,items);
                    final ExportTaskFinishedListener exportTaskFinishedListener=new ExportTaskFinishedListener() {
                        @Override
                        public void onFinished(@NonNull String error_message) {
                            if(!error_message.trim().equals("")){
                                new AlertDialog.Builder(activity)
                                        .setTitle(activity.getResources().getString(R.string.exception_title))
                                        .setMessage(activity.getResources().getString(R.string.exception_message)+error_message)
                                        .setPositiveButton(activity.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {}
                                        })
                                        .show();
                                return;
                            }
                            ToastManager.showToast(activity,activity.getResources().getString(R.string.toast_export_complete)+getSavePath(activity), Toast.LENGTH_SHORT);
                        }
                    };
                    if(!dulplicated_info.trim().equals("")){
                        new AlertDialog.Builder(activity)
                                .setTitle(activity.getResources().getString(R.string.dialog_duplicate_title))
                                .setMessage(activity.getResources().getString(R.string.dialog_duplicate_msg)+dulplicated_info)
                                .setPositiveButton(activity.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        exportCertainAppItemsToSetPathAndShare(activity, items, true,exportTaskFinishedListener);
                                    }
                                })
                                .setNegativeButton(activity.getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {}
                                })
                                .show();
                        return;
                    }
                    exportCertainAppItemsToSetPathAndShare(activity, items, true,exportTaskFinishedListener);
                }
            });
            dialog.show();
        }else {
            ArrayList<String>paths=new ArrayList<>();
            if(items.size()==1){
                AppItem item=items.get(0);
                paths.add(item.getSourcePath());
                shareCertainApps(activity,paths,activity.getResources().getString(R.string.share_title)+" "+item.getAppName());
            }else{
                for(AppItem item:items){
                    paths.add(item.getSourcePath());
                }
                shareCertainApps(activity,paths,activity.getResources().getString(R.string.share_title));
            }
        }
    }

    /**
     * 执行分享应用操作
     */
    private static void shareCertainApps(@NonNull Activity activity, @NonNull List<String>paths, @NonNull String title){
        if(paths.size()==0)return;
        Intent intent=new Intent();
        //intent.setType("application/vnd.android.package-archive");
        intent.setType("application/x-zip-compressed");
        if(paths.size()>1){
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            ArrayList<Uri> uris=new ArrayList<>();
            for(String path:paths) uris.add(Uri.fromFile(new File(path)));
            intent.putExtra(Intent.EXTRA_STREAM, uris);
        }else{
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM,Uri.fromFile(new File(paths.get(0))));
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, title);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(Intent.createChooser(intent,title));
    }


    /**
     * 获取当前应用导出的主路径
     * @return 应用导出路径，最后没有文件分隔符，例如 /storage/emulated/0
     */
    public static @NonNull String getSavePath(@NonNull Context context){
        try{
            return getGlobalSharedPreferences(context).getString(Constants.PREFERENCE_SAVE_PATH, Constants.PREFERENCE_SAVE_PATH_DEFAULT);
        }catch (Exception e){e.printStackTrace();}
        return Constants.PREFERENCE_SAVE_PATH_DEFAULT;
    }

    /**
     * 为AppItem获取一个绝对写入路径
     * @param extension "apk"或者"zip"
     */
    public static @NonNull String getAbsoluteWritePath(@NonNull Context context, @NonNull AppItem item, @NonNull String extension){
        try{
            SharedPreferences settings=getGlobalSharedPreferences(context);
            if(extension.toLowerCase(Locale.getDefault()).equals("apk")){
                return settings.getString(Constants.PREFERENCE_SAVE_PATH, Constants.PREFERENCE_SAVE_PATH_DEFAULT)
                        +"/"+settings.getString(Constants.PREFERENCE_FILENAME_FONT_APK, Constants.PREFERENCE_FILENAME_FONT_DEFAULT).replace(Constants.FONT_APP_NAME, String.valueOf(item.getAppName()))
                        .replace(Constants.FONT_APP_PACKAGE_NAME, String.valueOf(item.getPackageName()))
                        .replace(Constants.FONT_APP_VERSIONCODE, String.valueOf(item.getVersionCode()))
                        .replace(Constants.FONT_APP_VERSIONNAME, String.valueOf(item.getVersionName()))+".apk";
            }
            if(extension.toLowerCase(Locale.ENGLISH).equals("zip")){
                return settings.getString(Constants.PREFERENCE_SAVE_PATH, Constants.PREFERENCE_SAVE_PATH_DEFAULT)
                        +"/"+settings.getString(Constants.PREFERENCE_FILENAME_FONT_ZIP, Constants.PREFERENCE_FILENAME_FONT_DEFAULT).replace(Constants.FONT_APP_NAME, String.valueOf(item.getAppName()))
                        .replace(Constants.FONT_APP_PACKAGE_NAME, String.valueOf(item.getPackageName()))
                        .replace(Constants.FONT_APP_VERSIONCODE, String.valueOf(item.getVersionCode()))
                        .replace(Constants.FONT_APP_VERSIONNAME, String.valueOf(item.getVersionName()))+".zip";
            }
        }catch(Exception e){e.printStackTrace();}
        return "";
    }

    public static SharedPreferences getGlobalSharedPreferences(@NonNull Context context){
        return context.getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE);
    }

    /**
     * 刷新已安装的应用列表
     */
    public static class RefreshInstalledListTask extends Thread{
        private Context context;
        private boolean flag_system;
        private RefreshInstalledListTaskCallback listener;
        private List<AppItem> list_sum=new ArrayList<>();
        public RefreshInstalledListTask(@NonNull Context context,boolean flag_system,@Nullable RefreshInstalledListTaskCallback callback){
            this.context=context;
            this.flag_system=flag_system;
            this.listener=callback;
        }
        @Override
        public void run(){
            PackageManager manager=context.getApplicationContext().getPackageManager();
            SharedPreferences settings=getGlobalSharedPreferences(context);
            int flag=PackageManager.GET_SIGNATURES;
            if(settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS,Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT))flag|=PackageManager.GET_PERMISSIONS;
            if(settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES,Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT))flag|=PackageManager.GET_ACTIVITIES;
            if(settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS,Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT))flag|=PackageManager.GET_RECEIVERS;

            final List<PackageInfo> list = manager.getInstalledPackages(flag);
            for(int i=0;i<list.size();i++){
                PackageInfo info=list.get(i);
                boolean info_is_system_app=((info.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)>0);
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
            synchronized (Global.class){
                Global.list=list_sum;//向全局list保存一个引用
            }
            Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    if(listener!=null)listener.onRefreshCompleted(list_sum);
                }
            });

        }
    }

    public interface RefreshInstalledListTaskCallback{
        void onRefreshProgressUpdated(int current, int total);
        void onRefreshCompleted(List<AppItem> appList);
    }

}
