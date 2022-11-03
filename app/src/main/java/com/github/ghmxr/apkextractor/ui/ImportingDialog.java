package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.Formatter;

import com.github.ghmxr.apkextractor.R;

import java.text.DecimalFormat;

public class ImportingDialog extends ProgressDialog {
    private final long total;

    public ImportingDialog(@NonNull Context context, long total) {
        super(context, context.getResources().getString(R.string.dialog_import_title));
        this.total = total;
        progressBar.setIndeterminate(true);
        setCancelable(false);
    }

    public void setCurrentWritingName(String filename) {
        att.setText(getContext().getResources().getString(R.string.dialog_import_msg) + filename);
    }

    public void setProgress(long progress) {
        if (progressBar.isIndeterminate()) {
            progressBar.setIndeterminate(false);
        }
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
