package com.github.ghmxr.apkextractor.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.items.AppItem;

import java.io.OutputStream;

public class OutputUtil {

    /**
     * 为AppItem获取一个内置存储绝对写入路径
     * @param extension "apk"或者"zip"
     */
    public static @NonNull String getAbsoluteWritePath(@NonNull Context context, @NonNull AppItem item, @NonNull String extension){
        SharedPreferences settings= SPUtil.getGlobalSharedPreferences(context);
        return settings.getString(Constants.PREFERENCE_SAVE_PATH, Constants.PREFERENCE_SAVE_PATH_DEFAULT)
                +"/"+getWriteFileNameForAppItem(context,item,extension);
    }

    public static @Nullable DocumentFile getWritingDocumentFileForAppItem(@NonNull Context context,@NonNull AppItem appItem,@NonNull String extension) throws Exception{
        String writingFileName=getWriteFileNameForAppItem(context,appItem,extension);
        DocumentFile parent=getExportPathDocumentFile(context);
        DocumentFile documentFile=parent.findFile(writingFileName);
        if(documentFile!=null&&documentFile.exists())documentFile.delete();
        return parent.createFile(extension.toLowerCase().equals("apk")?"application/vnd.android.package-archive":"application/x-zip-compressed",writingFileName);
    }

    /*public static @Nullable DocumentFile getWritingDocumentFileForFileName(@NonNull Context context,@NonNull String fileName) throws Exception{
        DocumentFile parent=getExportPathDocumentFile(context);
        DocumentFile documentFile=parent.findFile(fileName);
        if(documentFile!=null&&documentFile.exists())documentFile.delete();
        return parent.createFile(EnvironmentUtil.getFileExtensionName(fileName).equalsIgnoreCase("apk")?
                "application/vnd.android.package-archive":"application/x-zip-compressed",
                fileName);
    }*/

    /**
     * 创建一个按照命名规则命名的写入documentFile的输出流
     * @param documentFile 要写入的documentFile
     * @return 已按照命名规则的写入的documentFile输出流
     */
    public static @Nullable
    OutputStream getOutputStreamForDocumentFile(@NonNull Context context, @NonNull DocumentFile documentFile) throws Exception{
        return context.getContentResolver().openOutputStream(documentFile.getUri());
    }

    /**
     * 获取导出根目录的documentFile
     */
    public static DocumentFile getExportPathDocumentFile(@NonNull Context context) throws Exception{
        String segments= SPUtil.getSaveSegment(context);
        return DocumentFileUtil.getDocumentFileBySegments(DocumentFile.fromTreeUri(context,Uri.parse(SPUtil.getExternalStorageUri(context))),segments);
    }

    /**
     * 为一个AppItem获取一个写入的文件名，例如example.apk
     */
    public static @NonNull String getWriteFileNameForAppItem(@NonNull Context context,@NonNull AppItem item,@NonNull String extension){
        SharedPreferences settings= SPUtil.getGlobalSharedPreferences(context);
        if(extension.toLowerCase().equals("apk")){
            return settings.getString(Constants.PREFERENCE_FILENAME_FONT_APK, Constants.PREFERENCE_FILENAME_FONT_DEFAULT).replace(Constants.FONT_APP_NAME, String.valueOf(item.getAppName()))
                    .replace(Constants.FONT_APP_PACKAGE_NAME, String.valueOf(item.getPackageName()))
                    .replace(Constants.FONT_APP_VERSIONCODE, String.valueOf(item.getVersionCode()))
                    .replace(Constants.FONT_APP_VERSIONNAME, String.valueOf(item.getVersionName()))+".apk";
        }
        if(extension.toLowerCase().equals("zip")){
            return settings.getString(Constants.PREFERENCE_FILENAME_FONT_ZIP, Constants.PREFERENCE_FILENAME_FONT_DEFAULT).replace(Constants.FONT_APP_NAME, String.valueOf(item.getAppName()))
                    .replace(Constants.FONT_APP_PACKAGE_NAME, String.valueOf(item.getPackageName()))
                    .replace(Constants.FONT_APP_VERSIONCODE, String.valueOf(item.getVersionCode()))
                    .replace(Constants.FONT_APP_VERSIONNAME, String.valueOf(item.getVersionName()))+".zip";
        }
        return "";
    }
}
