package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
import android.text.format.Formatter;

import androidx.annotation.NonNull;

import java.text.DecimalFormat;

public class FileTransferringDialog extends ProgressDialog {

    public FileTransferringDialog(@NonNull Context context, @NonNull String title) {
        super(context, title);
        setCancelable(false);
    }

    public void setCurrentFileInfo(String info) {
        att.setText(String.valueOf(info));
    }

    public void setProgressOfSending(long progress, long total) {
        if (progress < 0 || total <= 0) return;
        if (progress > total) return;
        progressBar.setMax((int) (total / 1024));
        progressBar.setProgress((int) (progress / 1024));
        DecimalFormat dm = new DecimalFormat("#.00");
        int percent = (int) (Double.valueOf(dm.format((double) progress / total)) * 100);
        att_right.setText(Formatter.formatFileSize(getContext(), progress) + "/" + Formatter.formatFileSize(getContext(), total) + "(" + percent + "%)");
    }

    public void setSpeed(long speed) {
        att_left.setText(Formatter.formatFileSize(getContext(), speed) + "/s");
    }

}
