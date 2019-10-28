package com.github.ghmxr.apkextractor.tasks;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.utils.OutputUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;
import com.github.ghmxr.apkextractor.utils.ZipFileUtil;

import java.io.File;
import java.util.List;

public class GetImportLengthAndDuplicateInfoTask extends Thread {

    private Context context;
    private List<ZipFileUtil.ZipFileInfo>zipFileInfos;
    private List<ImportItem>importItems;
    private GetImportLengthAndDuplicateInfoCallback callback;

    public GetImportLengthAndDuplicateInfoTask(@NonNull Context context, @NonNull List<ImportItem>importItems
            , @NonNull List<ZipFileUtil.ZipFileInfo>zipFileInfos
            , @Nullable GetImportLengthAndDuplicateInfoCallback callback) {
        super();
        this.context=context;
        this.zipFileInfos=zipFileInfos;
        this.importItems=importItems;
        this.callback=callback;
    }

    @Override
    public void run() {
        super.run();
        final StringBuilder stringBuilder=new StringBuilder();
        long total=0;
        for(int i=0;i<importItems.size();i++){
            try{
                ImportItem importItem=importItems.get(i);
                ZipFileUtil.ZipFileInfo zipFileInfo=zipFileInfos.get(i);
                List<String>entryPaths=zipFileInfo.getEntryPaths();
                if(importItem.importData){
                    total+=zipFileInfo.getDataSize();
                    //stringBuilder.append(zipFileInfos.get(i).getAlreadyExistingFilesInfoInMainStorage(context));
                }
                if(importItem.importObb){
                    total+=zipFileInfo.getObbSize();
                    //stringBuilder.append(zipFileInfos.get(i).getAlreadyExistingFilesInfoInMainStorage(context));
                }
                if(importItem.importApk){
                    total+=zipFileInfo.getApkSize();
                    //stringBuilder.append(zipFileInfos.get(i).getAlreadyExistingFilesInfoInMainStorage(context));
                }
                for(String s:entryPaths){
                    if(!s.contains("/")&&s.endsWith(".apk"))continue;
                    if(!importItem.importObb&&s.toLowerCase().startsWith("android/obb"))continue;
                    if(!importItem.importData&&s.toLowerCase().startsWith("android/data"))continue;
                    /*if(!s.contains("/")&&s.endsWith(".apk")){
                        if(SPUtil.getIsSaved2ExternalStorage(context)){
                            DocumentFile documentFile= OutputUtil.getExportPathDocumentFile(context);
                            DocumentFile writeDocumentFile=documentFile.findFile(s);
                            if(writeDocumentFile!=null){
                                stringBuilder.append(SPUtil.getDisplayingExportPath(context));
                                stringBuilder.append("/");
                                stringBuilder.append(s);
                                stringBuilder.append("\n\n");
                            }
                        }else{
                            File target=new File(SPUtil.getInternalSavePath(context)+"/"+s);
                            if(target.exists()){
                                stringBuilder.append(target.getAbsolutePath());
                                stringBuilder.append("\n\n");
                            }
                        }
                    }else{
                        File exportWritingTarget=new File(StorageUtil.getMainExternalStoragePath()+"/"+s);
                        if(exportWritingTarget.exists()){
                            stringBuilder.append(exportWritingTarget.getAbsolutePath());
                            stringBuilder.append("\n\n");
                        }
                    }*/
                    File exportWritingTarget=new File(StorageUtil.getMainExternalStoragePath()+"/"+s);
                    if(exportWritingTarget.exists()){
                        stringBuilder.append(exportWritingTarget.getAbsolutePath());
                        stringBuilder.append("\n\n");
                    }
                }
            }catch (Exception e){e.printStackTrace();}
        }
        final long total_length=total;
        if(callback!=null) Global.handler.post(new Runnable() {
            @Override
            public void run() {
                if(callback!=null)callback.onCheckingFinished(stringBuilder.toString(),total_length);
            }
        });
    }

    public interface GetImportLengthAndDuplicateInfoCallback{
        void onCheckingFinished(@NonNull String result,long total);
    }
}
