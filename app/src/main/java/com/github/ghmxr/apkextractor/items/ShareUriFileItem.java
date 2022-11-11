package com.github.ghmxr.apkextractor.items;

import android.content.ContentResolver;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.github.ghmxr.apkextractor.MyApplication;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class ShareUriFileItem extends FileItem {

    private final Uri contentUri;

    public ShareUriFileItem(Uri contentUri) {
        this.contentUri = contentUri;
    }

    @Override
    public String getName() {
        if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(contentUri.getScheme())) {
            return contentUri.getLastPathSegment();
        }
        String nameQueried = EnvironmentUtil.getFileNameFromContentUri(MyApplication.getApplication(), contentUri);
        if (!TextUtils.isEmpty(nameQueried)) return nameQueried;
        String pathQueried = EnvironmentUtil.getFilePathFromContentUri(MyApplication.getApplication(), contentUri);
        if (pathQueried != null && !TextUtils.isEmpty(pathQueried)) {
            return new File(pathQueried).getName();
        }
        try {
            DocumentFile documentFile = DocumentFile.fromSingleUri(MyApplication.getApplication(), contentUri);
            if (documentFile != null) {
                String fileName = documentFile.getName();
                if (!TextUtils.isEmpty(fileName)) {
                    return fileName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "unknown.file";
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean exists() {
        return length() > 0L;
    }

    @Override
    public boolean renameTo(@NonNull String newName) throws Exception {
        return false;
    }

    @Override
    public boolean canGetRealPath() {
        return !TextUtils.isEmpty(EnvironmentUtil.getFilePathFromContentUri(MyApplication.getApplication(), contentUri));
    }

    @Override
    public String getPath() {
        if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(contentUri.getScheme())) {
            return contentUri.getPath();
        }
        String path = EnvironmentUtil.getFilePathFromContentUri(MyApplication.getApplication(), contentUri);
        if (!TextUtils.isEmpty(path)) return path;
        return contentUri.toString();
    }

    @Override
    public boolean delete() {
        return false;
    }

    @NonNull
    @Override
    public List<FileItem> listFileItems() {
        return new ArrayList<>();
    }

    @Override
    public long length() {
        try {
            if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(contentUri.getScheme())) {
                return new File(contentUri.getPath()).length();
            }
            String length = EnvironmentUtil.getFileLengthFromContentUri(MyApplication.getApplication(), contentUri);
            if (length != null && !TextUtils.isEmpty(length)) return Long.parseLong(length);
            InputStream inputStream = getInputStream();
            int available = inputStream.available();
            inputStream.close();
            return available;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
    }

    @Override
    public long lastModified() {
        return 0L;
    }

    @Nullable
    @Override
    public FileItem getParent() {
        return null;
    }

    @Override
    public InputStream getInputStream() throws Exception {
        if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(contentUri.getScheme())) {
            return new FileInputStream(contentUri.getPath());
        }
        return MyApplication.getApplication().getContentResolver().openInputStream(contentUri);
    }

    @Override
    public OutputStream getOutputStream() throws Exception {
        return MyApplication.getApplication().getContentResolver().openOutputStream(contentUri);
    }

    @Override
    public boolean isShareUriInstance() {
        return true;
    }

    @Override
    public Uri getContentUri() {
        return contentUri;
    }

    @NonNull
    @Override
    public String toString() {
        return contentUri.toString();
    }
}
