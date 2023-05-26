package com.github.ghmxr.apkextractor.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.MyApplication;
import com.github.ghmxr.apkextractor.R;

public class DocumentFileUtil {


    /**
     * 通过segment片段定位到parent的指定文件夹，如果没有则尝试创建
     */
    @NonNull
    public static DocumentFile getDocumentFileBySegments(@NonNull DocumentFile parent, @Nullable String segment) throws Exception {
        return getDocumentFileBySegments(parent, segment, true);
    }

    /**
     * 通过segment片段定位到parent的指定文件夹
     */
    @NonNull
    public static DocumentFile getDocumentFileBySegments(@NonNull DocumentFile parent, @Nullable String segment, boolean create) throws Exception {
        if (segment == null) return parent;
        String[] segments = segment.split("/");
        DocumentFile documentFile = parent;
        for (int i = 0; i < segments.length; i++) {
            DocumentFile lookup = findDocumentFile(documentFile, segments[i]);
            if (lookup == null) {
                if (create) lookup = documentFile.createDirectory(segments[i]);
                else throw new Exception("can not find path " + segment);
            }
            if (lookup == null) {
                throw new Exception("Can not create folder " + segments[i]);
            }
            documentFile = lookup;
        }
        return documentFile;
    }

    public static @Nullable
    DocumentFile findDocumentFile(@NonNull DocumentFile parent, @NonNull String fileName) {
        try {
            DocumentFile result = parent.findFile(fileName);
            if (result != null) return result;
            DocumentFile[] list = parent.listFiles();
            for (DocumentFile d : list) {
                if (fileName.equalsIgnoreCase(d.getName())) {
                    return d;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将segments数组转换为string
     */
    public static @NonNull
    String toSegmentString(@NonNull Object[] segments) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            builder.append(segments[i]);
            if (i < segments.length - 1) builder.append("/");
        }
        return builder.toString();
    }

    /**
     * 测试是否有写入外置存储的权限
     */
    public static boolean canWrite2ExternalStorage(@NonNull Context context) {
        SharedPreferences settings = SPUtil.getGlobalSharedPreferences(context);
        String uri_value = settings.getString(Constants.PREFERENCE_SAVE_PATH_URI, "");
        if ("".equals(uri_value)) return false;
        try {
            return DocumentFile.fromTreeUri(context, Uri.parse(uri_value)).canWrite();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取一个documentFile用于展示的路径
     */
    public static @NonNull
    String getDisplayPathForDocumentFile(@NonNull Context context, @NonNull DocumentFile documentFile) {
        String uriPath = documentFile.getUri().getPath();
        if (uriPath == null) return "";
        int index = uriPath.lastIndexOf(":") + 1;
        if (index <= uriPath.length())
            return context.getResources().getString(R.string.external_storage) + "/" + uriPath.substring(index);
        return "";
    }

    public static String getDisplayPathForDataObbDocumentFile(DocumentFile documentFile) {
        final String uriPath = documentFile.getUri().getPath();
        return StorageUtil.getMainExternalStoragePath() + "/" + uriPath.substring(uriPath.lastIndexOf(":") + 1);
    }

    public static boolean canReadDataPathByDocumentFile() {
        try {
            return getDataDocumentFile().canRead();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean canWriteDataPathByDocumentFile() {
        try {
            return getDataDocumentFile().canWrite();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean canReadObbPathByDocumentFile() {
        try {
            return getObbDocumentFile().canRead();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean canWriteObbPathByDocumentFile() {
        try {
            return getObbDocumentFile().canWrite();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static volatile DocumentFile dataDocumentFile;

    public static DocumentFile getDataDocumentFile() {
        if (dataDocumentFile == null) {
            synchronized (DocumentFileUtil.class) {
                if (dataDocumentFile == null) {
                    dataDocumentFile = DocumentFile.fromTreeUri(MyApplication.getApplication(), Uri.parse(Global.URI_DATA));
                }
            }
        }
        return dataDocumentFile;
    }

    private static volatile DocumentFile obbDocumentFile;

    public static DocumentFile getObbDocumentFile() {
        if (obbDocumentFile == null) {
            synchronized (DocumentFileUtil.class) {
                if (obbDocumentFile == null) {
                    obbDocumentFile = DocumentFile.fromTreeUri(MyApplication.getApplication(), Uri.parse(Global.URI_OBB));
                }
            }
        }
        return obbDocumentFile;
    }

    public static boolean canRWDataDocumentFileOf(String packageName) {
        try {
            DocumentFile documentFile = getDataDocumentFileOf(packageName);
            return documentFile.canRead() && documentFile.canWrite();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean canRWObbDocumentFileOf(String packageName) {
        try {
            DocumentFile documentFile = getObbDocumentFileOf(packageName);
            return documentFile.canRead() && documentFile.canWrite();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static DocumentFile getDataDocumentFileOf(String packageName) throws Exception {
        return DocumentFile.fromTreeUri(MyApplication.getApplication(), Uri.parse(Global.URI_DATA + "%2F" + packageName));
    }

    public static DocumentFile getObbDocumentFileOf(String packageName) throws Exception {
        return DocumentFile.fromTreeUri(MyApplication.getApplication(), Uri.parse(Global.URI_OBB + "%2F" + packageName));
    }
}
