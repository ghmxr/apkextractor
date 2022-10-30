package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
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

    ProgressBar progressBar;
    TextView att;
    TextView att_left;
    TextView att_right;

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
}
