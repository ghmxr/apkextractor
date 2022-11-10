package com.github.ghmxr.apkextractor.items;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.github.ghmxr.apkextractor.utils.PinyinUtil;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public abstract class FileItem implements Comparable<FileItem> {

    public static int sort_config = 0;

    public abstract String getName();

    public abstract boolean isFile();

    public abstract boolean isDirectory();

    public abstract boolean exists();

    public abstract boolean renameTo(@NonNull String newName) throws Exception;

    public boolean mkdirs() {
        return false;
    }

    @Nullable
    public DocumentFile createDirectory(@NonNull String name) {
        return null;
    }

    public boolean isDocumentFile() {
        return false;
    }

    public boolean isFileInstance() {
        return false;
    }

    public boolean isShareUriInstance() {
        return false;
    }

    public abstract boolean canGetRealPath();

    public abstract String getPath();

    public abstract boolean delete();

    @NonNull
    public abstract List<FileItem> listFileItems();

    public abstract long length();

    public abstract long lastModified();

    @Nullable
    public abstract FileItem getParent();

    public boolean isHidden() {
        return false;
    }

    public abstract InputStream getInputStream() throws Exception;

    public abstract OutputStream getOutputStream() throws Exception;

    public DocumentFile getDocumentFile() {
        return null;
    }

    public File getFile() {
        return null;
    }

    public Uri getContentUri() {
        return null;
    }

    public static synchronized void setSort_config(int value) {
        sort_config = value;
    }

    @Override
    public int compareTo(FileItem o) {
        switch (sort_config) {
            default:
                break;
            case 0: {
                try {
                    if (PinyinUtil.getFirstSpell(String.valueOf(this.getName())).toLowerCase().compareTo(PinyinUtil.getFirstSpell(String.valueOf(o.getName())).toLowerCase()) > 0)
                        return 1;
                    if (PinyinUtil.getFirstSpell(String.valueOf(this.getName())).toLowerCase().compareTo(PinyinUtil.getFirstSpell(String.valueOf(o.getName())).toLowerCase()) < 0)
                        return -1;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;
            case 1: {
                try {
                    if (PinyinUtil.getFirstSpell(String.valueOf(this.getName())).toLowerCase().compareTo(PinyinUtil.getFirstSpell(String.valueOf(o.getName())).toLowerCase()) > 0)
                        return -1;
                    if (PinyinUtil.getFirstSpell(String.valueOf(this.getName())).toLowerCase().compareTo(PinyinUtil.getFirstSpell(String.valueOf(o.getName())).toLowerCase()) < 0)
                        return 1;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;
        }
        return 0;
    }

    @NonNull
    @Override
    public abstract String toString();

    public static FileItem createFileItemInstance(String path) {
        return new StandardFileItem(path);
    }

    public static FileItem createFileItemInstance(File file) {
        return new StandardFileItem(file);
    }

    public static FileItem createFileItemInstance(Uri treeUri, String segments) throws Exception {
        return new DocumentFileItem(treeUri, segments);
    }

    public static FileItem createFileItemInstance(DocumentFile documentFile) {
        return new DocumentFileItem(documentFile);
    }

    public static FileItem createFileItemInstance(Uri contentUri) {
        return new ShareUriFileItem(contentUri);
    }
}
