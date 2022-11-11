package com.github.ghmxr.apkextractor.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.tasks.GetDataObbTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataObbDialog extends AlertDialog implements View.OnClickListener {

    private final View view;
    private final DialogDataObbConfirmedCallback callback;
    private final List<AppItem> list = new ArrayList<>();
    private final List<AppItem> list_data_controllable = new ArrayList<>();
    private final List<AppItem> list_obb_controllable = new ArrayList<>();
    private final CheckBox cb_data;
    private final CheckBox cb_obb;

    /**
     * @param export_list 传递进来的AppItem可为源数据，初始Data和Obb导出值为false
     */
    public DataObbDialog(@NonNull Context context, @NonNull List<AppItem> export_list, final DialogDataObbConfirmedCallback callback) {
        super(context);
        this.callback = callback;
        for (AppItem appItem : export_list) {
            list.add(new AppItem(appItem, false, false));
        }
        view = LayoutInflater.from(context).inflate(R.layout.dialog_data_obb, null);
        cb_data = view.findViewById(R.id.dialog_checkbox_data);
        cb_obb = view.findViewById(R.id.dialog_checkbox_obb);
        TextView tv_att = view.findViewById(R.id.data_obb_att);
        tv_att.setText(context.getResources().getString(R.string.dialog_data_obb_message));
        setView(view);
        setTitle(context.getResources().getString(R.string.dialog_data_obb_title));

        setButton(AlertDialog.BUTTON_POSITIVE, context.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        setButton(AlertDialog.BUTTON_NEGATIVE, context.getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });


    }

    @Override
    public void show() {
        super.show();
        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(null);
        new GetDataObbTask(list, new GetDataObbTask.DataObbSizeGetCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataObbSizeGet(List<AppItem> containsData, List<AppItem> containsObb, Map<AppItem, GetDataObbTask.DataObbSizeInfo> map, GetDataObbTask.DataObbSizeInfo dataObbSizeInfo) {
                list_data_controllable.addAll(containsData);
                list_obb_controllable.addAll(containsObb);
                if (dataObbSizeInfo.data == 0 && dataObbSizeInfo.obb == 0) {
                    cancel();
                    if (callback != null) callback.onDialogDataObbConfirmed(list);
                    return;
                }
                view.findViewById(R.id.dialog_data_obb_wait_area).setVisibility(View.GONE);
                view.findViewById(R.id.dialog_data_obb_show_area).setVisibility(View.VISIBLE);
                cb_data.setEnabled(dataObbSizeInfo.data > 0);
                cb_obb.setEnabled(dataObbSizeInfo.obb > 0);
                cb_data.setText("Data(" + Formatter.formatFileSize(getContext(), dataObbSizeInfo.data) + ")");
                cb_obb.setText("Obb(" + Formatter.formatFileSize(getContext(), dataObbSizeInfo.obb) + ")");
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(DataObbDialog.this);
            }
        }).start();
    }

    @Override
    public void onClick(View v) {
        if (v.equals(getButton(AlertDialog.BUTTON_POSITIVE))) {
            if (cb_data.isChecked())
                for (AppItem item : list_data_controllable) item.exportData = true;
            if (cb_obb.isChecked())
                for (AppItem item : list_obb_controllable) item.exportObb = true;
            if (callback != null) callback.onDialogDataObbConfirmed(list);
            cancel();
        }
    }

    public interface DialogDataObbConfirmedCallback {
        void onDialogDataObbConfirmed(@NonNull List<AppItem> export_list);
    }
}
