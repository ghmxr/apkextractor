package com.github.ghmxr.apkextractor.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.MyApplication;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;
import com.github.ghmxr.apkextractor.utils.ZipFileUtil;

import java.util.Collections;
import java.util.List;

public class RefreshImportListTask extends Thread {
    private final Context context;
    //private FileItem fileItem;
    private final RefreshImportListTaskCallback callback;
    private boolean isInterrupted = false;
    @SuppressLint("StaticFieldLeak")
    private static RefreshImportListTask refreshImportListTask;
    final boolean exclude_invalid_package;

    public RefreshImportListTask(RefreshImportListTaskCallback callback) {
        this.context = MyApplication.getApplication();
        this.callback = callback;
        exclude_invalid_package = SPUtil.getGlobalSharedPreferences(context)
                .getBoolean(Constants.PREFERENCE_EXCLUDE_INVALID_PACKAGE, Constants.PREFERENCE_EXCLUDE_INVALID_PACKAGE_DEFAULT);
        //boolean isExternal= SPUtil.getIsSaved2ExternalStorage(context);
        /*if(isExternal){
            try{
                fileItem=new FileItem(context, Uri.parse(SPUtil.getExternalStorageUri(context)), SPUtil.getSaveSegment(context));
            }catch (Exception e){e.printStackTrace();}
        }else{
            fileItem=new FileItem(SPUtil.getInternalSavePath(context));
        }*/
    }

    @Override
    public void run() {
        if (callback != null) Global.handler.post(new Runnable() {
            @Override
            public void run() {
                callback.onRefreshStarted();
            }
        });
        final int package_scope_value = SPUtil.getGlobalSharedPreferences(context).getInt(Constants.PREFERENCE_PACKAGE_SCOPE, Constants.PREFERENCE_PACKAGE_SCOPE_DEFAULT);
        FileItem fileItem = null;
        if (package_scope_value == Constants.PACKAGE_SCOPE_ALL) {
            fileItem = FileItem.createFileItemInstance(StorageUtil.getMainExternalStoragePath());
        } else if (package_scope_value == Constants.PACKAGE_SCOPE_EXPORTING_PATH) {
            if (SPUtil.getIsSaved2ExternalStorage(context)) {
                try {
                    fileItem = FileItem.createFileItemInstance(Uri.parse(SPUtil.getExternalStorageUri(context)), SPUtil.getSaveSegment(context));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                fileItem = FileItem.createFileItemInstance(SPUtil.getInternalSavePath(context));
            }
        }
        Global.item_list.clear();
        if (!isInterrupted) {
            ImportItem.sort_config = SPUtil.getGlobalSharedPreferences(context).getInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, 0);
        }

        try {
            if (package_scope_value == Constants.PACKAGE_SCOPE_ALL) {//默认导出路径在data的情况下，未授权直接全盘遍历会导致找不到data下的文件，在这里先做一次遍历添加
                getAllImportItemsFromPath(FileItem.createFileItemInstance(SPUtil.getInternalSavePath(context)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            getAllImportItemsFromPath(fileItem);
        } catch (Exception e) {
            e.printStackTrace();
//            return;
        }

        try {
            if (!TextUtils.isEmpty(SPUtil.getExternalStorageUri(context)) && package_scope_value == Constants.PACKAGE_SCOPE_ALL) {
                getAllImportItemsFromPath(FileItem.createFileItemInstance(Uri.parse(SPUtil.getExternalStorageUri(context)), SPUtil.getSaveSegment(context)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (!isInterrupted) {
                Collections.sort(Global.item_list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*if (isInterrupted) return;
        Global.item_list.clear();
        Global.item_list.addAll(arrayList);*/
        HashTask.clearResultCache();
        GetSignatureInfoTask.clearCache();
        GetApkLibraryTask.clearOutsidePackageCache();
        GetPackageInfoViewTask.clearPackageInfoCache();
        synchronized (RefreshImportListTask.class) {
            refreshImportListTask = null;
        }
        if (callback != null && !isInterrupted) {
            Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onRefreshCompleted(Global.item_list);
                }
            });
        }
    }

    private void getAllImportItemsFromPath(final FileItem fileItem) {
//        ArrayList<ImportItem> arrayList = new ArrayList<>();
        try {
            if (fileItem == null) return;
            //File file=new File(fileItem.getPath());
            if (fileItem.isFile()) {
                final ImportItem importItem = new ImportItem(fileItem);
                if (fileItem.getPath().trim().toLowerCase().endsWith(".apk") || fileItem.getPath().trim().toLowerCase().endsWith(".zip")
                        || fileItem.getPath().trim().toLowerCase().endsWith(".xapk")
                        || fileItem.getPath().trim().toLowerCase().endsWith("." + SPUtil.getCompressingExtensionName(context).toLowerCase())) {
                    if (isInterrupted) {
//                        Global.item_list.clear();
                        throw new RuntimeException("task is interrupted");
                    }
                    if (callback != null) {
                        Global.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onProgress(false, importItem);
                            }
                        });
                    }
                    boolean canAdd = true;
                    if (fileItem.getPath().trim().toLowerCase().endsWith(".zip")
                            || fileItem.getPath().trim().toLowerCase().endsWith(".xapk")
                            || fileItem.getPath().trim().toLowerCase().endsWith("." + SPUtil.getCompressingExtensionName(context).toLowerCase())) {
                        ZipFileUtil.ZipFileInfo zipFileInfo = ZipFileUtil.getZipFileInfoOfImportItem(importItem);
                        if (exclude_invalid_package && (zipFileInfo == null || (zipFileInfo.getApkSize() == 0 && zipFileInfo.getDataSize() == 0 && zipFileInfo.getObbSize() == 0))) {
                            canAdd = false;
                        }
                    }
//                    if (canAdd) arrayList.add(importItem);
//                    SystemClock.sleep(1500);
//                    final ArrayList<ImportItem> container = new ArrayList<>();
                    if (!isInterrupted) {
                        if (canAdd) {
                            Global.item_list.add(importItem);
                        }
//                            Collections.sort(Global.item_list);
//                            container.addAll(Global.item_list);
                    }
//                    final CountDownLatch countDownLatch = new CountDownLatch(1);
                    final boolean fCanAdd = canAdd;
                    if (callback != null) {
                        Global.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onProgress(fCanAdd, importItem);
//                                countDownLatch.countDown();
                            }
                        });
                    }
//                    countDownLatch.await();
                }
                return;
            }
            if (fileItem.isDirectory()) {
                List<FileItem> fileItems = fileItem.listFileItems();
                for (final FileItem fileItem1 : fileItems) {
                    if (isInterrupted) {
//                        Global.item_list.clear();
                        throw new RuntimeException("task interrupted");
                    }
                    getAllImportItemsFromPath(fileItem1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        return arrayList;
    }

    private void setInterrupted() {
        this.isInterrupted = true;
        Global.item_list.clear();
    }

    @Override
    public void start() {
        synchronized (RefreshImportListTask.class) {
            if (refreshImportListTask != null) {
                refreshImportListTask.setInterrupted();
            }
            refreshImportListTask = this;
        }
        super.start();
    }

    public interface RefreshImportListTaskCallback {
        void onRefreshStarted();

        void onProgress(boolean canAdd, @NonNull ImportItem scannedItem);

        void onRefreshCompleted(List<ImportItem> list);
    }
}
