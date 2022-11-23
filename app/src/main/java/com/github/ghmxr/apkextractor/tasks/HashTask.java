package com.github.ghmxr.apkextractor.tasks;

import androidx.annotation.NonNull;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.FileUtil;

import java.util.concurrent.ConcurrentHashMap;

import static com.github.ghmxr.apkextractor.utils.CommonUtil.removeKeyFromMapIgnoreCase;

public class HashTask extends Thread {

    private static final ConcurrentHashMap<String, String> md5_cache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> sha1_cache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> sha256_cache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> crc32_cache = new ConcurrentHashMap<>();

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
                if (md5_cache.get(fileItem.getPath()) != null) {
                    result = md5_cache.get(fileItem.getPath());
                } else {
                    try {
                        result = EnvironmentUtil.hashMD5Value(fileItem.getInputStream());
                        md5_cache.put(fileItem.getPath(), result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            break;
            case SHA1: {
                if (sha1_cache.get(fileItem.getPath()) != null) {
                    result = sha1_cache.get(fileItem.getPath());
                } else {
                    try {
                        result = EnvironmentUtil.hashSHA1Value(fileItem.getInputStream());
                        sha1_cache.put(fileItem.getPath(), result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
            break;
            case SHA256: {
                if (sha256_cache.get(fileItem.getPath()) != null) {
                    result = sha256_cache.get(fileItem.getPath());
                } else {
                    try {
                        result = EnvironmentUtil.hashSHA256Value(fileItem.getInputStream());
                        sha256_cache.put(fileItem.getPath(), result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            break;
            case CRC32: {
                if (crc32_cache.get(fileItem.getPath()) != null) {
                    result = crc32_cache.get(fileItem.getPath());
                } else {
                    try {
                        result = Integer.toHexString((int) FileUtil.getCRC32FromInputStream(fileItem.getInputStream()).getValue());
                        crc32_cache.put(fileItem.getPath(), result);
                    } catch (Exception e) {
                        e.printStackTrace();
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

    public static void clearResultCache() {
        md5_cache.clear();
        sha1_cache.clear();
        sha256_cache.clear();
        crc32_cache.clear();
    }

    public static void clearResultCacheOfPath(String path) {
        removeKeyFromMapIgnoreCase(md5_cache, path);
        removeKeyFromMapIgnoreCase(sha1_cache, path);
        removeKeyFromMapIgnoreCase(sha256_cache, path);
        removeKeyFromMapIgnoreCase(crc32_cache, path);
    }

}
