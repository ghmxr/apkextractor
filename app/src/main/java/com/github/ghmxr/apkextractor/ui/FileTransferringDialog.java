package com.github.ghmxr.apkextractor.ui;

import android.content.Context;

import androidx.annotation.NonNull;

public class FileTransferringDialog extends ProgressDialog {

    public FileTransferringDialog(@NonNull Context context, @NonNull String title) {
        super(context, title);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    public void setCurrentFileInfo(String info) {
        super.setProgressText(String.valueOf(info));
    }

    public void setProgressOfSending(long progress, long total) {
        super.setProgress(progress, total);
    }

}
