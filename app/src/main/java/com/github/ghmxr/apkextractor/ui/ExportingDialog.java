package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.Formatter;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.AppItem;

import java.text.DecimalFormat;

public class ExportingDialog extends ProgressDialog {

    public ExportingDialog(@NonNull Context context) {
        super(context, context.getResources().getString(R.string.dialog_export_title));
        att.setText(context.getResources().getString(R.string.dialog_wait));
        progressBar.setIndeterminate(true);
    }

    public void setProgressOfApp(int current, int total, @NonNull AppItem item, @NonNull String write_path) {
        setTitle(getContext().getResources().getString(R.string.dialog_export_title) + "(" + current + "/" + total + ")" + ":" + item.getAppName());
        setIcon(item.getIcon());
        att.setText(getContext().getResources().getString(R.string.dialog_export_msg_apk) + write_path);
    }

    public void setProgressOfWriteBytes(long current, long total) {
        if (current < 0) return;
        if (current > total) return;
        if (progressBar.isIndeterminate()) {
            progressBar.setIndeterminate(false);
        }
        progressBar.setMax((int) (total / 1024));
        progressBar.setProgress((int) (current / 1024));
        DecimalFormat dm = new DecimalFormat("#.00");
        int percent = (int) (Double.valueOf(dm.format((double) current / total)) * 100);
        att_right.setText(Formatter.formatFileSize(getContext(), current) + "/" + Formatter.formatFileSize(getContext(), total) + "(" + percent + "%)");
    }

    public void setSpeed(long bytes) {
        att_left.setText(Formatter.formatFileSize(getContext(), bytes) + "/s");
    }

    public void setProgressOfCurrentZipFile(@NonNull String write_path) {
        att.setText(getContext().getResources().getString(R.string.dialog_export_zip) + write_path);
    }
}
