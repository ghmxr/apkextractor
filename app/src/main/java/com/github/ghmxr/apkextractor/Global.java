package com.github.ghmxr.apkextractor;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.ui.DataObbDialog;
import com.github.ghmxr.apkextractor.ui.ExportingDialog;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.tasks.ExportTask;
import com.github.ghmxr.apkextractor.utils.DocumentFileUtil;
import com.github.ghmxr.apkextractor.utils.OutputUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Global {

    /**
     * 全局Handler，用于向主UI线程发送消息
     */
    public static final Handler handler=new Handler(Looper.getMainLooper());

    /**
     * 用于持有对读取出的list的引用
     */
    public static final List<AppItem> app_list=new ArrayList<>();

    /**
     * 导出目录下的文件list引用
     */
    public static final List<ImportItem> item_list=new ArrayList<>();

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
     * @param list AppItem的副本，当check_data_obb值为true时无需初始，false时须提前设置好data,obb值
     * @param check_data_obb 传入true 则会执行一次data,obb检查（list中没有设置data,obb值）
     */
    public static void checkAndExportCertainAppItemsToSetPathWithoutShare(@NonNull final Activity activity, @NonNull final List<AppItem>list, boolean check_data_obb,@Nullable final ExportTaskFinishedListener listener){
        if(list.size()==0)return;
        if(check_data_obb){
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
        }else{
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
        }

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
        boolean external=SPUtil.getIsSaved2ExternalStorage(context);
        if(external){
            for(AppItem item:items){
                try{
                    DocumentFile searchFile=OutputUtil.getExportPathDocumentFile(context).findFile(OutputUtil.getWriteFileNameForAppItem(context,item,(item.exportData||item.exportObb)?"zip":"apk"));
                    if(searchFile!=null){
                        builder.append(DocumentFileUtil.getDisplayPathForDocumentFile(context,searchFile));
                        builder.append("\n\n");
                    }
                }catch (Exception e){e.printStackTrace();}
            }
        }else {
            for(AppItem item:items){
                File file=new File(OutputUtil.getAbsoluteWritePath(context,item,(item.exportData||item.exportObb)?"zip":"apk"));
                if(file.exists()){
                    builder.append(file.getAbsolutePath());
                    builder.append("\n\n");
                }
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
        boolean ifNeedExport= SPUtil.getGlobalSharedPreferences(activity)
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
                            ToastManager.showToast(activity,activity.getResources().getString(R.string.toast_export_complete)+ SPUtil.getDisplayingExportPath(activity), Toast.LENGTH_SHORT);
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


}
