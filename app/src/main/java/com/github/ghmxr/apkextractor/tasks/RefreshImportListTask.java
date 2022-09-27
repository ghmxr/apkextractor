package com.github.ghmxr.apkextractor.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class RefreshImportListTask extends Thread {
    private Context context;
    //private FileItem fileItem;
    private final RefreshImportListTaskCallback callback;
    private boolean isInterrupted = false;
    @SuppressLint("StaticFieldLeak")
    private static RefreshImportListTask refreshImportListTask;

    public RefreshImportListTask(Context context, RefreshImportListTaskCallback callback) {
        this.context = context;
        this.callback = callback;
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
        final ArrayList<ImportItem> arrayList = new ArrayList<>();
        if (callback != null) Global.handler.post(new Runnable() {
            @Override
            public void run() {
                callback.onRefreshStarted();
            }
        });
        final int package_scope_value = SPUtil.getGlobalSharedPreferences(context).getInt(Constants.PREFERENCE_PACKAGE_SCOPE, Constants.PREFERENCE_PACKAGE_SCOPE_DEFAULT);
        FileItem fileItem = null;
        if (package_scope_value == Constants.PACKAGE_SCOPE_ALL) {
            fileItem = new FileItem(StorageUtil.getMainExternalStoragePath());
        } else if (package_scope_value == Constants.PACKAGE_SCOPE_EXPORTING_PATH) {
            if (SPUtil.getIsSaved2ExternalStorage(context)) {
                try {
                    fileItem = new FileItem(context, Uri.parse(SPUtil.getExternalStorageUri(context)), SPUtil.getSaveSegment(context));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                fileItem = new FileItem(SPUtil.getInternalSavePath(context));
            }
        }
        synchronized (Global.item_list) {
            Global.item_list.clear();
        }
        if (!isInterrupted) {
            ImportItem.sort_config = SPUtil.getGlobalSharedPreferences(context).getInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, 0);
        }
        try {
            arrayList.addAll(getAllImportItemsFromPath(fileItem));
            if (!TextUtils.isEmpty(SPUtil.getExternalStorageUri(context)) && package_scope_value == Constants.PACKAGE_SCOPE_ALL) {
                arrayList.addAll(getAllImportItemsFromPath(new FileItem(context, Uri.parse(SPUtil.getExternalStorageUri(context)), SPUtil.getSaveSegment(context))));
            }
            if (!isInterrupted) {
                Collections.sort(arrayList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (isInterrupted) return;
        synchronized (Global.item_list) {
            Global.item_list.clear();
            Global.item_list.addAll(arrayList);
        }
        HashTask.clearResultCache();
        GetSignatureInfoTask.clearCache();
        refreshImportListTask = null;
        if (callback != null) {
            Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onRefreshCompleted(arrayList);
                }
            });
        }
    }

    private ArrayList<ImportItem> getAllImportItemsFromPath(final FileItem fileItem) {
        ArrayList<ImportItem> arrayList = new ArrayList<>();
        try {
            if (fileItem == null) return arrayList;
            //File file=new File(fileItem.getPath());
            if (fileItem.isFile()) {
                final ImportItem importItem = new ImportItem(context, fileItem);
                if (fileItem.getPath().trim().toLowerCase().endsWith(".apk") || fileItem.getPath().trim().toLowerCase().endsWith(".zip")
                        || fileItem.getPath().trim().toLowerCase().endsWith(".xapk")
                        || fileItem.getPath().trim().toLowerCase().endsWith(SPUtil.getCompressingExtensionName(context).toLowerCase())) {
                    if (isInterrupted) {
                        throw new RuntimeException("task is interrupted");
                    }
                    arrayList.add(importItem);
//                    SystemClock.sleep(1500);
                    final ArrayList<ImportItem> container = new ArrayList<>();
                    synchronized (Global.item_list) {
                        if (!isInterrupted) {
                            Global.item_list.add(importItem);
//                            Collections.sort(Global.item_list);
                            container.addAll(Global.item_list);
                        }
                    }
                    final CountDownLatch countDownLatch = new CountDownLatch(1);
                    if (callback != null) {
                        Global.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onProgress(importItem, container);
                                countDownLatch.countDown();
                            }
                        });
                    }
                    countDownLatch.await();
                }
                return arrayList;
            }
            if (fileItem.isDirectory()) {
                List<FileItem> fileItems = fileItem.listFileItems();
                for (final FileItem fileItem1 : fileItems) {
                    if (isInterrupted) {
                        throw new RuntimeException("task interrupted");
                    }
                    arrayList.addAll(getAllImportItemsFromPath(fileItem1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrayList;
    }

    public void setInterrupted() {
        this.isInterrupted = true;
    }

    @Override
    public synchronized void start() {
        if (refreshImportListTask != null) {
            refreshImportListTask.setInterrupted();
        }
        refreshImportListTask = this;
        super.start();
    }

    public interface RefreshImportListTaskCallback {
        void onRefreshStarted();

        void onProgress(@NonNull ImportItem scannedItem, @NonNull List<ImportItem> progress);

        void onRefreshCompleted(List<ImportItem> list);
    }
}
