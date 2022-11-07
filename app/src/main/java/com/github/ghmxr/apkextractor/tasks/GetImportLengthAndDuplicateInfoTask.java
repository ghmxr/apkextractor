package com.github.ghmxr.apkextractor.tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.utils.StorageUtil;
import com.github.ghmxr.apkextractor.utils.ZipFileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GetImportLengthAndDuplicateInfoTask extends Thread {

    //private Context context;
    private final List<ZipFileUtil.ZipFileInfo> zipFileInfos;
    private final List<ImportItem> importItems;
    private final GetImportLengthAndDuplicateInfoCallback callback;
    private volatile boolean isInterrupted = false;

    public GetImportLengthAndDuplicateInfoTask(@NonNull List<ImportItem> importItems
            , @NonNull List<ZipFileUtil.ZipFileInfo> zipFileInfos
            , @Nullable GetImportLengthAndDuplicateInfoCallback callback) {
        super();
        //this.context=context;
        this.zipFileInfos = zipFileInfos;
        this.importItems = importItems;
        this.callback = callback;
    }

    @Override
    public void run() {
        super.run();
        //final StringBuilder stringBuilder=new StringBuilder();
        final ArrayList<String> duplication_infos = new ArrayList<>();
        long total = 0;
        for (int i = 0; i < importItems.size(); i++) {
            if (isInterrupted) return;
            try {
                ImportItem importItem = importItems.get(i);
                ZipFileUtil.ZipFileInfo zipFileInfo = zipFileInfos.get(i);
                List<String> entryPaths = zipFileInfo.getEntryPaths();
                if (importItem.importData) {
                    total += zipFileInfo.getDataSize();
                    //stringBuilder.append(zipFileInfos.get(i).getAlreadyExistingFilesInfoInMainStorage(context));
                }
                if (importItem.importObb) {
                    total += zipFileInfo.getObbSize();
                    //stringBuilder.append(zipFileInfos.get(i).getAlreadyExistingFilesInfoInMainStorage(context));
                }
                if (importItem.importApk) {
                    total += zipFileInfo.getApkSize();
                    //stringBuilder.append(zipFileInfos.get(i).getAlreadyExistingFilesInfoInMainStorage(context));
                }
                for (String s : entryPaths) {
                    if (!s.contains("/") && s.endsWith(".apk")) continue;
                    if (!importItem.importObb && s.toLowerCase().startsWith("android/obb"))
                        continue;
                    if (!importItem.importData && s.toLowerCase().startsWith("android/data"))
                        continue;
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
                    File exportWritingTarget = new File(StorageUtil.getMainExternalStoragePath() + "/" + s);
                    if (exportWritingTarget.exists()) {
                        //stringBuilder.append(exportWritingTarget.getAbsolutePath());
                        //stringBuilder.append("\n\n");
                        duplication_infos.add(exportWritingTarget.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        final long total_length = total;
        if (callback != null && !isInterrupted) Global.handler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onCheckingFinished(duplication_infos, total_length);
            }
        });
    }

    public void setInterrupted() {
        this.isInterrupted = true;
    }

    public interface GetImportLengthAndDuplicateInfoCallback {
        void onCheckingFinished(@NonNull List<String> results, long total);
    }
}
