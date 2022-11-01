package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.utils.SPUtil;

public class AppItemSortConfigDialog extends AlertDialog implements View.OnClickListener {

    private final SharedPreferences settings;
    private final SortConfigDialogCallback callback;

    public AppItemSortConfigDialog(@NonNull Context context, @Nullable SortConfigDialogCallback callback) {
        super(context);
        this.callback = callback;

        settings = SPUtil.getGlobalSharedPreferences(context);

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sort, null);
        setView(dialogView);
        setTitle(context.getResources().getString(R.string.dialog_sort_appitem_title));

        int sort = settings.getInt(Constants.PREFERENCE_SORT_CONFIG, 0);
        RadioButton ra_default = dialogView.findViewById(R.id.sort_ra_default);
        RadioButton ra_name_ascend = dialogView.findViewById(R.id.sort_ra_ascending_appname);
        RadioButton ra_name_descend = dialogView.findViewById(R.id.sort_ra_descending_appname);
        RadioButton ra_size_ascend = dialogView.findViewById(R.id.sort_ra_ascending_appsize);
        RadioButton ra_size_descend = dialogView.findViewById(R.id.sort_ra_descending_appsize);
        RadioButton ra_update_time_ascend = dialogView.findViewById(R.id.sort_ra_ascending_date);
        RadioButton ra_update_time_descend = dialogView.findViewById(R.id.sort_ra_descending_date);
        RadioButton ra_install_time_ascend = dialogView.findViewById(R.id.sort_ra_ascending_install_date);
        RadioButton ra_install_time_descend = dialogView.findViewById(R.id.sort_ra_descending_install_date);
        RadioButton ra_package_name_ascend = dialogView.findViewById(R.id.sort_ra_ascending_package_name);
        RadioButton ra_package_name_descend = dialogView.findViewById(R.id.sort_ra_descending_package_name);

        ra_default.setChecked(sort == 0);
        ra_name_ascend.setChecked(sort == 1);
        ra_name_descend.setChecked(sort == 2);
        ra_size_ascend.setChecked(sort == 3);
        ra_size_descend.setChecked(sort == 4);
        ra_update_time_ascend.setChecked(sort == 5);
        ra_update_time_descend.setChecked(sort == 6);
        ra_install_time_ascend.setChecked(sort == 7);
        ra_install_time_descend.setChecked(sort == 8);
        ra_package_name_ascend.setChecked(sort == 9);
        ra_package_name_descend.setChecked(sort == 10);

        ra_default.setOnClickListener(this);
        ra_name_ascend.setOnClickListener(this);
        ra_name_descend.setOnClickListener(this);
        ra_size_ascend.setOnClickListener(this);
        ra_size_descend.setOnClickListener(this);
        ra_update_time_ascend.setOnClickListener(this);
        ra_update_time_descend.setOnClickListener(this);
        ra_install_time_ascend.setOnClickListener(this);
        ra_install_time_descend.setOnClickListener(this);
        ra_package_name_ascend.setOnClickListener(this);
        ra_package_name_descend.setOnClickListener(this);

        setButton(AlertDialog.BUTTON_NEGATIVE, context.getResources().getString(R.string.dialog_button_cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    public void onClick(View v) {
        int sort_config = 0;
        SharedPreferences.Editor editor = settings.edit();
        switch (v.getId()) {
            default:
                break;
            case R.id.sort_ra_default: {
                sort_config = 0;
            }
            break;
            case R.id.sort_ra_ascending_appname: {
                sort_config = 1;
            }
            break;
            case R.id.sort_ra_descending_appname: {
                sort_config = 2;
            }
            break;
            case R.id.sort_ra_ascending_appsize: {
                sort_config = 3;
            }
            break;
            case R.id.sort_ra_descending_appsize: {
                sort_config = 4;
            }
            break;
            case R.id.sort_ra_ascending_date: {
                sort_config = 5;
            }
            break;
            case R.id.sort_ra_descending_date: {
                sort_config = 6;
            }
            break;
            case R.id.sort_ra_ascending_install_date: {
                sort_config = 7;
            }
            break;
            case R.id.sort_ra_descending_install_date: {
                sort_config = 8;
            }
            break;
            case R.id.sort_ra_ascending_package_name: {
                sort_config = 9;
            }
            break;
            case R.id.sort_ra_descending_package_name: {
                sort_config = 10;
            }
            break;
        }
        editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sort_config);
        editor.apply();
        cancel();
        if (callback != null) callback.onOptionSelected(sort_config);
    }

}
