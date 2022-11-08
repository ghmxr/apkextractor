package com.github.ghmxr.apkextractor.tasks;

import android.os.Build;

import androidx.annotation.NonNull;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.utils.DocumentFileUtil;
import com.github.ghmxr.apkextractor.utils.FileUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class GetDataObbTask extends Thread {

    private static final ConcurrentHashMap<String, DataObbSizeInfo> cache_data_obb_size = new ConcurrentHashMap<>();

    public static class DataObbSizeInfo {
        public final long data;
        public final long obb;

        public DataObbSizeInfo(long data, long obb) {
            this.data = data;
            this.obb = obb;
        }
    }

    private final List<AppItem> list = new Vector<>();
    private boolean isInterrupted = false;
    private final DataObbSizeGetCallback callback;

    public interface DataObbSizeGetCallback {
        void onDataObbSizeGet(List<AppItem> containsData, List<AppItem> containsObb, DataObbSizeInfo dataObbSizeInfo);
    }

    public GetDataObbTask(List<AppItem> appItems, DataObbSizeGetCallback callback) {
        this.callback = callback;
        if (appItems != null) {
            list.addAll(appItems);
        }
    }

    public GetDataObbTask(@NonNull AppItem appItem, DataObbSizeGetCallback callback) {
        this.callback = callback;
        list.add(appItem);
    }

    @Override
    public void run() {
        super.run();
        long data = 0L, obb = 0L;
        final ArrayList<AppItem> containsDataCollection = new ArrayList<>();
        final ArrayList<AppItem> containsObbCollection = new ArrayList<>();
        for (AppItem item : list) {
            if (isInterrupted) return;
            DataObbSizeInfo dataObbSizeInfo = cache_data_obb_size.get(item.getPackageName());
            if (dataObbSizeInfo == null) {
                dataObbSizeInfo = getDataObbSizeInfo(item);
                cache_data_obb_size.put(item.getPackageName(), dataObbSizeInfo);
            }
            if (dataObbSizeInfo.data > 0) {
                containsDataCollection.add(item);
            }
            if (dataObbSizeInfo.obb > 0) {
                containsObbCollection.add(item);
            }
            data += dataObbSizeInfo.data;
            obb += dataObbSizeInfo.obb;
        }
        final DataObbSizeInfo info = new DataObbSizeInfo(data, obb);
        if (!isInterrupted) Global.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onDataObbSizeGet(containsDataCollection, containsObbCollection, info);
                }
            }
        });
    }

    public void setInterrupted() {
        isInterrupted = true;
    }

    public static void clearDataObbSizeCache() {
        cache_data_obb_size.clear();
    }

    private DataObbSizeInfo getDataObbSizeInfo(AppItem item) {
        long data = 0, obb = 0;
        if (Build.VERSION.SDK_INT < 30) {
            data = FileUtil.getFileOrFolderSize(new File(StorageUtil.getMainExternalStoragePath() + "/android/data/" + item.getPackageName()));
            obb = FileUtil.getFileOrFolderSize(new File(StorageUtil.getMainExternalStoragePath() + "/android/obb/" + item.getPackageName()));
        } else {
            if (DocumentFileUtil.canReadDataPathByDocumentFile()) {
                try {
                    data = FileUtil.getFileItemSize(new FileItem(DocumentFileUtil.getDocumentFileBySegments(DocumentFileUtil.getDataDocumentFile(), "" + item.getPackageName())));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (DocumentFileUtil.canReadObbPathByDocumentFile()) {
                try {
                    obb = FileUtil.getFileItemSize(new FileItem(DocumentFileUtil.getDocumentFileBySegments(DocumentFileUtil.getObbDocumentFile(), "" + item.getPackageName())));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return new DataObbSizeInfo(data, obb);
    }
}
