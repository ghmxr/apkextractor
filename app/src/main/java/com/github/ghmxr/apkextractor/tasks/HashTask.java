package com.github.ghmxr.apkextractor.tasks;

import android.support.annotation.NonNull;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.FileUtil;

import java.util.HashMap;

public class HashTask extends Thread {

    private static final HashMap<FileItem, String> md5_cache = new HashMap<>();
    private static final HashMap<FileItem, String> sha1_cache = new HashMap<>();
    private static final HashMap<FileItem, String> sha256_cache = new HashMap<>();
    private static final HashMap<FileItem, String> crc32_cache = new HashMap<>();

    private final FileItem fileItem;
    private final HashType hashType;
    private final CompletedCallback callback;

    public enum HashType {
        MD5, SHA1, SHA256, CRC32
    }

    public HashTask(@NonNull FileItem fileItem, @NonNull HashType hashType, @NonNull CompletedCallback callback) {
        this.fileItem = fileItem;
        this.hashType = hashType;
        this.callback = callback;
    }

    @Override
    public void run() {
        super.run();
        String result = null;
        switch (hashType) {
            case MD5: {
                synchronized (md5_cache) {
                    if (md5_cache.get(fileItem) != null) {
                        result = md5_cache.get(fileItem);
                    } else {
                        try {
                            result = EnvironmentUtil.hashMD5Value(fileItem.getInputStream());
                            md5_cache.put(fileItem, result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            break;
            case SHA1: {
                synchronized (sha1_cache) {
                    if (sha1_cache.get(fileItem) != null) {
                        result = sha1_cache.get(fileItem);
                    } else {
                        try {
                            result = EnvironmentUtil.hashSHA1Value(fileItem.getInputStream());
                            sha1_cache.put(fileItem, result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
            break;
            case SHA256: {
                synchronized (sha256_cache) {
                    if (sha256_cache.get(fileItem) != null) {
                        result = sha256_cache.get(fileItem);
                    } else {
                        try {
                            result = EnvironmentUtil.hashSHA256Value(fileItem.getInputStream());
                            sha256_cache.put(fileItem, result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            break;
            case CRC32: {
                synchronized (crc32_cache) {
                    if (crc32_cache.get(fileItem) != null) {
                        result = crc32_cache.get(fileItem);
                    } else {
                        try {
                            result = Integer.toHexString((int) FileUtil.getCRC32FromInputStream(fileItem.getInputStream()).getValue());
                            crc32_cache.put(fileItem, result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            break;
            default:
                break;
        }

        final String result_final = String.valueOf(result);
        Global.handler.post(new Runnable() {
            @Override
            public void run() {
                callback.onHashCompleted(result_final);
            }
        });
    }

    public interface CompletedCallback {
        void onHashCompleted(@NonNull String result);
    }

    static synchronized void clearResultCache() {
        md5_cache.clear();
        sha1_cache.clear();
        sha256_cache.clear();
        crc32_cache.clear();
    }
}
