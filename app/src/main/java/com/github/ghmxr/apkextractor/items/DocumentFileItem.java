package com.github.ghmxr.apkextractor.items;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.github.ghmxr.apkextractor.MyApplication;
import com.github.ghmxr.apkextractor.utils.DocumentFileUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class DocumentFileItem extends FileItem {

    private final DocumentFile documentFile;

    protected DocumentFileItem(Uri treeUri, String segments) throws Exception {
        DocumentFile documentFile = DocumentFile.fromTreeUri(MyApplication.getApplication(), treeUri);
        if (documentFile == null) throw new Exception("Can not get documentFile by the treeUri");
        this.documentFile = DocumentFileUtil.getDocumentFileBySegments(documentFile, segments);
    }

    protected DocumentFileItem(DocumentFile documentFile) {
        this.documentFile = documentFile;
    }

    @Override
    public String getName() {
        return documentFile.getName();
    }

    @Override
    public boolean isFile() {
        return documentFile.isFile();
    }

    @Override
    public boolean isDirectory() {
        return documentFile.isDirectory();
    }

    @Override
    public boolean exists() {
        return documentFile.exists();
    }

    @Override
    public boolean renameTo(@NonNull String newName) {
        return documentFile.renameTo(newName);
    }

    @Override
    public boolean canGetRealPath() {
        return true;
    }

    @Override
    public String getPath() {
        String uriPath = documentFile.getUri().getPath();
        if (uriPath == null || uriPath.isEmpty()) return "";
        return "external/" + uriPath.substring(uriPath.lastIndexOf(":") + 1);
    }

    @Override
    public boolean delete() {
        return documentFile.delete();
    }

    @NonNull
    @Override
    public List<FileItem> listFileItems() {
        ArrayList<FileItem> arrayList = new ArrayList<>();
        try {
            DocumentFile[] documentFiles = documentFile.listFiles();
            for (DocumentFile documentFile : documentFiles) {
                arrayList.add(new DocumentFileItem(documentFile));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return arrayList;
    }

    @Override
    public long length() {
        return documentFile.length();
    }

    @Override
    public long lastModified() {
        return documentFile.lastModified();
    }

    @Nullable
    @Override
    public FileItem getParent() {
        DocumentFile parent = documentFile.getParentFile();
        if (parent != null) {
            return new DocumentFileItem(parent);
        }
        return null;
    }

    @Override
    public InputStream getInputStream() throws Exception {
        return MyApplication.getApplication().getContentResolver().openInputStream(documentFile.getUri());
    }

    @Override
    public OutputStream getOutputStream() throws Exception {
        return MyApplication.getApplication().getContentResolver().openOutputStream(documentFile.getUri());
    }

    @Nullable
    @Override
    public DocumentFile createDirectory(@NonNull String name) {
        return documentFile.createDirectory(name);
    }

    @Override
    public boolean isDocumentFile() {
        return true;
    }

    @Override
    public DocumentFile getDocumentFile() {
        return documentFile;
    }

    @NonNull
    @Override
    public String toString() {
        return documentFile.getUri().toString();
    }
}
