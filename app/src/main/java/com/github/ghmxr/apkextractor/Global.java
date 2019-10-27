package com.github.ghmxr.apkextractor;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.tasks.GetImportLengthAndDuplicateInfoTask;
import com.github.ghmxr.apkextractor.tasks.ImportTask;
import com.github.ghmxr.apkextractor.ui.DataObbDialog;
import com.github.ghmxr.apkextractor.ui.ExportingDialog;
import com.github.ghmxr.apkextractor.ui.ImportingDataObbDialog;
import com.github.ghmxr.apkextractor.ui.ImportingDialog;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.tasks.ExportTask;
import com.github.ghmxr.apkextractor.utils.DocumentFileUtil;
import com.github.ghmxr.apkextractor.utils.OutputUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.ZipFileUtil;

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
            public void onExportTaskFinished(List<FileItem> write_paths, String error_message) {
                dialog.cancel();
                if(listener!=null)listener.onFinished(error_message);
                ArrayList<Uri>uris=new ArrayList<>();
                for(FileItem s:write_paths){
                    if(s.isFileInstance())uris.add(getUriForFileByFileProvider(activity,s.getFile()));
                    else uris.add(s.getDocumentFile().getUri());
                }
                if(if_share) shareCertainFiles(activity,uris,activity.getResources().getString(R.string.share_title));
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
            ArrayList<Uri>uris=new ArrayList<>();
            if(items.size()==1){
                AppItem item=items.get(0);
                uris.add(Uri.fromFile(new File(item.getSourcePath())));
                shareCertainFiles(activity,uris,activity.getResources().getString(R.string.share_title)+" "+item.getAppName());
            }else{
                for(AppItem item:items){
                    uris.add(Uri.fromFile(new File(item.getSourcePath())));
                }
                shareCertainFiles(activity,uris,activity.getResources().getString(R.string.share_title));
            }
        }
    }

    public static void showImportingDataObbDialog(@NonNull final Activity activity, @NonNull List<ImportItem>importItems,@Nullable final ImportTaskFinishedCallback callback){
        new ImportingDataObbDialog(activity, importItems, new ImportingDataObbDialog.ImportDialogDataObbConfirmedCallback() {
            @Override
            public void onImportingDataObbConfirmed(@NonNull final List<ImportItem> importItems, @NonNull List<ZipFileUtil.ZipFileInfo> zipFileInfos) {
                new GetImportLengthAndDuplicateInfoTask(activity,importItems,zipFileInfos,new GetImportLengthAndDuplicateInfoTask.GetImportLengthAndDuplicateInfoCallback(){
                    @Override
                    public void onCheckingFinished(@NonNull String result, long total) {
                        final ImportingDialog importingDialog=new ImportingDialog(activity,total);
                        final ImportTask.ImportTaskCallback importTaskCallback=new ImportTask.ImportTaskCallback() {
                            @Override
                            public void onImportTaskStarted() {}

                            @Override
                            public void onRefreshSpeed(long speed) {
                                importingDialog.setSpeed(speed);
                            }

                            @Override
                            public void onImportTaskProgress(@NonNull String writePath, long progress) {
                                importingDialog.setProgress(progress);
                                importingDialog.setCurrentWritingName(writePath);
                            }

                            @Override
                            public void onImportTaskFinished(@NonNull String errorMessage) {
                                importingDialog.cancel();
                                if(callback!=null)callback.onImportFinished(errorMessage);
                            }
                        };
                        final ImportTask importTask=new ImportTask(activity, importItems,importTaskCallback);
                        importingDialog.setButton(AlertDialog.BUTTON_NEGATIVE, activity.getResources().getString(R.string.word_stop), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                importTask.setInterrupted();
                                importingDialog.cancel();
                            }
                        });
                        if(result.equals("")){
                            importingDialog.show();
                            importTask.start();
                        }else{
                            new AlertDialog.Builder(activity)
                                    .setTitle(activity.getResources().getString(R.string.dialog_import_duplicate_title))
                                    .setMessage(activity.getResources().getString(R.string.dialog_import_duplicate_message)+result)
                                    .setPositiveButton(activity.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            importingDialog.show();
                                            importTask.start();
                                        }
                                    })
                                    .setNegativeButton(activity.getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {}
                                    })
                                    .show();
                        }
                    }
                }).start();
            }
        }).show();
    }

    public interface ImportTaskFinishedCallback{
        void onImportFinished(String error_message);
    }

    public static void shareImportItems(@NonNull Activity activity,@NonNull List<ImportItem>importItems){
        ArrayList<Uri>uris=new ArrayList<>();
        for(ImportItem importItem:importItems){
            try{
                FileItem fileItem=importItem.getFileItem();
                if(fileItem.isFileInstance()){
                    if(Build.VERSION.SDK_INT<=23)uris.add(Uri.fromFile(fileItem.getFile()));
                    else uris.add(getUriForFileByFileProvider(activity,fileItem.getFile()));
                }else{
                    uris.add(fileItem.getDocumentFile().getUri());
                }
            }catch (Exception e){e.printStackTrace();}
        }
        shareCertainFiles(activity,uris,activity.getResources().getString(R.string.share_title));
    }

    /**
     * 传入的file须为主存储下的文件，且对file有完整的读写权限
     */
    public static Uri getUriForFileByFileProvider(@NonNull Context context,@NonNull File file){
        return FileProvider.getUriForFile(context,"com.github.ghmxr.apkextractor.FileProvider",file);
    }

    /**
     * @deprecated
     */
    public static String getFilePathForUri(Context context,Uri uri){
        try{
            String result;
            Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{MediaStore.Files.FileColumns.DATA},//
                    null, null, null);
            if (cursor == null) return "";
            else {
                cursor.moveToFirst();
                int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                result = cursor.getString(index);
                cursor.close();
            }
            return result;
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    /**
     * 执行分享应用操作
     */
    private static void shareCertainFiles(@NonNull Activity activity, @NonNull List<Uri>uris, @NonNull String title){
        if(uris.size()==0)return;
        Intent intent=new Intent();
        //intent.setType("application/vnd.android.package-archive");
        intent.setType("application/x-zip-compressed");
        if(uris.size()>1){
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            //ArrayList<Uri> uris=new ArrayList<>();
            //for(String path:paths) uris.add(Uri.fromFile(new File(path)));
            intent.putExtra(Intent.EXTRA_STREAM, new ArrayList<>(uris));
        }else{
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM,uris.get(0));
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, title);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{
            activity.startActivity(Intent.createChooser(intent,title));
        }catch (Exception e){
            e.printStackTrace();
            ToastManager.showToast(activity,e.toString(),Toast.LENGTH_SHORT);
        }
    }


}
