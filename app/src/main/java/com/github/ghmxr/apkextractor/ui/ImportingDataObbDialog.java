package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.utils.ZipFileUtil;

import java.util.ArrayList;
import java.util.List;

public class ImportingDataObbDialog extends AlertDialog implements View.OnClickListener {
    private final View view;
    private final List<ImportItem> list = new ArrayList<>();
    private final List<ImportItem> list_data_controllable = new ArrayList<>();
    private final List<ImportItem> list_obb_controllable = new ArrayList<>();
    private final List<ImportItem> list_apk_controllable = new ArrayList<>();
    private final CheckBox cb_data;
    private final CheckBox cb_obb;
    private final CheckBox cb_apk;
    private final ImportDialogDataObbConfirmedCallback callback;

//    private final ArrayList<ZipFileUtil.ZipFileInfo> zipFileInfos = new ArrayList<>();

    /**
     * 传入的importItems为原始数据
     */
    public ImportingDataObbDialog(@NonNull Context context, @NonNull List<ImportItem> importItems, @Nullable ImportDialogDataObbConfirmedCallback callback) {
        super(context);
        this.callback = callback;
        for (ImportItem importItem : importItems) {
            list.add(new ImportItem(importItem, false, false, false));
        }
        view = LayoutInflater.from(context).inflate(R.layout.dialog_data_obb, null);
        cb_data = view.findViewById(R.id.dialog_checkbox_data);
        cb_obb = view.findViewById(R.id.dialog_checkbox_obb);
        TextView tv_att = view.findViewById(R.id.data_obb_att);
        cb_apk = view.findViewById(R.id.dialog_checkbox_apk);
        cb_apk.setVisibility(View.VISIBLE);
        setView(view);
        setTitle(context.getResources().getString(R.string.dialog_import_data_obb_title));
        tv_att.setText(context.getResources().getString(R.string.dialog_import_data_obb_att));
        if (Build.VERSION.SDK_INT >= Global.USE_STANDALONE_DOCUMENT_FILE_PERMISSION) {
            tv_att.setText(context.getResources().getString(R.string.dialog_import_data_obb_att2));
        }
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                long import_data = 0;
                long import_obb = 0;
                long import_apk = 0;
                for (ImportItem importItem : list) {
                    try {
                        ZipFileUtil.ZipFileInfo zipFileInfo = importItem.getZipFileInfo();
                        if (zipFileInfo == null) {
                            zipFileInfo = ZipFileUtil.getZipFileInfoOfImportItem(importItem);
                        }
                        long data = zipFileInfo.getDataSize();
                        long obb = zipFileInfo.getObbSize();
                        long apk = zipFileInfo.getApkSize();
                        import_data += data;
                        import_obb += obb;
                        import_apk += apk;
                        if (data > 0) list_data_controllable.add(importItem);
                        if (obb > 0) list_obb_controllable.add(importItem);
                        if (apk > 0) list_apk_controllable.add(importItem);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                final long total_data = import_data;
                final long total_obb = import_obb;
                final long total_apk = import_apk;
                Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (total_data == 0 && total_obb == 0 && total_apk == 0) {
                            cancel();
                            new AlertDialog.Builder(getContext())
                                    .setTitle(getContext().getResources().getString(R.string.dialog_import_invalid_zip_title))
                                    .setMessage(getContext().getResources().getString(R.string.dialog_import_invalid_zip_message))
                                    .setPositiveButton(getContext().getResources().getString(R.string.dialog_button_confirm), new OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    }).show();
                            return;
                        }
                        view.findViewById(R.id.dialog_data_obb_wait_area).setVisibility(View.GONE);
                        view.findViewById(R.id.dialog_data_obb_show_area).setVisibility(View.VISIBLE);
                        cb_data.setEnabled(total_data > 0);
                        cb_obb.setEnabled(total_obb > 0);
                        cb_apk.setEnabled(total_apk > 0);
                        cb_data.setText("Data(" + Formatter.formatFileSize(getContext(), total_data) + ")");
                        cb_obb.setText("Obb(" + Formatter.formatFileSize(getContext(), total_obb) + ")");
                        cb_apk.setText("APK(" + Formatter.formatFileSize(getContext(), total_apk) + ")");
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ImportingDataObbDialog.this);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onClick(View v) {
        if (v.equals(getButton(AlertDialog.BUTTON_POSITIVE))) {
            if (!cb_apk.isChecked() && !cb_data.isChecked() && !cb_obb.isChecked()) {
                ToastManager.showToast(getContext(), getContext().getResources().getString(R.string.activity_detail_nothing_checked), Toast.LENGTH_SHORT);
                return;
            }
            if (cb_data.isChecked())
                for (ImportItem item : list_data_controllable) item.importData = true;
            if (cb_obb.isChecked())
                for (ImportItem item : list_obb_controllable) item.importObb = true;
            if (cb_apk.isChecked())
                for (ImportItem item : list_apk_controllable) item.importApk = true;
            if (callback != null) callback.onImportingDataObbConfirmed(list);
            cancel();
        }
    }

    public interface ImportDialogDataObbConfirmedCallback {
        void onImportingDataObbConfirmed(@NonNull List<ImportItem> importItems);
    }
}
