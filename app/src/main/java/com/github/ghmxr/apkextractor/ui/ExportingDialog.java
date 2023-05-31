package com.github.ghmxr.apkextractor.ui;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.AppItem;

public class ExportingDialog extends ProgressDialog {

    public ExportingDialog(@NonNull Context context) {
        super(context, context.getResources().getString(R.string.dialog_export_title));
        super.setProgressText(context.getResources().getString(R.string.dialog_wait));
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    public void setProgressOfApp(int current, int total, @NonNull AppItem item, @NonNull String write_path) {
        setTitle(getContext().getResources().getString(R.string.dialog_export_title) + "(" + current + "/" + total + ")" + ":" + item.getAppName());
        setIcon(item.getIcon());
        super.setProgressText(getContext().getResources().getString(R.string.dialog_export_msg_apk) + write_path);
    }

    public void setProgressOfCurrentZipFile(@NonNull String write_path) {
        super.setProgressText(getContext().getResources().getString(R.string.dialog_export_zip) + write_path);
    }
}
