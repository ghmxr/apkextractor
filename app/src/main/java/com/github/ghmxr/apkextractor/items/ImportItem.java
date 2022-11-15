package com.github.ghmxr.apkextractor.items;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.ghmxr.apkextractor.DisplayItem;
import com.github.ghmxr.apkextractor.MyApplication;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.PinyinUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.ZipFileUtil;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImportItem implements DisplayItem<ImportItem>, Comparable<ImportItem> {

    public enum ImportType {
        APK, ZIP
    }

    public static int sort_config = 0;

    private final FileItem fileItem;
    private final long length;
    private ImportType importType = ImportType.ZIP;

    private PackageInfo packageInfo;
    private Drawable drawable = null;
    private String version_name = "";
    private String version_code = "";
    private String minSdkVersion = "";
    private String targetSdkVersion = "";
    private String apkLabel = "";
    private final long lastModified;
    private ZipFileUtil.ZipFileInfo zipFileInfo;

    public transient boolean importData = false;
    public transient boolean importObb = false;
    public transient boolean importApk = false;

    public ImportItem(@NonNull FileItem fileItem) {
        this.fileItem = fileItem;
        final Context context = MyApplication.getApplication();
        version_name = context.getResources().getString(R.string.word_unknown);
        version_code = context.getResources().getString(R.string.word_unknown);
        minSdkVersion = context.getResources().getString(R.string.word_unknown);
        targetSdkVersion = context.getResources().getString(R.string.word_unknown);
        drawable = context.getResources().getDrawable(R.drawable.icon_file);
        if (fileItem.getName().trim().toLowerCase().endsWith(".zip")
                || fileItem.getName().trim().toLowerCase().endsWith(SPUtil.getCompressingExtensionName(context).toLowerCase())) {
            importType = ImportType.ZIP;
            drawable = context.getResources().getDrawable(R.drawable.icon_zip);
        }
        if (fileItem.getName().trim().toLowerCase().endsWith(".xapk")) {
            importType = ImportType.ZIP;
            drawable = context.getResources().getDrawable(R.drawable.icon_xapk);
        }
        if (fileItem.getName().trim().toLowerCase().endsWith(".apk")) {
            importType = ImportType.APK;
            PackageManager packageManager = context.getApplicationContext().getPackageManager();
            if (fileItem.isFileInstance()) {
                try {
                    packageInfo = packageManager.getPackageArchiveInfo(fileItem.getPath(), 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (packageInfo != null) {
                    if (packageInfo.applicationInfo == null) {
                        packageInfo.applicationInfo = new ApplicationInfo();
                    }
                    packageInfo.applicationInfo.sourceDir = fileItem.getPath();
                    packageInfo.applicationInfo.publicSourceDir = fileItem.getPath();
                    drawable = packageInfo.applicationInfo.loadIcon(packageManager);
                    apkLabel = String.valueOf(packageInfo.applicationInfo.loadLabel(packageManager));
                    version_name = packageInfo.versionName;
                    version_code = String.valueOf(packageInfo.versionCode);
                    if (Build.VERSION.SDK_INT > 23) {
                        minSdkVersion = String.valueOf(packageInfo.applicationInfo.minSdkVersion);
                    }
                    targetSdkVersion = String.valueOf(packageInfo.applicationInfo.targetSdkVersion);
                } else {
                    drawable = context.getResources().getDrawable(R.drawable.icon_apk);

                }
            } else {
                drawable = context.getResources().getDrawable(R.drawable.icon_apk);
            }

        }
        length = fileItem.length();
        lastModified = fileItem.lastModified();
    }

    public ImportItem(@NonNull ImportItem wrapper, boolean importData, boolean importObb, boolean importApk) {
        this.drawable = wrapper.drawable;
        this.version_name = wrapper.version_name;
        this.fileItem = wrapper.fileItem;
        this.importType = wrapper.importType;
        this.length = wrapper.length;
        this.lastModified = wrapper.lastModified;
        this.zipFileInfo = wrapper.getZipFileInfo();
        this.importData = importData;
        this.importObb = importObb;
        this.importApk = importApk;
    }

    @Override
    public Drawable getIconDrawable() {
        return drawable;
    }

    @Override
    public String getTitle() {
        return fileItem.getName();
    }

    @Override
    public String getDescription() {
        DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance();
        if (importType == ImportType.APK)
            return dateFormat.format(new Date(lastModified)) + "(" + version_name + ")";
        return dateFormat.format(new Date(lastModified));
    }

    @Override
    public long getSize() {
        return length;
    }

    @Override
    public boolean isRedMarked() {
        return false;
    }

    public String getItemName() {
        return fileItem.getName();
    }

    public long getLastModified() {
        return lastModified;
    }

    public ImportType getImportType() {
        return importType;
    }

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    /**
     * 只针对apk的版本名
     */
    public String getVersionName() {
        return version_name;
    }

    public String getVersionCode() {
        return version_code;
    }

    public String getMinSdkVersion() {
        return minSdkVersion;
    }

    public String getTargetSdkVersion() {
        return targetSdkVersion;
    }

    public Drawable getItemIconDrawable() {
        return drawable;
    }

    public String getApkLabel() {
        return apkLabel;
    }

    public FileItem getFileItem() {
        return fileItem;
    }

    /**
     * 重命名导入项的文件名
     *
     * @param newName 文件名，对应的是原始路径下
     * @throws Exception 重命名可能抛的异常
     */
    public boolean renameFileItemTo(String newName) throws Exception {
        final boolean success = fileItem.renameTo(newName);
        if (success) {
            //将packageInfo的path更改为新重命名完成的path否则将无法获取包的信息
            if (packageInfo != null) {
                if (packageInfo.applicationInfo == null) {
                    packageInfo.applicationInfo = new ApplicationInfo();
                }
                packageInfo.applicationInfo.sourceDir = fileItem.getPath();
                packageInfo.applicationInfo.publicSourceDir = fileItem.getPath();
            }
        }
        return success;
    }

    /**
     * 当本项目为zip包时的输入流
     */
    public @Nullable
    InputStream getZipInputStream() throws Exception {
        if (importType == ImportType.ZIP) return fileItem.getInputStream();
        return null;
    }

    public @Nullable
    Uri getUri() {
        if (fileItem.isDocumentFile()) {
            return fileItem.getDocumentFile().getUri();
        }
        if (fileItem.isFileInstance()) {
            return EnvironmentUtil.getUriForFileByFileProvider(MyApplication.getApplication(), fileItem.getFile());
        }
        if (fileItem.isShareUriInstance()) {
            return fileItem.getContentUri();
        }
        return null;
    }

    /**
     * 如果此导入项为存储到内置存储的Uri.fromFile()
     *
     * @return uri
     */
    public @Nullable
    Uri getUriFromFile() {
        if (fileItem.isFileInstance()) return Uri.fromFile(fileItem.getFile());
        return null;
    }

    public ZipFileUtil.ZipFileInfo getZipFileInfo() {
        return zipFileInfo;
    }

    public void setZipFileInfo(ZipFileUtil.ZipFileInfo zipFileInfo) {
        this.zipFileInfo = zipFileInfo;
    }

    @Override
    public int compareTo(@NonNull ImportItem o) {
        switch (sort_config) {
            default:
                break;
            case 1: {
                try {
                    if (PinyinUtil.getFirstSpell(getTitle()).toLowerCase().compareTo(PinyinUtil.getFirstSpell(o.getTitle()).toLowerCase()) > 0)
                        return 1;
                    if (PinyinUtil.getFirstSpell(getTitle()).toLowerCase().compareTo(PinyinUtil.getFirstSpell(o.getTitle()).toLowerCase()) < 0)
                        return -1;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;
            case 2: {
                try {
                    if (PinyinUtil.getFirstSpell(getTitle()).toLowerCase().compareTo(PinyinUtil.getFirstSpell(o.getTitle()).toLowerCase()) < 0)
                        return 1;
                    if (PinyinUtil.getFirstSpell(getTitle()).toLowerCase().compareTo(PinyinUtil.getFirstSpell(o.getTitle()).toLowerCase()) > 0)
                        return -1;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;
            case 3: {
                if (getSize() - o.getSize() > 0) return 1;
                if (getSize() - o.getSize() < 0) return -1;
            }
            break;
            case 4: {
                if (getSize() - o.getSize() < 0) return 1;
                if (getSize() - o.getSize() > 0) return -1;
            }
            break;
            case 5: {
                if (getLastModified() - o.getLastModified() > 0) return 1;
                if (getLastModified() - o.getLastModified() < 0) return -1;
            }
            break;
            case 6: {
                if (getLastModified() - o.getLastModified() < 0) return 1;
                if (getLastModified() - o.getLastModified() > 0) return -1;
            }
            break;
        }
        return 0;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ImportItem) {
            if (fileItem != null && ((ImportItem) obj).fileItem != null) {
                return fileItem.toString().equalsIgnoreCase(((ImportItem) obj).fileItem.toString());
            }
        }
        return super.equals(obj);
    }
}
