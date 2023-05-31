package com.github.ghmxr.apkextractor.ui;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.ghmxr.apkextractor.R;

public class ImportingDialog extends ProgressDialog {

    private final long mTotal;

    public ImportingDialog(@NonNull Context context, long total) {
        super(context, context.getResources().getString(R.string.dialog_import_title));
        this.mTotal = total;
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    public void setCurrentWritingName(String filename) {
        super.setProgressText(getContext().getResources().getString(R.string.dialog_import_msg) + filename);
    }

    public void setProgress(long progress) {
        super.setProgress(progress, mTotal);
    }
}
