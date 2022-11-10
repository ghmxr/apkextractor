package com.github.ghmxr.apkextractor.items;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class StandardFileItem extends FileItem {
    private File file;

    protected StandardFileItem(String path) {
        this(new File(path));
    }

    protected StandardFileItem(File file) {
        this.file = file;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public boolean renameTo(@NonNull String newName) throws Exception {
        final File destFile = new File(file.getParentFile(), newName);
        if (destFile.exists()) {
            throw new Exception(destFile.getAbsolutePath() + " already exists");
        }
        if (file.renameTo(destFile)) {
            file = destFile;
            return true;
        } else {
            throw new Exception("error renaming to " + destFile.getAbsolutePath());
        }
    }

    @Override
    public boolean canGetRealPath() {
        return true;
    }

    @Override
    public String getPath() {
        return file.getAbsolutePath();
    }

    @Override
    public boolean delete() {
        return file.delete();
    }

    @NonNull
    @Override
    public List<FileItem> listFileItems() {
        ArrayList<FileItem> arrayList = new ArrayList<>();
        try {
            File[] files = file.listFiles();
            if (files == null) return arrayList;
            for (File f : files) arrayList.add(new StandardFileItem(f));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrayList;
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Nullable
    @Override
    public FileItem getParent() {
        File parent = file.getParentFile();
        if (parent != null) {
            return new StandardFileItem(parent);
        }
        return null;
    }

    @Override
    public InputStream getInputStream() throws Exception {
        return new FileInputStream(file);
    }

    @Override
    public OutputStream getOutputStream() throws Exception {
        return new FileOutputStream(file);
    }

    @Override
    public boolean isFileInstance() {
        return true;
    }

    @Override
    public boolean mkdirs() {
        return file.mkdirs();
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public boolean isHidden() {
        return file.isHidden();
    }

    @NonNull
    @Override
    public String toString() {
        return file.getAbsolutePath();
    }
}
