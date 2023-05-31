package com.github.ghmxr.apkextractor.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.github.ghmxr.apkextractor.R;

public abstract class ProgressDialog extends AlertDialog {

    private final ProgressBar progressBar;
    private final TextView att;
    private final TextView att_left;
    private final TextView att_right;

    public ProgressDialog(@NonNull Context context, @NonNull String title) {
        super(context);
        View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_with_progress, null);
        setView(dialog_view);
        progressBar = dialog_view.findViewById(R.id.dialog_progress_bar);
        att = dialog_view.findViewById(R.id.dialog_att);
        att_left = dialog_view.findViewById(R.id.dialog_att_left);
        att_right = dialog_view.findViewById(R.id.dialog_att_right);
        ((AppCompatCheckBox) dialog_view.findViewById(R.id.dialog_progress_keep_on)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        setTitle(title);
        progressBar.setIndeterminate(true);
    }

    @Override
    public void show() {
        super.show();
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setProgressText(String s) {
        att.setText(s);
    }

    @SuppressLint("SetTextI18n")
    public void setProgress(long current, long total) {
        if (current < 0) return;
        if (current > total) {
            if (!progressBar.isIndeterminate()) {
                progressBar.setIndeterminate(true);
            }
            att_right.setText(Formatter.formatFileSize(getContext(), current));
            return;
        }
        if (progressBar.isIndeterminate()) {
            progressBar.setIndeterminate(false);
        }
        int max = (int) (total / 1024);
        if (progressBar.getMax() != max) {
            progressBar.setMax(max);
        }
        progressBar.setProgress((int) (current / 1024));
        int percent = (int) (((double) current / total) * 100);
        att_right.setText(Formatter.formatFileSize(getContext(), current) + "/" + Formatter.formatFileSize(getContext(), total) + "(" + percent + "%)");
    }

    @SuppressLint("SetTextI18n")
    public void setSpeed(long speedOfBytes) {
        att_left.setText(Formatter.formatFileSize(getContext(), speedOfBytes) + "/s");
    }
}
