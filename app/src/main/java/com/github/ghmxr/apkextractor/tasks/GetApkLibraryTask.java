package com.github.ghmxr.apkextractor.tasks;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.items.FileItem;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class GetApkLibraryTask extends Thread {

    private static final ConcurrentHashMap<String, LibraryInfo> caches_installed = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LibraryInfo> caches_outside_package = new ConcurrentHashMap<>();

    public static class LibraryInfo {
        public enum LibraryType {
            ARM("armeabi"),
            ARM_V7A("armeabi-v7a"),
            ARM64_V8A("arm64-v8a"),
            X86("x86"),
            X86_64("x86_64"),
            MIPS("mips"),
            MIPS_64("mips64");
            String name;

            LibraryType(String s) {
                name = s;
            }

            public String getName() {
                return name;
            }
        }

        public final HashMap<LibraryType, Collection<String>> libraries = new HashMap<>();

        public Collection<LibraryType> getLibraryTypes(String libraryName) {
            final HashSet<LibraryType> libraryTypes = new HashSet<>();
            for (LibraryType type : LibraryType.values()) {
                final Collection<String> collection = libraries.get(type);
                if (collection != null && collection.contains(libraryName)) {
                    libraryTypes.add(type);
                }
            }

            return libraryTypes;
        }

        public Collection<String> getAllLibraryNames() {
            HashSet<String> hashSet = new HashSet<>();
            for (Collection<String> c : libraries.values()) {
                hashSet.addAll(c);
            }
            return hashSet;
        }

        public static boolean is64BitAbi(LibraryType type) {
            return type == LibraryType.ARM64_V8A || type == LibraryType.X86_64 || type == LibraryType.MIPS_64;
        }

    }

    public interface GetApkLibraryCallback {
        void onApkLibraryGet(LibraryInfo libraryInfo);
    }

    AppItem appItem;
    FileItem apkFile;
    final GetApkLibraryCallback callback;

    public GetApkLibraryTask(AppItem appItem, GetApkLibraryCallback callback) {
        this.appItem = appItem;
        this.callback = callback;
    }

    public GetApkLibraryTask(FileItem apkFile, GetApkLibraryCallback callback) {
        this.apkFile = apkFile;
        this.callback = callback;
    }

    @Override
    public void run() {
        super.run();
        LibraryInfo libraryInfo = null;
        if (appItem != null) {
            libraryInfo = caches_installed.get(appItem.getSourcePath());
            if (libraryInfo == null) {
                try {
                    libraryInfo = getLibraryInfo(appItem.getFileItem());
                    caches_installed.put(appItem.getSourcePath(), libraryInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (apkFile != null) {
            libraryInfo = caches_outside_package.get(apkFile.getPath());
            if (libraryInfo == null) {
                try {
                    libraryInfo = getLibraryInfo(apkFile);
                    caches_outside_package.put(apkFile.getPath(), libraryInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        final LibraryInfo libraryInfo1 = libraryInfo;
        Global.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onApkLibraryGet(libraryInfo1);
                }
            }
        });
    }

    public static void clearAppLibraryCache() {
        caches_installed.clear();
    }

    public static void clearOutsidePackageCache() {
        caches_installed.clear();
        caches_outside_package.clear();
    }

    private LibraryInfo getLibraryInfo(FileItem file) throws Exception {
        LibraryInfo libraryInfo = new LibraryInfo();
        if (file.isFileInstance()) {
            ZipFile zipFile = new ZipFile(file.getFile());
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) continue;
                dealWithEntryName(zipEntry, libraryInfo);
            }
        } else if (file.isDocumentFile()) {
            ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if (zipEntry.isDirectory()) continue;
                dealWithEntryName(zipEntry, libraryInfo);
                zipEntry = zipInputStream.getNextEntry();
            }
        }
        return libraryInfo;
    }

    private void dealWithEntryName(ZipEntry zipEntry, LibraryInfo libraryInfo) {
        final String entryName = zipEntry.getName().toLowerCase().replace("\\", "/");
        for (LibraryInfo.LibraryType type : LibraryInfo.LibraryType.values()) {
            if (entryName.startsWith("lib/" + type.getName())) {
                putEntry(entryName, libraryInfo, type);
            }
        }
    }

    private void putEntry(String entryName, LibraryInfo libraryInfo, LibraryInfo.LibraryType libraryType) {
        Collection<String> libNames = libraryInfo.libraries.get(libraryType);
        if (libNames == null) {
            libNames = new HashSet<>();
            libraryInfo.libraries.put(libraryType, libNames);
        }
        libNames.add(entryName.substring(entryName.lastIndexOf("/") + 1));
    }
}
