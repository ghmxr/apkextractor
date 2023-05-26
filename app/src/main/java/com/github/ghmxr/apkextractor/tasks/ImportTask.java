package com.github.ghmxr.apkextractor.tasks;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.PermissionChecker;
import androidx.documentfile.provider.DocumentFile;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.DocumentFileUtil;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.OutputUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ImportTask extends Thread {

    private final ArrayList<ImportItem> importItemArrayList = new ArrayList<>();
    private final Context context;
    private volatile boolean isInterrupted = false;
    //private ArrayList<String>write_paths=new ArrayList<>();
    private String currentWritePath;
    private FileItem currentWrtingFileItem;
    private long progress = 0;
    private long progress_check_length = 0;
    private long speed_bytes = 0;
    private long speed_check_time = 0;
    private final boolean isExternal;
    private ImportItem currentWritingApk;
    private final LinkedList<ImportItem> apkItems = new LinkedList<>();

    //    @Deprecated private final StringBuilder error_info = new StringBuilder();
    private final ArrayList<String> error_info_list = new ArrayList<>();

    private final ImportTaskCallback callback;
    private Uri apkUri;
    private int apk_num = 0;

    public ImportTask(@NonNull Context context, @NonNull List<ImportItem> importItems, ImportTaskCallback callback) {
        super();
        this.context = context;
        importItemArrayList.addAll(importItems);
        this.callback = callback;
        isExternal = SPUtil.getIsSaved2ExternalStorage(context);
    }

    @Override
    public void run() {
        super.run();
        for (ImportItem importItem : importItemArrayList) {
            if (isInterrupted) break;
            if (importItem.getImportType() != ImportItem.ImportType.ZIP) continue;
            boolean clearedCache = false;
            try {
                final boolean canAccessByZipFile = importItem.getFileItem().isFileInstance();
                if (canAccessByZipFile) {
                    ZipFile zipFile = new ZipFile(importItem.getFileItem().getFile());
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    ZipEntry zipEntry;
                    while (entries.hasMoreElements() && !isInterrupted) {
                        zipEntry = entries.nextElement();
                        try {
                            InputStream zipInputStream = zipFile.getInputStream(zipEntry);
                            dealWithZipEntry(importItem, zipEntry, zipInputStream);
                            if (!clearedCache) {
                                if (clearDataObbCacheForZipEntry(zipEntry)) {
                                    clearedCache = true;
                                }
                            }
                            zipInputStream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            putErrorLogsAndClear(e);
                        }
                    }
                    zipFile.close();
                } else {
                    ZipInputStream zipInputStream = new ZipInputStream(importItem.getFileItem().getInputStream());
                    ZipEntry zipEntry = zipInputStream.getNextEntry();
                    while (zipEntry != null && !isInterrupted) {
                        try {
                            dealWithZipEntry(importItem, zipEntry, zipInputStream);
                            if (!clearedCache) {
                                if (clearDataObbCacheForZipEntry(zipEntry)) {
                                    clearedCache = true;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            putErrorLogsAndClear(e);
                        }
                        if (!isInterrupted) zipEntry = zipInputStream.getNextEntry();
                        else break;
                    }
                    zipInputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();

                error_info_list.add(importItem.getFileItem().getPath() + ":" + e);
            }

        }
        if (isInterrupted) {
            try {
                if (currentWrtingFileItem != null) {
                    currentWrtingFileItem.delete();
                }
                if (currentWritingApk != null) {
                    apkItems.remove(currentWritingApk);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            //向全局列表添加本次导出的apk的引用内容，以避免重新刷新递归扫描
            Global.item_list.addAll(apkItems);
            Collections.sort(Global.item_list);

            if (callback != null) Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onImportTaskFinished(error_info_list);
//                context.sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
                    context.sendBroadcast(new Intent(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE));
                    context.sendBroadcast(new Intent(Constants.ACTION_REFILL_IMPORT_LIST));
                    try {
                        if (importItemArrayList.size() == 1 && apkUri != null && error_info_list.size() == 0) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                            context.startActivity(intent);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        ToastManager.showToast(context, e.toString(), Toast.LENGTH_SHORT);
                    }
                }
            });
        }
    }

    private void dealWithZipEntry(ImportItem importItem, ZipEntry zipEntry, InputStream zipInputStream) throws Exception {
        String entryPath = zipEntry.getName().replace("\\", "/");
        if ((entryPath.toLowerCase().startsWith("android/data")) && !zipEntry.isDirectory() && importItem.importData) {
            unZipToFile(zipInputStream, entryPath);
        } else if ((entryPath.toLowerCase().startsWith("android/obb")) && !zipEntry.isDirectory() && importItem.importObb) {
            unZipToFile(zipInputStream, entryPath);
        } else if ((entryPath.toLowerCase().endsWith(".apk")) && !zipEntry.isDirectory() && !entryPath.contains("/") && importItem.importApk) {
            OutputStream outputStream;
            final String fileName = entryPath.substring(entryPath.lastIndexOf("/") + 1);
            if (isExternal) {
                String writeFileName = getApkFileNameWithNum(fileName);
                DocumentFile checkFile = DocumentFileUtil.findDocumentFile(OutputUtil.getExportPathDocumentFile(context), writeFileName);
                while (checkFile != null && checkFile.exists()) {
                    apk_num++;
                    writeFileName = getApkFileNameWithNum(fileName);
                    checkFile = DocumentFileUtil.findDocumentFile(OutputUtil.getExportPathDocumentFile(context), writeFileName);
                }
                DocumentFile writeDocumentFile = OutputUtil.getExportPathDocumentFile(context)
                        .createFile("application/vnd.android.package-archive", writeFileName);
                outputStream = OutputUtil.getOutputStreamForDocumentFile(context
                        , writeDocumentFile);
                currentWritePath = SPUtil.getDisplayingExportPath(context) + "/" + writeFileName;
                currentWrtingFileItem = FileItem.createFileItemInstance(writeDocumentFile);
                apkUri = writeDocumentFile.getUri();
                postProgressToCallback();
            } else {
                String writePath = SPUtil.getInternalSavePath(context) + "/" + getApkFileNameWithNum(fileName);
                File writeFile = new File(writePath);
                while (writeFile.exists()) {
                    apk_num++;
                    writeFile = new File(SPUtil.getInternalSavePath(context) + "/" + getApkFileNameWithNum(fileName));
                }
                currentWritePath = writeFile.getAbsolutePath();
                currentWrtingFileItem = FileItem.createFileItemInstance(writeFile);
                postProgressToCallback();
                File export_path = new File(SPUtil.getInternalSavePath(context));
                if (!export_path.exists()) {
                    export_path.mkdirs();
                }
                outputStream = new FileOutputStream(writeFile);
                if (Build.VERSION.SDK_INT <= 23) apkUri = Uri.fromFile(writeFile);
                else
                    apkUri = EnvironmentUtil.getUriForFileByFileProvider(context, writeFile);
            }
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = zipInputStream.read(buffer)) != -1 && !isInterrupted) {
                bufferedOutputStream.write(buffer, 0, len);
                progress += len;
                checkSpeedAndPostToCallback(len);
                checkProgressAndPostToCallback();
            }
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            currentWritingApk = new ImportItem(currentWrtingFileItem);
            apkItems.add(currentWritingApk);
            if (!isInterrupted) {
                currentWrtingFileItem = null;
                currentWritingApk = null;
            }
        }


    }


    private void putErrorLogsAndClear(Exception e) {
        error_info_list.add(currentWritePath + ":" + e);

        try {
            currentWrtingFileItem.delete();
            if (currentWritingApk != null) {
                apkItems.remove(currentWritingApk);
            }
        } catch (Exception ee) {
        }
    }

    private void unZipToFile(InputStream zipInputStream, String entryPath) throws Exception {
        String writePath = StorageUtil.getMainExternalStoragePath() + "/" + entryPath;
        currentWritePath = writePath;
        OutputStream outputStream;
        postProgressToCallback();
        if (Build.VERSION.SDK_INT < Global.USE_DOCUMENT_FILE_SDK_VERSION && PermissionChecker.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED) {
            File writeFile = new File(writePath);
            currentWrtingFileItem = FileItem.createFileItemInstance(writeFile);
            File folder = new File(StorageUtil.getMainExternalStoragePath() + "/" + entryPath.substring(0, entryPath.lastIndexOf("/")));
            if (!folder.exists()) folder.mkdirs();
            outputStream = new BufferedOutputStream(new FileOutputStream(writeFile));
        } else if (Build.VERSION.SDK_INT < Global.USE_STANDALONE_DOCUMENT_FILE_PERMISSION) {
            final String formattedEntryPath = entryPath.toLowerCase();
            DocumentFile writingParentDocumentFile = null;
            String segments = null;
            if (formattedEntryPath.startsWith("android/data/")) {
                segments = entryPath.substring("android/data/".length(), entryPath.lastIndexOf("/"));
                writingParentDocumentFile = DocumentFileUtil.getDataDocumentFile();
            }
            if (formattedEntryPath.startsWith("android/obb/")) {
                segments = entryPath.substring("android/obb/".length(), entryPath.lastIndexOf("/"));
                writingParentDocumentFile = DocumentFileUtil.getObbDocumentFile();
            }
            if (writingParentDocumentFile == null || TextUtils.isEmpty(segments)) return;

            final String fileName = entryPath.substring(entryPath.lastIndexOf("/") + 1);

            DocumentFile documentFileBySegments = DocumentFileUtil.getDocumentFileBySegments(writingParentDocumentFile, segments);
            DocumentFile check_documentFile = DocumentFileUtil.findDocumentFile(documentFileBySegments, fileName);
            if (check_documentFile != null) {
                check_documentFile.delete();
            }

            DocumentFile writingDocumentFile = documentFileBySegments.createFile("", fileName);

            if (writingDocumentFile == null) {
                throw new RuntimeException("Can not obtain DocumentFile instance for " + DocumentFileUtil.getDisplayPathForDataObbDocumentFile(writingParentDocumentFile) + "/" + segments);
            }
            currentWritePath = DocumentFileUtil.getDisplayPathForDataObbDocumentFile(writingDocumentFile);
            currentWrtingFileItem = FileItem.createFileItemInstance(writingDocumentFile);
            outputStream = OutputUtil.getOutputStreamForDocumentFile(context, writingDocumentFile);
        } else {

            final String formattedEntryPath = entryPath.toLowerCase();
            final String packageName = EnvironmentUtil.getResolvedPackageNameOfEntryPath(formattedEntryPath);
            DocumentFile writingParentDocumentFile = null;
            String segments = null;
            if (formattedEntryPath.startsWith("android/data/")) {
                try {
                    segments = entryPath.substring(("android/data/" + packageName + "/").length(), entryPath.lastIndexOf("/"));
                } catch (Exception e) {
                    //
                }
                writingParentDocumentFile = DocumentFileUtil.getDataDocumentFileOf(packageName);
            }
            if (formattedEntryPath.startsWith("android/obb/")) {
                try {
                    segments = entryPath.substring(("android/obb/" + packageName + "/").length(), entryPath.lastIndexOf("/"));
                } catch (Exception e) {
                    //
                }
                writingParentDocumentFile = DocumentFileUtil.getObbDocumentFileOf(packageName);
            }

            final String fileName = entryPath.substring(entryPath.lastIndexOf("/") + 1);

            DocumentFile documentFileBySegments = DocumentFileUtil.getDocumentFileBySegments(writingParentDocumentFile, segments);
            DocumentFile check_documentFile = DocumentFileUtil.findDocumentFile(documentFileBySegments, fileName);
            if (check_documentFile != null) {
                check_documentFile.delete();
            }

            DocumentFile writingDocumentFile = documentFileBySegments.createFile("", fileName);

            if (writingDocumentFile == null) {
                throw new RuntimeException("Can not obtain DocumentFile instance for " + DocumentFileUtil.getDisplayPathForDataObbDocumentFile(writingParentDocumentFile)
                        + (segments == null ? "" : "/" + segments) + "/" + fileName);
            }
            currentWritePath = DocumentFileUtil.getDisplayPathForDataObbDocumentFile(writingDocumentFile);
            currentWrtingFileItem = FileItem.createFileItemInstance(writingDocumentFile);
            outputStream = OutputUtil.getOutputStreamForDocumentFile(context, writingDocumentFile);
        }

        if (outputStream == null) {
            throw new RuntimeException("Can not obtain output stream instance, please check writing permission");
        }

        byte[] buffer = new byte[1024];
        int len;
        while ((len = zipInputStream.read(buffer)) != -1 && !isInterrupted) {
            outputStream.write(buffer, 0, len);
            progress += len;
            checkSpeedAndPostToCallback(len);
            checkProgressAndPostToCallback();
        }
        outputStream.flush();
        outputStream.close();
        if (!isInterrupted) currentWrtingFileItem = null;
    }

    private boolean clearDataObbCacheForZipEntry(ZipEntry zipEntry) {
        try {
            String entryPath = zipEntry.getName().replace("\\", "/");
            String replaced_entry_path = null;
            if (entryPath.toLowerCase().startsWith("android/data/")) {
                replaced_entry_path = entryPath.substring("android/data/".length());
            } else if (entryPath.toLowerCase().startsWith("android/obb/")) {
                replaced_entry_path = entryPath.substring("android/obb/".length());
            }
            if (replaced_entry_path == null || !replaced_entry_path.contains("/")) return false;
            final String packageName = replaced_entry_path.substring(0, replaced_entry_path.indexOf("/"));
            return GetDataObbTask.removeDataObbSizeCache(packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void checkProgressAndPostToCallback() {
        if (progress - progress_check_length > 100 * 1024) {
            progress_check_length = progress;
            postProgressToCallback();
        }
    }

    private void postProgressToCallback() {
        if (callback != null) Global.handler.post(new Runnable() {
            @Override
            public void run() {
                callback.onImportTaskProgress(currentWritePath, progress);
            }
        });
    }

    private void checkSpeedAndPostToCallback(long speed_plus_value) {
        speed_bytes += speed_plus_value;
        long current = System.currentTimeMillis();
        if (current - speed_check_time > 1000) {
            speed_check_time = current;
            final long speed_post = speed_bytes;
            speed_bytes = 0;
            if (callback != null) Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onRefreshSpeed(speed_post);
                }
            });
        }
    }

    public void setInterrupted() {
        this.isInterrupted = true;
    }

    private String getApkFileNameWithNum(String originName) {
        return originName.substring(0, originName.lastIndexOf(".")) + (apk_num > 0 ? apk_num : "") + ".apk";
    }

    public interface ImportTaskCallback {
        void onImportTaskStarted();

        void onRefreshSpeed(long speed);

        void onImportTaskProgress(@NonNull String writePath, long progress);

        void onImportTaskFinished(@NonNull List<String> errorMessages);
    }
}
